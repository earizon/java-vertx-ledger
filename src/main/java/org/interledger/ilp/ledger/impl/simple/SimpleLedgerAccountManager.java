package org.interledger.ilp.ledger.impl.simple;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.interledger.ilp.exceptions.InterledgerException;
import org.interledger.ilp.ledger.LedgerFactory;
import org.interledger.ilp.ledger.account.ILPAccountSupport;
import org.interledger.ilp.ledger.account.LedgerAccount;
import org.interledger.ilp.ledger.account.LedgerAccountManager;

/**
 * Simple in-memory {@code LedgerAccountManager}.
 *
 * @author mrmx
 */
public class SimpleLedgerAccountManager implements LedgerAccountManager, ILPAccountSupport {
    private Map<String, LedgerAccount> accountMap;
    private static final String ILP_HOLD_ACCOUNT = "@@HOLD@@";

    public SimpleLedgerAccountManager() {
        accountMap = new TreeMap<String, LedgerAccount>();
    }
    
    @Override
    public LedgerAccount create(String name) throws InterledgerException {
        if (accountMap.containsKey(name)) {
            throw new InterledgerException(InterledgerException.RegisteredException.AccountExists, "account '"+name+"' already exists");

        }
        return new SimpleLedgerAccount(name);
    }

    @Override
    public void store(LedgerAccount account) {
        accountMap.put(account.getName(), account);
    }

    @Override
    public boolean hasAccount(String name) {
        return accountMap.containsKey(name);
    }
    
    @Override
    public LedgerAccount getAccountByName(String name) throws InterledgerException {
        if (!hasAccount(name)) {
            throw new InterledgerException(InterledgerException.RegisteredException.AccountNotFoundError, "local account '"+name+"' not found");
        }
        return accountMap.get(name);
    }

    @Override
    public URI getPublicURIForAccount(LedgerAccount account) {
        String baseURI = ((SimpleLedger)LedgerFactory.getDefaultLedger()).getConfig().getPublicURI().toString();
        String sURI = baseURI+"accounts/"+account.getName();
        try {
            URI result = new URI(sURI);
            return result;
        } catch (URISyntaxException e) {
            throw new RuntimeException("Can't create URI from string '"+sURI+"'");
        }
        

    }

    @Override
    public Collection<LedgerAccount> getAccounts(int page, int pageSize) {        
        List<LedgerAccount> accounts = new ArrayList<>();
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

    @Override
    public LedgerAccount getHOLDAccountILP() {
        if (accountMap.containsKey(ILP_HOLD_ACCOUNT)) { return accountMap.get(ILP_HOLD_ACCOUNT); }
        return create(ILP_HOLD_ACCOUNT);
    }


}
