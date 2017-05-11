package org.interledger.everledger;

import org.interledger.everledger.ledger.account.IfaceAccountManager;
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
    
    public static IfaceAccountManager createLedgerAccountManager() {
        return new SimpleLedgerAccountManager();
    }
}
