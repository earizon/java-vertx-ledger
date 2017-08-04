package com.everis.everledger.ifaces.transfer;

import java.time.ZonedDateTime;

import com.everis.everledger.ifaces.account.IfaceLocalAccount;
import org.interledger.Fulfillment;
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

    interface LocalTransferID {
       String getUniqueID();
    };

    interface TransferHalfEntry {
        IfaceLocalAccount getLocalAccount();
        MonetaryAmount    getAmount();
    }
    interface  Debit extends TransferHalfEntry {}; // "marker" interface
    interface Credit extends TransferHalfEntry {}; // "marker" interface
    /*
     * Get the transfer Unique ID
     */
    LocalTransferID getTransferID();

    /**
     * Get the local account that funds are being debited from.
     *
     * @return local account identifier
     */
    Credit[] getCredits();

    /**
     * Get the local account that funds are being debited from.
     *
     * @return local account identifier
     */
    Debit[] getDebits();

    TransferStatus getTransferStatus();

    // TODO:(0): the prepared/executed/rejected/proposed/expired are ILP related data move to ilp-core Interface.
    public ZonedDateTime getDTTM_prepared();

    public ZonedDateTime getDTTM_executed();

    public ZonedDateTime getDTTM_rejected();

    public ZonedDateTime getDTTM_expires();

    public ZonedDateTime getDTTM_proposed();

    public Fulfillment    getExecutionFulfillment();

    public Fulfillment    getCancellationFulfillment();

}