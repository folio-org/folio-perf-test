#!groovy

// ------------------------------
// staging methods
// ------------------------------

import groovy.json.JsonOutput

def getContext() {
  def ctx = [
    envName : "${params.EnvName}",
    awsKeyId : "${params.JenkinsAwsCredential}",
    sshKeyId : "${params.JenkinsEc2Credential}",
    awsKeyPair : "${params.AwsKeyPair}",

    awsSubnet : "${params.AwsSubnet}",
    awsSecurityGroups : "${params.AwsSecurityGroups}",
    awsUsePublicIp : "${params.AwsUsePublicIp}",

    mdRepo : "${params.MdRepo}",
    stableFolio : "${params.StableFolio}",
    fixedOkapi : "${params.FixedOkapi}",
    fixedMods : "${params.FixedMods}",
    dataRepo : "${params.SampleDataRepo}",
    dataName : "${params.SampleDataName}",

    sshUser : 'ec2-user', tenant : 'supertenant',
    okapiIp : '', modsIp : '', dbIp : '',
    okapiPvtIp : '', modsPvtIp : '', dbPvtIp : '',

    sshCmd : 'ssh -o StrictHostKeyChecking=no -o "UserKnownHostsFile /dev/null" -o "LogLevel ERROR"',
    scpCmd : 'scp -pr -o StrictHostKeyChecking=no'
  ]
  return ctx
}

def createEnv(ctx) {
  if (stackExists(ctx)) {
    echo "Skip creating ${ctx.envName}"
    return;
  }
  def cmd = "aws --output json cloudformation create-stack --stack-name ${ctx.envName}"
  cmd += " --template-body file://cloudformation/folio.yml"
  cmd += " --parameters ParameterKey=EnvName,ParameterValue=${ctx.envName}"
  cmd += " ParameterKey=KeyName,ParameterValue=${ctx.awsKeyPair}"
  cmd += " ParameterKey=Subnet,ParameterValue=${ctx.awsSubnet}"
  cmd += " ParameterKey=SecurityGroups,ParameterValue=${ctx.awsSecurityGroups}"
  cmd += " ParameterKey=PublicIp,ParameterValue=${ctx.awsUsePublicIp}"
  def resp = readJSON text: sh(script: "${cmd}", returnStdout: true)
  ctx.stackId = "${resp.StackId}"
  timeout(10) {
    cmd = "aws --output json cloudformation describe-stacks --stack-name ${ctx.envName}"
    waitUntil {
      sleep 10
      resp = readJSON text: sh(script: "${cmd}", returnStdout: true)
      "CREATE_COMPLETE" == "${resp.Stacks[0].StackStatus}"
    }
  }
}

def waitForEnv(ctx) {
  def cmd = "aws --output json cloudformation describe-stacks --stack-name ${ctx.envName}"
  def resp = readJSON text: sh(script: "${cmd}", returnStdout: true)
  getStackOutputIps(resp, ctx)
  echo "${ctx}"
  if (ctx.dns) {
    setupDns(ctx)
  }
  def dockerCmd = 'docker ps -l'
  timeout(10) {
    waitUntil {
      try {
        sh "${ctx.sshCmd} -l ${ctx.sshUser} ${ctx.dbIp} ${dockerCmd}"
        sh "${ctx.sshCmd} -l ${ctx.sshUser} ${ctx.modsIp} ${dockerCmd}"
        sh "${ctx.sshCmd} -l ${ctx.sshUser} ${ctx.okapiIp} ${dockerCmd}"
        true
      } catch (e) {
        sleep 10
        false
      }
    }
  }
}

