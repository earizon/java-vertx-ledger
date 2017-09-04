package com.everis.everledger.handlers

import com.everis.everledger.util.Config
import com.everis.everledger.util.HTTPInterledgerException
import com.everis.everledger.impl.manager.SimpleAccountManager
import com.everis.everledger.impl.manager.SimpleTransferManager
import com.everis.everledger.util.AuthManager
import com.everis.everledger.util.ILPExceptionSupport
import com.everis.everledger.util.JsonObjectBuilder
import io.jsonwebtoken.Jwts
import io.netty.handler.codec.http.HttpResponseStatus
import io.vertx.core.Handler
import io.vertx.core.http.HttpHeaders
import io.vertx.core.http.HttpMethod
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.RoutingContext
import org.apache.commons.lang3.StringUtils
import org.slf4j.LoggerFactory
import java.io.PrintWriter
import java.io.StringWriter
import java.net.MalformedURLException
import java.net.URISyntaxException
import java.net.URL
import java.util.*
import java.util.function.Supplier


private fun buildJSON(id: CharSequence, message: CharSequence): Supplier<JsonObject> {
    // TODO:(0) move to JSONSupport (Do not inherit)
    return buildJSONWith("id", id, "message", message)
}

private fun buildJSONWith(vararg pairs: Any): Supplier<JsonObject> {
    // TODO:(0) move to JSONSupport (Do not inherit)
    return JsonObjectBuilder.create().with(*pairs)
}
/**
 * @startuml
 * * title RestEndpointHandler REST sequence
 * * participant "rest client" as c
 * * participant "RestEndpointHandler\nchild" as h
 * * participant "Controller" as k
 * * c -> h : http request
 * * h -> h : parse & check \n @tainted input
 * * h -> k : @untainted input
 * * k -> k : apply bussiness logic
 * * k -> h : result | error
 * * h -> h : convert to ILP \n format result|error
 * * h -> c : result
 * *
 * @enduml
 */
abstract class RestEndpointHandler(httpMethods: Array<HttpMethod>, uriList: Array<String>) : Handler<RoutingContext>
{
    val routePaths: List<String>
    val httpMethods: List<HttpMethod>
    init {
        this.httpMethods = Arrays.asList(*httpMethods)
        this.routePaths = handlerPath(*uriList)
    }

    override fun handle(context: RoutingContext) {
        val handlerName = javaClass.name
        log.debug("In handler {}", handlerName)
        log.debug("context.request().method():" + context.request().method())
        try {
            try {
                when (context.request().method()) {
                    HttpMethod.HEAD -> handleHead(context)
                    HttpMethod.GET  -> handleGet(context)
                    HttpMethod.POST -> {
                        log.info ("POST BODY:"+context.body)
                        handlePost(context)
                    }
                    HttpMethod.PUT  -> {
                        log.info ("PUT  BODY:"+context.body)
                        handlePut(context)
                    }
                    else // CONNECT, DELETE, HEAD, OPTIONS, OTHER, PATCH, TRACE:
                    -> {}
                }
            } catch (t: Exception) {
                // TODO:(?) Improve logging
                val writer = StringWriter()
                val printWriter = PrintWriter(writer)
                t.printStackTrace(printWriter)
                printWriter.flush()
                log.warn("Captured exception { \n" + writer.toString() + "\n}")
                // exceptions already classified as Interledger Exceptions are passed to next "try"
                if (t is HTTPInterledgerException) throw t // ILP contemplated exception
                // Un-captured exception will be treated as internal errors
                // (Ideally, code would never reach this point)
                throw ILPExceptionSupport.createILPInternalException(
                    """
                    |Java UnhandledException {
                    |  Description    :  $t
                    |}
                    """.trimMargin())
            }

        } catch (ex: HTTPInterledgerException) {
            log.error("request body for captured exception:" + context.body.toString())
            val err = ex.interledgerError
            log.error(err.errCode.toString() + " -> " + err.errorType + " \n" + err.data)
            println(err.errCode.toString() + " -> " + err.errorType + " \n" + err.data)

            response( context, HttpResponseStatus.valueOf(ex.httpErrorCode),
                    buildJSONWith( "errCode", err.errCode.code,
                      "triggeredBy", err.triggeredBy.value.toString(), "data", err.data) )
        }

    }

