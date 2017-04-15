package org.interledger.ilp.ledger.api.handlers;

import io.netty.handler.codec.http.HttpResponseStatus;
import static io.vertx.core.http.HttpMethod.*;

import io.vertx.core.http.HttpHeaders;
import io.vertx.ext.web.RoutingContext;
import org.interledger.ilp.common.api.handlers.RestEndpointHandler;
import org.interledger.ilp.ledger.impl.simple.SimpleLedgerTransferManager;
import org.interledger.ilp.ledger.transfer.LedgerTransferManager;

/**
 * Single Account handler
 *
 * @author earizon
 */
public class UnitTestSupportHandler extends RestEndpointHandler {

    private final static String action  = "action";

    public UnitTestSupportHandler() {
        
        super("unitTestSupport", new String[] 
                {
                    "unitTestTransactionSupport/:" + action,
                });
        accept(GET);
    }

    public static UnitTestSupportHandler create() {
        return new UnitTestSupportHandler();
    }

    @Override
    protected void handleGet(RoutingContext context) {
        String sAction = context.request().getParam( action );
        HttpResponseStatus resultStatus = HttpResponseStatus.OK;
        String result = "OK";
        LedgerTransferManager tm = SimpleLedgerTransferManager.getSingleton();
        if ("resetTransactions".equals(sAction)) {
            ((SimpleLedgerTransferManager)tm).unitTestsResetTransactionDDBB();
        } else if ("getTotalTransactions".equals(sAction)) {
            result = ((SimpleLedgerTransferManager)tm).unitTestsGetTotalTransactions();
        } else {
            resultStatus = HttpResponseStatus.BAD_REQUEST;
            result = "KO";
        }
        context.response()
        .putHeader(HttpHeaders.CONTENT_TYPE, "text/plain")
        .putHeader(HttpHeaders.CONTENT_LENGTH, ""+result.length())
        .setStatusCode(resultStatus.code())
        .end(result);
    }

}
