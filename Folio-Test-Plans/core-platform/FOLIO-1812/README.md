Test checkout-by-barcode
---------------------------------
This is to test the performance of checkout by barcode.

The test will query FOLIO to get user barcode, service point id, and some item barcodes. The number of item barcodes will depend on the number of threads to use, and it assumes that each thread will run the test just once.

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
