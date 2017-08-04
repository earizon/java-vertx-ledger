package com.everis.everledger.handlers

import com.everis.everledger.Config
import com.everis.everledger.ifaces.account.IfaceLocalAccountManager
import com.everis.everledger.ifaces.transfer.IfaceTransferManager
import com.everis.everledger.impl.CC_NOT_PROVIDED
import com.everis.everledger.impl.FF_NOT_PROVIDED
import com.everis.everledger.impl.SimpleTransfer
import com.everis.everledger.impl.manager.SimpleAccountManager
import com.everis.everledger.impl.manager.SimpleTransferManager
import com.everis.everledger.transfer.Credit
import com.everis.everledger.transfer.Debit
import com.everis.everledger.transfer.LocalTransferID
import com.everis.everledger.util.AuthManager
import com.everis.everledger.util.ConversionUtil
import com.everis.everledger.util.ILPExceptionSupport
import com.everis.everledger.util.TimeUtils
import io.netty.handler.codec.http.HttpResponseStatus
import io.vertx.core.http.HttpHeaders
import io.vertx.core.http.HttpMethod
import io.vertx.core.logging.LoggerFactory
import io.vertx.ext.web.RoutingContext
import org.interledger.Condition
import org.interledger.ilp.InterledgerError
import org.interledger.ledger.model.TransferStatus
import org.javamoney.moneta.Money
import java.net.URI
import java.time.ZonedDateTime
import java.util.*
import javax.money.Monetary
import javax.money.MonetaryAmount

private val AM : IfaceLocalAccountManager = SimpleAccountManager;
private val TM : IfaceTransferManager     = SimpleTransferManager;
private val currencyUnit /* local ledger currency */ = Monetary.getCurrency(Config.ledgerCurrencyCode)

