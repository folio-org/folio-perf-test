#!groovy

properties([
    parameters([
        string(name: 'EnvName', defaultValue: 'folio-perf-test', description: 'Unique performance environment name'),
        string(name: 'AwsKeyPair', defaultValue: 'folio-live', description: 'Aws KeyPair name for EC2 instances'),
        string(name: 'AwsSubnet', defaultValue: 'default', description: 'AWS Subnet to create EC2 instances'),
        string(name: 'AwsSecurityGroups', defaultValue: 'default', description: 'AWS VPC Security Groups to use'),
        string(name: 'SshKeyFile', defaultValue: '/home/ec2-user/hji/key/folio-live.pem', description: 'Location of SSH key to connect to EC2'),
        string(name: 'JMeterFile', defaultValue: '/home/ec2-user/hji/tools/apache-jmeter-4.0/bin/jmeter.sh', description: 'Location of JMeter executables'),
        string(name: 'MdRepo', defaultValue: 'http://folio-registry.aws.indexdata.com', description: 'Module descriptor repository'),
        string(name: 'StableFolio', defaultValue: 'http://folio-snapshot-stable.aws.indexdata.com', description: 'Use stable version of modules'),
        string(name: 'SampleDataRepo', defaultValue: 'https://s3.amazonaws.com/folio-public-sample-data', description: 'Sample data repository'),
        string(name: 'SampleDataName', defaultValue: 'small', description: 'Sample dataset name'),
    ])
])

def envName = "${params.EnvName}"
def mdRepo = "${params.MdRepo}"
def stableFolio = "${params.StableFolio}"
def dataRepo = "${params.SampleDataRepo}"
def dataName = "${params.SampleDataName}"
def sshKeyFile = "${params.SshKeyFile}"
def jMeterPath = "${params.JMeterFile}"

// internal variables
def sshUser = "ec2-user"
def tenant = "supertenant"
def stackId = ""
def okapiIp = ""
def modsIp = ""
def dbIp = ""
def okapiPvtIp = ""
def modsPvtIp = ""
def dbPvtIp = ""