def bootstrapDb(ctx) {
  stopFolioDockers(ctx, ctx.dbIp)
  sh "${ctx.sshCmd} -l ${ctx.sshUser} ${ctx.dbIp} sudo service ecs stop"
  sh "${ctx.sshCmd} -l ${ctx.sshUser} ${ctx.dbIp} sudo amazon-linux-extras install -y postgresql12"
  sh "${ctx.sshCmd} -l ${ctx.sshUser} ${ctx.dbIp} sudo yum install -y jq wget"
  sh "${ctx.sshCmd} -l ${ctx.sshUser} ${ctx.dbIp} sudo yum install -y git"
  sh "${ctx.sshCmd} -l ${ctx.sshUser} ${ctx.dbIp} sudo curl -L https://github.com/docker/compose/releases/download/1.25.4/docker-compose-\$(uname -s)-\$(uname -m) -o /usr/local/bin/docker-compose"
  sh "${ctx.sshCmd} -l ${ctx.sshUser} ${ctx.dbIp} sudo chmod +x /usr/local/bin/docker-compose"
  sh "${ctx.sshCmd} -l ${ctx.sshUser} ${ctx.dbIp} sudo rm -fr kafka-docker"
  sh "${ctx.sshCmd} -l ${ctx.sshUser} ${ctx.dbIp} git clone https://github.com/wurstmeister/kafka-docker.git"
  sh "${ctx.sshCmd} -l ${ctx.sshUser} ${ctx.dbIp} sed -i.bak 's/9092/9092:9092/g' kafka-docker/docker-compose.yml"
  sh "${ctx.sshCmd} -l ${ctx.sshUser} ${ctx.dbIp} sed -i.bak2 's/192.168.99.100/${ctx.dbPvtIp}/g' kafka-docker/docker-compose.yml"
  sh "${ctx.sshCmd} -l ${ctx.sshUser} ${ctx.dbIp} docker-compose -f kafka-docker/docker-compose.yml up -d"
  def dbJob = readFile("config/db.sh").trim()
  sh "${ctx.sshCmd} -l ${ctx.sshUser} ${ctx.dbIp} ${dbJob}"
  def dockerCmd = 'docker ps | grep foliodb | wc -l'
  timeout(10) {
    waitUntil {
      try {
        def rs = sh(script: "${ctx.sshCmd} -l ${ctx.sshUser} ${ctx.dbIp} '${dockerCmd}'", returnStdout: true)
        rs.toInteger() >= 1
      } catch (e) {
        sleep 10
        false
      }
    }
  }

  def elasticJob = readFile("config/elasticsearch.sh").trim()
  sh "${ctx.sshCmd} -l ${ctx.sshUser} ${ctx.dbIp} ${elasticJob}"
  def elasticCmd = 'docker ps | grep elasticsearch | wc -l'
  timeout(10) {
    waitUntil {
      try {
        def rs = sh(script: "${ctx.sshCmd} -l ${ctx.sshUser} ${ctx.dbIp} '${elasticCmd}'", returnStdout: true)
        rs.toInteger() >= 1
      } catch (e) {
        sleep 10
        false
      }
    }
  }
}

def bootstrapOkapi(ctx) {
  stopFolioDockers(ctx, ctx.okapiIp)
  sh "${ctx.sshCmd} -l ${ctx.sshUser} ${ctx.okapiIp} sudo service ecs stop"
  def okapiVersion
  if (ctx.fixedOkapi) {
    okapiVersion = ctx.fixedOkapi
  } else {
    def okapiUrl = ctx.stableFolio.replaceFirst('\\.', '-okapi.')
    def okapiVersionResp = httpRequest url: "${okapiUrl}/_/version",
    customHeaders: [
      [name: 'x-okapi-tenant', value: 'supertenant']
    ]
    okapiVersion = okapiVersionResp.content
  }
  def okapiJob = readFile("config/okapi.sh").trim()
  if (okapiVersion.indexOf("SNAPSHOT") > 0) {
    okapiJob = okapiJob.replace("folioorg", "folioci")
  }
  okapiJob = okapiJob.replace('${okapiPvtIp}', ctx.okapiPvtIp )
  okapiJob = okapiJob.replace('${dbPvtIp}', ctx.dbPvtIp)
  okapiJob = okapiJob.replace('${okapiVersion}', okapiVersion)
  sh "${ctx.sshCmd} -l ${ctx.sshUser} ${ctx.okapiIp} \"${okapiJob}\""
  timeout(5) {
    waitUntil {
      try {
        httpRequest url: "http://${ctx.okapiIp}:9130/_/version",
        customHeaders: [
          [name: 'x-okapi-tenant', value: 'supertenant']
        ]
        true
      } catch (e) {
        sleep 10
        false
      }
    }
  }
}

def bootstrapModules(ctx) {
  stopFolioDockers(ctx, ctx.modsIp)
  sh "${ctx.sshCmd} -l ${ctx.sshUser} ${ctx.modsIp} sudo service ecs stop"
  def pgconf = readFile("config/pg.json").trim()
  pgconf = pgconf.replace('${db_host}', ctx.dbPvtIp)
  echo "pgconf: ${pgconf}"
  writeFile(text: pgconf, file: "folio-conf/pg.json")
  sh "${ctx.scpCmd} folio-conf ${ctx.sshUser}@${ctx.modsIp}:/tmp"
  sh "${ctx.scpCmd} folio-conf ${ctx.sshUser}@${ctx.dbIp}:/tmp"

  def mods = getMods(ctx.fixedMods, ctx.stableFolio + "/okapi-install.json")
  // def mods = getMods(ctx.fixedMods, ctx.stableFolio.replaceFirst("\\.", "-okapi.") + "/_/proxy/tenants/diku/modules")
  echo "mods: ${mods}"
  mods = registerMods(mods, ctx.mdRepo, ctx.okapiIp)
  echo "valid mods: ${mods}"
  deployMods(mods, ctx.okapiIp, ctx.modsIp, ctx.modsPvtIp, ctx.dbPvtIp, ctx.tenant, ctx.sshCmd, ctx.sshUser)
}

