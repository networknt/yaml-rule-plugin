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

        final var tokenRequestHeaderFieldsKey = "requestHeaders";
        final var tokenRequestHeaderFieldsValue = "Content-Type,application/json,Accept,application/json";
        final var tokenRequestHeaderFieldsActionValue = new RuleActionValue();
        tokenRequestHeaderFieldsActionValue.setActionValueId(tokenRequestHeaderFieldsKey);
        tokenRequestHeaderFieldsActionValue.setValue(tokenRequestHeaderFieldsValue);
        actionValues.add(tokenRequestHeaderFieldsActionValue);

        final var tokenRequestBodyValuesKey = "requestBodyEntries";
        final var tokenRequestBodyValuesValue = "client_id,${config.clientId},client_secret,${config.clientSecret}";
        final var tokenRequestBodyValuesActionValue = new RuleActionValue();
        tokenRequestBodyValuesActionValue.setActionValueId(tokenRequestBodyValuesKey);
        tokenRequestBodyValuesActionValue.setValue(tokenRequestBodyValuesValue);
        actionValues.add(tokenRequestBodyValuesActionValue);

        final var tokenDestinationSourceKey = "tokenSource";
        final var tokenDestinationSourceValue = "BODY";
        final var tokenDestinationSourceKeyActionValue = new RuleActionValue();
        tokenDestinationSourceKeyActionValue.setActionValueId(tokenDestinationSourceKey);
        tokenDestinationSourceKeyActionValue.setValue(tokenDestinationSourceValue);
        actionValues.add(tokenDestinationSourceKeyActionValue);

        final var tokenDestinationSourceFieldKey = "tokenSourceField";
        final var tokenDestinationSourceValueKey = "access_token";
        final var tokenDestinationSourceValueActionValue = new RuleActionValue();
        tokenDestinationSourceValueActionValue.setActionValueId(tokenDestinationSourceFieldKey);
        tokenDestinationSourceValueActionValue.setValue(tokenDestinationSourceValueKey);
        actionValues.add(tokenDestinationSourceValueActionValue);

        final var tokenDestinationTypeKey = "tokenDestination";
        final var tokenDestinationTypeValue = "HEADER";
        final var tokenDestinationTypeActionValue = new RuleActionValue();
        tokenDestinationTypeActionValue.setActionValueId(tokenDestinationTypeKey);
        tokenDestinationTypeActionValue.setValue(tokenDestinationTypeValue);
        actionValues.add(tokenDestinationTypeActionValue);

        final var tokenDestinationFieldKey = "tokenDestinationField";
        final var tokenDestinationFieldValue = "Authorization";
        final var tokenDestinationFieldActionValue = new RuleActionValue();
        tokenDestinationFieldActionValue.setActionValueId(tokenDestinationFieldKey);
        tokenDestinationFieldActionValue.setValue(tokenDestinationFieldValue);
        actionValues.add(tokenDestinationFieldActionValue);

        final var tokenDestinationValueKey = "tokenDestinationValue";
        final var tokenDestinationValueValue = "Bearer ${response.accessToken}";
        final var tokenDestinationValueActionValue = new RuleActionValue();
        tokenDestinationValueActionValue.setActionValueId(tokenDestinationValueKey);
        tokenDestinationValueActionValue.setValue(tokenDestinationValueValue);
        actionValues.add(tokenDestinationValueActionValue);

        final var config = new TokenTransformerConfig("lifeware_token_test");

        final var action = new TokenAction(actionValues, config.getPathPrefixAuths().get(0));
        Assert.assertEquals(4, action.bodyEntries.length);
        Assert.assertEquals(4, action.headers.length);

        Map<String, Object> resultMap = new HashMap<>();
        action.requestToken(this.client, resultMap);

        /* token response should have been 200 and resultMap should contain the new token. */
        Assert.assertTrue(resultMap.containsKey("requestHeaders"));

    }
}
