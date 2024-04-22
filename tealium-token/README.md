Tealium is an external API provided and it has a customized flow to get the JWT token for the authorization. The request and response are not standard. The caller is a Kafka sink connector and this plugin is deployed to the light-gateway to be responsible to get the token, cache the token and renew the token if necessary.

The jar file can be downloaded from maven central.

https://mvnrepository.com/artifact/com.networknt/tealium-token
