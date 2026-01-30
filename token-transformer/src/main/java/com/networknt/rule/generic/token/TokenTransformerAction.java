package com.networknt.rule.generic.token;
import com.networknt.client.ClientConfig;
import com.networknt.config.Config;
import com.networknt.config.JsonMapper;
import com.networknt.http.client.HttpClientRequest;
import com.networknt.http.client.ssl.ClientX509ExtendedTrustManager;
import com.networknt.http.client.ssl.TLSConfig;
import com.networknt.rule.RequestTransformAction;
import com.networknt.rule.RuleActionValue;
import com.networknt.rule.generic.token.exception.TokenRequestException;
import com.networknt.rule.generic.token.exception.TokenRequestInterruptedException;
import com.networknt.rule.generic.token.schema.RequestSchema;
import com.networknt.rule.generic.token.schema.SharedVariableSchema;
import com.networknt.rule.generic.token.schema.TokenSchema;
import com.networknt.rule.generic.token.schema.UpdateSchema;
import com.networknt.server.ModuleRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.net.ssl.*;
import java.io.IOException;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.Duration;
import java.util.*;

import static com.networknt.utility.Constants.ERROR_MESSAGE;

/**
 * Generic plugin for updating an in-flight request or response with a new/different token.
 * Tokens are either retrieved from a specified token service, or are taken from cache if not expired yet.
 * TokenSchema configurations allow for different types of token requests to be defined. (i.e. url-encoded, application/json, JWT construction, 2-Way-SSL, etc.).
 *
 * @author Kalev Gonvick
 */
public class TokenTransformerAction implements RequestTransformAction {

    private static final Logger LOG = LoggerFactory.getLogger(TokenTransformerAction.class);
    private static final TokenTransformerConfig CONFIG = TokenTransformerConfig.load();
    private final TokenKeyStoreManager keyStoreManager = new TokenKeyStoreManager();

    public TokenTransformerAction() {
        LOG.trace("Constructing token-transformer plugin");
        ModuleRegistry.registerPlugin(
                TokenTransformerAction.class.getPackage().getImplementationTitle(),
                TokenTransformerAction.class.getPackage().getImplementationVersion(),
                TokenTransformerConfig.CONFIG_NAME,
                TokenTransformerAction.class.getName(),
                Config.getNoneDecryptedInstance().getJsonMapConfigNoCache(TokenTransformerConfig.CONFIG_NAME),
                CONFIG.getModuleMasks()
        );
    }

    @Override
    public void performAction(String ruleId, String actionId, final Map<String, Object> objMap, final Map<String, Object> resultMap, final Collection<RuleActionValue> actionValues) {

        LOG.trace("TokenTransformer plugin starts with ruleId: {} actionId: {}.", ruleId, actionId);

        for (final var actionValue : actionValues) {

            if (actionValue.getActionValueId().equals(TokenTransformerConfig.TOKEN_SCHEMA)) {

                try {
                    this.handleTokenAction(actionValue.getValue(), resultMap);

                } catch (Exception e) {
                    LOG.error("Exception occurred while sending a new token request for schema '{}'", actionValue.getValue());
                    LOG.trace("TokenTransformer plugin ends with error.", e);
                    Thread.currentThread().interrupt();
                    resultMap.put(ERROR_MESSAGE, e.getMessage());
                    return;
                }
            }
        }
        LOG.trace("TokenTransformer plugin ends.");
    }

    /**
     * When an in-flight request/response has a matching tokenSchema, handle the token action for the in-flight request/response.
     * @param tokenSchema - defined schema for in-flight request/response.
     * @param resultMap - outbound map that stores the action result.
     *
     * @throws InterruptedException - Occurs when sending the request to the token service fails.
     */
    public void handleTokenAction(final String tokenSchema, final Map<String, Object> resultMap) throws InterruptedException {

        if (CONFIG.getTokenSchemas() == null)
            return;

        final var schema = CONFIG.getTokenSchemas().get(tokenSchema);

        if (schema != null) {

            if (this.isExpired(schema)) {

                LOG.debug("Cached token is expired. Requesting a new token.");

                if (schema.getTokenRequest().getJwtSchema() != null) {
                    final var constructedJwt = this.buildJwtToken(schema.getTokenRequest());
                    if(LOG.isTraceEnabled()) LOG.trace("Generated jwt = {}", constructedJwt);
                    schema.getSharedVariables().setConstructedJwt(constructedJwt);
                }

                final var client = this.getTokenSchemaHttpClient(schema.getTokenRequest());
                final var request = this.getTokenSchemaHttpRequest(schema.getTokenRequest(), schema.getSharedVariables());
                final var response = this.sendRequest(client, request);

                if (response.statusCode() >= 200 && response.statusCode() <= 299) {

                    /* update sharedVariables from http response */
                    schema.getTokenSource().writeResponseToSharedVariables(schema.getSharedVariables(), response);

                    if (schema.getTokenUpdate().isUpdateExpirationFromTtl())
                        schema.getSharedVariables().updateExpiration();

                    /* write new values to 'update' section of the tokenSchema */
                    updateResultMapFromSchema(schema.getTokenUpdate(), schema.getSharedVariables(), resultMap);

                } else LOG.error("The token request returned statusCode: '{}'", response.statusCode());

            } else {

                LOG.debug("Cached token is not expired. Updating result map from cached token data.");
                updateResultMapFromSchema(schema.getTokenUpdate(), schema.getSharedVariables(), resultMap);
            }

        } else throw new IllegalArgumentException("Provided token schema '" + tokenSchema + "' does not exist!");

    }

