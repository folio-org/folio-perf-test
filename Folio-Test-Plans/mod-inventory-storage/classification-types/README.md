Classification-types load profile
---------------------------------
* The average load for classification-types API is between 0 - 200 concurrent users
* As we continue to increase the number of concurrent users from 100 to 1300  gradually like 300/5sec -> 500/5sec -> 700/5sec -> 900/10sec -> 1100/10sec -> 1300/10sec, API is practically impossible to work with because on an average it takes more than 2 minutes for some endpoints such as PUT and DELETE HTTP requests to get some response back.
300/5sec - means all 300 threads(users) starts in 5 seconds 
900/10sec - means all 900 threads(users) starts in 10 seconds
* It also depends on machine configuration and what time of the day requests are triggered. During heavy network traffic, API will timeout or JMeter tests will fail or will not receive any HTTP response due to database locking.
