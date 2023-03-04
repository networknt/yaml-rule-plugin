package com.networknt.rule.soap;

import com.networknt.rule.soap.exception.InvalidSoapBodyException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.networknt.rule.IAction;
import com.networknt.rule.RuleActionValue;
import com.networknt.rule.RuleConstants;
import io.undertow.util.HeaderMap;
import io.undertow.util.HeaderValues;
import io.undertow.util.Headers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.io.IOException;
import java.util.Collection;
import java.util.Map;

/**
 * Transform a request or response body from XML to JSON
 */

public class Soap2RestTransformAction implements IAction {
    protected static final Logger logger = LoggerFactory.getLogger(Soap2RestTransformAction.class);

    @Override
    public void performAction(Map<String, Object> objMap, Map<String, Object> resultMap, Collection<RuleActionValue> actionValues) {
        // get the response body from the objMap and create a new response body in the resultMap. Both in string format.
        resultMap.put(RuleConstants.RESULT, true);
        String direction = "";
        logger.info("actionValues: {}", actionValues);
        for (RuleActionValue actionValue : actionValues) {
            if (actionValue == null || actionValue.getActionValueId() == null || actionValue.getValue() == null) {
                logger.error("Rules.yml does not contain ActionValues section. Please fix config");
                break;
            }
            if (actionValue.getActionValueId().equalsIgnoreCase("direction")) {
                direction = actionValue.getValue().trim();

                if (logger.isTraceEnabled())
                    logger.trace("actionValueID = direction and value = " + direction);

            }
        }

        switch (direction) {
            case Constants.REQUEST_DIRECTION:
                transformRequest(objMap, resultMap);
                break;
            case Constants.RESPONSE_DIRECTION:
                transformResponse(objMap, resultMap);
                break;
            default:
                logger.error("Rules.yml actionValue direction must be request or response. Please fix config");
        }
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
        HeaderMap headerMap = (HeaderMap) objMap.get("requestHeaders");
        String contentType = null;
        HeaderValues contentTypeObject = headerMap.get(Headers.CONTENT_TYPE);

        if (contentTypeObject != null)
            contentType = contentTypeObject.getFirst();

        if (logger.isTraceEnabled())
            logger.trace("request header contentType = " + contentType);

        if (contentType != null && (contentType.startsWith("text/xml") || contentType.startsWith("application/xml"))) {
            // change it to application/json
            headerMap.remove(Headers.CONTENT_TYPE);
            headerMap.put(Headers.CONTENT_TYPE, "application/json");

            if (logger.isTraceEnabled())
                logger.trace("request contentType has been changed from text/xml to application/json");
        } else {
            throw new InvalidSoapBodyException("Missing Content-Type header text/xml or application/xml in request.");
        }
    }

    private void transformResponse(Map<String, Object> objMap, Map<String, Object> resultMap) {

        String body = (String) objMap.get("responseBody");

        if (logger.isTraceEnabled())
            logger.trace("original response body = " + body);

        String output = "";
        try {
            output = Util.transformSoap2Rest(body);
            resultMap.put("responseBody", output);

            if (logger.isTraceEnabled())
                logger.trace("transformed response body = " + output);

        } catch (IOException ioe) {
            logger.error("Transform exception:", ioe);
        }

        // transform the content type header.
        HeaderMap headerMap = (HeaderMap) objMap.get("responseHeaders");

        String contentType = null;
        HeaderValues contentTypeObject = headerMap.get(Headers.CONTENT_TYPE);

        if (contentTypeObject != null)
            contentType = contentTypeObject.getFirst();

        if (logger.isTraceEnabled())
            logger.trace("response header contentType = " + contentType);


        if (contentType != null && (contentType.startsWith("text/xml") || contentType.startsWith("application/xml"))) {
            // change it to text/xml
            headerMap.remove(Headers.CONTENT_TYPE);
            headerMap.put(Headers.CONTENT_TYPE, "application/json");

            if (logger.isTraceEnabled())
                logger.trace("response contentType has been changed from */xml to application/json");
        } else {
            throw new InvalidSoapBodyException("Missing Content-Type header text/xml or application/xml in response.");
        }
    }

}
