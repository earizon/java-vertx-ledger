package org.interledger.everledger.ledger;

import org.interledger.everledger.ledger.account.LedgerAccountManager;
import org.interledger.everledger.ledger.impl.simple.SimpleLedgerAccountManager;

/**
 * Ledger factory.
 *
 * @author mrmx
 */
public class LedgerAccountManagerFactory {

    private static final SimpleLedgerAccountManager instance;
    static {
        instance = new SimpleLedgerAccountManager();
        // Create HOLD account required by ILP Protocol.
        instance.store(instance.getHOLDAccountILP());
    }

    public static SimpleLedgerAccountManager getLedgerAccountManagerSingleton() {
        return instance;
    }
    
    public static LedgerAccountManager createLedgerAccountManager() {
        return new SimpleLedgerAccountManager();
    }
}
