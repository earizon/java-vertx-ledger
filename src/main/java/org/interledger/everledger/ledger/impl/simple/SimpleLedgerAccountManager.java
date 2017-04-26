package org.interledger.everledger.ledger.impl.simple;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.interledger.everledger.common.api.util.ILPExceptionSupport;
import org.interledger.everledger.common.config.Config;
import org.interledger.everledger.ledger.account.IfaceAccount;
import org.interledger.everledger.ledger.account.IfaceAccountManager;
import org.interledger.everledger.ledger.account.IfaceLocalAccount;
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
        return create(ILP_HOLD_ACCOUNT);
    }
    // } end IfaceILPSpecAccountManager implementation
    

    // start IfaceILPSpecAccountManager implementation {
    public SimpleLedgerAccountManager() {
        accountMap = new TreeMap<String, IfaceAccount>();
    }
    
    @Override
    public IfaceAccount create(String name) throws InterledgerException {
        if (accountMap.containsKey(name)) {
            throw ILPExceptionSupport.createILPInternalException(
                    this.getClass().getName() +  "account '"+name+"' already exists");
        }
        return new SimpleLedgerAccount(name);
    }

    @Override
    public void store(IfaceAccount account) {
        accountMap.put(account.getLocalName(), account);
    }

    @Override
    public boolean hasAccount(String name) {
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
