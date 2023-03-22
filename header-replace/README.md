This module contains a yaml rule action implementation to replace one request header value with another request header value in the request transformer and a yaml rule action implementation to replace one response header value with another response header value in the response transformer. 

The file jar file for the module is about 6.5KB and it can be dropped into the rulesjar folder in the light-gateway or http-sidecar for RequestTransformerInterceptor and ResponseTransformerInterceptor to invoke via YAML RuleEngine. The RuleEngine rules and values.yml config file can be deployed to the light-portal if you have license. Otherwise, you can put them into the config folder. 

The jar file can be built locally or you can download it from the maven central for the released version or sonatype for the snapshot version. 

snapshot: https://oss.sonatype.org/service/local/repositories/snapshots/content/com/networknt/header-replace/2.1.3-SNAPSHOT/header-replace-2.1.3-20221025.034617-2.jar

release: 


### HeaderReplaceRequestTransformAction

To use this action plugin, add the following section into the rules.yml

Note that the activeValues define the sourceHeader as Flink-Token, targetHeader as Authorization and removeSourceHeader as true. This will allow the targetHeader to be updated with the value from the sourceHeader, and the sourceHeader will be removed after the update.

```
petstore-request-header-replace:
  ruleId: petstore-request-header-replace
  host: lightapi.net
  ruleType: request-transform
  visibility: public
  description: Transform the request to replace one header with the other header.
  conditions:
    - conditionId: path-pets
      propertyPath: requestPath
      operatorCode: EQ
      joinCode: AND
      index: 1
      conditionValues:
        - conditionValueId: path
          conditionValue: /v1/pets
  actions:
    - actionId: header-transform
      actionClassName: com.networknt.rule.HeaderReplaceRequestTransformAction
      actionValues:
        - actionValueId: sourceHeader
          value: Flink-Token
        - actionValueId: targetHeader
          value: Authorization
        - actionValueId: removeSourceHeader
          value: true

```
There is another use case to replace the targetHeader with a targetValue passed in from the rule action value. Here is the section of the actionValues for this use case. It will update the Authorization header to newToken.

When the targetValue is sensitive, you should encrypt it with the light-4j encrypt utility or your customized utility. The encrypted value will be decrypted in the plugin.

```
  actions:
    - actionId: header-transform
      actionClassName: com.networknt.rule.HeaderReplaceRequestTransformAction
      actionValues:
        - actionValueId: targetHeader
          value: Authorization
        - actionValueId: targetValue
          value: newToken
```

The above rule will be triggered when the request path is /v1/pets, which is an endpoint for the petstore API in the backend. Once the rule is triggered, it passes sourceHeader name, targetHeader name and removeSourceHeader flag through actionValues parameter to the plugin. The plugin will return requestHeaders object that contains a remove list of string for headers to be removed and a map of header name to header value in the update map. 

Here is the piece of code that in the plugin. 

```
        resultMap.put(RuleConstants.RESULT, true);
        String sourceHeader = null;
        String targetHeader = null;
        Boolean removeSourceHeader = null;
        for(RuleActionValue value: actionValues) {
            if("sourceHeader".equals(value.getActionValueId())) {
                sourceHeader = value.getValue();
                continue;
            }
            if("targetHeader".equals(value.getActionValueId())) {
                targetHeader = value.getValue();
                continue;
            }
            if("removeSourceHeader".equals(value.getActionValueId())) {
                removeSourceHeader = "true".equalsIgnoreCase(value.getValue()) ? Boolean.TRUE : Boolean.FALSE;
            }
        }
        if(logger.isDebugEnabled()) logger.debug("source request header = " + sourceHeader + " target request header = " + targetHeader + " removeSourceHeader = " + removeSourceHeader);
        HeaderMap headerMap = (HeaderMap)objMap.get("requestHeaders");
        String sourceValue = null;
        HeaderValues sourceObject = headerMap.get(sourceHeader);
        if(sourceObject != null) sourceValue = sourceObject.getFirst();
        if(logger.isDebugEnabled()) logger.debug("source request header = " + sourceHeader + " value = " + sourceValue);
        if(sourceValue != null) {
            Map<String, Object> requestHeaders = new HashMap<>();
            if(Boolean.TRUE.equals(removeSourceHeader)) {
                List<String> removeList = new ArrayList<>();
                removeList.add(sourceHeader);
                requestHeaders.put("remove", removeList);
            }
            Map<String, Object> updateMap = new HashMap<>();
            updateMap.put(targetHeader, sourceValue);
            requestHeaders.put("update", updateMap);
            if(logger.isDebugEnabled()) logger.debug("final requestHeaders = " + requestHeaders);
            resultMap.put("requestHeaders", requestHeaders);
        }

```

