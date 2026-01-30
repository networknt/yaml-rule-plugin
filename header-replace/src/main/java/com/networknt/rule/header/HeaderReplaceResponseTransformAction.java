package com.networknt.rule.header;

import com.networknt.config.ConfigInjection;
import com.networknt.rule.ResponseTransformAction;
import com.networknt.rule.RuleActionValue;
import com.networknt.server.ModuleRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;

/**
 * Replace a target response header value with the source response header in the response transformer. Each time, there is only one
 * header can be replaced with another.
 *
 * @author Steve Hu
 */
public class HeaderReplaceResponseTransformAction implements ResponseTransformAction {
    private static final Logger logger = LoggerFactory.getLogger(HeaderReplaceResponseTransformAction.class);

    public HeaderReplaceResponseTransformAction() {
        if(logger.isInfoEnabled()) logger.info("HeaderReplaceResponseTransformAction is constructed");
        ModuleRegistry.registerPlugin(
                HeaderReplaceResponseTransformAction.class.getPackage().getImplementationTitle(),
                HeaderReplaceResponseTransformAction.class.getPackage().getImplementationVersion(),
                null,
                HeaderReplaceResponseTransformAction.class.getName(),
                null,
                null);
    }

    @Override
    public void performAction(String ruleId, String actionId, Map<String, Object> objMap, Map<String, Object> resultMap, Collection<RuleActionValue> actionValues) {
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
        if(logger.isDebugEnabled())
            logger.debug("source response ruleId = {} actionId {} header = {} target response header = {} targetValue = {} removeSourceHeader = {}", ruleId, actionId, sourceHeader, targetHeader, targetValue, removeSourceHeader);
        Map<String, String> headerMap = (Map<String, String>)objMap.get("responseHeaders");
        // there are two situations to handler. sourceHeader vs targetValue. One of them should not be null.
        // if both are not null, then only the targetValue will be used.
        if(targetValue != null) {
            targetValue = (String) ConfigInjection.decryptEnvValue(ConfigInjection.getDecryptor(), targetValue);
            ResponseTransformAction.super.updateResponseHeader(resultMap, targetHeader, targetValue);
        } else {
            String sourceValue = headerMap.get(sourceHeader);
            if(logger.isDebugEnabled())
                logger.debug("source response header = {} value = {}", sourceHeader, sourceValue);
            if(sourceValue != null) {
                if(Boolean.TRUE.equals(removeSourceHeader)) {
                    ResponseTransformAction.super.removeResponseHeader(resultMap, sourceHeader);
                }
                ResponseTransformAction.super.updateResponseHeader(resultMap, targetHeader, sourceValue);
            }
        }
    }
}
