package org.interledger.ilp.ledger.api.handlers;

// TESTING FROM COMMAND LINE: https://blogs.oracle.com/PavelBucek/entry/websocket_command_line_client
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;

import static io.vertx.core.http.HttpMethod.GET;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.ext.web.RoutingContext;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.interledger.ilp.common.api.handlers.RestEndpointHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author earizon TransferWSEventHandler handler Wrapper to HTTP GET request to
 * upgrade it to WebSocketEventHandler ILP-Connector five-bells-plugins will
 * connect to a URL similar to: /accounts/alice/transfers This (GET) request
 * will be upgraded to a webSocket connection in order to send back internal
 * events (transfer accepted, rejected, ...)
 *
 * Internal java-ilp-ledger components will inform of events to this Handler
 * using a code similar to:
 *
 * String wsID =
 * TransferWSEventHandler.getServerWebSocketHandlerID(ilpConnectorIP);
 * context.vertx().eventBus().send(wsID, "PUT
 * transferID:"+transferID.transferID);
 */
// FIXME: implements ProtectedResource required?
public class TransferWSEventHandler extends RestEndpointHandler/* implements ProtectedResource */ {

    private static final Logger log = LoggerFactory.getLogger(TransferWSEventHandler.class);

    private final static String PARAM_NAME = "name";

    public TransferWSEventHandler() {
        super("transfer", "accounts/:" + PARAM_NAME + "/transfers");
        accept(GET);
    }

    public static TransferWSEventHandler create() {
        return new TransferWSEventHandler(); // TODO: return singleton?
    }

    @Override
    protected void handleGet(RoutingContext context) {
        String accountName = context.request().getParam(PARAM_NAME);
        // GET /accounts/alice/transfers -> Upgrade to websocket
        log.debug("TransferWSEventHandler Connected. Upgrading HTTP GET to WebSocket!");
        ServerWebSocket sws = context.request().upgrade();
        /* 
         * From vertx Docs:
         *    """When a Websocket is created it automatically registers an event 
         *    handler with the event bus - the ID of that handler is given by 
         *    this method. Given this ID, a different event loop can send a 
         *    binary frame to that event handler using the event bus and that 
         *    buffer will be received by this instance in its own event loop 
         *    and written to the underlying connection. This allows you to 
         *    write data to other WebSockets which are owned by different event loops."""
         */
        /* REF:
         * function * subscribeTransfer @ src/controllers/accounts: 
         *   // Send a message upon connection
         *   this.websocket.send(JSON.stringify({ type: 'connect' })) 
         */
        sws.writeFinalTextFrame("{\"type\" : \"connect\" }"); // TODO: recheck this line
        registerServerWebSocket(context, accountName, sws);
    }

    private static void registerServerWebSocket(RoutingContext context, String accountName, ServerWebSocket sws) {
        String handlerID = sws.textHandlerID(); // | binaryHandlerID

        log.debug("registering WS connection: "+accountName);

        sws.frameHandler/* bytes read from the connector */(/*WebSocketFrame*/frame -> {
               log.debug("ilpConnector input frame -> frame.textData()   " + frame.textData());
               log.debug("ilpConnector input frame -> frame.binaryData() " + frame.binaryData());
           });

        EventBus eventBus = context.vertx().eventBus();

        io.vertx.core.eventbus.MessageConsumer<String> mc = 
                eventBus.consumer("message-" + accountName, message -> { 
            log.debug("received '"+message.body()+"' from internal *Manager:");
            sws.writeFinalTextFrame(message.body());
            log.debug("message forwarded to websocket peer through websocket");
            });
        
        sws.closeHandler(new Handler<Void>() {
            @Override
            public void handle(final Void event) {
                log.debug("un-registering WS connection: "+accountName);
                mc.unregister();
            }
        });

        sws.exceptionHandler(/*Handler<Throwable>*/ throwable -> {
            StringWriter writer = new StringWriter();
            PrintWriter printWriter = new PrintWriter( writer );
            throwable.printStackTrace( printWriter );
            printWriter.flush();
            String stackTrace = writer.toString();
            log.warn("There was an exception in the ws "+accountName + ":"+throwable.toString()+ "\n" +stackTrace );
        });
    }

    /**
     * Send transacction status update to the ILP connector
     *
     * @param context
     * @param message
     */
    public static void notifyListener(RoutingContext context, String account, String message) {
        // Send notification to all existing webSockets
        log.debug("notifyListener to account:"+account + ", message:'''" + message + "'''\n");
        context.vertx().eventBus().send("message-"+account, message); // will be sent to handler "@bookmark1"
        
    }

}
