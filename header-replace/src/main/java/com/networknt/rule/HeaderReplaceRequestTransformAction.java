package com.networknt.rule;


import com.networknt.config.ConfigInjection;
import io.undertow.util.HeaderMap;
import io.undertow.util.HeaderValues;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Replace a target request header value with the source request header in the request transformer. Each time, there is only one
 * header can be replaced with another.
 *
 * @author Steve Hu
 */
public class HeaderReplaceRequestTransformAction implements IAction {
    private static final Logger logger = LoggerFactory.getLogger(HeaderReplaceRequestTransformAction.class);

    @Override
    public void performAction(Map<String, Object> objMap, Map<String, Object> resultMap, Collection<RuleActionValue> actionValues) {
        resultMap.put(RuleConstants.RESULT, true);
        String sourceHeader = null;
        String targetHeader = null;
        String targetValue = null;
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
            if("targetValue".equals(value.getActionValueId())) {
                targetValue = value.getValue();
                continue;
            }
            if("removeSourceHeader".equals(value.getActionValueId())) {
                removeSourceHeader = "true".equalsIgnoreCase(value.getValue()) ? Boolean.TRUE : Boolean.FALSE;
            }
        }
        if(logger.isDebugEnabled()) logger.debug("source request header = " + sourceHeader + " target request header = " + targetHeader + " targetValue = " + targetValue + " removeSourceHeader = " + removeSourceHeader);
        HeaderMap headerMap = (HeaderMap)objMap.get("requestHeaders");
        // there are two situations to handler. sourceHeader vs targetValue. One of them should not be null.
        // if both are not null, then only the targetValue will be used.
        if(targetValue != null) {
            // the targetValue is passed from the rule definition, and it might be encrypted.
            targetValue = (String) ConfigInjection.decryptEnvValue(ConfigInjection.getDecryptor(), targetValue);
            Map<String, Object> requestHeaders = new HashMap<>();
            Map<String, Object> updateMap = new HashMap<>();
            updateMap.put(targetHeader, targetValue);
            requestHeaders.put("update", updateMap);
            if(logger.isDebugEnabled()) logger.debug("final requestHeaders = " + requestHeaders);
            resultMap.put("requestHeaders", requestHeaders);
        } else {
            // use the sourceHeader to retrieve the value and replace the targetHeader.
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
        }
    }
}
