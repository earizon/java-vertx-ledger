package com.everis.everledger.util

import com.everis.everledger.AccessRoll
import com.everis.everledger.AuthInfo
import com.everis.everledger.util.Config
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import io.jsonwebtoken.impl.crypto.MacProvider
import io.vertx.core.http.HttpHeaders
import io.vertx.ext.web.RoutingContext
import org.slf4j.LoggerFactory
import java.security.Key
import java.util.*

object AuthManager {

    private val log = LoggerFactory.getLogger(AuthManager::class.java)
    var SigAlgth = SignatureAlgorithm.HS256
    var key: Key = MacProvider.generateKey(SigAlgth)
    var parser = Jwts.parser().setSigningKey(key)


    //    private final AuthProvider authProvider = new -();
    //    String realm = authConfig.getString(DEFAULT_BASIC_REALM, Auth.realm);
    //    public final AuthHandler authHandler = BasicAuthHandler.create(authProvider, realm);

    private var users: MutableMap<String, AuthInfo> = HashMap()

    fun configureDevelopmentEnvironment(): Map<AuthInfo, IntArray/*[balance,minAllowedBalance]*/> {
        // TODO:(0) This is a temporal "patch". Ideally functional-test will recreate the accounts
        //   through HTTP-requests.
        if (!Config.unitTestsActive) {
            throw RuntimeException("developer.unitTestsActive must be true @ application.conf " + "to be able to reset tests")
        }
        val result = HashMap<AuthInfo, IntArray/*balance*/>()
        val admin = AuthInfo(Config.test_ethereum_address_admin, "admin", "admin", AccessRoll.ADMIN)
        val ilpconnector = AuthInfo(Config.test_ethereum_address_ilpconnector, "ilpconnector", "ilpconnector", AccessRoll.CONNECTOR)
        val alice = AuthInfo(Config.test_ethereum_address_alice, "alice", "alice", AccessRoll.USER)
        val bob = AuthInfo(Config.test_ethereum_address_bob, "bob", "bob", AccessRoll.USER)
        // AuthInfo noBalance    = new AuthInfo(    "nobalance",    "nobalance",    "nobalance", "user");
        val eve = AuthInfo(Config.test_ethereum_address_eve, "eve", "eve", AccessRoll.USER)
        users = HashMap<String, AuthInfo>()
        users.put(admin.login, admin)
        result.put(admin, intArrayOf(10000,-1000000000))
        users.put(ilpconnector.login, ilpconnector)
        result.put(ilpconnector, intArrayOf(100,0))
        users.put(alice.login, alice)
        result.put(alice, intArrayOf(100,0))
        users.put(bob.login, bob)
        result.put(bob, intArrayOf(0,0))
        // users.put("nobalance"   , noBalance   ); result.put(noBalance   ,     0);
        return result
    }

    fun getUsers(): Map<String, AuthInfo> {
        return users
    }

    fun setUser(ai: AuthInfo) {
        users.put(ai.login, ai)
    }

    @JvmOverloads fun authenticate(context: RoutingContext, allowAnonymous: Boolean = false): AuthInfo {

            val request = context.request()

            //  "GET /websocket?token=eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJodHRwOi8vMTcyLjE3LjAuMTozMDAxLy9hY2NvdW50cy9hZG1pbiIsImlzcyI6IjE3Mi4xNy4wLjEifQ.yt95JiNCOzwn80MVP25KfXNhyHfxZmiclwCFATSN7wVNm3ODazPmaEqf8TLkvkiDJoHM49LqvDzgvzbKe_rSxw
            val token = request.getParam("token")
            if (token != null) {// REF: ./services/auth.js
                if (!parser.isSigned(token)) {
                    throw ILPExceptionSupport.createILPUnauthorizedException()
                }
                val URLAccount = parser.parseClaimsJws(token).body.subject
                val offset = URLAccount.lastIndexOf("/accounts/")
                val suser = URLAccount.substring(offset + "/accounts/".length)
                val authInfo = users[suser]
                if (authInfo == null) {
                    log.error("authInfo null. (User not int AuthManager.users lists)")
                    throw ILPExceptionSupport.createILPUnauthorizedException()
                }
                return authInfo
            }
            val authorization : String
            try {
                authorization = request.headers().get(HttpHeaders.AUTHORIZATION)
            }catch(e : Exception){
                if (allowAnonymous) return AuthInfo.ANONYMOUS
                throw ILPExceptionSupport.createILPUnauthorizedException()
            }
            if (authorization == null) {
                if (allowAnonymous) return AuthInfo.ANONYMOUS
                else throw ILPExceptionSupport.createILPUnauthorizedException()
            }
            val parts = authorization.split(" ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            val sscheme = parts[0]
            if ("Basic" != sscheme) {
                log.error("Only Basic Authorization support supported.")
                throw ILPExceptionSupport.createILPUnauthorizedException()
            }
            val decoded = String(Base64.getDecoder().decode(parts[1]))
            val colonIdx = decoded.indexOf(":")
            val suser = if (colonIdx != -1) decoded.substring(0, colonIdx) else decoded
            val spass = if (colonIdx != -1) decoded.substring(colonIdx + 1) else null

            val authInfo = users[suser]
            if (authInfo == null) {
                log.error("users[" + suser + "] null. (User not int AuthManager.users lists)")
                throw ILPExceptionSupport.createILPUnauthorizedException()
            }
            if (!authInfo.isAdmin && authInfo.pass != spass) {
                log.error("user " + authInfo.id + " is not admin and pass doesn't match")
                throw ILPExceptionSupport.createILPUnauthorizedException()
            }
            return authInfo
    }

}/*do not allow anonymous access */