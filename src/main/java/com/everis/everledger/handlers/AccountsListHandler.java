package com.everis.everledger.handlers;

import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.everis.everledger.AuthInfo;
import com.everis.everledger.ifaces.account.IfaceLocalAccountManager;
import com.everis.everledger.util.AuthManager;
import com.everis.everledger.util.ILPExceptionSupport;

import com.everis.everledger.impl.manager.SimpleAccountManager;

/**
 * Accounts handler
 */
public class AccountsListHandler extends RestEndpointHandler {
    private static final Logger log = LoggerFactory.getLogger(AccountsListHandler.class);

    private AccountsListHandler() {
        super(new HttpMethod[]{HttpMethod.GET}, new String[] {"accounts"});
    }

    public static AccountsListHandler create() {
        log.info("AccountsListHandler created");
        return new AccountsListHandler();
    }

    @Override
    protected void handleGet(RoutingContext context) {
        AuthInfo ai = AuthManager.authenticate(context);
        if (!ai.isAdmin()) {
            log.info("fail due to '"+ai.getId()+"' not having admin roll");
            throw ILPExceptionSupport.createILPForbiddenException();
        }
        JsonObject request = getBodyAsJson(context);
        int page = request.getInteger("page", 1);
        int pageSize = request.getInteger("pageSize", 10);
        IfaceLocalAccountManager AM = SimpleAccountManager.INSTANCE;
        context.response()
                .putHeader("content-type", "application/json; charset=utf-8") //TODO create decorator
                .end(Json.encode(AM.getAccounts(page, pageSize)));
    }

}
