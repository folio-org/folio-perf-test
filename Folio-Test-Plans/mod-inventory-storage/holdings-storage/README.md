Holdings-storage load profile
---------------------------------
* The average load for holdings-storage API is between 0 - 200 concurrent users
* As we continue to increase the number of concurrent users to 600, API is practically impossible to work. It takes more than 2 minutes for some endpoints such as PUT, DELETE and GET after DELETE HTTP requests to get some response back. It is also possible that database freezes or API's endpoints will not give any HTTP response. It also depends on machine configuration and what time of the day requests are being triggered. During heavy network traffic, API will timeout or JMeter tests will fail or will not receive any HTTP response due to database locking.
