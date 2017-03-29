
package org.interledger.ilp.ledger.api.handlers;

import org.interledger.ilp.common.api.handlers.EndpointHandler;
import org.interledger.ilp.common.api.handlers.RestEndpointHandler;

/**
 * List available connectors
 * 
 * @author mrmx
 */
public class ConnectorsHandler extends RestEndpointHandler {

    public ConnectorsHandler() {
        super("connectors");
    }

    public static EndpointHandler create() {
        return new ConnectorsHandler();
    }

   
    
}
