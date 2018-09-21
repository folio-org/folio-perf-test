Requests load profile
-----------------------------------------------
* The average load for requests API is between 0 - 20 concurrent users
* As we continue to increase the number of concurrent users from 1 to 12 gradually like 1/5sec -> 4/5sec -> 8/5sec -> 12/10sec, API breaks.  
* 1/5sec - means each thread(user) starts every 5 seconds 
* 12/10sec - means each thread(user) starts every 10 seconds
* Just before the API breaks, it takes more than 2 minutes for some endpoints such as GET all circulation requests and GET retrieve request item with given requestId to get a response back.
* It also depends on machine configuration and what time of the day requests are being triggered. During heavy network traffic, API will timeout or JMeter tests will fail or will not receive any HTTP response due to database locking.