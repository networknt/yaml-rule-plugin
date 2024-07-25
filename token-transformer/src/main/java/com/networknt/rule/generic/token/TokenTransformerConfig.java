package com.networknt.rule.generic.token;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.networknt.config.Config;
import com.networknt.rule.generic.token.schema.TokenSchema;
import org.apache.commons.lang3.NotImplementedException;

import java.util.List;
import java.util.Map;

public class TokenTransformerConfig {

    public static final String CONFIG_NAME = "token-transformer";
    public static final String TOKEN_SCHEMA = "tokenSchemas";
    public static final String PROXY_HOST = "proxyHost";
    public static final String PROXY_PORT = "proxyPort";
    public static final String ENABLE_HTTP2 = "enableHttp2";
    public static final String MODULE_MASKS = "moduleMasks";
    private final Config config;
    private final Map<String, Object> mappedConfig;

    @JsonProperty("proxyPort")
    private int proxyPort;

    @JsonProperty("proxyHost")
    private String proxyHost;

    @JsonProperty("enableHttp2")
    private boolean enableHttp2;

    @JsonProperty("moduleMasks")
    private List<String> moduleMasks;

    @JsonProperty("tokenSchemas")
    private Map<String, TokenSchema> tokenSchemas;

    public TokenTransformerConfig() {
        this(CONFIG_NAME);
    }

    public TokenTransformerConfig(final String configName) {
        this.config = Config.getInstance();
        this.mappedConfig = this.config.getJsonMapConfigNoCache(configName);
        setConfigData();
    }

    public static TokenTransformerConfig load() {
        return new TokenTransformerConfig();
    }

    public int getProxyPort() {
        return proxyPort;
    }

    public String getProxyHost() {
        return proxyHost;
    }

    public boolean isEnableHttp2() {
        return enableHttp2;
    }

    public Map<String, TokenSchema> getTokenSchemas() {
        return tokenSchemas;
    }

    public List<String> getModuleMasks() {
        return moduleMasks;
    }

    private void setProxyPort(int proxyPort) {
        this.proxyPort = proxyPort;
    }

    private void setProxyHost(String proxyHost) {
        this.proxyHost = proxyHost;
    }

    private void setEnableHttp2(boolean enableHttp2) {
        this.enableHttp2 = enableHttp2;
    }

    private void setModuleMasks(List<String> moduleMasks) {
        this.moduleMasks = moduleMasks;
    }

    private void setTokenSchemas(Map<String, TokenSchema> tokenSchemas) {
        this.tokenSchemas = tokenSchemas;
    }

    private void setConfigData() {
        var object = this.mappedConfig.get(PROXY_HOST);
        if (object instanceof String)
            setProxyHost((String) object);

        object = this.mappedConfig.get(PROXY_PORT);
        if (object instanceof Integer)
            setProxyPort((Integer) object);

        object = this.mappedConfig.get(ENABLE_HTTP2);
        if (object instanceof Boolean)
            setEnableHttp2((Boolean) object);

        object = this.mappedConfig.get(MODULE_MASKS);
        if (object instanceof List
                && !((List<?>) object).isEmpty()
                && ((List<?>) object).get(0) instanceof String)
            setModuleMasks((List<String>) object);

        if (this.mappedConfig.get(TOKEN_SCHEMA) != null) {
            final var rawTokenSchemas = this.mappedConfig.get(TOKEN_SCHEMA);
            if (rawTokenSchemas instanceof Map) {
                final var converted = Config.getInstance().getMapper().convertValue(rawTokenSchemas, new TypeReference<Map<String, TokenSchema>>() {});
                setTokenSchemas(converted);
            } else if (rawTokenSchemas instanceof String) {
                // TODO - handle string.
                throw new NotImplementedException("tokenSchema string-to-pojo not implemented!");
            }
        }

    }
}
