package org.interledger.ilp.ledger.api;

import com.google.common.base.Optional;

import io.vertx.ext.web.Router;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import org.apache.commons.lang3.StringUtils;
import org.interledger.ilp.common.api.AbstractMainEntrypointVerticle;
import org.interledger.ilp.common.api.handlers.EndpointHandler;
import org.interledger.ilp.common.api.handlers.IndexHandler;
import org.interledger.ilp.common.api.util.VertxRunner;
import org.interledger.ilp.common.config.Config;
import static org.interledger.ilp.common.config.Key.*;
import org.interledger.ilp.common.config.core.Configurable;
import org.interledger.ilp.common.config.core.ConfigurationException;
import org.interledger.ilp.common.util.NumberConversionUtil;
import org.interledger.ilp.core.Ledger;
import org.interledger.ilp.core.ledger.model.LedgerInfo;
import org.interledger.ilp.ledger.LedgerAccountManagerFactory;
import org.interledger.ilp.ledger.LedgerFactory;
import org.interledger.ilp.ledger.LedgerInfoBuilder;
import org.interledger.ilp.ledger.account.LedgerAccountManager;
import org.interledger.ilp.ledger.api.handlers.AccountHandler;
import org.interledger.ilp.ledger.api.handlers.AccountsHandler;
import org.interledger.ilp.ledger.api.handlers.ConnectorsHandler;
import org.interledger.ilp.ledger.api.handlers.HealthHandler;
import org.interledger.ilp.ledger.api.handlers.MessageHandler;
import org.interledger.ilp.ledger.api.handlers.TransferHandler;
import org.interledger.ilp.ledger.api.handlers.TransferWSEventHandler;
import org.interledger.ilp.ledger.api.handlers.TransfersHandler;
import org.interledger.ilp.ledger.api.handlers.UnitTestSupportHandler;
import org.interledger.ilp.ledger.api.handlers.TransferStateHandler;
import org.interledger.ilp.ledger.api.handlers.FulfillmentHandler;
import org.interledger.ilp.ledger.impl.simple.SimpleLedger;
import org.interledger.ilp.ledger.impl.simple.SimpleLedgerAccount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Vertx main entry point.
 *
 * @author mrmx
 */
public class Main extends AbstractMainEntrypointVerticle implements Configurable {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    private static final String DEFAULT_LEDGER_NAME = "ledger-simple";

    private String ilpPrefix;
    private Ledger ledger;

    //Development configuration namespace:
    enum Dev {
        uri,
        accounts,
        balance, minimum_allowed_balance,
        admin, disabled,
        connector
    }

    public static void main(String[] args) {
        VertxRunner.run(Main.class);

    }

    @Override
    public void start() throws Exception {
        log.info("Starting ILP ledger api server");
        super.start();
    }

    @Override
    public void configure(Config config) throws ConfigurationException {
        ilpPrefix = config.getString(LEDGER, ILP, PREFIX);
        String ledgerName = config.getString(DEFAULT_LEDGER_NAME, LEDGER, NAME);
        String currencyCode = config.getString(LEDGER, CURRENCY, CODE);
        URL baseUri = getServerPublicURL();
        //Development config
        Optional<Config> devConfig = config.getOptionalConfig(Dev.class);
        LedgerInfo ledgerInfo = new LedgerInfoBuilder()
                .setBaseUri(baseUri)
                .setCurrencyCodeAndSymbol(currencyCode)
                //TODO precission and scale
                .build();
        LedgerFactory.initialize(ledgerInfo, ledgerName, config);
        ledger = LedgerFactory.getDefaultLedger();
        //Development config
        if (devConfig.isPresent()) {
            configureDevelopmentEnvirontment(devConfig.get());
        }
    }

    @Override
    protected List<EndpointHandler> getEndpointHandlers(Config config) {
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
                UnitTestSupportHandler.create(),
                MessageHandler.create()
        );
    }

    @Override
    protected void initIndexHandler(Router router, IndexHandler indexHandler) {
        super.initIndexHandler(router, indexHandler);
        LedgerInfo ledgerInfo = ledger.getInfo();
        indexHandler
                .put("ilp_prefix", ilpPrefix)
                .put("currency_code", ledgerInfo.getCurrencyCode())
                .put("currency_symbol", ledgerInfo.getCurrencySymbol())
                .put("precision", ledgerInfo.getPrecision())
                .put("scale", ledgerInfo.getScale());

        Map<String, String > services = new HashMap<String, String >();

        // REF: 
        //   - five-bells-ledger/src/controllers/metadata.js
        //   - plugin.js (REQUIRED_LEDGER_URLS) @ five-bells-plugin
        //   The conector five-bells-plugin of the js-ilp-connector expect a 
        //   map urls { health:..., transfer: ..., 
        String base = ledgerInfo.getBaseUri();

            // Required by wallet
            services.put("health"              , base + "health"                   );
            services.put("accounts"            , base + "accounts"                 );
            services.put("transfer_state"      , base + "transfers/:id/state"      );
            services.put("account"             , base + "accounts/:name"           );
            services.put("account_transfers"   , base.replace("http://", "ws://")
                    .replace("https://", "ws://") + "accounts/:name/transfers" );
            // Required by wallet & ilp (ilp-plugin-bells) connector
            services.put("transfer"            , base + "transfers/:id"            );
            services.put("transfer_fulfillment", base + "transfers/:id/fulfillment");
            services.put("transfer_rejection"  , base + "transfers/:id/rejection"  );
            services.put("message"             , base + "messages"                 );

        indexHandler.put("urls", services);
        
        Config config = ((SimpleLedger)LedgerFactory.getDefaultLedger()).getConfig();
        String public_key = config.getString(SERVER, ED25519, PUBLIC_KEY);
        indexHandler.put("condition_sign_public_key", public_key);
 
        String[] list = new String[]{"alice", "bob", "ilpconnector"};// FIXME:(NOW) TODO: Connector accounts data hardcoded. 
        List<Map<String,String>> connectors = new ArrayList<Map<String,String>>();
        for (int idx = 0; idx < list.length ; idx++){
            String accountName = list[idx];
            
            Map<String, String > connector1 = new HashMap<String, String >();
                connector1.put("id", base +"accounts/"+accountName);
                connector1.put("name", accountName);
                connector1.put("connector", "localhost:4000");
             connectors.add(connector1);
        }
        indexHandler.put("connectors", connectors);
    }

    private void configureDevelopmentEnvirontment(Config config) {
        log.info("Preparing development environment");
        List<String> accounts = config.getStringList(Dev.accounts);
        LedgerAccountManager ledgerAccountManager = LedgerAccountManagerFactory.getLedgerAccountManagerSingleton();
        for (String accountName : accounts) {
            SimpleLedgerAccount account = (SimpleLedgerAccount) ledgerAccountManager.create(accountName);
            Config accountConfig = config.getConfig(accountName);
            account.setBalance(accountConfig.getInt(0, Dev.balance));
            if (accountConfig.getBoolean(false, Dev.admin)) {
                account.setAdmin(true);
            }
            account.setDisabled(accountConfig.getBoolean(false, Dev.disabled));
            account.setConnector(accountConfig.getString((String) null, Dev.connector));
            String minAllowedBalance = accountConfig.getString((String) null, Dev.minimum_allowed_balance);
            if (StringUtils.isNoneBlank(minAllowedBalance)) {
                account.setMinimumAllowedBalance(NumberConversionUtil.toNumber(minAllowedBalance, 0));
            }
            ledgerAccountManager.store(account);
        }
    }

}
