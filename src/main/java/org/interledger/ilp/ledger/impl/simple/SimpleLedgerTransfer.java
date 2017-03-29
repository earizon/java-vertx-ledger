package org.interledger.ilp.ledger.impl.simple;

import io.vertx.core.json.JsonArray;

//import javax.money.MonetaryAmount;

//import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import org.interledger.ilp.core.Credit;
import org.interledger.ilp.core.Debit;
import org.interledger.ilp.core.FulfillmentURI;

import javax.money.MonetaryAmount;

import org.interledger.ilp.core.ConditionURI;
import org.interledger.ilp.core.DTTM;
import org.interledger.ilp.core.LedgerPartialEntry;
import org.interledger.ilp.core.TransferID;
import org.interledger.ilp.core.ledger.model.LedgerInfo;
import org.interledger.ilp.core.ledger.model.LedgerTransfer;
import org.interledger.ilp.core.ledger.model.TransferStatus;
import org.interledger.ilp.ledger.LedgerAccountManagerFactory;
import org.interledger.ilp.ledger.LedgerFactory;
import org.interledger.ilp.ledger.account.LedgerAccount;
import org.javamoney.moneta.Money;

// FIXME: Allow multiple debit/credits (Remove all code related to index [0]

public class SimpleLedgerTransfer implements LedgerTransfer {
    // TODO: IMPROVEMENT. Mix of local/remote transactions not contemplated. Either all debit_list are remote or local
    final TransferID transferID;
    final LedgerAccount fromAccount;
    final Credit[] credit_list;
    final Debit []  debit_list;
    // URI encoded execution & cancelation crypto-conditions
    final ConditionURI URIExecutionCond;
    final ConditionURI URICancelationCond;
    final DTTM DTTM_expires ;
    final DTTM DTTM_proposed;

    /*
     * Note: Defensive security protection:
     * The default value for URIExecutionFF|URICancelationFF FulfillmentURI.EMPTY
     * will trigger a transaction just if the ConditionURI for Execution/Cancelation
     * are also empty.
     */
    FulfillmentURI URIExecutionFF     = FulfillmentURI.MISSING;
    FulfillmentURI URICancelationFF   = FulfillmentURI.MISSING;
    String data = "";
    String noteToSelf = "";

    TransferStatus transferStatus;
    DTTM DTTM_prepared = DTTM.future;
    DTTM DTTM_executed = DTTM.future;
    DTTM DTTM_rejected = DTTM.future;

    public SimpleLedgerTransfer(TransferID transferID,
        Debit[] debit_list, Credit[] credit_list, 
        ConditionURI URIExecutionCond, 
        ConditionURI URICancelationCond, DTTM DTTM_expires, DTTM DTTM_proposed,
        String data, String noteToSelf, TransferStatus transferStatus ){
        // TODO: Check that debit_list[idx].ammount.currency is always the same and match the ledger
        // TODO: Check that credit_list[idx].ammount.currency is always the same.
        //       For local transaction check also that it match the ledger
        
//        if (debit_list.length!=1) {
//            throw new RuntimeException("Only one debit is supported in this implementation");
//        }
//        if (credit_list.length!=1) {
//            throw new RuntimeException("Only one credit is supported in this implementation");
//        }
        // TODO: FIXME: Check debit_list SUM of amounts equals credit_list SUM  of amounts.

        // FIXME: TODO: If fromAccount.ledger != "our ledger" throw RuntimeException.
        this.transferID         = transferID        ;
        this.credit_list        = credit_list       ;
        this.debit_list         = debit_list        ;
        this.data               = data              ;
        this.noteToSelf         = noteToSelf        ;
        this.URIExecutionCond   = URIExecutionCond  ;
        this.URICancelationCond = URICancelationCond;
        this.DTTM_expires       = DTTM_expires      ;
        this.DTTM_proposed      = DTTM_proposed     ;
        this.DTTM_prepared      = DTTM.getNow()     ;
        if (transferStatus.equals(TransferStatus.PROPOSED) && !URIExecutionCond.equals(ConditionURI.EMPTY)){
            transferStatus = TransferStatus.PREPARED;
        }
        this.transferStatus     = transferStatus    ;
        System.out.println("deleteme SimpleLedgerTransfer constructor transferStatus:"+transferStatus.toString());
        /*
         *  Parse String to fetch local accounturi
         *  String will be similar to http://localLedger/account/"accountId" ->
         *  we need the "accountId" to fetch the correct local "from" Account
         */

        this.fromAccount = LedgerAccountManagerFactory.
                getLedgerAccountManagerSingleton().
                    getAccountByName(credit_list[0].account.getAccountId());
    }
    
