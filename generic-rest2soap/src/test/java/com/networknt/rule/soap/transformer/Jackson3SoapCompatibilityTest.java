package com.networknt.rule.soap.transformer;

import com.networknt.rule.soap.SoapMappers;
import com.networknt.rule.soap.Util;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.SerializationFeature;

import java.math.BigDecimal;
import java.util.LinkedHashMap;

import static org.junit.jupiter.api.Assertions.*;

class Jackson3SoapCompatibilityTest {
    @Test
    void soapResponseRetainsAttributesRepeatedElementsAndEmptyValues() {
        assertEquals("{\"id\":\"7\",\"item\":[\"one\",\"two\"],\"empty\":\"\"}",
                Util.transformSoap2Rest("<items id=\"7\"><item>one</item><item>two</item><empty/></items>"));
    }

    @Test
    void malformedResponsesStillPassThroughUnchanged() {
        assertEquals("not xml", Util.transformSoap2Rest("not xml"));
        assertEquals("<broken>", Util.transformSoap2Rest("<broken>"));
    }

    @Test
    void encodingUsesCompactWritersWithoutChangingSharedMapperSettings() {
        for (String format : new String[]{"XML", "JSON", "YAML"}) {
            LinkedHashMap<String, Object> child = new LinkedHashMap<>();
            child.put("amount", new BigDecimal("1.2300"));
            LinkedHashMap<String, Object> input = new LinkedHashMap<>();
            input.put("payload", child);
            EncodeTransformer transformer = new EncodeTransformer(input, "payload@@@BASE64@@@" + format);
            transformer.init();
            transformer.doTransform();
            assertNotNull(transformer.getAsString());
            assertTrue(Transformer.XML_MAPPER.isEnabled(SerializationFeature.INDENT_OUTPUT));
            assertTrue(Transformer.JSON_MAPPER.isEnabled(SerializationFeature.INDENT_OUTPUT));
            assertTrue(Transformer.YAML_MAPPER.isEnabled(SerializationFeature.INDENT_OUTPUT));
        }
    }

    @Test
    void formatMappersRetainJackson2StreamLimits() {
        for (var mapper : new tools.jackson.databind.ObjectMapper[]{
                SoapMappers.xmlBuilder().build(), SoapMappers.yamlBuilder().build(), Transformer.JSON_MAPPER}) {
            assertEquals(1000, mapper.tokenStreamFactory().streamReadConstraints().getMaxNestingDepth());
            assertEquals(20_000_000, mapper.tokenStreamFactory().streamReadConstraints().getMaxStringLength());
            assertEquals(1000, mapper.tokenStreamFactory().streamWriteConstraints().getMaxNestingDepth());
        }
        assertDoesNotThrow(() -> Transformer.XML_MAPPER.readTree("<a>".repeat(600) + "0" + "</a>".repeat(600)));
        assertDoesNotThrow(() -> Transformer.YAML_MAPPER.readTree("[".repeat(600) + "0" + "]".repeat(600)));
    }
}
