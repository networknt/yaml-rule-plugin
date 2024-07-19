package com.networknt.rule.encoder;

import org.junit.Assert;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class RequestBodyUtf8EncodingTransformActionTest {
    @Test
    public void testBodyWithDeclaration() throws Exception {
        String originalBody = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?><root>Café</root>";
        String expectedBody = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><root>Café</root>";
        Map<String, Object> objMap = new HashMap<>();
        objMap.put("requestBody", originalBody);
        RequestBodyUtf8EncodingTransformAction action = new RequestBodyUtf8EncodingTransformAction();
        Map<String, Object> resultMap = new HashMap<>();
        action.performAction(objMap, resultMap, null);
        String updatedBody = (String) resultMap.get("requestBody");
        Assert.assertEquals(expectedBody, updatedBody);
    }

    @Test
    public void testISO2UTF8WithoutDeclaration() {
        String originalBody = "<?xml version=\"1.0\"?><root>Café</root>";
        String expectedBody = "<?xml version=\"1.0\"?><root>Café</root>";
        Map<String, Object> objMap = new HashMap<>();
        objMap.put("requestBody", originalBody);
        RequestBodyUtf8EncodingTransformAction action = new RequestBodyUtf8EncodingTransformAction();
        Map<String, Object> resultMap = new HashMap<>();
        action.performAction(objMap, resultMap, null);
        String updatedBody = (String) resultMap.get("requestBody");
        Assert.assertEquals(expectedBody, updatedBody);
    }
}
