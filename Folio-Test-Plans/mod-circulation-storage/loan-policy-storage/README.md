Loan-policy-storage load profile
----------------------------------------
* The average load for loan-policy-storage API is between 0 - 200 concurrent users
* As we continue to increase the number of concurrent users from 100 to 1300 gradually like 300/5sec -> 500/5sec -> 700/5sec -> 900/10sec -> 1100/10sec -> 1300/10sec, API breaks. 
100/5sec - means each thread(user) starts every 5 seconds 
500/10sec - means each thread(user) starts every 10 seconds
* It takes more than a minute per endpoint to get a response back. Individually, PUT, GET and DELETE HTTP endpoint to retrieve, Update and Delete a loan-policy item respectively takes most of the latency. It also depends on machine configuration and what time of the day requests are being triggered. During heavy network traffic, API will timeout or JMeter tests will fail or will not receive any HTTP response due to database locking.