package com.networknt.rule.generic.token.schema;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;

import java.util.regex.Pattern;

/**
 * Shared variable object used to store and read different variables used in the request, source and update schemas.
 */
public class SharedVariableSchema {
    protected static final Pattern VARIABLE_PATTERN = Pattern.compile("\\!ref\\((.*?)\\)");

    @JsonProperty("grantType")
    private String grantType;

    @JsonProperty("authIssuer")
    private String authIssuer;

    @JsonProperty("authSubject")
    private String authSubject;

    @JsonProperty("authAudience")
    private String authAudience;

    @JsonProperty("iv")
    private String iv;

    @JsonProperty("tokenTtl")
    private long tokenTtl;

    @JsonProperty("tokenTtlUnit")
    @JsonSetter(nulls = Nulls.SKIP)
    private TtlUnit tokenTtlUnit = TtlUnit.SECOND;

    @JsonProperty("waitLength")
    private long waitLength;

    @JsonProperty("certFilename")
    private String certFilename;

    @JsonProperty("certPassword")
    private char[] certPassword;

    @JsonProperty("username")
    private String username;

    @JsonProperty("password")
    private char[] password;

    @JsonProperty("clientId")
    private String clientId;

    @JsonProperty("scope")
    private String scope;

    @JsonProperty("clientSecret")
    private char[] clientSecret;

    @JsonProperty("responseType")
    private String responseType;

    @JsonProperty("expiration")
    private long expiration;

    @JsonProperty("accessToken")
    private String accessToken;

    @JsonProperty("constructedJwt")
    private String constructedJwt;

    public String getConstructedJwt() {
        return constructedJwt;
    }

    public String getGrantType() {
        return grantType;
    }

    public String getAuthIssuer() {
        return authIssuer;
    }

    public String getAuthSubject() {
        return authSubject;
    }

    public String getAuthAudience() {
        return authAudience;
    }

    public String getIv() {
        return iv;
    }

    public long getTokenTtl() {
        return tokenTtl;
    }

    public long getWaitLength() {
        return waitLength;
    }

    public String getCertFilename() {
        return certFilename;
    }

    public char[] getCertPassword() {
        return certPassword;
    }

    public String getUsername() {
        return username;
    }

    public char[] getPassword() {
        return password;
    }

    public String getClientId() {
        return clientId;
    }

    public TtlUnit getTokenTtlUnit() {
        return tokenTtlUnit;
    }

    public String getScope() {
        return scope;
    }

    public char[] getClientSecret() {
        return clientSecret;
    }

    public String getResponseType() {
        return responseType;
    }

    public long getExpiration() {
        return expiration;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setGrantType(final String grantType) {
        this.grantType = grantType;
    }

    public void setAuthIssuer(final String authIssuer) {
        this.authIssuer = authIssuer;
    }

    public void setAuthSubject(final String authSubject) {
        this.authSubject = authSubject;
    }

    public void setAuthAudience(final String authAudience) {
        this.authAudience = authAudience;
    }

    public void setIv(final String iv) {
        this.iv = iv;
    }

    public void setTokenTtl(final long tokenTtl) {
        this.tokenTtl = tokenTtl;
    }

    public void setWaitLength(final long waitLength) {
        this.waitLength = waitLength;
    }

    public void setCertFilename(final String certFilename) {
        this.certFilename = certFilename;
    }

    public void setCertPassword(final char[] certPassword) {
        this.certPassword = certPassword;
    }

    public void setUsername(final String username) {
        this.username = username;
    }

    public void setPassword(final char[] password) {
        this.password = password;
    }

    public void setClientId(final String clientId) {
        this.clientId = clientId;
    }

    public void setScope(final String scope) {
        this.scope = scope;
    }

    public void setClientSecret(final char[] clientSecret) {
        this.clientSecret = clientSecret;
    }

    public void setResponseType(final String responseType) {
        this.responseType = responseType;
    }

    public void setExpiration(final long expiration) {
        this.expiration = expiration;
    }

    public void setAccessToken(final String accessToken) {
        this.accessToken = accessToken;
    }

    public void setConstructedJwt(String constructedJwt) {
        this.constructedJwt = constructedJwt;
    }

    public void setTokenTtlUnit(TtlUnit tokenTtlUnit) {
        this.tokenTtlUnit = tokenTtlUnit;
    }

    /**
     * Update the expiration of the token based on the configured ttl.
     * Converts ttl to milliseconds first before storing.
     */
    public void updateExpiration() {
        final var ttlInMillis = this.tokenTtlUnit.unitToMillis(this.tokenTtl);
        final var newExpiration = System.currentTimeMillis() + ttlInMillis;
        this.setExpiration(newExpiration);
    }
}
