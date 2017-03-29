package org.interledger.ilp.common.api.auth.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.AuthProvider;
import io.vertx.ext.auth.User;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.interledger.ilp.common.api.auth.AuthInfo;
import org.interledger.ilp.common.api.auth.AuthUserSupplier;
import org.interledger.ilp.common.api.auth.RoleUser;
import org.interledger.ilp.common.config.Config;
import org.interledger.ilp.common.config.core.Configurable;
import org.interledger.ilp.common.config.core.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple in-memory vertx {@code AuthProvider}.
 *
 * @author mrmx
 */
public class SimpleAuthProvider implements Configurable, AuthProvider , AuthUserSupplier<SimpleAuthProvider.SimpleUser> {

    private static final Logger log = LoggerFactory.getLogger(SimpleAuthProvider.class);

    private final static String USER_ROLE = "role";

    private final Map<String, SimpleUser> users;

    /**
     * Configuration keys
     */
    private enum Key {
        users
    }

    public SimpleAuthProvider() {
        this.users = new HashMap<>();
    }

    public void addUser(String username, String password, String role) {
        if (!users.containsKey(username)) {
            users.put(username, new SimpleUser(username, password, role));
        }
    }

    @Override
    public void configure(Config config) throws ConfigurationException {
        List<String> users = config.getStringList(Key.users);
        for (String user : users) {
            addUser(user,
                    config.getStringFor(user, "pass"),
                    config.hasKey(user, USER_ROLE)
                    ? config.getStringFor(user, USER_ROLE) : null
            );
        }
    }

    @Override
    public SimpleUser getAuthUser(AuthInfo authInfo) {
        return users.get(authInfo.getUsername());
    }
    
    @Override
    public void authenticate(JsonObject auth, Handler<AsyncResult<User>> resultHandler) {
        AuthInfo authInfo = AuthInfo.basic(auth);
        SimpleUser user = getAuthUser(authInfo);
        if (user != null && user.getPassword().equals(authInfo.getCredential())) {
            resultHandler.handle(Future.succeededFuture(user));
        } else {
            resultHandler.handle(Future.failedFuture(authInfo.getUsername()));
        }
    }

    public static final class SimpleUser extends RoleUser {

        public SimpleUser(String username, String password, String role) {
            super(AuthInfo.basic(username, password).put(USER_ROLE, role), role);
        }

        public String getUsername() {
            return getAuthInfo().getUsername();
        }

        public String getPassword() {
            return getAuthInfo().getCredential();
        }

    }

}
