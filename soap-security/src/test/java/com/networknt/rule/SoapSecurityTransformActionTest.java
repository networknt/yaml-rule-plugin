package com.networknt.rule;

import com.networknt.soap.SoapSecurityTransformAction;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class SoapSecurityTransformActionTest {
    @Test
    public void testAction() {
        String xml = "<?xml version='1.0'?><soapenv:Header><soapenv:Header/></soapenv:Header>";
        Map<String, Object> objMap = new HashMap<>();
        objMap.put("requestBody", xml);
        SoapSecurityTransformAction action = new SoapSecurityTransformAction();
        Map<String, Object> resultMap = new HashMap<>();
        action.performAction(objMap, resultMap, null);
        System.out.println(resultMap);
    }
}
