package com.networknt.rule.soap;

import com.networknt.rule.soap.transformer.TransformerAttributeManager;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.JacksonSerializable;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.jsontype.TypeSerializer;
import tools.jackson.dataformat.xml.ser.ToXmlGenerator;
import tools.jackson.core.JacksonException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Serializes JSON to SOAP XML with attributes.
 *
 * @author Kalev Gonvick
 */
public class SoapSerializable implements JacksonSerializable {

    private LinkedHashMap<String, Object> baseMap;
    private TransformerAttributeManager.AttributeInfo attributes;

    @Override
    public void serialize(JsonGenerator gen, SerializationContext serializers) throws JacksonException {
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
     * @throws JacksonException -
     */
    private void writeAttributes(ToXmlGenerator toXmlGenerator) throws JacksonException {
        if (this.attributes != null) {
            for(TransformerAttributeManager.Attribute a : this.attributes.getAttributeList()) {
                switch (a.getAttributeType()) {
                    case ATTRIBUTE:
                        toXmlGenerator.setNextIsAttribute(true);
                        toXmlGenerator.writeName(a.getKey());
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
     * @throws JacksonException -
     */
    private void writeMap(ToXmlGenerator toXmlGenerator) throws JacksonException {
        for (Map.Entry<String, Object> e: this.baseMap.entrySet()) {
            toXmlGenerator.writePOJOProperty(e.getKey(), e.getValue());
        }
    }

    @Override
    public void serializeWithType(JsonGenerator gen, SerializationContext serializers, TypeSerializer typeSer) throws JacksonException {
        serialize(gen, serializers);
    }

    public void setBaseMap(LinkedHashMap<String, Object> baseMap) {
        this.baseMap = baseMap;
    }

    public void setAttributes(TransformerAttributeManager.AttributeInfo attributes) {
        this.attributes = attributes;
    }
}
