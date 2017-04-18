package org.interledger.everledger.common.api.handlers;

import io.vertx.core.Handler;
import io.vertx.core.json.Json;
import io.vertx.ext.web.RoutingContext;

import java.util.HashMap;
import java.util.Map;

/**
 * Handler for the index route.
 *
 * @author mrmx
 */
public class IndexHandler implements Handler<RoutingContext> {
    private final Map<String, Object> index;

    IndexHandler() {
        index = new HashMap<>();
    }

    public static IndexHandler create() {
        return new IndexHandler();
    }

    public IndexHandler put(String key, Object value) {
        index.put(key, value);
        return this;
    }

    @Override
    public void handle(RoutingContext context) {
        context.response()
        .putHeader("content-type", "application/json; charset=utf-8")
        .end(Json.encodePrettily(index));
    }

}
