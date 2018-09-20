Contributor-types load profile
---------------------------------
* The average load for contributor-types API is between 0 - 200 concurrent users
* As we continue to increase the number of concurrent users to 700, API times out and sometimes database connection doesn’t close, so henceforth no response is returned for any HTTP request. Database kind of locks itself or freezes.