package com.networknt.rule.generic.token.schema;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;

import java.util.Map;

public class UpdateSchema extends SharedVariableRead {

    public enum UpdateDirection {

        @JsonProperty("REQUEST")
        @JsonAlias({"request", "Request"})
        REQUEST,

        @JsonProperty("RESPONSE")
        @JsonAlias({"response", "Response"})
        RESPONSE
    }

    @JsonProperty("direction")
    private UpdateDirection direction;

    @JsonProperty("headers")
    protected Map<String, String> headers;

    @JsonProperty("body")
    protected Map<String, String> body;

    @JsonProperty("updateExpirationFromTtl")
    @JsonSetter(nulls = Nulls.SKIP)
    private boolean updateExpirationFromTtl = true;

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

    public boolean isUpdateExpirationFromTtl() {
        return updateExpirationFromTtl;
    }

    /**
     * Takes in a sharedVariableSchema object and populates the header and body fields that use the !ref keyword.
     *
     * @param sharedVariableSchema - the provided SharedVariableSchema containing the data.
     */
    @Override
    public void writeSchemaFromSharedVariables(final SharedVariableSchema sharedVariableSchema) {
        SharedVariableRead.updateMapFromSharedVariables(this.headers, sharedVariableSchema);
        SharedVariableRead.updateMapFromSharedVariables(this.body, sharedVariableSchema);
    }
}
