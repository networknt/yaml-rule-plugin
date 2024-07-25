package com.networknt.rule.generic.token.schema;

import java.util.regex.Pattern;

public abstract class PathPrefixAuthRelationSchema {
    protected static final Pattern VARIABLE_PATTERN = Pattern.compile("\\!ref\\((.*?)\\)");

    /* pathPrefixAuth variable names */
    protected static final String GRANT_TYPE = "grantType";
    protected static final String PATH_PREFIX = "pathPrefix";
    protected static final String AUTH_ISSUER = "authIssuer";
    protected static final String AUTH_SUBJECT = "authSubject";
    protected static final String AUTH_AUDIENCE = "authAudience";
    protected static final String IV = "iv";
    protected static final String TOKEN_TTL = "tokenTtl";
    protected static final String WAIT_LENGTH = "waitLength";
    protected static final String TOKEN_URL = "tokenUrl";
    protected static final String CERT_FILENAME = "certFilename";
    protected static final String CERT_PASSWORD = "certPassword";
    protected static final String SERVICE_HOST = "serviceHost";
    protected static final String USERNAME = "username";
    protected static final String PASSWORD = "password";
    protected static final String CLIENT_ID = "clientId";
    protected static final String SCOPE = "scope";
    protected static final String CLIENT_SECRET = "clientSecret";
    protected static final String RESPONSE_TYPE = "responseType";
    protected static final String EXPIRATION = "expiration";
    protected static final String ACCESS_TOKEN = "accessToken";
    protected static final String HTTP_CLIENT = "httpClient";

    /* jwtSchema variable names */
    protected static final String CONSTRUCTED_JWT = "jwt";
}
