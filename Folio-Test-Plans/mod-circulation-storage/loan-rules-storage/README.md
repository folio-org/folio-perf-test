Loan-rules-storage load profile
----------------------------------------
* The average load for Loan-rules-storage API is between 0 - 200 concurrent users
* As we continue to increase the number of concurrent users from 100 to 1500 gradually like 100/5sec -> 350/5sec -> 600/5sec -> 850/5sec -> 1100/10sec -> 1350/10sec -> 1500/10sec, API breaks. 
100/5sec - means all 100 threads(users) starts in 5 seconds
1100/10sec - means all 1100 threads(users) starts in 10 seconds
* It takes more than a minute per endpoint to get a response back. It also depends on machine configuration and what time of the day requests are being triggered. During heavy network traffic, API will timeout or JMeter tests will fail or will not receive any HTTP response due to database locking.
