package com.everis.everledger.handlers;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.RoutingContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.everis.everledger.impl.manager.SimpleAccountManager;
import com.everis.everledger.impl.manager.SimpleTransferManager;

/**
 * Helper handler for testing. 
 *
 * <p>Reset state for accounts and transfers,...</p>
 */
public class DeveloperTestingRequestHandler extends RestEndpointHandler {
    private static final Logger log = LoggerFactory.getLogger(DeveloperTestingRequestHandler.class);
    
    private SimpleTransferManager TM = (SimpleTransferManager)SimpleTransferManager.INSTANCE;

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
        SimpleAccountManager.developerTestingReset();
        response(
                context,
                HttpResponseStatus.OK,
                buildJSON("","") );
    }

}
