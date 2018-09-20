Platforms load profile
---------------------------------
* The average load for platforms API is between 0 - 200 concurrent users
* As we continue to increase the number of concurrent users to 1500, API breaks. It takes more than 1.5 minutes per endpoint to get a response back. It also depends on machine configuration and what time of the day requests are being triggered. During heavy network traffic, API will timeout or JMeter tests will fail or will not receive any HTTP response due to database locking.