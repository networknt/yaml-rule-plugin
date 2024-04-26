package com.networknt.rule.generic.token;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.networknt.config.Config;
import com.networknt.config.JsonMapper;
import com.networknt.config.PathPrefixAuth;
import com.networknt.rule.RuleActionValue;
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

    private enum TokenDirection {
        REQUEST,
        RESPONSE,
        NONE
    }

    private static final String CONFIG_PREFIX = "config";
    private static final String RESPONSE_PREFIX = "response";
    static final String REQUEST_HEADERS = "requestHeaders";
    static final String REQUEST_BODY_ENTRIES = "requestBodyEntries";
    static final String SOURCE_HEADERS = "sourceHeaders";
    static final String SOURCE_BODY_ENTRIES = "sourceBodyEntries";
    static final String TOKEN_DIRECTION = "tokenDirection";
    static final String DESTINATION_HEADERS = "destinationHeaders";
    static final String DESTINATION_BODY_ENTRIES = "destinationBodyEntries";
    private final Collection<RuleActionValue> actionValues;
    private HttpRequest request;
    private final PathPrefixAuth pathPrefixAuth;
    String[] requestHeaders = null;
    String[] requestBodyEntries = null;
    String requestContentType = null;
    String[] sourceHeaders = null;
    String[] sourceBodyEntries = null;
    String[] destinationHeaders = null;
    String[] destinationBodyEntries = null;
    TokenDirection tokenDirection = TokenDirection.NONE;


    private final TokenActionVariables tokenActionVariables = new TokenActionVariables();

    public TokenAction(final Collection<RuleActionValue> actionValues, final PathPrefixAuth pathPrefixAuth) {
        this.actionValues = actionValues;
        this.pathPrefixAuth = pathPrefixAuth;
        this.tokenActionVariables.parseVariablesFromObject(CONFIG_PREFIX, this.pathPrefixAuth);
        this.parseActionValues();
        this.buildRequest();
    }

    /**
     * Parse the incoming rule actionValues.
     */
    private void parseActionValues() {
        for (var action : this.actionValues) {
            switch (action.getActionValueId()) {

                /* We can resolve request headers because the config is already loaded. */
                case REQUEST_HEADERS: {
                    String[] temp = action.getValue().split(",");
                    this.requestHeaders = this.tokenActionVariables.resolveArrayValues(temp);
                    break;
                }

                /* We can resolve request body entries because the config is already loaded. */
                case REQUEST_BODY_ENTRIES: {
                    String[] temp = action.getValue().split(",");
                    this.requestBodyEntries = this.tokenActionVariables.resolveArrayValues(temp);
                    break;
                }
                case TOKEN_DIRECTION: {
                    try {
                        this.tokenDirection = TokenDirection.valueOf(action.getValue());
                    } catch (IllegalArgumentException e) {
                        this.tokenDirection = TokenDirection.NONE;
                    }
                    break;
                }
                case DESTINATION_HEADERS: {
                    this.destinationHeaders = action.getValue().split(",");
                    break;
                }
                case DESTINATION_BODY_ENTRIES: {
                    this.destinationBodyEntries = action.getValue().split(",");
                    break;
                }
                case SOURCE_HEADERS: {
                    this.sourceHeaders = action.getValue().split(",");
                    break;
                }
                case SOURCE_BODY_ENTRIES: {
                    this.sourceBodyEntries = action.getValue().split(",");
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
        if (this.isValidArrayLength(this.requestHeaders)) {

            for (int x = 0; x < this.requestHeaders.length; x = x + 2) {

                if (this.requestHeaders[x].equalsIgnoreCase("Content-Type"))
                    this.requestContentType = this.requestHeaders[x + 1];

                builder.header(this.requestHeaders[x], this.requestHeaders[x + 1]);
            }
        }

        /* handle request body */
        if (this.isValidArrayLength(this.requestBodyEntries)) {

            /* add body key + value pairs to the request */
            final Map<String, String> parameters = new HashMap<>();

            for (int x = 0; x < this.requestBodyEntries.length; x = x + 2)
                parameters.put(this.requestBodyEntries[x], this.requestBodyEntries[x + 1]);

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
        return arr != null && (arr.length & 1) == 0;
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
                final var tokenResponseMap = new HashMap<String, Object>();

                if (this.sourceHeaders != null)
                    tokenResponseMap.putAll(parseTokenResponseArrayVariables(
                            this.sourceHeaders,
                            response.headers().toString()
                            ));

                if (this.sourceBodyEntries != null)
                    tokenResponseMap.putAll(parseTokenResponseArrayVariables(
                                    this.sourceBodyEntries,
                                    response.body().toString()
                            ));

                this.tokenActionVariables.parseVariablesFromMap(RESPONSE_PREFIX, tokenResponseMap);
                this.destinationHeaders = this.tokenActionVariables.resolveArrayValues(this.destinationHeaders);
                this.destinationBodyEntries = this.tokenActionVariables.resolveArrayValues(this.destinationBodyEntries);

                /* update path prefix auth token info */
                if (tokenResponseMap.containsKey("accessToken")) {
                    this.pathPrefixAuth.setAccessToken((String)tokenResponseMap.get("accessToken"));
                }

                if (tokenResponseMap.containsKey("grantType")) {
                    this.pathPrefixAuth.setGrantType((String)tokenResponseMap.get("grantType"));
                }

                if (tokenResponseMap.containsKey("expiration")) {
                    this.pathPrefixAuth.setExpiration((Integer)tokenResponseMap.get("expiration"));
                } else {
                    /* TODO - should expiration be set statically like this? */
                    this.pathPrefixAuth.setExpiration(System.currentTimeMillis() + this.pathPrefixAuth.getTokenTtl() * 1000L - 60000);
                }

                LOG.trace("Received a new token '{}' and cached it with an expiration time of '{}'.",
                        this.pathPrefixAuth.getAccessToken() != null ? this.pathPrefixAuth.getAccessToken().substring(0, 20) : null,
                        this.pathPrefixAuth.getExpiration()
                );

                /* update result map */
                switch (this.tokenDirection) {

                    case REQUEST: {
                        if (this.destinationBodyEntries != null) {
                            final var requestBodyMap = createUpdateMap(this.destinationBodyEntries);
                            resultMap.put("requestBody", requestBodyMap);
                        }

                        if (this.destinationHeaders != null) {
                            final var requestHeadersMap = createUpdateMap(this.destinationHeaders);
                            final var updateMap = new HashMap<String, Object>();
                            updateMap.put("update", requestHeadersMap);
                            resultMap.put("requestHeaders", updateMap);
                        }
                        break;
                    }
                    case RESPONSE: {
                        if (this.destinationBodyEntries != null) {
                            final var responseBodyMap = createUpdateMap(this.destinationBodyEntries);
                            resultMap.put("responseBody", responseBodyMap);
                        }

                        if (this.destinationHeaders != null) {
                            final var responseHeadersMap = createUpdateMap(this.destinationHeaders);
                            final var updateMap = new HashMap<String, Object>();
                            updateMap.put("update", responseHeadersMap);
                            resultMap.put("responseHeaders", updateMap);
                        }
                        break;
                    }
                    case NONE:
                    default:
                        break;
                }

            } else LOG.error("Error in getting the token with status code {} and body {}", response.statusCode(), response.body());

        } catch (Exception e) {
            LOG.error("Exception:", e);
        }
    }

    private static Map<String, Object> createUpdateMap(final String[] dataSourceArray) {
        final var updateMap = new HashMap<String, Object>();

        if (dataSourceArray == null || dataSourceArray.length == 0)
            return updateMap;

        for (int x = 0 ; x < dataSourceArray.length; x = x + 2) {
            updateMap.put(dataSourceArray[x], dataSourceArray[x+1]);
        }

        return updateMap;
    }

    private static Map<String, Object> parseTokenResponseArrayVariables(final String[] arr, final String string) {
        if (string == null || arr == null)
            return new HashMap<>();

        final Map<String, Object> tokenResponseData = new HashMap<>();
        final var map = JsonMapper.string2Map(string);
        if (arr.length > 0 && (arr.length & 1) == 0) {
            for (int x = 0; x < arr.length; x = x + 2) {
                final var value = map.get(arr[x]);
                final var key = getLocalVariableName(arr[x+1]);
                tokenResponseData.put(key, value);
            }
        }
        return tokenResponseData;
    }

    private static String getLocalVariableName(String in) {
        if (!in.contains("{") || !in.contains("}")) {
            return in;
        }
        final var spl = in.split("\\.");
        if (spl.length != 2) {
            return in;
        }

        return spl[1].replace("}", "");
    }



    private static final class TokenActionVariables {

        private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\$\\{(.*?)\\}");

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

            if (arr == null || arr.length == 0)
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
            final var matcher = VARIABLE_PATTERN.matcher(unresolved);
            final var stringBuilder = new StringBuilder();
            while (matcher.find()) {
                final var value = this.get(matcher.group(1));

                if (value.contains("\\$"))
                    matcher.appendReplacement(stringBuilder, value);

                else matcher.appendReplacement(stringBuilder, Matcher.quoteReplacement(value));

            }

            return matcher.appendTail(stringBuilder).toString();
        }

        public void parseVariablesFromJsonString(final String variablePrefix, final String string) {
            final var jsonMap = JsonMapper.string2Map(string);
            this.parseVariablesFromMap(variablePrefix, jsonMap);
        }

        /**
         * Updates the token variable pool with the data from an Object instance and a prefix associated.
         * Duplicates will override the old value.
         *
         * @param variablePrefix - prefix the variable will have.
         * @param obj - Provided source of variables.
         */
        public void parseVariablesFromObject(final String variablePrefix, final Object obj) {
            final var objMap = Config.getInstance().getMapper().convertValue(obj, new TypeReference<Map<String, Object>>() {});
            this.parseVariablesFromMap(variablePrefix, objMap);
        }
        public void parseVariablesFromMap(final String variablePrefix, final Map<String, Object> input) {
            for (final var pair : input.entrySet()) {
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
