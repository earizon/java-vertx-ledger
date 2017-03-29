package org.interledger.ilp.ledger.api.handlers;

import io.netty.handler.codec.http.HttpResponseStatus;
import static io.vertx.core.http.HttpMethod.*;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import org.apache.commons.lang3.StringUtils;
import org.interledger.ilp.common.api.ProtectedResource;
import org.interledger.ilp.common.api.auth.AuthInfo;
import org.interledger.ilp.common.api.auth.AuthManager;
import org.interledger.ilp.common.api.auth.RoleUser;
import org.interledger.ilp.core.InterledgerException;
import org.interledger.ilp.core.ledger.model.LedgerInfo;
import org.interledger.ilp.common.api.handlers.RestEndpointHandler;
import org.interledger.ilp.common.api.util.JsonObjectBuilder;
import org.interledger.ilp.common.util.NumberConversionUtil;
import org.interledger.ilp.ledger.LedgerAccountManagerFactory;
import org.interledger.ilp.ledger.LedgerFactory;
import org.interledger.ilp.ledger.account.LedgerAccount;
import org.interledger.ilp.ledger.account.LedgerAccountManager;
import org.interledger.ilp.ledger.impl.simple.SimpleLedgerAccount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Single Account handler
 *
 * @author mrmx
 */
public class AccountHandler extends RestEndpointHandler  implements ProtectedResource {

    private static final Logger log = LoggerFactory.getLogger(AccountHandler.class);

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
        AuthManager.getInstance().authenticate(context, res -> {
            AuthInfo authInfo = res.result();
            if (res.succeeded()) {
                handleAuthorizedGet(context, authInfo);
            } else if (authInfo.isEmpty()) {
                String accountName = getAccountName(context);
                if (StringUtils.isNotBlank(accountName)) {
                    handleAuthorizedGet(context, null);
                } else {
                    throw new InterledgerException(InterledgerException.RegisteredException.BadRequestError, "Required param " + PARAM_NAME);
                }
            } else {
                throw new InterledgerException(InterledgerException.RegisteredException.ForbiddenError);
            }
        });
    }

    @Override
    protected void handlePut(RoutingContext context) {
        log.debug("Handing put account");

        AuthInfo authInfo = AuthManager.getInstance().getAuthInfo(context);
        if (authInfo.isEmpty()) {
            throw new InterledgerException(InterledgerException.RegisteredException.UnauthorizedError, AuthManager.DEFAULT_BASIC_REALM);
        }
        RoleUser user = AuthManager.getInstance().getAuthUser(authInfo);
        log.debug("put with user {}", user);
        if (user == null || !user.hasRole(RoleUser.ROLE_ADMIN)) {
            throw new InterledgerException(InterledgerException.RegisteredException.ForbiddenError);
        }
        LedgerInfo ledgerInfo = LedgerFactory.getDefaultLedger().getInfo();
        String accountName = getAccountName(context);
        LedgerAccountManager accountManager = LedgerAccountManagerFactory.getLedgerAccountManagerSingleton();
        boolean exists = accountManager.hasAccount(accountName);
        JsonObject data = getBodyAsJson(context);
        if(exists && !accountName.equalsIgnoreCase(data.getString(PARAM_NAME))) {
            throw new InterledgerException(InterledgerException.RegisteredException.BadRequestError, accountName);
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
        log.debug("Put account {} balance: {}{}", accountName, balance, ledgerInfo.getCurrencyCode());        
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

    private void handleAuthorizedGet(RoutingContext context, AuthInfo authInfo) {
        log.debug("handleAuthorized {}", authInfo);
        LedgerAccount account = getAccountByName(context);
        JsonObject result;
        if (authInfo == null) {
            result = accountToJsonObject(account, false);
        } else {
            RoleUser user = AuthManager.getInstance().getAuthUser(authInfo);
            result = accountToJsonObject(account, user.hasRole(RoleUser.ROLE_ADMIN));
        }
        response(context, HttpResponseStatus.OK, result);
    }

    private JsonObject accountToJsonObject(LedgerAccount account, boolean isAdmin) {
        isAdmin = true; // FIXME deleteme
        String ledger = LedgerFactory.getDefaultLedger().getInfo().getBaseUri();
        if (ledger.endsWith("/")) { ledger = ledger.substring(0, ledger.length()-1); }
        JsonObjectBuilder build = JsonObjectBuilder.create()
                .put("id", account.getUri())
                .put("name", account.getName())
                .put("ledger", ledger);
        if (isAdmin){ 
            build
                .put("balance", account.getBalanceAsString())
                .put("connector", "localhost:4000" /* FIXME Hardcoded*/)
                .put("is_disabled", false /* FIXME Hardcoded*/)
                .put("minimum_allowed_balance", "0" /* FIXME Hardcoded*/)
                .put("id", account.getUri());
        }

        return build.get();
    }
}
