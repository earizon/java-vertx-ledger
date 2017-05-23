package org.interledger.everledger.handlers;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.RoutingContext;

import org.interledger.everledger.impl.manager.SimpleLedgerAccountManager;
import org.interledger.everledger.impl.manager.SimpleLedgerTransferManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper handler for testing. 
 *
 * <p>Reset state for accounts and transfers,...</p>
 */
public class DeveloperTestingRequestHandler extends RestEndpointHandler {
    private static final Logger log = LoggerFactory.getLogger(DeveloperTestingRequestHandler.class);
    
    private SimpleLedgerTransferManager TM = (SimpleLedgerTransferManager)SimpleLedgerTransferManager.getTransferManager();

    public static DeveloperTestingRequestHandler create() {
        return new DeveloperTestingRequestHandler(); // TODO: return singleton?
    }

    public DeveloperTestingRequestHandler() {
        // REF:
        // https://github.com/interledgerjs/five-bells-ledger/blob/master/src/lib/app.js
        super(
                new HttpMethod[] {HttpMethod.GET},
                new String[] { "developerTesting/reset"}
            );
    }
    
    @Override
    public void handle(RoutingContext context) {
        log.info("reseting ...");
        TM.developerTestingResetTransfers();
        SimpleLedgerAccountManager.developerTestingReset();
        response(
                context,
                HttpResponseStatus.OK,
                buildJSON("","") );
    }

}