class TransferHandler
// GET|PUT /transfers/3a2a1d9e-8640-4d2d-b06c-84f2cd613204
private constructor() : RestEndpointHandler(
        arrayOf(HttpMethod.GET, HttpMethod.PUT, HttpMethod.POST), arrayOf("transfers/:" + transferUUID )) {


    override fun handlePut(context: RoutingContext) {
        val ai = AuthManager.authenticate(context)
        val requestBody = RestEndpointHandler.getBodyAsJson(context)

        var transferMatchUser = false
        log.trace(this.javaClass.name + "handlePut invoqued ")
        log.trace(context.bodyAsString)
        /*
         * REQUEST: PUT /transfers/3a2a1d9e-8640-4d2d-b06c-84f2cd613204 HTTP/1.1
         * Authorization: Basic YWxpY2U6YWxpY2U=
         * {"id":"http://localhost/transfers/3a2a1d9e-8640-4d2d-b06c-84f2cd613204"
         * , "ledger":"http://localhost", "debits":[
         * {"account":"http://localhost/accounts/alice","amount":"50"},
         * {"account":"http://localhost/accounts/candice","amount":"20"}],
         * "credits":[
         * {"account":"http://localhost/accounts/bob","amount":"30"},
         * {"account":"http://localhost/accounts/dave","amount":"40"}],
         * "execution_condition"
         * :"cc:0:3:Xk14jPQJs7vrbEZ9FkeZPGBr0YqVzkpOYjFp_tIZMgs:7",
         * "expires_at":"2015-06-16T00:00:01.000Z", "state":"prepared"} ANSWER:
         * HTTP/1.1 201 Created
         * {"id":"http://localhost/transfers/3a2a1d9e-8640-4d2d-b06c-84f2cd613204"
         * , "ledger":..., "debits":[ ... ] "credits":[ ... ]
         * "execution_condition":"...", "expires_at":..., "state":"proposed",
         * "timeline":{"proposed_at":"2015-06-16T00:00:00.000Z"} }
         */

        val ilpTransferID: UUID
        try {
            ilpTransferID = UUID.fromString(context.request().getParam(transferUUID))
        } catch (e: Exception) {
            throw ILPExceptionSupport.createILPBadRequestException(
                    "'" + context.request().getParam(transferUUID) + "' is not a valid UUID")
        }

        val transferID = LocalTransferID.ILPSpec2LocalTransferID(ilpTransferID)

        // TODO: Check requestBody.getString("ledger") match ledger host/port

        // TODO: Check state is 'proposed' for new transactions?

        // TODO:(?) mark as "Tainted" object
        val debits = requestBody.getJsonArray("debits") ?: throw ILPExceptionSupport.createILPBadRequestException("debits not found")

        if (debits.size() != 1) {
            throw ILPExceptionSupport.createILPBadRequestException("Only one debitor supported by ledger")
        }
        var totalDebitAmmount = 0.0
        val debitList = Array<Debit>(debits.size(), { idx ->
            val jsonDebit = debits.getJsonObject(idx)
            log.debug("check123 jsonDebit: " + jsonDebit.encode())
            // debit0 will be similar to
            // {"account":"http://localhost/accounts/alice","amount":"50"}
            var account_name = jsonDebit.getString("account")
            if (account_name.lastIndexOf('/') > 0) {
                account_name = account_name.substring(account_name.lastIndexOf('/') + 1)
            }
            if (ai.getId() == account_name) { transferMatchUser = true }
            val debit_ammount: MonetaryAmount = try {
                val auxDebit = java.lang.Double.parseDouble(jsonDebit.getString("amount"))
                totalDebitAmmount += auxDebit
                Money.of(auxDebit, currencyUnit)
            } catch (e: Exception) {
                println(e.toString())
                throw ILPExceptionSupport.createILPBadRequestException("unparseable amount")
            }

            if (debit_ammount.getNumber().toFloat().toDouble() == 0.0) {
                throw ILPExceptionSupport.createILPException(422, InterledgerError.ErrorCode.F00_BAD_REQUEST, "debit is zero")
            }
            val debitor = AM.getAccountByName(account_name)
            log.debug("check123 debit_ammount (must match jsonDebit ammount: " + debit_ammount.toString())
            Debit(debitor, debit_ammount)
        })
        if (!ai.isAdmin && !transferMatchUser) {
            throw ILPExceptionSupport.createILPForbiddenException()
        }
        val jsonMemo = requestBody.getJsonObject("memo")
        val sMemo = if (jsonMemo == null) "" else jsonMemo.encode()
        // REF: JsonArray ussage:
        // http://www.programcreek.com/java-api-examples/index.php?api=io.vertx.core.json.JsonArray
        val credits = requestBody.getJsonArray("credits")

        val sExpiresAt = requestBody.getString("expires_at") // can be null
        val DTTM_expires: ZonedDateTime
        try {
            DTTM_expires = if (sExpiresAt == null) TimeUtils.future else ZonedDateTime.parse(sExpiresAt)
        } catch (e: Exception) {
            throw ILPExceptionSupport.createILPBadRequestException("unparseable expires_at")

        }

        val execution_condition = requestBody
                .getString("execution_condition")
        val URIExecutionCond: Condition = if (execution_condition != null)
            ConversionUtil.parseURI(URI.create(execution_condition))
            else CC_NOT_PROVIDED
        var totalCreditAmmount = 0.0
        val creditList = Array<Credit>(credits.size() , { idx ->
            val jsonCredit = credits.getJsonObject(idx)
            /* { "account":"http://localhost:3002/accounts/ilpconnector",
             * "amount":"1.01", "memo":{ "ilp_header":{
             * "account":"ledger3.eur.alice.fe773626-81fb-4294-9a60-dc7b15ea841e"
             * , "amount":"1", "data":{"expires_at":"2016-11-10T15:51:27.134Z"}
             * } } } */
            // JsonObject jsonMemoILPHeader = jsonCredit.getJsonObject("memo")
            // COMMENTED OLD API         .getJsonObject("ilp_header");
            var account_name = jsonCredit.getString("account")
            if (account_name.lastIndexOf('/') > 0) {
                account_name = account_name.substring(account_name.lastIndexOf('/') + 1)
            }
            val credit_ammount: MonetaryAmount = try {
                val auxCredit = java.lang.Double.parseDouble(jsonCredit.getString("amount"))
                totalCreditAmmount += auxCredit
                Money.of(auxCredit, currencyUnit)
            } catch (e: Exception) {
                throw ILPExceptionSupport.createILPBadRequestException("unparseable amount")
            }

            if (credit_ammount.getNumber().toFloat().toDouble() == 0.0) {
                throw ILPExceptionSupport.createILPException(422,
                        InterledgerError.ErrorCode.F00_BAD_REQUEST, "credit is zero")
            }
            val creditor = AM.getAccountByName(account_name)
            // COMMENTED OLD API String ilp_ph_ilp_dst_address = jsonMemoILPHeader
            // COMMENTED OLD API         .getString("account");

            // COMMENTED OLD API InterledgerAddress dstAddress = InterledgerAddressBuilder.builder()
            // COMMENTED OLD API         .value(ilp_ph_ilp_dst_address).build();
            // COMMENTED OLD API String ilp_ph_amount = jsonMemoILPHeader.getString("amount");
            // COMMENTED OLD API BigDecimal ammount = new BigDecimal(ilp_ph_amount);
            // COMMENTED OLD API Condition ilp_ph_condition = URIExecutionCond;
            // COMMENTED OLD API DTTM ilp_ph_expires = new DTTM(jsonMemoILPHeader.getJsonObject(
            // COMMENTED OLD API         "data").getString("expires_at"));
            // COMMENTED OLD API if (!DTTM_expires.equals(ilp_ph_expires)) {
            // COMMENTED OLD API     DTTM_expires = ilp_ph_expires;
            // COMMENTED OLD API }
            // COMMENTED OLD API ZonedDateTime zdt = ZonedDateTime.parse((DTTM_expires.toString()));
            // InterledgerPacketHeader(InterledgerAddress destinationAddress,
            // BigDecimal amount,
            // Condition condition, ZonedDateTime expiry)
            // COMMENTED OLD API InterledgerPacketHeader memo_ph = new InterledgerPacketHeader(
            // COMMENTED OLD API         dstAddress, ammount, ilp_ph_condition, zdt);
            // In five-bells-ledger, memo goes into transfer_adjustments table (@ src/sql/pg/...)
            Credit(creditor, credit_ammount/*, memo_ph*/)
        })
        if (totalCreditAmmount != totalDebitAmmount) {
            throw ILPExceptionSupport.createILPException(422,
                    InterledgerError.ErrorCode.F00_BAD_REQUEST, "total credits do not match total debits")
        }

        val data = "" // Not yet used
        val noteToSelf = "" // Not yet used
        val DTTM_proposed = ZonedDateTime.now() // TODO:(?) Add proposed -> prepared support
        val DTTM_prepared = ZonedDateTime.now() // TODO:(?) Add proposed -> prepared support

        val cancelation_condition = requestBody.getString("cancellation_condition")
        val URICancelationCond =  if (cancelation_condition != null)
            ConversionUtil.parseURI(URI.create(cancelation_condition)) else CC_NOT_PROVIDED
        val status = TransferStatus.PROPOSED // By default
        //        if (requestBody.getString("state") != null) {
        //            // TODO: Must status change be allowed or must we force it to be
        //            // 'prepared'?
        //            // (only Execution|Cancellation Fulfillments will change the state)
        //            // At this moment it's allowed (to make it compliant with
        //            // five-bells-ledger tests)
        //            status = TransferStatus.parse(requestBody.getString("state"));
        //            log.debug("transfer status " + status);
        //        }

        val receivedTransfer = SimpleTransfer(
                transferID, debitList, creditList,
                URIExecutionCond, URICancelationCond,
                DTTM_proposed, DTTM_prepared, DTTM_expires, TimeUtils.future, TimeUtils.future,
                data, noteToSelf,
                status,
                sMemo,
                FF_NOT_PROVIDED, FF_NOT_PROVIDED )

        // TODO:(0) Next logic must be on the Manager, not in the HTTP-protocol related handler
        val isNewTransfer = !TM.doesTransferExists(ilpTransferID)
        log.debug("is new transfer?: " + isNewTransfer)

        val effectiveTransfer = if (isNewTransfer) receivedTransfer else TM.getTransferById(transferID)
        if (!isNewTransfer) {
            // Check that received json data match existing transaction.
            // TODO: Recheck (Multitransfer active now)
            if (effectiveTransfer.credits[0] != receivedTransfer
                    .credits[0] || effectiveTransfer.debits[0] != receivedTransfer.debits[0]) {
                throw ILPExceptionSupport.createILPBadRequestException(
                        "data for credits and/or debits doesn't match existing registry")
            }
        } else {
            TM.createNewRemoteILPTransfer(receivedTransfer)
        }
        try { // TODO:(?) Refactor Next code for notification (next two loops) are
            // duplicated in FulfillmentHandler
            val resource = (effectiveTransfer as SimpleTransfer).toILPJSONStringifiedFormat()

            // Notify affected accounts:
            val eventType = if (receivedTransfer.transferStatus == TransferStatus.PREPARED)
                      TransferWSEventHandler.EventType.TRANSFER_CREATE
                 else TransferWSEventHandler.EventType.TRANSFER_UPDATE
            val setAffectedAccounts = HashSet<String>()
            for (debit in receivedTransfer.debits) setAffectedAccounts.add(debit.account.localID)
            for (credit in receivedTransfer.credits) setAffectedAccounts.add(credit.account.localID)
            TransferWSEventHandler.notifyListener(setAffectedAccounts, eventType, resource, null)
        } catch (e: Exception) {
            log.warn("transaction created correctly but ilp-connector couldn't be notified due to " + e.toString())
        }

        val response = (effectiveTransfer as SimpleTransfer).toILPJSONStringifiedFormat().encode()

        context.response()
                .putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                .putHeader(HttpHeaders.CONTENT_LENGTH, "" + response.length)
                .setStatusCode( if (isNewTransfer)
                             HttpResponseStatus.CREATED.code()
                        else  HttpResponseStatus.OK.code()).
                end(response)
    }

    override fun handleGet(context: RoutingContext) {
        log.debug(this.javaClass.name + "handleGet invoqued ")
        val ai = AuthManager.authenticate(context)
        val ilpTransferID = UUID.fromString(context.request().getParam(transferUUID))
        val transfer = TM.getTransferById(LocalTransferID.ILPSpec2LocalTransferID(ilpTransferID))

        val debit0_account = transfer.debits[0].account.localID
        val transferMatchUser = ai.getId() == debit0_account
        if (!transferMatchUser && ai.getRoll() != "admin") {
            log.error("transferMatchUser false: "
                    + "\n    ai.getId()    :" + ai.getId()
                    + "\n    debit0_account:" + debit0_account)
            throw ILPExceptionSupport.createILPForbiddenException()
        }
        response( context, HttpResponseStatus.OK, (transfer as SimpleTransfer).toILPJSONStringifiedFormat())
    }

    companion object {
        private val log = LoggerFactory.getLogger(AccountsHandler::class.java)
        private val transferUUID = "transferUUID";
        fun create(): TransferHandler {
            return TransferHandler()
        }
    }
}
