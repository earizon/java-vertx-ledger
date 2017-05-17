package org.interledger.everledger.ifaces.transfer;

import org.interledger.cryptoconditions.Condition;
import org.interledger.cryptoconditions.Fulfillment;
import org.interledger.everledger.ledger.transfer.ILPSpecTransferID;

public  interface IfaceILPSpecTransferManager {

    java.util.List<IfaceTransfer> getTransfersByExecutionCondition(Condition condition);

    void createNewRemoteILPTransfer(IfaceTransfer newTransfer);

    void executeRemoteILPTransfer(IfaceTransfer transfer, Fulfillment executionFulfillment);

    void abortRemoteILPTransfer(IfaceTransfer transfer, Fulfillment cancellationFulfillment);
    
    boolean doesTransferExists(ILPSpecTransferID transferId);


}
