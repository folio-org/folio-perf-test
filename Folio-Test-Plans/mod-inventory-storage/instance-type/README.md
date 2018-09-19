Instance-type load profile
---------------------------------
* Normal load for instance-type API is between 0 - 200 concurrent users
* As we continue to increase number of concurrent users to 1500, API will start to break. It will take more than a minute until samples will stop giving any http response or JMeter tests will fail due to timeout errors. During this time, CPU usage will spike close to 90%. It also depends on what time of the day requests are being triggered. During heavy network traffic, API will timeout or JMeter tests will fail or will receive no http response due to database locking. 
