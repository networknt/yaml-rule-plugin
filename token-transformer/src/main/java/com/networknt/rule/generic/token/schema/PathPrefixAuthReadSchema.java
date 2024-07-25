package com.networknt.rule.generic.token.schema;

import com.networknt.config.PathPrefixAuth;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;

public abstract class PathPrefixAuthReadSchema extends PathPrefixAuthRelationSchema {

    public abstract void writeSchemaFromPathPrefix(final PathPrefixAuth pathPrefixAuth);

    protected static void updateMapFromPathPrefix(final Map<String, String> map, final PathPrefixAuth pathPrefixAuth) {
        if (map == null)
            return;

        final var updateMap = new HashMap<String, String>();
        for (final var entry : map.entrySet()) {
            final var key = entry.getKey();
            final var value = injectPathPrefixVariable(entry.getValue(), pathPrefixAuth);
            updateMap.put(key, value);
        }
        map.putAll(updateMap);
    }

    private static String injectPathPrefixVariable(final String variableString, final PathPrefixAuth pathPrefixAuth) {
        final var matcher = VARIABLE_PATTERN.matcher(variableString);
        final var stringBuilder = new StringBuilder();

        while (matcher.find()) {
            final var foundVariableName = matcher.group(1);
            final var splitVariable = foundVariableName.split("\\.");
            final var bucketName = splitVariable[0];
            final String value;

            /* only reading from path prefix is allowed */
            /* method can be expanded in the future. */
            if (bucketName.equals("pathPrefixAuth"))
                value = getPathPrefixVariable(pathPrefixAuth, splitVariable[1]);

            else value = foundVariableName;


            if (value.contains("\\!ref"))
                matcher.appendReplacement(stringBuilder, value);

            else matcher.appendReplacement(stringBuilder, Matcher.quoteReplacement(value));

        }
        return matcher.appendTail(stringBuilder).toString();
    }

    public static String getPathPrefixVariable(final PathPrefixAuth pathPrefixAuth, final String find) {
        switch (find) {
            case GRANT_TYPE:
                return pathPrefixAuth.getGrantType();
            case PATH_PREFIX:
                return pathPrefixAuth.getPathPrefix();
            case AUTH_ISSUER:
                return pathPrefixAuth.getAuthIssuer();
            case AUTH_SUBJECT:
                return pathPrefixAuth.getAuthSubject();
            case AUTH_AUDIENCE:
                return pathPrefixAuth.getAuthAudience();
            case IV:
                return pathPrefixAuth.getIv();
            case TOKEN_TTL:
                return String.valueOf(pathPrefixAuth.getTokenTtl());
            case WAIT_LENGTH:
                return String.valueOf(pathPrefixAuth.getWaitLength());
            case TOKEN_URL:
                return pathPrefixAuth.getTokenUrl();
            case CERT_FILENAME:
                return pathPrefixAuth.getCertFilename();
            case CERT_PASSWORD:
                return pathPrefixAuth.getCertPassword();
            case SERVICE_HOST:
                return pathPrefixAuth.getServiceHost();
            case USERNAME:
                return pathPrefixAuth.getUsername();
            case PASSWORD:
                return pathPrefixAuth.getPassword();
            case CLIENT_ID:
                return pathPrefixAuth.getClientId();
            case SCOPE:
                return pathPrefixAuth.getScope();
            case CLIENT_SECRET:
                return pathPrefixAuth.getClientSecret();
            case RESPONSE_TYPE:
                return pathPrefixAuth.getResponseType();
            case EXPIRATION:
                return String.valueOf(pathPrefixAuth.getExpiration());
            case ACCESS_TOKEN:
                return pathPrefixAuth.getAccessToken();
            case HTTP_CLIENT:
            default:
                throw new IllegalArgumentException("Variable name '" + find + "' is not a member of PathPrefixAuth or is not accessible.");
        }
    }


}
