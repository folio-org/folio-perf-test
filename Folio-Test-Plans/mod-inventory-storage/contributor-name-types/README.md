Contributor-name-types load profile
---------------------------------
* Normal load for classification-types api is between 0 - 200 concurrent users
* As we continue to increase number of concurrent users to 1300, API is practically impossible to work with because on an average it takes more than 2 minutes per endpoint to get some response back. It also depends on what time of the day requests are being triggered, on heavy network traffic, APIs will timeout or JMeter tests will fail or will receive no http response due to database lock. 