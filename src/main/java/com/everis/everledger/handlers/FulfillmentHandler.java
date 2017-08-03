package com.everis.everledger.handlers;

import java.time.ZonedDateTime;
import java.util.UUID;
import java.util.Base64;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.RoutingContext;

import org.interledger.Fulfillment;

//import org.interledger.cryptoconditions.der.CryptoConditionReader;
//import org.interledger.cryptoconditions.der.DEREncodingException;
import org.interledger.ledger.model.TransferStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.everis.everledger.AuthInfo;
import com.everis.everledger.Config;
import com.everis.everledger.handlers.RestEndpointHandler;
import com.everis.everledger.ifaces.transfer.IfaceTransfer;
import com.everis.everledger.ifaces.transfer.IfaceTransferManager;
import com.everis.everledger.transfer.LocalTransferID;
import com.everis.everledger.util.AuthManager;
import com.everis.everledger.util.ConversionUtil;
import com.everis.everledger.util.ILPExceptionSupport;

import com.everis.everledger.impl.SimpleTransfer;
import com.everis.everledger.impl.TransferKt;
import com.everis.everledger.impl.manager.SimpleTransferManager;
/**
 * Fulfillment handler
 * 
 * REF: five-bells-ledger/src/controllers/transfers.js
 */
public class FulfillmentHandler extends RestEndpointHandler {

    private static final Logger log = LoggerFactory.getLogger(FulfillmentHandler.class);
    private static final String transferUUID= "transferUUID";
    private static final IfaceTransferManager TM = SimpleTransferManager.INSTANCE;

	/*
	 *  GET|PUT /transfers/25644640-d140-450e-b94b-badbe23d3389/fulfillment
	 *  fulfillment can be execution or cancellation
	 *  Note: Rejection != cancellation. Rejection in five-bells-ledger refers
	 *      to the rejection in the proposed (not-yet prepared) transfer (or part of the
	 *      transfer).
	 *      In the java-vertx-ledger there is not yet (2017-05) concept of proposed
	 *      state.
	 */

    public FulfillmentHandler() {
       // REF: _makeRouter @ five-bells-ledger/src/lib/app.js
        super(
                new HttpMethod[] {HttpMethod.GET, HttpMethod.PUT},
                new String[] {
                        "transfers/:" + transferUUID + "/fulfillment"
                }
            );
    }

    public static FulfillmentHandler create() {
        return new FulfillmentHandler(); // TODO: return singleton?
    }

