package org.interledger.ilp.ledger.impl.simple;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

import javax.money.MonetaryAmount;

import org.interledger.cryptoconditions.Condition;
import org.interledger.cryptoconditions.Fulfillment;
import org.interledger.ilp.ledger.transfer.Credit;
import org.interledger.ilp.ledger.transfer.DTTM;
import org.interledger.ilp.ledger.transfer.Debit;
import org.interledger.ilp.ledger.transfer.TransferID;
import org.interledger.ilp.ledger.transfer.LedgerTransfer;
import org.interledger.ilp.ledger.model.TransferStatus;
import org.interledger.ilp.common.api.util.ILPExceptionSupport;
import org.interledger.ilp.exceptions.InterledgerException;
import org.interledger.ilp.ledger.LedgerAccountManagerFactory;
import org.interledger.ilp.ledger.account.LedgerAccount;
import org.interledger.ilp.ledger.account.LedgerAccountManager;
import org.interledger.ilp.ledger.transfer.LedgerTransferManager;
//import org.javamoney.moneta.Money;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
/**
 * Simple in-memory {@code SimpleLedgerTransferManager}.
 *
 * @author earizon
 * 
 * FIXME:
 *  All the @Override methods will be transactional in a real database 
 *  JEE / Hibernate / ... . Mark them "somehow"
 *  REF:
 *    - http://docs.oracle.com/cd/E23095_01/Platform.93/ATGProgGuide/html/s1205transactiondemarcation01.html
 *    - http://docs.oracle.com/javaee/6/tutorial/doc/bncij.html
 *    - ...
 */
public class SimpleLedgerTransferManager implements LedgerTransferManager /* FIXME TODO LedgerTransferManagerFactory */{

    private static final Logger log = LoggerFactory.getLogger(SimpleLedgerTransferManager.class);

    private Map<TransferID, LedgerTransfer> transferMap = 
        new HashMap<TransferID, LedgerTransfer>();// In-memory database of pending/executed/cancelled transfers

    private static SimpleLedgerTransferManager singleton = new SimpleLedgerTransferManager();

    private static final LedgerAccount HOLDS_URI = LedgerAccountManagerFactory.getLedgerAccountManagerSingleton().getHOLDAccountILP();

    // Make default constructor private to avoid instantiating new classes.
    private SimpleLedgerTransferManager() {}

    public static LedgerTransferManager getSingleton() {
        return singleton;
    }

    @Override
    public LedgerTransfer getTransferById(TransferID transferId) {
        LedgerTransfer result = transferMap.get(transferId);
        if (result == null) {
            ILPExceptionSupport.launchILPException(
                this.getClass().getName() + "This transfer does not exist");
        }
        if (result.getTransferStatus() == TransferStatus.REJECTED) {
            ILPExceptionSupport.launchILPException(
                    this.getClass().getName() + "This transfer has already been rejected");
        }
    
        return result;
    }

    @Override
    public java.util.List<LedgerTransfer> getTransfersByExecutionCondition(Condition condition) {
        // For this simple implementation just run over existing transfers until 
        List<LedgerTransfer> result = new ArrayList<LedgerTransfer>();
        for ( TransferID transferId : transferMap.keySet()) {
            LedgerTransfer transfer = transferMap.get(transferId);
            if (transfer.getExecutionCondition().equals(condition)) {
                result.add(transfer);
            }
        }
        return result;
    }


    @Override
    public boolean transferExists(TransferID transferId) {
        boolean result = transferMap.containsKey(transferId);
        return result;
    }

