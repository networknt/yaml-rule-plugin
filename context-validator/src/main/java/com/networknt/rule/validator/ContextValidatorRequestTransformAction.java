package com.networknt.rule.validator;

import com.networknt.rule.RequestTransformAction;
import com.networknt.rule.RuleActionValue;
import com.networknt.rule.RuleConstants;
import com.networknt.utility.ModuleRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * This is a demo rule action implementation to validate the request header context against a constant value
 * and return an error message to the caller if not matched.
 *
 * @author Steve Hu
 */
public class ContextValidatorRequestTransformAction implements RequestTransformAction {
    private static final Logger logger = LoggerFactory.getLogger(ContextValidatorRequestTransformAction.class);

    // In the real implementation, this should be put into the configuration file.
    static final String secretContext = "secret";

    public ContextValidatorRequestTransformAction() {
        if(logger.isInfoEnabled()) logger.info("ContextValidatorRequestTransformAction is constructed");
        ModuleRegistry.registerPlugin(
                ContextValidatorRequestTransformAction.class.getPackage().getImplementationTitle(),
                ContextValidatorRequestTransformAction.class.getPackage().getImplementationVersion(),
                null,
                ContextValidatorRequestTransformAction.class.getName(),
                null,
                null);
    }

    @Override
    public void performAction(String ruleId, String actionId, Map<String, Object> objMap, Map<String, Object> resultMap, Collection<RuleActionValue> actionValues) {
        Map<String, String> headerMap = (Map<String, String>)objMap.get("requestHeaders");
        String contextValue  = headerMap.get("context");
        if(logger.isTraceEnabled()) logger.trace("ruleId = {} actionId = {} context request header is {}", ruleId, actionId, contextValue);
        if(secretContext.equals(contextValue)) {
            // validation passed and don't return anything to the request transformer interceptor
            if(logger.isTraceEnabled()) logger.trace("Context validation is passed and rule result is false");
        } else {
            if(logger.isTraceEnabled()) logger.trace("Context validation is failed and rule result is true");
            // validationError
            String errorMessage = "{\"error\":\"invalid context\"}";
            String contentType = "application/json";
            int statusCode = 401;
            if(logger.isTraceEnabled()) logger.trace("Return values: errorMessage {} contentType {} statusCode {}", errorMessage, contentType, statusCode);
            resultMap.put("errorMessage", errorMessage);
            resultMap.put("contentType", contentType);
            resultMap.put("statusCode", statusCode);
            resultMap.put("validationError", true);
        }
    }
}
