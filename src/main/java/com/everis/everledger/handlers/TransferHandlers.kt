package com.everis.everledger.handlers

import com.everis.everledger.util.Config
import com.everis.everledger.ifaces.account.IfaceLocalAccountManager
import com.everis.everledger.ifaces.transfer.ILocalTransfer
import com.everis.everledger.ifaces.transfer.IfaceTransferManager
import com.everis.everledger.impl.*
import com.everis.everledger.impl.manager.SimpleAccountManager
import com.everis.everledger.impl.manager.SimpleTransferManager
import com.everis.everledger.util.*
import io.netty.handler.codec.http.HttpResponseStatus
import io.vertx.core.http.HttpHeaders
import io.vertx.core.http.HttpMethod
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.LoggerFactory
import io.vertx.ext.web.RoutingContext
import org.interledger.Condition
import org.interledger.Fulfillment
import org.interledger.ilp.InterledgerError
import org.interledger.ledger.model.TransferStatus
import org.javamoney.moneta.Money
import java.net.URI
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
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

        val transferID : ILocalTransfer.LocalTransferID = ILPSpec2LocalTransferID(ilpTransferID)

        // TODO: Check requestBody.getString("ledger") match ledger host/port

        // TODO: Check state is 'proposed' for new transactions?

        // TODO:(?) mark as "Tainted" object
        val debits = requestBody.getJsonArray("debits") ?: throw ILPExceptionSupport.createILPBadRequestException("debits not found")

        if (debits.size() != 1) {
            throw ILPExceptionSupport.createILPBadRequestException("Only one debitor supported by ledger")
        }
        var totalDebitAmmount = 0.0
        val debitList = Array<ILocalTransfer.Debit>(debits.size(), { idx ->
            val jsonDebit = debits.getJsonObject(idx)
            log.debug("check123 jsonDebit: " + jsonDebit.encode())
            // debit0 will be similar to
            // {"account":"http://localhost/accounts/alice","amount":"50"}
            var account_name = jsonDebit.getString("account")
            if (account_name.lastIndexOf('/') > 0) {
                account_name = account_name.substring(account_name.lastIndexOf('/') + 1)
            }
            if (ai.id  == account_name) { transferMatchUser = true }
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

        val execution_condition = requestBody.getString("execution_condition")
        val URIExecutionCond: Condition = if (execution_condition != null)
            ConversionUtil.parseURI(URI.create(execution_condition))
            else CC_NOT_PROVIDED
        var totalCreditAmmount = 0.0
        val creditList = Array<ILocalTransfer.Credit>(credits.size() , { idx ->
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
            Credit(creditor, credit_ammount/*, memo_ph*/) as ILocalTransfer.Credit
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
                transferID as LocalTransferID, debitList, creditList,
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
            for (debit  in receivedTransfer.debits)  setAffectedAccounts.add((debit  as Debit ).account.localID)
            for (credit in receivedTransfer.credits) setAffectedAccounts.add((credit as Credit).account.localID)
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
        val transfer = TM.getTransferById(ILPSpec2LocalTransferID(ilpTransferID))

        val debit0_account = (transfer.debits[0] as Debit).account.localID
        val transferMatchUser = ai.id == debit0_account
        if (!transferMatchUser && ai.roll != "admin") {
            log.error("transferMatchUser false: "
                    + "\n    ai.id    :" + ai.id
                    + "\n    debit0_account:" + debit0_account)
            throw ILPExceptionSupport.createILPForbiddenException()
        }
        response( context, HttpResponseStatus.OK, (transfer as SimpleTransfer).toILPJSONStringifiedFormat())
    }

    companion object {
        private val log = LoggerFactory.getLogger(AccountsHandler::class.java)
        private val transferUUID = "transferUUID";
        fun create(): TransferHandler =  TransferHandler()
    }
}


// GET /transfers/byExecutionCondition/cc:0:3:vmvf6B7EpFalN6RGDx9F4f4z0wtOIgsIdCmbgv06ceI:7

class TransfersHandler : RestEndpointHandler(arrayOf(HttpMethod.GET), arrayOf("transfers/byExecutionCondition/:" + execCondition)) {
    override fun handleGet(context: RoutingContext) {
        /*
         *  GET /transfers/byExecutionCondition/cc:0:3:vmvf6B7EpFalN...I:7 HTTP/1.1
         *      HTTP/1.1 200 OK
         *      [{"ledger":"http://localhost",
         *        "execution_condition":"cc:0:3:vmvf6B7EpFalN...I:7",
         *        "cancellation_condition":"cc:0:3:I3TZF5S3n0-...:6",
         *        "id":"http://localhost/transfers/9e97a403-f604-44de-9223-4ec36aa466d9",
         *        "state":"executed",
         *        "debits":[
         *          {"account":"http://localhost/accounts/alice","amount":"10","authorized":true}],
         *        "credits":[{"account":"http://localhost/accounts/bob","amount":"10"}]}]
         */
        log.trace(this.javaClass.name + "handleGet invoqued ")
        val ai = AuthManager.authenticate(context)
        var transferMatchUser = false

        //        Condition condition = CryptoConditionUri.parse(URI.create(testVector.getConditionUri()));
        val sExecCond = context.request().getParam(execCondition)
        val executionCondition: Condition
        executionCondition = ConversionUtil.parseURI(URI.create(sExecCond))

        val transferList = TM.getTransfersByExecutionCondition(executionCondition)

        val ja = JsonArray()
        for (transfer in transferList) {
            if (ai.isAdmin
                    || (transfer. debits[0] as  Debit).account.localID == ai.id
                    || (transfer.credits[0] as Credit).account.localID == ai.id) {
                ja.add((transfer as SimpleTransfer).toILPJSONStringifiedFormat())
                transferMatchUser = true
            }
        }
        if (!ai.isAdmin && !transferMatchUser) {
            throw ILPExceptionSupport.createILPForbiddenException()
        }
        val response = ja.encode()
        context.response()
                .putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                .putHeader(HttpHeaders.CONTENT_LENGTH, "" + response.length)
                .setStatusCode(HttpResponseStatus.OK.code())
                .end(response)
    }

    companion object {
        private val log = org.slf4j.LoggerFactory.getLogger(TransfersHandler::class.java)
        private val execCondition = "execCondition"

        fun create(): TransfersHandler = TransfersHandler()
    }

}// REF: https://github.com/interledger/five-bells-ledger/blob/master/src/lib/app.js


// GET /transfers/25644640-d140-450e-b94b-badbe23d3389/state|state?type=sha256
class TransferStateHandler : RestEndpointHandler(
        arrayOf(HttpMethod.GET), arrayOf("transfers/:$transferUUID/state")) {

    override fun handleGet(context: RoutingContext) {
        /* GET transfer by UUID & type
         * *****************************
         * GET /transfers/3a2a1d9e-8640-4d2d-b06c-84f2cd613204/state?type=sha256 HTTP/1.1
         * {"type":"sha256",
         *  "message":{
         *    "id":"http:.../transfers/3a2a1d9e-8640-4d2d-b06c-84f2cd613204",
         *    "state":"proposed",
         *    "token":"xy9kB4n......Cg=="
         *   },
         *   "signer":"http://localhost",
         *   "digest":"P6K2HEaZxAthBeGmbjeyPau0BIKjgkaPqW781zmSvf4="
         * } */
        log.debug(this.javaClass.name + "handleGet invoqued ")
        val ai = AuthManager.authenticate(context)

        val transferId = context.request().getParam(transferUUID)
        val transferID = LocalTransferID(transferId)
        var status = TransferStatus.PROPOSED // default value
        var transferMatchUser = false
        if (!TM.doesTransferExists(transferID))
            throw ILPExceptionSupport.createILPNotFoundException()

        val transfer = TM.getTransferById(transferID)
        status = transfer.transferStatus
        transferMatchUser = ai.id == (transfer. debits[0] as  Debit).account.localID
                         || ai.id == (transfer.credits[0] as Credit).account.localID

        if (!ai.isAdmin && !transferMatchUser) {
            throw ILPExceptionSupport.createILPForbiddenException()
        }
        // REF: getStateResource @ transfers.js

        var receiptType: String? = context.request().getParam("type")
        // REF: getTransferStateReceipt(id, receiptType, conditionState) @ five-bells-ledger/src/models/transfers.js
        if (receiptType == null) {
            receiptType = RECEIPT_TYPE_ED25519
        }
        if (receiptType != RECEIPT_TYPE_ED25519 &&
                receiptType != RECEIPT_TYPE_SHA256 &&
                true) {
            throw ILPExceptionSupport.createILPBadRequestException(
                    "type not in := $RECEIPT_TYPE_ED25519* | $RECEIPT_TYPE_SHA256 "
            )
        }
        val jo = JsonObject()
        val signer = ""      // FIXME: config.getIn(['server', 'base_uri']),
        if (receiptType == RECEIPT_TYPE_ED25519) {
            // REF: makeEd25519Receipt(transferId, transferState) @
            //      @ five-bells-ledger/src/models/transfers.js
            val message = makeTransferStateMessage(transferID, status, RECEIPT_TYPE_ED25519)
            val signature = ""   // FIXME: sign(hashJSON(message))
            jo.put("type", RECEIPT_TYPE_ED25519)
            jo.put("message", message)
            jo.put("signer", signer)
            jo.put("public_key", DSAPrivPubKeySupport.savePublicKey(Config.ilpLedgerInfo.notificationSignPublicKey))
            jo.put("signature", signature)
        } else {
            // REF: makeSha256Receipt(transferId, transferState, conditionState) @
            //      @ five-bells-ledger/src/models/transfers.js
            val message = makeTransferStateMessage(transferID, status, RECEIPT_TYPE_SHA256)
            val digest = sha256(message.encode())
            jo.put("type", RECEIPT_TYPE_SHA256)
            jo.put("message", message)
            jo.put("signer", signer)
            jo.put("digest", digest)
            val conditionState = context.request().getParam("condition_state")
            if (conditionState != null) {
                val conditionMessage = makeTransferStateMessage(transferID, status, RECEIPT_TYPE_SHA256)
                val condition_digest = sha256(conditionMessage.encode())
                jo.put("condition_state", conditionState)
                jo.put("condition_digest", condition_digest)
            }
        }

        val response = jo.encode()
        context.response()
                .putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                .putHeader(HttpHeaders.CONTENT_LENGTH, "" + response.length)
                .setStatusCode(HttpResponseStatus.OK.code())
                .end(response)
    }

    companion object {

        private val log = org.slf4j.LoggerFactory.getLogger(TransferStateHandler::class.java)
        private val transferUUID = "transferUUID"
        private val RECEIPT_TYPE_ED25519 = "ed25519-sha512"
        private val RECEIPT_TYPE_SHA256 = "sha256"
        private val md256: MessageDigest

        init {
            md256 = MessageDigest.getInstance("SHA-256")
        }

        fun create(): TransferStateHandler = TransferStateHandler()

        private fun makeTransferStateMessage(transferId: LocalTransferID, state: TransferStatus, receiptType: String): JsonObject {
            val jo = JsonObject()
            // <-- TODO:(0) Move URI logic to Iface ILPTransferSupport and add iface to SimpleLedgerTransferManager
            jo.put("id", Config.publicURL.toString() + "transfers/" + transferId.transferID)
            jo.put("state", state.toString())
            if (receiptType == RECEIPT_TYPE_SHA256) {
                val token = "" // FIXME: sign(sha512(transferId + ':' + state))
                jo.put("token", token)
            }
            return jo
        }

        private fun sha256(input: String): String {
            md256.reset()
            md256.update(input.toByteArray())
            return String(md256.digest())
        }
    }
}// REF: https://github.com/interledger/five-bells-ledger/blob/master/src/lib/app.js


/*
	 *  GET|PUT /transfers/25644640-d140-450e-b94b-badbe23d3389/fulfillment
	 *  fulfillment can be execution or cancellation
	 *  Note: Rejection != cancellation. Rejection in five-bells-ledger refers
	 *      to the rejection in the proposed (not-yet prepared) transfer (or part of the
	 *      transfer).
	 *      In the java-vertx-ledger there is not yet (2017-05) concept of proposed
	 *      state.
	 */
class FulfillmentHandler : RestEndpointHandler(arrayOf(HttpMethod.GET, HttpMethod.PUT), arrayOf("transfers/:$transferUUID/fulfillment")) {

    override fun handlePut(context: RoutingContext) {
        val ai = AuthManager.authenticate(context)
        log.trace(this.javaClass.name + "handlePut invoqued ")

        /* (request from ILP Connector)
         * PUT /transfers/25644640-d140-450e-b94b-badbe23d3389/fulfillment HTTP/1.1 */
        val ilpTransferID = UUID.fromString(context.request().getParam(transferUUID))
        val transferID = ILPSpec2LocalTransferID(ilpTransferID)

        /*
         * REF: https://gitter.im/interledger/Lobby
         * Enrique Arizon Benito @earizon 17:51 2016-10-17
         *     Hi, I'm trying to figure out how the five-bells-ledger implementation validates fulfillments.
         *     Following the node.js code I see the next route:
         *
         *          router.put('/transfers/:id/fulfillment', transfers.putFulfillment)
         *
         *     I understand the fulfillment is validated at this (PUT) point against the stored condition
         *     in the existing ":id" transaction.
         *     Following the stack for this request it looks to me that the method
         *
         *     (/five-bells-condition/index.js)validateFulfillment (serializedFulfillment, serializedCondition, message)
         *
         *     is always being called with an undefined message and so an empty one is being used.
         *     I'm missing something or is this the expected behaviour?
         *
         * Stefan Thomas @justmoon 18:00 2016-10-17
         *     @earizon Yes, this is expected. We're using crypto conditions as a trigger, not to verify the
         *     authenticity of a message!
         *     Note that the actual cryptographic signature might still be against a message - via prefix
         *     conditions (which append a prefix to this empty message)
         **/
        val transfer = TM.getTransferById(transferID)
        if (transfer.executionCondition === CC_NOT_PROVIDED) {
            throw ILPExceptionSupport.createILPUnprocessableEntityException(
                    this.javaClass.name + "Transfer is not conditional")
        }
        val transferMatchUser = // TODO:(?) Recheck
                ai.id == transfer.debits[0].localAccount.localID || ai.id == transfer.credits[0].localAccount.localID
        if (!ai.isAdmin && !transferMatchUser) {
            throw ILPExceptionSupport.createILPForbiddenException()
        }

        val sFulfillmentInput = context.bodyAsString
        val fulfillmentBytes = Base64.getDecoder().decode(sFulfillmentInput)
        //        // REF: http://stackoverflow.com/questions/140131/convert-a-string-representation-of-a-hex-dump-to-a-byte-array-using-java
        //        byte[] fulfillmentBytes = DatatypeConverter.parseHexBinary(sFulfillment);
        val inputFF = Fulfillment.of(fulfillmentBytes)
        val message = byteArrayOf()
        var ffExisted = false // TODO:(0) Recheck usage
        log.trace("transfer.getExecutionCondition():" + transfer.executionCondition.toString())
        //        log.trace("transfer.getCancellationCondition():"+transfer.getCancellationCondition().toString());
        log.trace("request hexFulfillment:" + sFulfillmentInput)
        log.trace("request ff.getCondition():" + inputFF.condition.toString())
        if (transfer.executionCondition == inputFF.condition) {
            if (!inputFF.validate(inputFF.condition))
                throw ILPExceptionSupport.createILPUnprocessableEntityException("execution fulfillment doesn't validate")
            if (transfer.expiresAt.compareTo(ZonedDateTime.now()) < 0 && Config.unitTestsActive == false)
                throw ILPExceptionSupport.createILPUnprocessableEntityException("transfer expired")
            if (transfer.transferStatus != TransferStatus.EXECUTED)
                TM.executeILPTransfer(transfer, inputFF)
            //        } else if (transfer.getCancellationCondition().equals(inputFF.getCondition()) ){
            //            if ( transfer.getTransferStatus() == TransferStatus.EXECUTED) {
            //                throw ILPExceptionSupport.createILPBadRequestException("Already executed");
            //            }
            //            ffExisted = transfer.getCancellationFulfillment().equals(inputFF);
            //            if (!ffExisted) {
            //                if (!inputFF.verify(inputFF.getCondition(), message)){
            //                    throw ILPExceptionSupport.createILPUnprocessableEntityException("cancelation fulfillment doesn't validate");
            //                }
            //                TM.cancelILPTransfer(transfer, inputFF);
            //            }
        } else {
            throw ILPExceptionSupport.createILPUnprocessableEntityException(
                    "Fulfillment does not match any condition")
        }

        val response = ConversionUtil.fulfillmentToBase64(inputFF)
        if (sFulfillmentInput != response) {
            throw ILPExceptionSupport.createILPBadRequestException(
                    "Assert exception. Input '$sFulfillmentInput'doesn't match output '$response' ")
        }
        context.response()
                .putHeader(HttpHeaders.CONTENT_TYPE, "text/plain")
                .putHeader(HttpHeaders.CONTENT_LENGTH, "" + response.length)
                .setStatusCode(if (!ffExisted) HttpResponseStatus.CREATED.code() else HttpResponseStatus.OK.code())
                .end(response)
    }

    override fun handleGet(context: RoutingContext) {
        // GET /transfers/25644640-d140-450e-b94b-badbe23d3389/fulfillment
        val ai = AuthManager.authenticate(context)


        val ilpTransferID = UUID.fromString(context.request().getParam(transferUUID))
        val transferID = ILPSpec2LocalTransferID(ilpTransferID)

        val transfer = TM.getTransferById(transferID)

        var transferMatchUser =
                   ai.id == transfer.debits[0].localAccount.localID
                || ai.id == transfer.credits[0].localAccount.localID
        if (!ai.isAdmin && !(ai.isConnector && transferMatchUser))
            throw ILPExceptionSupport.createILPForbiddenException()

        val fulfillment = transfer.executionFulfillment
        if (fulfillment === FF_NOT_PROVIDED) {
            if (transfer.expiresAt.compareTo(ZonedDateTime.now()) < 0) {
                throw ILPExceptionSupport.createILPNotFoundException("This transfer expired before it was fulfilled")
            }
            throw ILPExceptionSupport.createILPUnprocessableEntityException("Unprocessable Entity")
        }

        val response = ConversionUtil.fulfillmentToBase64(fulfillment)

        context.response().putHeader(HttpHeaders.CONTENT_TYPE, "text/plain")
                .putHeader(HttpHeaders.CONTENT_LENGTH, "" + response.length)
                .setStatusCode(HttpResponseStatus.OK.code()).end(response)
    }

    companion object {
        private val log = org.slf4j.LoggerFactory.getLogger(FulfillmentHandler::class.java)
        private val transferUUID = "transferUUID"

        fun create(): FulfillmentHandler = FulfillmentHandler()
    }
}
