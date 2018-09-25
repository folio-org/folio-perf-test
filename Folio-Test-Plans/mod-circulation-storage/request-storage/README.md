Request-storage load profile
-----------------------------------------------
* The average load for Request-storage API is between 0-200 concurrent users
* As we continue to increase the number of concurrent users from 100 to 1200 gradually like 100/5sec -> 200/5sec -> 400/5sec -> 600/10sec -> 800/10sec -> 1000/10 sec -> 1200/10 sec, API breaks. 
100/5sec - means all 100 threads(users) starts in 5 seconds 
6000/10sec - means all 6000 threads(users) starts in 10 seconds
* Just before the API breaks, it takes more than a minute for some endpoints such as GET updated request item and PUT update a request item to get a response back. It also depends on machine configuration and what time of the day requests are being triggered. During heavy network traffic, API will timeout or JMeter tests will fail or will not receive any HTTP response due to database locking.