def populateData(ctx) {
  def cmd = readFile("config/data.sh").trim()
  // change to always use perf name for convenience
  // cmd = cmd.replace('${dataName}', ctx.dataName)
  cmd = cmd.replace('${tenant}', ctx.tenant)
  sh "${ctx.sshCmd} -l ${ctx.sshUser} ${ctx.dbIp} rm -f ${ctx.dataName}.tar.gz"
  sh "${ctx.sshCmd} -l ${ctx.sshUser} ${ctx.dbIp} wget ${ctx.dataRepo}/${ctx.dataName}.tar.gz"
  sh "${ctx.sshCmd} -l ${ctx.sshUser} ${ctx.dbIp} tar -zxvf ${ctx.dataName}.tar.gz"
  // change to always use perf name for convenience
  // sh "${ctx.sshCmd} -l ${ctx.sshUser} ${ctx.dbIp} \"cd ${ctx.dataName} && ${cmd}\""
  sh "${ctx.sshCmd} -l ${ctx.sshUser} ${ctx.dbIp} \"cd perf && ${cmd}\""
}

def runJmeterTests(ctx, platformOnly=false) {
  def jMeterInput = "Folio-Test-Plans"
  def jMeterOutput = "jmeter_perf.jtl"

  def jMeterConfTemplate = readFile("config/jmeter.csv").trim()
  def jMeterConf = jMeterConfTemplate.replace('tenant', ctx.tenant)
  jMeterConf = jMeterConf.replace('user', 'admin').replace('password', 'admin')
  jMeterConf = jMeterConf.replace('protocol', 'http').replace('host', ctx.okapiIp).replace('port', '9130')
  echo "JMeter config: ${jMeterConf}"
  writeFile(text: jMeterConf, file: "${jMeterInput}/config.csv")

  def jMeterKBConfTemplate = readFile("config/jmeterkb.csv").trim()
  def jMeterKBConf = jMeterKBConfTemplate.replace('kburl', 'https://api.ebsco.io')
  jMeterKBConf = jMeterKBConf.replace('custid', env.RMAPI_CUSTID).replace('apikey', env.RMAPI_PASSWORD)
  writeFile(text: jMeterKBConf, file: "${jMeterInput}/kbconfig.csv")

  def cmdTemplate = "jmeter -Jjmeter.save.saveservice.output_format=xml -n"
  cmdTemplate += " -l ${jMeterOutput}"
  def pattern = platformOnly ? 'Folio-Test-Plans/platform-workflow-performance/*.jmx' : '**/*.jmx'
  def files = findFiles(glob: pattern)
  for (file in files) {
    // skip broken tests
    if (file.path.indexOf("loan-rules") > 0) {
      echo "skip ${file}"
      continue;
    }
    // skip benchmark files
    if (file.path.indexOf("benchmark") > 0) {
      echo "skip ${file}"
      continue;
    }
    // skip edge modules till env is ready
    if (file.path.indexOf("edge-") > 0) {
      echo "skip ${file}"
      continue;
    }
    // skip common modules - they are included by others
    if (file.path.indexOf("common") > 0) {
      echo "skip ${file}"
      continue;
    }

    // skip kb-ebsco modules per rm api request
    if (file.path.indexOf("kb-ebsco-java") > 0) {
      echo "skip ${file}"
      continue;
    }

    def cmd = cmdTemplate + " -t ${file}"
    cmd += " -j jmeter_${BUILD_NUMBER}_${file.name}.log"
    echo "${cmd}"
    sh "${cmd}"
  }
  archiveArtifacts "jmeter_*"
  perfReport errorFailedThreshold: 1, percentiles: '0,50,90,100', sourceDataFiles: "${jMeterOutput}"
}

