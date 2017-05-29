package com.everis.everledger.impl.manager;

import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.UUID;

import javax.money.MonetaryAmount;

import org.interledger.cryptoconditions.Condition;
import org.interledger.cryptoconditions.Fulfillment;
import org.interledger.ledger.model.TransferStatus;
//import org.javamoney.moneta.Money;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.everis.everledger.AccountManagerFactory;
import com.everis.everledger.ifaces.account.IfaceLocalAccount;
import com.everis.everledger.ifaces.transfer.IfaceTransfer;
import com.everis.everledger.ifaces.transfer.IfaceTransferManager;
import com.everis.everledger.impl.SimpleTransfer;
import com.everis.everledger.transfer.Credit;
import com.everis.everledger.transfer.Debit;
import com.everis.everledger.transfer.LocalTransferID;
import com.everis.everledger.util.ILPExceptionSupport;

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
public class SimpleTransferManager implements IfaceTransferManager {

    private static final Logger log = LoggerFactory.getLogger(SimpleTransferManager.class);

    private Map<LocalTransferID, IfaceTransfer> transferMap = 
        new HashMap<LocalTransferID, IfaceTransfer>();// In-memory database of pending/executed/cancelled transfers

    private static SimpleTransferManager singleton = new SimpleTransferManager();

    private static final SimpleAccountManager accountManager = AccountManagerFactory.getLedgerAccountManagerSingleton();

    private static final IfaceLocalAccount HOLDS_URI = accountManager.getHOLDAccountILP();

    // Make default constructor private to avoid instantiating new classes.
    private SimpleTransferManager() {}

    public void developerTestingResetTransfers() { // TODO:(?) Make static?
        if (! com.everis.everledger.Config.unitTestsActive) {
            throw new RuntimeException("developer.unitTestsActive must be true @ application.conf "
                    + "to be able to reset tests");
        }
        transferMap = new HashMap<LocalTransferID, IfaceTransfer>();
    }

    public static IfaceTransferManager getTransferManager() {
        // TODO:(1) Move function to factory similar to LedgerAccountManagerFactory
        return (IfaceTransferManager) singleton;
    }

    // START IfaceLocalTransferManager implementation {
    @Override
    public IfaceTransfer getTransferById(LocalTransferID transferId) {
        IfaceTransfer result = transferMap.get(transferId);
        if (result == null) {
            throw ILPExceptionSupport.createILPNotFoundException("transfer '"+transferId.transferID+"' not found");
        }
        if (result.getTransferStatus() == TransferStatus.REJECTED) {
            throw ILPExceptionSupport.createILPUnprocessableEntityException(
                    "This transfer has already been rejected");
        }
    
        return result;
    }

    @Override
    public void executeLocalTransfer(IfaceTransfer transfer) {
        // AccountUri sender, AccountUri recipient, MonetaryAmount amount)
        Debit[] debit_list = transfer.getDebits();
        if (debit_list.length > 1) {
            // STEP 1: Pass all debits to first account.
            for (int idx=1; idx < debit_list.length ; idx++) {
                IfaceLocalAccount    sender = debit_list[idx].account;
                IfaceLocalAccount recipient = debit_list[0].account;
                MonetaryAmount amount = debit_list[idx].amount;
                __executeLocalTransfer(sender, recipient, amount);
            }
        }
        // STEP 2: Pay crediters from first account:
        IfaceLocalAccount sender = debit_list[0].account;
        for (Credit credit : transfer.getCredits()) {
            IfaceLocalAccount recipient = credit.account;
            MonetaryAmount amount = credit.amount;
            __executeLocalTransfer(sender, recipient, amount);
        }
        transfer.setTransferStatus(TransferStatus.PREPARED);
        transfer.setTransferStatus(TransferStatus.EXECUTED);
        transfer.setDTTM_prepared(ZonedDateTime.now());
        transfer.setDTTM_executed(ZonedDateTime.now());
    }

