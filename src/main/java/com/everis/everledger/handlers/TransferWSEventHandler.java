package com.everis.everledger.handlers;

// TESTING FROM COMMAND LINE: https://blogs.oracle.com/PavelBucek/entry/websocket_command_line_client
import io.vertx.core.Handler;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.ServerWebSocket;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.everis.everledger.AccountManagerFactory;
import com.everis.everledger.AuthInfo;
import com.everis.everledger.handlers.RestEndpointHandler;
import com.everis.everledger.ifaces.account.IfaceAccount;
import com.everis.everledger.impl.manager.SimpleAccountManager;
import com.everis.everledger.util.AuthManager;

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
    // TODO:(0) Protect listeners access. Can be accesed from different threads
    //      simultaneously.
    public static HashMap<ServerWebSocket,
            HashMap<String /*account*/, Set<EventType> > > listeners = 
              new HashMap<ServerWebSocket, HashMap<String /*account*/, Set<EventType> > >();

    public static enum EventType {
        ANY            ("*"),
        TRANSFER_CREATE("transfer.create"),
        TRANSFER_UPDATE("transfer.update"),
        TRANSFER_ANY   ("transfer.*"),
        MESSAGE_SEND   ("message.send"),
        MESSAGE_ANY    ("message.*");
        String s;
        
        private EventType(String s){
            this.s = s;
        }
        
        public static EventType parse(final String s){
            Objects.nonNull(s);
            if (s.equals("*"              )) return ANY            ;
            if (s.equals("transfer.create")) return TRANSFER_CREATE;
            if (s.equals("transfer.update")) return TRANSFER_UPDATE;
            if (s.equals("transfer.*"     )) return TRANSFER_ANY   ;
            if (s.equals("message.send"   )) return MESSAGE_SEND   ;
            if (s.equals("message.*"      )) return MESSAGE_ANY    ;
            throw new RuntimeException(s + "can NOT be parsed as EventType ");
        }
        
        @Override public String toString(){
            return s;
        }

    }

    private static final Logger log = LoggerFactory.getLogger(TransferWSEventHandler.class);
    private final SimpleAccountManager AM = AccountManagerFactory.getLedgerAccountManagerSingleton();

    public TransferWSEventHandler() {
        super(
                new HttpMethod[] {HttpMethod.GET},
                
                new String[] {"websocket"} // /websocket?token=9AtVZPN3t49Kx07stO813UHXv6pcES
             // new String[] {"accounts/:" + PARAM_NAME + "/transfers"}
            );
    }

    public static TransferWSEventHandler create() {
        return new TransferWSEventHandler(); // TODO: return singleton?
    }

    @Override
    protected void handleGet(RoutingContext context) {
        AuthInfo ai = AuthManager.authenticate(context, false);
        // String token = context.request().getParam("token"); // TODO:(0) check ussage in tests
//      String accountName = context.request().getParam(PARAM_NAME);
//      IfaceLocalAccount account = accountManager.getAccountByName(accountName);
//      // GET /accounts/alice/transfers -> Upgrade to websocket
//      log.debug("TransferWSEventHandler Connected. Upgrading HTTP GET to WebSocket!");
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
        sws.writeFinalTextFrame(
                "{"
              +   "\"jsonrpc\" : \"2.0\", "
              +   "\"method\": \"connect\", "
              +   "\"id\" : null "
              + "}");

        IfaceAccount account = AM.getAccountByName(ai.getName());

        registerServerWebSocket(account, sws);
    }

    private static void registerServerWebSocket(IfaceAccount channelAccountOwner, ServerWebSocket sws) {

        sws.frameHandler/* WebSocket input */(/*WebSocketFrame*/frame -> {
            String message = frame.  textData(); // TODO:(0) message can be bigger than ws frame?

            JsonObject jsonMessage = new JsonObject(message);
            
            String method = jsonMessage.getString("method");
            JsonObject params = jsonMessage.getJsonObject("params");
            String result;
            if (method.equals("subscribe_account") ) {
                //  {"jsonrpc":"2.0","id":1, "method":"subscribe_account",
                //     "params":{ "eventType":"*", "accounts":["..."]}
                //  }
                // Reset all previous subscriptions
                listeners.put(sws, new HashMap<String, Set<EventType>>()); 
                EventType eventType = EventType.parse(params.getString("eventType"));
                JsonArray jsonAccounts = params.getJsonArray("accounts");
                for (int idx=0; idx < jsonAccounts.size(); idx ++) {
                    String account = jsonAccounts.getString(idx);
                    final int offset = account.indexOf("/accounts/");
                    if (offset >= 0){
                        account = account.substring(offset + "/accounts/".length());
                    }
                    // TODO:(0) Check  channelAccountOwner..getLocalID() match account
                    Set<EventType> listeners4Account = 
                        TransferWSEventHandler.listeners.get(sws).get(account);
                    if (listeners4Account != null) {
                        // TODO:(0) Clear all previous subcriptions
                    }
                    listeners4Account = new HashSet<EventType>();
                    listeners4Account.add(eventType);
                    listeners.get(sws).put(account, listeners4Account);
                }
                result = ""+jsonAccounts.size();
            } else if ( method.equals("subscribe_all_accounts") ) {
                //  {"jsonrpc":"2.0","id":1, "method":"subscribe_all_accounts",
                //     "params":{ "eventType":"*"}
                //  }
                result = "TODO:(0) not implemented";
            } else {
                // TODO:(0) throw new RpcError(errors.INVALID_METHOD, 'Unknown method: ' + reqMessage.method)
                result = "TODO:(0) not implemented";
            }
            HashMap<String, Object> response = new HashMap<String, Object>();
            response.put("jsonrpc", "2.0");
            response.put("id", jsonMessage.getInteger("id"));
            response.put("result", result);
            String jsonString = (new JsonObject(response)).encode();
            sws.writeFinalTextFrame(jsonString);
        });

        sws.closeHandler(new Handler<Void>() {
            @Override public void handle(final Void event) {
                listeners.remove(sws);
                log.debug("un-registering WS connection: "+channelAccountOwner.getLocalID());
            }
        });

        sws.exceptionHandler(/*Handler<Throwable>*/ throwable -> {
            StringWriter writer = new StringWriter();
            PrintWriter printWriter = new PrintWriter( writer );
            throwable.printStackTrace( printWriter );
            printWriter.flush();
            log.warn("There was an exception in the WebSocket '"+channelAccountOwner.getLocalID()+ "':"+throwable.toString()+ "\n" +writer.toString() );
        });
    }

    public static void notifyListener(
            final Set<String> affectedAccounts, EventType type, JsonObject resource, JsonObject related_resources ) {
        // REF: emitNotifcation@https://github.com/interledgerjs/five-bells-ledger/blob/master/src/lib/notificationBroadcasterWebsocket.js

        for (ServerWebSocket sws : listeners.keySet()) {
            for (String account : affectedAccounts){
                Set<EventType> listeners4account = listeners.get(sws).get(account);
                if (listeners4account==null) continue;
                HashMap<String, Object> response = new HashMap<String, Object>();
                
                response.put("jsonrpc", "2.0");
                response.put("id", null);
                response.put("method", "notify" );
                
                {
                    HashMap<String, Object> params = new HashMap<String, Object>();
                    params.put("event", type.s);
                    params.put("resource", resource);
                    if (related_resources !=null ){
                        params.put("related_resources", related_resources);
                    }
                    
                    response.put("params", params);
    
                }
                String message = (new JsonObject(response)).encode();
    
                for (EventType typeI : listeners4account) {
                    System.out.println(type +" equals " + typeI         +"? ->"+ type.equals(typeI));
                    System.out.println(typeI +" equals " + EventType.ANY +"? ->"+ typeI.equals(EventType.ANY));
                    if (!type.equals(typeI) && !typeI.equals(EventType.ANY)) continue;
                    System.out.println("sendint message '"+message+"' to '"+account+"'");
                    sws.writeFinalTextFrame(message);
                }
            }
        }
    }
}