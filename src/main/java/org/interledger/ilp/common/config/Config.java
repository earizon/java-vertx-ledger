package org.interledger.ilp.common.config;

import com.google.common.base.Optional;
import java.util.Arrays;
import java.util.List;
import org.interledger.ilp.common.config.core.ArrayConfigKey;
import org.interledger.ilp.common.config.core.ClassConfigKey;
import org.interledger.ilp.common.config.core.CompoundConfigKey;
import org.interledger.ilp.common.config.core.ConfigKey;
import org.interledger.ilp.common.config.core.Configurable;
import org.interledger.ilp.common.config.core.Configuration;
import org.interledger.ilp.common.config.core.ConfigurationException;
import org.interledger.ilp.common.config.core.EnumConfigKey;
import org.interledger.ilp.common.config.core.ObjectConfigKey;
import org.interledger.ilp.common.config.core.RequiredKeyException;
import org.interledger.ilp.common.config.core.impl.DefaultConfigurationImpl;
import org.interledger.ilp.common.util.EnumUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Config façade to a default (but swappable) configuration subsystem.
 *
 * @author mrmx
 */
@SuppressWarnings("rawtypes")
public class Config {

    private static final Logger log = LoggerFactory.getLogger(Config.class);

    private static final String DEFAULT_CONFIG_FILE = "application.conf";

    private static final String MSG_ILLEGAL_NO_KEY_ARGS = "No key argument";

    private Configuration configuration;

    /**
     * Default ctor
     */
    public Config() {
        this(new DefaultConfigurationImpl());
    }

    /**
     *
     * @param configuration
     */
    public Config(Configuration configuration) {
        this.configuration = configuration;
    }

    /**
     * Loads default configuration from default config file.
     *
     * @return {@code Config}
     */
    public Config load() {
        configuration.load(DEFAULT_CONFIG_FILE);
        return this;
    }

    /**
     * Create and load default configuration from default config file.
     *
     * @return {@code Config}
     */
    public static Config create() {
        return new Config().load();
    }

    /**
     * Dumps this {@code Config} instance for debug inspection (via defined
     * logger)
     */
    public void debug() {
        configuration.debug();
    }

    /**
     * Applies this configuration to a possible {@code Configurable} object.
     *
     * @param target Object to apply configuration if it is a
     * {@code Configurable} instance.
     * @return this instace
     */
    public Config apply(Object target) {
        if (target == null) {
            log.warn("Unable to apply configuration to a null instance");
            return this;
        }
        if (Configurable.class.isAssignableFrom(target.getClass())) {
            log.debug("Configuring {}", target.getClass().getSimpleName());
            ((Configurable) target).configure(this);
        }
        return this;
    }

    /**
     * Checks that a key is defined.
     *
     * @param key
     * @return {@code true} if key is found, {@code false} otherwise.
     */
    public boolean hasKey(Enum... key) {
        return configuration.hasKey(EnumConfigKey.of(key));
    }

    /**
     * Checks that a key is defined.
     *
     * @param key
     * @return {@code true} if key is found, {@code false} otherwise.
     */
    public boolean hasKey(String... key) {
        return configuration.hasKey(ObjectConfigKey.of(key));
    }

    /**
     * Checks that a key is defined or throws a {@code RequiredKeyException}.
     * This method should be more efficient due to the fact that no value
     * evaluation and/or conversions will be performed.
     *
     * @param key
     * @return the resulting {@code ConfigKey} created from the input
     * {@code Enum} key/s
     * @throws RequiredKeyException if key is not found
     */
    public ConfigKey requireKey(Enum... key) throws RequiredKeyException {
        checkKey(key);
        return configuration.requireKey(EnumConfigKey.of(key));
    }

    /**
     * Checks that a key is defined or throws a {@code RequiredKeyException}.
     * This method should be more efficient due to the fact that no value
     * evaluation and/or conversions will be performed.
     *
     * @param key
     * @return the resulting {@code ConfigKey} created from the input
     * {@code String} key/s
     * @throws RequiredKeyException if key is not found
     */
    public ConfigKey requireKey(String... key) throws RequiredKeyException {
        checkKey(key);
        return configuration.requireKey(ArrayConfigKey.of(key));
    }

