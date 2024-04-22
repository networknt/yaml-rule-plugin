package com.networknt.rule.generic.token;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.networknt.client.oauth.TokenResponse;
import com.networknt.config.Config;
import com.networknt.config.JsonMapper;
import com.networknt.proxy.PathPrefixAuth;
import com.networknt.rule.RuleActionValue;
import org.apache.commons.lang3.EnumUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Action class that describes the token request, where the token can be found in the response,
 * and where to save the data to.
 *
 * @author Kalev Gonvick
 */
public final class TokenAction {

    private static final Logger LOG = LoggerFactory.getLogger(TokenAction.class);

    private enum TokenSection {
        HEADER,
        BODY,
        URL,
        NONE
    }

    private static final Pattern pattern = Pattern.compile("\\$\\{(.*?)\\}");
    private static final String CONFIG_PREFIX = "config";
    private static final String RESPONSE_PREFIX = "response";
    static final String REQUEST_HEADERS = "requestHeaders";
    static final String REQUEST_BODY_ENTRIES = "requestBodyEntries";
    static final String SOURCE = "tokenSource";
    static final String SOURCE_FIELD = "tokenSourceField";
    static final String DESTINATION = "tokenDestination";
    static final String DESTINATION_FIELD = "tokenDestinationField";
    static final String DESTINATION_VALUE = "tokenDestinationValue";
    private final Collection<RuleActionValue> actionValues;
    private HttpRequest request;
    private final PathPrefixAuth pathPrefixAuth;
    String[] headers = null;
    String[] bodyEntries = null;
    String requestContentType = null;
    TokenSection tokenSource = TokenSection.NONE;
    TokenSection tokenDestination = TokenSection.NONE;
    String destinationField = null;
    String destinationValue = null;
    String tokenSourceField = null;

    private final TokenActionVariables tokenActionVariables = new TokenActionVariables();

    public TokenAction(final Collection<RuleActionValue> actionValues, final PathPrefixAuth pathPrefixAuth) {
        this.actionValues = actionValues;
        this.pathPrefixAuth = pathPrefixAuth;
        this.tokenActionVariables.parsePathPrefixAuthVariables(CONFIG_PREFIX, this.pathPrefixAuth);
        this.parseActionValues();
        this.buildRequest();
    }

    /**
     * Parse the incoming rule actionValues.
     */
    private void parseActionValues() {
        for (var action : this.actionValues) {
            switch (action.getActionValueId()) {
                case REQUEST_HEADERS: {
                    String[] temp = action.getValue().split(",");
                    this.headers = this.tokenActionVariables.resolveArrayValues(temp);
                    break;
                }
                case REQUEST_BODY_ENTRIES: {
                    String[] temp = action.getValue().split(",");
                    this.bodyEntries = this.tokenActionVariables.resolveArrayValues(temp);
                    break;
                }
                case DESTINATION: {
                    if (EnumUtils.isValidEnum(TokenSection.class, action.getValue()))
                        this.tokenDestination = EnumUtils.getEnum(TokenSection.class, action.getValue());
                    break;
                }
                case DESTINATION_FIELD: {
                    this.destinationField = action.getValue();
                    break;
                }
                case DESTINATION_VALUE: {
                    this.destinationValue = action.getValue();
                    break;
                }
                case SOURCE: {
                    if (EnumUtils.isValidEnum(TokenSection.class, action.getValue()))
                        this.tokenSource = EnumUtils.getEnum(TokenSection.class, action.getValue());
                    break;
                }
                case SOURCE_FIELD: {
                    this.tokenSourceField = action.getValue();
                    break;
                }
                default: {
                    LOG.error("Unknown actionValueId '{}'.", action.getActionValueId());
                    break;
                }
            }
        }
    }

    /**
     * Builds the Http request based on the provided configuration.
     */
    private void buildRequest() {
        final var builder = HttpRequest.newBuilder();

        try {
            builder.uri(new URI(this.pathPrefixAuth.getTokenUrl()));

        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        /* handle request headers */
        if (this.isValidArrayLength(this.headers)) {

            for (int x = 0; x < this.headers.length; x = x + 2) {

                if (this.headers[x].equalsIgnoreCase("Content-Type"))
                    this.requestContentType = this.headers[x + 1];

                builder.header(this.headers[x], this.headers[x + 1]);
            }
        }

        /* handle request body */
        if (this.isValidArrayLength(this.bodyEntries)) {

            /* add body key + value pairs to the request */
            final Map<String, String> parameters = new HashMap<>();

            for (int x = 0; x < this.bodyEntries.length; x = x + 2)
                parameters.put(this.bodyEntries[x], this.bodyEntries[x + 1]);

            String body;

            /* format the body */
            if (this.isUrlEncoded()) {
                body = parameters.entrySet().stream().map(
                        e -> e.getKey() + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8)
                ).collect(Collectors.joining("&"));

            } else {
                try {
                    body = Config.getInstance().getMapper().writeValueAsString(parameters);
                } catch (JsonProcessingException e) {
                    LOG.error("Could not convert body parameters to string format: {}", e.getMessage());
                    return;
                }
            }

            /* only POST requests are supported right now. */
            builder.POST(HttpRequest.BodyPublishers.ofString(body));
        }
        this.request = builder.build();
    }

    /**
     * Checks to make sure the array is not null and has an even number of elements.
     *
     * @param arr   - array to check
     * @return      - true if the array is valid.
     */
    private boolean isValidArrayLength(Object[] arr) {
        return arr != null && arr.length % 2 == 0;
    }

    private boolean isUrlEncoded() {
        return this.requestContentType != null
                && this.requestContentType.equalsIgnoreCase("application/x-www-form-urlencoded");
    }

