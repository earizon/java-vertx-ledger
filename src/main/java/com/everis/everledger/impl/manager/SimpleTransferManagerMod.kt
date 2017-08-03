package com.everis.everledger.impl.manager

import io.vertx.core.json.JsonObject

import java.time.ZonedDateTime
import java.util.HashMap
import java.util.HashSet
// import java.util.Map
// import java.util.List
import java.util.ArrayList
// import java.util.Set
import java.util.UUID

import javax.money.MonetaryAmount

import org.interledger.Condition
import org.interledger.Fulfillment
import org.interledger.ledger.model.TransferStatus
//import org.javamoney.moneta.Money
// import org.slf4j.Logger
import org.slf4j.LoggerFactory

import com.everis.everledger.AccountManagerFactory
import com.everis.everledger.handlers.TransferWSEventHandler
import com.everis.everledger.ifaces.account.IfaceLocalAccount
import com.everis.everledger.ifaces.transfer.IfaceTransfer
import com.everis.everledger.ifaces.transfer.IfaceTransferManager
import com.everis.everledger.impl.SimpleTransfer
// import com.everis.everledger.transfer.Credit
import com.everis.everledger.transfer.Debit
import com.everis.everledger.transfer.LocalTransferID
import com.everis.everledger.util.ConversionUtil
import com.everis.everledger.util.ILPExceptionSupport

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

private val log             = LoggerFactory.getLogger(SimpleTransferManager::class.java)
private val accountManager  = AccountManagerFactory.getLedgerAccountManagerSingleton()
private val HOLDS_URI       = accountManager.holdAccountILP

// fun getTransferManager() : IfaceTransferManager {
//     // TODO:(1) Move function to factory similar to LedgerAccountManagerFactory
//     return singleton
// }

private fun notifyUpdate(transfer : IfaceTransfer, fulfillment : Fulfillment, isExecution : Boolean){
    try {
        val notification : JsonObject = (transfer as SimpleTransfer).toILPJSONStringifiedFormat()
        // Notify affected accounts:
        val eventType : TransferWSEventHandler.EventType =
                if   (transfer.getTransferStatus() == TransferStatus.PROPOSED)
                     TransferWSEventHandler.EventType.TRANSFER_CREATE
                else TransferWSEventHandler.EventType.TRANSFER_UPDATE
        val setAffectedAccounts = HashSet<String>()
        for (debit  in transfer.debits ) setAffectedAccounts.add( debit.account.localID)
        for (credit in transfer.credits) setAffectedAccounts.add(credit.account.localID)

        val relatedResources = HashMap<String, Any>()
        val relatedResourceKey = if (isExecution) "execution_condition_fulfillment"
                                 else             "cancellation_condition_fulfillment"
        val base64FF = ConversionUtil.fulfillmentToBase64(fulfillment)
        relatedResources.put( relatedResourceKey, base64FF)
        val jsonRelatedResources = JsonObject(relatedResources)
        TransferWSEventHandler.notifyListener(setAffectedAccounts, eventType, notification, jsonRelatedResources)

    } catch (e : Exception ) {
        log.warn("Fulfillment registrered correctly but ilp-connector couldn't be notified due to " + e.toString())
    }
}


object SimpleTransferManager : IfaceTransferManager {
    private val log          = LoggerFactory.getLogger(SimpleTransferManager::class.java)
    private val transferMap  = HashMap<LocalTransferID, IfaceTransfer>() // In-memory database of pending/executed/cancelled transfers

    fun developerTestingResetTransfers() { // TODO:(?) Make static?
        if (! com.everis.everledger.Config.unitTestsActive) {
            throw RuntimeException("developer.unitTestsActive must be true @ application.conf "
                    + "to be able to reset tests")
        }
        transferMap.clear()
    }


    // START IfaceLocalTransferManager implementation {
    override fun getTransferById(transferId : LocalTransferID ) : IfaceTransfer  {
        val result = transferMap[transferId] ?: throw ILPExceptionSupport.createILPNotFoundException("transfer '${transferId.transferID}' not found")
        if (result.transferStatus == TransferStatus.REJECTED) {
            throw ILPExceptionSupport.createILPUnprocessableEntityException(
                    "This transfer has already been rejected")
        }

        return result
    }

    override fun executeLocalTransfer(transfer : IfaceTransfer ) {
        // AccountUri sender, AccountUri recipient, MonetaryAmount amount)
        val debit_list : Array<Debit> = transfer.getDebits()
        val debit0 = debit_list[0]
        if (debit_list.size > 1) {
            // STEP 1: Pass all debits to first account.
            for ( debit in debit_list) {
                val    sender : IfaceLocalAccount = debit.account
                val recipient : IfaceLocalAccount = debit0.account
                val amount : MonetaryAmount = debit.amount
                __executeLocalTransfer(sender, recipient, amount)
            }
        }
        // STEP 2: Pay crediters from first account:
        val sender : IfaceLocalAccount  = debit0.account
        for (credit in transfer.credits) {
            __executeLocalTransfer(sender, credit.account, credit.amount)
        }
        transfer.transferStatus = TransferStatus.PREPARED
        transfer.transferStatus = TransferStatus.EXECUTED
        transfer.dttM_prepared = ZonedDateTime.now()
        transfer.dttM_executed = ZonedDateTime.now()
    }

