package org.interledger.ilp.common.config.core;

import org.interledger.ilp.common.config.Config;
import org.interledger.ilp.common.config.core.ConfigurationException;

/**
 * Defines a runtime configurable object via {@code Config}.
 * 
 * @author mrmx>
 */
public interface Configurable {
    public void configure(Config config) throws ConfigurationException;
}
