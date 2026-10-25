package com.networknt.rule.soap;

import com.networknt.jackson.Jackson2Defaults;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.core.StreamWriteFeature;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.SerializationFeature;

import static org.junit.jupiter.api.Assertions.*;

class SoapMapperPolicyTest {
    @Test
    void formatBuildersRetainCommonFeaturesAndStreamLimits() {
        var expected = Jackson2Defaults.mapper();
        var differences = new java.util.ArrayList<String>();
        for (var actual : new tools.jackson.databind.ObjectMapper[]{
                SoapMappers.xmlMapper(), SoapMappers.yamlBuilder().build()}) {
            for (var feature : MapperFeature.values())
                if (expected.isEnabled(feature) != actual.isEnabled(feature))
                    differences.add(actual.getClass().getSimpleName() + ":" + feature.name() + "=" + actual.isEnabled(feature));
            for (var feature : SerializationFeature.values())
                if (expected.isEnabled(feature) != actual.isEnabled(feature))
                    differences.add(actual.getClass().getSimpleName() + ":" + feature.name() + "=" + actual.isEnabled(feature));
            for (var feature : DeserializationFeature.values())
                if (expected.isEnabled(feature) != actual.isEnabled(feature))
                    differences.add(actual.getClass().getSimpleName() + ":" + feature.name() + "=" + actual.isEnabled(feature));
            for (var feature : StreamReadFeature.values())
                if (expected.isEnabled(feature) != actual.isEnabled(feature))
                    differences.add(actual.getClass().getSimpleName() + ":" + feature.name() + "=" + actual.isEnabled(feature));
            for (var feature : StreamWriteFeature.values())
                if (expected.isEnabled(feature) != actual.isEnabled(feature))
                    differences.add(actual.getClass().getSimpleName() + ":" + feature.name() + "=" + actual.isEnabled(feature));
            assertEquals(1000, actual.tokenStreamFactory().streamReadConstraints().getMaxNestingDepth());
            assertEquals(20_000_000, actual.tokenStreamFactory().streamReadConstraints().getMaxStringLength());
            assertEquals(1000, actual.tokenStreamFactory().streamWriteConstraints().getMaxNestingDepth());
        }
        assertEquals(java.util.List.of(), differences);
    }

    @Test
    void sharedXmlMapperHandlesConcurrentResponsesAndMalformedInput() throws Exception {
        try (var executor = Executors.newFixedThreadPool(4)) {
            var tasks = IntStream.range(0, 32).mapToObj(index -> (Callable<String>) () ->
                    Util.transformSoap2Rest("<reply><value>" + index + "</value></reply>")).toList();
            var results = executor.invokeAll(tasks, 10, TimeUnit.SECONDS);
            for (int index = 0; index < results.size(); index++) {
                assertFalse(results.get(index).isCancelled(), "SOAP conversion timed out");
                assertEquals("{\"value\":\"" + index + "\"}", results.get(index).get());
            }
        }
        assertEquals("upstream unavailable", Util.transformSoap2Rest("upstream unavailable"));
    }
}
