package org.interledger.everledger.ledger.api;

import io.vertx.ext.web.Router;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.interledger.everledger.common.api.AbstractMainEntrypointVerticle;
import org.interledger.everledger.common.api.auth.AuthInfo;
import org.interledger.everledger.common.api.auth.AuthManager;
import org.interledger.everledger.common.api.handlers.EndpointHandler;
import org.interledger.everledger.common.api.handlers.IndexHandler;
import org.interledger.everledger.common.api.util.VertxRunner;
import org.interledger.everledger.ledger.LedgerAccountManagerFactory;
import org.interledger.everledger.ledger.account.LedgerAccountManager;
import org.interledger.everledger.ledger.api.handlers.AccountHandler;
import org.interledger.everledger.ledger.api.handlers.AccountsHandler;
import org.interledger.everledger.ledger.api.handlers.ConnectorsHandler;
import org.interledger.everledger.ledger.api.handlers.FulfillmentHandler;
import org.interledger.everledger.ledger.api.handlers.HealthHandler;
import org.interledger.everledger.ledger.api.handlers.MessageHandler;
import org.interledger.everledger.ledger.api.handlers.TransferHandler;
import org.interledger.everledger.ledger.api.handlers.TransferStateHandler;
import org.interledger.everledger.ledger.api.handlers.TransferWSEventHandler;
import org.interledger.everledger.ledger.api.handlers.TransfersHandler;
import org.interledger.everledger.ledger.impl.simple.SimpleLedgerAccount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Vertx main entry point.
 *
 * @author mrmx
 */
public class Main extends AbstractMainEntrypointVerticle {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        configureDevelopmentEnvirontment();
        VertxRunner.run(Main.class);

    }

    @Override
    public void start() throws Exception {
        log.info("Starting ILP ledger api server");
        super.start();
    }

    @Override
    protected List<EndpointHandler> getEndpointHandlers() {
        return Arrays.asList(
                HealthHandler.create(),
                ConnectorsHandler.create(),
                AccountsHandler.create(),
                AccountHandler.create(),
                TransferHandler.create(),
                TransferWSEventHandler.create(),
                TransfersHandler.create(),
                TransferStateHandler.create(),
                FulfillmentHandler.create(),
                MessageHandler.create()
        );
    }

    @Override
    protected void initIndexHandler(Router router, IndexHandler indexHandler) {
        super.initIndexHandler(router, indexHandler); // TODO:(0) Update and sync with JS version
    }

    private static void configureDevelopmentEnvirontment() { // TODO:(0) Remove once everything is properly setup
        log.info("Preparing development environment");
        Map<String, AuthInfo> mapUsers = AuthManager.getUsers();
        Set<String> keyUsers = mapUsers.keySet();
        LedgerAccountManager ledgerAccountManager = LedgerAccountManagerFactory.getLedgerAccountManagerSingleton();
        for (String accountId : keyUsers) {
            SimpleLedgerAccount account = (SimpleLedgerAccount) ledgerAccountManager.create(accountId);
            account.setBalance(10000);
            if (accountId.equals("admin")) {
                account.setAdmin(true);
            }
            account.setMinimumAllowedBalance(0);
            ledgerAccountManager.store(account);
        }
    }

}