Here is the section in the RequestTransformerInterceptor. 

```
                                    case "requestHeaders":
                                        // if requestHeaders object is null, ignore it.
                                        Map<String, Object> requestHeaders = (Map)result.get("requestHeaders");
                                        if(requestHeaders != null) {
                                            // manipulate the request headers.
                                            List<String> removeList = (List)requestHeaders.get("remove");
                                            if(removeList != null) {
                                                removeList.forEach(s -> exchange.getRequestHeaders().remove(s));
                                            }
                                            Map<String, Object> updateMap = (Map)requestHeaders.get("update");
                                            if(updateMap != null) {
                                                updateMap.forEach((k, v) -> exchange.getRequestHeaders().put(new HttpString(k), (String)v));
                                            }
                                        }
                                        break;

```

The passing object and the logic are the same like the header handler in the light-4j header module. 

For values.yml config file, we need to make the following changes. 

```
# rule-loader.yml
rule-loader.endpointRules: {"/v1/pets@get":{"request-transform":[{"ruleId":"petstore-request-header-replace"}],"response-transform":[{"ruleId":"petstore-response-header-replace"}]},"/v1/notifications@get":{"response-transform":[{"ruleId":"petstore-notifications-transformer"}]},"/pets@get":{"request-transform":[{"ruleId":"petstore-request-path"}]},"/v1/flowers@post":{"request-transform":[{"ruleId":"petstore-flower-request"}],"response-transform":[{"ruleId":"petstore-flower-response"}]},"/devext/CANX/AntcMultiService@post":{"request-transform":[{"ruleId":"soap-security-request"}]}}

# request-transformer.yml
request-transformer.appliedPathPrefixes: ["/v1/pets","/pets","/v1/flowers","/devext/CANX/AntcMultiService"]
```

You can see that we added an endpoint /v1/pets@get in the endpointRules with a request-transform and a response-transform ruleId. Also, add /v1/pets to the request-transformer.appliedPathPrefixes list. 

Here is the link to the video walkthrough. 

https://www.youtube.com/watch?v=-mpzIOL41ZM


### HeaderReplaceResponseTransformAction

To use this action plugin, add the following section into the rules.yml

Note that the activeValues define the sourceHeader as X-Test-1, targetHeader as My-Header. This will allow the targetHeader to be updated with the value from the sourceHeader, and the sourceHeader will not be removed after the update.

```
petstore-response-header-replace:
  ruleId: petstore-response-header-replace
  host: lightapi.net
  ruleType: response-transform
  visibility: public
  description: Transform the response to replace one header with the other header.
  conditions:
    - conditionId: path-pets
      propertyPath: requestPath
      operatorCode: EQ
      joinCode: AND
      index: 1
      conditionValues:
        - conditionValueId: path
          conditionValue: /v1/pets
  actions:
    - actionId: header-transform
      actionClassName: com.networknt.rule.HeaderReplaceResponseTransformAction
      actionValues:
        - actionValueId: sourceHeader
          value: X-Test-1
        - actionValueId: targetHeader
          value: My-Header
```

There is another use case to replace the targetHeader with a targetValue passed in from the rule action value. Here is the section of the actionValues for this use case. It will update the My-Header header to newToken.

When the targetValue is sensitive, you should encrypt it with the light-4j encrypt utility or your customized utility. The encrypted value will be decrypted in the plugin.

When the targetValue is sensitive, you should encrypt it with the light-4j encrypt utility or your customized utility. The encrypted value will be decrypted in the plugin.

```
      actionValues:
        - actionValueId: targetValue
          value: newToken
        - actionValueId: targetHeader
          value: My-Header
```

