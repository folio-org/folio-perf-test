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
Metrics
This is a list of metrics that are gathered during this experiment:

Average response times (ART) for each transaction
Min and Max response times
Failure rate and errors/warnings in the logs.

Top three metrics are collected using JMeter Summary reports

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