def runNewman(ctx, postmanEnvironment) {
  def excludeFolders = ['environment', 'outdated', 'smoke-test']
  echo "Checkout folio-api-tests"
  checkout([
    $class: 'GitSCM',
    branches: [[name: '*/master']],
    extensions: scm.extensions + [[$class: 'SubmoduleOption',
                                    disableSubmodules: false,
                                    parentCredentials: false,
                                    recursiveSubmodules: true,
                                    reference: '',
                                    trackingSubmodules: false],
                                  [$class: 'RelativeTargetDirectory',
                                    relativeTargetDir: 'folio-api-tests']],
    userRemoteConfigs: [[url: 'https://github.com/folio-org/folio-api-tests.git']]
  ])
  def okapiDns = "ec2-" + ctx.okapiIp.replaceAll(/\./, "-") + ".compute-1.amazonaws.com"
  def okapiUser="super_admin"
  def okapiPwd="admin"
  dir("${env.WORKSPACE}/folio-api-tests") {
    withDockerContainer(image: 'postman/newman', args: '--user 0:0 --entrypoint=\'\'') {
      sh "npm install -g newman-reporter-htmlextra newman-reporter-testrail"
      jsonFiles = findFiles(glob: '**/*.json')
      for (file in jsonFiles) {
        def folderName = file.path.split('/')[0]
        def collectionName = file.name.tokenize('.')[0]
        if(folderName in excludeFolders) {
          echo "[DEBUG] ${file} is skipped"
          continue
        }
        echo "Run ${file.path} collection"
        withEnv(["TESTRAIL_DOMAIN=${params.TestRailUrl}", "TESTRAIL_PROJECTID=${params.TestRailProjectId}", "TESTRAIL_SUITEID=${folderName}", "TESTRAIL_TITLE=${collectionName}"]) {
          withCredentials([usernamePassword(credentialsId: 'testrail-ut56', passwordVariable: 'testrail_password', usernameVariable: 'TESTRAIL_USERNAME'),
                          string(credentialsId: 'testrail_ut56_token', variable: 'TESTRAIL_APIKEY')]) {
            sh """
              TESTRAIL_DOMAIN=${params.TestRailUrl}
              TESTRAIL_PROJECTID=${params.TestRailProjectId}
              TESTRAIL_SUITEID=${folderName}
              TESTRAIL_TITLE=${collectionName}
              newman run ${file.path} -e ${postmanEnvironment} \
                --suppress-exit-code 1 \
                --env-var xokapitenant=${ctx.tenant} \
                --env-var url=${okapiDns} \
                --env-var username=${okapiUser} \
                --env-var password=${okapiPwd} \
                --reporter-junit-export test_reports/${collectionName}.xml \
                --reporter-htmlextra-export test_reports/${collectionName}.html \
                --reporters cli,junit,htmlextra,testrail
            """
          }
        }
      }
      junit(testResults: 'test_reports/*.xml')
      publishHTML (target: [
        allowMissing: false,
        alwaysLinkToLastBuild: false,
        keepAll: true,
        reportDir: 'test_reports',
        reportFiles: '*.html',
        reportName: "Postman Report"
      ])
    }
  }
}

