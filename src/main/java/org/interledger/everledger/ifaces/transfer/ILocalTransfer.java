package org.interledger.everledger.ifaces.transfer;

import org.interledger.cryptoconditions.Fulfillment;
import org.interledger.everledger.ledger.transfer.Credit;
import org.interledger.everledger.ledger.transfer.DTTM;
import org.interledger.everledger.ledger.transfer.Debit;
import org.interledger.everledger.ledger.transfer.LocalTransferID;
import org.interledger.ilp.ledger.model.TransferStatus;

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

    public DTTM getDTTM_prepared();
    public void setDTTM_prepared(DTTM dTTM_prepared);

    public DTTM getDTTM_executed();
    public void setDTTM_executed(DTTM dTTM_executed);

    public DTTM getDTTM_rejected();
    public void setDTTM_rejected(DTTM dTTM_rejected);

    public DTTM getDTTM_expires();

    public DTTM getDTTM_proposed();

    public void           setExecutionFulfillment(Fulfillment ff);
    public Fulfillment    getExecutionFulfillment();

    public void           setCancelationFulfillment(Fulfillment ff);
    public Fulfillment    getCancellationFulfillment();

    public void checkBalancedTransaction();


}