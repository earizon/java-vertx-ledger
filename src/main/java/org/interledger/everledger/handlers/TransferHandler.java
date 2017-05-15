package org.interledger.everledger.handlers;

import java.net.URI;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

import javax.money.CurrencyUnit;
import javax.money.Monetary;
import javax.money.MonetaryAmount;

import org.interledger.cryptoconditions.Condition;
import org.interledger.cryptoconditions.types.PreimageSha256Condition;
import org.interledger.cryptoconditions.uri.CryptoConditionUri;
import org.interledger.cryptoconditions.uri.URIEncodingException;
import org.interledger.everledger.AuthInfo;
import org.interledger.everledger.Config;
import org.interledger.everledger.LedgerAccountManagerFactory;
import org.interledger.everledger.handlers.RestEndpointHandler;
import org.interledger.everledger.ifaces.account.IfaceLocalAccount;
import org.interledger.everledger.ifaces.account.IfaceLocalAccountManager;
import org.interledger.everledger.ifaces.transfer.ILedgerTransfer;
import org.interledger.everledger.ifaces.transfer.IfaceLocalTransferManager;
import org.interledger.everledger.ifaces.transfer.IfaceTransferManager;
import org.interledger.everledger.impl.SimpleLedgerTransfer;
import org.interledger.everledger.impl.manager.SimpleLedgerTransferManager;
import org.interledger.everledger.ledger.transfer.Credit;
import org.interledger.everledger.ledger.transfer.DTTM;
import org.interledger.everledger.ledger.transfer.Debit;
import org.interledger.everledger.ledger.transfer.ILPSpecTransferID;
import org.interledger.everledger.ledger.transfer.LocalTransferID;
import org.interledger.everledger.util.AuthManager;
import org.interledger.everledger.util.ILPExceptionSupport;
import org.interledger.ilp.InterledgerError.ErrorCode;
import org.interledger.ilp.ledger.model.TransferStatus;
import org.javamoney.moneta.Money;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TransferHandler handler
 *
 * @author earizon REF: five-bells-ledger/src/controllers/transfers.js
 */
public class TransferHandler extends RestEndpointHandler {

    private static final Logger log = LoggerFactory
            .getLogger(TransferHandler.class);
    private final static String transferUUID = "transferUUID";

    private static final IfaceLocalAccountManager ledgerAccountManager = LedgerAccountManagerFactory
            .getLedgerAccountManagerSingleton();

    // GET|PUT /transfers/3a2a1d9e-8640-4d2d-b06c-84f2cd613204

    // TODO:(0) Generate random preimage. This object is just used to avoid null
    // comparations. The real preimage
    // doesn't really matter (but in cryptography always is better to be
    // paranoid)

    public static final Condition Condition_NOT_PROVIDED = new PreimageSha256Condition(
            new byte[] { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1 }/* preimage */);

    public TransferHandler() {
        // REF:
        // https://github.com/interledgerjs/five-bells-ledger/blob/master/src/lib/app.js
        super(
                new HttpMethod[] {HttpMethod.GET, HttpMethod.POST, HttpMethod.PUT},
                new String[] { "transfers/:" + transferUUID }
            );
    }

    public static TransferHandler create() {
        return new TransferHandler(); // TODO: return singleton?
    }

