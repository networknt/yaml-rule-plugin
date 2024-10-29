package com.networknt.rule.soap.transformer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.networknt.rule.soap.Constants;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;

import java.util.*;

public class EncodeTransformer extends Transformer {
    private static final int NODE_INDEX = 0;
    private static final int ENCODE_INDEX = 1;
    private static final int FORMAT_INDEX = 2;
    Map<String, EncodeInfo> encodeInfo;
    public EncodeTransformer(LinkedHashMap<String, Object> baseMap, String encodeRaw) {
        super(baseMap);
        this.finalizedObjectRequired = false;
        this.encodeInfo = EncodeTransformer.parseEncodeRuleString(encodeRaw);
    }

    private static Map<String, EncodeInfo> parseEncodeRuleString(String in) {
        Map<String, EncodeInfo> encodeMap = new HashMap<>();
        List<String> split = List.of(in.split(Constants.PROPERTY_SEPARATOR));

        for (String splitStr : split) {
            String[] pair = splitStr.split(Constants.ATTRIBUTE_SEPARATOR);
            String nodeKey = pair[NODE_INDEX].toLowerCase();
            String encodeType = pair[ENCODE_INDEX];
            String dataFormat = pair[FORMAT_INDEX];
            encodeMap.put(nodeKey, new EncodeInfo(encodeType, dataFormat));
        }
        return encodeMap;
    }

    /**
     * Get the string name of the transformer.
     *
     * @return - transformer name string.
     */
    public static String getId() {
        return EncodeTransformer.class.getSimpleName();
    }

    @Override
    public void init() {
        this.endTransitionState = Transformer.copy(this.base);
    }

    @Override
    public void doTransform() {
        this.encodeSubStruct(this.base, this.endTransitionState);
    }

    @SuppressWarnings("unchecked")
    private void encodeSubStruct(LinkedHashMap<String, Object> pojoMap, LinkedHashMap<String, Object> newMap) {
        for (Map.Entry<String, Object> baseMapEntry : pojoMap.entrySet()) {

            /* get our map key, value from the base pojoMap, and value from the copied map */
            String pojoMap_currentKey = baseMapEntry.getKey();
            Object newMap_currentValue = newMap.get(pojoMap_currentKey);
            Object pojoMap_currentValue = baseMapEntry.getValue();

            /* recursively parse sub elements if the value is of type map. */
            if (pojoMap_currentValue instanceof LinkedHashMap) {
                this.encodeSubStruct((LinkedHashMap<String, Object>) pojoMap_currentValue, (LinkedHashMap<String, Object>) newMap_currentValue);
                newMap.remove(pojoMap_currentKey);

                if (this.isEncodingRequired(pojoMap_currentKey)) {
                    Object newVal = this.getEncodedValue(pojoMap_currentKey, newMap_currentValue);
                    newMap.put(pojoMap_currentKey, newVal);
                } else {
                    newMap.put(pojoMap_currentKey, newMap_currentValue);
                }
            } else {
                newMap.remove(pojoMap_currentKey);
                newMap.put(pojoMap_currentKey, newMap_currentValue);
            }
        }
    }

