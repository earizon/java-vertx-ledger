package org.interledger.everledger.common.api.auth;


import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

//import io.vertx.ext.auth.AuthProvider;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;
//import io.vertx.ext.web.handler.AuthHandler;

import org.interledger.everledger.common.api.util.ILPExceptionSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthManager {

    private static final Logger log = LoggerFactory.getLogger(AuthManager.class);
    
//    private final AuthProvider authProvider = new -();
//    String realm = authConfig.getString(DEFAULT_BASIC_REALM, Auth.realm);
//    public final AuthHandler authHandler = BasicAuthHandler.create(authProvider, realm);

    private static final Map<String, AuthInfo> users = new HashMap<String, AuthInfo>();
    static {
        // TODO:(0) Hardcoded
        users.put("admin"         , new AuthInfo(        "admin",        "admin",        "admin", "admin"));
        users.put("ilpconnector"  , new AuthInfo( "ilpconnector", "ilpconnector", "ilpconnector", "connector"));
        users.put("alice"         , new AuthInfo(        "alice",        "alice",        "alice", "user"));
        users.put("bob"           , new AuthInfo(          "bob",          "bob",          "bob", "user"));
        users.put("admin"         , new AuthInfo(      "candice",      "candice",      "candice", "user"));
    }
    
    public static Map<String, AuthInfo> getUsers() {
        return users;
    }

    public static AuthInfo authenticate(RoutingContext context, boolean allowAnonymous) {
        HttpServerRequest request = context.request();
        String authorization = request.headers().get(HttpHeaders.AUTHORIZATION);
        if (authorization == null) {
            if (allowAnonymous) { return AuthInfo.ANONYMOUS; }
            throw ILPExceptionSupport.createILPForbiddenException();
        }
        try {
            String[] parts = authorization.split(" ");
            String sscheme = parts[0];
            if (!"Basic".equals(sscheme)) {
                log.error("Only Basic Authorization support supported.");
                throw ILPExceptionSupport.createILPForbiddenException();
            }
            String decoded = new String(Base64.getDecoder().decode(parts[1]));
            int colonIdx = decoded.indexOf(":");
            String suser = (colonIdx != -1) ? decoded.substring(0, colonIdx) : decoded;
            String spass = (colonIdx != -1) ? decoded.substring(colonIdx + 1): null ;
            AuthInfo authInfo = users.get(suser);
            if (authInfo == null){
                log.error("authInfo null. (User not int AuthManager.users lists)");
                throw ILPExceptionSupport.createILPForbiddenException();
            }
            boolean isAdmin = authInfo.getRoll().equals("admin");
            if (!isAdmin && !authInfo.pass.equals(spass)  ) {
                log.error("authInfo null or pass doesn't match");
                throw ILPExceptionSupport.createILPForbiddenException();
            }
            return authInfo;
        } catch (Exception e) {
            log.error(e.toString());
            throw ILPExceptionSupport.createILPForbiddenException();
        }
    }

    public static AuthInfo authenticate(RoutingContext context) {
        return authenticate(context, false /*do not allow anonymous access */);
    }

}