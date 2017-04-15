package org.interledger.ilp.common.api.util;

import io.vertx.core.json.JsonObject;
import java.util.function.Supplier;

/**
 * Json build helper.
 *
 * @author mrmx
 */
public class JsonBuilder implements Supplier<JsonObject> {
    
    private JsonObject json;
    
    private JsonBuilder() {
        json = new JsonObject();
    }
    
    public static JsonBuilder create() {
        return new JsonBuilder();
    }
    
    @Override
    public JsonObject get() {
        return json;
    }
    
    public JsonBuilder with(Object... pairs) {
        if (pairs.length % 2 != 0) {
            throw new IllegalArgumentException("Argument pairs must be even! "
                    + pairs.length);
        }
        for (int i = 0; i < pairs.length; i += 2) {
            put(pairs[i], pairs[i + 1]);
        }
        return this;
    }
    
    public JsonBuilder put(Object key, Object value) {
        json.put(String.valueOf(key), value);
        return this;
    }    
    
}
