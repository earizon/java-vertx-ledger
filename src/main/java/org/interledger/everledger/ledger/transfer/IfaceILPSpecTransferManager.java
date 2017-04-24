package org.interledger.everledger.ledger.transfer;

import org.interledger.cryptoconditions.Condition;
import org.interledger.cryptoconditions.Fulfillment;
import org.interledger.everledger.ledger.transfer.LedgerTransfer;

public  interface IfaceILPSpecTransferManager {

    java.util.List<LedgerTransfer> getTransfersByExecutionCondition(Condition condition);

    void createNewRemoteILPTransfer(LedgerTransfer newTransfer);

    void executeRemoteILPTransfer(LedgerTransfer transfer, Fulfillment executionFulfillment);

    void abortRemoteILPTransfer(LedgerTransfer transfer, Fulfillment cancellationFulfillment);
    
    boolean doesTransferExists(ILPSpecTransferID transferId);


}
