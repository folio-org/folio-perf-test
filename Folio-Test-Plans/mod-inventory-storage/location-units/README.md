Location-units load profile
---------------------------------
* The average load for location-units API is between 0 - 100 concurrent users
* As we continue to increase the number of concurrent users from 100 to 1000 gradually like 200/5sec -> 400/5sec -> 600/5sec -> 800/10sec -> 1000/10sec, API breaks. It takes more than 1.5 minutes for most of the endpoints to get some response back. Individually, DELETE HTTP request can take on an average 2 minutes or more to get the HTTP response back. 
200/5sec - means all 200 threads(users) starts in 5 seconds 
800/10sec - means all 800 threads(users) starts in 10 seconds
* It also depends on machine configuration and what time of the day requests are being triggered. During heavy network traffic, API will timeout or JMeter tests will fail or will not receive any HTTP response due to database locking.