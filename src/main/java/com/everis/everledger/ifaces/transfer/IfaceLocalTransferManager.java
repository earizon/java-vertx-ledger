package com.everis.everledger.ifaces.transfer;

import com.everis.everledger.transfer.LocalTransferID;

public interface IfaceLocalTransferManager {

    IfaceTransfer getTransferById(LocalTransferID transferId);

    void executeLocalTransfer(IfaceTransfer transfer);
    
    boolean doesTransferExists(LocalTransferID transferId);

}
