package org.interledger.ilp.ledger.api;

import org.interledger.ilp.common.config.Config;
import org.junit.Test;

/**
 * Configuration test
 *
 * @author earizon
 */
public class ConfigurationTest {

    @Test
    public void testRunServer() {
        // TODO: Check othe config "stuff" in AbstractMainEntrypointVerticle.initConfig
        Config.create(); // Will raise and exception if parsing application.conf and/or app.conf fails.        
    }

}
