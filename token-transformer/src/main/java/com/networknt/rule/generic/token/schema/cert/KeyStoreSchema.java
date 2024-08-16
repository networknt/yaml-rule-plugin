package com.networknt.rule.generic.token.schema.cert;

import com.fasterxml.jackson.annotation.JsonProperty;

public class KeyStoreSchema {

    @JsonProperty("name")
    private String name;

    @JsonProperty("password")
    private char[] password;

    @JsonProperty("alias")
    private String alias;

    @JsonProperty("keyPass")
    private char[] keyPass;

    @JsonProperty("algorithm")
    private String algorithm;

    public String getName() {
        return name;
    }

    public char[] getPassword() {
        return password;
    }

    public String getAlias() {
        return alias;
    }

    public char[] getKeyPass() {
        return keyPass;
    }

    public String getAlgorithm() {
        return algorithm;
    }
}
