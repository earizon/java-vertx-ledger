package org.interledger.ilp.common.api.auth;

/**
 * Auth related RTE
 * 
 * @author mrmx
 */
public class AuthException extends RuntimeException {

    /**
     * Constructs an instance of {@code AuthException} with the specified
     * detail message.
     *
     * @param msg the detail message.
     */
    public AuthException(String msg) {
        super(msg);
    }
}
