package com.networknt.rule.conquest;

import com.networknt.client.ClientConfig;
import com.networknt.client.oauth.TokenResponse;
import com.networknt.config.Config;
import com.networknt.config.JsonMapper;
import com.networknt.config.PathPrefixAuth;
import com.networknt.config.TlsUtil;
import com.networknt.http.client.HttpClientRequest;
import com.networknt.http.client.ssl.TLSConfig;
import com.networknt.rule.RequestTransformAction;
import com.networknt.rule.RuleActionValue;
import com.networknt.rule.RuleConstants;
import com.networknt.utility.HashUtil;
import com.networknt.utility.ModuleRegistry;
import com.networknt.utility.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URLEncoder;
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
public class ConquestTokenRequestTransformAction implements RequestTransformAction {
    private static final Logger logger = LoggerFactory.getLogger(ConquestTokenRequestTransformAction.class);
    // change the config to static so that it can cache the token retrieved until expiration time.
    private static final ConquestConfig config = ConquestConfig.load();
    private static HttpClient client;

    public ConquestTokenRequestTransformAction() {
        if(logger.isInfoEnabled()) logger.info("ConquestTokenRequestTransformAction is constructed");
        List<String> masks = new ArrayList<>();
        masks.add("certPassword");
        ModuleRegistry.registerPlugin(
                ConquestTokenRequestTransformAction.class.getPackage().getImplementationTitle(),
                ConquestTokenRequestTransformAction.class.getPackage().getImplementationVersion(),
                ConquestConfig.CONFIG_NAME,
                ConquestTokenRequestTransformAction.class.getName(),
                Config.getNoneDecryptedInstance().getJsonMapConfigNoCache(ConquestConfig.CONFIG_NAME), masks);
    }

    @Override
    public void performAction(Map<String, Object> objMap, Map<String, Object> resultMap, Collection<RuleActionValue> actionValues) {
        String requestPath = (String)objMap.get("requestPath");
        if(logger.isTraceEnabled()) logger.trace("requestPath = " + requestPath);
        for(PathPrefixAuth pathPrefixAuth: config.getPathPrefixAuths()) {
            if(requestPath.startsWith(pathPrefixAuth.getPathPrefix())) {
                if(logger.isTraceEnabled()) logger.trace("found with requestPath = " + requestPath + " prefix = " + pathPrefixAuth.getPathPrefix());
                if(System.currentTimeMillis() >= (pathPrefixAuth.getExpiration())) {
                    if(logger.isTraceEnabled()) logger.trace("Cached token {} is expired with current time {} and expired time {}", pathPrefixAuth.getAccessToken(), System.currentTimeMillis(), pathPrefixAuth.getExpiration());
                    // get a new access token.
                    String jwt = null;
                    try {
                        jwt = createJwt(pathPrefixAuth.getCertFilename(), pathPrefixAuth.getCertPassword(), pathPrefixAuth.getAuthIssuer(), pathPrefixAuth.getAuthSubject(), pathPrefixAuth.getAuthAudience(), HashUtil.generateUUID(), pathPrefixAuth.getTokenTtl());
                        if(logger.isTraceEnabled()) logger.trace("generated jwt = {}", jwt);
                    } catch (Exception e) {
                        logger.error("Exception", e);
                        return;
                    }
                    if(jwt != null) {
                        // use the jwt to get the access token.
                        TokenResponse tokenResponse = getAccessToken(pathPrefixAuth.getTokenUrl(), jwt);
                        if(tokenResponse != null) {
                            pathPrefixAuth.setExpiration(System.currentTimeMillis() + tokenResponse.getExpiresIn() * 1000 - 60000);
                            pathPrefixAuth.setAccessToken(tokenResponse.getAccessToken());
                            if(logger.isTraceEnabled()) logger.trace("Got a new token {} and cached it with expiration time {}", pathPrefixAuth.getAccessToken(), pathPrefixAuth.getExpiration());
                        } else {
                            return;
                        }
                    }
                }
                // either a new token is retrieved or cached token is not expired. Put the token into the Authorization header.
                if(pathPrefixAuth.getAccessToken() != null) {
                    RequestTransformAction.super.updateRequestHeader(resultMap,"Authorization", "Bearer " + pathPrefixAuth.getAccessToken());
                    return;
                }
            }
        }
    }

