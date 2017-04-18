package org.interledger.everledger.ledger.api.handlers;

import javax.xml.bind.DatatypeConverter;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.http.HttpHeaders;
//import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.ext.web.RoutingContext;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.PUT;

import org.interledger.cryptoconditions.Fulfillment;
//import org.interledger.cryptoconditions.HexDump;
import org.interledger.cryptoconditions.der.CryptoConditionReader;
import org.interledger.cryptoconditions.der.DEREncodingException;
import org.interledger.everledger.common.api.auth.AuthInfo;
import org.interledger.everledger.common.api.auth.AuthManager;
import org.interledger.everledger.common.api.handlers.RestEndpointHandler;
import org.interledger.everledger.common.api.util.ILPExceptionSupport;
import org.interledger.everledger.ledger.impl.simple.SimpleLedgerTransfer;
import org.interledger.everledger.ledger.impl.simple.SimpleLedgerTransferManager;
import org.interledger.everledger.ledger.transfer.Credit;
import org.interledger.everledger.ledger.transfer.Debit;
import org.interledger.everledger.ledger.transfer.LedgerTransfer;
import org.interledger.everledger.ledger.transfer.LedgerTransferManager;
import org.interledger.everledger.ledger.transfer.TransferID;
import org.interledger.ilp.InterledgerError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fulfillment (and rejection) handler
 * 
 * REF: five-bells-ledger/src/controllers/transfers.js
 */
public class FulfillmentHandler extends RestEndpointHandler {

    private static final Logger log = LoggerFactory.getLogger(FulfillmentHandler.class);
    private final static String transferUUID= "transferUUID";

	// GET|PUT /transfers/25644640-d140-450e-b94b-badbe23d3389/fulfillment
	// PUT /transfers/4e36fe38-8171-4aab-b60e-08d4b56fbbf1/rejection

    public FulfillmentHandler() {
       // REF: _makeRouter @ five-bells-ledger/src/lib/app.js
        super("transfer", new String[] 
            {
                "transfers/:" + transferUUID + "/fulfillment",
                "transfers/:" + transferUUID + "/rejection",
            });
        accept(GET, PUT);
    }

    public static FulfillmentHandler create() {
        return new FulfillmentHandler(); // TODO: return singleton?
    }

    @Override
    protected void handlePut(RoutingContext context) {
        // FIXME: If debit's account owner != request credentials throw exception.
        // PUT /transfers/25644640-d140-450e-b94b-badbe23d3389/fulfillment
        // PUT /transfers/4e36fe38-8171-4aab-b60e-08d4b56fbbf1/rejection
        log.trace(this.getClass().getName() + "handlePut invoqued ");
        // boolean isFulfillment = false, isRejection   = false;
        /**********************
         * PUT/GET fulfillment (FROM ILP-CONNECTOR)
         *********************
         *
         * PUT /transfers/25644640-d140-450e-b94b-badbe23d3389/fulfillment HTTP/1.1
         *     HTTP 1.1 200 OK
         *     cf:0:ZXhlY3V0ZQ
         * 
         * GET /transfers/25644640-d140-450e-b94b-badbe23d3389/fulfillment HTTP/1.1
         *     HTTP 1.1 200 OK
         *     cf:0:ZXhlY3V0ZQ
         */
        TransferID transferID = new TransferID(context.request().getParam(transferUUID));
        LedgerTransferManager tm = SimpleLedgerTransferManager.getSingleton();
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
        LedgerTransfer transfer = tm.getTransferById(transferID);
        if ( transfer.getExecutionCondition() == null /* TODO:(0) Replace by DOESNT_EXITS */ ) {
            ILPExceptionSupport.launchILPException(
                    InterledgerError.ErrorCode.F00_BAD_REQUEST,
                    this.getClass().getName() + "Transfer is not conditional");
        }
        String hexFulfillment = context.getBodyAsString();
        // REF: http://stackoverflow.com/questions/140131/convert-a-string-representation-of-a-hex-dump-to-a-byte-array-using-java
        byte[] fulfillmentBytes = DatatypeConverter.parseHexBinary(hexFulfillment);
        
        Fulfillment ff;
        try {
            ff = CryptoConditionReader.readFulfillment(fulfillmentBytes);
        } catch (DEREncodingException e1) {
            throw new RuntimeException("body request '"+hexFulfillment+"' can not be parsed as HEX -> DER");
        }
        byte[] message = new byte[]{};
        boolean ffExisted = false;
        log.trace("transfer.getExecutionCondition():"+transfer.getExecutionCondition().toString());
        log.trace("transfer.getCancellationCondition():"+transfer.getCancellationCondition().toString());
        log.trace("request hexFulfillment:"+hexFulfillment);
        log.trace("request ff.getCondition():"+ff.getCondition().toString());

        if (/*isFulfillment && */transfer.getExecutionCondition().equals(ff.getCondition()) ) {
            ffExisted = transfer.getExecutionFulfillment().equals(ff);
            if (!ffExisted) {
                if (!ff.verify(ff.getCondition(), message)){
                    throw new RuntimeException("execution fulfillment doesn't validate");
                }
                tm.executeRemoteILPTransfer(transfer, ff);

            }
        } else if (/*isRejection && */transfer.getCancellationCondition().equals(ff.getCondition()) ){
            ffExisted = transfer.getCancellationFulfillment().equals(ff);
            if (!ffExisted) {
                if (!ff.verify(ff.getCondition(), message)){
                    throw new RuntimeException("cancelation fulfillment doesn't validate");
                }
                tm.abortRemoteILPTransfer(transfer, ff);
            }
        } else {
            ILPExceptionSupport.launchILPException(
                    InterledgerError.ErrorCode.F05_WRONG_CONDITION,
                    this.getClass().getName() + "Fulfillment does not match any condition");
        }
        log.trace("ffExisted:"+ffExisted);

        String response = ff.toString(); /*TODO:(0) Recheck. It was fulfillmentURI previously */
        context.response()
            .putHeader(HttpHeaders.CONTENT_TYPE, "text/plain")
            .putHeader(HttpHeaders.CONTENT_LENGTH, ""+response.length())
            .setStatusCode(!ffExisted ? HttpResponseStatus.CREATED.code() : HttpResponseStatus.OK.code())
            .end(response);
        try {
            String notification = ((SimpleLedgerTransfer) transfer).toMessageStringifiedFormat().encode();
            // Notify affected accounts:
            for (Debit  debit  : transfer.getDebits() ) {
                TransferWSEventHandler.notifyListener(context, debit.account, notification);
            }
            for (Credit credit : transfer.getCredits() ) {
                TransferWSEventHandler.notifyListener(context, credit.account, notification);
            }
        } catch (Exception e) {
            log.warn("Fulfillment registrered correctly but ilp-connector couldn't be notified due to " + e.toString());
        }
    }

