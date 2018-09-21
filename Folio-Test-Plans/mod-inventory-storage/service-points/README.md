Service-points load profile
---------------------------------
* The average load for service-points API is between 0 - 200 concurrent users
* As we continue to increase the number of concurrent users from 100 to 1600 gradually like 400/5sec -> 800/5sec -> 1200/10sec -> 1600/10sec, API breaks. It takes more than 1.5 minutes per endpoint to get a response back. 
400/5sec - means each thread(user) starts every 5 seconds 
1200/10sec - means each thread(user) starts every 10 seconds
* It also depends on machine configuration and what time of the day requests are being triggered. During heavy network traffic, API will timeout or JMeter tests will fail or will not receive any HTTP response due to database locking.