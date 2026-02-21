package com.networknt.rule.epam;

import com.networknt.config.Config;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

public class LoadCertTest {
    @Test
    @Disabled
    public void testCertLoad() {
        InputStream stream = Config.getInstance().getInputStreamFromFile("test.p12");
        System.out.println(stream);
    }
}
