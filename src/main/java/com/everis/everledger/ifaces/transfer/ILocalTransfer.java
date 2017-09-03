package com.everis.everledger.ifaces.transfer;

import com.everis.everledger.ifaces.account.IfaceLocalAccount;
import org.interledger.ledger.model.TransferStatus;
import javax.money.MonetaryAmount;

/*
 * Note: This interfaces implement local (non-ILP-related)
 * interface for a transfer of money from a list of debitors
 * to a list of creditors
 * 
 * The ILP interface is defined @ (java-ilp-core)org.interledger.ilp.ledger.model.LedgerTransfer
 * 
 * See SimpleLedgerTransfer for more info
 */
public interface ILocalTransfer {

    interface LocalTransferID { // TODO:(?) Can be replaced and move getUniqueID to ILocalTransfer?
       String getUniqueID();
    };

    interface TransferHalfEntry {
        IfaceLocalAccount getLocalAccount();
        MonetaryAmount    getAmount();
    }

    interface TXInput  extends TransferHalfEntry {}; // "marker" interface
    interface TXOutput extends TransferHalfEntry {}; // "marker" interface

    LocalTransferID getTransferID();

    TXInput getTXInput();
    TXOutput getTXOutput();

    TransferStatus getTransferStatus();

    void checkBalancedTransactionOrThrow();

    boolean isAuthorized();
}