package org.interledger.ilp.ledger.api.handlers;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.http.HttpHeaders;
//import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.ext.web.RoutingContext;

import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.PUT;

import org.interledger.cryptoconditions.Fulfillment;
import org.interledger.cryptoconditions.FulfillmentFactory;
import org.interledger.cryptoconditions.types.MessagePayload;
import org.interledger.ilp.common.api.ProtectedResource;
import org.interledger.ilp.common.api.auth.impl.SimpleAuthProvider;
import org.interledger.ilp.core.InterledgerException;
import org.interledger.ilp.common.api.handlers.RestEndpointHandler;
import org.interledger.ilp.core.ConditionURI;
import org.interledger.ilp.core.Credit;
import org.interledger.ilp.core.Debit;
import org.interledger.ilp.core.FulfillmentURI;
import org.interledger.ilp.core.TransferID;
import org.interledger.ilp.core.ledger.model.LedgerTransfer;
import org.interledger.ilp.ledger.impl.simple.SimpleLedgerTransfer;
import org.interledger.ilp.ledger.impl.simple.SimpleLedgerTransferManager;
import org.interledger.ilp.ledger.transfer.LedgerTransferManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Fulfillment (and rejection) handler
 * 
 * REF: five-bells-ledger/src/controllers/transfers.js
 */
public class FulfillmentHandler extends RestEndpointHandler implements ProtectedResource {

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
        if (ConditionURI.EMPTY.equals(transfer.getURIExecutionCondition())){
            throw new InterledgerException(
                    InterledgerException.RegisteredException.TransferNotConditionalError,
                    "Transfer is not conditional");
        }
        String   fulfillmentURI = context.getBodyAsString();
        log.trace("fulfillmentURI: "+fulfillmentURI);
        Fulfillment          ff = FulfillmentFactory.getFulfillmentFromURI(fulfillmentURI);
        MessagePayload message = new MessagePayload(new byte[]{});
        boolean ffExisted = false;
        log.trace("transfer.getURIExecutionCondition().URI:"+transfer.getURIExecutionCondition().URI.toString());
        log.trace("transfer.getURICancellationCondition().URI:"+transfer.getURICancellationCondition().URI.toString());
        log.trace("request fulfillmentURI:"+fulfillmentURI);
        log.trace("request ff.getCondition().toURI():"+ff.getCondition().toURI());

        if (/*isFulfillment && */transfer.getURIExecutionCondition().URI.equals(ff.getCondition().toURI()) ) {
            ffExisted = transfer.getURIExecutionFulfillment().URI.equals(fulfillmentURI);
            if (!ffExisted) {
                if (!ff.validate(message)){
                    throw new RuntimeException("execution fulfillment doesn't validate");
                }
                tm.executeRemoteILPTransfer(transfer, new FulfillmentURI(fulfillmentURI));

            }
        } else if (/*isRejection && */transfer.getURICancellationCondition().URI.equals(ff.getCondition().toURI()) ){
            ffExisted = transfer.getURICancellationFulfillment().URI.equals(fulfillmentURI);
            if (!ffExisted) {
                if (!ff.validate(message)){
                    throw new RuntimeException("cancelation fulfillment doesn't validate");
                }
                tm.abortRemoteILPTransfer(transfer, new FulfillmentURI(fulfillmentURI));
            }
        } else {
            throw new InterledgerException(InterledgerException.RegisteredException.UnmetConditionError, "Fulfillment does not match any condition");
        }
        log.trace("ffExisted:"+ffExisted);

        context.response()
            .putHeader(HttpHeaders.CONTENT_TYPE, "text/plain")
            .putHeader(HttpHeaders.CONTENT_LENGTH, ""+fulfillmentURI.length())
            .setStatusCode(!ffExisted ? HttpResponseStatus.CREATED.code() : HttpResponseStatus.OK.code())
            .end(fulfillmentURI);
        try {
            String notification = ((SimpleLedgerTransfer) transfer).toMessageStringifiedFormat().encode();
            // Notify affected accounts:
            for (Debit  debit  : transfer.getDebits() ) {
                TransferWSEventHandler.notifyListener(context, debit.account.getAccountId(), notification);
            }
            for (Credit credit : transfer.getCredits() ) {
                TransferWSEventHandler.notifyListener(context, credit.account.getAccountId(), notification);
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
        SimpleAuthProvider.SimpleUser user = (SimpleAuthProvider.SimpleUser) context.user();
        boolean isAdmin = user.hasRole("admin");
        boolean transferMatchUser = true; // FIXME: TODO: implement
        if (!isAdmin && !transferMatchUser) {
            throw new InterledgerException(InterledgerException.RegisteredException.ForbiddenError);
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
        if (ConditionURI.EMPTY.equals(transfer.getURIExecutionCondition())){
            throw new InterledgerException(
                    InterledgerException.RegisteredException.TransferNotConditionalError,
                    "Transfer does not have any conditions");
        }
        String fulfillmentURI = (isFulfillment) 
                ? transfer.getURIExecutionFulfillment().URI
                : transfer.getURICancellationFulfillment().URI;
        if ( FulfillmentURI.MISSING.URI.equals(fulfillmentURI)) {
            throw new InterledgerException(
                InterledgerException.RegisteredException.MissingFulfillmentError,
                "This transfer has not yet been fulfilled");
        }
        context.response()
            .putHeader(HttpHeaders.CONTENT_TYPE, "text/plain")
            .putHeader(HttpHeaders.CONTENT_LENGTH, ""+fulfillmentURI.length())
            .setStatusCode(HttpResponseStatus.OK.code())
            .end(fulfillmentURI);
    }
}

