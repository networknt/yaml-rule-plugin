package com.networknt.rule.generic.token.schema;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.networknt.config.JsonMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.http.HttpResponse;
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

    public void writeResponseToSharedVariables(final SharedVariableSchema sharedVariableSchema, final HttpResponse<?> response) {
        // TODO - add support for response headers
        //this.writeJsonStringToSharedVariables(sharedVariableSchema, response.headers().toString(), this.headers);
        this.writeJsonStringToSharedVariables(sharedVariableSchema, response.body().toString(), this.body);

        /* if an expiration schema is specified, grab it from the response and convert it to milliseconds. */
        if (this.expirationSchema != null) {
            switch (expirationSchema.location.toLowerCase()) {
                case "header":
                    final var expirationString = response.headers().map().get(expirationSchema.field).stream().findFirst();
                    if (expirationString.isPresent()) {
                        final var responseExpiration = Long.parseLong(expirationString.get());
                        sharedVariableSchema.setExpiration(expirationSchema.ttlUnit.unitToMillis(responseExpiration));
                    } else {
                        LOG.error("Could not find '{}' contained in the headers of the token response.", expirationSchema.field);
                    }
                    break;
                case "body":
                    final var bodyMap = JsonMapper.string2Map(response.body().toString());
                    if (bodyMap.containsKey(expirationSchema.field)) {
                        final var responseExpiration = Long.parseLong(String.valueOf(bodyMap.get(expirationSchema.field)));
                        sharedVariableSchema.setExpiration(expirationSchema.ttlUnit.unitToMillis(responseExpiration));
                    } else {
                        LOG.error("Could not find '{}' contained in the body of the token response.", expirationSchema.field);
                    }
                    break;
                default:
                    throw new IllegalStateException("Invalid location configured in ./source/expirationSchema: '" + expirationSchema.location + "'.");
            }
        }
    }

    public void writeJsonStringToSharedVariables(final SharedVariableSchema sharedVariableSchema, final String jsonString, List<SourceDestinationDefinition> sourceDestinationMapping) {
        final var dataSourceMap = JsonMapper.string2Map(jsonString);
        SharedVariableWrite.writeToSharedVariables(sharedVariableSchema, dataSourceMap, sourceDestinationMapping);
    }

    public static class ExpirationSchema {
        @JsonProperty("location")
        private String location;

        @JsonProperty("field")
        private String field;

        @JsonProperty("ttlUnit")
        @JsonSetter(nulls = Nulls.SKIP)
        private TtlUnit ttlUnit = TtlUnit.SECOND;

        public String getLocation() {
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