    @Override
    protected void handlePut(RoutingContext context) {
        // PUT /transfers/25644640-d140-450e-b94b-badbe23d3389/fulfillment
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
        UUID ilpTransferID = UUID.fromString(context.request().getParam(transferUUID));
        LocalTransferID      transferID = LocalTransferID.ILPSpec2LocalTransferID(ilpTransferID);
        
//        IfaceILPSpecTransferManager ilpTM = SimpleLedgerTransferManager.getILPSpecTransferManager();
//        IfaceLocalTransferManager localTM = SimpleLedgerTransferManager.getLocalTransferManager();

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
        IfaceTransfer transfer = TM.getTransferById(transferID);
        if ( transfer.getExecutionCondition() == TransferKt.getCC_NOT_PROVIDED()) {
            throw ILPExceptionSupport.createILPUnprocessableEntityException(
                    this.getClass().getName() + "Transfer is not conditional");
        }
        boolean transferMatchUser = // TODO:(?) Recheck 
            ai.getId().equals(transfer.getDebits ()[0].account.getLocalID())
         || ai.getId().equals(transfer.getCredits()[0].account.getLocalID()) ;
        if ( !ai.isAdmin()  &&  !transferMatchUser) {
            throw ILPExceptionSupport.createILPForbiddenException();
        }

        String sFulfillmentInput = context.getBodyAsString();
        byte[] fulfillmentBytes = Base64.getDecoder().decode(sFulfillmentInput);

//        // REF: http://stackoverflow.com/questions/140131/convert-a-string-representation-of-a-hex-dump-to-a-byte-array-using-java
//        byte[] fulfillmentBytes = DatatypeConverter.parseHexBinary(sFulfillment);
        
        Fulfillment inputFF = Fulfillment.of(fulfillmentBytes);

        byte[] message = new byte[]{};
        boolean ffExisted = false;
        log.trace("transfer.getExecutionCondition():"+transfer.getExecutionCondition().toString());
//        log.trace("transfer.getCancellationCondition():"+transfer.getCancellationCondition().toString());
        log.trace("request hexFulfillment:"+sFulfillmentInput);
        log.trace("request ff.getCondition():"+inputFF.getCondition().toString());

        if (transfer.getExecutionCondition().equals(inputFF.getCondition()) ) {
            if ( !inputFF.validate(inputFF.getCondition()) ){
                throw ILPExceptionSupport.createILPUnprocessableEntityException("execution fulfillment doesn't validate");
            }

            if (transfer.getExpiresAt().compareTo(ZonedDateTime.now())<0 && Config.unitTestsActive == false) {
                throw ILPExceptionSupport.createILPUnprocessableEntityException("transfer expired");
            }
            if ( transfer.getTransferStatus() != TransferStatus.EXECUTED) { TM.executeILPTransfer(transfer, inputFF); }
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
            throw ILPExceptionSupport.
                createILPUnprocessableEntityException(
                    "Fulfillment does not match any condition");
        }
        log.trace("ffExisted:"+ffExisted);

        String response  = ConversionUtil.fulfillmentToBase64(inputFF);
        if (!sFulfillmentInput.equals(response)) {
            throw ILPExceptionSupport.createILPBadRequestException(
                "Assert exception. Input '"+sFulfillmentInput+"'doesn't match output '"+response+"' ");
        }
        context.response()
            .putHeader(HttpHeaders.CONTENT_TYPE, "text/plain")
            .putHeader(HttpHeaders.CONTENT_LENGTH, ""+response.length())
            .setStatusCode(!ffExisted ? HttpResponseStatus.CREATED.code() : HttpResponseStatus.OK.code())
            .end(response);
    }

    @Override
    protected void handleGet(RoutingContext context) {
        // GET /transfers/25644640-d140-450e-b94b-badbe23d3389/fulfillment 
        log.trace(this.getClass().getName() + " handleGet invoqued ");
        AuthInfo ai = AuthManager.authenticate(context);
        
        boolean transferMatchUser = false;

        UUID ilpTransferID = UUID.fromString(context.request().getParam(transferUUID));
        LocalTransferID      transferID = LocalTransferID.ILPSpec2LocalTransferID(ilpTransferID);

        IfaceTransfer transfer = TM.getTransferById(transferID);
        
        transferMatchUser = false 
                || ai.getId().equals(transfer.getDebits ()[0].account.getLocalID())
                || ai.getId().equals(transfer.getCredits()[0].account.getLocalID()) ;
        if ( !ai.isAdmin()  &&  !(ai.isConnector() && transferMatchUser)  ){
            throw ILPExceptionSupport.createILPForbiddenException();
        }

        Fulfillment fulfillment=  transfer.getExecutionFulfillment();
        if ( fulfillment == TransferKt.getFF_NOT_PROVIDED()) {
            if (transfer.getExpiresAt().compareTo(ZonedDateTime.now())<0) {
                throw ILPExceptionSupport.createILPNotFoundException("This transfer expired before it was fulfilled");
            }
            throw ILPExceptionSupport.createILPUnprocessableEntityException("Unprocessable Entity");
        }

        String response  = ConversionUtil.fulfillmentToBase64(fulfillment); 

        context.response()
            .putHeader(HttpHeaders.CONTENT_TYPE, "text/plain")
            .putHeader(HttpHeaders.CONTENT_LENGTH, ""+response.length())
            .setStatusCode(HttpResponseStatus.OK.code())
            .end(response);
    }

}