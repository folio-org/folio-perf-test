Loan-storage load profile
-----------------------------------------------
* The average load for Loan-storage API is between 0 - 200 concurrent users
* As we continue to increase the number of concurrent users from 100 to 450 gradually like 100/5sec -> 200/5sec -> 300/5sec -> 400/10sec -> 450/10sec, API breaks.  
100/5sec - means each thread(user) starts every 5 seconds 
450/10sec - means each thread(user) starts every 10 seconds
* Just before the API breaks, it takes more than a minute for some endpoints such as GET update loan item and GET list of all loan items to get a response back. It also depends on machine configuration and what time of the day requests are being triggered. During heavy network traffic, API will timeout or JMeter tests will fail or will not receive any HTTP response due to database locking.