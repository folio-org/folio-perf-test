This JMeter script allows to create required number of invoice and invoice line records via invoice API 

---

### Environment specific options 
All configuration settings live in **"User Defined Variables"** section. In order to run the script against specific environment following variables should be populated with the proper values:

`host, protocol, port, tenant, username, password`

### Records generator settings
_**invoicesToBeCreated**_ - amount of invoice records to be generated per single thread (default value `50`)

_**invoiceLinesPerInvoice**_ - amount of invoice line records to be generated per invoice (default value `1`)

_**numOfParallelThreads**_ - number of parallel threads. Total amount of generated invoices will be calculated by formula `total = invoicesToBeCreated * numOfParallelThreads` (default value `1`)

_**invoiceLineDescription**_ - Description used just to highlight invoices created via Jmeter script (default value `perftest`)