    private String createJwt(String certFilename, String certPassword, String issuer, String subject, String audience, String jti, int tokenTtl) throws Exception {
        if(logger.isTraceEnabled()) logger.trace("certFilename = " + certFilename + " certPassword = " + StringUtils.maskHalfString(certPassword) + " issuer = " + issuer + " subject = " + subject + " audience = " + audience + " jti = " + jti + " tokenTtl = " + tokenTtl);
        String header = "{\"typ\":\"JWT\", \"alg\":\"RS256\"}";
        String claimTemplate = "'{'\"iss\": \"{0}\", \"sub\": \"{1}\", \"aud\": \"{2}\", \"jti\": \"{3}\", \"iat\": {4}, \"exp\": {5}'}'";
        StringBuffer token = new StringBuffer();
        // Encode the JWT Header and add it to our string to sign
        token.append(org.apache.commons.codec.binary.Base64.encodeBase64URLSafeString(header.getBytes("UTF-8")));
        // Separate with a period
        token.append(".");

        String[] claimArray = new String[6];
        claimArray[0] = issuer;
        claimArray[1] = subject;
        claimArray[2] = audience;
        claimArray[3] = jti;
        claimArray[4] = Long.toString(( System.currentTimeMillis()/1000 ));
        claimArray[5] = Long.toString(( System.currentTimeMillis()/1000 ) + tokenTtl);

        MessageFormat claims;
        claims = new MessageFormat(claimTemplate);
        String payload = claims.format(claimArray);

        if(logger.isTraceEnabled()) logger.trace("jwtHeaderString = {} jwtBodyString = {}", header, payload);

        // Add the encoded claims object
        token.append(org.apache.commons.codec.binary.Base64.encodeBase64URLSafeString(payload.getBytes("UTF-8")));

        KeyStore keystore = TlsUtil.loadKeyStore(certFilename, certPassword.toCharArray());
        PrivateKey privateKey = (PrivateKey) keystore.getKey(certFilename.substring(0, certFilename.indexOf(".")), certPassword.toCharArray());

        if(logger.isTraceEnabled()) logger.trace("Created PrivateKey with name {} password {} alias {} keyPass {}",
                certFilename,
                certPassword,
                certFilename.substring(0, certFilename.indexOf(".")),
                certPassword
        );

        if(logger.isTraceEnabled()) logger.trace("JWT Algorithm = {}", "SHA256withRSA");

        // Sign the JWT Header + "." + JWT Claims Object
        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(privateKey);
        signature.update(token.toString().getBytes("UTF-8"));
        String signedPayload = org.apache.commons.codec.binary.Base64.encodeBase64URLSafeString(signature.sign());

        // Separate with a period
        token.append(".");

        // Add the encoded signature
        token.append(signedPayload);
        return token.toString();
    }

