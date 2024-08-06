package com.networknt.rule.generic.token.schema;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.networknt.config.JsonMapper;
import com.networknt.config.PathPrefixAuth;

import java.net.http.HttpResponse;
import java.util.List;

public class SourceSchema extends PathPrefixAuthWriteSchema {

    @JsonProperty("headers")
    private List<SourceDestinationDefinition> headers;

    @JsonProperty("body")
    private List<SourceDestinationDefinition> body;

    public List<SourceDestinationDefinition> getHeaders() {
        return headers;
    }

    public List<SourceDestinationDefinition> getBody() {
        return body;
    }

    public void setHeaders(List<SourceDestinationDefinition> headers) {
        this.headers = headers;
    }

    public void setBody(List<SourceDestinationDefinition> body) {
        this.body = body;
    }

    public void writeResponseToPathPrefix(final PathPrefixAuth pathPrefixAuth, final HttpResponse<?> response) {
        // TODO - add support for response headers
        //this.writeJsonStringToPathPrefix(pathPrefixAuth, response.headers().toString(), this.headers);
        this.writeJsonStringToPathPrefix(pathPrefixAuth, response.body().toString(), this.body);
    }

    public void writeJsonStringToPathPrefix(final PathPrefixAuth pathPrefixAuth, final String jsonString, List<SourceDestinationDefinition> sourceDestinationMapping) {
        final var dataSourceMap = JsonMapper.string2Map(jsonString);
        PathPrefixAuthWriteSchema.writeToPathPrefixAuth(pathPrefixAuth, dataSourceMap, sourceDestinationMapping);
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
