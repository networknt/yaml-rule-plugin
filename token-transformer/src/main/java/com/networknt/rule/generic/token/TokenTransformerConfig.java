package com.networknt.rule.generic.token;

import com.fasterxml.jackson.core.type.TypeReference;
import com.networknt.config.Config;
import com.networknt.config.ConfigException;
import com.networknt.proxy.PathPrefixAuth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TokenTransformerConfig {

    private static final Logger LOG = LoggerFactory.getLogger(TokenTransformerConfig.class);
    public static final String CONFIG_NAME = "token-transformer";
    public static final String PROXY_HOST = "proxyHost";
    public static final String PROXY_PORT = "proxyPort";
    public static final String ENABLE_HTTP2 = "enableHttp2";
    public static final String PATH_PREFIX_AUTHS = "pathPrefixAuths";
    public static final String MODULE_MASKS = "moduleMasks";
    private String proxyHost;
    private int proxyPort;
    private boolean enableHttp2;
    private List<PathPrefixAuth> pathPrefixAuths;
    private List<String> moduleMasks;
    private final Config config;
    private final Map<String, Object> mappedConfig;

    private TokenTransformerConfig() {
        this(CONFIG_NAME);
    }

    /**
     * Please note that this constructor is only for testing to load different config files
     * to test different configurations.
     *
     * @param configName String
     */
    public TokenTransformerConfig(String configName) {
        this.config = Config.getInstance();
        this.mappedConfig = this.config.getJsonMapConfigNoCache(configName);
        setConfigData();
        setConfigList();
    }

    public static TokenTransformerConfig load() {
        return new TokenTransformerConfig();
    }

    public String getProxyHost() {
        return proxyHost;
    }

    public void setProxyHost(String proxyHost) {
        this.proxyHost = proxyHost;
    }

    public int getProxyPort() {
        return proxyPort;
    }

    public void setProxyPort(int proxyPort) {
        this.proxyPort = proxyPort;
    }

    public boolean isEnableHttp2() {
        return enableHttp2;
    }

    public void setEnableHttp2(boolean enableHttp2) {
        this.enableHttp2 = enableHttp2;
    }

    public List<PathPrefixAuth> getPathPrefixAuths() {
        return pathPrefixAuths;
    }

    public Config getConfig() {
        return config;
    }

    public Map<String, Object> getMappedConfig() {
        return mappedConfig;
    }

    public List<String> getModuleMasks() {
        return moduleMasks;
    }

    public void setModuleMasks(List<String> moduleMasks) {
        this.moduleMasks = moduleMasks;
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
    }

    private void setConfigList() {

        // path prefix auth mapping
        if (mappedConfig.get(PATH_PREFIX_AUTHS) != null) {
            var rawPathPrefixNode = mappedConfig.get(PATH_PREFIX_AUTHS);
            this.pathPrefixAuths = new ArrayList<>();

            if (rawPathPrefixNode instanceof String) {
                var s = (String) rawPathPrefixNode;
                s = s.trim();

                LOG.trace("pathPrefixAuth s = {}", s);

                if (s.startsWith("[")) {

                    // json format
                    try {
                        this.pathPrefixAuths = Config.getInstance().getMapper().readValue(s, new TypeReference<>() {});
                    } catch (Exception e) {
                        throw new ConfigException("could not parse the pathPrefixAuth json with a list of string and object.");
                    }

                } else throw new ConfigException("pathPrefixAuth must be a list of string object map.");

            } else if (rawPathPrefixNode instanceof List) {

                // the object is a list of map, we need convert it to PathPrefixAuth object.
                var prefixes = (List<Map<String, Object>>) rawPathPrefixNode;
                prefixes.forEach(value -> this.pathPrefixAuths.add(Config.getInstance().getMapper().convertValue(value, PathPrefixAuth.class)));

            } else throw new ConfigException("pathPrefixAuth must be a list of string object map.");
        }
    }
}
