package com.networknt.rule;

import com.networknt.soap.SoapSecurityTransformAction;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class SoapSecurityTransformActionTest {
    @Test
    public void testActionWithoutSpaces() {
        String xml = "<?xml version='1.0'?><soapenv:Header><soapenv:Header/></soapenv:Header>";
        Map<String, Object> objMap = new HashMap<>();
        objMap.put("requestBody", xml);
        SoapSecurityTransformAction action = new SoapSecurityTransformAction();
        Map<String, Object> resultMap = new HashMap<>();
        action.performAction(objMap, resultMap, null);
        System.out.println(resultMap);
    }

    @Test
    public void testActionWithSpaces() {
        String xml = "<?xml version='1.0'?><soapenv:Header>  <soapenv:Header/>  </soapenv:Header>";
        Map<String, Object> objMap = new HashMap<>();
        objMap.put("requestBody", xml);
        SoapSecurityTransformAction action = new SoapSecurityTransformAction();
        Map<String, Object> resultMap = new HashMap<>();
        action.performAction(objMap, resultMap, null);
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
}
