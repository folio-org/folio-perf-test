Contributor-types load profile
---------------------------------
* The average load for contributor-types API is between 0 - 200 concurrent users
* As we continue to increase the number of concurrent users from 100 to 700 gradually like 200/5sec -> 400/5sec -> 600/10sec -> 700/10sec, API times out and sometimes database connection doesn’t close, so henceforth no response is returned for any HTTP request. Database kind of locks itself or freezes.
200/5sec - means each thread(user) starts every 5 seconds 
700/10sec - means each thread(user) starts every 10 seconds
