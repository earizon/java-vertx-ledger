package com.everis.everledger.handlers

import com.everis.everledger.Config
import com.everis.everledger.util.AuthManager
import io.jsonwebtoken.Jwts
import io.netty.handler.codec.http.HttpResponseStatus
import io.vertx.core.http.HttpMethod
import io.vertx.core.json.Json
import io.vertx.ext.web.RoutingContext
import org.slf4j.LoggerFactory
import java.net.URISyntaxException
import java.util.HashMap

class IndexHandler private constructor() : RestEndpointHandler(arrayOf(HttpMethod.GET), arrayOf("")) {
    override fun handleGet(context: RoutingContext) =
        context.response()
                .putHeader("content-type", "application/json; charset=utf-8")
                .end(Json.encodePrettily(Config.indexHandlerMap))

    companion object {
        fun create(): RestEndpointHandler = IndexHandler()
    }
}


class HealthHandler : RestEndpointHandler(arrayOf(HttpMethod.GET, HttpMethod.HEAD), arrayOf("health")) {
    override fun handleGet(context: RoutingContext) =
        response(context, HttpResponseStatus.OK, RestEndpointHandler.buildJSONWith("status", "OK"))

    override fun handleHead(context: RoutingContext) =
        response(context, HttpResponseStatus.OK, RestEndpointHandler.buildJSONWith("status", "OK"))

    companion object {
        fun create(): RestEndpointHandler = HealthHandler()
    }
}

class AuthTokenHandler private constructor() : RestEndpointHandler(arrayOf(HttpMethod.GET), arrayOf("auth_token")) {

    public override fun handleGet(context: RoutingContext) {
        val ai = AuthManager.authenticate(context, false)
        /*
         * https://github.com/interledgerjs/five-bells-ledger/blob/master/src/models/authTokens.js
         * jwt.sign({}, config.authTokenSecret, {
         *   algorithm: 'HS256',
         *   subject: uri.make('account', requestingUser.name.toLowerCase()),
         *   issuer: config.server.base_uri,
         *   expiresIn: '7 days'
         * }
         */
        // REF: https://github.com/jwtk/jjwt/
        val urlAccount = Config.publicURL.toString() + "/accounts/" + ai.name.toLowerCase()
        val subject: String
        try {
            subject = java.net.URI(urlAccount).toString()
        } catch (e: URISyntaxException) {
            val sError = "'$urlAccount' can not be parsed as URI"
            log.error(sError)
            throw RuntimeException(sError)
        }

        val compactJwsToken = Jwts.builder()
                .setSubject(subject)
                .setIssuer(Config.serverPublicHost /* config.server.base_uri */)
                .signWith(AuthManager.SigAlgth, AuthManager.key).compact()
        val result = HashMap<String, Any>()
        result.put("token", compactJwsToken)
        context.response()
                .putHeader("content-type", "application/json; charset=utf-8")
                .end(Json.encodePrettily(result))
    }

    companion object {
        private val log = LoggerFactory.getLogger(AuthTokenHandler::class.java)


        fun create(): RestEndpointHandler = AuthTokenHandler()
    }
}