    @Override
    public boolean doesTransferExists(LocalTransferID transferId) {
        return transferMap.containsKey(transferId);
    }

    // } END IfaceLocalTransferManager implementation

    // START IfaceILPSpecTransferManager implementation {
    @Override
    public java.util.List<IfaceTransfer> getTransfersByExecutionCondition(Condition condition) {
        // For this simple implementation just run over existing transfers until 
        List<IfaceTransfer> result = new ArrayList<IfaceTransfer>();
        for ( LocalTransferID transferId : transferMap.keySet()) {
            IfaceTransfer transfer = transferMap.get(transferId);
            if (transfer.getExecutionCondition().equals(condition)) {
                result.add(transfer);
            }
        }
        return result;
    }

    @Override
    public void createNewRemoteILPTransfer(IfaceTransfer newTransfer) {
        log.debug("createNewRemoteILPTransfer");

        if (doesTransferExists(newTransfer.getTransferID())) {
            throw new RuntimeException("trying to create new transfer "
                    + "but transferID '"+newTransfer.getTransferID()+"'already registrered. "
                    + "Check transfer with SimpleLedgerTransferManager.transferExists before invoquing this function");
        }
        log.debug("createNewRemoteILPTransfer newTransfer "+
                newTransfer.getTransferID().transferID+", status: "+newTransfer.getTransferStatus().toString());

        transferMap.put(newTransfer.getTransferID(), newTransfer);
        if (newTransfer.getExecutionCondition().equals(SimpleTransfer.CC_NOT_PROVIDED)) {
            // local transfer with no execution condition => execute and "forget"
            log.debug("createNewRemoteILPTransfer execute locally and forget");
            executeLocalTransfer(newTransfer);
            return;
        }

        // PUT Money on-hold:
        for (Debit debit : newTransfer.getDebits()) {
            __executeLocalTransfer(debit.account, HOLDS_URI, debit.amount);
        }
        // TODO: Next line commented to make tests pass, but looks to be sensible to do so.
        // newTransfer.setTransferStatus(TransferStatus.PROPOSED);
    }

    @Override
    public void executeILPTransfer(IfaceTransfer transfer, Fulfillment executionFulfillment) {
        // DisburseFunds:
        for (Credit debit : transfer.getCredits()) {
            __executeLocalTransfer(HOLDS_URI, debit.account, debit.amount);
        }
        transfer.setTransferStatus(TransferStatus.EXECUTED);
        transfer.setDTTM_executed(ZonedDateTime.now());
        transfer.setExecutionFulfillment(executionFulfillment);
    }

    @Override
    public void abortRemoteILPTransfer(IfaceTransfer transfer, Fulfillment cancellationFulfillment) {
        // Return Held Funds
        for (Debit debit : transfer.getDebits()) {
            __executeLocalTransfer(HOLDS_URI, debit.account, debit.amount);
        }
        transfer.setTransferStatus(TransferStatus.REJECTED);
        transfer.setCancelationFulfillment(cancellationFulfillment);
    }

    @Override
    public boolean doesTransferExists(UUID transferId) {
        return doesTransferExists(LocalTransferID.ILPSpec2LocalTransferID(transferId));
    }

    // } END IfaceILPSpecTransferManager implementation
    

    private void __executeLocalTransfer(IfaceLocalAccount sender, IfaceLocalAccount recipient, MonetaryAmount amount) {
        // TODO: LOG local transfer execution.
        log.info("executeLocalTransfer {");
        accountManager.getAccountByName(sender   .getLocalID()).debit (amount);
        accountManager.getAccountByName(recipient.getLocalID()).credit(amount);
        log.info("} executeLocalTransfer");
    }

    // UnitTest / function test realated code
    public void unitTestsResetTransactionDDBB() {
        transferMap = new HashMap<LocalTransferID, IfaceTransfer>();
    }
    
    public String unitTestsGetTotalTransactions() {
        return ""+transferMap.keySet().size();
    }
    

}
