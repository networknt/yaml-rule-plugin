package com.networknt.rule.generic.token;

import com.networknt.client.ClientConfig;
import com.networknt.http.client.HttpClientRequest;
import com.networknt.http.client.ssl.TLSConfig;
import com.networknt.rule.RuleActionValue;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.*;

public class TokenActionTest {

    HttpClient client;
    @Before
    public void setUp() throws Exception {
        var clientBuilder = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .connectTimeout(Duration.ofMillis(ClientConfig.get().getTimeout()))
                .sslContext(HttpClientRequest.createSSLContext());

        // this a workaround to bypass the hostname verification in jdk11 http client.
        var tlsMap = (Map<String, Object>) ClientConfig.get().getMappedConfig().get(ClientConfig.TLS);

        if (tlsMap != null && !Boolean.TRUE.equals(tlsMap.get(TLSConfig.VERIFY_HOSTNAME))) {
            final Properties props = System.getProperties();
            props.setProperty("jdk.internal.httpclient.disableHostnameVerification", Boolean.TRUE.toString());
        }

        client = clientBuilder.build();
    }

    @Test
    @Ignore
    public void lifewareTokenTest() {
        final var actionValues = new ArrayList<RuleActionValue>();

        addNewRuleAction(actionValues, "requestHeaders", "Content-Type,application/json,Accept,application/json");
        addNewRuleAction(actionValues, "requestBodyEntries", "client_id,${config.clientId},client_secret,${config.clientSecret}");
        addNewRuleAction(actionValues, "sourceBodyEntries", "access_token,${response.accessToken}");
        addNewRuleAction(actionValues, "tokenDirection", "REQUEST");
        addNewRuleAction(actionValues, "destinationHeaders", "Authorization,Bearer ${response.accessToken}");

        final var config = new TokenTransformerConfig("lifeware_token_test");
        final var action = new TokenAction(actionValues, config.getPathPrefixAuths().get(0));

        Assert.assertEquals(4, action.requestBodyEntries.length);
        Assert.assertEquals(4, action.requestHeaders.length);

        Map<String, Object> resultMap = new HashMap<>();
        action.buildRequest();
        action.requestToken(this.client, resultMap);

        /* token response should have been 200 and resultMap should contain the new token. */
        Assert.assertTrue(resultMap.containsKey("requestHeaders"));

    }

    @Test
    @Ignore
    public void tealiumTokenTest() {
        final var actionValues = new ArrayList<RuleActionValue>();

        addNewRuleAction(actionValues, "requestHeaders", "Content-Type,application/x-www-form-urlencoded");
        addNewRuleAction(actionValues, "requestBodyEntries", "username,${config.username},key,${config.password}");
        addNewRuleAction(actionValues, "sourceBodyEntries", "token,${response.accessToken}");
        addNewRuleAction(actionValues, "tokenDirection", "REQUEST");
        addNewRuleAction(actionValues, "destinationHeaders", "Authorization,Bearer ${response.accessToken}");

        final var config = new TokenTransformerConfig("tealium_token_test");
        final var action = new TokenAction(actionValues, config.getPathPrefixAuths().get(0));

        Map<String, Object> resultMap = new HashMap<>();
        action.buildRequest();
        action.requestToken(this.client, resultMap);

        /* token response should have been 200 and resultMap should contain the new token. */
        Assert.assertTrue(resultMap.containsKey("requestHeaders"));
    }

    @Test
    @Ignore
    public void snowTokenTest() {
        final var actionValues = new ArrayList<RuleActionValue>();

        addNewRuleAction(actionValues, "requestHeaders", "Content-Type,application/x-www-form-urlencoded");
        addNewRuleAction(actionValues, "requestBodyEntries", "username,${config.username},password,${config.password},client_id,${config.clientId},client_secret,${config.clientSecret},grant_type,${config.grantType}");
        addNewRuleAction(actionValues, "sourceBodyEntries", "access_token,${response.accessToken},token_type,expires_in,${response.expiration}");
        addNewRuleAction(actionValues, "tokenDirection", "REQUEST");
        addNewRuleAction(actionValues, "destinationHeaders", "Authorization,Bearer ${response.accessToken}");

        final var config = new TokenTransformerConfig("snow_token_test");
        final var action = new TokenAction(actionValues, config.getPathPrefixAuths().get(0));

        Map<String, Object> resultMap = new HashMap<>();
        action.buildRequest();
        action.requestToken(this.client, resultMap);

        /* token response should have been 200 and resultMap should contain the new token. */
        Assert.assertTrue(resultMap.containsKey("requestHeaders"));
    }

    @Test
    @Ignore
    public void jwtCacheTest() {
        final var actionValues = new ArrayList<RuleActionValue>();

        addNewRuleAction(actionValues, "requestHeaders", "Content-Type,application/json,Accept,application/json");
        addNewRuleAction(actionValues, "requestBodyEntries", "client_id,${config.clientId},client_secret,${config.clientSecret}");
        addNewRuleAction(actionValues, "sourceBodyEntries", "access_token,${response.accessToken}");
        addNewRuleAction(actionValues, "tokenDirection", "REQUEST");
        addNewRuleAction(actionValues, "destinationHeaders", "Authorization,Bearer ${response.accessToken}");

        final var config = new TokenTransformerConfig("jwtCache_token_test");
        final var action = new TokenAction(actionValues, config.getPathPrefixAuths().get(0));

        Assert.assertEquals(4, action.requestBodyEntries.length);
        Assert.assertEquals(4, action.requestHeaders.length);

        Map<String, Object> resultMap = new HashMap<>();
        action.useCachedToken(resultMap);

        /* token response should have been 200 and resultMap should contain the new token. */
        Assert.assertTrue(resultMap.containsKey("requestHeaders"));
    }

    private static void addNewRuleAction(final List<RuleActionValue> actionValues, final String key, final String value) {
        final var newRuleAction = new RuleActionValue();
        newRuleAction.setActionValueId(key);
        newRuleAction.setValue(value);
        actionValues.add(newRuleAction);
    }


}