def runIntegrationTests(ctx) {
  echo "Checkout folio-integration-tests"
  checkout([
    $class: 'GitSCM',
    branches: [[name: '*/master']],
    extensions: scm.extensions + [[$class: 'SubmoduleOption',
                                   disableSubmodules: false,
                                   parentCredentials: false,
                                   recursiveSubmodules: true,
                                   reference: '',
                                   trackingSubmodules: false],
                                  [$class: 'RelativeTargetDirectory',
                                   relativeTargetDir: 'folio-integration-tests']],
    userRemoteConfigs: [[url: 'https://github.com/folio-org/folio-integration-tests.git']]
  ])

  echo "Run all folio-integration-tests"
  dir("${env.WORKSPACE}/folio-integration-tests") {
    withMaven(
      jdk: 'openjdk-8-jenkins-slave-all',
      maven: 'maven3-jenkins-slave-all',
      mavenSettingsConfig: 'folioci-maven-settings'
    ) {
      def okapiDns = "ec2-" + ctx.okapiIp.replaceAll(/\./, "-") + ".compute-1.amazonaws.com"
      withCredentials([usernamePassword(credentialsId: 'testrail-ut56', passwordVariable: 'testrail_password', usernameVariable: 'testrail_user'), string(credentialsId: 'mod-kb-ebsco-key', variable: 'ebsco_key'), string(credentialsId: 'mod-kb-ebsco-url', variable: 'ebsco_url'), string(credentialsId: 'mod-kb-ebsco-id', variable: 'ebsco_id')]) {
      sh """
      export kbEbscoCredentialsApiKey=${ebsco_key}
      export kbEbscoCredentialsUrl=${ebsco_url}
      export kbEbscoCredentialsCustomerId=${ebsco_id}
      mvn test -Dkarate.env=${okapiDns} -DfailIfNoTests=false -Dtestrail_url=${TestRailUrl} -Dtestrail_userId=${testrail_user} -Dtestrail_pwd=${testrail_password} -Dtestrail_projectId=${TestRailProjectId} -DkbEbscoCredentialsApiKey=${ebsco_key} -DkbEbscoCredentialsUrl=${ebsco_url} -DkbEbscoCredentialsCustomerId=${ebsco_id}
      """
      }
    }
    sh "mkdir ${env.WORKSPACE}/folio-integration-tests/cucumber-reports"
    sh "find . | grep json | grep '/target/karate-reports' | xargs -i cp {} ${env.WORKSPACE}/folio-integration-tests/cucumber-reports"
    teams = ['thunderjet', 'firebird', 'core-functional', 'folijet', 'spitfire', 'vega', 'core-platform', 'erm-delivery', 'fse', 'stripes', 'leipzig',
             'ncip', 'thor', 'falcon', 'volaris', 'knowledgeware', 'spring']
    teams_test = ['spitfire', 'folijet', 'thunderjet', 'firebird', 'core_functional', 'vega', 'core_platform', 'falcon']
    team_modules = [spitfire: ['mod-kb-ebsco-java', 'tags', 'codexekb', 'mod-notes', 'mod-quick-marc', 'passwordvalidator'],
                    folijet: ['mod-source-record-storage', 'mod-source-record-manager', 'mod-data-import', 'data-import', 'mod-data-import-converter-storage'],
                    thunderjet: ['mod-finance', 'edge-orders', 'mod-gobi', 'mod-orders', 'mod-invoice', 'mod-ebsconet'],
                    firebird: ['mod-audit', 'edge-dematic', 'edge-caiasoft', 'dataexport', 'oaipmh'],
                    core_functional: ['mod-inventory', 'mod-circulation', 'mod-users-bl'],
                    vega: ['mod-event-config', 'mod-sender', 'mod-template-engine', 'mod-email', 'mod-notify', 'mod-feesfines', 'mod-patron-blocks', 'mod-calendar'],
                    core_platform: ['mod-configuration', 'mod-permissions', 'mod-login-saml', 'mod-user-import'],
                    falcon: ['mod-search']
                    ]
    dir("${env.WORKSPACE}/folio-integration-tests/cucumber-reports"){
      for (team in teams_test){
        sh """
        mkdir ${team}
        touch ${team}/status.txt
        touch ${team}/failed.txt
        echo -n SUCCESS > ${team}/status.txt
        """
        for (mod in team_modules[team]){
          sh """
          for i in \$(find .. | grep json | grep '/target/karate-reports'| grep summary); do
		        if [[ \$(cat \$i | grep ${mod}) ]]; then
			        if [[ \$(cat \$i | grep '"failed":true') ]]; then
				        echo -n FAILED > ${team}/status.txt
                echo ${mod} >> ${team}/failed.txt
				        break
			        fi
		        fi
	        done
          """
        }
      }
    }
    cucumber buildStatus: "UNSTABLE",
      fileIncludePattern: "*.json",
      jsonReportDirectory: "cucumber-reports"
  }
}

def teardownEnv(ctx) {
  if (!stackExists(ctx)) {
    echo "Skip tearing down ${ctx.envName}"
    return;
  }
  if (!ctx.stackId) {
    def cmd = "aws --output json cloudformation describe-stacks --stack-name ${ctx.envName}"
    def resp = readJSON text: sh(script: "${cmd}", returnStdout: true)
    ctx.stackId = "${resp.Stacks[0].StackId}"
  }
  sh "aws cloudformation delete-stack --stack-name ${ctx.stackId}"
  timeout(10) {
    waitUntil {
      sleep 10
      def cmd = "aws --output json cloudformation describe-stacks --stack-name ${ctx.stackId}"
      def resp = readJSON text: sh(script: "${cmd}", returnStdout: true)
      "DELETE_COMPLETE" == "${resp.Stacks[0].StackStatus}"
    }
  }
}

// ------------------------------
// supporting methods
// ------------------------------

// get IPs from stack output
def getStackOutputIps(output, ctx) {
  for (o in output.Stacks[0].Outputs) {
    switch(o.OutputKey) {
      case 'OkapiIp' : ctx.okapiIp = o.OutputValue; break
      case 'OkapiPvtIp' : ctx.okapiPvtIp = o.OutputValue; break
      case 'ModsIp' : ctx.modsIp = o.OutputValue; break
      case 'ModsPvtIp' : ctx.modsPvtIp = o.OutputValue; break
      case 'DbIp' : ctx.dbIp = o.OutputValue; break
      case 'DbPvtIp' : ctx.dbPvtIp = o.OutputValue; break
      default: break
    }
  }
}

def setupDns(ctx) {
  def cmd = "aws route53 list-hosted-zones --query 'HostedZones[?Name==`${ctx.dns}`].Id' --output text"
  def zoneId = (sh(script: "${cmd}", returnStdout: true)).trim()

  def dnsTemplate = readFile("config/dns.json").trim()
  def dns = dnsTemplate.replace('${domain}', ctx.dns)
  dns = dns.replace('${envName}', ctx.envName)
  dns = dns.replace('${okapiIp}', ctx.okapiIp)
  dns = dns.replace('${modsIp}', ctx.modsIp)
  dns = dns.replace('${dbIp}', ctx.dbIp)

  def dnsFile = 'output/dns.json'
  writeFile(text: dns, file: dnsFile)
  cmd = "aws route53 change-resource-record-sets --hosted-zone-id '${zoneId}' --change-batch file://${dnsFile}"
  sh "${cmd}"
}

