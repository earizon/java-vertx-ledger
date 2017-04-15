package org.interledger.everledger.ledger.transfer;

// TODO:(0) Maybe "parts" of this interface can be extracted to an standard API
import org.interledger.cryptoconditions.Condition;
import org.interledger.cryptoconditions.Fulfillment;
import org.interledger.everledger.ledger.transfer.LedgerTransfer;
import org.interledger.everledger.ledger.transfer.TransferID;

public  interface LedgerTransferManager {
    LedgerTransfer getTransferById(TransferID transferId);

    java.util.List<LedgerTransfer> getTransfersByExecutionCondition(Condition condition);

    boolean transferExists(TransferID transferId);
    
    void createNewRemoteILPTransfer(LedgerTransfer newTransfer);

    // void executeLocalTransfer(AccountUri from, AccountUri to, MonetaryAmount amount);

    void executeLocalTransfer(LedgerTransfer transfer);

    void executeRemoteILPTransfer(LedgerTransfer transfer, Fulfillment executionFulfillment);

    void abortRemoteILPTransfer(LedgerTransfer transfer, Fulfillment cancellationFulfillment);

}
