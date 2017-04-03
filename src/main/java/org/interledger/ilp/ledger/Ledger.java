package org.interledger.ilp.ledger;

import org.interledger.ilp.ledger.model.LedgerInfo;

import org.interledger.ilp.common.config.Config;


public interface Ledger { // TODO:(0) Recheck this interface. Doesn't look to be very useful.

    /**
     * Retrieve some meta-data about the ledger. (useful for Wallet/ILP-connector)
     *
     * @return <code>LedgerInfo</code>
     */
    LedgerInfo getInfo();
    
    Config getConfig();

}
