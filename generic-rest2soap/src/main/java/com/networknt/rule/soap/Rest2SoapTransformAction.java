package com.networknt.rule.soap;

import com.networknt.rule.soap.exception.InvalidJsonBodyException;
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
 * Transform a request body from the JSON to XML in order to access SOAP API from rest client. This can be used in
 * light-gateway or http-sidecar to change the request from newly built consumer to access legacy service.
 *
 */

public class Rest2SoapTransformAction implements IAction {
    protected static final Logger logger = LoggerFactory.getLogger(Rest2SoapTransformAction.class);

    @Override
    public void performAction(Map<String, Object> objMap, Map<String, Object> resultMap, Collection<RuleActionValue> actionValues) {
        
    	resultMap.put(RuleConstants.RESULT, true);
        String direction = "";
        logger.info("actionValues: {}", actionValues);
        for (RuleActionValue actionValue : actionValues) {

            if (actionValue == null || actionValue.getActionValueId() == null || actionValue.getValue() == null) {
                logger.error("Rules.yml does not contain ActionValues section. Please fix config");
                break;
            }

            if (actionValue.getActionValueId().equals("direction")) {
                direction = actionValue.getValue().trim();

                if(logger.isTraceEnabled())
                    logger.trace("actionValueID = direction and value = " + direction);
            }
        }
        
        switch (direction) {
        case Constants.REQUEST_DIRECTION:
        	transformRequest(objMap, resultMap, actionValues);
        	break;
        case Constants.RESPONSE_DIRECTION:
        	transformResponse(objMap, resultMap, actionValues);
        	break;
        default:
        	logger.error("Rules.yml actionValue direction must be request or response. Please fix config");
        }
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

        // transform the content type header.
        HeaderMap headerMap = (HeaderMap)objMap.get("requestHeaders");
        String contentType = null;
        HeaderValues contentTypeObject = headerMap.get(Headers.CONTENT_TYPE);

        if(contentTypeObject != null)
            contentType = contentTypeObject.getFirst();

        if(logger.isTraceEnabled())
            logger.trace("header contentType = " + contentType);

        if(contentType != null && contentType.startsWith("application/json")) {
            // change it to text/xml
            headerMap.remove(Headers.CONTENT_TYPE);
            headerMap.put(Headers.CONTENT_TYPE, "text/xml");

            if(logger.isTraceEnabled())
                logger.trace("request contentType has been changed from application/json to text/xml");
        } else {
            throw new InvalidJsonBodyException("Missing Content-Type header application/json in request");
        }

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
        HeaderMap headerMap = (HeaderMap)objMap.get("responseHeaders");
        String contentType = null;
        HeaderValues contentTypeObject = headerMap.get(Headers.CONTENT_TYPE);

        if(contentTypeObject != null)
            contentType = contentTypeObject.getFirst();

        if(logger.isTraceEnabled())
            logger.trace("header contentType = " + contentType);

        if(contentType != null && contentType.startsWith("application/json")) {
            // change it to text/xml
            headerMap.remove(Headers.CONTENT_TYPE);
            headerMap.put(Headers.CONTENT_TYPE, "text/xml");

            if(logger.isTraceEnabled())
                logger.trace("response contentType has been changed from application/json to text/xml");
        } else {
            throw new InvalidJsonBodyException("Missing Content-Type header application/json in response");
        }
    }
}
