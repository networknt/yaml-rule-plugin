package com.networknt.rule.encoder;

import com.networknt.rule.IAction;
import com.networknt.rule.RuleActionValue;
import com.networknt.rule.RuleConstants;
import com.networknt.utility.ModuleRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Map;

/**
 * This action is used to transform the response body encoding from one encoding to UTF-8 encoding.
 * As the ResponseTransformInterceptor has already converted the body stream into UTF-8 encoding, here
 * we just need to change the declaration in the body string if it is an XML string.
 *
 * @author Steve Hu
 */
public class ResponseBodyUtf8EncodingTransformAction implements IAction {
    private static final Logger logger = LoggerFactory.getLogger(ResponseBodyUtf8EncodingTransformAction.class);
    public static final String RESPONSE_BODY = "responseBody";
    public static final String UTF8 = "UTF-8";

    public ResponseBodyUtf8EncodingTransformAction() {
        if(logger.isInfoEnabled()) logger.info("ResponseBodyUtf8EncodingTransformAction is constructed");
        ModuleRegistry.registerPlugin(
                ResponseBodyUtf8EncodingTransformAction.class.getPackage().getImplementationTitle(),
                ResponseBodyUtf8EncodingTransformAction.class.getPackage().getImplementationVersion(),
                null,
                ResponseBodyUtf8EncodingTransformAction.class.getName(),
                null,
                null);

    }

    @Override
    public void performAction(Map<String, Object> objMap, Map<String, Object> resultMap, Collection<RuleActionValue> actionValues) {
        // get the body from the objMap and create a new body in the resultMap. Both in string format.
        resultMap.put(RuleConstants.RESULT, true);
        String responseBody = (String)objMap.get(RESPONSE_BODY);
        if(logger.isTraceEnabled()) logger.debug("original response body = {}", responseBody);
        // replace XML encoding declaration if it is for XML strings.
        if(responseBody.startsWith("<?xml")) {
            responseBody = responseBody.replaceFirst("encoding=\"[^\"]*\"", "encoding=\"" + UTF8 + "\"");
        }
        if(logger.isTraceEnabled()) logger.trace("updated response body = {}", responseBody);
        resultMap.put(RESPONSE_BODY, responseBody);
    }
}
