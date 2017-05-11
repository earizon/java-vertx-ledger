package org.interledger.everledger.ifaces.transfer;

import org.interledger.everledger.ledger.transfer.LocalTransferID;

public interface IfaceLocalTransferManager {

    ILedgerTransfer getLocalTransferById(LocalTransferID transferId);

    void executeLocalTransfer(ILedgerTransfer transfer);
    
    boolean doesTransferExists(LocalTransferID transferId);

}
