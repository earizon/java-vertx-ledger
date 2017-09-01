package com.everis.everledger.handlers

import com.everis.everledger.AuthInfo
import com.everis.everledger.util.Config
import com.everis.everledger.ifaces.account.IfaceAccount
import com.everis.everledger.impl.manager.SimpleAccountManager
import com.everis.everledger.util.AuthManager
import com.everis.everledger.util.ILPExceptionSupport
import io.netty.handler.codec.http.HttpResponseStatus
import io.vertx.core.http.HttpHeaders
import io.vertx.core.http.HttpMethod
import io.vertx.core.http.ServerWebSocket
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.RoutingContext
import org.slf4j.LoggerFactory
import java.io.PrintWriter
import java.io.StringWriter
import java.util.*


class MessageHandler : RestEndpointHandler(arrayOf(HttpMethod.POST), arrayOf("messages")) {

    private val AM = SimpleAccountManager

    override fun handlePost(context: RoutingContext) {
        /*
         *  TODO: IMPROVEMENT: Create validation infrastructure for JSON Messages,
         *  similar to the JS code:
         *  const validationResult = validator.create('Message')(message)$
         *  if (validationResult.valid !== true) { $... }
         */

        val ai = AuthManager.authenticate(context)
        val jsonMessageReceived = RestEndpointHandler.getBodyAsJson(context) // TODO:(?) Mark as Tainted object
        val fromAccount = AM.getAccountById(jsonMessageReceived.getString("from"))

        log.debug("handlePost context.getBodyAsString():\n   " + context.bodyAsString)

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
        if (jsonMessageReceived.getString("account") != null &&
                jsonMessageReceived.getString("from") == null &&
                jsonMessageReceived.getString("to") == null) {
            jsonMessageReceived.put("to" as String?,jsonMessageReceived.getString("account") as String?)
            jsonMessageReceived.put("from", Config.publicURL.toString() + "accounts/" + ai.id)
        }
        val recipient = AM.getAccountById(jsonMessageReceived.getString("to"))
        val transferMatchUser = ai.id  == fromAccount.localID || ai.id == recipient.localID
        if (!ai.isAdmin && !transferMatchUser) {
            throw ILPExceptionSupport.createILPForbiddenException()
        }

        /*
         * REF: sendMessage @ src/model/messajes.js : Add account to message:
         * yield notificationBroadcaster.sendMessage(recipientName,
         *    Object.assign({}, message, {account: senderAccount}))
         */
        jsonMessageReceived.put("account", fromAccount.id)

        // REF: sendMessage @ models/messages.js:
        val notificationJSON = JsonObject()
        notificationJSON.put("type", "message")
        notificationJSON.put("resource", jsonMessageReceived)
        // final IfaceAccount[] affectedAccounts, EventType type, String resource
        val affectedAccounts = HashSet<String>()
        affectedAccounts.add(recipient.localID)
        TransferWSEventHandler.notifyListener(
                affectedAccounts, TransferWSEventHandler.EventType.MESSAGE_SEND, notificationJSON, null)
        val response = context.bodyAsString
        context.response()
                .putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                .putHeader(HttpHeaders.CONTENT_LENGTH, "" + response.length)
                .setStatusCode(HttpResponseStatus.CREATED.code() /*201*/)
                .end(response)
    }

    companion object {

        private val log = LoggerFactory.getLogger(MessageHandler::class.java)

        fun create(): MessageHandler {
            return MessageHandler() // TODO: return singleton?
        }
    }

}


class TransferWSEventHandler : RestEndpointHandler(arrayOf(HttpMethod.GET), arrayOf("websocket")) {
    enum class EventType private constructor(internal var s: String) {
        ANY("*"),
        TRANSFER_CREATE("transfer.create"),
        TRANSFER_UPDATE("transfer.update"),
        TRANSFER_ANY("transfer.*"),
        MESSAGE_SEND("message.send"),
        MESSAGE_ANY("message.*");

        val isTransfer: Boolean
            get() = this == TRANSFER_CREATE || this == TRANSFER_UPDATE || this == TRANSFER_ANY

