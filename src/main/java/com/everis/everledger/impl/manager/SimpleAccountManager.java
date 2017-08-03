package com.everis.everledger.impl.manager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.interledger.ilp.InterledgerError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.javamoney.moneta.Money;


import com.everis.everledger.Config;
import com.everis.everledger.impl.SimpleAccount;

import com.everis.everledger.AccountManagerFactory;
import com.everis.everledger.AuthInfo;
import com.everis.everledger.ifaces.account.IfaceAccount;
import com.everis.everledger.ifaces.account.IfaceAccountManager;
import com.everis.everledger.ifaces.account.IfaceLocalAccount;
import com.everis.everledger.util.AuthManager;
import com.everis.everledger.util.ILPExceptionSupport;

/**
 * Simple in-memory {@code LedgerAccountManager}.
 */
public class SimpleAccountManager implements IfaceAccountManager {
    private Map<String, IfaceAccount> accountMap;
    private static final String ILP_HOLD_ACCOUNT = "@@HOLD@@";
    
    private static final Logger log = LoggerFactory.getLogger(SimpleAccountManager.class);
    
    private void resetAccounts() {
        accountMap  = new TreeMap<String, IfaceAccount>();
    }

    public static void developerTestingReset(){
        if (! com.everis.everledger.Config.unitTestsActive) {
            throw new RuntimeException("developer.unitTestsActive must be true @ application.conf "
                    + "to be able to reset tests");
        }
        // 
        SimpleAccountManager ledgerAccountManager =(SimpleAccountManager)
                AccountManagerFactory.getLedgerAccountManagerSingleton();
        ledgerAccountManager.resetAccounts(); // STEP 1: Reset accounts to initial state

        // STEP 2: Create an account for each pre-configured user in AuthManager:
        final Map<AuthInfo, Integer /*blance*/> devUsers = AuthManager.configureDevelopmentEnvironment();
        Set<AuthInfo> users = devUsers.keySet();
        for (AuthInfo ai : users) {
            SimpleAccount account = new SimpleAccount(ai.getId(),
                    Money.of(0, Config.ledgerCurrencyCode), // TODO:(Kotlin) once kotlinified remove defaults
                    Money.of(0, Config.ledgerCurrencyCode),
                    false);
            ledgerAccountManager.store(account, false);
        }
    }

    @Override
    public IfaceAccount getHOLDAccountILP() {
        if (accountMap.containsKey(ILP_HOLD_ACCOUNT)) { return accountMap.get(ILP_HOLD_ACCOUNT); }
        SimpleAccount ilpHold = new SimpleAccount(ILP_HOLD_ACCOUNT,
                    Money.of(0, Config.ledgerCurrencyCode), // TODO:(Kotlin) once kotlinified remove defaults
                    Money.of(0, Config.ledgerCurrencyCode),
                    false);
        store(ilpHold, false);
        return ilpHold;
    }
    // } end IfaceILPSpecAccountManager implementation

    // start IfaceILPSpecAccountManager implementation {
    public SimpleAccountManager() {
        resetAccounts();
    }

    @Override
    public void store(IfaceAccount account, boolean update) {
        if (update == false && hasAccount(account.getName())) {
            throw ILPExceptionSupport.createILPForbiddenException("account already exists");
        }
        accountMap.put(account.getName(), account);
    }

    @Override
    public boolean hasAccount(String name) {
        return accountMap.containsKey(name);
    }

    @Override
    public IfaceAccount getAccountByName(String name) {
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
