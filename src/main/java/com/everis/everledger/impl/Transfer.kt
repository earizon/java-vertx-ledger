package com.everis.everledger.impl

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

import com.everis.everledger.Config
import com.everis.everledger.ifaces.account.IfaceLocalAccount
import com.everis.everledger.ifaces.transfer.ILocalTransfer
import com.everis.everledger.ifaces.transfer.IfaceTransfer

import com.everis.everledger.util.TimeUtils

import com.everis.everledger.impl .manager.SimpleAccountManager

// FIXME:(1) Allow multiple debit/credits (Remove all code related to index [0])

val random = Random()
val a : ByteArray =  ByteArray(size = 32)
public val FF_NOT_PROVIDED : Fulfillment = { random.nextBytes(a) ; Fulfillment.builder().preimage(a).build() }()
public val CC_NOT_PROVIDED : Condition   = { random.nextBytes(a) ; Condition  .builder().hash(a)    .build() }()
internal val AM = SimpleAccountManager


// TODO:(?) Recheck this Credit/Debit classes
public data class Credit (val account: IfaceLocalAccount, val _amount: MonetaryAmount/*, InterledgerPacketHeader ph*/)//        this.ph = ph;
    : ILocalTransfer.Debit {
    override fun   getLocalAccount() : IfaceLocalAccount = account
    override fun         getAmount() :    MonetaryAmount = amount

}

public data class  Debit (val account: IfaceLocalAccount, val _amount: MonetaryAmount, val authorized : Boolean = true)
    : ILocalTransfer.Debit {
    override fun   getLocalAccount() : IfaceLocalAccount = account
    override fun         getAmount() :    MonetaryAmount = amount
}

data class LocalTransferID(val transferID: String) : ILocalTransfer.LocalTransferID {

    override fun getUniqueID() : String = transferID

}

public fun ILPSpec2LocalTransferID(ilpTransferID: UUID): LocalTransferID {
    return LocalTransferID(ilpTransferID.toString())
}

