package org.interledger.ilp.common.api.core;

import io.vertx.core.Handler;
import io.vertx.core.json.Json;
import io.vertx.ext.web.RoutingContext;

/**
 *
 * @author mrmx
 */
public final class JsonResultHandler implements Handler<RoutingContext> {

    private final Object value;

    public JsonResultHandler(Object value) {
        this.value = value;
    }

    @Override
    public void handle(RoutingContext context) {
        context.response()
                .putHeader("content-type", "application/json; charset=utf-8")
                .end(Json.encodePrettily(value));
    }

    public static JsonResultHandler toJson(Object value) {
        return new JsonResultHandler(value);
    }

}
