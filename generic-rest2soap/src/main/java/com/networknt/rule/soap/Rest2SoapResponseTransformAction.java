package com.networknt.rule.soap;

import com.networknt.rule.*;
import com.networknt.rule.soap.exception.InvalidJsonBodyException;
import com.networknt.utility.MapUtil;
import com.networknt.utility.ModuleRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * Transform a response body from the JSON to XML in order to access JSON API from soap client. This can be used in
 * light-gateway or http-sidecar to change the response from legacy consumer to access rest service.
 *
 */
public class Rest2SoapResponseTransformAction implements ResponseTransformAction {
    protected static final Logger logger = LoggerFactory.getLogger(Rest2SoapResponseTransformAction.class);

    public Rest2SoapResponseTransformAction() {
        if(logger.isInfoEnabled()) logger.info("Rest2SoapResponseTransformAction is constructed");
        ModuleRegistry.registerPlugin(
                Rest2SoapResponseTransformAction.class.getPackage().getImplementationTitle(),
                Rest2SoapResponseTransformAction.class.getPackage().getImplementationVersion(),
                null,
                Rest2SoapResponseTransformAction.class.getName(),
                null,
                null);
    }

    @Override
    public void performAction(String ruleId, String actionId, Map<String, Object> objMap, Map<String, Object> resultMap, Collection<RuleActionValue> actionValues) {
        logger.info("ruleId: {} actionId: {} actionValues: {}", ruleId, actionId, actionValues);
        if(actionValues == null || actionValues.isEmpty()) {
            logger.error("Rules.yml does not contain ActionValues section. Please fix config");
            return;
        }
        transformResponse(objMap, resultMap, actionValues);
    }

    private void transformResponse(Map<String, Object> objMap, Map<String, Object> resultMap, Collection<RuleActionValue> actionValues) {
        String body = (String)objMap.get("responseBody");

        if(logger.isDebugEnabled())
            logger.debug("original response body = {}", body);

        String output = "";

        try {
            output = Util.transformRest2Soap(body, actionValues);

            resultMap.put("responseBody", output);
            if(logger.isDebugEnabled())
                logger.debug("transformed response body = {}", output);


        } catch (IOException ioe) {
            logger.error("Transform exception:", ioe);
        }

        // transform the content type header.
        Map<String, String> headerMap = (Map<String, String>)objMap.get("responseHeaders");
        Optional<String> contentTypeOptional = MapUtil.getValueIgnoreCase(headerMap, Constants.CONTENT_TYPE);
        if(contentTypeOptional.isPresent()) {
            String contentType = contentTypeOptional.get();

            if(logger.isTraceEnabled())
                logger.trace("header contentType = {}", contentType);

            if(contentType.startsWith("application/json")) {
                // transform the content type header.
                ResponseTransformAction.super.updateResponseHeader(resultMap, "Content-Type", "text/xml");
                if(logger.isTraceEnabled())
                    logger.trace("response contentType has been changed from application/json to text/xml");
            } else {
                throw new InvalidJsonBodyException("Missing Content-Type header application/json in response");
            }
        } else {
            if(logger.isDebugEnabled()) logger.debug("header Content-Type doesn't exist.");
        }
    }
}
