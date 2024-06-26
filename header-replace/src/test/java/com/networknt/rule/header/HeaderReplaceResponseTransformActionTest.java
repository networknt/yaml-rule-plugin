package com.networknt.rule.header;

import com.networknt.rule.RuleActionValue;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HeaderReplaceResponseTransformActionTest {
    /**
     * The test case to cover One header replace the other header in the response. The action will pick up the
     * source header value and put it into the targetHeader. The removeSourceHeader is true so that it should
     * be removed.
     */
    @Test
    public void testHeader2Header() {
        HeaderReplaceResponseTransformAction action = new HeaderReplaceResponseTransformAction();
        Map<String, Object> objMap = new HashMap<>();
        Map<String, Object> resultMap = new HashMap<>();
        List<RuleActionValue> actionValues = new ArrayList<>();

        Map<String, String> headerMap = new HashMap();
        headerMap.put("X-Test-1", "Token");
        objMap.put("responseHeaders", headerMap);

        RuleActionValue ruleActionValue1 = new RuleActionValue();
        ruleActionValue1.setActionValueId("sourceHeader");
        ruleActionValue1.setValue("X-Test-1");
        actionValues.add(ruleActionValue1);

        RuleActionValue ruleActionValue2 = new RuleActionValue();
        ruleActionValue2.setActionValueId("targetHeader");
        ruleActionValue2.setValue("My-Header");
        actionValues.add(ruleActionValue2);

        action.performAction(objMap, resultMap, actionValues);

        Assert.assertNotNull(resultMap);
        Map<String, Object> responseHeaders = (Map)resultMap.get("responseHeaders");
        Assert.assertNotNull(responseHeaders);
        // there should be one entry in the responseHeaders. One update the My-Header header with value "Token"
        Assert.assertEquals(1, responseHeaders.size());
        Map<String, Object> updateMap = (Map)responseHeaders.get("update");
        Assert.assertEquals("Token", updateMap.get("My-Header"));

    }

    /**
     * The test case to cover One response header value replaced by the passed in value. The action will pick up the
     * value and update or create the targetHeader.
     */
    @Test
    public void testValue2Header() {
        HeaderReplaceResponseTransformAction action = new HeaderReplaceResponseTransformAction();
        Map<String, Object> objMap = new HashMap<>();
        Map<String, Object> resultMap = new HashMap<>();
        List<RuleActionValue> actionValues = new ArrayList<>();

        // set the original oldToken in the Authorization header.
        Map<String, String> headerMap = new HashMap<>();
        headerMap.put("Authorization", "oldToken");
        objMap.put("responseHeaders", headerMap);

        // replace the Authorization header with targetValue newToken
        RuleActionValue ruleActionValue1 = new RuleActionValue();
        ruleActionValue1.setActionValueId("targetHeader");
        ruleActionValue1.setValue("Authorization");
        actionValues.add(ruleActionValue1);
        RuleActionValue ruleActionValue2 = new RuleActionValue();
        ruleActionValue2.setActionValueId("targetValue");
        ruleActionValue2.setValue("newToken");
        actionValues.add(ruleActionValue2);

        action.performAction(objMap, resultMap, actionValues);

        Assert.assertNotNull(resultMap);
        Map<String, Object> responseHeaders = (Map)resultMap.get("responseHeaders");
        Assert.assertNotNull(responseHeaders);
        // there should be one entry in the responseHeaders. One update the Authorization header with value "Token"
        Assert.assertEquals(1, responseHeaders.size());
        Map<String, Object> updateMap = (Map)responseHeaders.get("update");
        Assert.assertEquals("newToken", updateMap.get("Authorization"));
    }

    /**
     * The test case is similar to the above with the targetValue encrypted.
     */
    @Test
    @Ignore
    public void testEncryptedValue() {
        HeaderReplaceResponseTransformAction action = new HeaderReplaceResponseTransformAction();
        Map<String, Object> objMap = new HashMap<>();
        Map<String, Object> resultMap = new HashMap<>();
        List<RuleActionValue> actionValues = new ArrayList<>();

        // set the original oldToken in the Authorization header.
        Map<String, String> headerMap = new HashMap<>();
        headerMap.put("Authorization", "oldToken");
        objMap.put("responseHeaders", headerMap);

        // replace the Authorization header with targetValue newToken
        RuleActionValue ruleActionValue1 = new RuleActionValue();
        ruleActionValue1.setActionValueId("targetHeader");
        ruleActionValue1.setValue("Authorization");
        actionValues.add(ruleActionValue1);
        RuleActionValue ruleActionValue2 = new RuleActionValue();
        ruleActionValue2.setActionValueId("targetValue");
        ruleActionValue2.setValue("CRYPT:94069ad2905ea7a0a62bfdb0b7d1c590:c21c5c0980fc12c01a99fbc29ea40b2f");
        actionValues.add(ruleActionValue2);

        action.performAction(objMap, resultMap, actionValues);

        Assert.assertNotNull(resultMap);
        Map<String, Object> responseHeaders = (Map)resultMap.get("responseHeaders");
        Assert.assertNotNull(responseHeaders);
        // there should be one entry in the responseHeaders. One update the Authorization header with value "Token"
        Assert.assertEquals(1, responseHeaders.size());
        Map<String, Object> updateMap = (Map)responseHeaders.get("update");
        Assert.assertEquals("password", updateMap.get("Authorization"));
    }
}
