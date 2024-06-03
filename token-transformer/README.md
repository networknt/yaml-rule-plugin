This plugin is a generate plugin to get token from an external OAuth 2.0 provider. It can replace majority of the customized plugins(ServiceNow, Tealium etc.).

* How to handle the simple web token

For some of the external OAuth 2.0 providers, i.e. Salesforce, it uses simple web token. Unlike JWT token response that contains the expires-in field to indicate when the token will expire, the expiration must be obtained from the token introspection endpoint. Due to performance issue, we cannot use the introspection endpoint to get the expiration time each time the token is used. So we need a way to set the expiration time in the PathPrefixAuth object. Fortunately, most OAuth 2.0 provider will have a static expiration time for simple web token. The Salesforce token expires in 2 hours.

So, for the plugin to calculate correct cache time, we need to set the tokenTtl to 7200 in seconds.

```
pathPrefixAuths:
    tokenTtl: 7200
```

[Here](https://github.com/networknt/yaml-rule-plugin/blob/master/token-transformer/src/main/java/com/networknt/rule/generic/token/TokenAction.java#L240) is the code that handles the logic.
