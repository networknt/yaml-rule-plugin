package com.networknt.rule.epam;

import com.networknt.client.ClientConfig;
import com.networknt.client.Http2Client;
import com.networknt.client.oauth.TokenResponse;
import com.networknt.client.ssl.TLSConfig;
import com.networknt.config.Config;
import com.networknt.config.JsonMapper;
import com.networknt.config.TlsUtil;
import com.networknt.proxy.PathPrefixAuth;
import com.networknt.rule.IAction;
import com.networknt.rule.RuleActionValue;
import com.networknt.rule.RuleConstants;
import com.networknt.utility.HashUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Signature;
import java.text.MessageFormat;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/**
 * It is called from the request transform interceptor from the light-gateway to get the conquest planning API access
 * token to put into the Authorization header in order to access the conquest planning APIs. For the original consumer,
 * it might have another token in the Authorization header for the gateway to verify in order to invoke the external
 * service handler. Once the verification is done, the Authorization header will be replaced with the conquest token
 * from the cache or retrieved from the conquest if it is expired or about to expire.
 *
 * @author Steve Hu
 */
public class EpamTokenRequestTransformAction implements IAction {
    private static final Logger logger = LoggerFactory.getLogger(EpamTokenRequestTransformAction.class);
    // change the config to static so that it can cache the token retrieved until expiration time.
    private static final EpamConfig config = EpamConfig.load();
    private static HttpClient client;

    public EpamTokenRequestTransformAction() {
        if(logger.isInfoEnabled()) logger.info("EpamTokenRequestTransformAction is constructed");
    }

    @Override
    public void performAction(Map<String, Object> objMap, Map<String, Object> resultMap, Collection<RuleActionValue> actionValues) {
        resultMap.put(RuleConstants.RESULT, true);
        String requestPath = (String)objMap.get("requestPath");
        if(logger.isTraceEnabled()) logger.trace("requestPath = " + requestPath);
        for(PathPrefixAuth pathPrefixAuth: config.getPathPrefixAuths()) {
            if(requestPath.startsWith(pathPrefixAuth.getPathPrefix())) {
                if(logger.isTraceEnabled()) logger.trace("found with requestPath = " + requestPath + " prefix = " + pathPrefixAuth.getPathPrefix());
                if(System.currentTimeMillis() >= (pathPrefixAuth.getExpiration())) {
                    if(logger.isTraceEnabled()) logger.trace("Cached token {} is expired with current time {} and expired time {}", pathPrefixAuth.getAccessToken() != null ? pathPrefixAuth.getAccessToken().substring(0, 20) : null, System.currentTimeMillis(), pathPrefixAuth.getExpiration());
                    TokenResponse tokenResponse = getAccessToken(pathPrefixAuth.getTokenUrl(), pathPrefixAuth.getClientId(), pathPrefixAuth.getScope());
                    if(tokenResponse != null) {
                        pathPrefixAuth.setExpiration(System.currentTimeMillis() + tokenResponse.getExpiresIn() * 1000 - 60000);
                        pathPrefixAuth.setAccessToken(tokenResponse.getAccessToken());
                        if(logger.isTraceEnabled()) logger.trace("Got a new token {} and cached it with expiration time {}", pathPrefixAuth.getAccessToken() != null ? pathPrefixAuth.getAccessToken().substring(0, 20) : null, pathPrefixAuth.getExpiration());
                    } else {
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

    private TokenResponse getAccessToken(String serverUrl, String clientId, String scope) {
        String certFileName = config.getCertFilename(); // PKCS12 format
        String certPassword = config.getCertPassword();
        TokenResponse tokenResponse = null;
        if(client == null) {
            try {
                //  create keystore
                KeyStore keyStore = KeyStore.getInstance("PKCS12");
                keyStore.load(Config.getInstance().getInputStreamFromFile(certFileName), certPassword.toCharArray());
                KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
                kmf.init(keyStore, certPassword.toCharArray());
                SSLContext sslContext = SSLContext.getInstance("TLS");
                sslContext.init(kmf.getKeyManagers(), null, null);

                HttpClient.Builder clientBuilder = HttpClient.newBuilder()
                        .followRedirects(HttpClient.Redirect.NORMAL)
                        .connectTimeout(Duration.ofMillis(ClientConfig.get().getTimeout()))
                        .sslContext(sslContext);
                if(config.getProxyHost() != null) {
                    if(logger.isTraceEnabled()) logger.trace("use proxy " + config.getProxyHost() + ":" + config.getProxyPort());
                    clientBuilder.proxy(ProxySelector.of(new InetSocketAddress(config.getProxyHost(), config.getProxyPort() == 0 ? 443 : config.getProxyPort())));
                }
                if(config.isEnableHttp2()) {
                    if(logger.isTraceEnabled()) logger.trace("enable http2 is true");
                    clientBuilder.version(HttpClient.Version.HTTP_2);
                }
                // this a workaround to bypass the hostname verification in jdk11 http client.
                Map<String, Object> tlsMap = (Map<String, Object>)ClientConfig.get().getMappedConfig().get(Http2Client.TLS);
                if(tlsMap != null && !Boolean.TRUE.equals(tlsMap.get(TLSConfig.VERIFY_HOSTNAME))) {
                    if(logger.isTraceEnabled()) logger.trace("disable hostname verification");
                    final Properties props = System.getProperties();
                    props.setProperty("jdk.internal.httpclient.disableHostnameVerification", Boolean.TRUE.toString());
                }
                client = clientBuilder.build();

            } catch (Exception e) {
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
            parameters.put("grant_type", "client_credentials");
            parameters.put("client_id", clientId);
            parameters.put("scope", scope);

            String form = parameters.entrySet()
                    .stream()
                    .map(e -> e.getKey() + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                    .collect(Collectors.joining("&"));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(serverUrl))
                    .headers("Content-Type", "application/x-www-form-urlencoded", "accept", "application/json", "Host", extractHost(serverUrl))
                    .POST(HttpRequest.BodyPublishers.ofString(form))
                    .build();

            HttpResponse<?> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if(response.statusCode() == 200) {
                // construct a token response and return it.
                if(logger.isTraceEnabled()) logger.trace("response body " + response.body().toString());
                Map<String, Object> map = JsonMapper.string2Map(response.body().toString());
                if(map != null) {
                    tokenResponse = new TokenResponse();
                    tokenResponse.setAccessToken((String)map.get("access_token"));
                    tokenResponse.setTokenType((String)map.get("token_type"));
                    tokenResponse.setExpiresIn((Integer)map.get("expires_in"));
                    if(logger.isTraceEnabled()) logger.trace("tokenResponse = " + tokenResponse.toString());
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

    public String extractHost(String urlString) {
        try {
            URL url = new URL(urlString);
            return url.getHost();
        } catch (Exception e) {
            logger.error("Exception:", e);
        }
        return null;
    }
}
