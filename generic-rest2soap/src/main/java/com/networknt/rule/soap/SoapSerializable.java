package com.networknt.rule.soap;

import com.networknt.rule.soap.transformer.TransformerAttributeManager;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializable;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Serializes JSON to SOAP XML with attributes.
 *
 * @author Kalev Gonvick
 */
public class SoapSerializable implements JsonSerializable {

    private LinkedHashMap<String, Object> baseMap;
    private TransformerAttributeManager.AttributeInfo attributes;

    @Override
    public void serialize(JsonGenerator gen, SerializerProvider serializers) throws IOException {
        ToXmlGenerator toXmlGenerator = (ToXmlGenerator) gen;
        toXmlGenerator.writeStartObject();
        writeAttributes(toXmlGenerator);
        writeMap(toXmlGenerator);
        toXmlGenerator.writeEndObject();

    }

    /**
     * Write attributes associated with the rootNode.
     *
     * @param toXmlGenerator - xmlgen
     * @throws IOException -
     */
    private void writeAttributes(ToXmlGenerator toXmlGenerator) throws IOException {
        if (this.attributes != null) {
            for(TransformerAttributeManager.Attribute a : this.attributes.getAttributeList()) {
                switch (a.getAttributeType()) {
                    case ATTRIBUTE:
                        toXmlGenerator.setNextIsAttribute(true);
                        toXmlGenerator.writeFieldName(a.getKey());
                        toXmlGenerator.writeString(a.getValue());
                        toXmlGenerator.setNextIsAttribute(false);
                        break;
                    case PREFIX:
                    case XML_HEADER:
                    case XML_DECLARE:
                    default:
                        break;

                }
            }
        }
    }

    /**
     * Write the fields in our map.
     *
     * @param toXmlGenerator - xmlgen
     * @throws IOException -
     */
    private void writeMap(ToXmlGenerator toXmlGenerator) throws IOException {
        for (Map.Entry<String, Object> e: this.baseMap.entrySet()) {
            toXmlGenerator.writeObjectField(e.getKey(), e.getValue());
        }
    }

    @Override
    public void serializeWithType(JsonGenerator gen, SerializerProvider serializers, TypeSerializer typeSer) throws IOException {
        serialize(gen, serializers);
    }

    public void setBaseMap(LinkedHashMap<String, Object> baseMap) {
        this.baseMap = baseMap;
    }

    public void setAttributes(TransformerAttributeManager.AttributeInfo attributes) {
        this.attributes = attributes;
    }
}
