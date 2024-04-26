package com.networknt.rule.epam;

import com.networknt.client.ClientConfig;
import com.networknt.client.oauth.TokenResponse;
import com.networknt.config.Config;
import com.networknt.config.JsonMapper;
import com.networknt.proxy.PathPrefixAuth;
import com.networknt.http.client.ssl.TLSConfig;
import com.networknt.rule.IAction;
import com.networknt.rule.RuleActionValue;
import com.networknt.rule.RuleConstants;
import com.networknt.status.Status;
import com.networknt.utility.ModuleRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyStore;
import java.time.Duration;
import java.util.*;

/**
 * It is called from the request transform interceptor from the light-gateway to get the e-PAM API access token with
 * the passed in grant_type, client_id and scope. The plugin will create a brand new SSLContext with keystore to
 * enable authentication with PING OAuth server.
 *
 * @author Steve Hu
 */
public class EpamTokenRequestTransformAction implements IAction {
    public static final String GET_TOKEN_ERROR = "ERR10052";
    private static final Logger logger = LoggerFactory.getLogger(EpamTokenRequestTransformAction.class);
    // change the config to static so that it can cache the token retrieved until expiration time.
    private static final EpamConfig config = EpamConfig.load();
    private static HttpClient client;

    public EpamTokenRequestTransformAction() {
        if(logger.isInfoEnabled()) logger.info("EpamTokenRequestTransformAction is constructed");
        List<String> masks = new ArrayList<>();
        masks.add("certPassword");
        ModuleRegistry.registerPlugin(
                EpamTokenRequestTransformAction.class.getPackage().getImplementationTitle(),
                EpamTokenRequestTransformAction.class.getPackage().getImplementationVersion(),
                EpamConfig.CONFIG_NAME,
                EpamTokenRequestTransformAction.class.getName(),
                Config.getNoneDecryptedInstance().getJsonMapConfigNoCache(EpamConfig.CONFIG_NAME),
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
                    String requestBody = (String)objMap.get("requestBody");
                    if(logger.isTraceEnabled()) logger.trace("requestBody = " + requestBody);
                    TokenResponse tokenResponse = getAccessToken(pathPrefixAuth.getTokenUrl(), requestBody);
                    if(tokenResponse != null) {
                        pathPrefixAuth.setExpiration(System.currentTimeMillis() + tokenResponse.getExpiresIn() * 1000 - 60000);
                        pathPrefixAuth.setAccessToken(tokenResponse.getAccessToken());
                        if(logger.isTraceEnabled()) logger.trace("Got a new token {} and cached it with expiration time {}", pathPrefixAuth.getAccessToken() != null ? pathPrefixAuth.getAccessToken().substring(0, 20) : null, pathPrefixAuth.getExpiration());
                    } else {
                        if(logger.isTraceEnabled()) logger.trace("Failed to get a new token");
                        // need to return an error message to the caller.
                        resultMap.put("contentType", "application/json");
                        Status status = new Status(GET_TOKEN_ERROR);
                        resultMap.put("statusCode", status.getStatusCode());
                        resultMap.put("responseBody", status.toString());
                        return;
                    }
                }
                // either a new token is retrieved or cached token is not expired. Put the token into the Authorization header.
                if(pathPrefixAuth.getAccessToken() != null) {
                    resultMap.put("contentType", "application/json");
                    resultMap.put("statusCode", 200);
                    // construct the response body
                    Map<String, Object> responseBody = new HashMap<>();
                    responseBody.put("access_token", pathPrefixAuth.getAccessToken());
                    responseBody.put("token_type", "Bearer");
                    responseBody.put("expires_in", (pathPrefixAuth.getExpiration() - System.currentTimeMillis()) / 1000);
                    resultMap.put("responseBody", JsonMapper.toJson(responseBody));
                    return;
                }
            }
        }
    }

    private TokenResponse getAccessToken(String serverUrl, String requestBody) {
        String certFileName = config.getCertFilename(); // PKCS12 format
        if(logger.isTraceEnabled()) logger.trace("certFileName = " + certFileName + " serverUrl = " + serverUrl);
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
                Map<String, Object> tlsMap = (Map<String, Object>)ClientConfig.get().getMappedConfig().get(ClientConfig.TLS);
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

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(serverUrl))
                    .headers("Content-Type", "application/x-www-form-urlencoded", "accept", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
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
}