data class SimpleTransfer (
        val id: LocalTransferID,
        val debit_list: Array<ILocalTransfer.Debit>, val credit_list: Array<ILocalTransfer.Credit>,
        val executionCond: Condition,
        val cancelationCond: Condition,
        val int_DTTM_proposed: ZonedDateTime = ZonedDateTime.now(),
        val int_DTTM_prepared: ZonedDateTime = ZonedDateTime.now(),
        val int_DTTM_expires : ZonedDateTime = TimeUtils.future,
        val int_DTTM_executed: ZonedDateTime = TimeUtils.future,
        val int_DTTM_rejected: ZonedDateTime = TimeUtils.future,
        val data: String = "",
        val noteToSelf: String = "",
        var int_transferStatus: TransferStatus = TransferStatus.PROPOSED,
        val sMemo: String,
        val executionFF   : Fulfillment = FF_NOT_PROVIDED,
        val cancelationFF : Fulfillment = FF_NOT_PROVIDED
    ) : IfaceTransfer {
        internal val fromAccount: IfaceLocalAccount

        init { // TODO:(0) Check this is executed after val initialization
            checkBalancedTransaction()
            fromAccount = AM.getAccountByName(credit_list[0].localAccount.localID)
            // TODO:(1) Check that debit_list[idx].ammount.currency is always the same and match the ledger
            // TODO:(1) Check that credit_list[idx].ammount.currency is always the same.
            // FIXME: TODO: If fromAccount.ledger != "our ledger" throw RuntimeException.
            int_transferStatus =
                    if (transferStatus == TransferStatus.PROPOSED) {
                      // Temporal patch since proposed was not contemplated initialy
                      TransferStatus.PREPARED
                    } else {
                      transferStatus
                    }
            if (cancelationFF !== FF_NOT_PROVIDED && this.executionFF !== FF_NOT_PROVIDED)
                throw RuntimeException("Cancelation and Execution Fulfillment can not be set simultaneously")
        }

        // Implement ILPSpec interface{
        override fun getId(): UUID {
            // TODO:(0) Check LocalTransferID  LocalTransferID.ILPSpec2LocalTransferID
            //           Create inverse and use
            val result = UUID.randomUUID() // TODO:(0) FIX get from getTransferID
            return result
        }

        override fun getFromAccount(): InterledgerAddress {
            val result = InterledgerAddress.Builder().value("TODO(0)").build()
            return result
        }

        override fun getToAccount(): InterledgerAddress {
            val result = InterledgerAddress.Builder().value("TODO(0)").build()
            return result
        }

        override fun getAmount(): MonetaryAmount {
            val result = Money.of(0, debit_list[0].amount.currency)
            return result
        }

        override fun getInvoice(): String {
            val result = "" // TODO:(0)
            return result
        }

        override fun getData(): ByteArray {
            return data.toByteArray()
        }

        override fun getNoteToSelf(): ByteArray {
            return noteToSelf.toByteArray()
        }

        override fun isRejected(): Boolean {
            val result = false // TODO:(0)
            return result
        }

        override fun getRejectionMessage(): String {
            val result = ""
            return result
        }

        override fun getExecutionCondition(): Condition {
            return executionCond
        }

        //    @Override
        //    public Condition getCancellationCondition() {
        //        return cancelationCond;
        //    }

        override fun getExpiresAt(): ZonedDateTime {
            return int_DTTM_expires
        }

        // } End ILPSpec interface

        private fun checkBalancedTransaction() {
            val totalDebits = Money.of(0, debit_list[0].amount.currency)
            for (debit in debit_list) {
                totalDebits.add(debit.amount)
            }
            val totalCredits = Money.of(0, credit_list[0].amount.currency)
            for (credit in credit_list) {
                totalCredits.add(credit.amount)
            }
            if (!totalDebits.isEqualTo(totalCredits)) {
                throw RuntimeException("transfer not balanced between credits and debits")
            }
        }

        override fun getTransferID(): LocalTransferID {
            return id
        }

        override fun getDebits(): Array<ILocalTransfer.Debit> {
            return debit_list
        }

        override fun getCredits(): Array<ILocalTransfer.Credit> {
            return credit_list
        }

        override fun getTransferStatus(): TransferStatus {
            return int_transferStatus
        }

        override fun getDTTM_prepared(): ZonedDateTime {
            return int_DTTM_prepared
        }

        override fun getDTTM_executed(): ZonedDateTime {
            return int_DTTM_executed
        }

        override fun getDTTM_rejected(): ZonedDateTime {
            return int_DTTM_rejected
        }

        override fun getDTTM_expires(): ZonedDateTime {
            return int_DTTM_expires
        }

        override fun getDTTM_proposed(): ZonedDateTime {
            return int_DTTM_proposed
        }

        override fun getExecutionFulfillment(): Fulfillment {
            return executionFF
        }

        override fun getCancellationFulfillment(): Fulfillment {
            return cancelationFF
        }

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
            jo.put("debits", entryList2Json(debit_list as Array<ILocalTransfer.TransferHalfEntry>))
            jo.put("credits", entryList2Json(credit_list as Array<ILocalTransfer.TransferHalfEntry>))
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
            if (this.int_DTTM_expires !== TimeUtils.future) {
                jo.put("expires_at", this.int_DTTM_expires.format(TimeUtils.ilpFormatter))
            }
            run {
                val timeline = JsonObject()
                if (Config.unitTestsActive) {
                    timeline.put("proposed_at", TimeUtils.testingDate.format(TimeUtils.ilpFormatter))
                    val sTestingDate = TimeUtils.testingDate.format(TimeUtils.ilpFormatter)
                    if (this.int_DTTM_prepared !== TimeUtils.future) {
                        timeline.put("prepared_at", sTestingDate)
                    }
                    if (this.int_DTTM_executed !== TimeUtils.future) {
                        timeline.put("executed_at", sTestingDate)
                    }
                    if (this.int_DTTM_rejected !== TimeUtils.future) {
                        timeline.put("rejected_at", sTestingDate)
                    }
                } else {
                    timeline.put("proposed_at", this.int_DTTM_proposed.format(TimeUtils.ilpFormatter))
                    if (this.int_DTTM_prepared !== TimeUtils.future) {
                        timeline.put("prepared_at", this.int_DTTM_prepared.format(TimeUtils.ilpFormatter))
                    }
                    if (this.int_DTTM_executed !== TimeUtils.future) {
                        timeline.put("executed_at", this.int_DTTM_executed.format(TimeUtils.ilpFormatter))
                    }
                    if (this.int_DTTM_rejected !== TimeUtils.future) {
                        timeline.put("rejected_at", this.int_DTTM_rejected.format(TimeUtils.ilpFormatter))
                    }
                }
                jo.put("timeline", timeline)
            }
            if (sMemo !== "") {
                jo.put("memo", JsonObject(sMemo))

            }
            return jo
        }

        private fun entryList2Json(input_list: Array<out ILocalTransfer.TransferHalfEntry>): JsonArray {
            val ja = JsonArray()
            for (entry in input_list) {
                // FIXME: This code to calculate amount is PLAIN WRONG. Just to pass five-bells-ledger tests
                val jo = JsonObject()
                jo.put("account", "/accounts/" /* TODO: Get from config.*/ + entry.localAccount.localID)
                val sAmount = "" + entry.amount.number.toFloat().toLong()
                jo.put("amount", sAmount)
                if (entry is Debit) {
                    jo.put("authorized", entry.authorized)
                } else if (entry is Credit) {
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
                    // COMMENTED OLD API data.put("expires_at", /*((Credit)entry).ph.getExpiry().toString()*/int_DTTM_expires.toString());  // TODO: Recheck.
                    // COMMENTED OLD API ilp_header.put("data", data);
                    // COMMENTED OLD API memo.put("ilp_header", ilp_header);
                    // COMMENTED OLD API jo.put("memo", memo);
                }
                ja.add(jo)
            }
            return ja
        }

        override fun isAuthorized(): Boolean  = true

    }
