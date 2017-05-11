package org.interledger.everledger.handlers;

import javax.xml.bind.DatatypeConverter;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.RoutingContext;

import org.interledger.cryptoconditions.Fulfillment;
import org.interledger.cryptoconditions.der.CryptoConditionReader;
import org.interledger.cryptoconditions.der.DEREncodingException;
import org.interledger.everledger.common.api.auth.AuthInfo;
import org.interledger.everledger.common.api.auth.AuthManager;
import org.interledger.everledger.handlers.RestEndpointHandler;
import org.interledger.everledger.impl.SimpleLedgerTransfer;
import org.interledger.everledger.impl.SimpleLedgerTransferManager;
import org.interledger.everledger.common.api.util.ILPExceptionSupport;
import org.interledger.everledger.ledger.transfer.Credit;
import org.interledger.everledger.ledger.transfer.Debit;
import org.interledger.everledger.ledger.transfer.ILPSpecTransferID;
import org.interledger.everledger.ledger.transfer.IfaceILPSpecTransferManager;
import org.interledger.everledger.ledger.transfer.IfaceLocalTransferManager;
import org.interledger.everledger.ledger.transfer.LedgerTransfer;
import org.interledger.everledger.ledger.transfer.LocalTransferID;
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
        super(
                new HttpMethod[] {HttpMethod.GET, HttpMethod.PUT},
                new String[] {
                        "transfers/:" + transferUUID + "/fulfillment",
                        "transfers/:" + transferUUID + "/rejection"
                }
            );
    }

    public static FulfillmentHandler create() {
        return new FulfillmentHandler(); // TODO: return singleton?
    }

    @Override
    protected void handlePut(RoutingContext context) {
        // PUT /transfers/25644640-d140-450e-b94b-badbe23d3389/fulfillment
        // PUT /transfers/4e36fe38-8171-4aab-b60e-08d4b56fbbf1/rejection
        AuthInfo ai = AuthManager.authenticate(context);

        log.trace(this.getClass().getName() + "handlePut invoqued ");

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
        ILPSpecTransferID ilpTransferID = new ILPSpecTransferID(context.request().getParam(transferUUID));
        LocalTransferID      transferID = LocalTransferID.ILPSpec2LocalTransferID(ilpTransferID);
        IfaceILPSpecTransferManager ilpTM = SimpleLedgerTransferManager.getILPSpecTransferManager();
        IfaceLocalTransferManager localTM = SimpleLedgerTransferManager.getLocalTransferManager();

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
        LedgerTransfer transfer = localTM.getLocalTransferById(transferID);
        if ( transfer.getExecutionCondition() == SimpleLedgerTransfer.CC_NOT_PROVIDED) {
            ILPExceptionSupport.createILPInternalException(
                    this.getClass().getName() + "Transfer is not conditional");
        }
        boolean transferMatchUser = // TODO:(?) Recheck 
            ai.getId().equals(transfer.getDebits ()[0].account.getLocalName())
         || ai.getId().equals(transfer.getCredits()[0].account.getLocalName()) ;
        if ( !ai.isAdmin()  &&  !transferMatchUser) {
            throw ILPExceptionSupport.createILPForbiddenException();
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
                ilpTM.executeRemoteILPTransfer(transfer, ff);

            }
        } else if (/*isRejection && */transfer.getCancellationCondition().equals(ff.getCondition()) ){
            ffExisted = transfer.getCancellationFulfillment().equals(ff);
            if (!ffExisted) {
                if (!ff.verify(ff.getCondition(), message)){
                    throw new RuntimeException("cancelation fulfillment doesn't validate");
                }
                ilpTM.abortRemoteILPTransfer(transfer, ff);
            }
        } else {
            ILPExceptionSupport.createILPException(
                    403,
                    InterledgerError.ErrorCode.F05_WRONG_CONDITION,
                    this.getClass().getName() + "Fulfillment does not match any condition");
        }
        log.trace("ffExisted:"+ffExisted);

        String response = ff.toString();
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
        
        boolean transferMatchUser = false;
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
        IfaceLocalTransferManager localTM = SimpleLedgerTransferManager.getLocalTransferManager();

        ILPSpecTransferID ilpTransferID = new ILPSpecTransferID(context.request().getParam(transferUUID));
        LocalTransferID      transferID = LocalTransferID.ILPSpec2LocalTransferID(ilpTransferID);

        LedgerTransfer transfer = localTM.getLocalTransferById(transferID);
        transferMatchUser = false 
                || ai.getId().equals(transfer.getDebits ()[0].account.getLocalName())
                || ai.getId().equals(transfer.getCredits()[0].account.getLocalName()) ;
        if ( !ai.isAdmin()  &&  !(ai.isConnector() && transferMatchUser)  ){
            throw ILPExceptionSupport.createILPForbiddenException();
        }

        Fulfillment fulfillment= (isFulfillment) 
                ? transfer.getExecutionFulfillment()
                : transfer.getCancellationFulfillment();
        if ( fulfillment == SimpleLedgerTransfer.FF_NOT_PROVIDED) {
            ILPExceptionSupport.createILPException(
                    500,
                    InterledgerError.ErrorCode.F99_APPLICATION_ERROR,
                this.getClass().getName() + "This transfer has not yet been fulfilled");
        }

        String response  = fulfillment.toString();
        context.response()
            .putHeader(HttpHeaders.CONTENT_TYPE, "text/plain")
            .putHeader(HttpHeaders.CONTENT_LENGTH, ""+response.length())
            .setStatusCode(HttpResponseStatus.OK.code())
            .end(response);
    }
}