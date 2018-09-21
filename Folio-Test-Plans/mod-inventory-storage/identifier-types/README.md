Identifier-types load profile
---------------------------------
* The average load for identifier-types API is between 0 - 200 concurrent users
* As we continue to increase the number of concurrent users from 100 to 900 gradually like 300/5sec -> 500/5sec -> 700/10sec -> 900/10sec, API breaks. It takes more than a minute for some endpoints such as PUT and DELETE to get some response back. DELETE HTTP request can take on an average more than 1.5 minutes.
300/5sec - means each thread(user) starts every 5 seconds 
900/10sec - means each thread(user) starts every 10 seconds
* It also depends on machine configuration and what time of the day requests are being triggered. During heavy network traffic, API will timeout or JMeter tests will fail or will receive no HTTP response due to database lock. 
