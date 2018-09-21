Material-types load profile
---------------------------------
* The average load for material-types API is between 0 - 100 concurrent users
* As we continue to increase the number of concurrent users from 100 to 250 gradually like 150/5sec -> 200/5sec -> 250/10sec, API breaks. It takes more than 2 minutes for some endpoints such as DELETE and GET( requests to verify material type does not exist) endpoints to get a response back. 
100/5sec - means each thread(user) starts every 5 seconds 
450/10sec - means each thread(user) starts every 10 seconds
* It also depends on machine configuration and what time of the day requests are being triggered. During heavy network traffic, API will timeout or JMeter tests will fail or will not receive any HTTP response due to database locking.
* There is a JIRA open to fix this - https://issues.folio.org/browse/MODINVSTOR-124