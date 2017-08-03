package com.everis.everledger.ifaces.transfer;

import java.util.UUID;

import org.interledger.Condition;
import org.interledger.Fulfillment;

public interface IfaceILPSpecTransferManager {

    java.util.List<IfaceTransfer> getTransfersByExecutionCondition(Condition condition);

    void createNewRemoteILPTransfer(IfaceTransfer newTransfer);

    IfaceTransfer executeILPTransfer(IfaceTransfer transfer, Fulfillment executionFulfillment);

    IfaceTransfer cancelILPTransfer(IfaceTransfer transfer, Fulfillment cancellationFulfillment);
    
    boolean doesTransferExists(UUID transferId);


}