    /**
     * Send the token request and save the result to the resultMap.
     *
     * @param client - Java Http client
     * @param resultMap - rule engine resultMap
     */
    public void requestToken(final HttpClient client, final Map<String, Object> resultMap) {

        try {
            final HttpResponse<?> response = client.send(this.request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                final TokenResponse tokenResponse = this.handleTokenSource(response);

                /* TODO - should expiration be set statically like this? */
                this.pathPrefixAuth.setExpiration(System.currentTimeMillis() + this.pathPrefixAuth.getTokenTtl() * 1000L - 60000);
                this.pathPrefixAuth.setAccessToken(tokenResponse.getAccessToken());
                this.tokenActionVariables.parsePathPrefixAuthVariables(RESPONSE_PREFIX, this.pathPrefixAuth);

                LOG.trace("Received a new token '{}' and cached it with an expiration time of '{}'.",
                        this.pathPrefixAuth.getAccessToken() != null ? this.pathPrefixAuth.getAccessToken().substring(0, 20) : null,
                        this.pathPrefixAuth.getExpiration()
                );

                this.handleTokenDestination(resultMap);

            } else LOG.error("Error in getting the token with status code {} and body {}", response.statusCode(), response.body());

        } catch (Exception e) {
            LOG.error("Exception:", e);
        }
    }

    /**
     * Places a newly received token into the configured destination.
     * i.e. tokenDestination: HEADER, tokenDestinationField: Authorization will update the Authorization header with a token.
     *
     * @param resultMap - resultMap from the executed rule.
     */
    private void handleTokenDestination(Map<String, Object> resultMap) {
        final var resolvedDestinationValue = this.tokenActionVariables.resolveValue(this.destinationValue);
        switch (this.tokenDestination) {
            case HEADER: {
                final Map<String, Object> requestHeaders = new HashMap<>();
                final Map<String, Object> updateMap = new HashMap<>();
                updateMap.put(this.destinationField, resolvedDestinationValue);
                requestHeaders.put("update", updateMap);
                resultMap.put("requestHeaders", requestHeaders);
                break;
            }
            case BODY: {
                final Map<String, Object> requestBody = new HashMap<>();
                requestBody.put(this.destinationField, resolvedDestinationValue);
                try {
                    resultMap.put("requestBody", Config.getInstance().getMapper().writeValueAsString(requestBody));
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
                break;
            }
            case URL: {
                // TODO
                break;
            }
            case NONE:
            default: {
                break;
            }
        }
    }

    /**
     * Parses the token from a 200 TokenResponse.
     * We grab the token based on the provided rule configuration.
     *
     * @param response - 200 response for the token request.
     * @return - returns a TokenResponse with data.
     */
    private TokenResponse handleTokenSource(final HttpResponse<?> response) {
        final var tokenResponse = new TokenResponse();
        switch (this.tokenSource) {
            case BODY: {
                final var map = JsonMapper.string2Map(response.body().toString());
                tokenResponse.setAccessToken((String) map.get(this.tokenSourceField));
                break;
            }
            case HEADER: {
                final var map = JsonMapper.string2Map(response.headers().toString());
                tokenResponse.setAccessToken((String) map.get(this.tokenSourceField));
                break;
            }
            case URL: {
                // TODO
                break;
            }
            case NONE:
            default: {
                break;
            }
        }

        return tokenResponse;
    }

    private static final class TokenActionVariables {

        private final Map<String, String> variables = new HashMap<>();

        public void addVariable(String key, String value) {

            if (key == null || value == null)
                return;

            this.variables.put(key, value);
        }

        public String get(String key) {
            return this.variables.get(key);
        }

        /**
         * Resolves a string array of values.
         *
         * @param arr - provided string array.
         * @return - returns resolved string array.
         */
        private String[] resolveArrayValues(String[] arr) {

            if (arr.length == 0)
                return arr;

            var copy = Arrays.copyOf(arr, arr.length);

            for (int x = 0; x < arr.length; x++)
                copy[x] = this.resolveValue(arr[x]);

            return copy;
        }

        /**
         * Resolved a given variable (i.e. string containing ${****}).
         *
         * @param unresolved - unresolved string
         * @return - string containing the resolved variable value.
         */
        private String resolveValue(String unresolved) {
            final var matcher = pattern.matcher(unresolved);
            final var stringBuilder = new StringBuilder();
            while (matcher.find()) {
                final var value = this.get(matcher.group(1));

                if (value.contains("\\$"))
                    matcher.appendReplacement(stringBuilder, value);

                else matcher.appendReplacement(stringBuilder, Matcher.quoteReplacement(value));

            }

            return matcher.appendTail(stringBuilder).toString();
        }

        /**
         * Updates the token variable pool with the data from the PathPrefixAuth instance and a prefix associated.
         * Duplicates will override the old value.
         *
         * @param variablePrefix - prefix the variable will have.
         * @param pathPrefixAuth - the PathPrefixAuth instance that contains the data we want to save to the variables.
         */
        public void parsePathPrefixAuthVariables(String variablePrefix, PathPrefixAuth pathPrefixAuth) {
            final var pathPrefixMap = Config.getInstance().getMapper().convertValue(pathPrefixAuth, new TypeReference<Map<String, Object>>() {
            });
            for (var pair : pathPrefixMap.entrySet()) {
                if (pair.getValue() instanceof String) {
                    final var key = buildVariableKeyLookup(variablePrefix, pair.getKey());
                    this.addVariable(key, (String) pair.getValue());
                }
            }
        }

        /**
         * Formats the variable key before storing.
         *
         * @param variablePrefix - config prefix
         * @param variableKey - key of the variable
         * @return - returns a formatted String.
         */
        private static String buildVariableKeyLookup(String variablePrefix, String variableKey) {
            return variablePrefix + "." + variableKey;
        }
    }
}



