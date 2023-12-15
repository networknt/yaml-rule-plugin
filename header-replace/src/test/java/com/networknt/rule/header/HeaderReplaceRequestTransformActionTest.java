package com.networknt.rule.header;

import com.networknt.rule.RuleActionValue;
import com.networknt.rule.header.HeaderReplaceRequestTransformAction;
import io.undertow.util.HeaderMap;
import io.undertow.util.HttpString;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HeaderReplaceRequestTransformActionTest {
    /**
     * The test case to cover One header replace the other header. The action will pick up the source header
     * value and put it into the targetHeader. The removeSourceHeader is true so that it should be removed.
     */
    @Test
    public void testHeader2Header() {
        HeaderReplaceRequestTransformAction action = new HeaderReplaceRequestTransformAction();
        Map<String, Object> objMap = new HashMap<>();
        Map<String, Object> resultMap = new HashMap<>();
        List<RuleActionValue> actionValues = new ArrayList<>();

        HeaderMap headerMap = new HeaderMap();
        headerMap.add(new HttpString("Flink-Token"), "Token");
        objMap.put("requestHeaders", headerMap);

        RuleActionValue ruleActionValue1 = new RuleActionValue();
        ruleActionValue1.setActionValueId("sourceHeader");
        ruleActionValue1.setValue("Flink-Token");
        actionValues.add(ruleActionValue1);
        RuleActionValue ruleActionValue2 = new RuleActionValue();
        ruleActionValue2.setActionValueId("targetHeader");
        ruleActionValue2.setValue("Authorization");
        actionValues.add(ruleActionValue2);
        RuleActionValue ruleActionValue3 = new RuleActionValue();
        ruleActionValue3.setActionValueId("removeSourceHeader");
        ruleActionValue3.setValue("true");
        actionValues.add(ruleActionValue3);

        action.performAction(objMap, resultMap, actionValues);

        Assert.assertNotNull(resultMap);
        Map<String, Object> requestHeaders = (Map)resultMap.get("requestHeaders");
        Assert.assertNotNull(requestHeaders);
        // there should be two entries in the requestHeaders. One update the Authorization header with value "Token"
        Assert.assertEquals(2, requestHeaders.size());
        Map<String, Object> updateMap = (Map)requestHeaders.get("update");
        Assert.assertEquals("Token", updateMap.get("Authorization"));

        // and the other is to remove the header "Flink-Token"
        List<String> removeList = (List)requestHeaders.get("remove");
        Assert.assertEquals(1, removeList.size());
        Assert.assertEquals("Flink-Token", removeList.get(0));
    }

    /**
     * The test case to cover One header value replaced by the passed in value. The action will pick up the
     * value and update or create the targetHeader.
     */
    @Test
    public void testValue2Header() {
        HeaderReplaceRequestTransformAction action = new HeaderReplaceRequestTransformAction();
        Map<String, Object> objMap = new HashMap<>();
        Map<String, Object> resultMap = new HashMap<>();
        List<RuleActionValue> actionValues = new ArrayList<>();

        // set the original oldToken in the Authorization header.
        HeaderMap headerMap = new HeaderMap();
        headerMap.add(new HttpString("Authorization"), "oldToken");
        objMap.put("requestHeaders", headerMap);

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
        Map<String, Object> requestHeaders = (Map)resultMap.get("requestHeaders");
        Assert.assertNotNull(requestHeaders);
        // there should be two entries in the requestHeaders. One update the Authorization header with value "Token"
        Assert.assertEquals(1, requestHeaders.size());
        Map<String, Object> updateMap = (Map)requestHeaders.get("update");
        Assert.assertEquals("newToken", updateMap.get("Authorization"));
    }

    /**
     * The test case is similar to the above one but the targetValue is encrypted. The plugin should decrypt the value
     * and put into the targetHeader.
     */
    @Test
    @Ignore
    public void testEncryptedValue() {
        HeaderReplaceRequestTransformAction action = new HeaderReplaceRequestTransformAction();
        Map<String, Object> objMap = new HashMap<>();
        Map<String, Object> resultMap = new HashMap<>();
        List<RuleActionValue> actionValues = new ArrayList<>();

        // set the original oldToken in the Authorization header.
        HeaderMap headerMap = new HeaderMap();
        headerMap.add(new HttpString("Authorization"), "oldToken");
        objMap.put("requestHeaders", headerMap);

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
        Map<String, Object> requestHeaders = (Map)resultMap.get("requestHeaders");
        Assert.assertNotNull(requestHeaders);
        // there should be two entries in the requestHeaders. One update the Authorization header with value "Token"
        Assert.assertEquals(1, requestHeaders.size());
        Map<String, Object> updateMap = (Map)requestHeaders.get("update");
        Assert.assertEquals("password", updateMap.get("Authorization"));
    }

}