    @Override
    protected void handleGet(RoutingContext context) {
        // GET /transfers/25644640-d140-450e-b94b-badbe23d3389/fulfillment 
        //                                                    /rejection
        log.trace(this.getClass().getName() + " handleGet invoqued ");
        AuthInfo ai = AuthManager.authenticate(context);
        
        boolean transferMatchUser = true; // FIXME: TODO:(0) implement
        if (!ai.isAdmin() && !transferMatchUser) {
            ILPExceptionSupport.launchILPForbiddenException();
        }
        boolean isFulfillment = false; // false => isRejection
        if (context.request().path().endsWith("/fulfillment")){
            isFulfillment = true;
        } else if (context.request().path().endsWith("/rejection")){
            isFulfillment = false;
            /*
             * FIXME: rejection request doesn't look to be symmetrical with fulfillments.
             *    INFO: 127.0.0.1 - - [Wed, 16 Nov 2016 09:05:21 GMT] 
             *    "PUT /transfers/eec954ec-005e-460a-8dd6-829161da05ac/rejection HTTP/1.1" 200 19 "-" "-"
             *       Handle exception java.lang.IllegalArgumentException: serializedFulfillment 'transfer timed out.' must start with 'cf:'
             *       java.lang.IllegalArgumentException: serializedFulfillment 'transfer timed out.' must start with 'cf:'
             *       at org.interledger.cryptoconditions.FulfillmentFactory.getFulfillmentFromURI(FulfillmentFactory.java:24)
             */
        } else {
            throw new RuntimeException("path doesn't match /fulfillment | /rejection");
        }
        LedgerTransferManager tm = SimpleLedgerTransferManager.getSingleton();
        TransferID transferID = new TransferID(context.request().getParam(transferUUID));
        LedgerTransfer transfer = tm.getTransferById(transferID);
        if (transfer.getExecutionCondition()==null /* TODO:(0) Remove null*/){
            // TODO:(0) This could mean a crytical security error. At some point the condition was "lost"
            //   while the already-registered-transfer is supposed to have it attached to "lock" the execution.
            ILPExceptionSupport.launchILPException(
                    InterledgerError.ErrorCode.F05_WRONG_CONDITION,
                    this.getClass().getName());
        }
        Fulfillment fulfillment= (isFulfillment) 
                ? transfer.getExecutionFulfillment()
                : transfer.getCancellationFulfillment();
        if ( fulfillment == null /* TODO:(0) Fix null */) {
            ILPExceptionSupport.launchILPException(
                    InterledgerError.ErrorCode.F99_APPLICATION_ERROR,
                this.getClass().getName() + "This transfer has not yet been fulfilled");
        }

        String response  = fulfillment.toString(); // TODO:(0)  previously fulfillmentURI
        context.response()
            .putHeader(HttpHeaders.CONTENT_TYPE, "text/plain")
            .putHeader(HttpHeaders.CONTENT_LENGTH, ""+response.length())
            .setStatusCode(HttpResponseStatus.OK.code())
            .end(response);
    }
    
}

