package com.networknt.rule.sanitizer;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class BodySanitizerTransformActionTest {
    @Test
    public void testActionWithoutTag() {
        String json = "{\"name\":\"dog\",\"id\":112}";
        Map<String, Object> objMap = new HashMap<>();
        objMap.put("requestBody", json);
        BodySanitizerTransformAction action = new BodySanitizerTransformAction();
        Map<String, Object> resultMap = new HashMap<>();
        action.performAction(objMap, resultMap, null);
        System.out.println(resultMap);
    }

    @Test
    public void testActionWithTag() {
        //String json = "{\"name\":\"<script>alert`dog`</script>\",\"id\":112}";
        String json = "{\"name\":\"<script>alert('$varUnsafe')</script>\",\"id\":112}";
        Map<String, Object> objMap = new HashMap<>();
        objMap.put("requestBody", json);
        BodySanitizerTransformAction action = new BodySanitizerTransformAction();
        Map<String, Object> resultMap = new HashMap<>();
        action.performAction(objMap, resultMap, null);
        System.out.println(resultMap);
    }
}
