package com.networknt.rule.generic.token.schema.cert;

import com.fasterxml.jackson.annotation.JsonProperty;

public class KeyStoreSchema {

    @JsonProperty("name")
    private String name;

    @JsonProperty("password")
    private String password;

    @JsonProperty("alias")
    private String alias;

    @JsonProperty("aliasPass")
    private String aliasPass;

    public String getName() {
        return name;
    }

    public String getPassword() {
        return password;
    }

    public String getAlias() {
        return alias;
    }

    public String getAliasPass() {
        return aliasPass;
    }


}
