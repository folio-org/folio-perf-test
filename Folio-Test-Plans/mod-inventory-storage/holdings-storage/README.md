Holdings-storage load profile
---------------------------------
* The average load for holdings-storage API is between 0 - 200 concurrent users
* As we continue to increase the number of concurrent users from 100 to 600 gradually like 200/5sec -> 400/5sec -> 600/10sec, API is practically impossible to work. It takes more than 2 minutes for some endpoints such as PUT, DELETE and GET after DELETE HTTP requests to get some response back. It is also possible that database freezes or API's endpoints will not give any HTTP response. 
500/5sec - means all 500 threads(users) starts in 5 seconds 
600/10sec - means all 600 threads(users) starts in 10 seconds
* It also depends on machine configuration and what time of the day requests are being triggered. During heavy network traffic, API will timeout or JMeter tests will fail or will not receive any HTTP response due to database locking.
