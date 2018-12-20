#!groovy

// ------------------------------
// staging methods
// ------------------------------

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
  def dbJob = readFile("config/db.sh").trim()
  sh "${ctx.sshCmd} -l ${ctx.sshUser} ${ctx.dbIp} sudo yum install -y postgresql96 jq wget"
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
}

def bootstrapOkapi(ctx) {
  stopFolioDockers(ctx, ctx.okapiIp)
  def okapiVersionResp = httpRequest "${ctx.stableFolio}:9130/_/version"
  def okapiVersion = okapiVersionResp.content
  def okapiJob = readFile("config/okapi.sh").trim()
  okapiJob = okapiJob.replace('${okapiPvtIp}', ctx.okapiPvtIp)
  okapiJob = okapiJob.replace('${dbPvtIp}', ctx.dbPvtIp)
  okapiJob = okapiJob.replace('${okapiVersion}', okapiVersion)
  sh "${ctx.sshCmd} -l ${ctx.sshUser} ${ctx.okapiIp} \"${okapiJob}\""
  timeout(5) {
    waitUntil {
      try {
        httpRequest "http://${ctx.okapiIp}:9130/_/version"
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
  def pgconf = readFile("config/pg.json").trim()
  pgconf = pgconf.replace('${db_host}', ctx.dbPvtIp)
  echo "pgconf: ${pgconf}"
  writeFile(text: pgconf, file: "folio-conf/pg.json")
  sh "${ctx.scpCmd} folio-conf ${ctx.sshUser}@${ctx.modsIp}:/tmp"
  sh "${ctx.scpCmd} folio-conf ${ctx.sshUser}@${ctx.dbIp}:/tmp"

  def mods = getMods(ctx.stableFolio + "/install.json")
  echo "mods: ${mods}"
  mods = registerMods(mods, ctx.mdRepo, ctx.okapiIp)
  echo "valid mods: ${mods}"
  deployMods(mods, ctx.okapiIp, ctx.modsIp, ctx.modsPvtIp, ctx.tenant, ctx.sshCmd, ctx.sshUser)
}

def populateData(ctx) {
  def cmd = readFile("config/data.sh").trim()
  cmd = cmd.replace('${dataName}', ctx.dataName)
  cmd = cmd.replace('${tenant}', ctx.tenant)
  sh "${ctx.sshCmd} -l ${ctx.sshUser} ${ctx.dbIp} rm -f ${ctx.dataName}.tar.gz && wget ${ctx.dataRepo}/${ctx.dataName}.tar.gz"
  sh "${ctx.sshCmd} -l ${ctx.sshUser} ${ctx.dbIp} tar -zxvf ${ctx.dataName}.tar.gz"
  sh "${ctx.sshCmd} -l ${ctx.sshUser} ${ctx.dbIp} \"cd ${ctx.dataName} && ${cmd}\""
}

def runJmeterTests(ctx) {
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
  def files = findFiles(glob: '**/*.jmx')
  for (file in files) {
    def cmd = cmdTemplate + " -t ${file}"
    cmd += " -j jmeter_${BUILD_NUMBER}_${file.name}.log"
    echo "${cmd}"
    sh "${cmd}"
  }
  archiveArtifacts "jmeter_*"
  perfReport errorFailedThreshold: 1, percentiles: '0,50,90,100', sourceDataFiles: "${jMeterOutput}"
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
def getMods(mdRepo) {
  def resp = httpRequest "${mdRepo}"
  def mods = readJSON text: resp.content
  def latestMods = [:]
  for (mod in mods) {
    def group = (mod.id =~ /(^\D+)-(\d+.*$)/)
    def modName = group[0][1]
    // only select backend (mod-) and frontend (folio-) modules
    if (!modName.startsWith("mod-") && !modName.startsWith("folio_")) {
      continue
    }
    def modVer = group[0][2]
    if (!latestMods.containsKey(modName) || compareVersion(modVer, latestMods.get(modName))) {
      latestMods.put(modName, modVer)
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
    validMods.put(entry.getKey(), entry.getValue())
    httpRequest httpMode: 'POST', requestBody: md.content, url: "http://${okapiIp}:9130/_/proxy/modules?check=false"
  }
  return validMods
}

// deploy modules
def deployMods(mods, okapiIp, modsIp, modsPvtIp, tenant, sshCmd, sshUser) {
  def port = 9200
  def modJobTemplate = readFile("config/mods.sh").trim()
  def modKbEbscoTemplate = readFile("config/mod-kb-ebsco.sh").trim()
  def installTemplate = readFile("config/install.json").trim()
  def discoveryTemplate = readFile("config/discovery.json").trim()
  def installMods = []
  for (entry in mods.entrySet()) {
    def modName = entry.getKey()
    def modVer = entry.getValue()
    def modId = entry.getKey() + "-" + entry.getValue()
    // install needs both front and backend MDs
    installMods.add(installTemplate.replace('${modId}', modId))
    // discovery needs only backend MDs
    if (!modName.startsWith("mod-")) {
      continue
    }
    port += 1
    def modJob = modJobTemplate.replace('${modName}', modName)
    // mod-kb-ebsco has a different way to run Docker
    if (modName.equals("mod-kb-ebsco")) {
      modJob = modKbEbscoTemplate.replace('${modName}', modName)
    }
    modJob = modJob.replace('${port}', '' + port)
    modJob = modJob.replace('${modVer}', "" + modVer)
    // mod-inventory uses port 9403, not 8081
    if (modName.equals("mod-inventory")) {
      modJob = modJob.replace('8081', '9403')
    }
    // mod-circulation uses port 9801, not 8081
    if (modName.equals("mod-circulation")) {
      modJob = modJob.replace('8081', '9801')
    }
    // mod-login has a special parameter
    if (modName.equals("mod-login")) {
      modJob += " verify.user=true"
    }
    sh "${sshCmd} -l ${sshUser} ${modsIp} ${modJob}"
    def discoveryPayload = discoveryTemplate.replace('${modId}', modId)
    discoveryPayload = discoveryPayload.replace('${modUrl}', "http://${modsPvtIp}:${port}")
    echo "discoveryPayload: $discoveryPayload"
    httpRequest httpMode: 'POST', requestBody: discoveryPayload, url: "http://${okapiIp}:9130/_/discovery/modules"
  }
  def installPayload = "[" + installMods.join(",") + "]"
  echo "installPayload: $installPayload"
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
  def dockerCmd = "docker stop `docker ps -q`"
  try {
    sh "${ctx.sshCmd} -l ${ctx.sshUser} ${ip} '${dockerCmd}'"
    sleep 3
  } catch (e) {
  }
}

return this