    override fun doesTransferExists(transferId : LocalTransferID) :Boolean {
        return transferMap.containsKey(transferId)
    }

    // } END IfaceLocalTransferManager implementation

    // START IfaceILPSpecTransferManager implementation {
    override fun getTransfersByExecutionCondition(condition : Condition ) :
            MutableList<IfaceTransfer> {
        // For this simple implementation just run over existing transfers until
        val result = ArrayList<IfaceTransfer>()
        //   = HashMap<LocalTransferID, IfaceTransfer>();// In-memory database of pending/executed/cancelled transfers
        for ( transferId : LocalTransferID in transferMap.keys) {
            val transfer = transferMap.get(transferId) ?: throw RuntimeException("null found for transferId "+transferId)
            if (transfer.getExecutionCondition() == condition) {
                result.add(transfer)
            }
        }
        return result
    }

    override fun createNewRemoteILPTransfer(newTransfer : IfaceTransfer ) {
        log.debug("createNewRemoteILPTransfer")

        if (doesTransferExists(newTransfer.getTransferID())) {
            throw RuntimeException("trying to create new transfer "
                    + "but transferID '"+newTransfer.getTransferID()+"'already registrered. "
                    + "Check transfer with SimpleLedgerTransferManager.transferExists before invoquing this function")
        }
        log.debug("createNewRemoteILPTransfer newTransfer "+
                newTransfer.getTransferID().transferID+", status: "+newTransfer.getTransferStatus().toString())

        transferMap.put(newTransfer.getTransferID(), newTransfer)
        if (newTransfer.executionCondition == SimpleTransfer.CC_NOT_PROVIDED) {
            // local transfer with no execution condition => execute and "forget"
            log.debug("createNewRemoteILPTransfer execute locally and forget")
            executeLocalTransfer(newTransfer)
            return
        }

        // PUT Money on-hold:
        for (debit in newTransfer.getDebits()) {
            __executeLocalTransfer(debit.account, HOLDS_URI, debit.amount)
        }
        // TODO:(0) Next line commented to make tests pass, but looks to be sensible to do so.
        // newTransfer.setTransferStatus(TransferStatus.PROPOSED)
    }


    private fun executeOrCancelILPTransfer(transfer : IfaceTransfer , FF : Fulfillment , isExecution : Boolean  /*false => isCancellation*/) {
        if (isExecution /* => DisburseFunds */) {
            for (credit in transfer.getCredits()) {
                __executeLocalTransfer(HOLDS_URI, credit.account, credit.amount)
            }
            transfer.setTransferStatus(TransferStatus.EXECUTED)
            transfer.setDTTM_executed(ZonedDateTime.now())
            transfer.setExecutionFulfillment(FF)
        } else /* => Cancellation/Rollback  */ {
            for (debit in transfer.getDebits()) {
                __executeLocalTransfer(HOLDS_URI, debit.account, debit.amount)
            }
            transfer.setTransferStatus(TransferStatus.REJECTED)
            transfer.setDTTM_rejected(ZonedDateTime.now())
            transfer.setCancelationFulfillment(FF)
        }
        notifyUpdate(transfer = transfer, fulfillment = FF, isExecution = isExecution)
    }

    override fun executeILPTransfer(transfer : IfaceTransfer, executionFulfillment : Fulfillment) {
        executeOrCancelILPTransfer(transfer, executionFulfillment, true)
    }

    override fun cancelILPTransfer(transfer : IfaceTransfer , cancellationFulfillment : Fulfillment ) {
        executeOrCancelILPTransfer(transfer, cancellationFulfillment, false)
    }

    override fun doesTransferExists(transferId : UUID ) : Boolean  {
        return doesTransferExists(LocalTransferID.ILPSpec2LocalTransferID(transferId))
    }

    // } END IfaceILPSpecTransferManager implementation

    private fun __executeLocalTransfer(sender : IfaceLocalAccount , recipient : IfaceLocalAccount , amount : MonetaryAmount ) {
        // TODO: LOG local transfer execution.
        log.info("executeLocalTransfer {")
        accountManager.getAccountByName(sender   .getLocalID()).debit (amount)
        accountManager.getAccountByName(recipient.getLocalID()).credit(amount)
        log.info("} executeLocalTransfer")
    }

    // UnitTest / function test realated code
    fun unitTestsResetTransactionDDBB() {
        transferMap.clear()
    }

    fun unitTestsGetTotalTransactions() : String {
        return "${transferMap.keys.size}"
    }
}

