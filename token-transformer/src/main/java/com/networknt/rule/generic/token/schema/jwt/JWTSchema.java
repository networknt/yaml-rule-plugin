package com.networknt.rule.generic.token.schema.jwt;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.networknt.rule.generic.token.schema.cert.KeyStoreSchema;
import com.networknt.rule.generic.token.schema.jwt.JwtPartialSchema;

public class JWTSchema {

    @JsonProperty("jwtTtl")
    private long jwtTtl;

    @JsonProperty("keyStore")
    private KeyStoreSchema keyStore;

    @JsonProperty("algorithm")
    private String algorithm;

    @JsonProperty("jwtHeader")
    private JwtPartialSchema jwtHeader;

    @JsonProperty("jwtBody")
    private JwtPartialSchema jwtBody;

    public long getJwtTtl() {
        return jwtTtl;
    }

    public KeyStoreSchema getKeyStore() {
        return keyStore;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public JwtPartialSchema getJwtHeader() {
        return jwtHeader;
    }

    public JwtPartialSchema getJwtBody() {
        return jwtBody;
    }
}
