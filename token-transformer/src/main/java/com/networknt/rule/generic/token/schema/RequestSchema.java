package com.networknt.rule.generic.token.schema;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import com.networknt.config.PathPrefixAuth;
import com.networknt.rule.generic.token.schema.cert.SSLContextSchema;
import com.networknt.rule.generic.token.schema.jwt.JWTSchema;

import javax.net.ssl.SSLContext;
import java.net.http.HttpRequest;
import java.util.Map;


@JsonIgnoreProperties(value={ "sslContext", "httpRequest" }, allowGetters=true)
public class RequestSchema extends PathPrefixAuthReadSchema {
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
    @JsonProperty("sslContext")
    private SSLContext sslContext;
    @JsonProperty("sslContextSchema")
    private SSLContextSchema sslContextSchema;
    @JsonProperty("jwtSchema")
    private JWTSchema jwtSchema;
    @JsonProperty("tokenGracePeriod")
    @JsonSetter(nulls = Nulls.SKIP)
    private long tokenGracePeriod = 0L;

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
    public long getTokenGracePeriod() {
        return tokenGracePeriod;
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

    public void setHttpRequest(HttpRequest httpRequest) {
        this.httpRequest = httpRequest;
    }

    public void setSslContext(SSLContext sslContext) {
        this.sslContext = sslContext;
    }

    @Override
    public void writeSchemaFromPathPrefix(PathPrefixAuth pathPrefixAuth) {
        PathPrefixAuthReadSchema.updateMapFromPathPrefix(this.headers, pathPrefixAuth);
        PathPrefixAuthReadSchema.updateMapFromPathPrefix(this.body, pathPrefixAuth);
    }
}
