package com.networknt.rule.generic.token.schema.cert;

import com.networknt.rule.generic.token.schema.cert.KeyStoreSchema;
import com.networknt.rule.generic.token.schema.cert.TrustStoreSchema;

public class SSLContextSchema {
    private TrustStoreSchema trustStore;
    private KeyStoreSchema keyStore;
    private String tlsVersion;

    public TrustStoreSchema getTrustStore() {
        return trustStore;
    }

    public KeyStoreSchema getKeyStore() {
        return keyStore;
    }

    public String getTlsVersion() {
        return tlsVersion;
    }

}
