package com.everis.everledger.ifaces.transfer;

import com.everis.everledger.transfer.LocalTransferID;

public interface IfaceLocalTransferManager {

    IfaceTransfer getTransferById(LocalTransferID transferId);

    IfaceTransfer executeLocalTransfer(IfaceTransfer transfer); // TODO:(0) Check returned IfaceTransfer
    
    boolean doesTransferExists(LocalTransferID transferId);

}
