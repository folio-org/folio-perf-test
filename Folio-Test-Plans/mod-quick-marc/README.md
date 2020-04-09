Mod-quick-marc load profile
-----------------------------------------------
* The average load for mod-quick-marc's is between 0 - 500 concurrent users
* As we continue to increase the number of concurrent users from 1 to 500 gradually during 20 sec like 1/1sec -> 5/1sec -> 10/1sec -> 25/1sec -> 500/20sec, API breaks.  
25/1sec - means all 25 threads(users) starts in 1 seconds 
500/20sec - means all 500 threads(users) starts in 20 seconds
