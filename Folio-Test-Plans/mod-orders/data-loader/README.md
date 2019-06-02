This JMeter script allows to create the required number of order/order-line records in a multi-threaded environment
according to normal flow: Create Pending Order -> Create Order-Line for this Order -> Update Order to corresponding status. 
Script execution is configured by two csv files located in the script directory. The first, *config.csv*, has the format:

_**tenant, user, password, protocol, host, port, num_of_threads, num_of_records_per_thread**_

and contains information about the host, the number of threads (_num_of_threads_) and the number of records (_num_of_records_per_thread_) 
per one thread. It should be noted that in the case of a multi-threaded recording (_num_of_threads_>1), the total number 
of records is calculated by the formula: _total_num_of_records_ = _num_of_threads_ * _num_of_records_per_thread_. 

The second configuration file, percentage.csv, has the format:

_**open, pending, closed, physical, electronic, p / e_mix**_

and contains information about the percentage distribution of records created. 
User must control that the percentage amounts for all types of orders and all types of order-lines should not exceed 100%. 
The configurable percentage for orders and order-lines satisfies the following features:
* Pending, Open, Closed orders are distributed according to configured percentage amounts values.
* Each order-line corresponds to only one order.
* Electronic, Physical, P/E Mix order-lines are distributed within a given ratio for a particular type of order, 
as well as in general, regardless of the type of order.

The script can be run both from the JMeter GUI and the console by command: 

jmeter -Jjmeter.save.saveservice.output_format=xml -n -l jmeter_perf.jtl -t Folio-Test-Plans/mod-orders/data-loader/Orders-DataLoader.jmx -j jmeter_Orders_DataLoader.jmx.log.

In this case, the console output will contain information about the initial and 
final number of order and order-line records.
