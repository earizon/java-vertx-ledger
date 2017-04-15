package org.interledger.ilp.common.config.core;

/**
 * Defines a configuration {@code RuntimeException}.
 *
 * @author mrmx
 */
public class ConfigurationException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs an instance of {@code ConfigurationException} with the
     * specified detail message.
     *
     * @param msg the detail message.
     */
    public ConfigurationException(String msg) {
        super(msg);
    }

    /**
     * Constructs an instance of {@code ConfigurationException} with the
     * specified detail message and {@code Throwable} cause.
     *
     * @param msg the detail message.
     * @param cause the {@code Throwable} cause
     */
    public ConfigurationException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
