This module contains two rule action implementations to change the request or response body from other encoding to UTF-8 in the request transform interceptor based on the rule engine.

The encoding to UTF-8 is done in the interceptor while parsing the stream into a string. However, if the body type is XML and it has encoding="xxx" as the declaration, we need to change it to encoding="UTF-8" to make sure that the body is parsed correctly.

### Download Jar

The plugin jar file can be downloaded at

https://mvnrepository.com/artifact/com.networknt/body-encoder

### Configuration:

In the rules.yml file, add the following rules if you want to transform both request body and response body. Choose only one if only request or response body needs to be transformed.

```
body-encoder-request:
  ruleId: body-encoder-request
  host: lightapi.net
  ruleType: request-transform
  visibility: public
  description: Transform the request body to UTF-8 encoding and update the encoding delaration to UTF-8 if it exists.
  conditions:
    - conditionId: path-encoder
      propertyPath: requestPath
      operatorCode: EQ
      joinCode: AND
      index: 1
      conditionValues:
        - conditionValueId: path
          conditionValue: /path/to/encode
  actions:
    - actionId: encoder-request-transform
      actionClassName: com.networknt.rule.encoder.RequestBodyUtf8EncodingTransformAction

body-encoder-response:
  ruleId: body-encoder-response
  host: lightapi.net
  ruleType: response-transform
  visibility: public
  description: Transform the response body to UTF-8 encoding and update the encoding delaration to UTF-8 if it exists.
  conditions:
    - conditionId: path-encoder
      propertyPath: requestPath
      operatorCode: EQ
      joinCode: AND
      index: 1
      conditionValues:
        - conditionValueId: path
          conditionValue: /path/to/encode
  actions:
    - actionId: encoder-response-transform
      actionClassName: com.networknt.rule.encoder.ResponseBodyUtf8EncodingTransformAction

```

This rule will be triggered when the request path matches the condition and the action class will be invoked to encode the request body or response body.

In the values.yml file, we need to do that following changes to overwrite default bodyEncoder. Also, you need to make sure that RequestTransformerInterceptor and RequestBodyInterceptor are configured in the service.yml section.



```
# service.yml
service.singletons:
  .
  .
  .
  - com.networknt.handler.RequestInterceptor:
      - com.networknt.reqtrans.RequestTransformerInterceptor
      - com.networknt.body.RequestBodyInterceptor
  - com.networknt.handler.ResponseInterceptor:
      - com.networknt.restrans.ResponseTransformerInterceptor
      - com.networknt.body.ResponseBodyInterceptor
```


```
# body.yml
body.cacheRequestBody: true
body.cacheResponseBody: true
```

Update the body handler to allow the request body and response body to be cached so that they can be populated into the audit log.

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
  - requestInterceptor
  - responseInterceptor
  .
  .
  .
```

You need to update the handler.yml section to add the interceptor handlers and put it into the default chain before proxy and router handler.


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
