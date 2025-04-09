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
        action.performAction("ruleId", "actionId", objMap, resultMap, null);
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
        action.performAction("ruleId", "actionId", objMap, resultMap, null);
        String updatedBody = (String) resultMap.get("requestBody");
        Assert.assertEquals(expectedBody, updatedBody);
    }

    @Test
    public void testISO2UTFEncoding() throws Exception{
        String str = "Hello, world! çáéíóú ÇÁÉÍÓÚ";
        System.out.println("original string: " + str);
        byte[] isoBytes = str.getBytes("ISO-8859-1");
        // this is the byte array we are dealing with. Convert back to string.
        String isoString = new String(isoBytes, "ISO-8859-1");
        System.out.println("ISO-8859-1 string: " + isoString);
        byte[] utf8Bytes = isoString.getBytes();
        String utf8String = new String(utf8Bytes);
        System.out.println("UTF-8 string: " + utf8String);
    }
}
