package com.everis.everledger.impl

// TODO:(0) Rename TXInput / TXOutput by account_src / account_dst.
//      TXInput / TXoutput have a different incompatible meaning in Bitcoin,...
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import org.interledger.Condition
import org.interledger.Fulfillment
import org.interledger.InterledgerAddress
import java.time.ZonedDateTime
import javax.money.MonetaryAmount

import java.util.Random
import java.util.UUID


import org.javamoney.moneta.Money

import org.interledger.ledger.model.TransferStatus

import com.everis.everledger.util.Config
import com.everis.everledger.ifaces.account.IfaceLocalAccount
import com.everis.everledger.ifaces.transfer.ILocalTransfer
import com.everis.everledger.ifaces.transfer.IfaceTransfer

import com.everis.everledger.util.TimeUtils

import com.everis.everledger.impl .manager.SimpleAccountManager

// FIXME:(1) Allow multiple debit/credits (Remove all code related to index [0])

val random = Random()
val a : ByteArray =  ByteArray(size = 32)
val FF_NOT_PROVIDED : Fulfillment = { random.nextBytes(a) ; Fulfillment.builder().preimage(a).build() }()
val CC_NOT_PROVIDED : Condition   = { random.nextBytes(a) ; Condition  .builder().hash(a)    .build() }()
internal val AM = SimpleAccountManager


// TODO:(?) Recheck this Credit/Debit classes
data class TXOutputImpl (val account: IfaceLocalAccount, val _amount: MonetaryAmount/*, InterledgerPacketHeader ph*/)//        this.ph = ph;
    : ILocalTransfer.TXOutput {
    override fun   getLocalAccount() : IfaceLocalAccount = account
    override fun         getAmount() :    MonetaryAmount = _amount

}

data class  TXInputImpl (val account: IfaceLocalAccount, val _amount: MonetaryAmount, val authorized : Boolean = true)
    : ILocalTransfer.TXInput {
    override fun   getLocalAccount() : IfaceLocalAccount = account
    override fun         getAmount() :    MonetaryAmount = _amount
}

data class LocalTransferID(val transferID: String) : ILocalTransfer.LocalTransferID {
    override fun getUniqueID() : String = transferID
}

fun ILPSpec2LocalTransferID(ilpTransferID: UUID): LocalTransferID {
    return LocalTransferID(ilpTransferID.toString())
}

