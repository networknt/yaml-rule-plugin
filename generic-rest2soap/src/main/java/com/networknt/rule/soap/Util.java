package com.networknt.rule.soap;

import java.io.*;
import java.util.Collection;
import java.util.LinkedHashMap;

import com.networknt.rule.soap.transformer.TransformChain;
import com.networknt.rule.soap.transformer.XmlTransformer;
import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.networknt.rule.RuleActionValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Util {
    private static final Logger logger = LoggerFactory.getLogger(Util.class);

    public static String transformRest2Soap(String input, Collection<RuleActionValue> actionValues) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        String returnString = input;

        try {
            TypeReference<LinkedHashMap<String, Object>> typeReference = new TypeReference<>() {};
            LinkedHashMap<String, Object> result = mapper.readValue(input, typeReference);
            LinkedHashMap<String, Object> envelope = XmlTransformer.createBasicSoapBody(result);
            TransformChain transformManager = TransformChain.createNewChain(envelope, actionValues);
            returnString = transformManager.getResultString();
        } catch (JsonProcessingException e) {
            if(logger.isDebugEnabled())
                logger.debug("Failed to transform response body: {}\n{}", input, e.getMessage());
            return returnString;
        }
        return returnString;
    }

    public static String transformSoap2Rest(String input) throws JsonProcessingException {
        XmlMapper xmlMapper = new XmlMapper();
        ObjectMapper objectMapper = new ObjectMapper();
        String output;
        try {
            JsonNode jsonNode = xmlMapper.readTree(input);
            output = objectMapper.writeValueAsString(jsonNode);
            return output;
        } catch (JsonProcessingException e) {
            // if we receive something that is not valid SOAP
            // (i.e. error from proxy) we just return the string.
            if (logger.isTraceEnabled())
                logger.trace("Returning original response because conversion failed with exception: {}", e.getMessage());
            return input;
        }
    }

}
