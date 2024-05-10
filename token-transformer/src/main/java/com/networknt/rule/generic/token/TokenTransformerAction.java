package com.networknt.rule.generic.token;

import com.networknt.client.ClientConfig;
import com.networknt.config.Config;
import com.networknt.proxy.PathPrefixAuth;
import com.networknt.http.client.HttpClientRequest;
import com.networknt.http.client.ssl.TLSConfig;
import com.networknt.rule.IAction;
import com.networknt.rule.RuleActionValue;
import com.networknt.rule.RuleConstants;
import com.networknt.utility.ModuleRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.*;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.*;

/**
 * Called from the request interceptor, we make an outbound call to a token provider and attach the token to the request.
 * Each step can be configured to account for different token request flows, different response payloads, and different ways of storing the returned token.
 *
 * @author Kalev Gonvick
 */
public class TokenTransformerAction implements IAction {

    private static final Logger LOG = LoggerFactory.getLogger(TokenTransformerAction.class);
    private static final TokenTransformerConfig CONFIG = TokenTransformerConfig.load();
    private static HttpClient httpClient;

    public TokenTransformerAction() {
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
    public void performAction(Map<String, Object> objMap, Map<String, Object> resultMap, Collection<RuleActionValue> actionValues) {
        resultMap.put(RuleConstants.RESULT, true);
        var requestPath = (String) objMap.get("requestPath");

        LOG.trace("requestPath = {}", requestPath);

        for (var authPath : CONFIG.getPathPrefixAuths()) {

            if (requestPath.startsWith(authPath.getPathPrefix())) {
                final var action = new TokenAction(actionValues, authPath);

                if (System.currentTimeMillis() >= authPath.getExpiration()) {
                    LOG.trace("Cached token '{}' is expired with current time '{}' and expired time '{}'",
                            authPath.getAccessToken() != null ? authPath.getAccessToken().substring(0, 20) : null,
                            System.currentTimeMillis(),
                            authPath.getExpiration()
                    );

                    /* token is expired, build the token request */
                    action.buildRequest();

                    if (httpClient == null) {
                        httpClient = createClient();
                        if (httpClient == null) {
                            LOG.error("Could not create client!");
                            return;
                        }
                    }
                    action.requestToken(httpClient, resultMap);

                } else {
                    LOG.trace("Cached token '{}' is NOT expired. Updating resultMap using cache.",
                            authPath.getAccessToken() != null ? authPath.getAccessToken().substring(0, 20) : null
                    );
                    action.useCachedToken(resultMap);
                }
            }
        }
    }

    private static HttpClient createClient() {
        try {
            var clientBuilder = HttpClient.newBuilder()
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .connectTimeout(Duration.ofMillis(ClientConfig.get().getTimeout()))
                    .sslContext(HttpClientRequest.createSSLContext());

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

            return clientBuilder.build();

        } catch (IOException e) {
            LOG.error("Cannot create HttpClient:", e);
            return null;
        }
    }
}
