---

## Writing an Action Plugin for `yaml-rule`

When using the `yaml-rule`, rules are defined with YAML in the `rules.yml` file or through the `light-portal`. Whenever a rule evaluates as true, a list of specified actions will be executed. The `yaml-rule` and other `light-4j` products come with pre-defined actions, but users can also create custom plugins by implementing specific interfaces to perform actions on the `objMap` and `resultMap`.

### `IAction` Interface

The main interface for creating a plugin is `IAction`. Below is the code for this interface:

```java
public interface IAction {
    /**
     * Called when the rule conditions evaluate to true. It takes the inputMap
     * and returns another map as the result of the rule engine execution.
     *
     * @param inputMap Input map
     * @param resultMap Result map
     * @param actionValues Action values
     */
    void performAction(Map<String, Object> inputMap, Map<String, Object> resultMap, Collection<RuleActionValue> actionValues);

    default void postPerformAction(Map<String, Object> objMap, Map<String, Object> resultMap) {
        // Default NOOP for classes implementing IAction
    }
}
```

All plugins that implement this interface must define the `performAction` method. Additionally, the `postPerformAction` method is available as a default and is called after `performAction` in the `RuleEngine`. Since this is a general-purpose interface, the default method doesn't perform any action.

### `TransformAction` Interface

One of the more commonly used actions is the `TransformAction`, which is used to modify requests or responses, such as in `http-sidecar` or `light-gateway`. Here is the interface code for `TransformAction`:

```java
package com.networknt.rule;

import java.util.*;

public interface TransformAction extends IAction {
    String REQUEST_HEADERS = "requestHeaders";
    String RESPONSE_HEADERS = "responseHeaders";
    String REMOVE = "remove";
    String UPDATE = "update";

    default void removeRequestHeader(Map<String, Object> resultMap, String headerName) {
        Map<String, Object> requestHeaders = (Map) resultMap.get(REQUEST_HEADERS);
        if (requestHeaders == null) {
            requestHeaders = new HashMap<>();
            resultMap.put(REQUEST_HEADERS, requestHeaders);
        }
        List<String> removeList = (List<String>) requestHeaders.get(REMOVE);
        if (removeList == null) {
            removeList = new ArrayList<>();
            requestHeaders.put(REMOVE, removeList);
        }
        removeList.add(headerName);
    }

    default void updateRequestHeader(Map<String, Object> resultMap, String headerName, String headerValue) {
        Map<String, Object> requestHeaders = (Map) resultMap.get(REQUEST_HEADERS);
        if (requestHeaders == null) {
            requestHeaders = new HashMap<>();
            resultMap.put(REQUEST_HEADERS, requestHeaders);
        }
        Map<String, String> updateMap = (Map<String, String>) requestHeaders.get(UPDATE);
        if (updateMap == null) {
            updateMap = new HashMap<>();
            requestHeaders.put(UPDATE, updateMap);
        }
        updateMap.put(headerName, headerValue);
    }

    default void removeResponseHeader(Map<String, Object> resultMap, String headerName) {
        Map<String, Object> responseHeaders = (Map) resultMap.get(RESPONSE_HEADERS);
        if (responseHeaders == null) {
            responseHeaders = new HashMap<>();
            resultMap.put(RESPONSE_HEADERS, responseHeaders);
        }
        List<String> removeList = (List<String>) responseHeaders.get(REMOVE);
        if (removeList == null) {
            removeList = new ArrayList<>();
            responseHeaders.put(REMOVE, removeList);
        }
        removeList.add(headerName);
    }

    default void updateResponseHeader(Map<String, Object> resultMap, String headerName, String headerValue) {
        Map<String, Object> responseHeaders = (Map) resultMap.get(RESPONSE_HEADERS);
        if (responseHeaders == null) {
            responseHeaders = new HashMap<>();
            resultMap.put(RESPONSE_HEADERS, responseHeaders);
        }
        Map<String, String> updateMap = (Map<String, String>) responseHeaders.get(UPDATE);
        if (updateMap == null) {
            updateMap = new HashMap<>();
            responseHeaders.put(UPDATE, updateMap);
        }
        updateMap.put(headerName, headerValue);
    }
}
```

This interface extends IAction and provides four default methods to handle header manipulations. Specifically, it includes removeRequestHeader and updateRequestHeader for modifying request headers in the resultMap, and similarly, removeResponseHeader and updateResponseHeader for managing response headers. These methods help build structures similar to the HeaderHandler in light-4j, allowing updates to request and response headers.

The purpose of these methods is to enable users to manipulate headers while considering potential conflicts with other actions that may modify headers simultaneously. Each method first checks if the corresponding header object exists in the resultMap and creates it if not.

Notably, these methods do not modify the objMap directly for header updates. Instead, they only update the internal structure for adding or removing headers. Given that multiple rule actions may access the same object within the same rule, a later action could overwrite values set by a previous action. However, as there are no current use cases where two or more actions modify the same request or response header, this scenario is not handled at the moment.

In the future, if such use cases arise, the default methods can be enhanced to update the requestHeaders and responseHeaders in objMap passed from the interceptor. This would ensure that the result from the first action becomes the input for the next action in the sequence.

### `RequestTransformAction`

This interface is specifically designed for transforming request bodies in `light-4j` via the `RequestTransformerInterceptor`. It allows several actions to be chained and to update the `objMap` from the `resultMap`. Here’s the code:

```java
public interface RequestTransformAction extends TransformAction {
    String REQUEST_BODY = "requestBody";

    @Override
    default void postPerformAction(Map<String, Object> objMap, Map<String, Object> resultMap) {
        TransformAction.super.postPerformAction(objMap, resultMap);
        // Copy requestBody from resultMap to objMap if it exists.
        if (resultMap.get(REQUEST_BODY) != null) {
            objMap.put(REQUEST_BODY, resultMap.get(REQUEST_BODY));
        }
    }
}
```

