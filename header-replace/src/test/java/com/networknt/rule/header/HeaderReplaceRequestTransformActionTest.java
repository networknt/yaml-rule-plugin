package com.networknt.rule.header;

import com.networknt.rule.Rule;
import com.networknt.rule.RuleAction;
import com.networknt.rule.RuleActionValue;
import com.networknt.rule.RuleConstants;
import com.networknt.rule.RuleEngine;
import com.networknt.rule.RuleMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HeaderReplaceRequestTransformActionTest {

    @Test
    public void testLegacyYamlActionValuesReachPluginAsCollection() throws Exception {
        Map<String, Rule> rules = RuleMapper.string2RuleMap(readResource("rules-2.0.1-compat.yml"));
        Rule rule = rules.get("legacy-header-replace");
        RuleAction action = rule.getActions().iterator().next();

        Assertions.assertEquals("legacy-header-action", action.getActionId());
        Assertions.assertEquals(HeaderReplaceRequestTransformAction.class.getName(), action.getActionClassName());
        Assertions.assertInstanceOf(Collection.class, action.getActionValues());
        Assertions.assertEquals(List.of("targetHeader", "targetValue"), action.getActionValues().stream()
                .map(RuleActionValue::getActionValueId)
                .toList());
        Assertions.assertEquals(List.of("Authorization", "legacy-token"), action.getActionValues().stream()
                .map(RuleActionValue::getValue)
                .toList());
        Assertions.assertEquals("legacy-yaml", action.getParameters().get("source"));

        RuleEngine engine = new RuleEngine(rules, null);
        Map<String, Object> input = new HashMap<>();
        input.put("requestHeaders", new HashMap<>(Map.of("Authorization", "old-token")));

        Map<String, Object> result = engine.executeRule("legacy-header-replace", input);

        Assertions.assertEquals(Boolean.TRUE, result.get(RuleConstants.RESULT));
        Map<String, Object> requestHeaders = (Map<String, Object>) result.get("requestHeaders");
        Map<String, Object> updates = (Map<String, Object>) requestHeaders.get("update");
        Assertions.assertEquals("legacy-token", updates.get("Authorization"));
        Assertions.assertInstanceOf(HeaderReplaceRequestTransformAction.class,
                engine.actionClassCache.get(HeaderReplaceRequestTransformAction.class.getName()));
    }

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

        Map<String, String> headerMap = new HashMap<>();
        headerMap.put("Flink-Token", "Token");
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

        action.performAction("ruleId", "actionId", objMap, resultMap, actionValues);

        Assertions.assertNotNull(resultMap);
        Map<String, Object> requestHeaders = (Map)resultMap.get("requestHeaders");
        Assertions.assertNotNull(requestHeaders);
        // there should be two entries in the requestHeaders. One update the Authorization header with value "Token"
        Assertions.assertEquals(2, requestHeaders.size());
        Map<String, Object> updateMap = (Map)requestHeaders.get("update");
        Assertions.assertEquals("Token", updateMap.get("Authorization"));

        // and the other is to remove the header "Flink-Token"
        List<String> removeList = (List)requestHeaders.get("remove");
        Assertions.assertEquals(1, removeList.size());
        Assertions.assertEquals("Flink-Token", removeList.get(0));
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
        Map<String, String> headerMap = new HashMap<>();
        headerMap.put("Authorization", "oldToken");
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

        action.performAction("ruleId", "actionId", objMap, resultMap, actionValues);

        Assertions.assertNotNull(resultMap);
        Map<String, Object> requestHeaders = (Map)resultMap.get("requestHeaders");
        Assertions.assertNotNull(requestHeaders);
        // there should be two entries in the requestHeaders. One update the Authorization header with value "Token"
        Assertions.assertEquals(1, requestHeaders.size());
        Map<String, Object> updateMap = (Map)requestHeaders.get("update");
        Assertions.assertEquals("newToken", updateMap.get("Authorization"));
    }

    /**
     * The test case is similar to the above one but the targetValue is encrypted. The plugin should decrypt the value
     * and put into the targetHeader.
     */
    @Test
    @Disabled
    public void testEncryptedValue() {
        HeaderReplaceRequestTransformAction action = new HeaderReplaceRequestTransformAction();
        Map<String, Object> objMap = new HashMap<>();
        Map<String, Object> resultMap = new HashMap<>();
        List<RuleActionValue> actionValues = new ArrayList<>();

        // set the original oldToken in the Authorization header.
        Map<String, String> headerMap = new HashMap<>();
        headerMap.put("Authorization", "oldToken");
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

        action.performAction("ruleId", "actionId", objMap, resultMap, actionValues);

        Assertions.assertNotNull(resultMap);
        Map<String, Object> requestHeaders = (Map)resultMap.get("requestHeaders");
        Assertions.assertNotNull(requestHeaders);
        // there should be two entries in the requestHeaders. One update the Authorization header with value "Token"
        Assertions.assertEquals(1, requestHeaders.size());
        Map<String, Object> updateMap = (Map)requestHeaders.get("update");
        Assertions.assertEquals("password", updateMap.get("Authorization"));
    }

    private static String readResource(String resourceName) throws IOException {
        try (InputStream stream = HeaderReplaceRequestTransformActionTest.class.getClassLoader()
                .getResourceAsStream(resourceName)) {
            Assertions.assertNotNull(stream, resourceName);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

}
