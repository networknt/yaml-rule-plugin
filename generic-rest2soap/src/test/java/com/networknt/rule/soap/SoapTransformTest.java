package com.networknt.rule.soap;

import com.networknt.rule.RuleActionValue;
import org.junit.Assert;
import org.junit.Test;


import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

public class SoapTransformTest {

	private String fixSystemIndents(String in) {
		return in.replaceAll("\r", "").replaceAll("\n", "").trim();
	}

    /**
     * Test if REST json is getting transformed to XML SOAP as example provided
     */
    @Test
    public void LabProxy_rest2soap() {
		String xmlTransformerId = "XmlTransformer";
		String xmlTransformerValue = "DeliverExamOneContentResponse@@@xmlns@@@http://QuestWebServices/EOService,Envelope@@@$prefix@@@soap@@@xmlns:xsd@@@http://www.w3.org/2001/XMLSchema@@@xmlns:xsi@@@http://www.w3.org/2001/XMLSchema-instance@@@xmlns:soap@@@http://schemas.xmlsoap.org/soap/envelope/@@@$xmlDeclare@@@true,Body@@@$prefix@@@soap,Header@@@$prefix@@@soap@@@$xmlHeader@@@true";
		String encodeTransformerId = "EncodeTransformer";
		String encodeTransformerValue = "DeliverExamOneContentResult@@@HTML@@@XML";

		Collection<RuleActionValue> actionValues = new ArrayList<>();

		RuleActionValue xmlActionValue = new RuleActionValue();
		xmlActionValue.setActionValueId(xmlTransformerId);
		xmlActionValue.setValue(xmlTransformerValue);

		RuleActionValue encodeActionValue = new RuleActionValue();
		encodeActionValue.setActionValueId(encodeTransformerId);
		encodeActionValue.setValue(encodeTransformerValue);

		actionValues.add(encodeActionValue);
		actionValues.add(xmlActionValue);

		String example = "{\n" +
				"\"DeliverExamOneContentResponse\": {\n" +
    			"\"DeliverExamOneContentResult\" : {\n" +
				"\"ReturnValues\": {\n" +
    			"\"TransactionID\" : \"4aef1b48-7342-4f93-9314-ed7b93bfc1ee\",\n" +
    			"\"ResponseCode\" : \"1\",\n" +
    			"\"ResponseCodeText\" : \"Successfully received message\",\n" +
    			"\"ResponseMessage\": \"\"}\n" +
				"}\n" +
    			"}\n" +
				"}\n" +
    			"";
    	String expectedSoapResponse = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
				"<soap:Envelope xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
				"  <soap:Header/>\n" +
				"  <soap:Body>\n" +
				"    <DeliverExamOneContentResponse xmlns=\"http://QuestWebServices/EOService\">\n" +
				"      <DeliverExamOneContentResult>&lt;ReturnValues&gt;&lt;TransactionID&gt;4aef1b48-7342-4f93-9314-ed7b93bfc1ee&lt;/TransactionID&gt;&lt;ResponseCode&gt;1&lt;/ResponseCode&gt;&lt;ResponseCodeText&gt;Successfully received message&lt;/ResponseCodeText&gt;&lt;ResponseMessage&gt;&lt;/ResponseMessage&gt;&lt;/ReturnValues&gt;</DeliverExamOneContentResult>\n" +
				"    </DeliverExamOneContentResponse>\n" +
				"  </soap:Body>\n" +
				"</soap:Envelope>";
    	String response = "";
    	try {
			response = Util.transformRest2Soap(example, actionValues);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	Assert.assertEquals(this.fixSystemIndents(expectedSoapResponse), this.fixSystemIndents(response));
    }

	@Test
	public void EvidenceAnalyzer_rest2soap() {
		String xmlTransformerId = "XmlTransformer";
		String xmlTransformerValue = "Envelope@@@$prefix@@@SOAP-ENV@@@xmlns:SOAP-ENV@@@http://schemas.xmlsoap.org/soap/envelope/,Body@@@$prefix@@@SOAP-ENV,AddRequirementRequestBody@@@$prefix@@@ea@@@xmlns:ea@@@http://www.w3.org/2001/XMLSchema";

		Collection<RuleActionValue> actionValues = new ArrayList<>();

		RuleActionValue xmlActionValue = new RuleActionValue();
		xmlActionValue.setActionValueId(xmlTransformerId);
		xmlActionValue.setValue(xmlTransformerValue);

		actionValues.add(xmlActionValue);


		String example = "{\n" +
				"\"AddRequirementRequestBody\": {\n" +
				"\"caseId\": \"1\",\n" +
				"\"underwritableEntityType\": \"life\",\n" +
				"\"underwritableEntityInstance\": \"1\",\n" +
				"\"requirementCode\": \"BLOODCHEMISTRYPROFILE\"\n" +
				"}\n" +
				"}";
		String expected = "<SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\">\n" +
				"  <SOAP-ENV:Body>\n" +
				"    <ea:AddRequirementRequestBody xmlns:ea=\"http://www.w3.org/2001/XMLSchema\">\n" +
				"      <caseId>1</caseId>\n" +
				"      <underwritableEntityType>life</underwritableEntityType>\n" +
				"      <underwritableEntityInstance>1</underwritableEntityInstance>\n" +
				"      <requirementCode>BLOODCHEMISTRYPROFILE</requirementCode>\n" +
				"    </ea:AddRequirementRequestBody>\n" +
				"  </SOAP-ENV:Body>\n" +
				"</SOAP-ENV:Envelope>";
		String request = "";
		try {
			request = Util.transformRest2Soap(example, actionValues);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		Assert.assertEquals(expected, request);

	}
}
