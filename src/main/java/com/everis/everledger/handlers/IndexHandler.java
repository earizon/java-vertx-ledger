package com.everis.everledger.handlers;

import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.Json;
import io.vertx.ext.web.RoutingContext;

import com.everis.everledger.Config;
import com.everis.everledger.handlers.RestEndpointHandler;

/**
 * Handler for the index route.
 *
 * @author mrmx
 */
public class IndexHandler extends RestEndpointHandler {

    private IndexHandler() {
        super(new HttpMethod[]{HttpMethod.GET}, new String[] {""});
    }

    public static RestEndpointHandler create() {
        return new IndexHandler();
    }

    @Override
    public void handleGet(RoutingContext context) {
        context.response()
        .putHeader("content-type", "application/json; charset=utf-8")
        .end(Json.encodePrettily(Config.indexHandlerMap));
    }

}