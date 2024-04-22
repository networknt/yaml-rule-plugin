When calling ServiceNow API from a consumer application, you can get the password grant_type token from the ServiceNow token endpoint and use it to call the API; however, it requires you have username, password, client_id, client_secret for each token retrieaval. To make the API to API invocation easier, you can still use your client_credentials token from Okta, for example, to make the call to the light-gateway. This plugin will be injected with the RequestTranformer to get the configuration from the snow.yml to get the password grant type token to replace the client_credentials token for the call to the ServiceNow API.

The jar file can be downloaded from maven central.

https://mvnrepository.com/artifact/com.networknt/snow-token
