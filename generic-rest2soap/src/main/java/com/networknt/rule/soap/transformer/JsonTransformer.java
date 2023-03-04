package com.networknt.rule.soap.transformer;

import java.util.LinkedHashMap;

public class JsonTransformer extends Transformer {

    public JsonTransformer(LinkedHashMap<String, Object> baseMap, String displayFormatString) {
        super(baseMap);
        this.finalizedObjectRequired = true;
    }

    public static String getId() {
        return JsonTransformer.class.getSimpleName();
    }

    @Override
    public void init() {
        this.endTransitionState = Transformer.copy(this.base);
    }

    @Override
    public void doTransform() {
        // TODO: move transforms to here
    }

    @Override
    public String getAsString() {
        return null;
    }
}
