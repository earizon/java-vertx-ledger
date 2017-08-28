package com.everis.everledger.ifaces.transfer;

import java.util.UUID;

import org.interledger.Condition;
import org.interledger.Fulfillment;

public interface IfaceILPSpecTransferManager {
   // TODO:(0)
    java.util.List<IfaceTransfer> getTransfersByExecutionCondition(Condition condition);

    // TODO:(0) proposeILPTransfer(ILPTransfer
    void prepareILPTransfer(IfaceTransfer newTransfer); // TODO:(0) Rename / split into propose/prepare ?

    IfaceTransfer executeILPTransfer(IfaceTransfer transfer, Fulfillment executionFulfillment);

    IfaceTransfer cancelILPTransfer(IfaceTransfer transfer, Fulfillment cancellationFulfillment);
    
    boolean doesTransferExists(UUID transferId);


}