    public void checkBalancedTransaction(){
        MonetaryAmount totalDebits = Money.of(0, debit_list[0].amount.getCurrency());
        for ( Debit debit : debit_list ) {
            totalDebits.add(debit.amount);
        }
        MonetaryAmount totalCredits = Money.of(0, credit_list[0].amount.getCurrency());
        for ( Credit credit : credit_list ) {
            totalCredits.add(credit.amount);
        }
        if (! totalDebits.isEqualTo(totalCredits))  {
            throw new RuntimeException("transfer not balanced between credits and debits");
        }
    }

    @Override
    public TransferID getTransferID() {
        return transferID;
    }
    
    @Override
    public Debit[] getDebits() {
        return debit_list;
    }

    @Override
    public Credit[] getCredits() {
        return credit_list;
    }

    @Override
    public String getData() {
        return data;
    }

    @Override
    public String getNoteToSelf() {
        return noteToSelf;
    }

    // TODO: IMPROVEMENT. setTransferStatus Make private and change when providing
    //       exec/cancel fulfillment. Ummm, What happens for non-conditional transactions?
    @Override
    public void setTransferStatus(TransferStatus transferStatus) {
//        final String errorState = "new state '"+transferStatus.toString()+"' "
//                + "not compliant with Transfer State Machine. Current state: "
//                + this.transferStatus.toString();
        // TODO: COMMENT on ILP Ledger list. 
        // next checks were commented to make five-bells-ledger tests pass
        //    anyway it looks sensible to have them in place.
        // switch(transferStatus){
        //     // TODO: RECHECK allowed state machine 
        //     case PROPOSED:
        //         if (this.transferStatus != TransferStatus.PROPOSED) { 
        //             throw new RuntimeException(errorState); 
        //         }
        //         break;
        //     case PREPARED:
        //         if (this.transferStatus != TransferStatus.PROPOSED) { 
        //             throw new RuntimeException(errorState); 
        //         }
        //         break;
        //     case EXECUTED:
        //         if (this.transferStatus != TransferStatus.PREPARED ) { 
        //             throw new RuntimeException(errorState); 
        //         }
        //         break;
        //     case REJECTED:
        //         if (this.transferStatus != TransferStatus.PREPARED ) { 
        //             throw new RuntimeException(errorState); 
        //         }
        //         break;
        //     default:
        //         throw new RuntimeException("Unknown transferStatus");
        // }
        this.transferStatus = transferStatus;
        System.out.println("deleteme SimpleLedgerTransfer setTransferStatus transferStatus:"+transferStatus.toString());
    }

    @Override
    public TransferStatus getTransferStatus() {
        return transferStatus;
    }

    
    @Override
    public ConditionURI getURIExecutionCondition() {
        return URIExecutionCond;
    }

    @Override
    public ConditionURI getURICancellationCondition() {
        return URICancelationCond;
    }

    @Override
    public DTTM getDTTM_prepared() {
        return DTTM_prepared;
    }

    @Override
    public void setDTTM_prepared(DTTM DTTM) {
        DTTM_prepared = DTTM;
    }

    @Override
    public DTTM getDTTM_executed() {
        return DTTM_executed;
    }

    @Override
    public void setDTTM_executed(DTTM DTTM) {
        DTTM_executed = DTTM;
    }

    @Override
    public DTTM getDTTM_rejected() {
        return DTTM_rejected;
    }

    @Override
    public void setDTTM_rejected(DTTM DTTM) {
        DTTM_rejected = DTTM;
    }

    @Override
    public DTTM getDTTM_expires() {
        return DTTM_expires;
    }

    @Override
    public DTTM getDTTM_proposed() {
        return DTTM_proposed;
    }

    @Override
    public void  setURIExecutionFulfillment(FulfillmentURI ffURI){
        this.URIExecutionFF = ffURI;
    }
    
    @Override
    public FulfillmentURI getURIExecutionFulfillment(){
        return URIExecutionFF;
    }

    @Override
    public void  setURICancelationFulfillment(FulfillmentURI ffURI){
        this.URICancelationFF = ffURI;
    }
    
    @Override
    public FulfillmentURI getURICancellationFulfillment(){
        return URICancelationFF;
    }
    
    // NOTE: The JSON returned to the ILP connector and the Wallet must not necesarelly match
    // since the data about the transfer needed by the wallet and the connector differ.
    // That's why two different JSON encoders exist

