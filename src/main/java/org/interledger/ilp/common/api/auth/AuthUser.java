package org.interledger.ilp.common.api.auth;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.AbstractUser;
import io.vertx.ext.auth.AuthProvider;

/**
 * Base user class
 *
 * @author mrmx
 */
public abstract class AuthUser extends AbstractUser {

    private final AuthInfo authInfo;
    private AuthProvider authProvider;

    AuthUser(AuthInfo authInfo) {
        this.authInfo = authInfo;
    }

    public AuthInfo getAuthInfo() {
        return authInfo;
    }

    @Override
    public int hashCode() {
        return authInfo.hashCode();
    }

    @Override
    public JsonObject principal() {
        return authInfo.getPrincipal();
    }

    @Override
    public void setAuthProvider(AuthProvider authProvider) {
        this.authProvider = authProvider;
    }

    @Override
    public String toString() {
        return authInfo.toString();
    }

}
