package com.networknt.rule.generic.token;

import com.networknt.rule.RuleActionValue;
import com.networknt.rule.generic.token.schema.SharedVariableSchema;
import com.networknt.rule.generic.token.schema.SourceSchema;
import com.networknt.rule.generic.token.schema.UpdateSchema;
import com.networknt.utility.Constants;
import org.junit.Assert;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;

public class TokenTransformerActionTest {

    @Test
    public void tokenParseTest() {
        /* blank path prefix */
        var testSharedVariable = new SharedVariableSchema();

        /* configure source to grab data from response body */
        var testSource = new SourceSchema();
        var testSourceAndDestinationDefinition = new SourceSchema.SourceDestinationDefinition();
        testSourceAndDestinationDefinition.setSource("access_token");
        testSourceAndDestinationDefinition.setDestination("!ref(sharedVariables.accessToken)");

        String jsonString = "{\"access_token\": \"abc123\"}";
        testSource.setHeaders(Collections.singletonList(testSourceAndDestinationDefinition));
        testSource.writeJsonStringToSharedVariables(testSharedVariable, jsonString, testSource.getHeaders());

        Assert.assertEquals("abc123", testSharedVariable.getAccessToken());
    }

    @Test
    public void updateHeaderTest() {
        /* test access token */
        var testSharedVariable = new SharedVariableSchema();
        testSharedVariable.setAccessToken("abc123");

        /* create test update schema */
        var testUpdateSchema = new UpdateSchema();
        Map<String, String> myUpdateHeaders = new HashMap<>();
        myUpdateHeaders.put("Authorization", "Bearer !ref(sharedVariables.accessToken)");
        testUpdateSchema.setHeaders(myUpdateHeaders);

        /* testing update */
        Assert.assertEquals("Bearer abc123", testUpdateSchema.getResolvedHeaders(testSharedVariable).get("Authorization"));

    }

    @Test
    public void expirationTest() {
        final var action = new TokenTransformerAction();
        final var actionValues = new ArrayList<RuleActionValue>();
        final var tokenSchemaKey = "tokenSchemas";
        final var tokenSchemaValue = "expirationTest";
        final var tokenSchemaActionValue = new RuleActionValue();
        tokenSchemaActionValue.setActionValueId(tokenSchemaKey);
        tokenSchemaActionValue.setValue(tokenSchemaValue);
        actionValues.add(tokenSchemaActionValue);

        final var resultMap = new HashMap<String, Object>();
        action.performAction(new HashMap<>(), resultMap, actionValues);

        final var requestHeaderMap = (Map<String, Object>)resultMap.get("requestHeaders");
        Assert.assertNotNull(requestHeaderMap);

        final var updateMap = (Map<String, Object>) requestHeaderMap.get("update");
        Assert.assertNotNull(updateMap);

        final var authHeader = updateMap.get("Authorization");
        Assert.assertNotNull(authHeader);
        Assert.assertEquals("Bearer abc-123", authHeader);
    }

    @Test
    public void gracePeriodTest() {
        final var action = new TokenTransformerAction();
        final var actionValues = new ArrayList<RuleActionValue>();
        final var tokenSchemaKey = "tokenSchemas";
        final var tokenSchemaValue = "gracePeriodTest";
        final var tokenSchemaActionValue = new RuleActionValue();
        tokenSchemaActionValue.setActionValueId(tokenSchemaKey);
        tokenSchemaActionValue.setValue(tokenSchemaValue);
        actionValues.add(tokenSchemaActionValue);

        final var resultMap = new HashMap<String, Object>();

        action.performAction(new HashMap<>(), resultMap, actionValues);
        Assert.assertEquals("Request failed trying to send a request to: https://fake.token.com/services/oauth2/token", resultMap.get(Constants.ERROR_MESSAGE));
    }

    @Test
    public void multiThreadTest() throws BrokenBarrierException, InterruptedException {
        final var action = new TokenTransformerAction();
        final var actionValues = new ArrayList<RuleActionValue>();
        final var tokenSchemaKey = "tokenSchemas";
        final var tokenSchemaValue = "multiThreadTest";
        final var tokenSchemaActionValue = new RuleActionValue();
        tokenSchemaActionValue.setActionValueId(tokenSchemaKey);
        tokenSchemaActionValue.setValue(tokenSchemaValue);
        actionValues.add(tokenSchemaActionValue);

        /* create barrier with 3 awaits. */
        final var gate = new CyclicBarrier(3);
        final var resultMap1 = new HashMap<String, Object>();
        final var t1 = new Thread(() -> {
            try {
                gate.await();
            } catch (InterruptedException | BrokenBarrierException e) {
                throw new RuntimeException(e);
            }
            action.performAction(new HashMap<>(), resultMap1, actionValues);
        });
        final var resultMap2 = new HashMap<String, Object>();
        final var t2 = new Thread(() -> {
            try {
                gate.await();
            } catch (InterruptedException | BrokenBarrierException e) {
                throw new RuntimeException(e);
            }

            action.performAction(new HashMap<>(), resultMap2, actionValues);
        });

        /* prep-threads */
        t1.start();
        t2.start();

        /* start at the same time */
        gate.await();
        t1.join();
        t2.join();

        final var requestHeaderMap1 = (Map<String, Object>)resultMap1.get("requestHeaders");
        Assert.assertNotNull(requestHeaderMap1);

        final var updateMap1 = (Map<String, Object>) requestHeaderMap1.get("update");
        Assert.assertNotNull(updateMap1);

        final var authHeader1 = updateMap1.get("Authorization");
        Assert.assertNotNull(authHeader1);
        Assert.assertEquals("Bearer abc-123", authHeader1);

        final var requestHeaderMap2 = (Map<String, Object>)resultMap1.get("requestHeaders");
        Assert.assertNotNull(requestHeaderMap2);

        final var updateMap2 = (Map<String, Object>) requestHeaderMap1.get("update");
        Assert.assertNotNull(updateMap2);

        final var authHeader2 = updateMap1.get("Authorization");
        Assert.assertNotNull(authHeader2);
        Assert.assertEquals("Bearer abc-123", authHeader2);
    }

    @Test
    public void ttlUnitTest() {
        final var testConfig = TokenTransformerConfig.load();
        final var schema = testConfig.getTokenSchemas().get("aliasConfigTest");
        final var sharedTtlUnit = schema.getSharedVariables().getTokenTtlUnit();

        Assert.assertEquals(3, sharedTtlUnit.millisToUnit(3000));

        final var expirationSchemaTtlUnit = schema.getTokenSource().getExpirationSchema().getTtlUnit();

        Assert.assertEquals(1, expirationSchemaTtlUnit.millisToUnit(60000));
    }

    @Test
    public void resolvedVariableCachingTest() {
        /* test access token */
        var testSharedVariable = new SharedVariableSchema();
        testSharedVariable.setAccessToken("abc123");

        /* create test update schema */
        var testUpdateSchema = new UpdateSchema();
        Map<String, String> myUpdateHeaders = new HashMap<>();
        myUpdateHeaders.put("Authorization", "Bearer !ref(sharedVariables.accessToken)");
        testUpdateSchema.setHeaders(myUpdateHeaders);

        /* testing update */
        Assert.assertEquals("Bearer abc123", testUpdateSchema.getResolvedHeaders(testSharedVariable).get("Authorization"));

        /* testing second update */
        testSharedVariable.setAccessToken("abc124");
        Assert.assertEquals("Bearer abc124", testUpdateSchema.getResolvedHeaders(testSharedVariable).get("Authorization"));
    }
}
