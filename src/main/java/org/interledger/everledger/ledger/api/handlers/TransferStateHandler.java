package org.interledger.everledger.ledger.api.handlers;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

import org.interledger.everledger.common.api.auth.AuthInfo;
import org.interledger.everledger.common.api.auth.AuthManager;
import org.interledger.everledger.common.api.handlers.RestEndpointHandler;
import org.interledger.everledger.common.api.util.ILPExceptionSupport;
import org.interledger.everledger.common.config.Config;
import org.interledger.everledger.common.util.DSAPrivPubKeySupport;
import org.interledger.everledger.ledger.impl.simple.SimpleLedgerTransferManager;
import org.interledger.everledger.ledger.transfer.LedgerTransfer;
import org.interledger.everledger.ledger.transfer.IfaceLocalTransferManager;
import org.interledger.everledger.ledger.transfer.LocalTransferID;
import org.interledger.ilp.ledger.model.TransferStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * TransferHandler handler
 *
 */
public class TransferStateHandler extends RestEndpointHandler {

    private static final Logger log = LoggerFactory.getLogger(TransferStateHandler.class);
    // TODO:(0) Clean code. Check that getConfig is not used "N" times in the same class.
    private static final String transferUUID  = "transferUUID";
    private static final String RECEIPT_TYPE_ED25519 = "ed25519-sha512",
                                RECEIPT_TYPE_SHA256  = "sha256";
    private static final MessageDigest md256;
    static {
        try {
            md256 = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }
    // GET /transfers/25644640-d140-450e-b94b-badbe23d3389/state|state?type=sha256 
    public TransferStateHandler() {
        // REF: https://github.com/interledger/five-bells-ledger/blob/master/src/lib/app.js
        super( new HttpMethod[] {HttpMethod.GET} ,
                new String[] { "transfers/:" + transferUUID + "/state" }
            );
    }
    

    public static TransferStateHandler create() {
        return new TransferStateHandler(); // TODO: return singleton?
    }
    
    private static JsonObject makeTransferStateMessage(LocalTransferID transferId, TransferStatus state, String receiptType) {
        JsonObject jo = new JsonObject();
        // <-- TODO:(0) Move URI logic to Iface ILPTransferSupport and add iface to SimpleLedgerTransferManager
        jo.put("id", Config.publicURL + "transfers/" + transferId.transferID);
        jo.put("state", state.toString());
        if (receiptType.equals(RECEIPT_TYPE_SHA256)) {
            String token = ""; // FIXME: sign(sha512(transferId + ':' + state))
            jo.put("token", token);
        }
        return jo;
    }

    @Override
    protected void handleGet(RoutingContext context) {
        /*
         * *****************************
         * * GET transfer by UUID & type
         * *****************************
         * GET /transfers/3a2a1d9e-8640-4d2d-b06c-84f2cd613204/state?type=sha256 HTTP/1.1
         * HTTP/1.1 200 OK
         * {"type":"sha256","message":{"id":"http://localhost/transfers/3a2a1d9e-8640-4d2d-b06c-84f2cd613204","state":"proposed","token":"xy9kB4n/nWd+MsI84WeK2qg/tLfDr/4SIe5xO9OAz9PTmAwKOUzzJxY1+7c7e3rs0iQ0jy57L3U1Xu8852qlCg=="},"signer":"http://localhost","digest":"P6K2HEaZxAthBeGmbjeyPau0BIKjgkaPqW781zmSvf4="}
         */
        log.debug(this.getClass().getName() + "handleGet invoqued ");
        AuthInfo ai = AuthManager.authenticate(context);

        String transferId = context.request().getParam(transferUUID);
        LocalTransferID transferID = new LocalTransferID(transferId);
        IfaceLocalTransferManager tm = SimpleLedgerTransferManager.getLocalTransferManager();
        TransferStatus status = TransferStatus.PROPOSED; // default value
        boolean transferMatchUser = false;
        if (tm.doesTransferExists(transferID)) { 
            LedgerTransfer transfer = tm.getLocalTransferById(transferID);
            status = transfer.getTransferStatus();
            transferMatchUser = ai.getId().equals(transfer.getDebits ()[0].account.getLocalName())
                            ||  ai.getId().equals(transfer.getCredits()[0].account.getLocalName());
        } else {
            //TODO:(0) ???? Continue with status equal proposed if transfer not found ???
        }

        
        if (!ai.isAdmin() && !transferMatchUser) {
            throw ILPExceptionSupport.createILPForbiddenException();
        }
        // REF: getStateResource @ transfers.js

        String receiptType = context.request().getParam("type");
        // REF: getTransferStateReceipt(id, receiptType, conditionState) @ five-bells-ledger/src/models/transfers.js
        if (receiptType == null) { receiptType = RECEIPT_TYPE_ED25519; }
        if ( ! receiptType.equals(RECEIPT_TYPE_ED25519) &&
             ! receiptType.equals(RECEIPT_TYPE_SHA256 ) &&
             true ) {
            throw new RuntimeException("type not in := "+RECEIPT_TYPE_ED25519+"* | "+RECEIPT_TYPE_SHA256+" ");
        }
        JsonObject jo = new JsonObject();
        String signer = "";      // FIXME: config.getIn(['server', 'base_uri']),
        if (receiptType.equals(RECEIPT_TYPE_ED25519)) {
            // REF: makeEd25519Receipt(transferId, transferState) @
            //      @ five-bells-ledger/src/models/transfers.js
            JsonObject message = makeTransferStateMessage(transferID, status, RECEIPT_TYPE_ED25519);
            String signature = "";   // FIXME: sign(hashJSON(message))
            jo.put("type", RECEIPT_TYPE_ED25519);
            jo.put("message", message);
            jo.put("signer", signer);
            jo.put("public_key", DSAPrivPubKeySupport.savePublicKey(Config.ilpLedgerInfo.getNotificationSignPublicKey()));
            jo.put("signature", signature);
        } else {
            // REF: makeSha256Receipt(transferId, transferState, conditionState) @
            //      @ five-bells-ledger/src/models/transfers.js
            JsonObject message = makeTransferStateMessage(transferID, status, RECEIPT_TYPE_SHA256);
            String digest = sha256(message.encode());
            jo.put("type", RECEIPT_TYPE_SHA256);
            jo.put("message", message);
            jo.put("signer", signer);
            jo.put("digest", digest);
            String conditionState = context.request().getParam("condition_state");
            if (conditionState != null) {
                JsonObject conditionMessage = makeTransferStateMessage(transferID, status, RECEIPT_TYPE_SHA256);
                String condition_digest = sha256(conditionMessage.encode());
                jo.put("condition_state", conditionState);
                jo.put("condition_digest", condition_digest);
            }
        }

        String response = jo.encode();
        context.response()
        .putHeader(HttpHeaders.CONTENT_TYPE, "application/json")
        .putHeader(HttpHeaders.CONTENT_LENGTH, ""+response.length())
        .setStatusCode(HttpResponseStatus.OK.code())
        .end(response);
    }
    
    private static String sha256(String input) {
        md256.reset();
        md256.update(input.getBytes());
        return new String(md256.digest());
    }

}