    /**
     * Sends the token request based on the constructed client and request.
     *
     * @param client - created client for the schema.
     * @param request - created request for the schema.
     * @return - returns a response from the token service.
     *
     * @throws InterruptedException - if the request is interrupted on the thread.
     */
    public HttpResponse<String> sendRequest(final HttpClient client, final HttpRequest request) throws InterruptedException {

        try {
            final HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (LOG.isTraceEnabled())
                LOG.trace("Status = {}, ResponseBody = {}", response.statusCode(), response.body());

            return response;

        } catch (IOException e) {
            LOG.error("Exception:", e);
            LOG.trace("URI = {}, Headers = {}, Method = {} ", request.uri(), request.headers(), request.method());
            throw new TokenRequestException(request);

        } catch (InterruptedException e) {

            if (Thread.interrupted())
                throw new InterruptedException();

            else throw new TokenRequestInterruptedException(request);
        }
    }

    /**
     * Gets the http request used in the token service call.
     * It's either created or retrieved from cache.
     *
     * @param schema - token schema for in-flight request/response.
     * @return - HttpRequest for the token service.
     */
    private HttpRequest getTokenSchemaHttpRequest(final RequestSchema schema, final SharedVariableSchema sharedVariableSchema) {

        /* Use the same request if it already exists and if we do not have a defined JWT schema. */
        if (this.shouldBuildHttpRequest(schema)) {

            /* build our request, and cache it for later. */
            final var httpRequestBuilder = new HttpTokenRequestBuilder(schema.getUrl())
                    .withHeaders(schema.getResolvedHeaders(sharedVariableSchema))
                    .withBody(schema.getResolvedBody(sharedVariableSchema), schema.getType());

            schema.setHttpRequest(httpRequestBuilder.build());
        }

        return schema.getHttpRequest();
    }

    /**
     * Gets the Http client for the token request. Either retrieved from cache or a creates a new client.
     *
     * @param schema - the token schema
     * @return - returns HttpClient for the request.
     */
    private HttpClient getTokenSchemaHttpClient(final RequestSchema schema) {

        if (this.shouldCreateHttpClient(schema)) {

            /* Use SSL configuration if we have one, otherwise create a default context */
            final var sslContext = this.createSSLContext(schema);
            final var clientBuilder = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .connectTimeout(Duration.ofMillis(ClientConfig.get().getTimeout()))
                    .sslContext(sslContext);

            if (CONFIG.getProxyHost() != null)
                clientBuilder.proxy(ProxySelector.of(new InetSocketAddress(
                        CONFIG.getProxyHost(),
                        CONFIG.getProxyPort() == 0 ? 443 : CONFIG.getProxyPort())
                ));

            if (CONFIG.isEnableHttp2())
                clientBuilder.version(HttpClient.Version.HTTP_2);

            else clientBuilder.version(HttpClient.Version.HTTP_1_1);


            if (ClientConfig.get().getMappedConfig().get(ClientConfig.TLS) instanceof Map) {

                // this a workaround to bypass the hostname verification in jdk11 http client.
                var tlsMap = (Map<String, Object>) ClientConfig.get().getMappedConfig().get(ClientConfig.TLS);
                if (tlsMap != null && !Boolean.TRUE.equals(tlsMap.get(TLSConfig.VERIFY_HOSTNAME))) {
                    final Properties props = System.getProperties();
                    props.setProperty("jdk.internal.httpclient.disableHostnameVerification", Boolean.TRUE.toString());
                }
                schema.setHttpClient(clientBuilder.build());

            } else throw new RuntimeException("Invalid client configuration provided.");


        }
        return schema.getHttpClient();
    }

