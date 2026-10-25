package com.networknt.rule.soap;

import com.networknt.config.Jackson2Defaults;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.core.StreamWriteFeature;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.cfg.MapperBuilder;
import tools.jackson.dataformat.xml.XmlFactory;
import tools.jackson.dataformat.xml.XmlMapper;
import tools.jackson.dataformat.yaml.YAMLFactory;
import tools.jackson.dataformat.yaml.YAMLMapper;

/** Applies the framework's measured Jackson 2 policy to the SOAP format mappers. */
public final class SoapMappers {
    private SoapMappers() { }

    public static XmlMapper.Builder xmlBuilder() {
        var factory = XmlFactory.builder().configureForJackson2()
                .streamReadConstraints(Jackson2Defaults.mapper().tokenStreamFactory().streamReadConstraints())
                .streamWriteConstraints(Jackson2Defaults.mapper().tokenStreamFactory().streamWriteConstraints()).build();
        return apply(XmlMapper.builder(factory));
    }

    public static YAMLMapper.Builder yamlBuilder() {
        var factory = YAMLFactory.builder().configureForJackson2()
                .streamReadConstraints(Jackson2Defaults.mapper().tokenStreamFactory().streamReadConstraints())
                .streamWriteConstraints(Jackson2Defaults.mapper().tokenStreamFactory().streamWriteConstraints()).build();
        return apply(YAMLMapper.builder(factory));
    }

    private static <M extends ObjectMapper, B extends MapperBuilder<M, B>> B apply(B builder) {
        var defaults = Jackson2Defaults.mapper();
        builder.configureForJackson2();
        for (MapperFeature feature : MapperFeature.values()) {
            builder.configure(feature, defaults.isEnabled(feature));
        }
        for (SerializationFeature feature : SerializationFeature.values()) {
            builder.configure(feature, defaults.isEnabled(feature));
        }
        for (DeserializationFeature feature : DeserializationFeature.values()) {
            builder.configure(feature, defaults.isEnabled(feature));
        }
        for (StreamReadFeature feature : StreamReadFeature.values()) {
            builder.configure(feature, defaults.tokenStreamFactory().isEnabled(feature));
        }
        for (StreamWriteFeature feature : StreamWriteFeature.values()) {
            builder.configure(feature, defaults.tokenStreamFactory().isEnabled(feature));
        }
        return builder;
    }
}