// find stack output value
def getStackOutput(output, key) {
  for (o in output.Stacks[0].Outputs) {
    if (o.OutputKey == key) {
      return o.OutputValue
    }
  }
}

// get modules
def getMods(fixedMods, mdRepo) {
  def mods;
  if (fixedMods) {
    echo "fixed install.json: ${fixedMods}"
    mods = readJSON text: fixedMods
  } else {
    def resp = httpRequest "${mdRepo}"
    echo "new install.json: ${resp.content}"
    mods = readJSON text: resp.content
  }
  def latestMods = [:]
  for (mod in mods) {
// skip edge-sip2 for now due to regex issue
    // should be fixed later
    if (mod.id.startsWith("edge-sip2")) {
      continue
    }
    if (mod.id.startsWith("mod-oa")) {
      continue
    }
    if (mod.id.startsWith("mod-eusage-reports")) {
      continue
    }

    // registering in Okapi issue
    // should be fixed later
    if (mod.id.startsWith("mod-data-export-spring")) {
      continue
    }
	
	if (mod.id.startsWith("mod-data-export-worker")) {
      continue
    }
	if (mod.id.startsWith("mod-service-interaction")) {
      continue
    }
    def group = (mod.id =~ /(^\D+)-(\d+.*$)/)
    def modName = group[0][1]
    // only select backend (mod-) and frontend (folio-) modules
    if (!modName.startsWith("mod-") && !modName.startsWith("folio_")) {
      continue
    }
    // skip mod-marccat and folio_marccat for now due to database issue
    if (modName.startsWith("mod-marccat")) {
      continue
    }
    if (modName.startsWith("folio_marccat")) {
      continue
    }
    def modVer = group[0][2]
    if (!latestMods.containsKey(modName) || compareVersion(modVer, latestMods.get(modName))) {
      latestMods.put(modName, modVer)
    }
  }
  def extraMods = readJSON text : readFile("config/extraMods.json").trim()
  for (mod in extraMods) {
    if (!latestMods.containsKey(mod.name)) {
      latestMods.put(mod.name, mod.version)
    }
  }
  return latestMods
}

// compare module version
def compareVersion(a, b) {
  List verA = a.tokenize('.')
  List verB = b.tokenize('.')
  def commonIndices = Math.min(verA.size(), verB.size())
  for (int i = 0; i < commonIndices; ++i) {
    def numA = verA[i].replace('-SNAPSHOT', "").toInteger()
    def numB = verB[i].replace('-SNAPSHOT', "").toInteger()
    if (numA != numB) {
      return numA > numB
    }
  }
  return verA.size() > verB.size()
}

// register modules
def registerMods(mods, mdRepo, okapiIp) {
  def validMods = [:]
  for (entry in mods.entrySet()) {
    def modId = entry.getKey() + "-" + entry.getValue()
    def md = httpRequest url: "${mdRepo}/_/proxy/modules/${modId}", validResponseCodes: '100:399,404'
    if (md.status != 200) {
      echo "skip ${modId}"
      continue;
    }
    def mdBody = md.content
    // remove authtoken dependency to simplify logic
    def mdJson = readJSON text: md.content
    if (mdJson.requires) {
      def found = -1
      mdJson.requires.eachWithIndex { a, b ->
        if (a.id == 'authtoken') {
          found = b
        }
      }
      if (found >= 0) {
        mdJson.requires.remove(found)
      }
      mdBody = JsonOutput.toJson(mdJson)
    }
    validMods.put(entry.getKey(), entry.getValue())
    httpRequest httpMode: 'POST', requestBody: mdBody, url: "http://${okapiIp}:9130/_/proxy/modules?check=false"
  }
  return validMods
}