    protected open fun handleGet(context: RoutingContext) {
        throw ILPExceptionSupport.createILPInternalException("Not implemented")
    }

    protected open fun handleHead(context: RoutingContext) {
        throw ILPExceptionSupport.createILPInternalException("Not implemented")
    }

    protected open fun handlePost(context: RoutingContext) {
        throw ILPExceptionSupport.createILPInternalException("Not implemented")
    }

    protected open fun handlePut(context: RoutingContext) {
        throw ILPExceptionSupport.createILPInternalException("Not implemented")
    }

    protected fun response(context: RoutingContext, responseStatus: Int, reasonPhrase: String, t: Throwable?) {
        log.debug("Response error", t)
        response(context, HttpResponseStatus.valueOf(responseStatus),
                buildJSON(reasonPhrase, if (t == null) reasonPhrase else t.message!! ) )
    }

    protected fun response(context: RoutingContext, responseStatus: HttpResponseStatus, response: Supplier<JsonObject>) {
        response(context, responseStatus, response.get())
    }

    protected fun response(context: RoutingContext, responseStatus: HttpResponseStatus, response: JsonObject) {
        val plainEncoding = StringUtils.isNotBlank(context.request().getParam(PARAM_ENCODE_PLAIN_JSON))
        val jsonResponse = if (plainEncoding) response.encode() else response.encodePrettily()
        log.debug("response:\n{}", jsonResponse)
        context.response()
                .putHeader(HttpHeaders.CONTENT_TYPE, MIME_JSON_WITH_ENCODING)
                .putHeader(HttpHeaders.CONTENT_LENGTH, jsonResponse.length.toString())
                .setStatusCode(responseStatus.code())
                .end(jsonResponse)
        if (responseStatus.code() >= HttpResponseStatus.BAD_REQUEST.code()) {
            context.fail(responseStatus.code())
        }
    }

    override fun toString() = this.javaClass.name

    companion object {

        private val log = LoggerFactory.getLogger(RestEndpointHandler::class.java)

        private val PARAM_ENCODE_PLAIN_JSON = "plainjson"
        private val MIME_JSON_WITH_ENCODING = "application/json; charset=utf-8"

        private fun __paths(parent: String, vararg childs: String): String {
            val path = StringBuilder()
            if ("/" != parent) { path.append(parent) }
            for (child in childs) { path.append("/").append(child) }
            return path.toString()
        }

        fun getBodyAsJson(context: RoutingContext): JsonObject {
            val bodyAsString = context.bodyAsString
            if (StringUtils.isBlank(bodyAsString)) {
                return JsonObject()
            }
            return JsonObject(bodyAsString)
        }

        private fun handlerPath(vararg uriList: String): List<String> {
            val result = LinkedList<String>()
            for (uri in uriList) {
                val url = URL(Config.publicURL, __paths(Config.ledgerPathPrefix, uri))
                result.add(url.path)
            }
            return result
        }

    }
}

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
        response(context, HttpResponseStatus.OK, buildJSONWith("status", "OK"))

    override fun handleHead(context: RoutingContext) =
        response(context, HttpResponseStatus.OK, buildJSONWith("status", "OK"))

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
        val urlAccount = Config.publicURL.toString() + "/accounts/" + ai.id.toLowerCase()
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
                .setIssuer(Config.serverPublicHost
                        + if (Config.serverPublicPort.isEmpty()) "" else Config.serverPublicPort)
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

class DeveloperTestingRequestHandler : RestEndpointHandler(arrayOf(HttpMethod.GET), arrayOf("developerTesting/reset")) {
    private val TM = SimpleTransferManager

    override fun handle(context: RoutingContext) {
        log.info("reseting ...")
        TM.developerTestingResetTransfers()
        SimpleAccountManager.developerTestingReset()
        response(context, HttpResponseStatus.OK, buildJSON("", ""))
    }

    companion object {
        private val log = LoggerFactory.getLogger(DeveloperTestingRequestHandler::class.java)
        fun create(): DeveloperTestingRequestHandler = DeveloperTestingRequestHandler()
    }

}// REF:
// https://github.com/interledgerjs/five-bells-ledger/blob/master/src/lib/app.js


