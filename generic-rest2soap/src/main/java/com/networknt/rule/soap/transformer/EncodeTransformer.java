package com.networknt.rule.soap.transformer;

import com.networknt.rule.soap.Constants;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.text.StringEscapeUtils;

import java.util.*;

public class EncodeTransformer extends Transformer {
    Map<String, EncodeInfo> encodeInfo;
    public EncodeTransformer(LinkedHashMap<String, Object> baseMap, String encodeRaw) {
        super(baseMap);
        this.finalizedObjectRequired = false;
        this.encodeInfo = EncodeTransformer.parseEncodeRuleString(encodeRaw);
    }

    private static Map<String, EncodeInfo> parseEncodeRuleString(String in) {
        Map<String, EncodeInfo> encodeMap = new HashMap<>();
        List<String> split = List.of(in.split(","));
        for (String splitStr : split) {
            String[] pair = splitStr.split(Constants.ATTRIBUTE_SEPARATOR);
            String nodeKey = pair[0];
            String encodeType = pair[1];
            String dataFormat = pair[2];
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
                    String newVal = this.getEncodedValue(pojoMap_currentKey, newMap_currentValue);
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

    private String getEncodedValue(String key, Object val) {
        try {
            return this.encodeInfo.get(key).encode(val);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isEncodingRequired(String key) {
        return this.encodeInfo.containsKey(key);
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

        private String encode(Object val) throws JsonProcessingException {
            String returnString;
            switch (this.dataFormat) {
                case XML:
                    Transformer.XML_MAPPER.configure(SerializationFeature.INDENT_OUTPUT, false);
                    ObjectNode xmlNode = (ObjectNode) Transformer.XML_MAPPER.convertValue(val, JsonNode.class);
                    returnString = Transformer.XML_MAPPER.writeValueAsString(xmlNode);
                    Transformer.XML_MAPPER.configure(SerializationFeature.INDENT_OUTPUT, true);
                    break;
                case JSON:
                    Transformer.JSON_MAPPER.configure(SerializationFeature.INDENT_OUTPUT, false);
                    ObjectNode jsonNode = (ObjectNode) Transformer.JSON_MAPPER.convertValue(val, JsonNode.class);
                    returnString = Transformer.JSON_MAPPER.writeValueAsString(jsonNode);
                    Transformer.JSON_MAPPER.configure(SerializationFeature.INDENT_OUTPUT, true);
                    break;
                case YAML:
                    // TODO
                case NONE:
                    // TODO
                default:
                    return null;
            }
            return this.encode(returnString);
        }

        private String encode(String in) {
            String returnString = in;
            switch (this.encodeType) {
                case HTML:
                    returnString = StringEscapeUtils.escapeHtml4(returnString);
                    break;
                case BASE64:
                    returnString = Base64.getEncoder().encodeToString(returnString.getBytes());
                    break;
                case HEX:
                    returnString = String.valueOf(Hex.encodeHex(returnString.getBytes()));
                    break;
                case URL:
                    // TODO
                case UNICODE:
                    // TODO
                case ASCII:
                    // TODO
                case NONE:
                    // TODO
                default:
                    return null;
            }
            return returnString;
        }
    }
}