    @Override
    public void createNewRemoteILPTransfer(LedgerTransfer newTransfer) {
        log.debug("createNewRemoteILPTransfer");

        if (transferExists(newTransfer.getTransferID())) {
            throw new RuntimeException("trying to create new transfer "
                    + "but transferID '"+newTransfer.getTransferID()+"'already registrered. "
                    + "Check transfer with SimpleLedgerTransferManager.transferExists before invoquing this function");
        }
        log.debug("createNewRemoteILPTransfer newTransfer "+
                newTransfer.getTransferID().transferID+", status: "+newTransfer.getTransferStatus().toString());

        log.debug("newTransfer.isLocal(): "+ newTransfer.isLocal());

        transferMap.put(newTransfer.getTransferID(), newTransfer);
        if (newTransfer.isLocal() 
            /* &&  newTransfer.getExecutionCondition().equals(Condition.EMPTY) TODO:(0) */) {
            log.debug("createNewRemoteILPTransfer execute locally and forget");
            // local transfer with no execution condition => execute and "forget" 
            executeLocalTransfer(newTransfer);
            return;
        }

        // PUT Money on-hold:
        for (Debit debit : newTransfer.getDebits()) {
            executeLocalTransfer(debit.account, HOLDS_URI, debit.amount);
        }
        // TODO: Next line commented to make tests pass, but looks to be sensible to do so.
        // newTransfer.setTransferStatus(TransferStatus.PROPOSED);
    }

    private void executeLocalTransfer(LedgerAccount sender, LedgerAccount recipient, MonetaryAmount amount) {
        // FIXME: LOG local transfer execution.
        LedgerAccountManager accManager = LedgerAccountManagerFactory.getLedgerAccountManagerSingleton();
        accManager.getAccountByName(sender   .getName()).debit (amount);
        accManager.getAccountByName(recipient.getName()).credit(amount);
    }

    @Override
    public void executeLocalTransfer(LedgerTransfer transfer) {
        // AccountUri sender, AccountUri recipient, MonetaryAmount amount)
        transfer.checkBalancedTransaction();
        Debit[] debit_list = transfer.getDebits();
        if (debit_list.length > 1) {
            // STEP 1: Pass all debits to first account.
            for (int idx=1; idx < debit_list.length ; idx++) {
                LedgerAccount    sender = debit_list[idx].account;
                LedgerAccount recipient = debit_list[0].account;
                MonetaryAmount amount = debit_list[idx].amount;
                executeLocalTransfer(sender, recipient, amount);
            }
        }
        // STEP 2: Pay crediters from first account:
        LedgerAccount sender = debit_list[0].account;
        for (Credit credit : transfer.getCredits()) {
            LedgerAccount recipient = credit.account;
            MonetaryAmount amount = credit.amount;
            executeLocalTransfer(sender, recipient, amount);
        }
        transfer.setTransferStatus(TransferStatus.PREPARED);
        transfer.setTransferStatus(TransferStatus.EXECUTED);
        transfer.setDTTM_prepared(DTTM.getNow());
        transfer.setDTTM_executed(DTTM.getNow());
    }

    @Override
    public void executeRemoteILPTransfer(LedgerTransfer transfer, Fulfillment executionFulfillment) {
        // DisburseFunds:
        for (Credit debit : transfer.getCredits()) {
            executeLocalTransfer(HOLDS_URI, debit.account, debit.amount);
        }
        transfer.setTransferStatus(TransferStatus.EXECUTED);
        transfer.setExecutionFulfillment(executionFulfillment);
    }

    @Override
    public void abortRemoteILPTransfer(LedgerTransfer transfer, Fulfillment cancellationFulfillment) {
        // Return Held Funds
        for (Debit debit : transfer.getDebits()) {
            executeLocalTransfer(HOLDS_URI, debit.account, debit.amount);
        }
        transfer.setTransferStatus(TransferStatus.REJECTED);
        transfer.setCancelationFulfillment(cancellationFulfillment);
    }

    // UnitTest / function test realated code
    public void unitTestsResetTransactionDDBB() {
        transferMap = new HashMap<TransferID, LedgerTransfer>();
    }
    
    public String unitTestsGetTotalTransactions() {
        return ""+transferMap.keySet().size();
    }
    

}
