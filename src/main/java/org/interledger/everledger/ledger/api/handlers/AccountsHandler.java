package org.interledger.everledger.ledger.api.handlers;

import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

import org.interledger.everledger.common.api.auth.AuthInfo;
import org.interledger.everledger.common.api.auth.AuthManager;
import org.interledger.everledger.common.api.handlers.RestEndpointHandler;
import org.interledger.everledger.common.api.util.ILPExceptionSupport;
import org.interledger.everledger.common.api.util.VertxUtils;
import org.interledger.everledger.ledger.LedgerAccountManagerFactory;
import org.interledger.everledger.ledger.account.IfaceLocalAccountManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Accounts handler
 */
public class AccountsHandler extends RestEndpointHandler {
    private static final Logger log = LoggerFactory.getLogger(AccountsHandler.class);

    private AccountsHandler() {
        super(new HttpMethod[]{HttpMethod.GET}, new String[] {"accounts"});
    }

    public static AccountsHandler create() {
        log.info("AccountsHandler created");
        return new AccountsHandler();
    }

    @Override
    protected void handleGet(RoutingContext context) {
        AuthInfo ai = AuthManager.authenticate(context);
        if (!ai.isAdmin()) {
            throw ILPExceptionSupport.createILPForbiddenException();
        }
        JsonObject request = VertxUtils.getBodyAsJson(context);
        int page = request.getInteger("page", 1);
        int pageSize = request.getInteger("pageSize", 10);
        IfaceLocalAccountManager accountManager = LedgerAccountManagerFactory.getLedgerAccountManagerSingleton();
        context.response()
                .putHeader("content-type", "application/json; charset=utf-8") //TODO create decorator
                .end(Json.encode(accountManager.getAccounts(page, pageSize)));
    }

}
