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
import org.interledger.everledger.impl.SimpleLedgerAccount;
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
public class AccountsHandler extends RestEndpointHandler {

    private static final Logger log = LoggerFactory.getLogger(AccountsHandler.class);
    private final SimpleLedgerAccountManager accountManager = LedgerAccountManagerFactory.getLedgerAccountManagerSingleton();

    private final static String PARAM_NAME = "name";
    private final static String PARAM_BALANCE = "balance";
    private final static String PARAM_MIN_ALLOWED_BALANCE = "minimum_allowed_balance";
    // private final static String PARAM_DISABLED = "is_disabled";

    private AccountsHandler() {
        super(
            new HttpMethod[] {HttpMethod.GET, HttpMethod.PUT},
            new String[] {"accounts/:" + PARAM_NAME}
        );
    }

    public static AccountsHandler create() {
        return new AccountsHandler();
    }

    @Override
    protected void handleGet(RoutingContext context) {
        AuthInfo ai = AuthManager.authenticate(context, true);
        String accountName = getAccountName(context);
        IfaceAccount account = accountManager.getAccountByName(accountName);
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
        String password;
            try{
        String data_id = data.getString("id");
        if (data_id == null) throw new RuntimeException("id no provided");
        password = data.getString("password");
        if (password == null) throw new RuntimeException("password no provided");
        int li = data_id.lastIndexOf('/'); if (li < 0) li = 0;
        if (! accountName.equals(data_id.substring(li)) ) {
            throw ILPExceptionSupport.createILPBadRequestException();
        }
            }catch(Exception e){
        throw ILPExceptionSupport.createILPBadRequestException(e.toString());
            }
        if(exists && !accountName.equalsIgnoreCase(accountName)) {
            throw ILPExceptionSupport.createILPBadRequestException();
        }
        
        log.debug("Put data: {} to account {}", data,accountName);
        Number balance = null;
        Number minAllowedBalance = NumberConversionUtil.toNumber(data.getValue(PARAM_MIN_ALLOWED_BALANCE, 0d));

        if (!exists) {
            accountManager.store(new SimpleLedgerAccount(accountName));
            AuthManager.setUser(accountName, password, "user"/*roll*/ /* TODO:(1) allow user|admin|...*/);
        }
        
        IfaceAccount account = accountManager.getAccountByName(accountName);
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
