package org.interledger.everledger;

import org.interledger.everledger.ifaces.account.IfaceAccountManager;
import org.interledger.everledger.impl.manager.SimpleAccountManager;

/**
 * Ledger factory.
 *
 * @author mrmx
 */
public class AccountManagerFactory {
    // TODO:(?) Simplificar / eliminar esta clase??
    private static final SimpleAccountManager instance;
    static {
        instance = new SimpleAccountManager();
        // Create HOLD account required by ILP Protocol.
        instance.store(instance.getHOLDAccountILP());
    }

    public static SimpleAccountManager getLedgerAccountManagerSingleton() {
        return instance;
    }
    
    public static IfaceAccountManager createLedgerAccountManager() {
        return new SimpleAccountManager();
    }
}
