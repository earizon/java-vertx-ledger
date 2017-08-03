package com.everis.everledger.ifaces.transfer;

import java.time.ZonedDateTime;

import org.interledger.Fulfillment;
import org.interledger.ledger.model.TransferStatus;

import com.everis.everledger.transfer.Credit;
import com.everis.everledger.transfer.Debit;
import com.everis.everledger.transfer.LocalTransferID;

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