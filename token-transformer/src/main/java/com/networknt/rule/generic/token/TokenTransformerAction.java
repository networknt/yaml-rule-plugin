package com.networknt.rule.generic.token;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.networknt.client.ClientConfig;
import com.networknt.config.Config;
import com.networknt.config.JsonMapper;
import com.networknt.config.TlsUtil;
import com.networknt.http.client.HttpClientRequest;
import com.networknt.http.client.ssl.ClientX509ExtendedTrustManager;
import com.networknt.http.client.ssl.TLSConfig;
import com.networknt.rule.IAction;
import com.networknt.rule.RuleActionValue;
import com.networknt.rule.RuleConstants;
import com.networknt.rule.generic.token.schema.cert.KeyStoreSchema;
import com.networknt.rule.generic.token.schema.cert.TrustStoreSchema;
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

    public TokenTransformerAction() throws URISyntaxException, JsonProcessingException {
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
        for (final var actionValue : actionValues) {
            if (actionValue.getActionValueId().equals(TokenTransformerConfig.TOKEN_SCHEMA)) {
                try {
                    this.handleTokenAction(actionValue.getValue(), resultMap);
                } catch (URISyntaxException e) {
                    LOG.error("Invalid token URL provided for tokenSchema '{}'", actionValue.getValue());
                    return;
                } catch (IOException e) {
                    LOG.error("Could not create HTTP Client for schema '{}'. Ended with exception {}", actionValue.getValue(), e.getMessage());
                    return;
                } catch (InterruptedException e) {
                    LOG.error("Exception occurred while sending a new token request for schema '{}'", actionValue.getValue());
                    Thread.currentThread().interrupt();
                    return;
                } catch (UnrecoverableKeyException e) {
                    LOG.error("Exception finding key: '{}'", e.getMessage());
                    return;
                } catch (NoSuchAlgorithmException e) {
                    LOG.error("Unknown algorithm: '{}'", e.getMessage());
                    return;
                } catch (KeyStoreException e) {
                    LOG.error("Exception while loading key store: '{}'", e.getMessage());
                    return;
                } catch (SignatureException e) {
                    LOG.error("Exception while signing JWT: '{}'", e.getMessage());
                    return;
                } catch (InvalidKeyException e) {
                    LOG.error("Exception while loading key: '{}'", e.getMessage());
                    return;
                } catch (KeyManagementException e) {
                    LOG.error("Exception while loading key manager: '{}'", e.getMessage());
                    return;
                }
            }
        }
        resultMap.put(RuleConstants.RESULT, true);
    }

    public void handleTokenAction(final String tokenSchema, final Map<String, Object> resultMap) throws
            URISyntaxException,
            IOException,
            InterruptedException,
            UnrecoverableKeyException,
            NoSuchAlgorithmException,
            KeyStoreException,
            SignatureException,
            InvalidKeyException,
            KeyManagementException {
        final var schema = CONFIG.getTokenSchemas().get(tokenSchema);
        if (schema != null) {
            if (System.currentTimeMillis() >= schema.getPathPrefixAuth().getExpiration()) {
                final var client = this.getTokenSchemaHttpClient(schema);
                final var request = this.getTokenSchemaHttpRequest(schema);
                this.requestNewToken(schema, client, request, resultMap);
            } else {
                // TODO - Do we still update resultMap with cachedToken?
                LOG.info("Cached token is not expired");

            }

        } else throw new IllegalArgumentException("Provided token schema '" + tokenSchema + "' does not exist!");

    }

    private HttpRequest getTokenSchemaHttpRequest(final TokenSchema schema) throws
            URISyntaxException,
            JsonProcessingException,
            UnrecoverableKeyException,
            NoSuchAlgorithmException,
            KeyStoreException,
            SignatureException,
            InvalidKeyException {

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

    private HttpClient getTokenSchemaHttpClient(final TokenSchema schema) throws
            IOException,
            NoSuchAlgorithmException,
            KeyManagementException,
            UnrecoverableKeyException,
            KeyStoreException {
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

    private String buildJwtToken(final RequestSchema schema) throws
            NoSuchAlgorithmException,
            InvalidKeyException,
            UnrecoverableKeyException,
            KeyStoreException,
            SignatureException {

        final var tokenBuilder = new StringBuilder();

        /* create JWT payload header */
        final var jwtHeaderMap = schema.getJwtSchema().getJwtHeader().buildJwtMap(schema.getJwtSchema().getJwtTtl());
        final var jwtHeaderString = JsonMapper.toJson(jwtHeaderMap);
        tokenBuilder.append(encodeBase64URLSafeString(jwtHeaderString.getBytes(StandardCharsets.UTF_8)));
        tokenBuilder.append(".");

        /* create JWT payload body */
        final var jwtBodyMap = schema.getJwtSchema().getJwtBody().buildJwtMap(schema.getJwtSchema().getJwtTtl());
        final var jwtBodyString = JsonMapper.toJson(jwtBodyMap);
        tokenBuilder.append(encodeBase64URLSafeString(jwtBodyString.getBytes(StandardCharsets.UTF_8)));
        tokenBuilder.append(".");

        /* load the keystore */
        final var keystore = TlsUtil.loadKeyStore(
                schema.getJwtSchema().getKeyStore().getName(),
                schema.getJwtSchema().getKeyStore().getPassword().toCharArray()
        );

        /* load key from keystore based on provided alias */
        final var privateKey = (PrivateKey) keystore.getKey(
                schema.getJwtSchema().getKeyStore().getAlias(),
                schema.getJwtSchema().getKeyStore().getAliasPass().toCharArray()
        );

        /* create a signed payload from 'jwtHeader' + '.' + 'jwtBody'  */
        final var signature = Signature.getInstance(schema.getJwtSchema().getAlgorithm());
        signature.initSign(privateKey);
        signature.update(tokenBuilder.toString().getBytes(StandardCharsets.UTF_8));
        final var signedPayload = org.apache.commons.codec.binary.Base64.encodeBase64URLSafeString(signature.sign());

        /* append unsigned payload + signed payload together */
        tokenBuilder.append(signedPayload);
        return tokenBuilder.toString();
    }

    private SSLContext createSSLContext(final RequestSchema schema) throws
            NoSuchAlgorithmException,
            KeyManagementException,
            KeyStoreException,
            UnrecoverableKeyException,
            IOException {

        // TODO - do we want to use default context
        if (schema.getSslContextSchema() == null)
            return HttpClientRequest.createSSLContext();

        /* Create a new context if we don't have one cached, or if we have cache disabled. */
        if (schema.getSslContext() == null || !schema.isCacheSSLContext()) {

            /* keystore */
            KeyManager[] keyManagers = null;
            if (schema.getSslContextSchema().getKeyStore() != null) {
                keyManagers = this.createKeyManagers(schema.getSslContextSchema().getKeyStore());
            }

            /* truststore  */
            List<TrustManager> trustManagerList = new ArrayList<>();
            if (schema.getSslContextSchema().getTrustStore() != null) {
                trustManagerList = this.createTrustManagers(schema.getSslContextSchema().getTrustStore());
            }

            SSLContext sslContext = null;
            sslContext = SSLContext.getInstance(schema.getSslContextSchema().getTlsVersion());
            if (trustManagerList.isEmpty()) {
                LOG.error("No trust store is loaded. Please check client.yml");
            } else {
                TrustManager[] extendedTrustManagers = { new ClientX509ExtendedTrustManager(trustManagerList) };
                sslContext.init(keyManagers, extendedTrustManagers, null);
            }

            schema.setSslContext(sslContext);
        }
        return schema.getSslContext();
    }

    private List<TrustManager> createTrustManagers(final TrustStoreSchema trustStoreSchema) throws KeyStoreException, NoSuchAlgorithmException {
        TrustManager[] trustManagers = null;
        List<TrustManager> trustManagerList = new ArrayList<>();
        // temp loading the certificate from the keystore instead of truststore from the config.
        final var trustStoreName = trustStoreSchema.getName();
        final var trustStorePass = trustStoreSchema.getPassword();
        LOG.trace("trustStoreName = {} trustStorePass = {}",
                trustStoreName,
                (trustStorePass == null ? null : trustStorePass.substring(0, 4))
        );
        if (trustStoreName != null && trustStorePass != null) {
            KeyStore trustStore = TlsUtil.loadKeyStore(trustStoreName, trustStorePass.toCharArray());
            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(trustStore);
            trustManagers = trustManagerFactory.getTrustManagers();
        }
        if (trustManagers != null && trustManagers.length > 0) {
            trustManagerList.addAll(Arrays.asList(trustManagers));
        }
        return trustManagerList;
    }

    private KeyManager[] createKeyManagers(final KeyStoreSchema keyStoreSchema) throws
            NoSuchAlgorithmException,
            UnrecoverableKeyException,
            KeyStoreException {
        // load key store for client certificate as two-way ssl is used.
        final var keyStoreName = keyStoreSchema.getName();
        final var keyStorePass = keyStoreSchema.getPassword();
        final var aliasPass = keyStoreSchema.getAliasPass();

        LOG.trace("keyStoreName = {} keyStorePass = {} keyPass = {}",
                keyStoreName,
                (keyStorePass == null ? null : keyStorePass.substring(0, 4)),
                (aliasPass == null ? null : aliasPass.substring(0, 4))
        );

        if (keyStoreName != null && keyStorePass != null && aliasPass != null) {
            final var keyStore = TlsUtil.loadKeyStore(keyStoreName, keyStorePass.toCharArray());

            // TODO - allow configuration of algorithm for loading key manager.
            final var keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            keyManagerFactory.init(keyStore, aliasPass.toCharArray());
            return keyManagerFactory.getKeyManagers();

        } else return new KeyManager[0];
    }

    private void requestNewToken(final TokenSchema schema, final HttpClient client, final HttpRequest request, final Map<String, Object> resultMap) throws IOException, InterruptedException {
        final HttpResponse<?> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200) {

            /* update pathPrefix from http response */
            schema.getTokenSource().writeResponseToPathPrefix(schema.getPathPrefixAuth(), response);

            /* write new values to 'update' section of the tokenSchema */
            schema.getTokenUpdate().writeSchemaFromPathPrefix(schema.getPathPrefixAuth());
            updateResultMapFromSchema(schema.getTokenUpdate(), resultMap);
        } else {
            LOG.error("The token request returned statusCode: '{}'", response.statusCode());
        }
    }

    private void updateResultMapFromSchema(final UpdateSchema update, final Map<String, Object> resultMap) {
        switch (update.getDirection()) {
            case REQUEST:
                if (!update.getBody().isEmpty()) {
                    final var requestBodyUpdateMap = new HashMap<>(update.getBody());
                    resultMap.put("requestBody", requestBodyUpdateMap);
                }
                if (!update.getHeaders().isEmpty()) {
                    final var requestHeaderUpdateMap = new HashMap<>(update.getHeaders());
                    final var updateHeaderMap = new HashMap<String, Object>();
                    updateHeaderMap.put("update", requestHeaderUpdateMap);
                    resultMap.put("requestHeaders", updateHeaderMap);
                }
                break;
            case RESPONSE:
                if (!update.getBody().isEmpty()) {
                    final var responseBodyUpdateMap = new HashMap<>(update.getBody());
                    resultMap.put("responseBody", responseBodyUpdateMap);
                }
                if (!update.getHeaders().isEmpty()) {
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

    private boolean shouldCreateHttpClient(final TokenSchema schema) {
        return schema.getPathPrefixAuth().getHttpClient() == null
                || !schema.getTokenRequest().isCacheHttpClient();
    }

    private boolean shouldBuildHttpRequest(final TokenSchema schema) {
        return schema.getTokenRequest().getHttpRequest() == null
                || schema.getTokenRequest().getJwtSchema() != null;
    }


}
