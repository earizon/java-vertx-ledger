package org.interledger.everledger.handlers;

import java.net.URI;
import java.util.UUID;
import java.time.ZonedDateTime;

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
import org.interledger.cryptoconditions.uri.CryptoConditionUri;
import org.interledger.cryptoconditions.uri.URIEncodingException;
import org.interledger.everledger.AuthInfo;
import org.interledger.everledger.Config;
import org.interledger.everledger.LedgerAccountManagerFactory;
import org.interledger.everledger.handlers.RestEndpointHandler;
import org.interledger.everledger.ifaces.account.IfaceLocalAccount;
import org.interledger.everledger.ifaces.account.IfaceLocalAccountManager;
import org.interledger.everledger.ifaces.transfer.IfaceTransfer;
import org.interledger.everledger.ifaces.transfer.IfaceLocalTransferManager;
import org.interledger.everledger.ifaces.transfer.IfaceTransferManager;
import org.interledger.everledger.impl.SimpleTransfer;
import org.interledger.everledger.impl.manager.SimpleLedgerTransferManager;
import org.interledger.everledger.transfer.Credit;
import org.interledger.everledger.transfer.Debit;
import org.interledger.everledger.transfer.LocalTransferID;
import org.interledger.everledger.util.AuthManager;
import org.interledger.everledger.util.ILPExceptionSupport;
import org.interledger.everledger.util.TimeUtils;
import org.interledger.ilp.InterledgerError.ErrorCode;
import org.interledger.ledger.model.TransferStatus;
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

    private static final IfaceTransferManager TM = SimpleLedgerTransferManager.getTransferManager();

    // GET|PUT /transfers/3a2a1d9e-8640-4d2d-b06c-84f2cd613204

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
        
        UUID ilpTransferID; try {
            ilpTransferID = UUID.fromString(context.request().getParam(transferUUID));
            }catch(Exception e){
        throw ILPExceptionSupport.createILPBadRequestException(
            "'"+context.request().getParam(transferUUID)+"' is not a valid UUID");
            }
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

        double totalDebitAmmount = 0.0;
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
            double auxDebit = Double.parseDouble(jsonDebit.getString("amount"));
            totalDebitAmmount += auxDebit;
            debit_ammount = Money.of(auxDebit, currencyUnit);
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
        JsonObject jsonMemo = requestBody.getJsonObject("memo");
        String sMemo = (jsonMemo == null) ? "" : jsonMemo.encode(); 
        // REF: JsonArray ussage:
        // http://www.programcreek.com/java-api-examples/index.php?api=io.vertx.core.json.JsonArray
        JsonArray credits = requestBody.getJsonArray("credits");

        String sExpiresAt = requestBody.getString("expires_at"); // can be null
        ZonedDateTime DTTM_expires ; try {
            DTTM_expires = (sExpiresAt==null) ? TimeUtils.future : ZonedDateTime.parse(sExpiresAt);
        }catch(Exception e){
            throw ILPExceptionSupport.createILPBadRequestException("unparseable expires_at");

        }

        String execution_condition = requestBody
                .getString("execution_condition");
        Condition URIExecutionCond;
        try {
            URIExecutionCond = (execution_condition != null) ? CryptoConditionUri
                    .parse(URI.create(execution_condition))
                    : SimpleTransfer.CC_NOT_PROVIDED;
        } catch (URIEncodingException e1) {
            throw ILPExceptionSupport.createILPBadRequestException("execution_condition '"
                    + execution_condition + "' could not be parsed as URI");
        }
        Credit[] creditList = new Credit[credits.size()];

        double totalCreditAmmount = 0.0;
        for (int idx = 0; idx < credits.size(); idx++) {
            JsonObject jsonCredit = credits.getJsonObject(idx);
            /*
             * { "account":"http://localhost:3002/accounts/ilpconnector",
             * "amount":"1.01", "memo":{ "ilp_header":{
             * "account":"ledger3.eur.alice.fe773626-81fb-4294-9a60-dc7b15ea841e"
             * , "amount":"1", "data":{"expires_at":"2016-11-10T15:51:27.134Z"}
             * } } }
             */
            // JsonObject jsonMemoILPHeader = jsonCredit.getJsonObject("memo")
            // COMMENTED OLD API         .getJsonObject("ilp_header");
            String account_name = jsonCredit.getString("account");
            if (account_name.lastIndexOf('/')>0){
                   account_name = account_name.substring(account_name.lastIndexOf('/')+1);
            }
            MonetaryAmount credit_ammount;
            try {
                double auxCredit = Double.parseDouble(jsonCredit.getString("amount"));
                totalCreditAmmount += auxCredit;
                credit_ammount = Money.of(auxCredit, currencyUnit);
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
            creditList[idx] = new Credit(creditor, credit_ammount/*, memo_ph*/);
        }
        if (totalCreditAmmount != totalDebitAmmount) {
            throw ILPExceptionSupport.createILPException(422,
                    ErrorCode.F00_BAD_REQUEST , "total credits do not match total debits"); 
        }

        String data = ""; // Not yet used
        String noteToSelf = ""; // Not yet used
        ZonedDateTime DTTM_proposed = ZonedDateTime.now();

        String cancelation_condition = requestBody
                .getString("cancellation_condition");
        Condition URICancelationCond;
        try {
            URICancelationCond = (cancelation_condition != null) ? CryptoConditionUri
                    .parse(URI.create(cancelation_condition))
                    : SimpleTransfer.CC_NOT_PROVIDED;
        } catch (URIEncodingException e1) {
            throw ILPExceptionSupport.createILPBadRequestException("cancelation_condition '"
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
        
        IfaceTransfer receivedTransfer = new SimpleTransfer(transferID,
                debitList, creditList, URIExecutionCond, URICancelationCond,
                DTTM_expires, DTTM_proposed, data, noteToSelf, status, sMemo);

        // TODO:(0) Next logic must be on the Manager, not in the HTTP-protocol related handler
        boolean isNewTransfer = !TM.doesTransferExists(ilpTransferID);
        log.debug("is new transfer?: " + isNewTransfer);

        IfaceTransfer effectiveTransfer = (isNewTransfer) ? receivedTransfer
                : TM.getTransferById(transferID);
        if (!isNewTransfer) {
            // Check that received json data match existing transaction.
            // TODO: Recheck (Multitransfer active now)
            if (!effectiveTransfer.getCredits()[0].equals(receivedTransfer
                    .getCredits()[0])
                    || !effectiveTransfer.getDebits()[0]
                            .equals(receivedTransfer.getDebits()[0])) {
                throw ILPExceptionSupport.createILPBadRequestException(
                        "data for credits and/or debits doesn't match existing registry");
            }
        } else {
            TM.createNewRemoteILPTransfer(receivedTransfer);
        }
        try { // TODO:(?) Refactor Next code for notification (next two loops) are
              // duplicated in FulfillmentHandler
            String notification = ((SimpleTransfer) effectiveTransfer)
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
        String response = ((SimpleTransfer) effectiveTransfer)
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
        UUID ilpTransferID = UUID.fromString(context.request().getParam(
                transferUUID));
        IfaceTransfer transfer = TM.getTransferById(
                LocalTransferID.ILPSpec2LocalTransferID(ilpTransferID));

        String debit0_account = transfer.getDebits()[0].account.getLocalID();
        boolean transferMatchUser = ai.getId().equals(debit0_account);
        if (!transferMatchUser && !ai.getRoll().equals("admin")) {
            log.error("transferMatchUser false: "
                    + "\n    ai.getId()    :" + ai.getId()
                    + "\n    debit0_account:" + debit0_account );
            throw ILPExceptionSupport.createILPForbiddenException();
        }
        response(
                context,
                HttpResponseStatus.OK,
                ((SimpleTransfer) transfer).toILPJSONStringifiedFormat());
    }

    public static void main(String[] args){
        System.out.println(">"+TimeUtils.testingDate.format(TimeUtils.ilpFormatter));
    }
}
