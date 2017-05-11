package org.interledger.everledger.ifaces.transfer;

import org.interledger.cryptoconditions.Condition;
import org.interledger.cryptoconditions.Fulfillment;
import org.interledger.everledger.ledger.transfer.ILPSpecTransferID;

public  interface IfaceILPSpecTransferManager {

    java.util.List<ILedgerTransfer> getTransfersByExecutionCondition(Condition condition);

    void createNewRemoteILPTransfer(ILedgerTransfer newTransfer);

    void executeRemoteILPTransfer(ILedgerTransfer transfer, Fulfillment executionFulfillment);

    void abortRemoteILPTransfer(ILedgerTransfer transfer, Fulfillment cancellationFulfillment);
    
    boolean doesTransferExists(ILPSpecTransferID transferId);


}
