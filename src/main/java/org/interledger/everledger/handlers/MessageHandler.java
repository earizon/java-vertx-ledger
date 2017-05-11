package org.interledger.everledger.handlers;

import io.vertx.core.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

import org.interledger.everledger.AuthInfo;
import org.interledger.everledger.Config;
import org.interledger.everledger.LedgerAccountManagerFactory;
import org.interledger.everledger.handlers.RestEndpointHandler;
import org.interledger.everledger.ifaces.account.IfaceLocalAccount;
import org.interledger.everledger.impl.manager.SimpleLedgerAccountManager;
import org.interledger.everledger.util.AuthManager;
import org.interledger.everledger.util.ILPExceptionSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TransferHandler handler
 *
 * REF: five-bells-ledger/src/controllers/transfers.js
 * 
 */
public class MessageHandler extends RestEndpointHandler {

    private static final Logger log = LoggerFactory.getLogger(MessageHandler.class);

    private final SimpleLedgerAccountManager accountManager = LedgerAccountManagerFactory.getLedgerAccountManagerSingleton();

    public MessageHandler() {
        super(new HttpMethod[] {HttpMethod.POST},
              new String[] {"messages"});
    }

    public static MessageHandler create() {
        return new MessageHandler(); // TODO: return singleton?
    }

    @Override
    protected void handlePost(RoutingContext context) {
        /*
         *  TODO: IMPROVEMENT: Create validation infrastructure for JSON Messages, 
         *  similar to the JS code:
         *  const validationResult = validator.create('Message')(message)$
         *  if (validationResult.valid !== true) { $... }
         */
        
        AuthInfo ai = AuthManager.authenticate(context);
        JsonObject jsonMessageReceived = getBodyAsJson(context); // TODO:(?) Mark as Tainted object
        IfaceLocalAccount fromAccount = accountManager.getAccountByName(jsonMessageReceived.getString("from"));

        log.debug("handlePost context.getBodyAsString():\n   "+context.getBodyAsString());

        /*
        * Received json message will be similar to:
        * postMessage (data.method == quote_request) is similar to:
        * {
        *   "ledger": "http://localhost:3000/",
        *   "from"  : "http://localhost:3000/accounts/alice",
        *   "to"    : "http://localhost:3000/accounts/ilpconnector",
        *   "data"  : {"method":"quote_request",
        *               "data":{
        *                 "source_address":"ledger1.eur.alice",
        *                 "destination_address":"ledger2.eur.alice.0a046f8e-b3d5-433d-a813-1cf01461a97c",
        *                 "destination_amount":"10"
        *               },
        *               "id"    : "67599ddd-f3dc-403a-bdce-3e3b98c1f82e"
        *             }
        * }
        * 
        * or (data.method == quote_response)
        * 
        * {
        *   "ledger":"http://localhost:3002",
        *   "account":"http://localhost:3002/accounts/alice",
        *   "data":{
        *       "id":"434b23c4-9b53-4e4e-8dc2-8d3ca6479736",
        *       "method":"quote_response",
        *       "data":
        *           {
        *               "source_ledger":"ledger2.eur.",
        *               "destination_ledger":"ledger3.eur.",
        *               "source_connector_account":"ledger2.eur.ilpconnector",
        *               "source_amount":"1.01",
        *               "destination_amount":"1.00",
        *               "source_expiry_duration":"6",
        *               "destination_expiry_duration":"5"}
        *           }
        *       }
        * }
        *   {"ledger":"http://localhost:3001/","from":"http://localhost:3001/accounts/alice",
        *   "to":"http://localhost:3001/accounts/ilpconnector",
        *   "data":{"method":"quote_request",
        *   "data":{
        *       "source_address":"ledger1.eur.alice",
        *       "destination_address":"ledger3.eur.alice.5d920eb8-2c89-46e8-8b02-3f027924181b",
        *       "destination_amount":"1"
        *   },
        *   "id":"71dde319-4 dd53-4749-807f-12dbcc598878"}}$
        */
        // For backwards compatibility. (Ref: messages.js @ five-bells-ledger)
        if (jsonMessageReceived.getString("account") !=null && 
            jsonMessageReceived.getString("from") == null && 
            jsonMessageReceived.getString("to") == null) {
            jsonMessageReceived.put("to",jsonMessageReceived.getString("account"));
            jsonMessageReceived.put("from", Config.publicURL + "accounts/" + ai.getId());
        }
        IfaceLocalAccount recipient = accountManager.getAccountByName(jsonMessageReceived.getString("to"));
        boolean transferMatchUser = ai.getId().equals(fromAccount.getLocalName()) || ai.getId().equals(recipient.getLocalName());
        if (!ai.isAdmin() && !transferMatchUser) {
            throw ILPExceptionSupport.createILPForbiddenException();
        }

        String URIAccount = accountManager.getPublicURIForAccount(fromAccount).toString();
        /*
         * REF: sendMessage @ src/model/messajes.js : Add account to message:
         * yield notificationBroadcaster.sendMessage(recipientName,
         *    Object.assign({}, message, {account: senderAccount}))
         */
        jsonMessageReceived.put("account", URIAccount);
        
        // REF: sendMessage @ models/messages.js:
        JsonObject notificationJSON = new JsonObject();
        notificationJSON.put("type", "message");
        notificationJSON.put("resource", jsonMessageReceived);
        String notification = notificationJSON.encode();
        TransferWSEventHandler.notifyListener(context, recipient, notification);
        String response = context.getBodyAsString();
        context.response()
            .putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
            .putHeader(HttpHeaders.CONTENT_LENGTH, ""+response.length())
            .setStatusCode(HttpResponseStatus.CREATED.code() /*201*/)
            .end(response);
    }

}
