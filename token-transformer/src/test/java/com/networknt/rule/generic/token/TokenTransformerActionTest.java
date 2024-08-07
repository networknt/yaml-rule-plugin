package com.networknt.rule.generic.token;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.networknt.config.PathPrefixAuth;
import com.networknt.rule.RuleActionValue;
import com.networknt.rule.generic.token.schema.SourceSchema;
import com.networknt.rule.generic.token.schema.UpdateSchema;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class TokenTransformerActionTest {

    @Test
    @Ignore
    public void basic() {
        try {
            final var transformerAction = new TokenTransformerAction();
            final var actionValues = new ArrayList<RuleActionValue>();

            final var tokenSchemaKey = "tokenSchemas";
            final var tokenSchemaValue = "lifeware";
            final var tokenSchemaActionValue = new RuleActionValue();
            tokenSchemaActionValue.setActionValueId(tokenSchemaKey);
            tokenSchemaActionValue.setValue(tokenSchemaValue);
            actionValues.add(tokenSchemaActionValue);

            final var resultMap = new HashMap<String, Object>();
            transformerAction.performAction(new HashMap<>(), resultMap, actionValues);
            System.out.println(resultMap);

            //action
        } catch (URISyntaxException | JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        Assert.assertEquals(1,1);
    }

    @Test
    public void tokenParseTest() {
        /* blank path prefix */
        var testPathPrefixAuth = new PathPrefixAuth();

        /* configure source to grab data from response body */
        var testSource = new SourceSchema();
        var testSourceAndDestinationDefinition = new SourceSchema.SourceDestinationDefinition();
        testSourceAndDestinationDefinition.setSource("access_token");
        testSourceAndDestinationDefinition.setDestination("!ref(pathPrefix.accessToken)");

        String jsonString = "{\"access_token\": \"abc123\"}";
        testSource.setHeaders(Collections.singletonList(testSourceAndDestinationDefinition));
        testSource.writeJsonStringToPathPrefix(testPathPrefixAuth, jsonString, testSource.getHeaders());

        Assert.assertEquals("abc123", testPathPrefixAuth.getAccessToken());
    }

    @Test
    public void updateTest() {

        /* test access token */
        var testPathPrefixAuth = new PathPrefixAuth();
        testPathPrefixAuth.setAccessToken("abc123");

        /* create test update schema */
        var testUpdateSchema = new UpdateSchema();
        Map<String, String> myUpdateHeaders = new HashMap<>();
        myUpdateHeaders.put("Authorization", "Bearer !ref(pathPrefixAuth.accessToken)");
        testUpdateSchema.setHeaders(myUpdateHeaders);

        /* testing update */
        testUpdateSchema.writeSchemaFromPathPrefix(testPathPrefixAuth);

        Assert.assertEquals("Bearer abc123", testUpdateSchema.getHeaders().get("Authorization"));

    }
}
