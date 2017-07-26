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

    void setTransferStatus(TransferStatus transferStatus);
    TransferStatus getTransferStatus();

    public ZonedDateTime getDTTM_prepared();
    public void setDTTM_prepared(ZonedDateTime dTTM_prepared);

    public ZonedDateTime getDTTM_executed();
    public void setDTTM_executed(ZonedDateTime dTTM_executed);

    public ZonedDateTime getDTTM_rejected();
    public void setDTTM_rejected(ZonedDateTime dTTM_rejected);

    public ZonedDateTime getDTTM_expires();

    public ZonedDateTime getDTTM_proposed();

    public void           setExecutionFulfillment(Fulfillment ff);
    public Fulfillment    getExecutionFulfillment();

    public void           setCancelationFulfillment(Fulfillment ff);
    public Fulfillment    getCancellationFulfillment();

}