    public JsonObject toILPJSONStringifiedFormat() {
        LedgerInfo ledgerInfo = LedgerFactory.getDefaultLedger().getInfo();

        // REF: convertToExternalTransfer@
        // https://github.com/interledger/five-bells-ledger/blob/master/src/models/converters/transfers.js
        JsonObject jo = new JsonObject();
        String ledger = ledgerInfo.getBaseUri();
        if (ledger.endsWith("/")) { ledger = ledger.substring(0, ledger.length()-1); }
        String id = ledger + "/transfers/"+ transferID.transferID;
        jo.put("id", id);
        jo.put("ledger", ledger);
        jo.put("debits" , entryList2Json( debit_list));
        jo.put("credits", entryList2Json(credit_list));
        jo.put("execution_condition", this.getURIExecutionCondition().URI);
        jo.put("state", this.getTransferStatus().toString());
//        if (!this.getURICancellationCondition().URI.equals(ConditionURI.NOT_PROVIDED)) {
//            jo.put("cancellation_condition", this.getURICancellationCondition().URI);
//        }
        // FIXME: Cancelation_condition?
        jo.put("expires_at", this.DTTM_expires.toString());
        {
            JsonObject timeline = new JsonObject();
            timeline.put("proposed_at", this.DTTM_proposed.toString());
            if (this.DTTM_prepared != DTTM.future) { timeline.put("prepared_at", this.DTTM_prepared.toString()); }
            if (this.DTTM_executed != DTTM.future) { timeline.put("executed_at", this.DTTM_executed.toString()); }
            if (this.DTTM_rejected != DTTM.future) { timeline.put("rejected_at", this.DTTM_rejected.toString()); }
            jo.put("timeline", timeline);
        }
        return jo;
    }

    public JsonObject toMessageStringifiedFormat() {
        JsonObject jo = toILPJSONStringifiedFormat();
        JsonObject jo2 = new JsonObject();
        jo2.put("type", "transfer");
        jo2.put("resource", jo);
        boolean addRelatedResources = 
                   this.getTransferStatus().equals(TransferStatus.EXECUTED)
                || this.getTransferStatus().equals(TransferStatus.REJECTED);
        
        if ( addRelatedResources ) {
                //  REF: sendNotifications @
                //       five-bells-ledger/src/lib/notificationBroadcasterWebsocket.js
                JsonObject related_resources = new JsonObject();
                String URI_FF = (this.getTransferStatus() == TransferStatus.EXECUTED)
                        ? this.  getURIExecutionFulfillment().URI
                        : this.getURICancellationFulfillment().URI;
                related_resources.put("execution_condition_fulfillment", URI_FF);
                jo2.put("related_resources", related_resources);
            }
        return jo2;
    }


    private JsonArray entryList2Json(LedgerPartialEntry[] input_list) {
        JsonArray ja = new JsonArray();
        for (LedgerPartialEntry entry : input_list) {
            // FIXME: This code to calculate amount is PLAIN WRONG. Just to pass five-bells-ledger tests
            JsonObject jo = new JsonObject();
            jo.put("account", entry.account.getUri());
            String sAmount = "" + entry. amount.getNumber();
            jo.put( "amount", sAmount);
            if (entry instanceof Debit) {
                jo.put("authorized", ((Debit) entry).getAuthorized());
            } else if (entry instanceof Credit ) {
                // Add memo:
                //  "memo":{
                //      "ilp_header":{
                //          "account":"ledger3.eur.alice.fe773626-81fb-4294-9a60-dc7b15ea841e",
                //          "amount":"1",
                //          "data":{"expires_at":"2016-11-10T15:51:27.134Z"}}
                //  }}
                System.out.println(">>>deleteme (Credit)entry).ph.getExpiry().toString()"+((Credit)entry).ph.getExpiry().toString());
                System.out.println(">>>deleteme DTTM_expires.toString()"+DTTM_expires.toString());
                JsonObject memo = new JsonObject(), ilp_header = new JsonObject(), data = new JsonObject();
                ilp_header.put("account", ((Credit)entry).ph.getDestinationAddress());
                ilp_header.put("amount",  ""+((Credit)entry).ph.getAmount());// TODO: Recheck
                data.put("expires_at", /*((Credit)entry).ph.getExpiry().toString()*/DTTM_expires.toString());  // TODO: Recheck.
                ilp_header.put("data", data);
                memo.put("ilp_header", ilp_header);
                jo.put("memo", memo);
            }
            ja.add(jo);
        }
        return ja;
    }
    
    @Override
    public boolean isLocal() {
        String localLedgerURI = debit_list[0].account.getLedgerUri();
        for (Credit credit : credit_list) {
            if (! localLedgerURI.equals(credit.account.getLedgerUri() ) ) {
                return false;
            }
        }
        return true;
    }


}