node {

    stage("Checkout") {
        sh "rm -fr *"
        checkout scm
    }

    stage("Create Environment") {
        def cmd = "aws cloudformation create-stack --stack-name " + envName
        cmd += " --template-body file://cloudformation/folio.yml"
        cmd += " --parameters ParameterKey=EnvName,ParameterValue=${envName}"
        cmd += " ParameterKey=KeyName,ParameterValue=${params.AwsKeyPair}"
        cmd += " ParameterKey=Subnet,ParameterValue=${params.AwsSubnet}"
        cmd += " ParameterKey=SecurityGroups,ParameterValue=${params.AwsSecurityGroups}"
        def resp = readJSON text: sh(script: "${cmd}", returnStdout: true)
        stackId = "${resp.StackId}"
        timeout(10) {
            cmd = "aws cloudformation describe-stacks --stack-name " + envName
            waitUntil {
                sleep 10
                resp = readJSON text: sh(script: "${cmd}", returnStdout: true)
                "CREATE_COMPLETE" == "${resp.Stacks[0].StackStatus}"
            }
        }
    }

    stage("Wait for Environment") {
        def cmd = "aws cloudformation describe-stacks --stack-name " + (stackId ?: envName)
        def resp = readJSON text: sh(script: "${cmd}", returnStdout: true)
        okapiIp = getStackOutput(resp, "OkapiIp")
        okapiPvtIp = getStackOutput(resp, "OkapiPvtIp")
        modsIp = getStackOutput(resp, "ModsIp")
        modsPvtIp = getStackOutput(resp, "ModsPvtIp")
        dbIp = getStackOutput(resp, "DbIp")
        dbPvtIp = getStackOutput(resp, "DbPvtIp")
        echo "$okapiIp, $okapiPvtIp, $modsIp, $modsPvtIp, $dbIp, $dbPvtIp"
        waitUntil {
            try {
                sh "ssh -o StrictHostKeyChecking=no -i ${sshKeyFile} -l ${sshUser} ${dbIp} docker ps -l"
                sh "ssh -o StrictHostKeyChecking=no -i ${sshKeyFile} -l ${sshUser} ${modsIp} docker ps -l"
                sh "ssh -o StrictHostKeyChecking=no -i ${sshKeyFile} -l ${sshUser} ${okapiIp} docker ps -l"
                true
            } catch (e) {
                sleep 10
                false
            }
        }
    }

    stage("Bootstrap DB") {
        def dbJob = readFile "config/db.sh"
        sh "ssh -o StrictHostKeyChecking=no -i ${sshKeyFile} -l ${sshUser} ${dbIp} sudo yum install -y postgresql96 jq wget"
        sh "ssh -o StrictHostKeyChecking=no -i ${sshKeyFile} -l ${sshUser} ${dbIp} ${dbJob}"
        timeout(10) {
            waitUntil {
                try {
                    def rs = sh(script: "ssh -o StrictHostKeyChecking=no -i ${sshKeyFile} -l ${sshUser} ${dbIp} 'docker ps | grep foliodb | wc -l'", returnStdout: true)
                    rs.toInteger() >= 1
                } catch (e) {
                    sleep 10
                    false
                }
            }
        }
    }

    stage("Bootstrap Okapi") {
        def okapiVersionResp = httpRequest "${stableFolio}:9130/_/version"
        def okapiVersion = okapiVersionResp.content
        def okapiJob = readFile "config/okapi.sh"
        okapiJob = okapiJob.replace('${okapiPvtIp}', okapiPvtIp)
        okapiJob = okapiJob.replace('${dbPvtIp}', dbPvtIp)
        okapiJob = okapiJob.replace('${okapiVersion}', okapiVersion)
        sh "ssh -o StrictHostKeyChecking=no -i ${sshKeyFile} -l ${sshUser} ${okapiIp} \"${okapiJob}\""
        timeout(5) {
            waitUntil {
                try {
                    httpRequest "http://${okapiIp}:9130/_/version"
                    true
                } catch (e) {
                    sleep 10
                    false
                }
            }
        }
    }

    stage("Bootstrap modules") {
        // db config
        def pgconf = readFile "config/pg.json"
        pgconf = pgconf.replace('${db_host}', dbPvtIp)
        echo "pgconf: ${pgconf}"
        writeFile(text: pgconf, file: "folio-conf/pg.json")
        sh "scp -pr -o StrictHostKeyChecking=no -i ${sshKeyFile} folio-conf ${sshUser}@${modsIp}:/tmp"
        sh "scp -pr -o StrictHostKeyChecking=no -i ${sshKeyFile} folio-conf ${sshUser}@${dbIp}:/tmp"

        // get MDs and register
        def mods = getMods(stableFolio + "/install.json")
        echo "mods: ${mods}"
        registerMods(mods, mdRepo, okapiIp)

        // deploy and enable mods
        deployMods(mods, okapiIp, modsIp, modsPvtIp, tenant, sshKeyFile, sshUser)
    }

    stage("Populate data") {
        def cmd = readFile "config/data.sh"
        cmd = cmd.replace('${dataName}', dataName)
        cmd = cmd.replace('${tenant}', tenant)
        sh "ssh -o StrictHostKeyChecking=no -i ${sshKeyFile} -l ${sshUser} ${dbIp} wget ${dataRepo}/${dataName}.tar.gz"
        sh "ssh -o StrictHostKeyChecking=no -i ${sshKeyFile} -l ${sshUser} ${dbIp} tar -zxvf ${dataName}.tar.gz"
        sh "ssh -o StrictHostKeyChecking=no -i ${sshKeyFile} -l ${sshUser} ${dbIp} \"cd ${dataName} && ${cmd}\""
    }

    stage("Running JMeter tests") {
        def jMeterInput = "Folio-Test-Plans"
        def jMeterOutput = "jmeter_perf.jtl"

        def jMeterConfTemplate = readFile "config/jmeter.csv"
        echo "JMeter config template: ${jMeterConfTemplate}"
        def jMeterConf = jMeterConfTemplate.replace('tenant', tenant)
        jMeterConf = jMeterConf.replace('user', 'admin')
        jMeterConf = jMeterConf.replace('password', 'admin')
        jMeterConf = jMeterConf.replace('protocol', 'http')
        jMeterConf = jMeterConf.replace('host', okapiIp)
        jMeterConf = jMeterConf.replace('port', '9130')
        echo "JMeter config: ${jMeterConf}"
        writeFile(text: jMeterConf, file: "${jMeterInput}/config.csv")

        def cmdTemplate = "${jMeterPath} -Jjmeter.save.saveservice.output_format=xml -n"
        cmdTemplate += " -l ${jMeterOutput}"
        def files = findFiles(glob: '**/*.jmx')
        for (file in files) {
            def cmd = cmdTemplate + " -t ${file}"
            cmd += " -j jmeter_${BUILD_NUMBER}_${file.name}.log"
            echo "${cmd}"
            sh "${cmd}"
        }
        archiveArtifacts "jmeter_*"
        perfReport percentiles: '0,50,90,100', sourceDataFiles: "${jMeterOutput}"
    }

    stage("Tear down environment") {
        if (!stackId) {
            def cmd = "aws cloudformation describe-stacks --stack-name " + envName
            def resp = readJSON text: sh(script: "${cmd}", returnStdout: true)
            stackId = "${resp.Stacks[0].StackId}"
        }
        sh "aws cloudformation delete-stack --stack-name " + (stackId ?: envName)
        timeout(10) {
            waitUntil {
                sleep 10
                def cmd = "aws cloudformation describe-stacks --stack-name " + stackId
                def resp = readJSON text: sh(script: "${cmd}", returnStdout: true)
                "DELETE_COMPLETE" == "${resp.Stacks[0].StackStatus}"
            }
        }
    }
}

