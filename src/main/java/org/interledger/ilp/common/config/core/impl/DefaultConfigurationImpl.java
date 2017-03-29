package org.interledger.ilp.common.config.core.impl;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigRenderOptions;
import java.util.List;
import org.interledger.ilp.common.config.core.ConfigKey;
import org.interledger.ilp.common.config.core.Configuration;
import org.interledger.ilp.common.config.core.ConfigurationException;
import org.interledger.ilp.common.config.core.ObjectConfigKey;
import org.interledger.ilp.common.config.core.RequiredKeyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default {@link Configuration} implementation using
 * <a href="https://github.com/typesafehub/config">https://github.com/typesafehub/config</a>.
 * See also
 * <a href="https://github.com/typesafehub/config/blob/master/HOCON.md"
 * target="new">HOCON documentation</a>.
 *
 * @author mrmx
 */
public final class DefaultConfigurationImpl implements Configuration {

    private static final Logger log = LoggerFactory.getLogger(DefaultConfigurationImpl.class);
    private static final String CONFIG_LOWERCASEKEYS = "config.lowercasekeys";
    private static final String CONFIG_DEBUG = "config.debug";

    private Config config;

    /**
     * Flag for converting all configuration keys to lowercase.
     *
     * Defaults to {@code true}
     */
    private boolean lowercaseKeys;

    public DefaultConfigurationImpl() {
    }

    public DefaultConfigurationImpl(Config parentConfig,DefaultConfigurationImpl parent) {
        this.config = parentConfig;
        this.lowercaseKeys = parent.isLowercaseKeys();
    }

    @Override
    public void loadDefault() {
        config = ConfigFactory.load();
        init();
    }

    @Override
    public void load(String path) {
        config = ConfigFactory.load(path);
        init();
    }

    @Override
    public boolean hasKey(ConfigKey key) {
        return config.hasPath(filterKeyPath(key));
    }

    @Override
    public ConfigKey requireKey(ConfigKey key) throws RequiredKeyException {
        String path = filterKeyPath(key);
        if (!config.hasPath(path)) {
            throw new RequiredKeyException(path);
        }
        return key;
    }

    @Override
    public Configuration getConfiguration(ConfigKey prefix) throws ConfigurationException {
        requireKey(prefix);
        try {
            Config child = config.getConfig(filterKeyPath(prefix));
            return new DefaultConfigurationImpl(child,this);
        } catch (Exception ex) {
            throw new ConfigurationException("Unable to get child configuration at path '" + prefix + "'", ex);
        }
    }

    @Override
    public String getString(ConfigKey key) throws ConfigurationException {
        try {
            return config.getString(filterKeyPath(key));
        } catch (Exception ex) {
            throw new ConfigurationException("(String) key: " + key, ex);
        }
    }

    @Override
    public String getString(ConfigKey key, String defaultValue) {
        String value = defaultValue;
        try {
            value = config.getString(filterKeyPath(key));
        } catch (Exception ex) {
            log.warn("key: {} {}", key, ex.getMessage());
        }
        return value;
    }
    
    @Override
    public List<String> getStringList(ConfigKey key) throws ConfigurationException {
        try {
            return config.getStringList(filterKeyPath(key));
        } catch (Exception ex) {
            throw new ConfigurationException("(String) key: " + key, ex);
        }
    }

    @Override
    public int getInt(ConfigKey key) throws ConfigurationException {
        try {
            return config.getInt(filterKeyPath(key));
        } catch (Exception ex) {
            throw new ConfigurationException("(int) key: " + key, ex);
        }
    }

    @Override
    public int getInt(ConfigKey key, int defaultValue) {
        int value = defaultValue;
        try {
            value = config.getInt(filterKeyPath(key));
        } catch (Exception ex) {
            log.warn("key: {} {}", key, ex.getMessage());
        }
        return value;
    }

    @Override
    public boolean getBoolean(ConfigKey key) throws ConfigurationException {
        try {
            return config.getBoolean(filterKeyPath(key));
        } catch (Exception ex) {
            throw new ConfigurationException("(boolean) key: " + key, ex);
        }
    }

    @Override
    public boolean getBoolean(ConfigKey key, boolean defaultValue) {
        boolean value = defaultValue;
        try {
            value = config.getBoolean(filterKeyPath(key));
        } catch (Exception ex) {
            log.warn("key: {} {}", key, ex.getMessage());
        }
        return value;
    }

    @Override
    public boolean isLowercaseKeys() {
        return lowercaseKeys;
    }

    @Override
    public void debug() {
        log.debug(
        "\n############## Configuration debug dump: ##############\n{}####################################################",
            config.root().render(
                ConfigRenderOptions.defaults()
                .setOriginComments(false)
                .setComments(false)
        ));
    }

    private String filterKeyPath(ConfigKey key) {
        if (key == null) {
            throw new IllegalArgumentException(ConfigKey.class.getSimpleName());
        }
        String path = key.getPath();
        return lowercaseKeys ? path.toLowerCase() : path;
    }

    private void init() {
        lowercaseKeys = getBoolean(ObjectConfigKey.of(CONFIG_LOWERCASEKEYS), true);
        if (getBoolean(ObjectConfigKey.of(CONFIG_DEBUG), false)) {
            log.info(
                    "\n############## Effective configuration: ##############\n{}######################################################",
                    config.root().render(
                            ConfigRenderOptions.defaults()
                            .setOriginComments(false)
                            .setComments(false)
                    ));
        }
    }

}
