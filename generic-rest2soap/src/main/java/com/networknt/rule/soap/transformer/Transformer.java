package com.networknt.rule.soap.transformer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.networknt.rule.RuleActionValue;

import java.util.LinkedHashMap;

public abstract class Transformer {

    protected LinkedHashMap<String, Object> base;
    protected LinkedHashMap<String, Object> endTransitionState;
    protected Transformer n;
    protected Transformer p;
    protected JsonNode finalizedObject;
    protected boolean finalizedObjectRequired;

    protected final static YAMLMapper YAML_MAPPER = new YAMLMapper();
    static {
        YAML_MAPPER.configure(SerializationFeature.INDENT_OUTPUT, true);
    }

    protected final static XmlMapper XML_MAPPER = new XmlMapper();
    static {
        XML_MAPPER.configure(SerializationFeature.INDENT_OUTPUT, true);
        XML_MAPPER.configure(ToXmlGenerator.Feature.WRITE_XML_DECLARATION, false);
        XML_MAPPER.configure(ToXmlGenerator.Feature.UNWRAP_ROOT_OBJECT_NODE, true);
    }
    protected final static ObjectMapper JSON_MAPPER = new ObjectMapper();
    static {
        JSON_MAPPER.configure(SerializationFeature.INDENT_OUTPUT, true);
    }

    /**
     * Base constructor, all transformers require a base map.
     * @param baseMap - base map struct
     */
    public Transformer(LinkedHashMap<String, Object> baseMap) {
        this.base = baseMap;
    }

    /**
     * Tells us if the transformer requires the object to be finalized or not.
     *
     * @return - true if finalized object required.
     */
    protected boolean isFinalizedObjectRequired() {
        return this.finalizedObjectRequired;
    }

    /**
     * Execute the transformers transform operation.
     */
    public abstract void doTransform();

    /**
     * Gets the final object in string form.
     *
     * @return - final object in string form.
     */
    public abstract String getAsString();

    /**
     * Any kind of initialization process that needs to
     * happen before we can do the transform happens here first.
     */
    public abstract void init();

    /**
     * Get the string name of the transformer.
     *
     * @return - transformer name string.
     */
    public static String getId() {
        return Transformer.class.getSimpleName();
    }

    /**
     * Does a deep copy of the base hashmap.
     * @param baseMap base map to be copied.
     * @return - returns an exact copy of our hashmap.
     */
    protected static LinkedHashMap<String, Object> copy(LinkedHashMap<String, Object> baseMap) {
        JsonNode jsonNode = Transformer.JSON_MAPPER.convertValue(baseMap, JsonNode.class);
        JsonNode copiedNode = jsonNode.deepCopy();
        TypeReference<LinkedHashMap<String, Object>> t = new TypeReference<>() {};
        return Transformer.JSON_MAPPER.convertValue(copiedNode, t);
    }

    protected static Transformer getTransformerFromId(RuleActionValue actionValue, LinkedHashMap<String, Object> baseMap) {
        String tid = actionValue.getActionValueId();
       if (tid.equalsIgnoreCase(XmlTransformer.getId())) {
           return new XmlTransformer(baseMap, actionValue.getValue());
       } else if (tid.equalsIgnoreCase(EncodeTransformer.getId())) {
           return new EncodeTransformer(baseMap, actionValue.getValue());
       } else if (tid.equalsIgnoreCase(JsonTransformer.getId())) {
           return new JsonTransformer(baseMap, actionValue.getValue());
       } else {
           return null;
       }
    }

    /**
     * Transfer the result of one transformer, to the start of the next transformer.
     */
    protected void passToNextTransformer() {
        if (this.n != null) {
            this.n.base = this.endTransitionState;
        }
    }

    /**
     * Default write object.
     * Takes the final transition map, and writes it to the ObjectNode.
     */
    protected void finalizeObject() {
        ObjectNode newNode = Transformer.JSON_MAPPER.createObjectNode();
        newNode = newNode.putPOJO("ROOT_NODE", this.endTransitionState);
        this.finalizedObject = newNode.get("ROOT_NODE");
    }
}
