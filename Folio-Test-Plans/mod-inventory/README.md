Mod-inventory load profile
-----------------------------------------------
* The average load for mod-inventory's item and instance API is between 0 - 25 concurrent users
* As we continue to increase the number of concurrent users from 1 to 25 gradually like 1/5sec -> 5/5sec -> 10/5sec -> 15/5sec -> 20/10sec -> 25/10sec, API breaks.  
10/5sec - means all 10 threads(users) starts in 5 seconds 
25/10sec - means all 25 threads(users) starts in 10 seconds
* Just before the API breaks, it takes around 2 minutes for some endpoints such as POST mod-inventory/item, PUT mod-inventory/item and GET to verify item does not exists to get a response back. It also depends on machine configuration and what time of the day requests are being triggered. During heavy network traffic, API will timeout or JMeter tests will fail or will not receive any HTTP response due to database locking.