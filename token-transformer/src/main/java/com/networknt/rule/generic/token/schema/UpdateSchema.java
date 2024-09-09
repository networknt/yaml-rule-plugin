package com.networknt.rule.generic.token.schema;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class UpdateSchema extends SharedVariableRead {

    private static final Logger LOG = LoggerFactory.getLogger(UpdateSchema.class);

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

    public Map<String, String> getResolvedHeaders(final SharedVariableSchema sharedVariableSchema) {

        if (this.headers == null) {
            LOG.trace("No headers defined in update schema.");
            return  new HashMap<>();
        }

        final var resolvedHeaders = new HashMap<>(this.headers);
        SharedVariableRead.updateMapFromSharedVariables(resolvedHeaders, sharedVariableSchema);
        return resolvedHeaders;
    }

    public Map<String, String> getResolvedBody(final SharedVariableSchema sharedVariableSchema) {

        if (this.body == null) {
            LOG.trace("No body defined in update schema.");
            return  new HashMap<>();
        }

        final var resolvedBody = new HashMap<>(this.body);
        SharedVariableRead.updateMapFromSharedVariables(resolvedBody, sharedVariableSchema);
        return resolvedBody;
    }
}
