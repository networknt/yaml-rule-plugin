This module contains a rule action implementation to sanitize request body in the request transform interceptor based on the rule engine. 

For users who use http-sidecar or light-gateway, you cannot use the middleware handler [SanitizerHandler](https://doc.networknt.com/concern/sanitizer/). The only option is to use this plugin to transform the request body with the sanitizer.yml configuration.

### Download Jar

The plugin jar file can be downloaded at 

https://mvnrepository.com/artifact/com.networknt/body-sanitizer

### Configuration: 

In the rules.yml file, add the following rule.

```
body-sanitizer-request:
  ruleId: body-sanitizer-request
  host: lightapi.net
  ruleType: request-transform
  visibility: public
  description: Transform the request body based on the sanitizer.yml to encode the request body for cross-site scripting.
  conditions:
    - conditionId: path-sanitizer
      propertyPath: requestPath
      operatorCode: EQ
      joinCode: AND
      index: 1
      conditionValues:
        - conditionValueId: path
          conditionValue: /path/to/sanitize
  actions:
    - actionId: sanitizer-request-transform
      actionClassName: com.networknt.rule.sanitizer.BodySanitizerTransformAction

```

This rule will be triggered when the request path matches the condition and the action class will be invoked to encode the request body. 

In the values.yml file, we need to do that following changes to overwrite default bodyEncoder. Also, you need to make sure that RequestTransformerInterceptor and RequestBodyInterceptor are configured in the service.yml section.



```
# sanitizer.yml
sanitizer.bodyEncoder: javascript-source

# service.yml
service.singletons:
  .
  .
  .
  - com.networknt.handler.RequestInterceptor:
      - com.networknt.reqtrans.RequestTransformerInterceptor
      - com.networknt.body.RequestBodyInterceptor


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
  - com.networknt.sanitizer.SanitizerHandler@sanitizer
  - com.networknt.handler.ResponseInterceptorInjectionHandler@responseInterceptor
  - com.networknt.handler.RequestInterceptorInjectionHandler@requestInterceptor
  .
  .
  .

handler.chains.default:
  .
  .
  .
  - sanitizer
  - requestInterceptor
  - responseInterceptor
  .
  .
  .
```

When you are sanitizing the body, you might sanitize the request headers at the same time. You need to update the handler.yml section to add the SanitizerHandler and put it into the default chain before proxy and router handler. 


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
request-transformer.appliedPathPrefixes: ["/v1/pets","/v1/flowers"]

```

Add the request path prefix to the request-transformer.appliedPathPrefixes list. 

rule-loader.yml

```
rule-loader.endpointRules: {"/v1/pets@post":{"request-transform":[{"ruleId":"body-sanitizer-request"}]}}

```

Add the endpoint /v1/pets@post and the ruleId to the mapping.

### Tutorial

The following is a video walk through; however, some the configuration might be changed already. Please following the configuration above if you want try it out. 

[video tutorial](https://youtu.be/Tkg9Q-XVT4U)



