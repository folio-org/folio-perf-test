Instance-type load profile
---------------------------------
* The average load for instance-type API is between 0 - 200 concurrent users
* As we continue to increase number of concurrent users from 100 to 1500 gradually like 300/5sec -> 600/5sec -> 900/5sec -> 1200/10sec -> 1500/10sec, API will start to break. It will take more than a minute until samples will stop giving any http response or JMeter tests will fail due to timeout errors. During this time, CPU usage will spike close to 90%. It also depends on what time of the day requests are being triggered. 
600/5sec - means each thread(user) starts every 5 seconds 
1200/10sec - means each thread(user) starts every 10 seconds
* During heavy network traffic, API will timeout or JMeter tests will fail or will receive no http response due to database locking. 
