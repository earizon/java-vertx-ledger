package org.interledger.everledger.handlers;



import io.vertx.core.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

import org.apache.commons.lang3.StringUtils;
import org.interledger.everledger.AuthInfo;
import org.interledger.everledger.Config;
import org.interledger.everledger.LedgerAccountManagerFactory;
import org.interledger.everledger.handlers.RestEndpointHandler;
import org.interledger.everledger.ifaces.account.IfaceAccount;
import org.interledger.everledger.impl.manager.SimpleLedgerAccountManager;
import org.interledger.everledger.util.AuthManager;
import org.interledger.everledger.util.ILPExceptionSupport;
import org.interledger.everledger.util.JsonObjectBuilder;
import org.interledger.everledger.util.NumberConversionUtil;
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
    // private final static String PARAM_DISABLED = "is_disabled";

    private AccountHandler() {
        super(
            new HttpMethod[] {HttpMethod.GET, HttpMethod.PUT},
            new String[] {"accounts/:" + PARAM_NAME}
        );
    }

    public static AccountHandler create() {
        return new AccountHandler();
    }

    @Override
    protected void handleGet(RoutingContext context) {
        AuthInfo ai = AuthManager.authenticate(context, true);
        IfaceAccount account = getAccountByName(context);
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
            throw ILPExceptionSupport.createILPForbiddenException();
        }
        log.debug("Put data: {} to account {}", data,accountName);
        Number balance = null;
        Number minAllowedBalance = NumberConversionUtil.toNumber(data.getValue(PARAM_MIN_ALLOWED_BALANCE, 0d));

        IfaceAccount account = exists
                ? accountManager.getAccountByName(accountName)
                : accountManager.create(accountName);
        if(data.containsKey(PARAM_BALANCE)) {
            balance = NumberConversionUtil.toNumber(data.getValue(PARAM_BALANCE));
            account.setBalance(balance);
        }
        account.setMinimumAllowedBalance(minAllowedBalance);
        // if(data.containsKey(PARAM_DISABLED)) {
        //     ((SimpleLedgerAccount)account).setDisabled(data.getBoolean(PARAM_DISABLED, false));
        // }
        log.debug("Put account {} balance: {}{}", accountName, balance, Config.ledgerCurrencyCode);
        accountManager.store(account);
        response(context, exists ? HttpResponseStatus.OK : HttpResponseStatus.CREATED,
                JsonObjectBuilder.create().from(account));
    }

    private IfaceAccount getAccountByName(RoutingContext context) {
        String accountName = getAccountName(context);
        log.debug("Get account {}", accountName);
        return LedgerAccountManagerFactory.getLedgerAccountManagerSingleton().getAccountByName(accountName);
    }

    private String getAccountName(RoutingContext context) {
        String accountName = context.request().getParam(PARAM_NAME);
        if (StringUtils.isBlank(accountName)) {
            throw ILPExceptionSupport.createILPBadRequestException(PARAM_NAME + "not provided");
        }
        return accountName;
    }

    private JsonObject accountToJsonObject(IfaceAccount account, boolean isAdmin) {
        String ledger = Config.publicURL.toString();
        if (ledger.endsWith("/")) { ledger = ledger.substring(0, ledger.length()-1); }
        
        JsonObjectBuilder build = JsonObjectBuilder.create()
                // .put("id", accountManager.getPublicURIForAccount(account))
                .put("id", accountManager.getPublicURIForAccount(account))
                .put("name", account.getLocalName())
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