In this interface, the default `postPerformAction` method is provided to copy the request body from the `resultMap` to the `objMap`, ensuring that it can be used as input for subsequent actions in the chain. This mechanism supports scenarios where multiple actions may modify the request body simultaneously but target different sections of it.

For example, the `Rest2SoapRequestTransformAction` might transform a JSON request body into XML, while the `SoapSecurityTransformAction` could add a security header to the XML body to handle credentials. These actions work together seamlessly because each step in the chain can access the updated request body from the `objMap`, allowing for multiple transformations on different parts of the request.

### `ResponseTransformAction`

The `ResponseTransformAction` is similar to `RequestTransformAction` but works on response bodies:

```java
public interface ResponseTransformAction extends TransformAction {
    String RESPONSE_BODY = "responseBody";

    default void postPerformAction(Map<String, Object> objMap, Map<String, Object> resultMap) {
        TransformAction.super.postPerformAction(objMap, resultMap);
        // Copy responseBody from resultMap to objMap if it exists.
        if (resultMap.get(RESPONSE_BODY) != null) {
            objMap.put(RESPONSE_BODY, resultMap.get(RESPONSE_BODY));
        }
    }
}
```

Similar to `RequestTransformAction`, this interface copies the response body from the `resultMap` to the `objMap`, ensuring that the updated response body is available for subsequent actions in the chain. This process allows each action to access and modify the response body incrementally, making it possible for multiple actions to work on different aspects of the response without conflicts.

### Plugin Example: `Rest2SoapRequestTransformAction`

To illustrate, let’s look at an example plugin, `Rest2SoapRequestTransformAction`, which transforms a request from JSON to XML for accessing SOAP APIs:

```java
public class Rest2SoapRequestTransformAction implements RequestTransformAction {
    protected static final Logger logger = LoggerFactory.getLogger(Rest2SoapRequestTransformAction.class);

    public Rest2SoapRequestTransformAction() {
        if (logger.isInfoEnabled()) logger.info("Rest2SoapRequestTransformAction is constructed");
        ModuleRegistry.registerPlugin(
                Rest2SoapRequestTransformAction.class.getPackage().getImplementationTitle(),
                Rest2SoapRequestTransformAction.class.getPackage().getImplementationVersion(),
                null,
                Rest2SoapRequestTransformAction.class.getName(),
                null,
                null
        );
    }

    @Override
    public void performAction(Map<String, Object> objMap, Map<String, Object> resultMap, Collection<RuleActionValue> actionValues) {
        logger.info("actionValues: {}", actionValues);
        if (actionValues == null || actionValues.isEmpty()) {
            logger.error("rules.yml does not contain ActionValues section. Please fix config");
            return;
        }
        transformRequest(objMap, resultMap, actionValues);
    }

    private void transformRequest(Map<String, Object> objMap, Map<String, Object> resultMap, Collection<RuleActionValue> actionValues) {
        String body = (String) objMap.get("requestBody");

        if (logger.isDebugEnabled()) logger.debug("original request body = {}", body);

        try {
            String output = Util.transformRest2Soap(body, actionValues);
            resultMap.put("requestBody", output);
            if (logger.isDebugEnabled()) logger.debug("transformed request body = {}", output);
        } catch (IOException ioe) {
            logger.error("Transform exception:", ioe);
        }

        Map<String, String> headerMap = (Map<String, String>) objMap.get("requestHeaders");
        String contentType = headerMap.get("Content-Type");

        if (contentType != null && contentType.startsWith("application/json")) {
            // Update the Content-Type header.
            RequestTransformAction.super.updateRequestHeader(resultMap, "Content-Type", "text/xml");
        } else {
            throw new InvalidJsonBodyException("Missing Content-Type header application/json in request");
        }
    }
}
```


* The class implements `RequestTransformAction`, which indicates that it will update the request body, headers, or other aspects related to the request within the request chain of the light-4j handlers.

* In the constructor, it invokes the `ModuleRegistry.registryPlugin` method to register the plugin, making it visible through the `/adm/server/info` endpoint. The method accepts the plugin title, version, config name, class name, and a list of masks to hide sensitive information from the configuration map.

* The `performAction` method contains the core logic for manipulating the request. You retrieve the original request body from the `objMap`, modify it, and push the updated content back into the `resultMap`. The rule engine will then copy the modified request body from the `resultMap` to the `objMap` using the default `postPerformAction` method from `RequestTransformAction`. Additional properties from the `objMap` can be used in the plugin, which can be referenced from the light-4j repositories for both [request](https://github.com/networknt/light-4j/blob/master/request-transformer/src/main/java/com/networknt/reqtrans/RequestTransformerInterceptor.java#L129) and [response](https://github.com/networknt/light-4j/blob/master/response-transformer/src/main/java/com/networknt/restrans/ResponseTransformerInterceptor.java#L235).

* For header manipulation, the default methods from the `TransformAction` interface can be utilized. For instance, the plugin in the example calls `RequestTransformAction.super.updateRequestHeader` to change the `Content-Type` to `text/xml`.

* Other request attributes can be updated within the plugin, and the relevant lists of attributes are available in the respective [request](https://github.com/networknt/light-4j/blob/master/request-transformer/src/main/java/com/networknt/reqtrans/RequestTransformerInterceptor.java#L178) and [response](https://github.com/networknt/light-4j/blob/master/response-transformer/src/main/java/com/networknt/restrans/ResponseTransformerInterceptor.java#L176) transformer interceptors.
