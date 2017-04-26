package org.interledger.everledger.ledger.api.handlers;

import java.net.URI;
import java.util.List;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.web.RoutingContext;

import org.interledger.cryptoconditions.Condition;
import org.interledger.cryptoconditions.uri.CryptoConditionUri;
import org.interledger.cryptoconditions.uri.URIEncodingException;
import org.interledger.everledger.common.api.auth.AuthInfo;
import org.interledger.everledger.common.api.auth.AuthManager;
import org.interledger.everledger.common.api.handlers.RestEndpointHandler;
import org.interledger.everledger.common.api.util.ILPExceptionSupport;
import org.interledger.everledger.ledger.impl.simple.SimpleLedgerTransfer;
import org.interledger.everledger.ledger.impl.simple.SimpleLedgerTransferManager;
import org.interledger.everledger.ledger.transfer.IfaceILPSpecTransferManager;
import org.interledger.everledger.ledger.transfer.LedgerTransfer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TransferHandler handler
 *
 * REF: five-bells-ledger/src/controllers/transfers.js
 */
public class TransfersHandler extends RestEndpointHandler {

    private static final Logger log = LoggerFactory.getLogger(TransfersHandler.class);
    private final static String execCondition = "execCondition";
    // GET /transfers/byExecutionCondition/cc:0:3:vmvf6B7EpFalN6RGDx9F4f4z0wtOIgsIdCmbgv06ceI:7 

    public TransfersHandler() {
        // REF: https://github.com/interledger/five-bells-ledger/blob/master/src/lib/app.js
        super( new HttpMethod[] {HttpMethod.GET} ,
                new String[] { "transfers/byExecutionCondition/:" + execCondition }
            );
    }

    public static TransfersHandler create() {
        return new TransfersHandler(); // TODO: return singleton?
    }

    @Override
    protected void handleGet(RoutingContext context) {
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
        log.trace(this.getClass().getName() + "handleGet invoqued ");
        AuthInfo ai = AuthManager.authenticate(context);
        boolean transferMatchUser = false;
        
        IfaceILPSpecTransferManager tm = SimpleLedgerTransferManager.getILPSpecTransferManager();
//        Condition condition = CryptoConditionUri.parse(URI.create(testVector.getConditionUri()));
        String sExecCond = context.request().getParam(execCondition);
        Condition executionCondition;
        try {
            executionCondition = CryptoConditionUri.parse(URI.create(sExecCond));
        } catch (URIEncodingException e) {
            throw new RuntimeException("'"+ sExecCond + "' can't be parsed as URI");
        }
        List<LedgerTransfer> transferList = tm.getTransfersByExecutionCondition(executionCondition);
        
        JsonArray ja = new JsonArray();
        for (LedgerTransfer transfer : transferList) {
            if (ai.isAdmin() 
                 || transfer.getDebits ()[0].account.getLocalName().equals(ai.getId())
                 || transfer.getCredits()[0].account.getLocalName().equals(ai.getId())
               ) {
                ja.add(((SimpleLedgerTransfer)transfer).toILPJSONStringifiedFormat());
                transferMatchUser = true;
            }
        }
        if (!ai.isAdmin() && !transferMatchUser) {
            throw ILPExceptionSupport.createILPForbiddenException();
        }
        String response = ja.encode();
        context.response()
            .putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
            .putHeader(HttpHeaders.CONTENT_LENGTH, ""+response.length())
            .setStatusCode(HttpResponseStatus.OK.code())
            .end(response);
    }

}



