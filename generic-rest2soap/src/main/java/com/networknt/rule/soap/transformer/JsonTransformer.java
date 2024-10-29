package com.networknt.rule.soap.transformer;

import com.fasterxml.jackson.core.JsonProcessingException;
import java.util.LinkedHashMap;

public class JsonTransformer extends Transformer {
    private TransformerAttributeManager attributeManager;
    private boolean hasRootNode = false;

    public JsonTransformer(LinkedHashMap<String, Object> baseMap, String displayFormatString) {
        super(baseMap);
        this.attributeManager = new TransformerAttributeManager(displayFormatString);
        this.finalizedObjectRequired = true;
    }

    public static String getId() {
        return JsonTransformer.class.getSimpleName();
    }

    @Override
    public void init() {
        this.hasRootNode = this.attributeManager.hasRootNode();
        this.endTransitionState = Transformer.copy(this.base);
    }

    @Override
    public void doTransform() {
        if (this.hasRootNode) {
            this.handleRootNode(this.base, this.endTransitionState);
        }
    }

    @SuppressWarnings("unchecked")
    private void handleRootNode(LinkedHashMap<String, Object> pojoMap, LinkedHashMap<String, Object> newMap) {
        for (var e : pojoMap.entrySet()) {
            String field = e.getKey();
            if (this.attributeManager.isRootNode(field) && e.getValue() instanceof LinkedHashMap) {
                newMap.clear();
                newMap.put(field, e.getValue());
            } else if (e.getValue() instanceof LinkedHashMap) {
                this.handleRootNode((LinkedHashMap<String, Object>) e.getValue(), newMap);
            }
        }
    }

    @Override
    public String getAsString() {
        this.finalizeObject();
        try {
            String returnString = Transformer.JSON_MAPPER.writeValueAsString(this.finalizedObject);
            return returnString.trim();
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
