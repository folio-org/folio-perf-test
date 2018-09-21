Instance-format load profile
---------------------------------
* The average load for instance-format API is between 0 - 200 concurrent users
* As we continue to increase number of concurrent users from 100 to 1200 gradually like 200/5sec -> 400/5sec -> 600/5sec -> 800/5sec -> 1000/10sec -> 1200/10sec, API will be practically impossible to work. It takes more than a minute per endpoint to get some response back. DELETE http request can take around 1.5 to 2 minutes to complete. Network traffic will spike heavily, subsequently CPU usage will increase close to 90%. 
200/5sec - means each thread(user) starts every 5 seconds 
1000/10sec - means each thread(user) starts every 10 seconds
* It also depends on machine configuration and what time of the day requests are being triggered. During heavy network traffic, API will timeout or JMeter tests will fail or will receive no http response due to database locking. 
