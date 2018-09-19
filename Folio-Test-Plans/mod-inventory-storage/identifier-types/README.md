Identifier-types load profile
---------------------------------
* Normal load for identifier-types API is between 0 - 200 concurrent users
* As we continue to increase number of concurrent users to 900, API is practically impossible to work. It takes more than a minute per endpoint to get some response back. DELETE http request can take on an average more than 1.5 minutes. It also depends on what time of the day requests are being triggered. During heavy network traffic, API will timeout or JMeter tests will fail or will receive no http response due to database lock. 