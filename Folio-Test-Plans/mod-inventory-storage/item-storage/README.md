Item-storage load profile
---------------------------------
* The average load for item-storage API is between 0 - 200 concurrent users
* As we continue to increase the number of concurrent users from 1 to 45 gradually like 15/5sec -> 30/5sec -> 45/10sec, API will break. It takes more than a minute per endpoint to get some response back. During this time, CPU usage spikes close to 100%. 
15/5sec - means all 15 threads(users) starts in 5 seconds 
45/10sec - means all 45 threads(users) starts in 10 seconds
* It also depends on machine configuration and what time of the day requests are being triggered. During heavy network traffic, API will timeout or JMeter tests will fail or will receive no HTTP response due to database locking.