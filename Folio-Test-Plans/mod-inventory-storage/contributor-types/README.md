Contributor-types load profile
---------------------------------
* Normal load for classification-types api is between 0 - 200 concurrent users
* As we continue to increase number of concurrent users to 700, API times out and sometimes database connection doesn’t close so henceforth no response is returned for any HTTP request. Database kind of just locks itself or freezes.