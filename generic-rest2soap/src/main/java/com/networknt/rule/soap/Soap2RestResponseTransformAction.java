package com.networknt.rule.soap;

import com.networknt.rule.ResponseTransformAction;
import com.networknt.rule.RuleActionValue;
import com.networknt.rule.soap.exception.InvalidSoapBodyException;
import com.networknt.utility.MapUtil;
import com.networknt.utility.ModuleRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * Transform a response body from the XML to JSON in order to access Soap API from Rest client. This can be used in
 * light-gateway or http-sidecar to change the response from legacy service to access with REST client.
 *
 */
public class Soap2RestResponseTransformAction implements ResponseTransformAction {
    protected static final Logger logger = LoggerFactory.getLogger(Soap2RestResponseTransformAction.class);
    String direction = "";

    public Soap2RestResponseTransformAction() {
        if (logger.isInfoEnabled()) logger.info("Soap2RestResponseTransformAction is constructed");
        ModuleRegistry.registerPlugin(
                Soap2RestResponseTransformAction.class.getPackage().getImplementationTitle(),
                Soap2RestResponseTransformAction.class.getPackage().getImplementationVersion(),
                null,
                Soap2RestResponseTransformAction.class.getName(),
                null,
                null);

    }

    @Override
    public void performAction(String ruleId, String actionId, Map<String, Object> objMap, Map<String, Object> resultMap, Collection<RuleActionValue> actionValues) {
        // get the response body from the objMap and create a new response body in the resultMap. Both in string format.
        logger.info("ruleId: {} actionId: {} actionValues: {}", ruleId, actionId, actionValues);
        if (actionValues == null || actionValues.isEmpty()) {
            logger.error("Rules.yml does not contain ActionValues section. Please fix config");
            return;
        }
        transformResponse(objMap, resultMap);
    }

    private void transformResponse(Map<String, Object> objMap, Map<String, Object> resultMap) {

        String body = (String) objMap.get("responseBody");

        if (logger.isTraceEnabled())
            logger.trace("original response body = {}", body);

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
        Map<String, String> headerMap = (Map<String, String>) objMap.get("responseHeaders");
        Optional<String> contentTypeOptional = MapUtil.getValueIgnoreCase(headerMap, Constants.CONTENT_TYPE);
        if (contentTypeOptional.isPresent()) {
            String contentType = contentTypeOptional.get();
            if (logger.isTraceEnabled())
                logger.trace("response header contentType = {}", contentType);

            if (contentType.contains("/xml")) {
                // transform the content type header.
                ResponseTransformAction.super.updateResponseHeader(resultMap, "Content-Type", "application/json");
                if (logger.isTraceEnabled())
                    logger.trace("response contentType has been changed from */xml to application/json");
            } else {
                throw new InvalidSoapBodyException("Missing Content-Type header text/xml or application/xml in response.");
            }
        } else {
            if(logger.isDebugEnabled()) logger.debug("header Content-Type doesn't exist.");
        }
    }
}
