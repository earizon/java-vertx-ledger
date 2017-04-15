package org.interledger.ilp.common.config.core;

/**
 * Defines a configuration key
 *
 * @author mrmx
 */
public interface ConfigKey {

    /**
     * Get this config key as a {@code String} path representation. Eg:
     * a.key.path file.encoding
     *
     * @return key path
     */
    String getPath();

}