// ------------------------------
// supporting methods
// ------------------------------
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
        // skip non-qualified UI modules
        if (modName.startsWith("folio_react") || modName.startsWith("folio_stripes-smart-components") ) {
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
    for (entry in mods.entrySet()) {
        def modId = entry.getKey() + "-" + entry.getValue()
        def md = httpRequest "${mdRepo}/_/proxy/modules/${modId}"
        httpRequest httpMode: 'POST', requestBody: md.content, url: "http://${okapiIp}:9130/_/proxy/modules?check=false"
    }
}

// deploy modules
def deployMods(mods, okapiIp, modsIp, modsPvtIp, tenant, sshKeyFile, sshUser) {
    def port = 9200
    def modJobTemplate = readFile "config/mods.sh"
    def modKbEbscoTemplate = readFile "config/mod-kb-ebsco.sh"
    def installTemplate = readFile "config/install.json"
    def discoveryTemplate = readFile "config/discovery.json"
    def installMods = [];
    for (entry in mods.entrySet()) {
        def modName = entry.getKey()
        def modVer = entry.getValue()
        def modId = entry.getKey() + "-" + entry.getValue()
        // install needs both front and backend MDs
        installMods.add(installTemplate.replace('${modId}', modId));
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
        if (modName.equals("mod-login")) {
            modJob += " verify.user=true"
        }
        sh "ssh -o StrictHostKeyChecking=no -i ${sshKeyFile} -l ${sshUser} ${modsIp} ${modJob}"
        def discoveryPayload = discoveryTemplate.replace('${modId}', modId)
        discoveryPayload = discoveryPayload.replace('${modUrl}', "http://${modsPvtIp}:${port}")
        echo "discoveryPayload: $discoveryPayload"
        httpRequest httpMode: 'POST', requestBody: discoveryPayload, url: "http://${okapiIp}:9130/_/discovery/modules"
    }
    def installPayload = "[" + installMods.join(",") + "]"
    echo "installPayload: $installPayload"
    httpRequest httpMode: 'POST', requestBody: installPayload.toString(), url: "http://${okapiIp}:9130/_/proxy/tenants/${tenant}/install"
}
