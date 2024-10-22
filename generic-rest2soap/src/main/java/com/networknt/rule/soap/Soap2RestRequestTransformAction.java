package com.networknt.rule.soap;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.networknt.rule.RequestTransformAction;
import com.networknt.rule.RuleActionValue;
import com.networknt.rule.RuleConstants;
import com.networknt.rule.soap.exception.InvalidSoapBodyException;
import com.networknt.utility.ModuleRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;

/**
 * Transform a request body from the XML to JSON in order to access REST API from soap client. This can be used in
 * light-gateway or http-sidecar to change the request from legacy consumer to access REST service.
 *
 */
public class Soap2RestRequestTransformAction implements RequestTransformAction {
    protected static final Logger logger = LoggerFactory.getLogger(Soap2RestRequestTransformAction.class);
    String direction = "";

    public Soap2RestRequestTransformAction() {
        if (logger.isInfoEnabled()) logger.info("Soap2RestRequestTransformAction is constructed");
        ModuleRegistry.registerPlugin(
                Soap2RestRequestTransformAction.class.getPackage().getImplementationTitle(),
                Soap2RestRequestTransformAction.class.getPackage().getImplementationVersion(),
                null,
                Soap2RestRequestTransformAction.class.getName(),
                null,
                null);

    }

    @Override
    public void performAction(Map<String, Object> objMap, Map<String, Object> resultMap, Collection<RuleActionValue> actionValues) {
        // get the response body from the objMap and create a new response body in the resultMap. Both in string format.
        logger.info("actionValues: {}", actionValues);
        if(actionValues == null || actionValues.isEmpty()) {
            logger.error("Rules.yml does not contain ActionValues section. Please fix config");
            return;
        }
        transformRequest(objMap, resultMap);
    }

    private void transformRequest(Map<String, Object> objMap, Map<String, Object> resultMap) {
        String body = (String) objMap.get("requestBody");

        if (logger.isTraceEnabled())
            logger.trace("original request body = " + body);

        String output = "";
        try {
            output = Util.transformSoap2Rest(body);
            resultMap.put("requestBody", output);
            if (logger.isTraceEnabled()) logger.trace("transformed request body = " + output);
        } catch (JsonProcessingException ioe) {
            logger.error("Transform exception:", ioe);
        }

        // transform the content type header.
        Map<String, String> headerMap = (Map<String, String>) objMap.get("requestHeaders");
        String contentType = headerMap.get("Content-Type");

        if (logger.isTraceEnabled())
            logger.trace("request header contentType = " + contentType);

        if (contentType != null && (contentType.startsWith("text/xml") || contentType.startsWith("application/xml"))) {
            // transform the content type header.
            RequestTransformAction.super.updateRequestHeader(resultMap, "Content-Type", "application/json");
            if (logger.isTraceEnabled())
                logger.trace("request contentType has been changed from text/xml to application/json");
        } else {
            throw new InvalidSoapBodyException("Missing Content-Type header text/xml or application/xml in request.");
        }
    }

}
