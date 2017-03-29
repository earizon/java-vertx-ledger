package org.interledger.ilp.common.api.auth;

import io.vertx.ext.web.RoutingContext;

/**
 * Auth info builder
 *
 * @author mrmx
 */
public interface AuthInfoBuilder {

    public AuthInfo build(RoutingContext context);
}
