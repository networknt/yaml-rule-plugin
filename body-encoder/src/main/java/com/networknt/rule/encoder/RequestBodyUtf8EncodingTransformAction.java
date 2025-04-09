package com.networknt.rule.encoder;

import com.networknt.rule.RequestTransformAction;
import com.networknt.rule.RuleActionValue;
import com.networknt.rule.RuleConstants;
import com.networknt.utility.ModuleRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;

/**
 * This action is used to transform the request body encoding from one encoding to UTF-8 encoding.
 * As the RequestTransformInterceptor has already converted the body stream into UTF-8 encoding, here
 * we just need to change the declaration in the body string if it is an XML string.
 *
 * @author Steve Hu
 */
public class RequestBodyUtf8EncodingTransformAction implements RequestTransformAction {
    private static final Logger logger = LoggerFactory.getLogger(RequestBodyUtf8EncodingTransformAction.class);
    public static final String REQUEST_BODY = "requestBody";
    public static final String UTF8 = "UTF-8";

    public RequestBodyUtf8EncodingTransformAction() {
        if(logger.isInfoEnabled()) logger.info("RequestBodyUtf8EncodingTransformAction is constructed");
        ModuleRegistry.registerPlugin(
                RequestBodyUtf8EncodingTransformAction.class.getPackage().getImplementationTitle(),
                RequestBodyUtf8EncodingTransformAction.class.getPackage().getImplementationVersion(),
                null,
                RequestBodyUtf8EncodingTransformAction.class.getName(),
                null,
                null);
    }

    @Override
    public void performAction(String ruleId, String actionId, Map<String, Object> objMap, Map<String, Object> resultMap, Collection<RuleActionValue> actionValues) {
        // get the body from the objMap and create a new body in the resultMap.
        String requestBody = (String)objMap.get(REQUEST_BODY);
        if(logger.isTraceEnabled()) logger.trace("ruleId: {} actionId: {} original requestBody: {}", ruleId, actionId, requestBody);
        if(requestBody.startsWith("<?xml")) {
            requestBody = requestBody.replaceFirst("encoding=\"[^\"]*\"", "encoding=\"" + UTF8 + "\"");
        }
        if(logger.isTraceEnabled()) logger.trace("updated request body = {}", requestBody);
        resultMap.put(REQUEST_BODY, requestBody);
    }
}
