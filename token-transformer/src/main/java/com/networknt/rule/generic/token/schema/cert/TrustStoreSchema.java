package com.networknt.rule.generic.token.schema.cert;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TrustStoreSchema {

    @JsonProperty("name")
    private String name;

    @JsonProperty("password")
    private char[] password;

    @JsonProperty("algorithm")
    private String algorithm;

    public String getName() {
        return name;
    }

    public char[] getPassword() {
        return password;
    }

    public String getAlgorithm() {
        return algorithm;
    }
}
