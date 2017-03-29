package org.interledger.ilp.common.api.auth;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.shareddata.impl.ClusterSerializable;
import java.util.Iterator;
import java.util.Map;

/**
 * Auth information holding username, pass, token.
 *
 * @author mrmx
 */
public class AuthInfo implements Iterable<Map.Entry<String, Object>>, ClusterSerializable {

    public static final String USERNAME = "username";
    public static final String PASSWORD = "password";

    private final JsonObject principalDelegate;
    private final String credentialKey;

    private AuthInfo(JsonObject delegate, String credentialKey) {
        this.principalDelegate = delegate;
        this.credentialKey = credentialKey;
    }

    private AuthInfo(String credentialKey) {
        this(new JsonObject(), credentialKey);
    }

    public JsonObject getPrincipal() {
        return principalDelegate;
    }

    public String getUsername() {
        return principalDelegate.getString(USERNAME);
    }

    public String getCredential() {
        return principalDelegate.getString(credentialKey);
    }

    public static AuthInfo basic(JsonObject authInfo) {
        return new AuthInfo(authInfo, PASSWORD);
    }

    public static AuthInfo basic(String username, String credential) {
        return new AuthInfo(PASSWORD)
                .put(USERNAME, username).put(PASSWORD, credential);
    }

    public boolean isEmpty() {
        return principalDelegate == null || principalDelegate.isEmpty();
    }

    public AuthInfo put(String key, String value) {
        principalDelegate.put(key, value);
        return this;
    }

    @Override
    public Iterator<Map.Entry<String, Object>> iterator() {
        return principalDelegate.iterator();
    }

    @Override
    public void writeToBuffer(Buffer buffer) {
        principalDelegate.writeToBuffer(buffer);
    }

    @Override
    public int readFromBuffer(int pos, Buffer buffer) {
        return principalDelegate.readFromBuffer(pos, buffer);
    }

    @Override
    public int hashCode() {
        return principalDelegate.hashCode();
    }

    @Override
    public String toString() {
        return principalDelegate == null ? null : principalDelegate.toString();
    }

}
