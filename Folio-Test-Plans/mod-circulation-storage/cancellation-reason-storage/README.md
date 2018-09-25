Cancellation-reason-storage load profile
----------------------------------------
* The average load for cancellation-reason-storage API is between 0 - 200 concurrent users
* As we continue to increase the number of concurrent users from 100 to 900 gradually like 100/5sec -> 300/5sec -> 500/5sec -> 700/10sec -> 900/10sec, API breaks. 
100/5sec - means all 100 threads(users) starts in 5 seconds 
700/10sec - means all 700 threads(users) starts in 10 seconds
* It takes more than 1.5 minutes per endpoint to get a response back. Individually, GET updated cancellation-reasons, DELETE cancellation-reason and GET again to verify cancellation reason item does not exists, takes most of the latency. It also depends on machine configuration and what time of the day requests are being triggered. During heavy network traffic, API will timeout or JMeter tests will fail or will not receive any HTTP response due to database locking.
