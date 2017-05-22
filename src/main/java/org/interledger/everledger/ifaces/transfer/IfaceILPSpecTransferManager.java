package org.interledger.everledger.ifaces.transfer;

import java.util.UUID;

import org.interledger.cryptoconditions.Condition;
import org.interledger.cryptoconditions.Fulfillment;

public  interface IfaceILPSpecTransferManager {

    java.util.List<IfaceTransfer> getTransfersByExecutionCondition(Condition condition);

    void createNewRemoteILPTransfer(IfaceTransfer newTransfer);

    void executeRemoteILPTransfer(IfaceTransfer transfer, Fulfillment executionFulfillment);

    void abortRemoteILPTransfer(IfaceTransfer transfer, Fulfillment cancellationFulfillment);
    
    boolean doesTransferExists(UUID transferId);


}