If you have multiple request paths that need to apply this rule, you can construct your rule like this. Notice that the first joinCode is AND, and all others are OR.  

```
petstore-response-header-replace:
  ruleId: petstore-response-header-replace
  host: lightapi.net
  ruleType: response-transform
  visibility: public
  description: Transform the response to replace one header with the other header.
  conditions:
    - conditionId: path-pets
      propertyPath: requestPath
      operatorCode: EQ
      joinCode: AND
      index: 1
      conditionValues:
        - conditionValueId: path
          conditionValue: /v1/pets
    - conditionId: path-dogs
      propertyPath: requestPath
      operatorCode: EQ
      joinCode: OR
      index: 2
      conditionValues:
        - conditionValueId: path
          conditionValue: /v1/dogs
    - conditionId: path-cats
      propertyPath: requestPath
      operatorCode: EQ
      joinCode: OR
      index: 3
      conditionValues:
        - conditionValueId: path
          conditionValue: /v1/cats

  actions:
    - actionId: header-transform
      actionClassName: com.networknt.rule.HeaderReplaceResponseTransformAction
      actionValues:
        - actionValueId: sourceHeader
          value: X-Test-1
        - actionValueId: targetHeader
          value: My-Header

```
Or you can write your rule this way. Notice that we use IN operator code and give multiple conditionValues objects. 

```
petstore-response-header-replace-in:
  ruleId: petstore-response-header-replace-in
  host: lightapi.net
  ruleType: response-transform
  visibility: public
  description: Transform the response to replace one header with the other header.
  conditions:
    - conditionId: path-pets
      propertyPath: requestPath
      operatorCode: IN
      joinCode: AND
      index: 1
      conditionValues:
        - conditionValueId: pets
          conditionValue: /v1/pets
        - conditionValueId: dogs
          conditionValue: /v1/dogs
        - conditionValueId: cats
          conditionValue: /v1/cats
  actions:
    - actionId: header-transform
      actionClassName: com.networknt.rule.ValidationAction
      actionValues:
        - actionValueId: sourceHeader
          value: X-Test-1
        - actionValueId: targetHeader
          value: My-Header

```

If you have request path with query parameters, you can use pattern matching as the condition. For example. 

```
petstore-response-header-replace-match:
  ruleId: petstore-response-header-replace-match
  host: lightapi.net
  ruleType: response-transform
  visibility: public
  description: Transform the response to replace one header with the other header.
  conditions:
    - conditionId: BankingServices
      propertyPath: requestPath
      operatorCode: MATCH
      joinCode: AND
      index: 1
      conditionValues:
        - conditionValueId: path
          conditionValue: /v3/(.*)/BankingServices/(.*)
    - conditionId: insight
      propertyPath: requestPath
      operatorCode: MATCH
      joinCode: OR
      index: 2
      conditionValues:
        - conditionValueId: path
          conditionValue: /v3/(.*)/insight/login/(.*)/attributes/(.*)
    - conditionId: Investments
      propertyPath: requestPath
      operatorCode: MATCH
      joinCode: OR
      index: 3
      conditionValues:
        - conditionValueId: path
          conditionValue: /v3/(.*)/Investments
    - conditionId: attributes
      propertyPath: requestPath
      operatorCode: MATCH
      joinCode: OR
      index: 4
      conditionValues:
        - conditionValueId: path
          conditionValue: /v3/(.*)/attributes/(.*)

```

The above rule can pass the following paths. 

```
/v3/{customerId}/BankingServices/Token
/v3/{customerId}/Investments
/v3/{customerId}/insight/login/{loginId}/attributes/{requestId}/GetLendingAttributes
/v3/{customerId}/attributes/library
```

Compare with the request side of the rule, we only pass the sourceHeader and targetHeader to the rule engine, so the source header won't be removed after the execution.

Make the following update to the values.yml to add the /v1/pets to the appliedPathPrefixes to the response-transformer.appliedPathPrefixes. 

```
# response-transformer.yml
response-transformer.appliedPathPrefixes: ["/v1/pets","/v1/notifications","/v1/flowers"]

```

In the previous step, we have added the ruleId to the rule-loader.endpointRules, so we don't need to do anything extra here.

