package com.networknt.rule.generic.token.schema.jwt;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.networknt.rule.generic.token.schema.TtlUnit;
import com.networknt.rule.generic.token.schema.cert.KeyStoreSchema;

public class JWTSchema {

    @JsonProperty("jwtTtl")
    private long jwtTtl;

    @JsonProperty("ttlUnit")
    @JsonSetter(nulls = Nulls.SKIP)
    private TtlUnit ttlUnit = TtlUnit.SECOND;

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

    public TtlUnit getTtlUnit() {
        return ttlUnit;
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
