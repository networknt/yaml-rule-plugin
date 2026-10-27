package com.networknt.rule.generic.token;

import org.junit.jupiter.api.Test;

import java.net.http.HttpRequest;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HttpTokenRequestBuilderJacksonTest {
    @Test
    void jsonBodyRetainsEscapingAndPostMethod() throws Exception {
        HttpRequest request = new HttpTokenRequestBuilder("https://example.invalid/token")
                .withBody(Map.of("scope", "synthetic \"scope\" & only"), "application/json").build();
        assertEquals("POST", request.method());
        assertEquals("{\"scope\":\"synthetic \\\"scope\\\" & only\"}", body(request));
    }

    @Test
    void formBodyRetainsUrlEncoding() throws Exception {
        HttpRequest request = new HttpTokenRequestBuilder("https://example.invalid/token")
                .withBody(Map.of("scope", "synthetic & only"), "application/x-www-form-urlencoded").build();
        assertEquals("scope=synthetic+%26+only", body(request));
    }

    private String body(HttpRequest request) throws Exception {
        CompletableFuture<String> result = new CompletableFuture<>();
        request.bodyPublisher().orElseThrow().subscribe(new Flow.Subscriber<ByteBuffer>() {
            private final StringBuilder text = new StringBuilder();

            public void onSubscribe(Flow.Subscription subscription) {
                subscription.request(Long.MAX_VALUE);
            }

            public void onNext(ByteBuffer buffer) {
                text.append(StandardCharsets.UTF_8.decode(buffer));
            }

            public void onError(Throwable error) {
                result.completeExceptionally(error);
            }

            public void onComplete() {
                result.complete(text.toString());
            }
        });
        return result.get(5, TimeUnit.SECONDS);
    }
}
