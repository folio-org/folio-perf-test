Locations load profile
---------------------------------
* The average load for locations API is between 0 - 100 concurrent users
* As we continue to increase the number of concurrent users from 100 to 300 gradually like 200/5sec -> 250/5sec -> 300/10sec, API breaks. It takes more than 2 minutes for some endpoints such as DELETE and GET( requests to verify location does not exist) endpoints to get a response back. It is practically impossible to work before it breaks. 
200/5sec - means all 200 threads(users) starts in 5 seconds 
300/10sec - means all 300 threads(users) starts in 10 seconds
* It also depends on machine configuration and what time of the day requests are being triggered. During heavy network traffic, API will timeout or JMeter tests will fail or will not receive any HTTP response due to database locking.