    /**
     * Token requests that require a JWT to retrieve another will be constructed here.
     * JWT structure = { jwtHeader } . { jwtBody } . signed({ jwtHeader } . { jwtBody })
     *
     * @param schema - the token request schema
     * @return - returns a newly constructed jwt String.
     */
    private String buildJwtToken(final RequestSchema schema) {
        final var tokenBuilder = new StringBuilder();

        /* create JWT payload header */
        final var jwtHeaderMap = schema.getJwtSchema().getJwtHeader().buildJwtMap(schema.getJwtSchema().getJwtTtl(), schema.getJwtSchema().getTtlUnit());
        final var jwtHeaderString = JsonMapper.toJson(jwtHeaderMap);
        tokenBuilder.append(org.apache.commons.codec.binary.Base64.encodeBase64URLSafeString(jwtHeaderString.getBytes(StandardCharsets.UTF_8)));
        tokenBuilder.append(".");

        /* create JWT payload body */
        final var jwtBodyMap = schema.getJwtSchema().getJwtBody().buildJwtMap(schema.getJwtSchema().getJwtTtl(), schema.getJwtSchema().getTtlUnit());
        final var jwtBodyString = JsonMapper.toJson(jwtBodyMap);
        tokenBuilder.append(org.apache.commons.codec.binary.Base64.encodeBase64URLSafeString(jwtBodyString.getBytes(StandardCharsets.UTF_8)));
        if(LOG.isTraceEnabled()) LOG.trace("jwtHeaderString = {} jwtBodyString = {}", jwtHeaderString, jwtBodyString);

        final var privateKey = this.keyStoreManager.getPrivateKey(
                schema.getJwtSchema().getKeyStore().getName(),
                schema.getJwtSchema().getKeyStore().getPassword(),
                schema.getJwtSchema().getKeyStore().getAlias(),
                schema.getJwtSchema().getKeyStore().getKeyPass()
        );

        if(LOG.isTraceEnabled()) LOG.trace("Created PrivateKey with name {} password {} alias {} keyPass {}",
                schema.getJwtSchema().getKeyStore().getName(),
                schema.getJwtSchema().getKeyStore().getPassword(),
                schema.getJwtSchema().getKeyStore().getAlias(),
                schema.getJwtSchema().getKeyStore().getKeyPass()
        );

        /* create a signed payload from 'jwtHeader' + '.' + 'jwtBody'  */
        if(LOG.isTraceEnabled()) LOG.trace("JWT Algorithm = {}", schema.getJwtSchema().getAlgorithm());
        final Signature signature;
        try {
            signature = Signature.getInstance(schema.getJwtSchema().getAlgorithm());
        } catch (NoSuchAlgorithmException e) {
            LOG.error("NoSuchAlgorithmException", e);
            throw new IllegalArgumentException("Algorithm '" + schema.getJwtSchema().getAlgorithm() + "' is invalid.");
        }


        try {
            signature.initSign(privateKey);

        } catch (InvalidKeyException e) {
            LOG.error("InvalidKeyException", e);
            throw new IllegalArgumentException("Invalid key for selected algorithm '" + schema.getJwtSchema().getAlgorithm() +"'.");
        }

        final String signedPayload;
        try {
            signature.update(tokenBuilder.toString().getBytes(StandardCharsets.UTF_8));
            signedPayload = org.apache.commons.codec.binary.Base64.encodeBase64URLSafeString(signature.sign());

        } catch (SignatureException e) {
            LOG.error("SignatureException", e);
            throw new IllegalArgumentException("Invalid signature for JWT.");
        }

        /* Append 'unsigned payload' + '.'  + 'signed payload' together. */
        tokenBuilder.append(".");
        tokenBuilder.append(signedPayload);
        return tokenBuilder.toString();
    }

