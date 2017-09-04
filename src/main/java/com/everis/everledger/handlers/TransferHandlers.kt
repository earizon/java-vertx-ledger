package com.everis.everledger.handlers

/*
 * TODO:(?) Remove debitor/creditor for code. Use TX_src/TX_dst or imilar to avoid confusion with debit/credit terms.
 * TODO:(?) For blockchains ai.isAdmin doesn't make any sense => There is no "root" admin user.
 */
import com.everis.everledger.AuthInfo
import com.everis.everledger.ifaces.account.IfaceAccount
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
        val jsonDebit = debits.getJsonObject(0)
        // debit0 will be similar to
        // {"account":"http://localhost/accounts/alice","amount":"50"}
        var input_account_id = jsonDebit.getString("account")
        if (input_account_id.lastIndexOf('/') > 0) {
            input_account_id = input_account_id.substring(input_account_id.lastIndexOf('/') + 1)
        }
        val debit_ammount: MonetaryAmount = try {
            val auxDebit = java.lang.Double.parseDouble(jsonDebit.getString("amount"))
            Money.of(auxDebit, currencyUnit)
        } catch (e: Exception) {
            println(e.toString())
            throw ILPExceptionSupport.createILPBadRequestException("unparseable amount")
        }
        if ( debit_ammount.number.toFloat().toDouble() == 0.0) {
            throw ILPExceptionSupport.createILPException(422, InterledgerError.ErrorCode.F00_BAD_REQUEST, "debit is zero")
        }
        val debitor = AM.getAccountById(input_account_id)
        _assertAuthInfoMatchTXDebitorOrThrow(debitor, ai)

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
        val jsonCredit = credits.getJsonObject(0)
        /* { "account":"http://localhost:3002/accounts/ilpconnector",
         * "amount":"1.01", "memo":{ "ilp_header":{
         * "account":"ledger3.eur.alice.fe773626-81fb-4294-9a60-dc7b15ea841e"
         * , "amount":"1", "data":{"expires_at":"2016-11-10T15:51:27.134Z"}
         * } } } */
        // JsonObject jsonMemoILPHeader = jsonCredit.getJsonObject("memo")
        // COMMENTED OLD API         .getJsonObject("ilp_header");
        var credit_account_id = jsonCredit.getString("account")
        if (credit_account_id.lastIndexOf('/') > 0) {
            credit_account_id = credit_account_id.substring(credit_account_id.lastIndexOf('/') + 1)
        }

        val credit_ammount: MonetaryAmount = try {
            val auxCredit = java.lang.Double.parseDouble(jsonCredit.getString("amount"))
            Money.of(auxCredit, currencyUnit)
        } catch (e: Exception) {
            throw ILPExceptionSupport.createILPBadRequestException("unparseable amount")
        }

        if (credit_ammount.getNumber().toFloat().toDouble() == 0.0) {
            throw ILPExceptionSupport.createILPException(422,
                    InterledgerError.ErrorCode.F00_BAD_REQUEST, "credit is zero")
        }
        val creditor = AM.getAccountById(credit_account_id)
        if (!credit_ammount.equals(debit_ammount)) {
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
                transferID as LocalTransferID,
                TXInputImpl(debitor, debit_ammount),
                TXOutputImpl(creditor, credit_ammount/*, memo_ph*/),
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
            if (   effectiveTransfer.txInput != receivedTransfer.txInput
                || effectiveTransfer.txInput != receivedTransfer.txInput) {
                throw ILPExceptionSupport.createILPBadRequestException(
                        "data for credits and/or debits doesn't match existing registry")
            }
        } else {
            TM.prepareILPTransfer(receivedTransfer)
        }
        try { // TODO:(?) Refactor Next code for notification (next two loops) are
            // duplicated in FulfillmentHandler
            val resource = (effectiveTransfer as SimpleTransfer).toILPJSONStringifiedFormat()

            // Notify affected accounts:
            val eventType = if (receivedTransfer.transferStatus == TransferStatus.PREPARED)
                      TransferWSEventHandler.EventType.TRANSFER_CREATE
                 else TransferWSEventHandler.EventType.TRANSFER_UPDATE
            val setAffectedAccounts = HashSet<String>()
            setAffectedAccounts.add(receivedTransfer.txOutput.localAccount.localID)
            setAffectedAccounts.add(receivedTransfer.txInput .localAccount.localID)
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
        val debitor = AM.getAccountById(transfer.txInput.localAccount.localID)
        _assertAuthInfoMatchTXDebitorOrThrow(debitor, ai)
        response( context, HttpResponseStatus.OK, (transfer as SimpleTransfer).toILPJSONStringifiedFormat())
    }

    private fun _assertAuthInfoMatchTXDebitorOrThrow(debitor: IfaceAccount, ai : AuthInfo){
        var transferMatchUser = AM.authInfoMatchAccount(debitor, ai)
        if (!ai.isAdmin && !transferMatchUser) {
            log.error("transferMatchUser false and user is not ADMIN: "
                    + "\n    ai.id    :" + ai.id
                    + "\n    transfer.txInput.localAccount.localID:" + debitor)
            throw ILPExceptionSupport.createILPForbiddenException()
        }

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

        //        Condition condition = CryptoConditionUri.parse(URI.create(testVector.getConditionUri()));
        val sExecCond = context.request().getParam(execCondition)
        val executionCondition: Condition
        executionCondition = ConversionUtil.parseURI(URI.create(sExecCond))
        val transferList = TM.getTransfersByExecutionCondition(executionCondition)

        val ja = JsonArray()
        for (transfer in transferList) {
            val debitor  = AM.getAccountById(transfer.txInput .localAccount.localID)
            val creditor = AM.getAccountById(transfer.txOutput.localAccount.localID)
            if ( !ai.isAdmin &&
                 !( AM.authInfoMatchAccount(debitor  , ai) ||
                    AM.authInfoMatchAccount(creditor , ai) ) ) continue
            ja.add((transfer as SimpleTransfer).toILPJSONStringifiedFormat())
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
        if (!TM.doesTransferExists(transferID))
            throw ILPExceptionSupport.createILPNotFoundException()

        val transfer = TM.getTransferById(transferID)
        var status = transfer.transferStatus
        val debitor  = AM.getAccountById(transfer.txInput .localAccount.localID)
        var transferMatchUser = AM.authInfoMatchAccount(debitor , ai)

        if (!ai.isAdmin && !transferMatchUser) throw ILPExceptionSupport.createILPForbiddenException()

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
            if (transfer.ilpExpiresAt.compareTo(ZonedDateTime.now()) < 0 && Config.unitTestsActive == false)
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

        val debitor  = AM.getAccountById(transfer.txInput .localAccount.localID)
        val creditor = AM.getAccountById(transfer.txOutput.localAccount.localID)
        var transferMatchUser = AM.authInfoMatchAccount(debitor , ai)
                             || AM.authInfoMatchAccount(creditor, ai)
        if (!ai.isAdmin && !(ai.isConnector && transferMatchUser))
            throw ILPExceptionSupport.createILPForbiddenException()

        val fulfillment = transfer.executionFulfillment
        if (fulfillment === FF_NOT_PROVIDED) {
            if (transfer.ilpExpiresAt.compareTo(ZonedDateTime.now()) < 0) {
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
