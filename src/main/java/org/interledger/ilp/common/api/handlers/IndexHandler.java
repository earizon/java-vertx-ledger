package org.interledger.ilp.common.api.handlers;

import io.vertx.core.Handler;
import io.vertx.ext.web.RoutingContext;
import java.util.HashMap;
import java.util.Map;
import org.interledger.ilp.common.api.core.JsonResultHandler;

/**
 * Handler for the index route.
 *
 * @author mrmx
 */
public class IndexHandler implements Handler<RoutingContext> {
    private final Map<String, Object> index;
    private final JsonResultHandler handler;

    IndexHandler() {
        index = new HashMap<>();
        handler = new JsonResultHandler(index);
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
        handler.handle(context);
    }

}
