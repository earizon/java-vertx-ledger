package org.interledger.ilp.common.config.core;

/**
 * Base class for {@code ConfigKey} implementations.
 *
 * @author mrmx
 */
public abstract class AbstractConfigKey implements ConfigKey {

    /**
     * Default path separator for keys. Eg: a.key.with.subkeys
     */
    protected static final String DEFAULT_PATH_SEPARATOR = ".";

    protected final String separator;
    private String path;
    private StringBuilder pathBuilder;

    public AbstractConfigKey() {
        this(DEFAULT_PATH_SEPARATOR);
    }

    public AbstractConfigKey(String separator) {
        this.separator = separator;
    }

    public String getSeparator() {
        return separator;
    }

    @Override
    public String getPath() {
        if (path == null) {
            pathBuilder = new StringBuilder();
            buildKeyPath();
            path = pathBuilder.toString();
            pathBuilder = null;
        }
        return path;
    }

    @Override
    public String toString() {
        return getPath();
    }

    protected abstract void buildKeyPath();

    protected void append(Object key) {
        if (pathBuilder.length() > 0) {
            pathBuilder.append(separator);
        }
        pathBuilder.append(key);
    }
}
