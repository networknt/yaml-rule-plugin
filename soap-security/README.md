This module contains a rule action implementation to inject security section into a SOAP XML request body in the request transform interceptor based on the rule engine. 

One of our customers is accessing a Cannex Soap API from the corporate network with light-gateway via the generic [ExternalServiceHandler](/concern/external-handler/). For all the consumers, they don't need to set the security header in the SOAP request; the light-gateway intercepts the request body and adds security header with the correct credentials to access the Cannex. This is very similar to the above REST to SOAP request transformer; however, it is a little bit complicated as we are accessing an external service instead of internal services. 

For detailed configuration, please review this [PR](https://github.com/networknt/light-gateway/pull/69)

Also, the following is a video walk through. 

https://www.youtube.com/watch?v=MTA6Pf1TjaU

