package org.interledger.everledger.ledger.transfer;

import org.interledger.everledger.ledger.transfer.LedgerTransfer;

public  interface IfaceLocalTransferManager {

    LedgerTransfer getLocalTransferById(LocalTransferID transferId);

    void executeLocalTransfer(LedgerTransfer transfer);
    
    boolean doesTransferExists(LocalTransferID transferId);

}
