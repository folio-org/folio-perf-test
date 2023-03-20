Test search by identifier
---------------------------------
This is to test the performance of search by identifier value and search by identifier type and value. As of today (2019-02-1). the performance of those two searches are poor on perf environment. For a single user, search by identifier value takes about 2 minutes and search by identifier type and value takes over 30 seconds.

The test will try to get some sample identifiers from the testing FOLIO environment first. If cannot, it will fall back to use default identifier type and value. The size of the sample data and default identifiers values can be adjusted in the test under Test Plan's user defined varialbes.
* SampleSize - default to be 200
* DefaultIdType - default to be 8261054f-be78-422d-bd51-4ed9f33c3422 (ISBN)
* DefaultIdValue - default to 7620724099 (random id existing in Perf env)

The test environment and load profile can be adjusted in config.csv and load.csv files.
* values in config.csv
  * tenant
  * user
  * password
  * protocol
  * host
  * port
* values in load.csv
  * number of threads/users 
  * ramp-up time in seconds (to create above number of threads/users)
  * loop count (number of times to execute the test for each thread/user)
