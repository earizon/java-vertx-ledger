package com.everis.everledger.handlers

import com.everis.everledger.Config
import io.netty.handler.codec.http.HttpResponseStatus
import io.vertx.core.http.HttpMethod
import io.vertx.core.json.Json
import io.vertx.ext.web.RoutingContext

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