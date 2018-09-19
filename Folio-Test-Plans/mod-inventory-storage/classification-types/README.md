Classification-types load profile
---------------------------------
* Normal load for classification-types api is between 0 - 200 concurrent users
* As we continue to increase number of concurrent users to 1300, the API will starts to break and will be practically impossible to work with because on average it takes around 2 minutes per endpoint. Performance outcome of the API also depends on what time of the day requests are being triggered, during heavy network traffic, APIs will timeout or JMeter tests will fail or will receive no http response due to database lock. 