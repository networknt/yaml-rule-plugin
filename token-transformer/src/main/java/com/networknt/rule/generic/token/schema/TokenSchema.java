package com.networknt.rule.generic.token.schema;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.networknt.config.PathPrefixAuth;

public class TokenSchema {
    @JsonProperty("pathPrefixAuth")
    private PathPrefixAuth pathPrefixAuth;

    @JsonProperty("request")
    private RequestSchema tokenRequest;

    @JsonProperty("source")
    private SourceSchema tokenSource;

    @JsonProperty("update")
    private UpdateSchema tokenUpdate;

    public PathPrefixAuth getPathPrefixAuth() {
        return pathPrefixAuth;
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

    public void setTokenRequest(RequestSchema tokenRequest) {
        this.tokenRequest = tokenRequest;
    }

    public void setTokenUpdate(UpdateSchema tokenUpdate) {
        this.tokenUpdate = tokenUpdate;
    }
}
