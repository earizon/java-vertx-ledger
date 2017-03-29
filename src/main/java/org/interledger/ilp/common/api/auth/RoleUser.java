package org.interledger.ilp.common.api.auth;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Role based {@code AuthUser}
 *
 * @author mrmx
 */
public class RoleUser extends AuthUser {
    
    public static final String ROLE_ADMIN   =   "admin";

    private static final Logger log = LoggerFactory.getLogger(RoleUser.class);

    private final String role;

    public RoleUser(AuthInfo authInfo, String role) {
        super(authInfo);
        if (StringUtils.isBlank(role)) {
            throw new IllegalArgumentException("empty user role for user " + authInfo);
        }
        this.role = role;
    }

    public String getRole() {
        return role;
    }

    public boolean hasRole(String role) {
        return this.role != null && this.role.equalsIgnoreCase(role);
    }

    @Override
    protected void doIsPermitted(String permission, Handler<AsyncResult<Boolean>> resultHandler) {
        if (permission != null && permission.equalsIgnoreCase(role)) {
            resultHandler.handle(Future.succeededFuture(true));
        } else {
            log.debug("User {} has no permission {}", getAuthInfo(), permission);
            resultHandler.handle(Future.failedFuture(permission));
        }
    }

}