        val isMessage: Boolean
            get() = this == MESSAGE_SEND || this == MESSAGE_ANY

        override fun toString(): String {
            return s
        }

        companion object {

            fun parse(s: String): EventType {
                Objects.nonNull(s)
                if (s == "*") return ANY
                if (s == "transfer.create") return TRANSFER_CREATE
                if (s == "transfer.update") return TRANSFER_UPDATE
                if (s == "transfer.*") return TRANSFER_ANY
                if (s == "message.send") return MESSAGE_SEND
                if (s == "message.*") return MESSAGE_ANY
                throw RuntimeException(s + "can NOT be parsed as EventType ")
            }
        }

    }

    override fun handleGet(context: RoutingContext) {
        val ai = AuthManager.authenticate(context, false)
        // String token = context.request().getParam("token"); // TODO:(0) check ussage in tests
        //      String accountName = context.request().getParam(PARAM_NAME);
        //      IfaceLocalAccount account = accountManager.getAccountByName(accountName);
        //      // GET /accounts/alice/transfers -> Upgrade to websocket
        //      log.debug("TransferWSEventHandler Connected. Upgrading HTTP GET to WebSocket!");
        val sws = context.request().upgrade()
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
        writeJsonRPCResponse(sws, -1 , "null", "null", "connect")

        val account = AM.getAccountById(ai.id)

        registerServerWebSocket(ai, account, sws)
    }

