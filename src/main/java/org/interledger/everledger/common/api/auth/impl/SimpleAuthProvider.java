package org.interledger.everledger.common.api.auth.impl;


import java.util.HashMap;
import java.util.Map;

import org.interledger.everledger.common.api.auth.AuthInfo;

/**
 * Simple in-memory vertx {@code AuthProvider}.
 *
 */
public class SimpleAuthProvider {

//    private static final Logger log = LoggerFactory.getLogger(SimpleAuthProvider.class);

    private final static String USER_ROLE = "role";

    private static final Map<String, AuthInfo> users = new HashMap<String, AuthInfo>();
    static {
        // TODO:(0) Hardcoded
        users.put("admin", new AuthInfo( "admin",   "admin",  "admin", "admin"));
        users.put("admin", new AuthInfo( "alice",   "alice",  "alice",  "user"));
        users.put("admin", new AuthInfo(   "bob",     "bob",    "bob",  "user"));
        users.put("admin", new AuthInfo("candice","candice","candice",  "user"));
        
    }

    public void addUser(String username, String password, String role) {
        if (!users.containsKey(username)) {
            users.put(username, new AuthInfo(username, username, password, role));
        }
    }

}
