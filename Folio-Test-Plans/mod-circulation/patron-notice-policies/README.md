Patron notice policy load profile
-----------------------------------------------
Performance testing - time-based patron loan notices (one item)

CSV files:
1.`config.csv`
Description:
* The file contains the configuration of the test server and its credentials
Parameters:
* tenant : tenant name
* user : user name
* password : user password
* protocol : server protocol
* host : test server host
* port : test server port

2.`expectedResult.csv`
Description:
* The file contains expected test result
Parameters:
* expectedNumberOfEmails : number of emails processed
* emailStatus : status of emails to be checked
* delayTime : time waiting for the test result (time in milliseconds)

3.`noticeSchedulerConfiguration.csv`
Description:
* Configuration notification schedule for the mod-configuration
Parameters:
* module : module name for schedule notifications
* configName : config name
* noticesLimit : schedule notification limit values

4.`noticePolicyConfig.csv`
Description:
* The configuration file for creating notifications
Parameters:
* numberOfTemplates : number of templates for notification
* numberOfCheckout : number of checkout for creation schedule notifications
