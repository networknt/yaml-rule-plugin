package com.networknt.rule;

import io.undertow.util.HeaderMap;
import io.undertow.util.HeaderValues;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * This is a demo rule action implementation to validate the request header context against a constant value
 * and return an error message to the caller if not matched.
 *
 * @author Steve Hu
 */
public class ContextValidatorRequestTransformAction implements IAction {
    private static final Logger logger = LoggerFactory.getLogger(ContextValidatorRequestTransformAction.class);

    // In the real implementation, this should be put into the configuration file.
    static final String secretContext = "secret";

    @Override
    public void performAction(Map<String, Object> objMap, Map<String, Object> resultMap, Collection<RuleActionValue> actionValues) {
        HeaderMap headerMap = (HeaderMap)objMap.get("requestHeaders");
        String contextValue = null;
        HeaderValues contextObject = headerMap.get("context");
        if(contextObject != null) contextValue = contextObject.getFirst();
        if(logger.isTraceEnabled()) logger.trace("context request header is {}" + contextValue);
        if(contextValue != null && secretContext.equals(contextValue)) {
            // validation passed and don't return anything to the request transformer interceptor
            resultMap.put(RuleConstants.RESULT, false);
            if(logger.isTraceEnabled()) logger.trace("Context validation is passed and rule result is false");
        } else {
            resultMap.put(RuleConstants.RESULT, true);
            if(logger.isTraceEnabled()) logger.trace("Context validation is failed and rule result is true");
            // validationError
            String errorMessage = "{\"error\":\"invalid context\"}";
            String contentType = "application/json";
            int statusCode = 401;
            if(logger.isTraceEnabled()) logger.trace("Return values: errorMessage {} contentType {} statusCode", errorMessage, contentType, statusCode);
            resultMap.put("errorMessage", errorMessage);
            resultMap.put("contentType", contentType);
            resultMap.put("statusCode", statusCode);
            resultMap.put("validationError", true);
        }
    }
}
