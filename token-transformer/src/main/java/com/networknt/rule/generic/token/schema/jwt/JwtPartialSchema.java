package com.networknt.rule.generic.token.schema.jwt;

import com.networknt.rule.generic.token.schema.TtlUnit;
import com.networknt.utility.HashUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JwtPartialSchema {
    private static final Logger LOG = LoggerFactory.getLogger(JwtPartialSchema.class);
    private Map<String, String> staticFields;
    private List<String> uuidFields;
    private List<String> currentTimeFields;
    private List<String> expiryFields;

    public Map<String, String> getStaticFields() {
        return staticFields;
    }

    public List<String> getUuidFields() {
        return uuidFields;
    }

    public List<String> getCurrentTimeFields() {
        return currentTimeFields;
    }

    public List<String> getExpiryFields() {
        return expiryFields;
    }

    public Map<String, String> buildJwtMap(final long ttl, TtlUnit ttlUnit) {
        final var jwtMap = new HashMap<String, String>();

        if (this.staticFields != null)
            jwtMap.putAll(this.staticFields);


        if (this.expiryFields != null) {
            final var jwtTtlUnit = TtlUnit.SECOND;
            final var ttlInMillis = ttlUnit.unitToMillis(ttl);
            final var jwtTtl = jwtTtlUnit.millisToUnit(ttlInMillis);
            LOG.debug("Token TTL is {}", jwtTtl);
            for (final var expiryField : this.expiryFields)
                jwtMap.put(expiryField, String.valueOf(System.currentTimeMillis()/1000 + jwtTtl));

        }

        if (this.currentTimeFields != null)
            for (final var currentTimeField : this.currentTimeFields)
                jwtMap.put(currentTimeField, String.valueOf(System.currentTimeMillis()/1000));

        if (this.uuidFields != null)
            for (final var uuidField : this.uuidFields)
                jwtMap.put(uuidField, HashUtil.generateUUID());

        LOG.debug("Header section created = {}", jwtMap);
        return jwtMap;

    }
}
