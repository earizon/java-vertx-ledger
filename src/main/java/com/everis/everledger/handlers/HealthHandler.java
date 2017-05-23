package com.everis.everledger.handlers;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.RoutingContext;

import com.everis.everledger.handlers.RestEndpointHandler;

/**
 * Health handler
 *
 * @author mrmx
 */
public class HealthHandler extends RestEndpointHandler {

    public HealthHandler() {
        super(
                new HttpMethod[] {HttpMethod.GET, HttpMethod.HEAD},
                new String[] {"health"}
            );
    }

    public static RestEndpointHandler create() {
        return new HealthHandler();
    }

    @Override
    protected void handleGet(RoutingContext context) {
        response(context,HttpResponseStatus.OK,buildJSONWith("status","OK"));
    }

    @Override
    protected void handleHead(RoutingContext context) {
        response(context,HttpResponseStatus.OK,buildJSONWith("status","OK"));
    }

}