// deploy modules
def deployMods(mods, okapiIp, modsIp, modsPvtIp, dbPvtIp, tenant, sshCmd, sshUser) {
  def port = 9200
  def modJobTemplate = readFile("config/mods.sh").trim()
  def installTemplate = readFile("config/install.json").trim()
  def discoveryTemplate = readFile("config/discovery.json").trim()
  def installModsBatchOne = []
  def installModsBatchTwo = []
  def installModsBatchThree = []
  for (entry in mods.entrySet()) {
    def modName = entry.getKey()
    def modVer = entry.getValue()
    def modId = entry.getKey() + "-" + entry.getValue()
    def modInstall = installTemplate.replace('${modId}', modId)
    // install some modules first
    if (modName.equals("mod-users") ||
    modName.equals("mod-login") ||
    modName.equals("mod-permissions") ||
    modName.equals("mod-pubsub") ||
    modName.equals("mod-inventory-storage") ||
    modName.equals("mod-circulation-storage") ||
    modName.equals("mod-feesfines")) {
      installModsBatchOne.add(modInstall)
    } else if (modName.equals("mod-authtoken")) {
      installModsBatchThree.add(modInstall)
    } else {
      installModsBatchTwo.add(modInstall)
    }
    // discovery needs only backend MDs
    if (!modName.startsWith("mod-")) {
      continue
    }
    port += 1
    def modJob = modJobTemplate
    // mod-kb-ebsco has a different way to run Docker
    // if (modName.equals("mod-kb-ebsco")) {
    //   modJob = readFile("config/mod-kb-ebsco.sh").trim()
    // }
    // erm modules run differently
    if (modName.equals("mod-agreements") || modName.equals("mod-licenses")
    || modName.equals("mod-erm-usage")) {
      modJob = readFile("config/mod-erm.sh").trim()
      modJob = modJob .replace('${dbHost}', dbPvtIp)
      if (modName.equals("mod-erm-usage")) {
        modJob = modJob.replace('8080', '8081')
      }
    }
    // mod-inventory-storage, mod-source-record-storage and mod-ebsconet have different env variables
    if (modName.equals("mod-inventory-storage") || 
    modName.equals("mod-source-record-storage") || 
    modName.equals("mod-ebsconet") || 
    modName.equals("mod-source-record-manager")) {
      modJob = readFile("config/mod-inventory-storage.sh").trim()
      modJob = modJob.replace('${dbHost}', dbPvtIp)
      modJob = modJob.replace('${okapiIp}', okapiIp)
    }
    // mod-bursar-export and mod-password-validator have different env variables
    if (modName.equals("mod-bursar-export") || 
    modName.equals("mod-password-validator") || 
    modName.equals("mod-login")) {
      modJob = readFile("config/mod-bursar-export.sh").trim()
      modJob = modJob.replace('${dbHost}', dbPvtIp)
      modJob = modJob.replace('${okapiIp}', okapiIp)
    }
    // mod-search has different env variables
    if (modName.equals("mod-search")) {
      modJob = readFile("config/mod-search.sh").trim()
      modJob = modJob.replace('${dbHost}', dbPvtIp)
      modJob = modJob.replace('${okapiIp}', okapiIp)
    }
    //mod-inn-reach needs additional db params
	if ((modName.equals("mod-inn-reach")) ||
	modName.equals("mod-tags"))
	{
      modJob = readFile("config/mod-inn-reach.sh").trim()
      modJob = modJob.replace('${dbHost}', dbPvtIp)
    }
	// added s3 credentials to data-export
	if (modName.equals("mod-data-export")) { 
      modJob = readFile("config/mod-data-export.sh").trim()
      modJob = modJob.replace('${AWS_ACCESS_KEY_ID}', AWS_ACCESS_KEY_ID)
      modJob = modJob.replace('${AWS_SECRET_ACCESS_KEY}', AWS_SECRET_ACCESS_KEY)
	}
	if (modName.equals("mod-inventory")) { 
      modJob = readFile("config/mod-inventory.sh").trim()
	  modJob = modJob.replace('${dbHost}', dbPvtIp)
      modJob = modJob.replace('${okapiIp}', okapiIp)
	  }
    // temporary solution to escape mod-service-interaction failure
   //if (modName.equals("mod-service-interaction")) {
    //  continue 
      // modJob = readFile("config/mod-service-interaction.sh").trim()
      // modJob = modJob.replace('${dbHost}', dbPvtIp)
      // modJob = modJob.replace('${okapiIp}', okapiIp)
   // }
    // mod-pubsub has different env variables
    if (modName.equals("mod-pubsub") || 
    modName.equals("mod-ebsconet") ||
    modName.equals("mod-remote-storage") ||
    modName.equals("mod-quick-marc")) {
      modJob = readFile("config/mod-pubsub.sh").trim()
      modJob = modJob.replace('${dbHost}', dbPvtIp)
      modJob = modJob.replace('${okapiIp}', okapiIp)
    }
    // mod-graphql has a different way to run Docker
    if (modName.equals("mod-graphql")) {
      modJob = readFile("config/mod-graphql.sh").trim()
    }
    modJob = modJob.replace('${modName}', modName)
    modJob = modJob.replace('${port}', '' + port)
    modJob = modJob.replace('${modVer}', "" + modVer)
    // mod-inventory uses port 9403, not 8081
   // if (modName.equals("mod-inventory")) {
    //  modJob = modJob.replace('8081', '9403')
   // }
    // mod-circulation uses port 9801, not 8081
    if (modName.equals("mod-circulation")) {
      modJob = modJob.replace('8081', '9801')
	 
    }
    // mod-login has a special parameter
    if (modName.equals("mod-login")) {
      modJob += " verify.user=true"
    }
    // replace folioci to folioorg for non-snapshot version
    if (!modVer.toUpperCase().contains("SNAPSHOT") &&
    modVer.substring(modVer.lastIndexOf(".") + 1).toInteger() < 100000) {
      echo "change Docker Hub from folioci to folioorg for $modName-$modVer"
      modJob = modJob.replace('folioci', 'folioorg')
    }
    sh "${sshCmd} -l ${sshUser} ${modsIp} ${modJob}"
    def discoveryPayload = discoveryTemplate.replace('${modId}', modId)
    discoveryPayload = discoveryPayload.replace('${modUrl}', "http://${modsPvtIp}:${port}")
    echo "discoveryPayload: $discoveryPayload"
    httpRequest httpMode: 'POST', requestBody: discoveryPayload, url: "http://${okapiIp}:9130/_/discovery/modules"
  }
  // install modules with both reference data and sample data
  def installPayload = "[" + installModsBatchOne.join(",") + "]"
  echo "installPayload with both reference and sample: $installPayload"
  httpRequest httpMode: 'POST', requestBody: installPayload.toString(), url: "http://${okapiIp}:9130/_/proxy/tenants/${tenant}/install?tenantParameters=loadReference%3Dtrue%2CloadSample%3Dtrue"
  // install other modules without reference data and sample data
  installPayload = "[" + installModsBatchTwo.join(",") + "]"
  echo "installPayload without reference and sample: $installPayload"
  httpRequest httpMode: 'POST', requestBody: installPayload.toString(), url: "http://${okapiIp}:9130/_/proxy/tenants/${tenant}/install?tenantParameters=loadReference%3Dfalse%2CloadSample%3Dfalse"
  // install mod-authtoken, mod-login-saml, edge, and ui modules
  installPayload = "[" + installModsBatchThree.join(",") + "]"
  echo "installPayload of mod-authtoken: $installPayload"
  httpRequest httpMode: 'POST', requestBody: installPayload.toString(), url: "http://${okapiIp}:9130/_/proxy/tenants/${tenant}/install"
}