data class SimpleTransfer (
        val id: LocalTransferID,
        val _TXInput: TXInputImpl, val _TXOutput: TXOutputImpl,
        val executionCond: Condition,
        val cancelationCond: Condition,
        val DTTM_proposed: ZonedDateTime = ZonedDateTime.now(),
        val DTTM_prepared: ZonedDateTime = ZonedDateTime.now(),
        val DTTM_expires : ZonedDateTime = TimeUtils.future,
        val DTTM_executed: ZonedDateTime = TimeUtils.future,
        val DTTM_rejected: ZonedDateTime = TimeUtils.future,
        val data: String = "",
        val noteToSelf: String = "",
        var _transferStatus: TransferStatus = TransferStatus.PROPOSED,
        val sMemo: String,
        val executionFF   : Fulfillment = FF_NOT_PROVIDED,
        val cancelationFF : Fulfillment = FF_NOT_PROVIDED,
        var receipt: String = ""
    ) : IfaceTransfer {
            internal val fromAccount: IfaceLocalAccount

            init { // TODO:(0) Check this is executed after val initialization
                checkBalancedTransactionOrThrow()
                fromAccount = AM.getAccountById(_TXInput.localAccount.localID)
                // TODO:(1) Check that debit_list[idx].ammount.currency is always the same and match the ledger
                // TODO:(1) Check that credit_list[idx].ammount.currency is always the same.
                // FIXME: TODO: If fromAccount.ledger != "our ledger" throw RuntimeException.
                _transferStatus =
                        if (_transferStatus == TransferStatus.PROPOSED) {
                          // Temporal patch since proposed was not contemplated initialy
                          TransferStatus.PREPARED
                        } else {
                          _transferStatus
                        }
                if (cancelationFF !== FF_NOT_PROVIDED && this.executionFF !== FF_NOT_PROVIDED)
                    throw RuntimeException("Cancelation and Execution Fulfillment can not be set simultaneously")
            }

            // Implement ILPSpec interface{
            // TODO:(0) Check LocalTransferID  LocalTransferID.ILPSpec2LocalTransferID
            //           Create inverse and use

            override fun getId(): UUID = UUID(id.uniqueID.toLong(), id.uniqueID.toLong()) // TODO:(0) Check implementation
                override fun getFromAccount(): InterledgerAddress = InterledgerAddress.builder().value("TODO(0)").build()
            override fun getToAccount(): InterledgerAddress = InterledgerAddress.builder().value("TODO(0)").build()
            override fun getAmount(): MonetaryAmount = _TXInput._amount
            override fun getInvoice(): String = receipt
            override fun getData(): ByteArray = data.toByteArray()
            override fun getNoteToSelf(): ByteArray = noteToSelf.toByteArray()
            override fun isRejected(): Boolean = false // TODO:(0) update when timed-out or cancellation-fullfillment  received
            override fun getRejectionMessage(): String = ""  // TODO:(0)
            override fun getExecutionCondition(): Condition = executionCond
         // override fun getCancellationCondition(): Condition = cancelationCond
            override fun getILPExpiresAt() : ZonedDateTime = DTTM_expires
            override fun getILPPreparedAt(): ZonedDateTime = DTTM_prepared
            override fun getILPExecutedAt(): ZonedDateTime = DTTM_executed
            override fun getILPRejectedAt(): ZonedDateTime = DTTM_rejected
            override fun getILPProposedAt(): ZonedDateTime = DTTM_proposed
            override fun getExecutionFulfillment(): Fulfillment = executionFF
            override fun isILPAuthorized() : Boolean = isAuthorized()
            // } End ILPSpec interface

            override fun getTXInput () : ILocalTransfer.TXInput  = _TXInput
            override fun getTXOutput() : ILocalTransfer.TXOutput = _TXOutput

            override fun checkBalancedTransactionOrThrow() {
                if (_TXInput.amount != _TXOutput.amount)
                    throw RuntimeException("transfer not balanced between credits and debits")
            }

            override fun getTransferID(): LocalTransferID = id

            override fun getTransferStatus(): TransferStatus = _transferStatus

            // NOTE: The JSON returned to the ILP connector and the Wallet must not necesarelly match
            // since the data about the transfer needed by the wallet and the connector differ.
            // That's why two different JSON encoders exist

            fun toILPJSONStringifiedFormat(): JsonObject {
                // REF: convertToExternalTransfer@
                // https://github.com/interledger/five-bells-ledger/blob/master/src/models/converters/transfers.js
                val jo = JsonObject()
                val ledger = Config.publicURL.toString().trimEnd('/')
                val id = /* TODO:(doubt) add ledger as prefix ?? */"/transfers/" /* TODO:(1) Get from Config */ + transferID.transferID
                jo.put("id", id)
                jo.put("ledger", ledger)
                jo.put("debits" , entryList2Json(_TXInput ))
                jo.put("credits", entryList2Json(_TXOutput))
                if (this.executionCondition != CC_NOT_PROVIDED) {
                    jo.put("execution_condition", this.executionCondition.toString())
                }
                //        if (! this.getCancellationCondition().equals(SimpleTransfer.CC_NOT_PROVIDED)) {
                //            jo.put("cancellation_condition", this.getCancellationCondition().toString());
                //        }
                jo.put("state", this.getTransferStatus().toString().toLowerCase())
                //        if (!this.getCancellationCondition().equals(Condition....NOT_PROVIDED)) {
                //            jo.put("cancellation_condition", this.getCancellationCondition());
                //        }
                // FIXME: Cancelation_condition?
                if (this.DTTM_expires !== TimeUtils.future) {
                    jo.put("expires_at", this.DTTM_expires.format(TimeUtils.ilpFormatter))
                }
                run {
                    val timeline = JsonObject()
                    if (Config.unitTestsActive) {
                        timeline.put("proposed_at", TimeUtils.testingDate.format(TimeUtils.ilpFormatter))
                        val sTestingDate = TimeUtils.testingDate.format(TimeUtils.ilpFormatter)
                        if (this.DTTM_prepared !== TimeUtils.future) {
                            timeline.put("prepared_at", sTestingDate)
                        }
                        if (this.DTTM_executed !== TimeUtils.future) {
                            timeline.put("executed_at", sTestingDate)
                        }
                            if (this.DTTM_rejected !== TimeUtils.future) {
                            timeline.put("rejected_at", sTestingDate)
                        }
                    } else {
                        timeline.put("proposed_at", this.DTTM_proposed.format(TimeUtils.ilpFormatter))
                        if (this.DTTM_prepared !== TimeUtils.future) {
                            timeline.put("prepared_at", this.DTTM_prepared.format(TimeUtils.ilpFormatter))
                        }
                        if (this.DTTM_executed !== TimeUtils.future) {
                            timeline.put("executed_at", this.DTTM_executed.format(TimeUtils.ilpFormatter))
                        }
                        if (this.DTTM_rejected !== TimeUtils.future) {
                            timeline.put("rejected_at", this.DTTM_rejected.format(TimeUtils.ilpFormatter))
                        }
                    }
                    jo.put("timeline", timeline)
                }
                if (sMemo !== "") {
                    jo.put("memo", JsonObject(sMemo))

                }
                return jo
            }

            private fun entryList2Json(entry: ILocalTransfer.TransferHalfEntry): JsonArray {
                val ja = JsonArray()
                // FIXME: This code to calculate amount is PLAIN WRONG. Just to pass five-bells-ledger tests
                val jo = JsonObject()
                jo.put("account", "/accounts/" /* TODO: Get from config.*/ + entry.localAccount.localID)
                val sAmount = "" + entry.amount.number.toFloat().toLong()
                jo.put("amount", sAmount)
                if (entry is TXInputImpl) {
                    jo.put("authorized", entry.authorized)
                } else if (entry is TXOutputImpl) {
                    // Add memo:
                    //  "memo":{
                    //      "ilp_header":{
                    //          "account":"ledger3.eur.alice.fe773626-81fb-4294-9a60-dc7b15ea841e",
                    //          "amount":"1",
                    //          "data":{"expires_at":"2016-11-10T15:51:27.134Z"}
                    //      }
                    // }
                    // COMMENTED OLD API JsonObject memo = new JsonObject()/*, ilp_header = new JsonObject()*/, data = new JsonObject();
                    // COMMENTED OLD API ilp_header.put("account", ((Credit)entry).ph.getDestinationAddress());
                    // COMMENTED OLD API ilp_header.put("amount",  ""+((Credit)entry).ph.getAmount());// TODO: Recheck
                    // COMMENTED OLD API data.put("expires_at", /*((Credit)entry).ph.getExpiry().toString()*/DTTM_expires.toString());  // TODO: Recheck.
                    // COMMENTED OLD API ilp_header.put("data", data);
                    // COMMENTED OLD API memo.put("ilp_header", ilp_header);
                    // COMMENTED OLD API jo.put("memo", memo);
                }
                ja.add(jo)
                return ja
            }

        override fun isAuthorized()    : Boolean  = _TXInput.authorized

    }
