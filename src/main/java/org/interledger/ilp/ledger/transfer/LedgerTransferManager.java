package org.interledger.ilp.ledger.transfer;


import org.interledger.ilp.core.ConditionURI;
import org.interledger.ilp.core.FulfillmentURI;
import org.interledger.ilp.core.TransferID;
import org.interledger.ilp.core.ledger.model.LedgerTransfer;

public  interface LedgerTransferManager {
    LedgerTransfer getTransferById(TransferID transferId);

    java.util.List<LedgerTransfer> getTransfersByExecutionCondition(ConditionURI condition);

    boolean transferExists(TransferID transferId);
    
    void createNewRemoteILPTransfer(LedgerTransfer newTransfer);

    // void executeLocalTransfer(AccountUri from, AccountUri to, MonetaryAmount amount);

    void executeLocalTransfer(LedgerTransfer transfer);

    void executeRemoteILPTransfer(LedgerTransfer transfer, FulfillmentURI executionFulfillmentURI);

    void abortRemoteILPTransfer(LedgerTransfer transfer, FulfillmentURI cancellationFulfillmentURI);

}
