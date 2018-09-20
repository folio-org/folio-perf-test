Instance-storage load profile
---------------------------------
* The average load for instance-storage API is between 0 - 200 concurrent users
* As we continue to increase number of concurrent users to 900, API will start failing. It will take more than 1.5 minutes until samples stop giving any http response or JMeter tests will fail due to timeout errors. During this time, CPU usage will spike close to 90%. It also depends on machine configuration and what time of the day requests are being triggered. During heavy network traffic, API will timeout or JMeter tests will fail or will receive no http response due to database locking. 
