package com.networknt.rule.epam;

import com.networknt.config.Config;
import org.junit.Ignore;
import org.junit.Test;

import java.io.InputStream;

public class LoadCertTest {
    @Test
    @Ignore
    public void testCertLoad() {
        InputStream stream = Config.getInstance().getInputStreamFromFile("test.p12");
        System.out.println(stream);
    }
}
