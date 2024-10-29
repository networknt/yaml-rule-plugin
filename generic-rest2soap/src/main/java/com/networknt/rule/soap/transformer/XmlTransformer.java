package com.networknt.rule.soap.transformer;

import com.networknt.rule.soap.Constants;
import com.networknt.rule.soap.SoapSerializable;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.*;

/**
 * This transforms JSON to SOAP XML with the addition of being able to specify attributes to be added to the XML fields.
 * In the future, a lot of this can be made more generic. see [0006-CustomTransform]
 *
 * @author Kalev Gonvick
 */
public class XmlTransformer extends Transformer {

    private final static String ROOT_START = "<POJONode>";
    private final static String ROOT_END = "</POJONode>";
    private final static String XML_VERSION_DEFAULT = "1.0";
    private final static String XML_ENCODING_DEFAULT = "utf-8";
    private final static String XML_DECLARE_START = "<?xml";
    private final static String XML_DECLARE_END = "?>\n";
    private final static String XML_DECLARE_VERSION = "version=";
    private final static String XML_DECLARE_ENCODING = "encoding=";
    private final TransformerAttributeManager attributeManager;
    private boolean addXmlDeclare = false;
    private String xmlVersion = XML_VERSION_DEFAULT;
    private String xmlEncoding = XML_ENCODING_DEFAULT;

    public XmlTransformer(LinkedHashMap<String, Object> baseMap, String rawAttrString) {
        super(baseMap);
        this.attributeManager = new TransformerAttributeManager(rawAttrString);
        this.finalizedObjectRequired = true;
    }

    public static String getId() {
        return XmlTransformer.class.getSimpleName();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void init() {
        this.addXmlDeclare = this.attributeManager.hasXmlDeclare();
        if (!this.attributeManager.hasXmlHeader()) {
           LinkedHashMap<String, Object> map = ((LinkedHashMap<String, Object>) this.base.get(Constants.SOAP_ENVELOPE));
           if (map != null) {
               map.remove(Constants.SOAP_HEADER);
           }
        }
        this.endTransitionState = Transformer.copy(this.base);
    }

    /**
     * Does the transformation steps to our hashmap.
     */
    @Override
    public void doTransform() {
        this.setupSerializers(this.base, this.endTransitionState);
    }

    @Override
    protected void finalizeObject() {
        ObjectNode newNode = Transformer.XML_MAPPER.createObjectNode();
        newNode = newNode.putPOJO("ROOT_NODE", this.endTransitionState);
        this.finalizedObject = newNode.get("ROOT_NODE");
    }

    /**
     * Converts our XML to String format. We also have to trim the outer node.
     *
     * @return - xml in string form.
     */
    @Override
    public String getAsString() {
        this.finalizeObject();
        try {
            String returnString = Transformer.XML_MAPPER.writeValueAsString(this.finalizedObject);
            return this.fixXmlFormat(returnString).trim();
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * TODO: this method cleans up nodes in final XML object, but does it in a very messy way right now.
     *
     * @param in - string in.
     * @return - formatted string
     */
    private String fixXmlFormat(String in) {
        String o = in.substring(ROOT_START.length(), in.indexOf(ROOT_END)).trim();
        StringBuilder fs = new StringBuilder();
        this.removeSpacePadding(fs, o);
        if (this.addXmlDeclare) {
            StringBuilder xd = new StringBuilder();
            xd.append(XML_DECLARE_START).append(" ");
            xd.append(XML_DECLARE_VERSION).append("\"").append(xmlVersion).append("\"").append(" ");
            xd.append(XML_DECLARE_ENCODING).append("\"").append(xmlEncoding).append("\"");
            xd.append(XML_DECLARE_END);
            fs.insert(0, xd);
        }
        o = fs.toString().replace("&amp;", "&");
        return o;
    }

    private void removeSpacePadding(StringBuilder fs, String o) {
        Collection<String> split = List.of(o.split(System.lineSeparator()));
        int x = 0;
        for (String index : split) {
            if (index.startsWith("  ")) {
                fs.append(index.substring(2));
            } else {
                fs.append(index);
            }
            if (!(x == split.size() - 1)) {
                fs.append("\n");
            }
            x++;
        }
    }


    /**
     * For structures found inside out HashMap, we need to replace them with SoapSerializable structures instead.
     * This is so we can inject XML attributes into our converted JSON to XML object.
     *
     * @param pojoMap - base pojo map (JSON request converted to HashMap)
     */
    @SuppressWarnings("unchecked")
    private void setupSerializers(LinkedHashMap<String, Object> pojoMap, LinkedHashMap<String, Object> newMap) {
        for (Map.Entry<String, Object> member : pojoMap.entrySet()) {

            /* get our map key, value from the base pojoMap, and value from the copied map */
            String pojoMap_currentKey = member.getKey();
            Object copyMap_currentValue = newMap.get(pojoMap_currentKey);
            Object pojoMap_currentValue = member.getValue();

            TransformerAttributeManager.AttributeInfo attributes = this.getAttributesFromField(pojoMap_currentKey);

            /* recursively parse sub elements if the value is of type map. */
            if (pojoMap_currentValue instanceof LinkedHashMap) {

                this.setupSerializers((LinkedHashMap<String, Object>) pojoMap_currentValue, (LinkedHashMap<String, Object>) copyMap_currentValue);
                String prefix = this.checkPrefix(pojoMap_currentKey);
                newMap.remove(pojoMap_currentKey);
                newMap.put(prefix + pojoMap_currentKey, this.wrap((LinkedHashMap<String, Object>) copyMap_currentValue, attributes));

            /* everything else handled here */
            } else {

                String prefix = this.checkPrefix(pojoMap_currentKey);
                newMap.remove(pojoMap_currentKey);
                newMap.put(prefix + pojoMap_currentKey, copyMap_currentValue);
            }
        }
    }

    private String checkPrefix(String field) {
        return this.attributeManager.getPrefix(field);
    }

    /**
     * Get all attributes from attribute field map key.
     *
     * @param k - current key that contains all attributes.
     * @return - returns a Map of attributes (name & value)
     */
    private TransformerAttributeManager.AttributeInfo getAttributesFromField(String k) {
        return this.attributeManager.getInfo(k);
    }

    private SoapSerializable wrap(LinkedHashMap<String, Object> bMap, TransformerAttributeManager.AttributeInfo attributes) {
        SoapSerializable soapSerializable = new SoapSerializable();
        soapSerializable.setBaseMap(bMap);
        soapSerializable.setAttributes(attributes);
        return soapSerializable;
    }

    public static LinkedHashMap<String, Object> createBasicSoapHeader(Object header) {
        return createBasicSoapBody(null, header);
    }

    public static LinkedHashMap<String, Object> createBasicSoapBody(Object body) {
        return createBasicSoapBody(body, null);
    }

    public static LinkedHashMap<String, Object> createBasicSoapBody() {
        return createBasicSoapBody(null, null);
    }

    public static LinkedHashMap<String, Object> createBasicSoapBody(Object body, Object header) {
        /* create schema transform */
        LinkedHashMap<String, Object> envelopeValue = new LinkedHashMap<>();
        envelopeValue.put(Constants.SOAP_HEADER, header);
        envelopeValue.put(Constants.SOAP_BODY, body);
        LinkedHashMap<String, Object> envelope = new LinkedHashMap<>();
        envelope.put(Constants.SOAP_ENVELOPE, envelopeValue);
        return envelope;
    }


}
