package org.interledger.ilp.common.api.util;

import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 *  {@code JsonObject} build helper.
 *
 * @author mrmx
 */
public class JsonObjectBuilder implements Supplier<JsonObject> {

    private Map<String, Object> beanMap;

    private JsonObjectBuilder() {
        beanMap = new HashMap<>();
    }

    public static JsonObjectBuilder create() {
        return new JsonObjectBuilder();
    }

    @Override
    public JsonObject get() {
        return new JsonObject(beanMap);
    }

    public JsonObjectBuilder from(Object value) {
        beanMap = Json.mapper.convertValue(value, Map.class);
        return this;
    }

    public JsonObjectBuilder with(Object... pairs) {
        if (pairs.length % 2 != 0) {
            throw new IllegalArgumentException("Argument pairs must be even! "
                    + pairs.length);
        }
        for (int i = 0; i < pairs.length; i += 2) {
            put(pairs[i], pairs[i + 1]);
        }
        return this;
    }

    public JsonObjectBuilder put(Object key, Object value) {
        beanMap.put(String.valueOf(key), value);
        return this;
    }

}
