package org.interledger.everledger.ledger;

import org.interledger.everledger.ledger.account.IfaceLocalAccountManager;
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
    
    public static IfaceLocalAccountManager createLedgerAccountManager() {
        return new SimpleLedgerAccountManager();
    }
}
