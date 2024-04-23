package com.networknt.rule.snow;

import com.fasterxml.jackson.core.type.TypeReference;
import com.networknt.config.Config;
import com.networknt.config.ConfigException;
import com.networknt.config.PathPrefixAuth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SnowConfig {
    private static final Logger logger = LoggerFactory.getLogger(SnowConfig.class);

    public static final String CONFIG_NAME = "snow";
    public static final String PATH_PREFIX = "pathPrefix";
    public static final String TOKEN_URL = "tokenUrl";
    public static final String TOKEN_TTL = "tokenTtl";
    public static final String PROXY_HOST = "proxyHost";
    public static final String PROXY_PORT = "proxyPort";
    public static final String ENABLE_HTTP2 = "enableHttp2";
    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";
    public static final String CLIENT_ID = "client_id";
    public static final String CLIENT_SECRET = "client_secret";
    public static final String GRANT_TYPE = "grant_type";
    public static final String PATH_PREFIX_AUTHS = "pathPrefixAuths";

    String proxyHost;
    int proxyPort;
    boolean enableHttp2;

    List<PathPrefixAuth> pathPrefixAuths;
    private final Config config;
    private Map<String, Object> mappedConfig;

    private SnowConfig() {
        this(CONFIG_NAME);
    }

    /**
     * Please note that this constructor is only for testing to load different config files
     * to test different configurations.
     * @param configName String
     */
    private SnowConfig(String configName) {
        config = Config.getInstance();
        mappedConfig = config.getJsonMapConfigNoCache(configName);
        setConfigData();
        setConfigList();
    }
    public static SnowConfig load() {
        return new SnowConfig();
    }

    public static SnowConfig load(String configName) {
        return new SnowConfig(configName);
    }

    void reload() {
        mappedConfig = config.getJsonMapConfigNoCache(CONFIG_NAME);
        setConfigData();
        setConfigList();
    }

    public Map<String, Object> getMappedConfig() {
        return mappedConfig;
    }

    public void setMappedConfig(Map<String, Object> mappedConfig) {
        this.mappedConfig = mappedConfig;
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

    public void setPathPrefixAuths(List<PathPrefixAuth> pathPrefixAuths) {
        this.pathPrefixAuths = pathPrefixAuths;
    }

    private void setConfigData() {
        Object object = mappedConfig.get(PROXY_HOST);
        if(object != null) {
            setProxyHost((String) object);
        }
        object = mappedConfig.get(PROXY_PORT);
        if (object != null) {
            setProxyPort((int) object);
        }
        object = mappedConfig.get(ENABLE_HTTP2);
        if(object != null && (Boolean) object) {
            setEnableHttp2(true);
        }
    }

    private void setConfigList() {
        // path prefix auth mapping
        if (mappedConfig.get(PATH_PREFIX_AUTHS) != null) {
            Object object = mappedConfig.get(PATH_PREFIX_AUTHS);
            pathPrefixAuths = new ArrayList<>();
            if(object instanceof String) {
                String s = (String)object;
                s = s.trim();
                if(logger.isTraceEnabled()) logger.trace("pathPrefixAuth s = " + s);
                if(s.startsWith("[")) {
                    // json format
                    try {
                        pathPrefixAuths = Config.getInstance().getMapper().readValue(s, new TypeReference<List<PathPrefixAuth>>() {});
                    } catch (Exception e) {
                        throw new ConfigException("could not parse the pathPrefixAuth json with a list of string and object.");
                    }
                } else {
                    throw new ConfigException("pathPrefixAuth must be a list of string object map.");
                }
            } else if (object instanceof List) {
                // the object is a list of map, we need convert it to PathPrefixAuth object.
                List<Map<String, Object>> values = (List<Map<String, Object>>)object;
                for(Map<String, Object> value: values) {
                    PathPrefixAuth pathPrefixAuth = new PathPrefixAuth();
                    pathPrefixAuth.setPathPrefix((String)value.get(PATH_PREFIX));
                    pathPrefixAuth.setTokenTtl((Integer)value.get(TOKEN_TTL));
                    pathPrefixAuth.setTokenUrl((String)value.get(TOKEN_URL));
                    pathPrefixAuth.setUsername((String)value.get(USERNAME));
                    pathPrefixAuth.setPassword((String)value.get(PASSWORD));
                    pathPrefixAuth.setClientId((String)value.get(CLIENT_ID));
                    pathPrefixAuth.setClientSecret((String)value.get(CLIENT_SECRET));
                    pathPrefixAuth.setGrantType((String)value.get(GRANT_TYPE));
                    pathPrefixAuths.add(pathPrefixAuth);
                }
            } else {
                throw new ConfigException("pathPrefixAuth must be a list of string object map.");
            }
        }
    }

}