    /**
     * Checks that a key is defined or throws a {@code RequiredKeyException}.
     * This method should be more efficient due to the fact that no value
     * evaluation and/or conversions will be performed.
     *
     * @param key
     * @return the resulting {@code ConfigKey} created from the input
     * {@code Enum} {@code Class} key
     * @throws RequiredKeyException if key is not found
     */
    public <T extends Enum> ConfigKey requireKey(Class<T> key) throws RequiredKeyException {
        checkKey(key);
        return configuration.requireKey(ClassConfigKey.of(key));
    }

    /**
     * Creates a child configuration from a prefix key
     *
     * @param prefixKey
     * @return {@code Config} child.
     * @throws RequiredKeyException if no prefix is found
     * @throws ConfigurationException if some other error occurred.
     */
    public Config getConfig(Enum... prefixKey) throws RequiredKeyException, ConfigurationException {
        return new Config(configuration.getConfiguration(requireKey(prefixKey)));
    }

    /**
     * Creates a child configuration from a prefix key
     *
     * @param prefixKey
     * @return {@code Config} child.
     * @throws RequiredKeyException if no prefix is found
     * @throws ConfigurationException if some other error occurred.
     */
    public Config getConfig(String... prefixKey) throws RequiredKeyException, ConfigurationException {
        return new Config(configuration.getConfiguration(requireKey(prefixKey)));
    }

    /**
     * Creates a child configuration from a prefix key
     *
     * @param prefixKey
     * @return {@code Config} child.
     * @throws RequiredKeyException if no prefix is found
     * @throws ConfigurationException if some other error occurred.
     */
    public <T extends Enum<T>> Config getConfig(Class<T> prefixKey) throws RequiredKeyException, ConfigurationException {
        return new Config(configuration.getConfiguration(requireKey(prefixKey)));
    }

    /**
     * Gets an {@link Optional} child configuration from a prefix key
     *
     * @param prefixKey
     * @return {@link Optional} {@code Config} child.
     * @throws ConfigurationException if some other error occurred.
     */
    public <T extends Enum<T>> Optional<Config> getOptionalConfig(Class<T> prefixKey) throws ConfigurationException {
        ConfigKey configKey = ClassConfigKey.of(prefixKey);
        if (!configuration.hasKey(configKey)) {
            return Optional.absent();
        }
        return Optional.of(new Config(configuration.getConfiguration(configKey)));
    }

    /**
     * Gets an {@link Optional} child configuration from a prefix key
     *
     * @param prefixKey
     * @return {@link Optional} {@code Config} child.
     * @throws ConfigurationException if some other error occurred.
     */
    public <T extends Enum<T>> Optional<Config> getOptionalConfig(String... prefixKey) throws ConfigurationException {
        ConfigKey configKey = ArrayConfigKey.of(prefixKey);
        if (!configuration.hasKey(configKey)) {
            return Optional.absent();
        }
        return Optional.of(new Config(configuration.getConfiguration(configKey)));
    }

    /**
     * Gets a required {@code Enum} value within a defined enum type for an
     * {@code Enum}(s) key type context.
     *
     * The resulting key is [keyContext]enumtype
     *
     * @param enumType Enum {@code Class} type
     * @param keyContext Optional Key context/namespace
     * @return {@code Enum} value
     * @throws RequiredKeyException if key is not found
     * @throws ConfigurationException if some other error occurred
     */
    public <T extends Enum<T>> T getEnum(Class<T> enumType, Enum... keyContext) throws RequiredKeyException, ConfigurationException {
        ConfigKey configKey = ClassConfigKey.of(enumType);
        if (keyContext != null && keyContext.length > 0) {
            configKey = CompoundConfigKey.of(EnumConfigKey.of(keyContext), configKey);
        }
        configuration.requireKey(configKey);
        String enumName = configuration.getString(configKey);
        T value = EnumUtil.getEnumValue(enumType, enumName, configuration.isLowercaseKeys());
        if (value == null) {
            throw new ConfigurationException(
                    "Enum '" + enumName + "' not found for key " + configKey
                    + ". Should be one of " + Arrays.asList(enumType.getEnumConstants())
            );
        }
        return value;
    }

