package org.interledger.ilp.common.api.handlers;

import io.vertx.core.Handler;
import io.vertx.core.MultiMap;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.Json;
import io.vertx.ext.web.RoutingContext;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dumps reqeust headers for debugging purposes.
 * 
 * @author mrmx
 */
public class DebugRequestHandler implements Handler<RoutingContext> {
    private static final Logger log = LoggerFactory.getLogger(DebugRequestHandler.class);

    @Override
    public void handle(RoutingContext context) {
        HttpServerRequest request = context.request();
        MultiMap headers = request.headers();
        Map<String,String> headerMap = new HashMap<>(headers.size());
        for(Entry<String,String> header : headers) {
            headerMap.put(header.getKey(), header.getValue());
        }                
        log.debug("headers\n{}",Json.encodePrettily(headerMap));        
        if(!request.params().isEmpty()) {
            log.debug("params\n{}",Json.encodePrettily(request.params().entries()));
        }
        context.next();
    }

}
