package com.networknt.rule.generic.token;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.networknt.client.oauth.TokenRequest;
import com.networknt.config.Config;
import com.networknt.config.JsonMapper;
import com.networknt.config.TlsUtil;
import com.networknt.rule.generic.token.schema.RequestSchema;
import com.networknt.rule.generic.token.schema.TokenSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class HttpTokenRequestBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(HttpTokenRequestBuilder.class);

    final HttpRequest.Builder httpRequestBuilder = HttpRequest.newBuilder();

    public HttpTokenRequestBuilder(String url) throws URISyntaxException {
        this.httpRequestBuilder.uri(new URI(url));
    }

    public HttpTokenRequestBuilder withHeaders(final Map<String, String> headers) {
        if (!headers.isEmpty()) {

            LOG.trace("Adding headers to token request.");

            for (final var header : headers.entrySet()) {
                LOG.trace("Header Key = {} Header Value = {}", header.getKey(), header.getValue());
                this.httpRequestBuilder.header(header.getKey(), String.valueOf(header.getValue()));
            }
        }
        return this;
    }

    public HttpTokenRequestBuilder withBody(final Map<String, String> body, final String type) throws JsonProcessingException {
        if (!body.isEmpty()) {
            LOG.trace("Adding body to token request.");
            final var parameters = new HashMap<String, String>();
            for (final var entry : body.entrySet()) {
                LOG.trace("Body key = {} Body value = {}", entry.getKey(), entry.getValue());
                parameters.put(entry.getKey(), String.valueOf(entry.getValue()));
            }

            String jsonBody;
            if (type.equals("application/x-www-form-urlencoded")) {
                LOG.trace("Formatting body as form data.");
                jsonBody = parameters.entrySet().stream().map(
                        e -> e.getKey() + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8)
                ).collect(Collectors.joining("&"));

            } else {
                LOG.trace("Formatting body as JSON.");
                jsonBody = Config.getInstance().getMapper().writeValueAsString(parameters);
            }

            /* only POST requests are supported right now. */
            this.httpRequestBuilder.POST(HttpRequest.BodyPublishers.ofString(jsonBody));
        }
        return this;
    }

    public HttpRequest build() {
        return this.httpRequestBuilder.build();
    }
}
