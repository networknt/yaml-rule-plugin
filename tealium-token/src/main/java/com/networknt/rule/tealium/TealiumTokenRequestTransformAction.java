package com.networknt.rule.tealium;

import com.networknt.client.ClientConfig;
import com.networknt.client.Http2Client;
import com.networknt.client.oauth.TokenResponse;
import com.networknt.client.ssl.TLSConfig;
import com.networknt.config.JsonMapper;
import com.networknt.proxy.PathPrefixAuth;
import com.networknt.rule.IAction;
import com.networknt.rule.RuleActionValue;
import com.networknt.rule.RuleConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * It is called from the request transform interceptor from the light-gateway to get the Tealium API access token with
 * the configured username and password. The token will then be put into the authorization header to replace the existing
 * one that is used to secure the connection between the consumer to the gateway.
 *
 * @author Steve Hu
 */
public class TealiumTokenRequestTransformAction implements IAction {
    private static final Logger logger = LoggerFactory.getLogger(TealiumTokenRequestTransformAction.class);
    // change the config to static so that it can cache the token retrieved until expiration time.
    private static final TealiumConfig config = TealiumConfig.load();
    private static HttpClient client;

    public TealiumTokenRequestTransformAction() {
        if(logger.isInfoEnabled()) logger.info("TealiumTokenRequestTransformAction is constructed");
    }

    @Override
    public void performAction(Map<String, Object> objMap, Map<String, Object> resultMap, Collection<RuleActionValue> actionValues) {
        resultMap.put(RuleConstants.RESULT, true);
        String requestPath = (String)objMap.get("requestPath");
        if(logger.isTraceEnabled()) logger.trace("requestPath = " + requestPath);

        for(PathPrefixAuth pathPrefixAuth: config.getPathPrefixAuths()) {
            if(requestPath.startsWith(pathPrefixAuth.getPathPrefix())) {
                if(logger.isTraceEnabled()) logger.trace("found with requestPath = " + requestPath + " prefix = " + pathPrefixAuth.getPathPrefix());
                if(System.currentTimeMillis() >= pathPrefixAuth.getExpiration()) {
                    if(logger.isTraceEnabled()) logger.trace("Cached token {} is expired with current time {} and expired time {}", pathPrefixAuth.getAccessToken() != null ? pathPrefixAuth.getAccessToken().substring(0, 20) : null, System.currentTimeMillis(), pathPrefixAuth.getExpiration());
                    TokenResponse tokenResponse = getAccessToken(pathPrefixAuth.getTokenUrl(), pathPrefixAuth.getUsername(), pathPrefixAuth.getPassword());
                    if(tokenResponse != null) {
                        pathPrefixAuth.setExpiration(System.currentTimeMillis() + pathPrefixAuth.getTokenTtl() * 1000 - 60000);
                        pathPrefixAuth.setAccessToken(tokenResponse.getAccessToken());
                        if(logger.isTraceEnabled()) logger.trace("Got a new token {} and cached it with expiration time {}", pathPrefixAuth.getAccessToken() != null ? pathPrefixAuth.getAccessToken().substring(0, 20) : null, pathPrefixAuth.getExpiration());
                    } else {
                        if(logger.isTraceEnabled()) logger.trace("tokenResponse is null");
                        return;
                    }
                }
                // either a new token is retrieved or cached token is not expired. Put the token into the Authorization header.
                if(pathPrefixAuth.getAccessToken() != null) {
                    Map<String, Object> requestHeaders = new HashMap<>();
                    Map<String, Object> updateMap = new HashMap<>();
                    updateMap.put("Authorization", "Bearer " + pathPrefixAuth.getAccessToken());
                    requestHeaders.put("update", updateMap);
                    resultMap.put("requestHeaders", requestHeaders);
                    return;
                }
            }
        }
    }

    private TokenResponse getAccessToken(String serverUrl, String username, String password) {
        TokenResponse tokenResponse = null;
        if(client == null) {
            try {
                HttpClient.Builder clientBuilder = HttpClient.newBuilder()
                        .followRedirects(HttpClient.Redirect.NORMAL)
                        .connectTimeout(Duration.ofMillis(ClientConfig.get().getTimeout()))
                        .sslContext(Http2Client.createSSLContext());
                if(config.getProxyHost() != null) clientBuilder.proxy(ProxySelector.of(new InetSocketAddress(config.getProxyHost(), config.getProxyPort() == 0 ? 443 : config.getProxyPort())));
                if(config.isEnableHttp2()) clientBuilder.version(HttpClient.Version.HTTP_2);
                // this a workaround to bypass the hostname verification in jdk11 http client.
                Map<String, Object> tlsMap = (Map<String, Object>)ClientConfig.get().getMappedConfig().get(Http2Client.TLS);
                if(tlsMap != null && !Boolean.TRUE.equals(tlsMap.get(TLSConfig.VERIFY_HOSTNAME))) {
                    final Properties props = System.getProperties();
                    props.setProperty("jdk.internal.httpclient.disableHostnameVerification", Boolean.TRUE.toString());
                }
                client = clientBuilder.build();
            } catch (IOException e) {
                logger.error("Cannot create HttpClient:", e);
                return null;
            }
        }
        try {
            if(serverUrl == null) {
                logger.error("tokenUrl is null");
                return null;
            }

            Map<String, String> parameters = new HashMap<>();
            parameters.put("username", username);
            parameters.put("key", password);

            String form = parameters.entrySet()
                    .stream()
                    .map(e -> e.getKey() + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                    .collect(Collectors.joining("&"));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(serverUrl))
                    .headers("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(form))
                    .build();

            HttpResponse<?> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            // {"token":"eyJ0eXAiOiJKV1QiLC...","host":"us-west-2-platform.tealiumapis.com"}
            if(response.statusCode() == 200) {
                // construct a token response and return it.
                Map<String, Object> map = JsonMapper.string2Map(response.body().toString());
                if(map != null) {
                    tokenResponse = new TokenResponse();
                    tokenResponse.setAccessToken((String)map.get("token"));
                    return tokenResponse;
                } else {
                    logger.error("response body cannot be parsed as a JSON " + response.body());
                    return null;
                }
            } else {
                logger.error("Error in getting the token with status code " + response.statusCode() + " and body " + response.body().toString());
                return null;
            }
        } catch (Exception e) {
            logger.error("Exception:", e);
            return null;
        }
    }
}
