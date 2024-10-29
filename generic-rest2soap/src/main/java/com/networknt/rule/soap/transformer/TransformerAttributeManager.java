package com.networknt.rule.soap.transformer;

import com.networknt.rule.soap.Constants;

import java.util.*;

public class TransformerAttributeManager {
    private final Map<String, AttributeInfo> attributesMap;

    public TransformerAttributeManager(String raw) {
        this.attributesMap = TransformerAttributeManager.parseAttributeRuleString(raw);
    }

    public String getPrefix(String field) {
        String prefix = "";
        AttributeInfo ai = this.attributesMap.get(field);
        if (ai != null) {
            for (Attribute a : ai.al) {
                if (a.at == Attribute.ATTRIBUTE_TYPE.PREFIX) {
                    prefix = a.v + ":";
                }
            }
        }
        return prefix;
    }

    public AttributeInfo getInfo(String f) {
        return this.attributesMap.get(f);
    }

    public boolean isRootNode(String f) {
        var ai = this.attributesMap.get(f);
        if (ai != null) {
            return ai.hasRootNode();
        } else {
            return false;
        }

    }

    public boolean hasRootNode() {
        for (var e : this.attributesMap.entrySet()) {
            if (e.getValue().hasRootNode()) {
                return true;
            }
        }
        return false;
    }

    public boolean hasXmlHeader() {
        if (this.attributesMap.get(Constants.SOAP_HEADER) != null) {
            return this.attributesMap.get(Constants.SOAP_HEADER).hasXmlHeader();
        } else {
            return false;
        }

    }

    public boolean hasXmlDeclare() {
        return this.attributesMap.get(Constants.SOAP_ENVELOPE).hasXmlDeclare();
    }

    public String getXmlVersion() {
        return this.attributesMap.get(Constants.SOAP_ENVELOPE).xmlVersion();
    }

    public String getXmlEncoding() {
        return this.attributesMap.get(Constants.SOAP_ENVELOPE).xmlEncoding();
    }

    /**
     * This parses the raw string given in the rules.yml file.
     * In the future, when we are able to support more types other than string,
     * we can change to a map type structure instead of a single string line.
     *
     * @param in - string in value.
     * @return - map of field key and value is another map of attributes to be applied.
     */
    private static Map<String, AttributeInfo> parseAttributeRuleString(String in) {
        Map<String, AttributeInfo> attributeMap = new HashMap<>();
        List<String> fields = List.of(in.split(","));
        for (String field : fields) {
            String[] attributes = field.split(Constants.ATTRIBUTE_SEPARATOR);
            String fieldName = attributes[0];
            AttributeInfo ai = new AttributeInfo();
            for (int x = 1; x < attributes.length;) {
                ai.add(attributes[x], attributes[x+1]);
                x=x+2;
            }
            attributeMap.put(fieldName, ai);
        }
        return attributeMap;
    }

    public static class AttributeInfo {
        private final List<Attribute> al = new ArrayList<>();
        public void add(String k, String v) {
            this.al.add(new Attribute(k, v));
        }
        public void pop() {
            this.al.remove(this.al.size() - 1);
        }
        public void remove(String k) {
            int x = 0;
            for (Attribute a : al) {
                if (a.k.equalsIgnoreCase(k)) {
                    al.remove(x);
                    return;
                }
                x++;
            }
        }

        public List<Attribute> getAttributeList() {
            return al;
        }

        private boolean hasRootNode() {
            Attribute att = this.getAttribute(Attribute.ATTRIBUTE_TYPE.ROOT_NODE);
            return att != null;
        }

        private boolean hasXmlHeader() {
            Attribute att = this.getAttribute(Attribute.ATTRIBUTE_TYPE.XML_HEADER);
            return att != null;
        }

        private boolean hasXmlDeclare() {
            Attribute att = this.getAttribute(Attribute.ATTRIBUTE_TYPE.XML_DECLARE);
            return att != null;
        }

        private String xmlVersion() {
            Attribute att = this.getAttribute(Attribute.ATTRIBUTE_TYPE.XML_VERSION);
            if (att != null) {
                return att.v;
            }
            return "";
        }

        private String xmlEncoding() {
            Attribute att = this.getAttribute(Attribute.ATTRIBUTE_TYPE.XML_ENCODING);
            if (att != null) {
                return att.v;
            }
            return "";
        }

        private Attribute getAttribute(Attribute.ATTRIBUTE_TYPE att) {
            for (var a : al) {
                if (a.at.equals(att)) {
                    return a;
                }
            }
            return null;
        }
    }

    public static class Attribute {
        private final String k;
        private final String v;
        private final ATTRIBUTE_TYPE at;

        public enum ATTRIBUTE_TYPE {
            PREFIX,
            XML_HEADER,
            XML_DECLARE,
            XML_VERSION,
            XML_ENCODING,
            ROOT_NODE,
            ATTRIBUTE
        }

        Attribute(String k, String v) {
            this.k = k;
            this.v = v;
            this.at = this.setAttributeType(k);
        }

        private ATTRIBUTE_TYPE setAttributeType(String in) {
            if (in.equalsIgnoreCase("$prefix")) {
                return ATTRIBUTE_TYPE.PREFIX;
            } else if (in.equalsIgnoreCase("$xmlHeader")) {
                return ATTRIBUTE_TYPE.XML_HEADER;
            } else if (in.equalsIgnoreCase("$xmlDeclare")) {
                return ATTRIBUTE_TYPE.XML_DECLARE;
            } else if (in.equalsIgnoreCase("$xmlVersion")) {
                return ATTRIBUTE_TYPE.XML_VERSION;
            } else if (in.equalsIgnoreCase("$xmlEncoding")) {
                return ATTRIBUTE_TYPE.XML_ENCODING;
            } else if (in.equalsIgnoreCase("$rootNode")) {
                return ATTRIBUTE_TYPE.ROOT_NODE;
            } else {
                return ATTRIBUTE_TYPE.ATTRIBUTE;
            }
        }

        public String getKey() {
            return k;
        }

        public String getValue() {
            return v;
        }

        public ATTRIBUTE_TYPE getAttributeType() {
            return at;
        }
    }
}