    @Override
    protected void handlePut(RoutingContext context) {
        AuthInfo ai = AuthManager.authenticate(context);
        JsonObject requestBody = getBodyAsJson(context);
System.out.println("deleteme context.getBodyAsString():"+context.getBodyAsString());

        boolean transferMatchUser = false;
        log.trace(this.getClass().getName() + "handlePut invoqued ");
        log.trace(context.getBodyAsString());
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
        
        ILPSpecTransferID ilpTransferID = new ILPSpecTransferID(context.request().getParam(
                transferUUID));
        LocalTransferID transferID = LocalTransferID.ILPSpec2LocalTransferID(ilpTransferID);

        // TODO: Check requestBody.getString("ledger") match ledger host/port

        // TODO: Check state is 'proposed' for new transactions?

        // TODO:(?) mark as "Tainted" object
        JsonArray debits = requestBody.getJsonArray("debits");

        if (debits == null) {
            throw ILPExceptionSupport.createILPBadRequestException("debits not found");
        }
        if (debits.size()!=1) {
            throw ILPExceptionSupport.createILPBadRequestException("Only one debitor supported by ledger");
        }
        Debit[] debitList = new Debit[debits.size()];
        CurrencyUnit currencyUnit /* local ledger currency */= Monetary
                .getCurrency(Config.ledgerCurrencyCode);

        for (int idx = 0; idx < debits.size(); idx++) {
            JsonObject jsonDebit = debits.getJsonObject(idx);
            log.debug("check123 jsonDebit: " + jsonDebit.encode());
            // debit0 will be similar to
            // {"account":"http://localhost/accounts/alice","amount":"50"}
            String account_name = jsonDebit.getString("account");
            if (account_name.lastIndexOf('/')>0){
                   account_name = account_name.substring(account_name.lastIndexOf('/')+1);
            }
            if (ai.getId().equals(account_name)) { 
                transferMatchUser = true; 
            }
            MonetaryAmount debit_ammount; 
                    try { 
            debit_ammount = Money.of(
                    Double.parseDouble(jsonDebit.getString("amount")), 
                    currencyUnit);
            }catch(Exception e){
                System.out.println(e.toString());
                throw ILPExceptionSupport.createILPBadRequestException("unparseable amount");
            }
            if (debit_ammount.getNumber().floatValue() == 0.0) {
                throw ILPExceptionSupport.createILPException(422, ErrorCode.F00_BAD_REQUEST , "debit is zero"); 
            }
            IfaceLocalAccount debitor = ledgerAccountManager
                    .getAccountByName(account_name);
            log.debug("check123 debit_ammount (must match jsonDebit ammount: "
                    + debit_ammount.toString());
            debitList[idx] = new Debit(debitor, debit_ammount);
        }
        if (!ai.isAdmin() && !transferMatchUser) {
            throw ILPExceptionSupport.createILPForbiddenException();
        }
        // REF: JsonArray ussage:
        // http://www.programcreek.com/java-api-examples/index.php?api=io.vertx.core.json.JsonArray
        JsonArray credits = requestBody.getJsonArray("credits");
System.out.println("deleteme JsonArray credits:"+credits.encodePrettily());

        String sExpiresAt = requestBody.getString("expires_at"); // can be null
        DTTM DTTM_expires ; try {
            DTTM_expires = (sExpiresAt==null) ? DTTM.future : new DTTM(sExpiresAt);
        }catch(Exception e){
            throw ILPExceptionSupport.createILPBadRequestException("unparseable expires_at");

        }

        String execution_condition = requestBody
                .getString("execution_condition");
        Condition URIExecutionCond;
        try {
            URIExecutionCond = (execution_condition != null) ? CryptoConditionUri
                    .parse(URI.create(execution_condition))
                    : TransferHandler.Condition_NOT_PROVIDED;
        } catch (URIEncodingException e1) {
            throw new RuntimeException("execution_condition '"
                    + execution_condition + "' could not be parsed as URI");
        }
        Credit[] creditList = new Credit[credits.size()];

        for (int idx = 0; idx < credits.size(); idx++) {
            JsonObject jsonCredit = credits.getJsonObject(idx);
            /*
             * { "account":"http://localhost:3002/accounts/ilpconnector",
             * "amount":"1.01", "memo":{ "ilp_header":{
             * "account":"ledger3.eur.alice.fe773626-81fb-4294-9a60-dc7b15ea841e"
             * , "amount":"1", "data":{"expires_at":"2016-11-10T15:51:27.134Z"}
             * } } }
             */
            // COMMENTED OLD API JsonObject jsonMemoILPHeader = jsonCredit.getJsonObject("memo")
            // COMMENTED OLD API         .getJsonObject("ilp_header");
            String account_name = jsonCredit.getString("account");
            if (account_name.lastIndexOf('/')>0){
                   account_name = account_name.substring(account_name.lastIndexOf('/')+1);
            }
            MonetaryAmount credit_ammount;
            try {
System.out.println("deleteme credit amount:"+jsonCredit.getString("amount"));
                credit_ammount = Money.of(
                    Double.parseDouble(jsonCredit.getString("amount")),
                    currencyUnit);
            }catch(Exception e){
                throw ILPExceptionSupport.createILPBadRequestException("unparseable amount");
            }
            if (credit_ammount.getNumber().floatValue() == 0.0) {
                throw ILPExceptionSupport.createILPException(422,
                        ErrorCode.F00_BAD_REQUEST , "credit is zero"); 
            }

            IfaceLocalAccount creditor = ledgerAccountManager
                    .getAccountByName(account_name);


            // COMMENTED OLD API String ilp_ph_ilp_dst_address = jsonMemoILPHeader
            // COMMENTED OLD API         .getString("account");

            // COMMENTED OLD API InterledgerAddress dstAddress = InterledgerAddressBuilder.builder()
            // COMMENTED OLD API         .value(ilp_ph_ilp_dst_address).build();
            // COMMENTED OLD API String ilp_ph_amount = jsonMemoILPHeader.getString("amount");
            // COMMENTED OLD API BigDecimal ammount = new BigDecimal(ilp_ph_amount); // TODO:(0)
                                                                // FIXME?
            // COMMENTED OLD API Condition ilp_ph_condition = URIExecutionCond;
            // COMMENTED OLD API DTTM ilp_ph_expires = new DTTM(jsonMemoILPHeader.getJsonObject(
            // COMMENTED OLD API         "data").getString("expires_at"));
            // COMMENTED OLD API if (!DTTM_expires.equals(ilp_ph_expires)) {
            // COMMENTED OLD API     DTTM_expires = ilp_ph_expires;// TODO: Recheck
            // COMMENTED OLD API }
            // COMMENTED OLD API ZonedDateTime zdt = ZonedDateTime.parse((DTTM_expires.toString()));
            // InterledgerPacketHeader(InterledgerAddress destinationAddress,
            // BigDecimal amount,
            // Condition condition, ZonedDateTime expiry)
            // COMMENTED OLD API InterledgerPacketHeader memo_ph = new InterledgerPacketHeader(
            // COMMENTED OLD API         dstAddress, ammount, ilp_ph_condition, zdt);
            // In five-bells-ledger, memo goes into transfer_adjustments table (@ src/sql/pg/...)
            creditList[idx] = new Credit(creditor, credit_ammount/*, memo_ph*/);
        }
        IfaceTransferManager TM   = SimpleLedgerTransferManager.getTransferManager();
        String data = ""; // Not yet used
        String noteToSelf = ""; // Not yet used
        DTTM DTTM_proposed = DTTM.getNow();
        log.debug(transferID.transferID + " expires_at == null"
                + (requestBody.getString("expires_at") == null));

        String cancelation_condition = requestBody
                .getString("cancellation_condition");
        Condition URICancelationCond;
        try {
            URICancelationCond = (cancelation_condition != null) ? CryptoConditionUri
                    .parse(URI.create(cancelation_condition))
                    : Condition_NOT_PROVIDED;
        } catch (URIEncodingException e1) {
            throw new RuntimeException("cancelation_condition '"
                    + cancelation_condition + "' could not be parsed as URI");
        }
        TransferStatus status = TransferStatus.PROPOSED; // By default
        if (requestBody.getString("state") != null) {
            // TODO: Must status change be allowed or must we force it to be
            // 'prepared'?
            // (only Execution|Cancellation Fulfillments will change the state)
            // At this moment it's allowed (to make it compliant with
            // five-bells-ledger tests)
            status = TransferStatus.parse(requestBody.getString("state"));
            log.debug("transfer status " + status);
        }

        ILedgerTransfer receivedTransfer = new SimpleLedgerTransfer(transferID,
                debitList, creditList, URIExecutionCond, URICancelationCond,
                DTTM_expires, DTTM_proposed, data, noteToSelf, status);

        // TODO:(0) Next logic must be on the Manager, not in the HTTP-protocol related handler
        boolean isNewTransfer = !TM.doesTransferExists(ilpTransferID);
        log.debug("is new transfer?: " + isNewTransfer);

        ILedgerTransfer effectiveTransfer = (isNewTransfer) ? receivedTransfer
                : TM.getLocalTransferById(transferID);
        if (!isNewTransfer) {
            // Check that received json data match existing transaction.
            // TODO: Recheck (Multitransfer active now)
            if (!effectiveTransfer.getCredits()[0].equals(receivedTransfer
                    .getCredits()[0])
                    || !effectiveTransfer.getDebits()[0]
                            .equals(receivedTransfer.getDebits()[0])) {
                throw new RuntimeException(
                        "data for credits and/or debits doesn't match existing registry");
            }
        } else {
            TM.createNewRemoteILPTransfer(receivedTransfer);
        }
        try { // TODO: Refactor Next code for notification (next two loops) are
              // duplicated in FulfillmentHandler
            String notification = ((SimpleLedgerTransfer) effectiveTransfer)
                    .toMessageStringifiedFormat().encode();
            log.info("send transfer update to ILP Connector through websocket: \n:"
                    + notification + "\n");
            // Notify affected accounts:
            for (Debit debit : effectiveTransfer.getDebits()) {
                TransferWSEventHandler.notifyListener(context, debit.account,
                        notification);
            }
            for (Credit credit : effectiveTransfer.getCredits()) {
                TransferWSEventHandler.notifyListener(context, credit.account,
                        notification);
            }
        } catch (Exception e) {
            log.warn("transaction created correctly but ilp-connector couldn't be notified due to "
                    + e.toString());
        }
        String response = ((SimpleLedgerTransfer) effectiveTransfer)
                .toILPJSONStringifiedFormat().encode();// .encode();

        context.response()
                .putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                .putHeader(HttpHeaders.CONTENT_LENGTH, "" + response.length())
                .setStatusCode(
                        isNewTransfer ? HttpResponseStatus.CREATED.code()
                                : HttpResponseStatus.OK.code()).end(response);
    }

    @Override
    protected void handleGet(RoutingContext context) {
        log.debug(this.getClass().getName() + "handleGet invoqued ");
        AuthInfo ai = AuthManager.authenticate(context);

        IfaceLocalTransferManager TM = SimpleLedgerTransferManager.getTransferManager();
        ILPSpecTransferID ilpTransferID = new ILPSpecTransferID(context.request().getParam(
                transferUUID));
        ILedgerTransfer transfer = TM.getLocalTransferById(
                LocalTransferID.ILPSpec2LocalTransferID(ilpTransferID));

        String debit0_account = transfer.getDebits()[0].account.getLocalName();
        boolean transferMatchUser = ai.getId().equals(debit0_account);
        if (!transferMatchUser) {
            log.error("transferMatchUser false: "
                    + "\n    ai.getId()    :" + ai.getId()
                    + "\n    debit0_account:" + debit0_account );
            throw ILPExceptionSupport.createILPForbiddenException();
        }
        response(
                context,
                HttpResponseStatus.OK,
                buildJSON("result", ((SimpleLedgerTransfer) transfer)
                        .toILPJSONStringifiedFormat().encode()));
    }
}
