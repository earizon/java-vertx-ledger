package org.interledger.everledger;

import org.interledger.everledger.ifaces.account.IfaceAccountManager;
import org.interledger.everledger.impl.manager.SimpleLedgerAccountManager;

/**
 * Ledger factory.
 *
 * @author mrmx
 */
public class LedgerAccountManagerFactory {
    // TODO:(?) Simplificar / eliminar esta clase??
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
