package org.interledger.everledger.ledger.api.handlers;

import io.netty.handler.codec.http.HttpResponseStatus;

import static io.vertx.core.http.HttpMethod.*;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;


import org.apache.commons.lang3.StringUtils;
import org.interledger.everledger.common.api.auth.AuthInfo;
import org.interledger.everledger.common.api.auth.AuthManager;
import org.interledger.everledger.common.api.handlers.RestEndpointHandler;
import org.interledger.everledger.common.api.util.ILPExceptionSupport;
import org.interledger.everledger.common.api.util.JsonObjectBuilder;
import org.interledger.everledger.common.config.Config;
import org.interledger.everledger.common.util.NumberConversionUtil;
import org.interledger.everledger.ledger.LedgerAccountManagerFactory;
import org.interledger.everledger.ledger.account.LedgerAccount;
import org.interledger.everledger.ledger.impl.simple.SimpleLedgerAccount;
import org.interledger.everledger.ledger.impl.simple.SimpleLedgerAccountManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Single Account handler
 *
 */
public class AccountHandler extends RestEndpointHandler {

    private static final Logger log = LoggerFactory.getLogger(AccountHandler.class);
    private final SimpleLedgerAccountManager accountManager = LedgerAccountManagerFactory.getLedgerAccountManagerSingleton();


    private final static String PARAM_NAME = "name";
    private final static String PARAM_BALANCE = "balance";
    private final static String PARAM_MIN_ALLOWED_BALANCE = "minimum_allowed_balance";
    private final static String PARAM_DISABLED = "is_disabled";

    public AccountHandler() {
        super("account","accounts/:" + PARAM_NAME);
        accept(GET, PUT);
    }

    public static AccountHandler create() {
        return new AccountHandler();
    }

    @Override
    protected void handleGet(RoutingContext context) {
        AuthInfo ai = AuthManager.authenticate(context);
        LedgerAccount account = getAccountByName(context);
        JsonObject result = accountToJsonObject(account, ai.isAdmin());
        response(context, HttpResponseStatus.OK, result);
    }

    @Override
    protected void handlePut(RoutingContext context) {
        log.debug("Handing put account");
        AuthManager.authenticate(context);
        String accountName = getAccountName(context);
        boolean exists = accountManager.hasAccount(accountName);
        JsonObject data = getBodyAsJson(context);
        if(exists && !accountName.equalsIgnoreCase(data.getString(PARAM_NAME))) {
            ILPExceptionSupport.launchILPForbiddenException();
        }
        log.debug("Put data: {} to account {}", data,accountName);
        Number balance = null;
        Number minAllowedBalance = NumberConversionUtil.toNumber(data.getValue(PARAM_MIN_ALLOWED_BALANCE, 0d));

        LedgerAccount account = exists
                ? accountManager.getAccountByName(accountName)
                : accountManager.create(accountName);
        if(data.containsKey(PARAM_BALANCE)) {
            balance = NumberConversionUtil.toNumber(data.getValue(PARAM_BALANCE));
            account.setBalance(balance);
        }        
        account.setMinimumAllowedBalance(minAllowedBalance);
        if(data.containsKey(PARAM_DISABLED)) {
            ((SimpleLedgerAccount)account).setDisabled(data.getBoolean(PARAM_DISABLED, false));
        }
        log.debug("Put account {} balance: {}{}", accountName, balance, Config.ledgerCurrencyCode);
        accountManager.store(account);
        response(context, exists ? HttpResponseStatus.OK : HttpResponseStatus.CREATED,
                JsonObjectBuilder.create().from(account));
    }

    private LedgerAccount getAccountByName(RoutingContext context) {
        String accountName = getAccountNameOrThrowException(context);
        log.debug("Get account {}", accountName);
        return LedgerAccountManagerFactory.getLedgerAccountManagerSingleton().getAccountByName(accountName);
    }

    private String getAccountName(RoutingContext context) {
        String accountName = context.request().getParam(PARAM_NAME);
        return accountName == null ? null : accountName.trim().toLowerCase();
    }

    private String getAccountNameOrThrowException(RoutingContext context) {
        String accountName = getAccountName(context);
        if (StringUtils.isBlank(accountName)) {
            throw new RestEndpointException(HttpResponseStatus.BAD_REQUEST, accountName);
        }
        return accountName;
    }



    private JsonObject accountToJsonObject(LedgerAccount account, boolean isAdmin) {
        String ledger = Config.publicURL.toString();
        if (ledger.endsWith("/")) { ledger = ledger.substring(0, ledger.length()-1); }
        
        JsonObjectBuilder build = JsonObjectBuilder.create()
                .put("id", accountManager.getPublicURIForAccount(account))
                .put("name", account.getName())
                .put("ledger", ledger);
        if (isAdmin){ 
            build
                .put("balance", account.getBalanceAsString())
                // .put("connector", "??????" /* TODO:(?) Recheck */)
                .put("is_disabled", account.isDisabled())
                .put("minimum_allowed_balance", account.getMinimumAllowedBalance().getNumber().toString());
        }

        return build.get();
    }
}
