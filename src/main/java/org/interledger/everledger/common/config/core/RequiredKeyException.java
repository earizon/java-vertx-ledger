package org.interledger.ilp.common.config.core;

/**
 * A {@code ConfigurationException} signaling that a required key is missing.
 *
 * @author mrmx
 */
public class RequiredKeyException extends ConfigurationException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates a new instance of {@code RequiredKeyException} with the required
     * {@code ConfigKey} as the detail message.
     *
     * @param key
     */
    public RequiredKeyException(ConfigKey key) {
        this(key.getPath());
    }

    /**
     * Constructs an instance of {@code RequiredKeyException} with the specified
     * detail message.
     *
     * @param msg the detail message.
     */
    public RequiredKeyException(String msg) {
        super(msg);
    }
}