    private Object getEncodedValue(String key, Object val) {
        try {
            return this.encodeInfo.get(key.toLowerCase()).applyEncode(val);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isEncodingRequired(String key) {
        return this.encodeInfo.containsKey(key.toLowerCase());
    }

    @Override
    public String getAsString() {
        try {
            return Transformer.JSON_MAPPER.writeValueAsString(this.endTransitionState);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    protected static class EncodeInfo {
        protected enum ENCODE_TYPE {
            HTML,
            URL,
            UNICODE,
            ACCENT_STRIP,
            BASE64,
            HEX,
            ASCII,
            NONE
        }

        protected enum FORMAT_TYPE {
            XML,
            JSON,
            YAML,
            NONE
        }

        private final ENCODE_TYPE encodeType;
        private final FORMAT_TYPE dataFormat;

        EncodeInfo(String encodeType, String dataFormat) {
            this.encodeType = getEncodeType(encodeType);
            this.dataFormat = getFormatType(dataFormat);
        }

        private static ENCODE_TYPE getEncodeType(String in) {
            try {
                return ENCODE_TYPE.valueOf(in);
            } catch (IllegalArgumentException e) {
                return ENCODE_TYPE.NONE;
            }
        }

        private static FORMAT_TYPE getFormatType(String in) {
            try {
                return FORMAT_TYPE.valueOf(in);
            } catch (IllegalArgumentException e) {
                return FORMAT_TYPE.NONE;
            }
        }

        private Object applyEncode(Object val) throws JsonProcessingException {
            Object returnObj;
            switch (this.dataFormat) {
                case XML:
                    Transformer.XML_MAPPER.configure(SerializationFeature.INDENT_OUTPUT, false);
                    ObjectNode xmlNode = (ObjectNode) Transformer.XML_MAPPER.convertValue(val, JsonNode.class);
                    returnObj = Transformer.XML_MAPPER.writeValueAsString(xmlNode);
                    Transformer.XML_MAPPER.configure(SerializationFeature.INDENT_OUTPUT, true);
                    break;
                case JSON:
                    Transformer.JSON_MAPPER.configure(SerializationFeature.INDENT_OUTPUT, false);
                    ObjectNode jsonNode = (ObjectNode) Transformer.JSON_MAPPER.convertValue(val, JsonNode.class);
                    returnObj = Transformer.JSON_MAPPER.writeValueAsString(jsonNode);
                    Transformer.JSON_MAPPER.configure(SerializationFeature.INDENT_OUTPUT, true);
                    break;
                case YAML:
                    Transformer.YAML_MAPPER.configure(SerializationFeature.INDENT_OUTPUT, false);
                    ObjectNode yamlNode = (ObjectNode) Transformer.YAML_MAPPER.convertValue(val, JsonNode.class);
                    returnObj = Transformer.YAML_MAPPER.writeValueAsString(yamlNode);
                    Transformer.YAML_MAPPER.configure(SerializationFeature.INDENT_OUTPUT, true);
                    break;
                case NONE:
                    Map<String, Object> noneNode = Transformer.JSON_MAPPER.convertValue(val, new TypeReference<Map<String, Object>>() {
                    });
                    return encodeObject(noneNode);
                default:
                    return null;
            }
            return this.encodeString((String)returnObj);
        }

        @SuppressWarnings("unchecked")
        private Object encodeObject(Map<String, Object> map) {
            for(var entry: map.entrySet()) {
                if(entry.getValue() instanceof String)
                    entry.setValue(this.encodeString((String)entry.getValue()));
                else if (entry.getValue() instanceof Map)
                    entry.setValue(encodeObject((Map<String, Object>)entry.getValue()));
                else if (entry.getValue() instanceof List)
                    entry.setValue(this.encodeArr((List<Object>)entry.getValue()));
            }
            return map;
        }

        @SuppressWarnings("unchecked")
        private Object encodeArr(List<Object> list) {
            for(int x = 0; x < list.size(); x++) {
                if(list.get(x) instanceof String)
                    list.set(x, this.encodeString((String) list.get(x)));
                else if (list.get(x) instanceof Map)
                    list.set(x, encodeObject((Map<String, Object>)list.get(x)));
                else if (list.get(x) instanceof List)
                    list.set(x, encodeArr((List<Object>)list.get(x)));
            }
            return list;
        }

        private String encodeString(String in) {
            switch (this.encodeType) {
                /* html encodes string */
                case HTML:
                    in = StringEscapeUtils.escapeHtml4(in);
                    break;
                /* converts string into base64 */
                case BASE64:
                    in = Base64.getEncoder().encodeToString(in.getBytes(StandardCharsets.UTF_8));
                    break;
                /* converts array of byte characters into hex codes */
                case HEX:
                    in = String.valueOf(Hex.encodeHex(in.getBytes(StandardCharsets.UTF_8)));
                    break;
                /* java standard url encoder translates string into application/x-www-urlencoded format */
                case URL:
                    in = URLEncoder.encode(in, StandardCharsets.UTF_8);
                    break;
                /* apache common strip accent replaces accented characters to non-accented equivalent */
                case ACCENT_STRIP:
                    in = StringUtils.stripAccents(in);
                    break;
                case UNICODE:
                case ASCII:
                case NONE:
                default:
                    return in;
            }
            return in;
        }
    }
}
