package com.networknt.rule.generic.token.schema;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.networknt.config.PathPrefixAuth;

import java.util.Map;

public class UpdateSchema extends PathPrefixAuthReadSchema {

    public enum UpdateDirection {

        @JsonProperty("REQUEST")
        REQUEST,

        @JsonProperty("RESPONSE")
        RESPONSE
    }

    @JsonProperty("direction")
    private UpdateDirection direction;

    @JsonProperty("headers")
    protected Map<String, String> headers;

    @JsonProperty("body")
    protected Map<String, String> body;

    public UpdateDirection getDirection() {
        return direction;
    }
    public Map<String, String> getHeaders() {
        return headers;
    }
    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }
    public Map<String, String> getBody() {
        return body;
    }
    public void setBody(Map<String, String> body) {
        this.body = body;
    }

    @Override
    public void writeSchemaFromPathPrefix(PathPrefixAuth pathPrefixAuth) {
        PathPrefixAuthReadSchema.updateMapFromPathPrefix(this.headers, pathPrefixAuth);
        PathPrefixAuthReadSchema.updateMapFromPathPrefix(this.body, pathPrefixAuth);
    }
}
