Material-types load profile
---------------------------------
* The average load for material-types API is between 0 - 100 concurrent users
* As we continue to increase the number of concurrent users to 250, API breaks. It takes more than 2 minutes for some endpoints such as DELETE and GET( requests to verify material type does not exist) endpoints to get a response back. It also depends on machine configuration and what time of the day requests are being triggered. During heavy network traffic, API will timeout or JMeter tests will fail or will not receive any HTTP response due to database locking.
* There is a JIRA open to fix this - https://issues.folio.org/browse/MODINVSTOR-124