// test if stack exists
def stackExists(ctx) {
  try {
    sh("aws --output json cloudformation describe-stacks --stack-name ${ctx.envName}")
    return true;
  } catch (e) {
    return false;
  }
}

// stop existing FOLIO dockers
def stopFolioDockers(ctx, ip) {
  def dockerCmd = "docker ps -q | xargs --no-run-if-empty docker stop"
  try {
    sh "${ctx.sshCmd} -l ${ctx.sshUser} ${ip} '${dockerCmd}'"
    sleep 3
  } catch (e) {
  }
}

def notifySlack(String buildStatus = 'STARTED') {
  teams_test = ['spitfire', 'folijet', 'thunderjet', 'firebird', 'core_functional', 'vega', 'core_platform', 'falcon']
  teams_channels = [spitfire: '#spitfire', folijet: '#folijet', thunderjet: '#acquisitions-dev', firebird: '#firebird',
                    core_functional: '#prokopovych', vega: '#vega', core_platform: '#core-platform', falcon: '#falcon']

  // Build status of null means success.
  buildStatus = buildStatus ?: 'SUCCESS'
  def msg = "${buildStatus}: `${env.JOB_NAME}` #${env.BUILD_NUMBER}:\n${env.BUILD_URL}"
  
  //slackSend(color: color, message: msg, channel: '#api-integration-testing')
  for (team in teams_test) {
    def tests_status = readFile "${env.WORKSPACE}/folio-integration-tests/cucumber-reports/${team}/status.txt"
    def failed_mod = readFile "${env.WORKSPACE}/folio-integration-tests/cucumber-reports/${team}/failed.txt"
    def color
    def team_msg
    if (tests_status == 'STARTED') {
        color = '#D4DADF'
    } else if (tests_status == 'SUCCESS') {
        color = '#BDFFC3'
    } else if (tests_status == 'UNSTABLE') {
        color = '#FFFE89'
    } else {
      color = '#FF9FA1'
    }
    if (tests_status == 'FAILED'){
      team_msg = "${tests_status}: `${env.JOB_NAME}` #${env.BUILD_NUMBER}:\n${env.BUILD_URL}\n Failed:\n${failed_mod}"
    } else {
      team_msg = "${tests_status}: `${env.JOB_NAME}` #${env.BUILD_NUMBER}:\n${env.BUILD_URL}"
      }
    slackSend(color: color, message: team_msg, channel: "#karate-tests-reports-${team}")
    slackSend(color: color, message: team_msg, channel: "${teams_channels[team]}")
  }
}

return this
