package com.networknt.rule.generic.token.schema;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.networknt.rule.generic.token.schema.cert.SSLContextSchema;
import com.networknt.rule.generic.token.schema.jwt.JWTSchema;

import javax.net.ssl.SSLContext;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.util.Map;


@JsonIgnoreProperties(value={ "sslContext", "httpRequest", "httpClient" }, allowGetters=true)
public class RequestSchema extends SharedVariableRead {
    @JsonProperty("url")
    private String url;
    @JsonProperty("headers")
    protected Map<String, String> headers;
    @JsonProperty("body")
    protected Map<String, String> body;
    @JsonProperty("type")
    private String type;
    @JsonProperty("cacheHttpClient")
    private boolean cacheHttpClient;
    @JsonProperty("cacheSSLContext")
    private boolean cacheSSLContext;
    @JsonProperty("httpRequest")
    private HttpRequest httpRequest;
    @JsonProperty("httpClient")
    private HttpClient httpClient;
    @JsonProperty("sslContext")
    private SSLContext sslContext;
    @JsonProperty("sslContextSchema")
    private SSLContextSchema sslContextSchema;
    @JsonProperty("jwtSchema")
    private JWTSchema jwtSchema;

    public Map<String, String> getHeaders() {
        return headers;
    }

    public Map<String, String> getBody() {
        return body;
    }

    public boolean isCacheHttpClient() {
        return cacheHttpClient;
    }

    public boolean isCacheSSLContext() {
        return cacheSSLContext;
    }

    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }

    public void setBody(Map<String, String> body) {
        this.body = body;
    }

    public String getUrl() {
        return url;
    }
    public SSLContextSchema getSslContextSchema() {
        return sslContextSchema;
    }

    public JWTSchema getJwtSchema() {
        return jwtSchema;
    }

    public SSLContext getSslContext() {
        return sslContext;
    }

    public String getType() {
        return type;
    }

    public HttpRequest getHttpRequest() {
        return httpRequest;
    }

    public HttpClient getHttpClient() {
        return httpClient;
    }

    public void setHttpRequest(HttpRequest httpRequest) {
        this.httpRequest = httpRequest;
    }

    public void setHttpClient(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public void setSslContext(SSLContext sslContext) {
        this.sslContext = sslContext;
    }

    @Override
    public void writeSchemaFromSharedVariables(final SharedVariableSchema sharedVariableSchema) {
        SharedVariableRead.updateMapFromSharedVariables(this.headers, sharedVariableSchema);
        SharedVariableRead.updateMapFromSharedVariables(this.body, sharedVariableSchema);
    }
}
