package com.everis.everledger.handlers;

import io.vertx.core.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.everis.everledger.AccountManagerFactory;
import com.everis.everledger.AuthInfo;
import com.everis.everledger.Config;
import com.everis.everledger.ifaces.account.IfaceAccount;
import com.everis.everledger.impl.manager.SimpleAccountManager;
import com.everis.everledger.util.AuthManager;
import com.everis.everledger.util.ILPExceptionSupport;
import com.everis.everledger.util.JsonObjectBuilder;
import com.everis.everledger.util.ConversionUtil;
import org.javamoney.moneta.Money;

import com.everis.everledger.impl.SimpleAccount;

/**
 * Single Account handler
 *
 */
public class AccountsHandler extends RestEndpointHandler {

    private static final Logger log = LoggerFactory.getLogger(AccountsHandler.class);
    private final SimpleAccountManager accountManager = AccountManagerFactory.getLedgerAccountManagerSingleton();

    private final static String PARAM_NAME = "name";
    private final static String PARAM_BALANCE = "balance";
    private final static String PARAM_MIN_ALLOWED_BALANCE = "minimum_allowed_balance";
    // private final static String PARAM_DISABLED = "is_disabled";

    private AccountsHandler() {
        super(
            new HttpMethod[] {HttpMethod.GET, HttpMethod.PUT, HttpMethod.POST},
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
        boolean isAuthenticated = ai.getRoll().equals("admin") || ai.getId().equals(accountName); 
        IfaceAccount account = accountManager.getAccountByName(accountName);
        JsonObject result = accountToJsonObject(account, isAuthenticated);
        response(context, HttpResponseStatus.OK, result);
    }

    @Override
    protected void handlePut(RoutingContext context) {
        log.debug("Handing put account");
        AuthManager.authenticate(context);
        String accountName = getAccountName(context);
        boolean exists = accountManager.hasAccount(accountName);
        JsonObject data = getBodyAsJson(context);
        String password; String data_id;
        data_id = data.getString("id");
        if (data_id == null) data_id = data.getString("name"); // Second chance
        if (data_id == null) {
            throw ILPExceptionSupport.createILPBadRequestException("id no provided");
        }
        password = data.getString("password");
        if (password == null && exists == true) {
            password = AuthManager.getUsers().get(accountName).pass;
        }
        if (password == null) {
            throw ILPExceptionSupport.createILPBadRequestException("password no provided for id:"+data_id);
        }
        int li = data_id.lastIndexOf('/'); if (li < 0) li = -1;
        data_id = data_id.substring(li+1); 
        if (! accountName.equals(data_id) ) {
            throw ILPExceptionSupport.createILPBadRequestException(
                "id in body '"+data_id+"'doesn't match account name '"+accountName+"' in URL");
        }
        if(exists && !accountName.equalsIgnoreCase(accountName)) {
            throw ILPExceptionSupport.createILPBadRequestException();
        }
        
        log.debug("Put data: {} to account {}", data,accountName);
        String sMinAllowVal = data.getString(PARAM_MIN_ALLOWED_BALANCE);
        if (sMinAllowVal==null) sMinAllowVal = "0"; // TODO:(1) Arbitrary value. Use Config....
        sMinAllowVal = sMinAllowVal.toLowerCase().replace("infinity", "1000000000000000"); // TODO:(0) Arbitrary value. Use Config...
        Number minAllowedBalance = ConversionUtil.toNumber(sMinAllowVal);

        Number balance = (data.containsKey(PARAM_BALANCE))
            ? ConversionUtil.toNumber(data.getValue(PARAM_BALANCE))
            : ConversionUtil.toNumber(data.getValue(PARAM_BALANCE));

        IfaceAccount account = new SimpleAccount(
                accountName,
                Money.of( balance, Config.ledgerCurrencyCode),
                Money.of( minAllowedBalance, Config.ledgerCurrencyCode),
                false
        );

        accountManager.store(account, true /*update if already exists*/);
        AuthManager.setUser(accountName, password, "user"/*roll*/ /* TODO:(1) allow user|admin|...*/);

        // if(data.containsKey(PARAM_DISABLED)) {
        //     ((SimpleLedgerAccount)account).setDisabled(data.getBoolean(PARAM_DISABLED, false));
        // }
        response(context, exists ? HttpResponseStatus.OK : HttpResponseStatus.CREATED,
                JsonObjectBuilder.create().from(account));
    }

    @Override
    protected void handlePost(RoutingContext context) {
        handlePut(context);
    }

    private String getAccountName(RoutingContext context) {
        String accountName = context.request().getParam(PARAM_NAME);
        if (StringUtils.isBlank(accountName)) {
            throw ILPExceptionSupport.createILPBadRequestException(PARAM_NAME + "not provided");
        }
        return accountName;
    }

    private JsonObject accountToJsonObject(IfaceAccount account, boolean isAuthenticated) {
        String ledger = Config.publicURL.toString();
        if (ledger.endsWith("/")) { ledger = ledger.substring(0, ledger.length()-1); }
        
        JsonObjectBuilder build = JsonObjectBuilder.create()
                .put("id", account.getId())
                .put("name", account.getLocalID())
                .put("ledger", ledger);
        if (isAuthenticated){
            build
                .put("balance", account.getBalanceAsString())
                // .put("connector", "??????" /* TODO:(?) Recheck */)
                .put("is_disabled", account.isDisabled())
                .put("minimum_allowed_balance", account.getILPMinimumAllowedBalance().getNumber().toString());
        }

        return build.get();
    }
}
