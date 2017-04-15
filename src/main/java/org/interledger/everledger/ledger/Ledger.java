package org.interledger.everledger.ledger;

import org.interledger.everledger.common.config.Config;
import org.interledger.ilp.ledger.model.LedgerInfo;


public interface Ledger { // TODO:(0) Recheck this interface. Doesn't look to be very useful.

    /**
     * Retrieve some meta-data about the ledger. (useful for Wallet/ILP-connector)
     *
     * @return <code>LedgerInfo</code>
     */
    LedgerInfo getInfo();
    
    Config getConfig();

}
