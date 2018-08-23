folio-perf-test - Jenkins pipeline to test FOLIO performance
=================================

Copyright (C) 2018 The Open Library Foundation

This software is distributed under the terms of the Apache License,
Version 2.0. See the file "[LICENSE](LICENSE)" for more information.

System requirements
-------------------

* Jenkins on Linux with default plugins installed

* Extra Jenkins plugins: Pipeline Utility Steps, HTTP Request Plugin, SSH Agent Plugin, CloudBees AWS Credentials Plugin, Performance

* Install aws-cli and JMeter on Jenkins

* Jenkins access to AWS

Test Strategies
---------------
### Metrics:
This is a list of metrics that are gathered during this experiment:

* Average response times (ART) for each transaction
* Min and Max response times
* Median
* Failure rate and errors/warnings in the logs
* Throughtput

These metrics are collected as part of Performance Report which are generated as build artifact

### Tools:
Tools used for these test cases is JMeter - https://jmeter.apache.org/ Utilized non-GUI JMeter.

For example, non-GUI commandline to generate report:

jmeter -Jjmeter.save.saveservice.output_format=xml -n -l jmeter_perf.jtl -t Folio-Test-Plans/mod-inventory-storage/instance-storage/instance-storageTestPlan.jmx -j jmeter_46_instance-storageTestPlan.jmx.log

Reports are generating in Jenkins using JMeter Performance plugin - https://wiki.jenkins.io/display/JENKINS/Performance+Plugin 
 
### SLA Goals:
* The average response time (AVG RT) for the JMeter captured transaction should not be more than 1000 milliseconds.
* The percent of CPU utilization on any module should not be more than 50%.
* JMeter tests running nightly in Jenkins pipeline as a job will fail if even a single test fails 


### Environment:
* Engine: PostgreSQL 9.6.8
* Entire Stack(environment) is created fresh from scratch everyday by populating dataset in database then running JMeter on top of it and once tests complete running, tear down the environment.
* JMeter scripts are running against ~3 million Harvard dataset

### Workflow used for most of the APIs:
* Create new data by doing POST http request and cleaning it by doing DELETE http request once test completes.


Quick start
-----------

* Import in Jenkins as standard pipeline project

* Pick a Jenkinsfile and adjust build parameters as needed

## Additional information

### Issue tracker

See project [FOLIO](https://issues.folio.org/browse/FOLIO)
at the [FOLIO issue tracker](https://dev.folio.org/guidelines/issue-tracker).

### Other documentation

Other [infrastructure projects](https://dev.folio.org/source-code/#other-projects) are described,
with further FOLIO Developer documentation at [dev.folio.org](https://dev.folio.org/)
