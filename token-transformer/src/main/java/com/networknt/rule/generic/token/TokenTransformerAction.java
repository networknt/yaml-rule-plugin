package com.networknt.rule.generic.token;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.networknt.client.ClientConfig;
import com.networknt.config.Config;
import com.networknt.config.JsonMapper;
import com.networknt.http.client.HttpClientRequest;
import com.networknt.http.client.ssl.ClientX509ExtendedTrustManager;
import com.networknt.http.client.ssl.TLSConfig;
import com.networknt.rule.IAction;
import com.networknt.rule.RuleActionValue;
import com.networknt.rule.RuleConstants;
import com.networknt.rule.generic.token.schema.RequestSchema;
import com.networknt.rule.generic.token.schema.TokenSchema;
import com.networknt.rule.generic.token.schema.UpdateSchema;
import com.networknt.utility.ModuleRegistry;
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

import static org.apache.commons.codec.binary.Base64.encodeBase64URLSafeString;

public class TokenTransformerAction implements IAction {

    private static final Logger LOG = LoggerFactory.getLogger(TokenTransformerAction.class);
    private static final TokenTransformerConfig CONFIG = TokenTransformerConfig.load();
    private final TokenKeyStoreManager keyStoreManager = new TokenKeyStoreManager();

    public TokenTransformerAction() throws URISyntaxException, JsonProcessingException {
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
    public void performAction(final Map<String, Object> objMap, final Map<String, Object> resultMap, final Collection<RuleActionValue> actionValues) {
        LOG.trace("TokenTransformer plugin starts.");
        for (final var actionValue : actionValues) {
            if (actionValue.getActionValueId().equals(TokenTransformerConfig.TOKEN_SCHEMA)) {
                try {
                    this.handleTokenAction(actionValue.getValue(), resultMap);
                } catch (InterruptedException e) {
                    LOG.error("Exception occurred while sending a new token request for schema '{}'", actionValue.getValue());
                    LOG.trace("TokenTransformer plugin ends with error.");
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
        LOG.trace("TokenTransformer plugin ends.");
        resultMap.put(RuleConstants.RESULT, true);
    }

    public void handleTokenAction(final String tokenSchema, final Map<String, Object> resultMap) throws InterruptedException {

        if (CONFIG.getTokenSchemas() == null)
            return;

        final var schema = CONFIG.getTokenSchemas().get(tokenSchema);
        if (schema != null) {
            if (System.currentTimeMillis() >= schema.getPathPrefixAuth().getExpiration()) {
                LOG.debug("Cached token is expired. Requesting a new token.");
                final var client = this.getTokenSchemaHttpClient(schema);
                final var request = this.getTokenSchemaHttpRequest(schema);

                final HttpResponse<?> response;
                try {
                    response = client.send(request, HttpResponse.BodyHandlers.ofString());
                    LOG.trace("Status = {}, ResponseBody = {}", response.statusCode(), response.body());
                } catch (IOException e) {
                    LOG.trace("URI = {}, Headers = {}, Method = {} ", request.uri(), request.headers(), request.method());
                    throw new RuntimeException("Exception while trying to send a request." + e.getMessage());
                }
                if (response.statusCode() == 200) {

                    /* update pathPrefix from http response */
                    schema.getTokenSource().writeResponseToPathPrefix(schema.getPathPrefixAuth(), response);

                    /* write new values to 'update' section of the tokenSchema */
                    schema.getTokenUpdate().writeSchemaFromPathPrefix(schema.getPathPrefixAuth());
                    updateResultMapFromSchema(schema.getTokenUpdate(), resultMap);
                } else {
                    LOG.error("The token request returned statusCode: '{}'", response.statusCode());
                }
            } else {
                // TODO - Do we still update resultMap with cachedToken?
                LOG.debug("Cached token is not expired");
                updateResultMapFromSchema(schema.getTokenUpdate(), resultMap);

            }

        } else throw new IllegalArgumentException("Provided token schema '" + tokenSchema + "' does not exist!");

    }

    private HttpRequest getTokenSchemaHttpRequest(final TokenSchema schema) {

        /* Use the same request if it already exists and if we do not have a defined JWT schema. */
        if (this.shouldBuildHttpRequest(schema)) {

            /* if we have a JWT schema defined, create one and save it under accessToken temporarily. */
            if (schema.getTokenRequest().getJwtSchema() != null) {
                final var constructedJwt = this.buildJwtToken(schema.getTokenRequest());
                schema.getPathPrefixAuth().setAccessToken(constructedJwt);
            }

            /* populate the request schema with our pathPrefixAuth data. */
            schema.getTokenRequest().writeSchemaFromPathPrefix(schema.getPathPrefixAuth());

            /* build our request, and cache it for later. */
            final var httpRequestBuilder = new HttpTokenRequestBuilder(schema.getTokenRequest().getUrl())
                    .withHeaders(schema.getTokenRequest().getHeaders())
                    .withBody(schema.getTokenRequest().getBody(), schema.getTokenRequest().getType());

            schema.getTokenRequest().setHttpRequest(httpRequestBuilder.build());
        }

        return schema.getTokenRequest().getHttpRequest();
    }

    private HttpClient getTokenSchemaHttpClient(final TokenSchema schema) {

        if (this.shouldCreateHttpClient(schema)) {

            /* Use SSL configuration if we have one, otherwise create a default context */
            final var sslContext = this.createSSLContext(schema.getTokenRequest());
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

            // this a workaround to bypass the hostname verification in jdk11 http client.
            var tlsMap = (Map<String, Object>) ClientConfig.get().getMappedConfig().get(ClientConfig.TLS);

            if (tlsMap != null && !Boolean.TRUE.equals(tlsMap.get(TLSConfig.VERIFY_HOSTNAME))) {
                final Properties props = System.getProperties();
                props.setProperty("jdk.internal.httpclient.disableHostnameVerification", Boolean.TRUE.toString());
            }
            schema.getPathPrefixAuth().setHttpClient(clientBuilder.build());
        }
        return schema.getPathPrefixAuth().getHttpClient();
    }

    private String buildJwtToken(final RequestSchema schema) {
        final var tokenBuilder = new StringBuilder();

        /* create JWT payload header */
        final var jwtHeaderMap = schema.getJwtSchema().getJwtHeader().buildJwtMap(schema.getJwtSchema().getJwtTtl());
        final var jwtHeaderString = JsonMapper.toJson(jwtHeaderMap);
        tokenBuilder.append(org.apache.commons.codec.binary.Base64.encodeBase64URLSafeString(jwtHeaderString.getBytes(StandardCharsets.UTF_8)));
        tokenBuilder.append(".");

        /* create JWT payload body */
        final var jwtBodyMap = schema.getJwtSchema().getJwtBody().buildJwtMap(schema.getJwtSchema().getJwtTtl());
        final var jwtBodyString = JsonMapper.toJson(jwtBodyMap);
        tokenBuilder.append(org.apache.commons.codec.binary.Base64.encodeBase64URLSafeString(jwtBodyString.getBytes(StandardCharsets.UTF_8)));

        // TODO - change 'alias pass' to 'keypass'.
        final var privateKey = this.keyStoreManager.getPrivateKey(
                schema.getJwtSchema().getKeyStore().getName(),
                schema.getJwtSchema().getKeyStore().getPassword(),
                schema.getJwtSchema().getKeyStore().getAlias(),
                schema.getJwtSchema().getKeyStore().getAliasPass()
        );

        /* create a signed payload from 'jwtHeader' + '.' + 'jwtBody'  */
        final Signature signature;
        try {
            signature = Signature.getInstance(schema.getJwtSchema().getAlgorithm());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalArgumentException("Algorithm '" + schema.getJwtSchema().getAlgorithm() + "' is invalid.");
        }

        try {
            signature.initSign(privateKey);
        } catch (InvalidKeyException e) {
            throw new IllegalArgumentException("Invalid key for selected algorithm '" + schema.getJwtSchema().getAlgorithm() +"'.");
        }

        final String signedPayload;
        try {
            signature.update(tokenBuilder.toString().getBytes(StandardCharsets.UTF_8));
            signedPayload = org.apache.commons.codec.binary.Base64.encodeBase64URLSafeString(signature.sign());
        } catch (SignatureException e) {
            throw new IllegalArgumentException("Invalid signature for JWT.");
        }

        /* append unsigned payload + signed payload together */
        tokenBuilder.append(".");

        tokenBuilder.append(signedPayload);
        return tokenBuilder.toString();
    }

    private SSLContext createSSLContext(final RequestSchema schema) {

        // TODO - do we want to use default context
        if (schema.getSslContextSchema() == null) {
            try {
                LOG.trace("Creating default SSL context from client.yml.");
                return HttpClientRequest.createSSLContext();
            } catch (IOException e) {
                throw new RuntimeException("Could not create SSL context: " + e.getMessage());
            }
        }

        /* Create a new context if we don't have one cached, or if we have cache disabled. */
        if (schema.getSslContext() == null || !schema.isCacheSSLContext()) {
            LOG.trace("Creating new SSL context from the SSL context schema.");

            /* keystore */
            final var keyStoreName = schema.getSslContextSchema().getKeyStore().getName();
            final var keyStorePass = schema.getSslContextSchema().getKeyStore().getPassword();
            final var keyPass = schema.getSslContextSchema().getKeyStore().getAliasPass();
            KeyManager[] keyManagers = this.keyStoreManager.getKeyManagers(keyStoreName, keyStorePass, keyPass, null);

            /* truststore  */
            final var trustStoreName = schema.getSslContextSchema().getTrustStore().getName();
            final var trustStorePass = schema.getSslContextSchema().getTrustStore().getPassword();
            TrustManager[] trustManagers = this.keyStoreManager.getTrustManagers(trustStoreName, trustStorePass, null);

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
     * Based on the configured update schema. Populate the result map with data.
     *
     * @param update - the update schema.
     * @param resultMap - the to-be populated result map.
     */
    private void updateResultMapFromSchema(final UpdateSchema update, final Map<String, Object> resultMap) {
        switch (update.getDirection()) {
            case REQUEST:
                if (update.getBody() != null && !update.getBody().isEmpty()) {
                    final var requestBodyUpdateMap = new HashMap<>(update.getBody());
                    resultMap.put("requestBody", requestBodyUpdateMap);
                }
                if (update.getHeaders() != null && !update.getHeaders().isEmpty()) {
                    final var requestHeaderUpdateMap = new HashMap<>(update.getHeaders());
                    final var updateHeaderMap = new HashMap<String, Object>();
                    updateHeaderMap.put("update", requestHeaderUpdateMap);
                    resultMap.put("requestHeaders", updateHeaderMap);
                }
                break;
            case RESPONSE:
                if (update.getBody() != null && !update.getBody().isEmpty()) {
                    final var responseBodyUpdateMap = new HashMap<>(update.getBody());
                    resultMap.put("responseBody", responseBodyUpdateMap);
                }
                if (update.getHeaders() != null && !update.getHeaders().isEmpty()) {
                    final var responseHeaderUpdateMap = new HashMap<>(update.getHeaders());
                    final var updateHeaderMap = new HashMap<String, Object>();
                    updateHeaderMap.put("update", responseHeaderUpdateMap);
                    resultMap.put("responseHeaders", updateHeaderMap);
                }
                break;
            default:
                throw new IllegalArgumentException("Invalid update direction '" + update.getDirection().toString() + "'.");
        }
    }

    /**
     * Returns true if you a new client should be created.
     * If the client is null or if we do not want to cache the client, a new client will need to be created.
     *
     * @param schema - the schema containing both the client and cache configuration.
     * @return - true if client should be created.
     */
    private boolean shouldCreateHttpClient(final TokenSchema schema) {
        return schema.getPathPrefixAuth().getHttpClient() == null
                || !schema.getTokenRequest().isCacheHttpClient();
    }

    /**
     * Returns true if you a new HTTP client should be created.
     * If the request is null or if we have a JWT configuration, a new request will need to be created.
     *
     * @param schema - the schema containing the http request and possible jwt configuration.
     * @return - true if request should be created.
     */
    private boolean shouldBuildHttpRequest(final TokenSchema schema) {
        return schema.getTokenRequest().getHttpRequest() == null
                || schema.getTokenRequest().getJwtSchema() != null;
    }


}
