package org.interledger.ilp.common.api.auth;

/**
 * Defines a {@code Supplier} of {@code AuthUser} instances.
 * 
 * @author mrmx
 */
public interface AuthUserSupplier<T extends AuthUser> {
    
    T getAuthUser(AuthInfo authInfo);
}
