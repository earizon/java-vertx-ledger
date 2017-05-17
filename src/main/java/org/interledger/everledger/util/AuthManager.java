package org.interledger.everledger.util;


import java.util.Base64;
import java.util.HashMap;
import java.util.Map;





//import io.vertx.ext.auth.AuthProvider;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;
//import io.vertx.ext.web.handler.AuthHandler;





import org.interledger.everledger.AuthInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AuthManager {

    private static final Logger log = LoggerFactory.getLogger(AuthManager.class);
    
//    private final AuthProvider authProvider = new -();
//    String realm = authConfig.getString(DEFAULT_BASIC_REALM, Auth.realm);
//    public final AuthHandler authHandler = BasicAuthHandler.create(authProvider, realm);

    private static final Map<String, AuthInfo> users = new HashMap<String, AuthInfo>();

    public static Map<AuthInfo, Integer /*blance*/> configureDevelopmentEnvironment() {
        if (! org.interledger.everledger.Config.unitTestsActive) {
            throw new RuntimeException("developer.unitTestsActive must be true @ application.conf "
                    + "to be able to reset tests");
        }
        Map<AuthInfo, Integer /*blance*/> result = new HashMap<AuthInfo, Integer /*blance*/>();
        AuthInfo admin        = new AuthInfo(        "admin",        "admin",        "admin", "admin");
        AuthInfo ilpconnector = new AuthInfo( "ilpconnector", "ilpconnector", "ilpconnector", "connector");
        AuthInfo alice        = new AuthInfo(        "alice",        "alice",        "alice", "user");
        AuthInfo bob          = new AuthInfo(          "bob",          "bob",          "bob", "user");
        // AuthInfo noBalance    = new AuthInfo(    "nobalance",    "nobalance",    "nobalance", "user"); 
        AuthInfo eve          = new AuthInfo(          "eve",          "eve",          "eve", "user");

        users.put("admin"       , admin       ); result.put(admin       , 10000);
        users.put("ilpconnector", ilpconnector); result.put(ilpconnector,   100);
        users.put("alice"       , alice       ); result.put(alice       ,   100);
        users.put("bob"         , bob         ); result.put(bob         ,     0);
        users.put("eve"         , eve         ); result.put(eve         ,     0);
        // users.put("nobalance"   , noBalance   ); result.put(noBalance   ,     0);
        return result;
    }
    
    public static Map<String, AuthInfo> getUsers() {
        return users;
    }
    
    public static void setUser(String id, String pass, String roll){
        final AuthInfo ai = new AuthInfo(id, id, pass, roll);
        users.put(id, ai);
    }

    public static AuthInfo authenticate(RoutingContext context, boolean allowAnonymous) {
        HttpServerRequest request = context.request();
        String authorization = request.headers().get(HttpHeaders.AUTHORIZATION);
        if (authorization == null) {
            if (allowAnonymous) { return AuthInfo.ANONYMOUS; }
            throw ILPExceptionSupport.createILPUnauthorizedException();
        }
        try {
            String[] parts = authorization.split(" ");
            String sscheme = parts[0];
            if (!"Basic".equals(sscheme)) {
                log.error("Only Basic Authorization support supported.");
                throw ILPExceptionSupport.createILPUnauthorizedException();
            }
            String decoded = new String(Base64.getDecoder().decode(parts[1]));
            int colonIdx = decoded.indexOf(":");
            String suser = (colonIdx != -1) ? decoded.substring(0, colonIdx) : decoded;
            String spass = (colonIdx != -1) ? decoded.substring(colonIdx + 1): null ;
            AuthInfo authInfo = users.get(suser);
            if (authInfo == null){
                log.error("authInfo null. (User not int AuthManager.users lists)");
                throw ILPExceptionSupport.createILPUnauthorizedException();
            }
            boolean isAdmin = authInfo.getRoll().equals("admin");
            if (!isAdmin && !authInfo.pass.equals(spass)  ) {
                log.error("user "+authInfo.id+" is not admin and pass doesn't match");
                throw ILPExceptionSupport.createILPUnauthorizedException();
            }
            return authInfo;
        } catch (Exception e) {
            log.error("Unhandled Auth Exception: " + e.toString());
            throw ILPExceptionSupport.createILPUnauthorizedException();
        }
    }

    public static AuthInfo authenticate(RoutingContext context) {
        return authenticate(context, false /*do not allow anonymous access */);
    }

}