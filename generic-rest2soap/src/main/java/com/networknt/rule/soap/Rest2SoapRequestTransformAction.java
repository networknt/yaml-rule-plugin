package com.networknt.rule.soap;

import com.networknt.rule.RequestTransformAction;
import com.networknt.rule.RuleActionValue;
import com.networknt.rule.RuleConstants;
import com.networknt.rule.soap.exception.InvalidJsonBodyException;
import com.networknt.utility.ModuleRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

/**
 * Transform a request body from the JSON to XML in order to access SOAP API from rest client. This can be used in
 * light-gateway or http-sidecar to change the request from newly built consumer to access legacy service.
 *
 */
public class Rest2SoapRequestTransformAction implements RequestTransformAction {
    protected static final Logger logger = LoggerFactory.getLogger(Rest2SoapRequestTransformAction.class);

    public Rest2SoapRequestTransformAction() {
        if(logger.isInfoEnabled()) logger.info("Rest2SoapRequestTransformAction is constructed");
        ModuleRegistry.registerPlugin(
                Rest2SoapRequestTransformAction.class.getPackage().getImplementationTitle(),
                Rest2SoapRequestTransformAction.class.getPackage().getImplementationVersion(),
                null,
                Rest2SoapRequestTransformAction.class.getName(),
                null,
                null);
    }

    @Override
    public void performAction(Map<String, Object> objMap, Map<String, Object> resultMap, Collection<RuleActionValue> actionValues) {
        logger.info("actionValues: {}", actionValues);
        if(actionValues == null || actionValues.isEmpty()) {
            logger.error("Rules.yml does not contain ActionValues section. Please fix config");
            return;
        }
        transformRequest(objMap, resultMap, actionValues);
    }

    private void transformRequest(Map<String, Object> objMap, Map<String, Object> resultMap, Collection<RuleActionValue> actionValues) {
        String body = (String)objMap.get("requestBody");

        if(logger.isDebugEnabled())
            logger.debug("original request body = {}", body);

        String output = "";
        try {
            output = Util.transformRest2Soap(body, actionValues);
            resultMap.put("requestBody", output);

            if (logger.isDebugEnabled())
                logger.debug("transformed request body = {}", output);

        } catch (IOException ioe) {
            logger.error("Transform exception:", ioe);
        }

        Map<String, String> headerMap = (Map<String, String>)objMap.get("requestHeaders");
        String contentType = headerMap.get("Content-Type");

        if(logger.isTraceEnabled())
            logger.trace("header contentType = " + contentType);

        if(contentType != null && contentType.startsWith("application/json")) {
            // transform the content type header.
            RequestTransformAction.super.updateRequestHeader(resultMap, "Content-Type", "text/xml");
            if(logger.isTraceEnabled())
                logger.trace("request contentType has been changed from application/json to text/xml");
        } else {
            throw new InvalidJsonBodyException("Missing Content-Type header application/json in request");
        }

    }
}
