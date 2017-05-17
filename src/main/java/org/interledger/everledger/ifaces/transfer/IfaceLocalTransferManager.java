package org.interledger.everledger.ifaces.transfer;

import org.interledger.everledger.ledger.transfer.LocalTransferID;

public interface IfaceLocalTransferManager {

    IfaceTransfer getTransferById(LocalTransferID transferId);

    void executeLocalTransfer(IfaceTransfer transfer);
    
    boolean doesTransferExists(LocalTransferID transferId);

}
