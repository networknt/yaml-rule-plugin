package com.networknt.rule.snow;

import com.networknt.client.ClientConfig;
import com.networknt.client.Http2Client;
import com.networknt.client.oauth.TokenResponse;
import com.networknt.client.ssl.TLSConfig;
import com.networknt.config.Config;
import com.networknt.config.JsonMapper;
import com.networknt.proxy.PathPrefixAuth;
import com.networknt.rule.IAction;
import com.networknt.rule.RuleActionValue;
import com.networknt.rule.RuleConstants;
import com.networknt.utility.ModuleRegistry;
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
 * It is called from the request transform interceptor from the light-gateway to get the password grant type ServiceNow token
 * to replace the JWT token that consumers use to access the gateway. This allows the ServiceNow API to be exposed on the
 * Gateway as a standard API with client credentials token for API to API invocations.
 *
 * @author Steve Hu
 */
public class SnowTokenRequestTransformAction implements IAction {
    private static final Logger logger = LoggerFactory.getLogger(SnowTokenRequestTransformAction.class);
    // change the config to static so that it can cache the token retrieved until expiration time.
    private static final SnowConfig config = SnowConfig.load();
    private static HttpClient client;

    public SnowTokenRequestTransformAction() {
        if(logger.isInfoEnabled()) logger.info("SnowTokenRequestTransformAction is constructed");
        List<String> masks = new ArrayList<>();
        masks.add("password");
        masks.add("client_secret");
        ModuleRegistry.registerPlugin(
                SnowTokenRequestTransformAction.class.getPackage().getImplementationTitle(),
                SnowTokenRequestTransformAction.class.getPackage().getImplementationVersion(),
                SnowConfig.CONFIG_NAME,
                SnowTokenRequestTransformAction.class.getName(),
                Config.getNoneDecryptedInstance().getJsonMapConfigNoCache(SnowConfig.CONFIG_NAME),
                masks);
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
                    TokenResponse tokenResponse = getAccessToken(pathPrefixAuth.getTokenUrl(), pathPrefixAuth.getUsername(), pathPrefixAuth.getPassword(), pathPrefixAuth.getClientId(), pathPrefixAuth.getClientSecret(), pathPrefixAuth.getGrantType());
                    if(tokenResponse != null) {
                        pathPrefixAuth.setExpiration(System.currentTimeMillis() + tokenResponse.getExpiresIn() * 1000 - 60000);
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

    private TokenResponse getAccessToken(String serverUrl, String username, String password, String clientId, String clientSecret, String grantType) {
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
            parameters.put("password", password);
            parameters.put("client_id", clientId);
            parameters.put("client_secret", clientSecret);
            parameters.put("grant_type", grantType);

            String form = parameters.entrySet()
                    .stream()
                    .map(e -> e.getKey() + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                    .collect(Collectors.joining("&"));

            if(logger.isTraceEnabled()) logger.trace("request body = " + form);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(serverUrl))
                    .headers("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(form))
                    .build();

            HttpResponse<?> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            // {"access_token":"kldsfoo4...","refresh_token: "390343...", "scope": "useraccount","token_type":"Bearer", "expires_in":1079}
            if(response.statusCode() == 200) {
                // construct a token response and return it.
                Map<String, Object> map = JsonMapper.string2Map(response.body().toString());
                if(map != null) {
                    tokenResponse = new TokenResponse();
                    tokenResponse.setAccessToken((String)map.get("access_token"));
                    tokenResponse.setExpiresIn(((Integer)map.get("expires_in")));
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
