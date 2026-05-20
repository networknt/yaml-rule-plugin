package com.networknt.rule;

import com.networknt.config.Config;
import com.networknt.soap.SoapSecurityTransformAction;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SoapSecurityTransformActionTest {
    @Test
    public void testActionWithoutSpaces() {
        String xml = "<?xml version='1.0'?><soapenv:Header><soapenv:Header/></soapenv:Header>";
        Map<String, Object> objMap = new HashMap<>();
        objMap.put("requestBody", xml);
        SoapSecurityTransformAction action = new SoapSecurityTransformAction();
        Map<String, Object> resultMap = new HashMap<>();
        action.performAction("ruleId", "actionId", objMap, resultMap, Collections.emptyList());
        System.out.println(resultMap);
    }

    @Test
    public void testActionWithSpaces() {
        String xml = "<?xml version='1.0'?><soapenv:Header>  <soapenv:Header/>  </soapenv:Header>";
        Map<String, Object> objMap = new HashMap<>();
        objMap.put("requestBody", xml);
        SoapSecurityTransformAction action = new SoapSecurityTransformAction();
        Map<String, Object> resultMap = new HashMap<>();
        action.performAction("ruleId", "actionId", objMap, resultMap, Collections.emptyList());
        System.out.println(resultMap);
    }

    @Test
    public void testXmlReplace() {
        String xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?><soapenv:Header>Hello, World!</soapenv:Header>";
        String replacementText = "Goodbye, World!";

        // Construct the regex pattern
        String pattern = "<soapenv:Header>(.*?)</soapenv:Header>";

        // Replace the text content using regex
        String modifiedXml = xml.replaceAll(pattern, "<soapenv:Header>" + replacementText + "</soapenv:Header>");

        System.out.println(modifiedXml);
    }

    @Test
    public void testActionSimpleHeader() {
        String xml = "<?xml version='1.0'?><soapenv:Header/>";
        Map<String, Object> objMap = new HashMap<>();
        objMap.put("requestBody", xml);
        SoapSecurityTransformAction action = new SoapSecurityTransformAction();
        Map<String, Object> resultMap = new HashMap<>();
        action.performAction("ruleId", "actionId", objMap, resultMap, Collections.emptyList());
        System.out.println(resultMap);
    }

    @Test
    public void testGeneratedSecurityUsesRandomNonceAndSha256Digest() throws Exception {
        String xml = "<?xml version='1.0'?><soapenv:Header></soapenv:Header>";
        Map<String, Object> objMap = new HashMap<>();
        objMap.put("requestBody", xml);
        SoapSecurityTransformAction action = new SoapSecurityTransformAction();
        Map<String, Object> resultMap = new HashMap<>();

        action.performAction("ruleId", "actionId", objMap, resultMap, Collections.emptyList());

        String modifiedBody = (String) resultMap.get("requestBody");
        String nonce = extractElementValue(modifiedBody, "wsse:Nonce");
        byte[] nonceBytes = Base64.getDecoder().decode(nonce);
        assertEquals(32, nonceBytes.length);
        assertFalse(new String(nonceBytes, StandardCharsets.UTF_8).matches("\\d+"));

        String created = extractElementValue(modifiedBody, "wsu:Created");
        String passwordDigest = extractElementValue(modifiedBody, "wsse:Password");
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update(nonceBytes);
        digest.update(created.getBytes(StandardCharsets.UTF_8));
        String password = (String) Config.getInstance().getJsonMapConfigNoCache("cannex").get("password");
        String expectedDigest = Base64.getEncoder().encodeToString(digest.digest(password.getBytes(StandardCharsets.UTF_8)));
        assertEquals(expectedDigest, passwordDigest);
    }

    private String extractElementValue(String xml, String elementName) {
        Pattern pattern = Pattern.compile("<" + elementName + "(?:\\s[^>]*)?>(.*?)</" + elementName + ">", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(xml);
        assertTrue(matcher.find(), "Missing element " + elementName);
        return matcher.group(1);
    }

}
