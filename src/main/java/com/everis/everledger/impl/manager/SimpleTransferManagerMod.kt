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
import java.io.File

import javax.money.MonetaryAmount

import org.interledger.Condition
import org.interledger.Fulfillment
import org.interledger.ledger.model.TransferStatus
//import org.javamoney.moneta.Money
// import org.slf4j.Logger
import org.slf4j.LoggerFactory

import com.everis.everledger.handlers.TransferWSEventHandler
import com.everis.everledger.ifaces.account.IfaceLocalAccount
import com.everis.everledger.ifaces.transfer.IfaceTransfer
import com.everis.everledger.ifaces.transfer.IfaceTransferManager

import com.everis.everledger.util.ConversionUtil
import com.everis.everledger.util.ILPExceptionSupport

import com.everis.everledger.util.Config
import com.everis.everledger.impl.SimpleTransfer
import com.everis.everledger.impl.CC_NOT_PROVIDED
import com.everis.everledger.ifaces.transfer.ILocalTransfer
import com.everis.everledger.impl.ILPSpec2LocalTransferID
import org.web3j.crypto.CipherException
import org.web3j.crypto.Credentials
import org.web3j.crypto.WalletUtils
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameterName
import org.web3j.protocol.core.Request
import org.web3j.protocol.core.methods.response.EthGetBalance
import org.web3j.protocol.core.methods.response.TransactionReceipt
import org.web3j.protocol.http.HttpService
import org.web3j.tx.Transfer
import org.web3j.utils.Convert
import java.math.BigDecimal
import java.math.BigInteger

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
private val accountManager  = SimpleAccountManager

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
        setAffectedAccounts.add(transfer.txInput .localAccount.localID)
        setAffectedAccounts.add(transfer.txOutput.localAccount.localID)

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
    private val transferMap  = HashMap<ILocalTransfer.LocalTransferID, IfaceTransfer>() // In-memory database of pending/executed/cancelled transfers

    fun developerTestingResetTransfers() { // TODO:(?) Make static?
        if (! Config.unitTestsActive) {
            throw RuntimeException("developer.unitTestsActive must be true @ application.conf "
                    + "to be able to reset tests")
        }
        transferMap.clear()
    }


    // START IfaceLocalTransferManager implementation {
    override fun getTransferById(transferId : ILocalTransfer.LocalTransferID ) : IfaceTransfer  {
        val result = transferMap[transferId] ?: throw ILPExceptionSupport.createILPNotFoundException("transfer '${transferId.uniqueID}' not found")
        if (result.transferStatus == TransferStatus.REJECTED) {
            throw ILPExceptionSupport.createILPUnprocessableEntityException(
                    "This transfer has already been rejected")
        }

        return result
    }

    override fun executeLocalTransfer(transfer : IfaceTransfer ) : IfaceTransfer {
        // AccountUri sender, AccountUri recipient, MonetaryAmount amount)
        // STEP 1: Pass all debits to first account.
        __executeLocalTransfer(
                transfer.txInput .localAccount,
                transfer.txOutput.localAccount, transfer.amount)

        return (transfer as SimpleTransfer).copy(
                    _transferStatus=TransferStatus.EXECUTED,
                    DTTM_prepared=ZonedDateTime.now(),
                    DTTM_executed=ZonedDateTime.now() )
    }

    override fun doesTransferExists(transferId : ILocalTransfer.LocalTransferID) :Boolean {
        return transferMap.containsKey(transferId)
    }

    // } END IfaceLocalTransferManager implementation

    // START IfaceILPSpecTransferManager implementation {
    override fun getTransfersByExecutionCondition(condition : Condition ) :
            MutableList<IfaceTransfer> {
        // For this simple implementation just run over existing transfers until
        val result = ArrayList<IfaceTransfer>()
        //   = HashMap<LocalTransferID, IfaceTransfer>();// In-memory database of pending/executed/cancelled transfers
        for ( transferId : ILocalTransfer.LocalTransferID in transferMap.keys) {
            val transfer = transferMap.get(transferId) ?: throw RuntimeException("null found for transferId "+transferId)
            if (transfer.getExecutionCondition() == condition) {
                result.add(transfer)
            }
        }
        return result
    }

    override fun prepareILPTransfer(newTransfer : IfaceTransfer ) {
        log.debug("createNewRemoteILPTransfer")

        if (doesTransferExists(newTransfer.getTransferID())) {
            throw RuntimeException("trying to create new transfer "
                    + "but transferID '"+newTransfer.getTransferID()+"'already registrered. "
                    + "Check transfer with SimpleLedgerTransferManager.transferExists before invoquing this function")
        }
        log.debug("createNewRemoteILPTransfer newTransfer "+
                newTransfer.getTransferID().uniqueID+", status: "+newTransfer.getTransferStatus().toString())

        transferMap.put(newTransfer.getTransferID(), newTransfer)
        if (newTransfer.executionCondition == CC_NOT_PROVIDED) {
            // local transfer with no execution condition => execute and "forget"
            log.debug("createNewRemoteILPTransfer execute locally and forget")
            executeLocalTransfer(newTransfer)
            return
        }

        // PUT Money on-hold:
        __executeLocalTransfer(newTransfer.txInput.localAccount, accountManager.holdAccountILP, newTransfer.amount)

        // TODO:(0) Next line commented to make tests pass, but looks to be sensible to do so.
        // newTransfer.setTransferStatus(TransferStatus.PROPOSED)
    }


    private fun executeOrCancelILPTransfer(transfer : IfaceTransfer , FF : Fulfillment , isExecution : Boolean  /*false => isCancellation*/) : IfaceTransfer {
        if (isExecution /* => DisburseFunds */) {
            __executeLocalTransfer(sender = accountManager.holdAccountILP, recipient = transfer.txOutput.localAccount, amount = transfer.amount)
            return (transfer as SimpleTransfer).copy(
                    _transferStatus=TransferStatus.EXECUTED,
                    DTTM_executed=ZonedDateTime.now(),
                    executionFF=FF )
        } else /* => Cancellation/Rollback  */ {
            __executeLocalTransfer(sender = accountManager.holdAccountILP, recipient = transfer.txInput.localAccount, amount = transfer.amount)
            return (transfer as SimpleTransfer).copy(
                    _transferStatus=TransferStatus.REJECTED,
                    DTTM_executed=ZonedDateTime.now(),
                    executionFF=FF )
        }
        notifyUpdate(transfer = transfer, fulfillment = FF, isExecution = isExecution)
    }

    // TODO:(?) Update returned instance in ddbb if needed
    override fun executeILPTransfer(transfer : IfaceTransfer,    executionFulfillment : Fulfillment ) : IfaceTransfer {
        return executeOrCancelILPTransfer(transfer, executionFulfillment, true)
    }

    // TODO:(?) Update returned instance in ddbb if needed
    override fun cancelILPTransfer (transfer : IfaceTransfer, cancellationFulfillment : Fulfillment ) : IfaceTransfer {
        return executeOrCancelILPTransfer(transfer, cancellationFulfillment, false)
    }

    override fun doesTransferExists(transferId : UUID ) : Boolean  {
        return doesTransferExists(ILPSpec2LocalTransferID(transferId))
    }

    // } END IfaceILPSpecTransferManager implementation

    private fun __executeLocalTransfer(sender : IfaceLocalAccount , recipient : IfaceLocalAccount , amount : MonetaryAmount ) {
        // TODO: LOG local transfer execution.
        val Euro2wei  = BigInteger("1000000"); // TODO:(0)
        val weiAmount = BigInteger(amount.number.toString()).times(Euro2wei)
        log.info("executeLocalTransfer {")
        val ethereumTXHashID = sendTransfer(
            sender   .getLocalID(),
            recipient.getLocalID(),
            weiAmount,
            transferUnit =  Convert.Unit.WEI)
        log.info("} executeLocalTransfer")
    }

    // UnitTest / function test realated code
    fun unitTestsResetTransactionDDBB() {
        transferMap.clear()
    }

    fun unitTestsGetTotalTransactions() : String {
        return "${transferMap.keys.size}"
    }

    // IfaceEthereumBlockchain START {
    // See org.web3j.console.WalletSendFunds ... for reference
    val web3j = Web3j.build(HttpService())

    private fun loadWalletFile(walletFile: File): Credentials {
        while (true) {
            val password = "1234" // TODO:(0)
            try {
                return WalletUtils.loadCredentials(password, walletFile)
            } catch (e: CipherException) {
                throw RuntimeException("Invalid password specified for wallet\n")
            }
        }
    }

    private fun getCredentials(walletFilePath: String): Credentials {
        // TODO:(0) cache crenditials
        val walletFile = File(walletFilePath)
        if (!walletFile.exists() || !walletFile.isFile) {
            // TODO:(0): Launch ILP exception?
            throw RuntimeException("Unable to read wallet file: " + walletFile)
        }

        return loadWalletFile(walletFile)
    }

    private fun performTransfer(
            web3j: Web3j, destinationAddress: String, credentials: Credentials,
            amountInWei: BigInteger): TransactionReceipt {

        log.info("Commencing transfer (this may take a few minutes) ")
        try {
            val future = Transfer.sendFundsAsync(
                    web3j, credentials, destinationAddress, BigDecimal(amountInWei), Convert.Unit.WEI)

            while (!future.isDone) {
                Thread.sleep(500)
            }
            return future.get()
        } catch (e: Exception) {
            throw RuntimeException("Problem encountered transferring funds: \n" + e.message)
        }
    }

    override fun /*receipt*/sendTransfer(
            walletFilePath : String,
            destinationAddress : String,
            weiAmountToTransfer : BigInteger,
            transferUnit: Convert.Unit) : String  {
        val credentials = getCredentials(walletFilePath)
        // console.printf("Wallet for address " + credentials.getAddress() + " loaded\n")

        if (!WalletUtils.isValidAddress(destinationAddress)) {
            // TODO:(0) Launch ILP exception?
            throw RuntimeException("Invalid destination address specified")
        }

        // val amountInWei = Convert.toWei(weiAmountToTransfer, transferUnit)

        val transactionReceipt = performTransfer(
                web3j, destinationAddress, credentials, weiAmountToTransfer)

        log.info("Funds have been successfully transferred from %s to %s%n" + "Transaction hash: %s%nMined block number: %s%n",
                credentials.getAddress(),
                destinationAddress,
                transactionReceipt.getTransactionHash(),
                transactionReceipt.getBlockNumber())
        return transactionReceipt.getTransactionHash();
    }

    override  fun getWeiBalance(walletFilePath : String , transferUnit: Convert.Unit): BigInteger{
        val credentials = getCredentials(walletFilePath)
        val result  : EthGetBalance = web3j.ethGetBalance(credentials.address, DefaultBlockParameterName.LATEST).send()
        return result.balance


    }

    // } IfaceEthereumBlockchain END
}

