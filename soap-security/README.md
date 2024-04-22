This module contains a rule action implementation to inject security section into a SOAP XML request body in the request transform interceptor based on the rule engine.

One of our customers is accessing a Cannex Soap API from the corporate network with light-gateway via the generic [ExternalServiceHandler](/concern/external-handler/). For all the consumers, they don't need to set the security header in the SOAP request; the light-gateway intercepts the request body and adds security header with the correct credentials to access the Cannex. This is very similar to the above REST to SOAP request transformer; however, it is a little bit complicated as we are accessing an external service instead of internal services.

### Download Jar

The plugin jar file can be downloaded at this location for the snapshot version.

https://oss.sonatype.org/service/local/repositories/snapshots/content/com/networknt/soap-security/2.1.3-SNAPSHOT/soap-security-2.1.3-20221031.175118-1.jar

The released jar will be added later but the codebase should be the same.


### Configuration:

In the rules.yml file, add the following rule.

```
soap-security-request:
  ruleId: soap-security-request
  host: lightapi.net
  ruleType: request-transform
  visibility: public
  description: Transform the request body to add the security section into the header for soap API call for external service.
  conditions:
    - conditionId: path-soap
      propertyPath: requestPath
      operatorCode: EQ
      joinCode: AND
      index: 1
      conditionValues:
        - conditionValueId: path
          conditionValue: /devext/CANX/AntcMultiService
  actions:
    - actionId: soap-request-transform
      actionClassName: com.networknt.soap.SoapSecurityTransformAction

```

This rule will be triggered when the request path matches the condition and the action class will be invoked to add the securit header in the SOAP reqeust body.

In the values.yml file, we need to do that following changes to overwrite several config properties.

cannex.yml

```
# cannex.yml
cannex.username: SLUATWS

```
You can also add the cannex.password: real-password into the values.yml file; however, I have added it into an environment variable in .profile file.

```
export CANNEX_PASSWORD=real-password
```

external-service.yml
```
externalService.pathHostMappings:
  - /devext/CANX/AntcMultiService https://wwwdev.cannex.com
```

Add the request path and the external host mapping to the externalService.pathHostMappings


body.yml

```
# body.yml
body.cacheRequestBody: true
body.cacheResponseBody: true

```

Update the body handler to allow the request body and repsonse body to be cached so that they can be populated into the audit log.


handler.yml

```
handler.handlers:
  .
  .
  .
  - com.networknt.handler.ResponseInterceptorInjectionHandler@responseInterceptor
  - com.networknt.handler.RequestInterceptorInjectionHandler@requestInterceptor
  .
  .
  .

handler.chains.default:
  .
  .
  .
  - header
  - requestInterceptor
  - responseInterceptor
  .
  .
  .
```

service.yml

```
  - com.networknt.handler.RequestInterceptor:
      - com.networknt.reqtrans.RequestTransformerInterceptor
      - com.networknt.body.RequestBodyInterceptor

```

Above add the RequestTransformerInterceptor and RequestBodyInterceptor to the default chain via requestInterceptor defined in the handler.yml file.

Please note that RequestBodyInterceptor must be the last interceptor in the interceptor list.

request-transformer.yml

```
# request-transformer.yml
request-transformer.appliedPathPrefixes: ["/pets","/v1/flowers","/devext/CANX/AntcMultiService"]

```

Add the request path prefix to the request-transformer.appliedPathPrefixes list.

rule-loader.yml

```
rule-loader.endpointRules: {"/v1/pets@get":{"request-transform":[{"ruleId":"petstore-request-header-replace"}],"response-transform":[{"ruleId":"petstore-response-header-replace"}]},"/v1/notifications@get":{"response-transform":[{"ruleId":"petstore-notifications-transformer"}]},"/pets@get":{"request-transform":[{"ruleId":"petstore-request-path"}]},"/v1/flowers@post":{"request-transform":[{"ruleId":"petstore-flower-request"}],"response-transform":[{"ruleId":"petstore-flower-response"}]},"/devext/CANX/AntcMultiService@post":{"request-transform":[{"ruleId":"soap-security-request"}]}}

```

Add the endpoint /devext/CANX/AntcMultiService@post and the ruleId to the mapping.

### Tutorial


The following is a video walk through; however, some the configuration might be changed already. Please following the configuration above if you want try it out.

https://www.youtube.com/watch?v=MTA6Pf1TjaU
