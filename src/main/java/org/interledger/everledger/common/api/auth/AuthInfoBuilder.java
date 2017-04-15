package org.interledger.everledger.common.api.auth;

import io.vertx.ext.web.RoutingContext;

public interface AuthInfoBuilder {

    public AuthInfo build(RoutingContext context);
}
