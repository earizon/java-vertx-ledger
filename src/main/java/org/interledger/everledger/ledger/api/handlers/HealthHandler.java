package org.interledger.ilp.ledger.api.handlers;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.ext.web.RoutingContext;

import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.HEAD;

import org.interledger.ilp.common.api.handlers.EndpointHandler;
import org.interledger.ilp.common.api.handlers.RestEndpointHandler;

/**
 * Health handler
 *
 * @author mrmx
 */
public class HealthHandler extends RestEndpointHandler {

    public HealthHandler() {
        super("health");
        accept(GET, HEAD);
    }

    public static EndpointHandler create() {
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
