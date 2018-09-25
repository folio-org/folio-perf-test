Loan-types load profile
---------------------------------
* The average load for loan-types API is between 0 - 100 concurrent users
* As we continue to increase the number of concurrent users from 100 to 250 gradually like 200/5sec -> 250/10sec, API will break. It takes more than a minute per endpoint to get some response back. Individually, DELETE and GET after delete HTTP requests consistently take on average 2 minutes to complete. During this time, CPU usage spikes close to 90%. 
200/5sec - means all 200 threads(users) starts in 5 seconds 
250/10sec - means all 250 threads(users) starts in 10 seconds
* It also depends on machine configuration and what time of the day requests are triggered. During heavy network traffic, API will timeout or JMeter tests will fail or will not receive any HTTP response due to database locking.