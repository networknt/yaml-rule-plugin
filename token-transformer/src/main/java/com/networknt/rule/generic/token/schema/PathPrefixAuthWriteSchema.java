package com.networknt.rule.generic.token.schema;

import com.networknt.config.PathPrefixAuth;

import java.util.List;
import java.util.Map;

public abstract class PathPrefixAuthWriteSchema extends PathPrefixAuthRelationSchema {

    /**
     * From a provided source-destination map, write new values to pathPrefixAuth.
     *
     * @param pathPrefixAuth - pathPrefixAuth being written to.
     * @param sourceData - source data that contains new values.
     * @param sourceDestinationMapping - sourceDestinationMapping that maps the values to a pathPrefixAuth field.
     */
    protected static void writeToPathPrefixAuth(final PathPrefixAuth pathPrefixAuth, final Map<String, Object> sourceData, final List<SourceSchema.SourceDestinationDefinition> sourceDestinationMapping) {
        if (sourceDestinationMapping == null || sourceDestinationMapping.isEmpty() || sourceData == null || sourceData.isEmpty())
            return;

        for (final var sourceEntry : sourceDestinationMapping) {
            final var value = sourceData.get(sourceEntry.getSource());
            if (value instanceof String) {
                setPathPrefixVariable(pathPrefixAuth, sourceEntry.getDestination(), (String) value);
            }
        }
    }

    /**
     * Sets the value of pathPrefixAuth variable based on provided name.
     *
     * @param pathPrefixAuth - the pathPrefixAuth object being written to.
     * @param find - the variable name as a String
     * @param value - the new value.
     */
    private static void setPathPrefixVariable(final PathPrefixAuth pathPrefixAuth, final String find, final String value) {
        final var matcher = VARIABLE_PATTERN.matcher(find);
        if (matcher.find()) {
            final var variable = matcher.group(1);
            final var searchArr = variable.split("\\.");
            if (searchArr.length == 2) {
                switch (searchArr[1]) {
                    case GRANT_TYPE:
                        pathPrefixAuth.setGrantType(value);
                        break;
                    case PATH_PREFIX:
                        pathPrefixAuth.setPathPrefix(value);
                        break;
                    case AUTH_ISSUER:
                        pathPrefixAuth.setAuthIssuer(value);
                        break;
                    case AUTH_SUBJECT:
                        pathPrefixAuth.setAuthSubject(value);
                        break;
                    case AUTH_AUDIENCE:
                        pathPrefixAuth.setAuthAudience(value);
                        break;
                    case IV:
                        pathPrefixAuth.setIv(value);
                        break;
                    case TOKEN_TTL:
                        pathPrefixAuth.setTokenTtl(Integer.parseInt(value));
                        break;
                    case WAIT_LENGTH:
                        pathPrefixAuth.setWaitLength(Integer.parseInt(value));
                        break;
                    case TOKEN_URL:
                        pathPrefixAuth.setTokenUrl(value);
                        break;
                    case CERT_FILENAME:
                        pathPrefixAuth.setCertFilename(value);
                        break;
                    case CERT_PASSWORD:
                        pathPrefixAuth.setCertPassword(value);
                        break;
                    case SERVICE_HOST:
                        pathPrefixAuth.setServiceHost(value);
                        break;
                    case USERNAME:
                        pathPrefixAuth.setUsername(value);
                        break;
                    case PASSWORD:
                        pathPrefixAuth.setPassword(value);
                        break;
                    case CLIENT_ID:
                        pathPrefixAuth.setClientId(value);
                        break;
                    case SCOPE:
                        pathPrefixAuth.setScope(value);
                        break;
                    case CLIENT_SECRET:
                        pathPrefixAuth.setClientSecret(value);
                        break;
                    case RESPONSE_TYPE:
                        pathPrefixAuth.setResponseType(value);
                        break;
                    case EXPIRATION:
                        pathPrefixAuth.setExpiration(Long.parseLong(value));
                        break;
                    case ACCESS_TOKEN:
                        pathPrefixAuth.setAccessToken(value);
                        break;
                    case HTTP_CLIENT:
                    default:
                        throw new IllegalArgumentException("Variable name '" + find + "' is not a member of PathPrefixAuth or is not accessible.");
                }
            }
        } else throw new IllegalArgumentException("Provided variable '" + find + "' does not follow !ref format");

    }

}
