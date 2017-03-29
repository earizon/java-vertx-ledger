package org.interledger.ilp.ledger.account;

import java.util.Collection;
import org.interledger.ilp.core.AccountURI;

/**
 * Defines an account manager.
 *
 * @author mrmx 
 */
public interface LedgerAccountManager {

    LedgerAccount create(String name);

    int getTotalAccounts();

    void store(LedgerAccount account);

    boolean hasAccount(String name);

    LedgerAccount getAccountByName(String name);

    Collection<LedgerAccount> getAccounts(int page, int pageSize);

    AccountURI getAccountUri(LedgerAccount account);

    LedgerAccount getHOLDAccountILP();
}
