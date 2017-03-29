package org.interledger.ilp.common.api.auth.impl;

import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.RoutingContext;
import java.util.Base64;
import org.interledger.ilp.common.api.auth.AuthInfo;
import org.interledger.ilp.common.api.auth.AuthInfoBuilder;

/**
 * Builds an {@code AuthInfo} instance from a basic auth request context.
 *
 * @author mrmx based on code from
 * {@code io.vertx.ext.web.handler.impl.BasicAuthHandlerImpl}
 */
public class BasicAuthInfoBuilder implements AuthInfoBuilder {

    public AuthInfo build(RoutingContext context) {
        AuthInfo authInfo = AuthInfo.basic(null);
        User user = context.user();
        if (user != null) {
            return AuthInfo.basic(user.principal());
        } else {
            HttpServerRequest request = context.request();
            String authorization = request.headers().get(HttpHeaders.AUTHORIZATION);

            if (authorization != null) {
                String suser;
                String spass;
                String sscheme;

                try {
                    String[] parts = authorization.split(" ");
                    sscheme = parts[0];
                    String decoded = new String(Base64.getDecoder().decode(parts[1]));
                    int colonIdx = decoded.indexOf(":");
                    if (colonIdx != -1) {
                        suser = decoded.substring(0, colonIdx);
                        spass = decoded.substring(colonIdx + 1);
                    } else {
                        suser = decoded;
                        spass = null;
                    }
                } catch (ArrayIndexOutOfBoundsException e) {
                    return authInfo;
                } catch (IllegalArgumentException | NullPointerException e) {
                    // IllegalArgumentException includes PatternSyntaxException
                    //context.fail(e);
                    return authInfo;
                }

                if (!"Basic".equals(sscheme)) {
                    //context.fail(400);
                } else {
                    authInfo = AuthInfo.basic(suser, spass);
                }
            }

        }
        return authInfo;
    }
}
