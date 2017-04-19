package org.interledger.everledger.common.api.handlers;

import io.vertx.core.Handler;
import io.vertx.core.json.Json;
import io.vertx.ext.web.RoutingContext;

import org.interledger.everledger.common.config.Config;

/**
 * Handler for the index route.
 *
 * @author mrmx
 */
public class IndexHandler implements Handler<RoutingContext> {

    // public IndexHandler() {}

    @Override
    public void handle(RoutingContext context) {
        context.response()
        .putHeader("content-type", "application/json; charset=utf-8")
        .end(Json.encodePrettily(Config.indexHandlerMap));
    }

}
