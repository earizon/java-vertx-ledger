package org.interledger.ilp.common.config.core;

import java.util.List;

/**
 * Defines a configuration object.
 *
 * @author mrmx
 */
public interface Configuration {

    void loadDefault();

    void load(String path);
    
    void debug();
    
    boolean isLowercaseKeys();

    boolean hasKey(ConfigKey key);

    ConfigKey requireKey(ConfigKey key) throws RequiredKeyException;

    Configuration getConfiguration(ConfigKey prefix) throws ConfigurationException;

    String getString(ConfigKey key) throws ConfigurationException;
    String getString(ConfigKey key, String defaultValue);
    List<String> getStringList(ConfigKey key) throws ConfigurationException;

    int getInt(ConfigKey key) throws ConfigurationException;
    int getInt(ConfigKey key, int defaultValue);

    boolean getBoolean(ConfigKey key) throws ConfigurationException;
    boolean getBoolean(ConfigKey key, boolean defaultValue);
}
