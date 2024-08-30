package com.networknt.rule.generic.token.schema;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TokenSchema {
    @JsonProperty("sharedVariables")
    private SharedVariableSchema sharedVariables;

    @JsonProperty("request")
    private RequestSchema tokenRequest;

    @JsonProperty("source")
    private SourceSchema tokenSource;

    @JsonProperty("update")
    private UpdateSchema tokenUpdate;

    public SharedVariableSchema getSharedVariables() {
        return sharedVariables;
    }

    public RequestSchema getTokenRequest() {
        return tokenRequest;
    }

    public SourceSchema getTokenSource() {
        return tokenSource;
    }

    public UpdateSchema getTokenUpdate() {
        return tokenUpdate;
    }
}