    /**
     * Gets a required {@code String} value for an {@code Enum}(s) key type.
     *
     * @param key
     * @return {@code String} value
     * @throws RequiredKeyException if key is not found
     * @throws ConfigurationException if some other error occurred
     */
    public String getString(Enum... key) throws RequiredKeyException, ConfigurationException {
        return configuration.getString(requireKey(key));
    }

    /**
     * Gets a {@code String} value for an enum(s) key type.
     *
     * @param defaultValue
     * @param key
     * @return {@code String} if key is found, defaultValue otherwise
     */
    public String getString(String defaultValue, Enum... key) {
        checkKey(key);
        return configuration.getString(EnumConfigKey.of(key), defaultValue);
    }

    /**
     * Gets a required {@code String} value for a {@code String}(s) key type.
     *
     * @param key
     * @return {@code String} value
     * @throws RequiredKeyException if key is not found
     * @throws ConfigurationException if some other error occurred
     */
    public String getStringFor(String... key) throws RequiredKeyException, ConfigurationException {
        return configuration.getString(configuration.requireKey(ObjectConfigKey.of(key)));
    }

    /**
     * Gets a required {@link java.util.List} of {@code String} values for a
     * {@code String}(s) key type.
     *
     * @param key
     * @return {@link java.util.List} of {@code String} values
     * @throws RequiredKeyException if key is not found
     * @throws ConfigurationException if some other error occurred
     */
    public List<String> getStringList(Enum... key) throws RequiredKeyException, ConfigurationException {
        return configuration.getStringList(requireKey(key));
    }

    /**
     * Gets a required {@code int} value for an enum(s) key type.
     *
     * @param key
     * @return {@code int} value if key is found and its value is convertible to
     * an {@code int}
     * @throws RequiredKeyException if key is not found
     * @throws ConfigurationException if some other error occurred
     */
    public int getInt(Enum... key) throws RequiredKeyException, ConfigurationException {
        return configuration.getInt(requireKey(key));
    }

    /**
     * Gets a required {@code int} value for an {@code Enum}(s) key type.
     *
     * @param defaultValue
     * @param key
     * @return {@code int} value if key is found and its value is convertible to
     * an {@code int} or defaultValue otherwise
     */
    public int getInt(int defaultValue, Enum... key) {
        checkKey(key);
        return configuration.getInt(EnumConfigKey.of(key), defaultValue);
    }

    /**
     * Gets a required {@code boolean} value for an {@code Enum}(s) key type.
     *
     * @param key
     * @return {@code boolean} value if key is found and its value is
     * convertible to a {@code boolean}
     * @throws RequiredKeyException if key is not found
     * @throws ConfigurationException if some other error occurred
     */
    public boolean getBoolean(Enum... key) throws RequiredKeyException, ConfigurationException {
        return configuration.getBoolean(requireKey(key));
    }

    /**
     * Gets a {@code boolean} value for an {@code Enum}(s) key type.
     *
     * @param defaultValue
     * @param key
     * @return {@code true} if key is found and its value is {@code true} or
     * defaultValue otherwise
     */
    public boolean getBoolean(boolean defaultValue, Enum... key) {
        checkKey(key);
        return configuration.getBoolean(EnumConfigKey.of(key), defaultValue);
    }

    private <T> void checkKey(T... key) {
        if (key == null || key.length == 0) {
            throw new IllegalArgumentException(MSG_ILLEGAL_NO_KEY_ARGS);
        }
    }

    private <T extends Enum> void checkKey(Class<T> key) {
        if (key == null) {
            throw new IllegalArgumentException(MSG_ILLEGAL_NO_KEY_ARGS);
        }
    }

}
