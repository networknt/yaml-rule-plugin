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

        final var requestHeadersKey = "requestHeaders";
        final var requestHeadersValue = "Content-Type,application/json,Accept,application/json";
        final var requestHeaders = new RuleActionValue();
        requestHeaders.setActionValueId(requestHeadersKey);
        requestHeaders.setValue(requestHeadersValue);
        actionValues.add(requestHeaders);

        final var requestBodyEntriesKey = "requestBodyEntries";
        final var requestBodyEntriesValue = "client_id,${config.clientId},client_secret,${config.clientSecret}";
        final var requestBodyEntries = new RuleActionValue();
        requestBodyEntries.setActionValueId(requestBodyEntriesKey);
        requestBodyEntries.setValue(requestBodyEntriesValue);
        actionValues.add(requestBodyEntries);

        final var sourceBodyEntriesKey = "sourceBodyEntries";
        final var sourceBodyEntriesValue = "access_token,${response.accessToken}";
        final var sourceBodyEntries = new RuleActionValue();
        sourceBodyEntries.setActionValueId(sourceBodyEntriesKey);
        sourceBodyEntries.setValue(sourceBodyEntriesValue);
        actionValues.add(sourceBodyEntries);

        final var tokenDirectionKey = "tokenDirection";
        final var tokenDirectionValue = "REQUEST";
        final var tokenDirection = new RuleActionValue();
        tokenDirection.setActionValueId(tokenDirectionKey);
        tokenDirection.setValue(tokenDirectionValue);
        actionValues.add(tokenDirection);

        final var destinationHeadersKey = "destinationHeaders";
        final var destinationHeadersValue = "Authorization,Bearer ${response.accessToken}";
        final var destinationHeaders = new RuleActionValue();
        destinationHeaders.setActionValueId(destinationHeadersKey);
        destinationHeaders.setValue(destinationHeadersValue);
        actionValues.add(destinationHeaders);

        final var config = new TokenTransformerConfig("lifeware_token_test");

        final var action = new TokenAction(actionValues, config.getPathPrefixAuths().get(0));
        Assert.assertEquals(4, action.requestBodyEntries.length);
        Assert.assertEquals(4, action.requestHeaders.length);

        Map<String, Object> resultMap = new HashMap<>();
        action.requestToken(this.client, resultMap);

        /* token response should have been 200 and resultMap should contain the new token. */
        Assert.assertTrue(resultMap.containsKey("requestHeaders"));

    }

    @Test
    @Ignore
    public void tealiumTokenTest() {
        final var actionValues = new ArrayList<RuleActionValue>();

        final var tokenRequestHeaderFieldsKey = "requestHeaders";
        final var tokenRequestHeaderFieldsValue = "Content-Type,application/x-www-form-urlencoded";
        final var tokenRequestHeaderFieldsActionValue = new RuleActionValue();
        tokenRequestHeaderFieldsActionValue.setActionValueId(tokenRequestHeaderFieldsKey);
        tokenRequestHeaderFieldsActionValue.setValue(tokenRequestHeaderFieldsValue);
        actionValues.add(tokenRequestHeaderFieldsActionValue);

        final var requestBodyEntriesKey = "requestBodyEntries";
        final var requestBodyEntriesValue = "username,${config.username},key,${config.password}";
        final var requestBodyEntries = new RuleActionValue();
        requestBodyEntries.setActionValueId(requestBodyEntriesKey);
        requestBodyEntries.setValue(requestBodyEntriesValue);
        actionValues.add(requestBodyEntries);

        final var tokenDestinationSourceFieldKey = "sourceBodyEntries";
        final var tokenDestinationSourceValueKey = "token,${response.accessToken}";
        final var tokenDestinationSourceValueActionValue = new RuleActionValue();
        tokenDestinationSourceValueActionValue.setActionValueId(tokenDestinationSourceFieldKey);
        tokenDestinationSourceValueActionValue.setValue(tokenDestinationSourceValueKey);
        actionValues.add(tokenDestinationSourceValueActionValue);

        final var tokenDirectionKey = "tokenDirection";
        final var tokenDirectionValue = "REQUEST";
        final var tokenDirection = new RuleActionValue();
        tokenDirection.setActionValueId(tokenDirectionKey);
        tokenDirection.setValue(tokenDirectionValue);
        actionValues.add(tokenDirection);

        final var destinationHeadersKey = "destinationHeaders";
        final var destinationHeadersValue = "Authorization,Bearer ${response.accessToken}";
        final var destinationHeaders = new RuleActionValue();
        destinationHeaders.setActionValueId(destinationHeadersKey);
        destinationHeaders.setValue(destinationHeadersValue);
        actionValues.add(destinationHeaders);

        final var config = new TokenTransformerConfig("tealium_token_test");

        final var action = new TokenAction(actionValues, config.getPathPrefixAuths().get(0));
        Map<String, Object> resultMap = new HashMap<>();
        action.requestToken(this.client, resultMap);

        /* token response should have been 200 and resultMap should contain the new token. */
        Assert.assertTrue(resultMap.containsKey("requestHeaders"));
    }

    @Test
    @Ignore
    public void snowTokenTest() {
        final var actionValues = new ArrayList<RuleActionValue>();

        final var tokenRequestHeaderFieldsKey = "requestHeaders";
        final var tokenRequestHeaderFieldsValue = "Content-Type,application/x-www-form-urlencoded";
        final var tokenRequestHeaderFieldsActionValue = new RuleActionValue();
        tokenRequestHeaderFieldsActionValue.setActionValueId(tokenRequestHeaderFieldsKey);
        tokenRequestHeaderFieldsActionValue.setValue(tokenRequestHeaderFieldsValue);
        actionValues.add(tokenRequestHeaderFieldsActionValue);

        final var requestBodyEntriesKey = "requestBodyEntries";
        final var requestBodyEntriesValue = "username,${config.username},password,${config.password},client_id,${config.clientId},client_secret,${config.clientSecret},grant_type,${config.grantType}";
        final var requestBodyEntries = new RuleActionValue();
        requestBodyEntries.setActionValueId(requestBodyEntriesKey);
        requestBodyEntries.setValue(requestBodyEntriesValue);
        actionValues.add(requestBodyEntries);

        final var tokenDestinationSourceFieldKey = "sourceBodyEntries";
        final var tokenDestinationSourceValueKey = "access_token,${response.accessToken},token_type,expires_in,${response.expiration}";
        final var tokenDestinationSourceValueActionValue = new RuleActionValue();
        tokenDestinationSourceValueActionValue.setActionValueId(tokenDestinationSourceFieldKey);
        tokenDestinationSourceValueActionValue.setValue(tokenDestinationSourceValueKey);
        actionValues.add(tokenDestinationSourceValueActionValue);

        final var tokenDirectionKey = "tokenDirection";
        final var tokenDirectionValue = "REQUEST";
        final var tokenDirection = new RuleActionValue();
        tokenDirection.setActionValueId(tokenDirectionKey);
        tokenDirection.setValue(tokenDirectionValue);
        actionValues.add(tokenDirection);

        final var destinationHeadersKey = "destinationHeaders";
        final var destinationHeadersValue = "Authorization,Bearer ${response.accessToken}";
        final var destinationHeaders = new RuleActionValue();
        destinationHeaders.setActionValueId(destinationHeadersKey);
        destinationHeaders.setValue(destinationHeadersValue);
        actionValues.add(destinationHeaders);

        final var config = new TokenTransformerConfig("snow_token_test");

        final var action = new TokenAction(actionValues, config.getPathPrefixAuths().get(0));
        Map<String, Object> resultMap = new HashMap<>();
        action.requestToken(this.client, resultMap);

        /* token response should have been 200 and resultMap should contain the new token. */
        Assert.assertTrue(resultMap.containsKey("requestHeaders"));
    }


}
