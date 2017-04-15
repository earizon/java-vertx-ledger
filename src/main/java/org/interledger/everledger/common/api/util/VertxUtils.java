package org.interledger.ilp.common.api.util;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.apache.commons.lang3.StringUtils;

/**
 * Various Vertx util methods.
 *
 * @author mrmx
 */
public final class VertxUtils {

    private VertxUtils() {

    }

    public static JsonObject getBodyAsJson(RoutingContext context) {
        String bodyAsString = context.getBodyAsString();
        if (StringUtils.isBlank(bodyAsString)) {
            return new JsonObject();
        }
        return new JsonObject(bodyAsString);
    }
}
