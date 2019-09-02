Patron notice policy load profile
-----------------------------------------------
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
* numberOfTemplates - number of templates for notification