    private TokenResponse getAccessToken(String serverUrl, String jwt) {
        TokenResponse tokenResponse = null;
        if(client == null) {
            try {
                HttpClient.Builder clientBuilder = HttpClient.newBuilder()
                        .followRedirects(HttpClient.Redirect.NORMAL)
                        .connectTimeout(Duration.ofMillis(ClientConfig.get().getTimeout()))
                        .sslContext(HttpClientRequest.createSSLContext());
                if(config.getProxyHost() != null) clientBuilder.proxy(ProxySelector.of(new InetSocketAddress(config.getProxyHost(), config.getProxyPort() == 0 ? 443 : config.getProxyPort())));
                if(config.isEnableHttp2()) {
                    clientBuilder.version(HttpClient.Version.HTTP_2);
                } else {
                    clientBuilder.version(HttpClient.Version.HTTP_1_1);
                }
                // this a workaround to bypass the hostname verification in jdk11 http client.
                Map<String, Object> tlsMap = (Map<String, Object>)ClientConfig.get().getMappedConfig().get(ClientConfig.TLS);
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
            parameters.put("grant_type", "client_credentials");
            parameters.put("client_assertion_type", "urn:ietf:params:oauth:client-assertion-type:jwt-bearer");
            parameters.put("client_assertion", jwt);

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
            // {"access_token":"eyJ0eXAiOiJKV1QiLCJ6aXAiOiJOT05FIiwia2lkIjoiM0ptbzhUWFJtQTJ2U2hkcFJ6UHpUbC9Xak1zPSIsImFsZyI6IlJTMjU2In0.eyJzdWIiOiJjb25xdWVzdC1wdWJsaWMtdWF0LXN1bmxpZmUtand0LWludGVncmF0aW9uIiwiYXVkaXRUcmFja2luZ0lkIjoiM2UwNjc5ODYtMzRmYy00MzhjLThiYmEtOWJlODhiNGUzZTgxIiwiaXNzIjoiaHR0cHM6Ly9zdW5saWZlLWF1dGgudWF0LmNvbnF1ZXN0LXB1YmxpYy5jb25xdWVzdHBsYW5uaW5nLmNvbTo0NDMvbG9naW4vb2F1dGgyL3JlYWxtcy9yb290L3JlYWxtcy9jb24vcmVhbG1zL3VhdCIsInRva2VuTmFtZSI6ImFjY2Vzc190b2tlbiIsInR5cCI6IkJlYXJlciIsInRva2VuX3R5cGUiOiJCZWFyZXIiLCJhdXRoR3JhbnRJZCI6IjhmODVjOTFkLTQ1NzAtNDA5Ni1iYTdkLWI3Mzk2NDJiZGVhMiIsImF1ZCI6ImNvbnF1ZXN0LXB1YmxpYy11YXQtc3VubGlmZS1qd3QtaW50ZWdyYXRpb24iLCJuYmYiOjE2NjYyOTg1OTksInJlYWxtX2FjY2VzcyI6e30sInNjb3BlIjoiYXBpLmNvbnF1ZXN0cGxhbm5pbmcuY29tIiwiYXV0aF90aW1lIjoxNjY2Mjk4NTk5LCJyZWFsbSI6Ii9jb24vdWF0IiwiZXhwIjoxNjY2MzAwMzk5LCJpYXQiOjE2NjYyOTg1OTksImV4cGlyZXNfaW4iOjE4MDAwMDAsImp0aSI6IjU0ZjMzYzU2LTRhYjktNGI2OC1hYWU2LTAwZGJhZWJiNmVhOSJ9.Fvp2bs2h4pRo9Dcd_w7yMJGwY0Acq4h1fouYbo6b0WVVu8KTTC3Xxrl59kPT7f8Rsd-BjeORM83VypgAVWBvEhZWSOY_PpEIgPL0_EHBDOsOyd9x6Q_78WtVxpQ37Vag3nGT_EZA2b5ECWX1fg4C0qIJ4uUf4wyI6a91fui-95EgVBRsdsNa7TaX4AcsCX4T_96X-sqUY127YGyKV20S9ppKzwpg2kR1Xp43_HxtyBu5i-oSj8ry1EVZd5I0hTl2dzddyYUT8SfCiitS-BrAXC_1MM91td00kn3WlMjFahE5PcC6rg8yVFGpG0OQyIbElvCnfSeqNLjx3FPyVx3rqw","scope":"api.conquestplanning.com","token_type":"Bearer","expires_in":1799}
            if(response.statusCode() == 200) {
                // construct a token response and return it.
                Map<String, Object> map = JsonMapper.string2Map(response.body().toString());
                if(map != null) {
                    tokenResponse = new TokenResponse();
                    tokenResponse.setAccessToken((String)map.get("access_token"));
                    tokenResponse.setTokenType((String)map.get("token_type"));
                    tokenResponse.setScope((String)map.get("scope"));
                    tokenResponse.setExpiresIn((Integer)map.get("expires_in"));
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
