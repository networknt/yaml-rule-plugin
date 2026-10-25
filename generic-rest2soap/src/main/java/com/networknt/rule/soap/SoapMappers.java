package com.networknt.rule.soap;

import com.networknt.jackson.Jackson2Defaults;
import tools.jackson.dataformat.xml.XmlFactory;
import tools.jackson.dataformat.xml.XmlMapper;
import tools.jackson.dataformat.yaml.YAMLFactory;
import tools.jackson.dataformat.yaml.YAMLMapper;

/** Applies the framework's measured Jackson 2 policy to SOAP format mappers. */
public final class SoapMappers {
    private static final XmlMapper XML_MAPPER = xmlBuilder().build();

    private SoapMappers() { }

    public static XmlMapper xmlMapper() {
        return XML_MAPPER;
    }

    public static XmlMapper.Builder xmlBuilder() {
        var factory = Jackson2Defaults.configure(XmlFactory.builder()).build();
        return Jackson2Defaults.configure(XmlMapper.builder(factory));
    }

    public static YAMLMapper.Builder yamlBuilder() {
        var factory = Jackson2Defaults.configure(YAMLFactory.builder()).build();
        return Jackson2Defaults.configure(YAMLMapper.builder(factory));
    }
}
