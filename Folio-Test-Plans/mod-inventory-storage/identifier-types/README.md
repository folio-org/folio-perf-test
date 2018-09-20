Identifier-types load profile
---------------------------------
* The average load for identifier-types API is between 0 - 200 concurrent users
* As we continue to increase the number of concurrent users to 900, API breaks. It takes more than a minute for some endpoints such as PUT and DELETE to get some response back. DELETE HTTP request can take on an average more than 1.5 minutes. It also depends on machine configuration and what time of the day requests are being triggered. During heavy network traffic, API will timeout or JMeter tests will fail or will receive no HTTP response due to database lock. 
