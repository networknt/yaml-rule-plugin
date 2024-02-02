package com.networknt.rule.sanitizer;

import com.networknt.config.JsonMapper;
import com.networknt.rule.IAction;
import com.networknt.rule.RuleActionValue;
import com.networknt.rule.RuleConstants;
import com.networknt.config.Config;
import com.networknt.sanitizer.SanitizerConfig;
import com.networknt.utility.ModuleRegistry;
import org.owasp.encoder.EncoderWrapper;
import org.owasp.encoder.Encoders;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.*;

/**
 * Transform the request body to encode the cross-site scripting based on the sanitizer.yml configuration. It is
 * used in http-sidecar and light-gateway to intercept the body and return the updated/encoded request body to the
 * transform interceptor to update the body in order to send the updated one to the downstream API.
 *
 * @author Steve Hu
 */
public class BodySanitizerTransformAction implements IAction {
    private static final Logger logger = LoggerFactory.getLogger(BodySanitizerTransformAction.class);
    private static final SanitizerConfig config = SanitizerConfig.load();
    private static final EncoderWrapper bodyEncoder = new EncoderWrapper(Encoders.forName(config.getBodyEncoder()), config.getBodyAttributesToIgnore(), config.getBodyAttributesToEncode());
    public BodySanitizerTransformAction() {
        if(logger.isInfoEnabled()) logger.info("BodySanitizerTransformAction is constructed");
        ModuleRegistry.registerPlugin(
                BodySanitizerTransformAction.class.getPackage().getImplementationTitle(),
                BodySanitizerTransformAction.class.getPackage().getImplementationVersion(),
                SanitizerConfig.CONFIG_NAME,
                BodySanitizerTransformAction.class.getName(),
                Config.getNoneDecryptedInstance().getJsonMapConfigNoCache(SanitizerConfig.CONFIG_NAME),
                null);
    }
    @Override
    public void performAction(Map<String, Object> objMap, Map<String, Object> resultMap, Collection<RuleActionValue> actionValues) {
        // get the body from the objMap and create a new body in the resultMap. Both in string format.
        resultMap.put(RuleConstants.RESULT, true);
        String requestBody = (String)objMap.get("requestBody");
        if(logger.isTraceEnabled()) logger.debug("original request body = " + requestBody);
        // convert the body from string to json map or list.
        try {
            Object body = Config.getInstance().getMapper().readValue(requestBody, Object.class);
            if(body instanceof Map) {
                Map<String, Object> bodyMap = (Map<String, Object>)body;
                bodyEncoder.encodeNode(bodyMap);
                requestBody = JsonMapper.toJson(bodyMap);
            } else if(body instanceof List) {
                List<Object> bodyList = (List<Object>)body;
                bodyEncoder.encodeList(bodyList);
                requestBody = JsonMapper.toJson(bodyList);
            } else {
                // if the body is not a map or list, then it is a string and we cannot encode it.
                if(logger.isTraceEnabled()) logger.trace("request body is not a map or list, skip encoding.");
            }
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        if(logger.isTraceEnabled()) logger.trace("encoded request body = " + requestBody);
        resultMap.put("requestBody", requestBody);
    }
}
