package org.interledger.everledger.common.api.auth;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.AuthHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthManager {

    private static final Logger log = LoggerFactory.getLogger(AuthManager.class);
    
    private static final Map<String, AuthInfo> users = new HashMap<String, AuthInfo>();
    static {
        // TODO:(0) Hardcoded
        users.put("admin", new AuthInfo( "admin",   "admin",  "admin", "admin"));
        users.put("admin", new AuthInfo( "alice",   "alice",  "alice",  "user"));
        users.put("admin", new AuthInfo(   "bob",     "bob",    "bob",  "user"));
        users.put("admin", new AuthInfo("candice","candice","candice",  "user"));
    }

    private final AuthHandler authHandler;

    public AuthManager(AuthHandler authHandler) {
        this.authHandler = authHandler;
    }

    public AuthHandler getAuthHandler() {
        return authHandler;
    }

    private static AuthInfo getAuthInfo(RoutingContext context) {
        HttpServerRequest request = context.request();
        String authorization = request.headers().get(HttpHeaders.AUTHORIZATION);
        if (authorization == null) {
            return AuthInfo.ANONYMOUS;
        }

        try {
            String[] parts = authorization.split(" ");
            String sscheme = parts[0];
            if (!"Basic".equals(sscheme)) {
                // TODO:(0) 40X denied
                throw new RuntimeException("Only Basic authentication is supported");
            }
            String decoded = new String(Base64.getDecoder().decode(parts[1]));
            int colonIdx = decoded.indexOf(":");
            String suser = (colonIdx != -1) ? decoded.substring(0, colonIdx) : decoded;
            String spass = (colonIdx != -1) ? decoded.substring(colonIdx + 1): null ;
            AuthInfo authInfo = users.get(suser);
            if (authInfo == null || authInfo.pass.equals(spass)) {
             // TODO:(0) 40X denied
             throw new RuntimeException("Denied");
            }
            return authInfo;
        } catch (Exception e) {
            // TODO:(0) 40X denied
            throw new RuntimeException("Denied");
        }
    }

    public static void authenticate(RoutingContext context, Handler<AsyncResult<AuthInfo>> resultHandler, String roll) {
        AuthInfo authInfo = getAuthInfo(context);
        if (!authInfo.getRoll().equals(roll)){
            // TODO:(0) 40x denied
            throw new RuntimeException("Denied");
        }
    }

}