    companion object {

        // TODO:(0) Protect listeners access. Can be accesed from different threads
        //      simultaneously.
        private val listeners = HashMap<ServerWebSocket, HashMap<String /*account*/, MutableSet<EventType>>>()/*account*/
        private val AM = SimpleAccountManager

        private val log = LoggerFactory.getLogger(TransferWSEventHandler::class.java)

        fun create(): TransferWSEventHandler {
            return TransferWSEventHandler() // TODO: return singleton?
        }

        private fun registerServerWebSocket(ai: AuthInfo, channelAccountOwner: IfaceAccount, sws: ServerWebSocket) {
            sws.frameHandler/* WebSocket input */{ frame ->
                val message = frame.textData() // TODO:(0) message can be bigger than ws frame?
                val jsonMessage = JsonObject(message)

                val method = jsonMessage.getString("method")
                val params = jsonMessage.getJsonObject("params")
                val id = jsonMessage.getInteger("id")
                val result: Int
                if (method == "subscribe_account") {
                    //  {"jsonrpc":"2.0","id":1, "method":"subscribe_account",
                    //     "params":{ "eventType":"*", "accounts":["..."]}
                    //  }
                    // Reset all previous subscriptions
                    if (id == null) { // TODO:(?) Refactor to make common
                        writeJsonRPCError(sws, id, 40000, "Invalid id")
                        return@frameHandler
                    }
                    listeners.put(sws, HashMap<String, MutableSet<EventType>>())
                    val eventType = EventType.parse(params.getString("eventType"))
                    val jsonAccounts = params.getJsonArray("accounts")
                    for (idx in 0..jsonAccounts.size() - 1) {
                        var account_id = jsonAccounts.getString(idx)
                        val offset = account_id.indexOf("/accounts/")
                        if (offset >= 0) {
                            account_id = account_id.substring(offset + "/accounts/".length)
                        }
                        try {
                            AM.getAccountById(account_id)
                        } catch (e: Exception) {
                            writeJsonRPCError(sws, id, 40002, "Invalid account: " + account_id)
                            return@frameHandler
                        }

                        if (!ai.isAdmin && ai.id != account_id /* TODO:(0) use account not in ai.associatedAccount/s*/) {
                            writeJsonRPCError(sws, id, 40300, "Not authorized")
                            return@frameHandler
                        }
                        // TODO:(0) Check  channelAccountOwner..getLocalID() match account
                        var listeners4Account: MutableSet<EventType>? = TransferWSEventHandler.listeners[sws]!!.get(account_id)
                        if (listeners4Account != null) {
                            // TODO:(0) Clear all previous subcriptions
                        }
                        listeners4Account = HashSet<EventType>()
                        listeners4Account.add(eventType)
                        listeners[sws]!!.put(account_id, listeners4Account)
                    }
                    result = jsonAccounts.size()
                } else if (method == "subscribe_all_accounts") {
                    if (!ai.isAdmin) {
                        writeJsonRPCError(sws, id, 40300, "Not authorized")
                        return@frameHandler
                    }
                    //                if (channelAccountOwner)
                    //  {"jsonrpc":"2.0","id":1, "method":"subscribe_all_accounts",
                    //     "params":{ "eventType":"*"}
                    //  }
                    throw RuntimeException("TODO:(0) not implemented")
                } else {
                    writeJsonRPCError(sws, id, -32601, "Unknown method: " + method)
                    return@frameHandler
                }
                writeJsonRPCResponse(sws, jsonMessage.getInteger("id"), "result", result, "null"/*method*/)
            }

            sws.closeHandler {
                listeners.remove(sws)
                log.debug("un-registering WS connection: " + channelAccountOwner.localID)
            }

            sws.exceptionHandler { throwable ->
                val writer = StringWriter()
                val printWriter = PrintWriter(writer)
                throwable.printStackTrace(printWriter)
                printWriter.flush()
                log.warn("There was an exception in the WebSocket '" + channelAccountOwner.localID + "':" + throwable.toString() + "\n" + writer.toString())
            }
        }

        fun notifyListener(
                affectedAccounts: Set<String>, type: EventType, resource: JsonObject, related_resources: JsonObject?) {
            // REF: emitNotifcation@https://github.com/interledgerjs/five-bells-ledger/blob/master/src/lib/notificationBroadcasterWebsocket.js

            for (sws in listeners.keys) {
                websocketchannel@ for (account in affectedAccounts) {
                    val listeners4account = listeners[sws]!![account] ?: continue
                    val params = HashMap<String, Any>()
                    params.put("event", type.s)
                    params.put("resource", resource)
                    if (related_resources != null) {
                        params.put("related_resources", related_resources)
                    }
                    for (typeI in listeners4account) {
                        println(type.toString() + " equals " + typeI + "? ->" + (type == typeI))
                        println(typeI.toString() + " equals " + EventType.ANY + "? ->" + (typeI == EventType.ANY))
                        if (type != typeI &&
                                typeI != EventType.ANY &&
                                !(type.isTransfer && typeI == EventType.TRANSFER_ANY) &&
                                !(type.isMessage && typeI == EventType.MESSAGE_ANY))
                            continue
                        println("sendint notify to account:'$account'")
                        writeJsonRPCResponse(sws, -1, "params", params, "notify")
                        break@websocketchannel
                    }
                }
            }
        }
    // private static void writeJsonRPCResponse(ServerWebSocket sws, Integer id, String key, Object value, String method) {
    //     HashMap<String, Object > response = new HashMap<String, Object >();
    //     response.put("jsonrpc", "2.0");
    //     response.put("id", id);
    //     if (key !=null ) { response.put(key, value);}
    //     if (method != null ) { response.put("method", method); }
    //     sws.writeFinalTextFrame((new JsonObject(response)).encode());
    // }
        private fun writeJsonRPCResponse(sws: ServerWebSocket, id: Int, key: String, value: Any, method: String) {
            val response = HashMap<String, Any>()
            response.put("jsonrpc", "2.0")
            response.put("id", if (id!=-1) {""+id} else { "null" })
            if (value != "null" ) response.put(key, value)
            if (method !="null" ) response.put("method", method)
            sws.writeFinalTextFrame(JsonObject(response).encode())
        }

        private fun writeJsonRPCError(sws: ServerWebSocket, id: Int, code: Int, message: String) {
            val error = HashMap<String, Any>()
            error.put("code", code)
            error.put("message", "RpcError: " + message)

            val data = HashMap<String, Any>()
            data.put("name", "RpcError")
            data.put("message", message)
            error.put("data", data)
            writeJsonRPCResponse(sws, id, "error", JsonObject(error), "null" /* <-- argg!! needed for compatibility */)
        }
    }
}// /websocket?token=9AtVZPN3t49Kx07stO813UHXv6pcES
// new String[] {"accounts/:" + PARAM_NAME + "/transfers"}