    /**
     * Token requests that specify needing a different SSLContext will be constructed here.
     * Otherwise, the default context will be used.
     *
     * @param schema - the token request schema.
     * @return - returns the ssl context for the token request.
     */
    private SSLContext createSSLContext(final RequestSchema schema) {

        if (schema.getSslContextSchema() == null) {

            try {

                LOG.debug("Creating default SSL context from client.yml.");

                return HttpClientRequest.createSSLContext();

            } catch (IOException e) {
                throw new RuntimeException("Could not create SSL context: " + e.getMessage());
            }
        }

        /* Create a new context if we don't have one cached, or if we have cache disabled. */
        if (schema.getSslContext() == null || !schema.isCacheSSLContext()) {

            LOG.debug("Creating new SSL context from the SSL context schema.");

            /* keystore */
            final var keyStoreName = schema.getSslContextSchema().getKeyStore().getName();
            final var keyStorePass = schema.getSslContextSchema().getKeyStore().getPassword();
            final var keyPass = schema.getSslContextSchema().getKeyStore().getKeyPass();
            final var keyStoreAlgorithm = schema.getSslContextSchema().getKeyStore().getAlgorithm();
            KeyManager[] keyManagers = this.keyStoreManager.getKeyManagers(keyStoreName, keyStorePass, keyPass, keyStoreAlgorithm);

            /* truststore  */
            final var trustStoreName = schema.getSslContextSchema().getTrustStore().getName();
            final var trustStorePass = schema.getSslContextSchema().getTrustStore().getPassword();
            final var trustStoreAlgorithm = schema.getSslContextSchema().getTrustStore().getAlgorithm();
            TrustManager[] trustManagers = this.keyStoreManager.getTrustManagers(trustStoreName, trustStorePass, trustStoreAlgorithm);

            final SSLContext sslContext;
            try {
                sslContext = SSLContext.getInstance(schema.getSslContextSchema().getTlsVersion());

            } catch (NoSuchAlgorithmException e) {
                throw new IllegalArgumentException("Requested algorithm '" + schema.getSslContextSchema().getTlsVersion() + "' is not available." + e.getMessage());
            }

            if (trustManagers.length == 0) {
                throw new IllegalStateException("No trust managers found for SSL context.");

            } else {
                TrustManager[] extendedTrustManagers = { new ClientX509ExtendedTrustManager(Arrays.asList(trustManagers)) };

                try {
                    sslContext.init(keyManagers, extendedTrustManagers, null);

                } catch (KeyManagementException e) {
                    throw new RuntimeException("Exception occurred when initializing ssl context. " + e.getMessage());
                }
            }

            schema.setSslContext(sslContext);
        }
        return schema.getSslContext();
    }

    /**
     * Based on the configured update schema. Populate the result map with the resulting token data.
     *
     * @param update - the update schema.
     * @param resultMap - the to-be populated result map.
     */
    private void updateResultMapFromSchema(final UpdateSchema update, final SharedVariableSchema sharedVariables, final Map<String, Object> resultMap) {
        if (update.getBody() != null && !update.getBody().isEmpty()) {
            final var requestBodyUpdateMap = new HashMap<>(update.getResolvedBody(sharedVariables));
            resultMap.put("requestBody", requestBodyUpdateMap);
        }

        Map<String, Object> requestHeaders = (Map)resultMap.get(REQUEST_HEADERS);
        if(requestHeaders == null) {
            requestHeaders = new HashMap<>();
            resultMap.put(REQUEST_HEADERS, requestHeaders);
        }
        if (update.getHeaders() != null && !update.getHeaders().isEmpty()) {
            Map<String, String> updateMap = (Map<String, String>)requestHeaders.get(UPDATE);
            if(updateMap == null) {
                updateMap = new HashMap<>();
                requestHeaders.put(UPDATE, updateMap);
            }
            updateMap.putAll(update.getResolvedHeaders(sharedVariables));
        }
    }

    /**
     * Returns true if you a new client should be created.
     * If the client is null or if we do not want to cache the client, a new client will need to be created.
     *
     * @param schema - the schema containing both the client and cache configuration.
     * @return - true if client should be created.
     */
    private boolean shouldCreateHttpClient(final RequestSchema schema) {
        return schema.getHttpClient() == null
                || !schema.isCacheHttpClient();
    }

    /**
     * Returns true if you a new HTTP client should be created.
     * If the request is null or if we have a JWT configuration, a new request will need to be created.
     *
     * @param schema - the schema containing the http request and possible jwt configuration.
     * @return - true if request should be created.
     */
    private boolean shouldBuildHttpRequest(final RequestSchema schema) {
        return schema.getHttpRequest() == null
                || schema.getJwtSchema() != null;
    }

    /**
     * Checks to see if the token is expired including the waitLength grace period.
     *
     * @param schema - token configuration
     * @return - true if token is not expired
     */
    private boolean isExpired(final TokenSchema schema) {
        final var waitLengthUnit = schema.getSharedVariables().getTokenTtlUnit();
        final var waitLengthAsMillis = waitLengthUnit.unitToMillis(schema.getSharedVariables().getWaitLength());
        return System.currentTimeMillis() >= (schema.getSharedVariables().getExpiration() - waitLengthAsMillis);
    }


}
