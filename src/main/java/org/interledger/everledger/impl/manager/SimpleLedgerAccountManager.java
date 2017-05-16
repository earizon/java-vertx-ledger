package org.interledger.everledger.impl.manager;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.interledger.everledger.AuthInfo;
import org.interledger.everledger.Config;
import org.interledger.everledger.LedgerAccountManagerFactory;
import org.interledger.everledger.ifaces.account.IfaceAccount;
import org.interledger.everledger.ifaces.account.IfaceAccountManager;
import org.interledger.everledger.ifaces.account.IfaceLocalAccount;
import org.interledger.everledger.impl.SimpleLedgerAccount;
import org.interledger.everledger.util.AuthManager;
import org.interledger.everledger.util.ILPExceptionSupport;
import org.interledger.ilp.exceptions.InterledgerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple in-memory {@code LedgerAccountManager}.
 */
public class SimpleLedgerAccountManager implements IfaceAccountManager {
    private Map<String, IfaceAccount> accountMap;
    private static final String ILP_HOLD_ACCOUNT = "@@HOLD@@";
    
    private static final Logger log = LoggerFactory.getLogger(SimpleLedgerAccountManager.class);
    
    private void resetAccounts() {
        accountMap  = new TreeMap<String, IfaceAccount>();
        store(getHOLDAccountILP());
    }

    public static void developerTestingReset(){
        if (! org.interledger.everledger.Config.unitTestsActive) {
            throw new RuntimeException("developer.unitTestsActive must be true @ application.conf "
                    + "to be able to reset tests");
        }
        // 
        SimpleLedgerAccountManager ledgerAccountManager =(SimpleLedgerAccountManager)
                LedgerAccountManagerFactory.getLedgerAccountManagerSingleton();
        ledgerAccountManager.resetAccounts(); // STEP 1: Reset accounts to initial state

        // STEP 2: Create an account for each pre-configured user in AuthManager:
        final Map<AuthInfo, Integer /*blance*/> devUsers = AuthManager.configureDevelopmentEnvironment();
        Set<AuthInfo> users = devUsers.keySet();
        for (AuthInfo ai : users) {
            SimpleLedgerAccount account = new SimpleLedgerAccount(ai.getId());
            account.setBalance(devUsers.get(ai).intValue());
            account.setMinimumAllowedBalance(0);
            ledgerAccountManager.store(account);
        }
    }
    // start IfaceILPSpecAccountManager implementation {
    @Override
    public URI getPublicURIForAccount(IfaceLocalAccount account) {
        String baseURI = Config.publicURL.toString();
        String sURI = baseURI+"accounts/"+account.getLocalName();
        try {
            URI result = new URI(sURI);
            return result;
        } catch (URISyntaxException e) {
            throw new RuntimeException("Can't create URI from string '"+sURI+"'");
        }
    }

    @Override
    public IfaceAccount getHOLDAccountILP() {
        if (accountMap.containsKey(ILP_HOLD_ACCOUNT)) { return accountMap.get(ILP_HOLD_ACCOUNT); }
        SimpleLedgerAccount ilpHold = new SimpleLedgerAccount(ILP_HOLD_ACCOUNT);
        store(ilpHold);
        return ilpHold;
    }
    // } end IfaceILPSpecAccountManager implementation

    // start IfaceILPSpecAccountManager implementation {
    public SimpleLedgerAccountManager() {
        resetAccounts();
    }

    @Override
    public void store(IfaceAccount account) {
        log.info(">>>>>>>>>>>>>>>>>>>>>deleteme: store account.getName():"+account.getName());
        accountMap.put(account.getName(), account);
    }

    @Override
    public boolean hasAccount(String name) {
        log.info(">>>>>>>>>>>>>>>>>>>>>deleteme: hasAccount account. name:"+name);

        return accountMap.containsKey(name);
    }

    @Override
    public IfaceAccount getAccountByName(String name) throws InterledgerException {
        if (!hasAccount(name)) {
            log.warn("'"+ name + "' account not found");
            throw ILPExceptionSupport.createILPNotFoundException();
        }
        return accountMap.get(name);
    }

    @Override
    public Collection<IfaceLocalAccount> getAccounts(int page, int pageSize) {        
        List<IfaceLocalAccount> accounts = new ArrayList<>();
        accountMap.values()
                .stream()
                // .filter((LedgerAccount a) -> !a.getName().equals(ILP_HOLD_ACCOUNT))
                .forEach(accounts::add);
        return accounts;
    }

    @Override
    public int getTotalAccounts() {
        return accountMap.size();
    }

}
