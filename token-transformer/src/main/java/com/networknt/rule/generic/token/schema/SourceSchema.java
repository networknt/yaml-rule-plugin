package com.networknt.rule.generic.token.schema;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.networknt.config.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.List;

public class SourceSchema extends SharedVariableWrite {
    private static final Logger LOG = LoggerFactory.getLogger(SourceSchema.class);
    @JsonProperty("headers")
    private List<SourceDestinationDefinition> headers;

    @JsonProperty("body")
    private List<SourceDestinationDefinition> body;

    @JsonProperty("expirationSchema")
    private ExpirationSchema expirationSchema;

    public List<SourceDestinationDefinition> getHeaders() {
        return headers;
    }

    public List<SourceDestinationDefinition> getBody() {
        return body;
    }

    public ExpirationSchema getExpirationSchema() {
        return expirationSchema;
    }

    public void setHeaders(List<SourceDestinationDefinition> headers) {
        this.headers = headers;
    }

    public void setBody(List<SourceDestinationDefinition> body) {
        this.body = body;
    }

    private void writeHeadersToSharedVariables(final SharedVariableSchema sharedVariableSchema, final HttpResponse<?> response) {
        final var headerMap = new HashMap<String, Object>();

        for (var header : response.headers().map().entrySet()) {

            if (!header.getValue().isEmpty()) {

                final var builder = new StringBuilder();

                for (int x = 0; x < header.getValue().size(); x++) {
                    builder.append(header.getValue().get(x));

                    if (header.getValue().size() > 1 && x < header.getValue().size() - 1)
                        builder.append(",");

                }

                final var completeHeaderValue = builder.toString();

                if (!completeHeaderValue.isEmpty())
                    headerMap.put(header.getKey(), completeHeaderValue);
            }
        }

        if (!headerMap.isEmpty())
            SharedVariableWrite.writeToSharedVariables(sharedVariableSchema, headerMap, this.headers);
    }

    private void writeExpirationToSharedVariables(final SharedVariableSchema sharedVariableSchema, final HttpResponse<?> response) {

        switch (this.expirationSchema.location) {

            case HEADER:
                final var expirationString = response.headers().map().get(this.expirationSchema.field).stream().findFirst();

                if (expirationString.isPresent()) {
                    final var responseExpiration = Long.parseLong(expirationString.get());
                    sharedVariableSchema.setExpiration(this.expirationSchema.ttlUnit.unitToMillis(responseExpiration));

                } else LOG.error("Could not find '{}' contained in the headers of the token response.", this.expirationSchema.field);

                break;

            case BODY:
                final var bodyMap = JsonMapper.string2Map(response.body().toString());

                if (bodyMap.containsKey(this.expirationSchema.field)) {
                    final var responseExpiration = Long.parseLong(String.valueOf(bodyMap.get(this.expirationSchema.field)));
                    sharedVariableSchema.setExpiration(this.expirationSchema.ttlUnit.unitToMillis(responseExpiration));

                } else LOG.error("Could not find '{}' contained in the body of the token response.", this.expirationSchema.field);

                break;
            default:
                throw new IllegalStateException("Invalid location configured in ./source/expirationSchema: '" + this.expirationSchema.location + "'.");
        }
    }

    public void writeResponseToSharedVariables(final SharedVariableSchema sharedVariableSchema, final HttpResponse<?> response) {

        /* write headers values from response to sharedVariables */
        this.writeHeadersToSharedVariables(sharedVariableSchema, response);

        /* write body values from response to sharedVariables */
        this.writeJsonStringToSharedVariables(sharedVariableSchema, response.body().toString(), this.body);

        /* if an expiration schema is specified, grab it from the response and convert it to milliseconds. */
        if (this.expirationSchema != null)
            this.writeExpirationToSharedVariables(sharedVariableSchema, response);

    }

    /**
     * Overwrites shared variable values with new ones based on source-destinations mappings.
     *
     * @param sharedVariableSchema - object holding our variables.
     * @param jsonString - raw source of data
     * @param sourceDestinationMapping - mapping that defines what values we are getting from the raw data source and what shared variable it will be saved to.
     */
    public void writeJsonStringToSharedVariables(final SharedVariableSchema sharedVariableSchema, final String jsonString, List<SourceDestinationDefinition> sourceDestinationMapping) {
        final var dataSourceMap = JsonMapper.string2Map(jsonString);
        SharedVariableWrite.writeToSharedVariables(sharedVariableSchema, dataSourceMap, sourceDestinationMapping);
    }

    public static class ExpirationSchema {

        public enum ExpireLocation {

            @JsonProperty("HEADER")
            @JsonAlias({"header", "Header"})
            HEADER,

            @JsonProperty("BODY")
            @JsonAlias({"body"})
            BODY
        }

        @JsonProperty("location")
        private ExpireLocation location;

        @JsonProperty("field")
        private String field;

        @JsonProperty("ttlUnit")
        @JsonSetter(nulls = Nulls.SKIP)
        private TtlUnit ttlUnit = TtlUnit.SECOND;

        public ExpireLocation getLocation() {
            return location;
        }

        public String getField() {
            return field;
        }

        public TtlUnit getTtlUnit() {
            return ttlUnit;
        }
    }

    public static class SourceDestinationDefinition {

        @JsonProperty("source")
        private String source;

        @JsonProperty("destination")
        private String destination;

        public String getSource() {
            return source;
        }

        public String getDestination() {
            return destination;
        }

        public void setSource(String source) {
            this.source = source;
        }

        public void setDestination(String destination) {
            this.destination = destination;
        }
    }
}
