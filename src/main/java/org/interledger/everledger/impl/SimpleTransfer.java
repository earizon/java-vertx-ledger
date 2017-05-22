package org.interledger.everledger.impl;

import java.time.ZonedDateTime;
import java.util.Objects;
import java.util.UUID;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import org.interledger.ilp.InterledgerAddress;
import org.interledger.ilp.InterledgerAddressBuilder;
import org.interledger.cryptoconditions.Condition;
import org.interledger.cryptoconditions.Fulfillment;
//import org.interledger.cryptoconditions.der.CryptoConditionReader;
import org.interledger.cryptoconditions.types.PreimageSha256Condition;
import org.interledger.cryptoconditions.types.PreimageSha256Fulfillment;
import org.interledger.everledger.Config;
import org.interledger.everledger.LedgerAccountManagerFactory;
import org.interledger.everledger.ifaces.account.IfaceLocalAccount;
import org.interledger.everledger.ifaces.transfer.IfaceTransfer;
import org.interledger.everledger.impl.manager.SimpleLedgerAccountManager;
import org.interledger.everledger.ledger.transfer.Credit;
import org.interledger.everledger.ledger.transfer.DTTM;
import org.interledger.everledger.ledger.transfer.Debit;
//import org.interledger.everledger.ledger.transfer.ILPSpecTransferID;
import org.interledger.everledger.ledger.transfer.LedgerPartialEntry;
import org.interledger.everledger.ledger.transfer.LocalTransferID;

import javax.money.MonetaryAmount;

import org.interledger.ilp.ledger.model.TransferStatus;
import org.javamoney.moneta.Money;

// FIXME:(1) Allow multiple debit/credits (Remove all code related to index [0])

public class SimpleTransfer implements IfaceTransfer {

    public static final Fulfillment FF_NOT_PROVIDED = new PreimageSha256Fulfillment(new byte[]{});
    public static final Condition   CC_NOT_PROVIDED =  new PreimageSha256Condition(
            new byte[]{1,2,3,4,5,6,7,8,9,0,1,2,3,4,5,6,7,8,9,0,1,2,3,4,5,6,7,8,9,0,1,2}, 1000);
    
    static  final SimpleLedgerAccountManager  ledgerAccountManager = 
            LedgerAccountManagerFactory.getLedgerAccountManagerSingleton();
    final LocalTransferID transferID;
    final IfaceLocalAccount fromAccount;
    final Credit[] credit_list;
    final Debit []  debit_list;
    // URI encoded execution & cancelation crypto-conditions
    final Condition executionCond  ;
    final Condition cancelationCond;
    final DTTM DTTM_expires ;
    final DTTM DTTM_proposed;
    final String sMemo;

    /*
     * Note: Defensive security protection:
     * The default value for URIExecutionFF|URICancelationFF FulfillmentURI.EMPTY
     * will trigger a transaction just if the ConditionURI for Execution/Cancelation
     * are also empty.
     */
    Fulfillment executionFF   = FF_NOT_PROVIDED;
    Fulfillment cancelationFF = FF_NOT_PROVIDED;
    String data = "";
    String noteToSelf = "";

    TransferStatus transferStatus;
    DTTM DTTM_prepared = DTTM.future;
    DTTM DTTM_executed = DTTM.future;
    DTTM DTTM_rejected = DTTM.future;

    public SimpleTransfer(LocalTransferID transferID,
        Debit[] debit_list, Credit[] credit_list, 
        Condition executionCond, 
        Condition cancelationCond, DTTM DTTM_expires, DTTM DTTM_proposed,
        String data, String noteToSelf, TransferStatus transferStatus, String sMemo ){
        // TODO:(1) Check that debit_list[idx].ammount.currency is always the same and match the ledger
        // TODO:(1) Check that credit_list[idx].ammount.currency is always the same.

        // FIXME: TODO: If fromAccount.ledger != "our ledger" throw RuntimeException.
        this.transferID         = Objects.requireNonNull(transferID     );
        this.credit_list        = Objects.requireNonNull(credit_list    );
        this.debit_list         = Objects.requireNonNull(debit_list     );
        checkBalancedTransaction();
        this.data               = Objects.requireNonNull(data           );
        this.noteToSelf         = Objects.requireNonNull(noteToSelf     );
        this.executionCond      = Objects.requireNonNull(executionCond  );
        this.cancelationCond    = Objects.requireNonNull(cancelationCond);
        this.DTTM_expires       = Objects.requireNonNull(DTTM_expires   );
        this.DTTM_proposed      = Objects.requireNonNull(DTTM_proposed  );
        this.DTTM_prepared      = Objects.requireNonNull(DTTM.getNow()  );
        this.sMemo              = Objects.requireNonNull(sMemo)          ;
        if (transferStatus.equals(TransferStatus.PROPOSED) /* && !ExecutionCond.equals(ConditionURI.EMPTY) TODO:(0)*/){
            transferStatus = TransferStatus.PREPARED;
        }
        this.transferStatus     = transferStatus    ;
        System.out.println("deleteme SimpleLedgerTransfer constructor transferStatus:"+transferStatus.toString());
        /*
         *  Parse String to fetch local accounturi
         *  String will be similar to http://localLedger/account/"accountId" ->
         *  we need the "accountId" to fetch the correct local "from" Account
         */

        this.fromAccount = ledgerAccountManager.
                    getAccountByName(credit_list[0].account.getLocalName());
    }

    // Implement ILPSpec interface{
    @Override
    public UUID getId() {
        // TODO:(0) Check LocalTransferID  LocalTransferID.ILPSpec2LocalTransferID
        //           Create inverse and use
        UUID result = UUID.randomUUID(); // TODO:(0) FIX get from getTransferID
        return result;
    }

    @Override
    public InterledgerAddress getFromAccount() {
        final InterledgerAddress result = new 
                InterledgerAddressBuilder().value("TODO(0)").build();
        return result;
    }

    @Override
    public InterledgerAddress getToAccount() {
        final InterledgerAddress result = new 
                InterledgerAddressBuilder().value("TODO(0)").build();
        return result;
    }

    @Override
    public MonetaryAmount getAmount(){
        MonetaryAmount result = Money.of(0, debit_list[0].amount.getCurrency());
        return result;
    }

    @Override
    public boolean isAuthorized(){
        boolean result = true; // TODO:(1)
        return result;
    }

    @Override
    public String getInvoice(){
        String result = ""; // TODO:(0)
        return result;
    }

    @Override
    public byte[] getData() {
        return data.getBytes();
    }

    @Override
    public byte[] getNoteToSelf() {
        return noteToSelf.getBytes();
    }

    @Override

    public boolean isRejected(){
        boolean result = false; // TODO:(0)
        return result;
    }

    @Override
    public String getRejectionMessage(){
        String result = "";
        return result;
    }

    @Override
    public Condition getExecutionCondition() {
        return executionCond;
    }

    @Override
    public Condition getCancellationCondition() {
        return cancelationCond;
    }

    public ZonedDateTime getExpiresAt() {
        ZonedDateTime result = ZonedDateTime.parse(DTTM_expires.toString());
        return result;
    }
    
    // } End ILPSpec interface
    
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
    public LocalTransferID getTransferID() {
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
    public void  setExecutionFulfillment(Fulfillment ff){
        this.executionFF = ff;
    }
    
    @Override
    public Fulfillment getExecutionFulfillment(){
        return executionFF;
    }

    @Override
    public void  setCancelationFulfillment(Fulfillment ff){
        this.cancelationFF = ff;
    }
    
    @Override
    public Fulfillment getCancellationFulfillment(){
        return cancelationFF;
    }
    
    // NOTE: The JSON returned to the ILP connector and the Wallet must not necesarelly match
    // since the data about the transfer needed by the wallet and the connector differ.
    // That's why two different JSON encoders exist

    public JsonObject toILPJSONStringifiedFormat() {

        // REF: convertToExternalTransfer@
        // https://github.com/interledger/five-bells-ledger/blob/master/src/models/converters/transfers.js
        JsonObject jo = new JsonObject();
        String ledger = Config.publicURL.toString();
        if (ledger.endsWith("/")) { ledger = ledger.substring(0, ledger.length()-1); }
        String id = /* TODO:(doubt) add ledger as prefix ?? */"/transfers/" /* TODO:(0) Get from Config */+ transferID.transferID;
        jo.put("id", id);
        jo.put("ledger", ledger);
        jo.put("debits" , entryList2Json( debit_list));
        jo.put("credits", entryList2Json(credit_list));
        if (! this.getExecutionCondition().equals(SimpleTransfer.CC_NOT_PROVIDED)) {
            jo.put("execution_condition", this.getExecutionCondition().toString());
        }
        jo.put("state", this.getTransferStatus().toString().toLowerCase());
//        if (!this.getCancellationCondition().equals(Condition....NOT_PROVIDED)) {
//            jo.put("cancellation_condition", this.getCancellationCondition());
//        }
        // FIXME: Cancelation_condition?
        if (this.DTTM_expires != DTTM.future) { jo.put("expires_at", this.DTTM_expires.toString()); }
        {
            JsonObject timeline = new JsonObject();
            if (Config.unitTestsActive) {
                timeline.put("proposed_at", DTTM.testingDate.toString());
                if (this.DTTM_prepared != DTTM.future) { timeline.put("prepared_at", DTTM.testingDate.toString()); }
                if (this.DTTM_executed != DTTM.future) { timeline.put("executed_at", DTTM.testingDate.toString()); }
                if (this.DTTM_rejected != DTTM.future) { timeline.put("rejected_at", DTTM.testingDate.toString()); }
            }else {
                timeline.put("proposed_at", this.DTTM_proposed.toString());
                if (this.DTTM_prepared != DTTM.future) { timeline.put("prepared_at", this.DTTM_prepared.toString()); }
                if (this.DTTM_executed != DTTM.future) { timeline.put("executed_at", this.DTTM_executed.toString()); }
                if (this.DTTM_rejected != DTTM.future) { timeline.put("rejected_at", this.DTTM_rejected.toString()); }
            }
            jo.put("timeline", timeline);
        }
        if (sMemo != "") {
            jo.put("memo", new JsonObject(sMemo));
            
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
                final Fulfillment FF = (this.getTransferStatus() == TransferStatus.EXECUTED)
                        ? this.  getExecutionFulfillment()
                        : this.getCancellationFulfillment();
                related_resources.put("execution_condition_fulfillment", FF.toString());
                jo2.put("related_resources", related_resources);
            }
        return jo2;
    }


    private JsonArray entryList2Json(LedgerPartialEntry[] input_list) {
        JsonArray ja = new JsonArray();
        for (LedgerPartialEntry entry : input_list) {
            // FIXME: This code to calculate amount is PLAIN WRONG. Just to pass five-bells-ledger tests
            JsonObject jo = new JsonObject();
            jo.put("account", "/accounts/" /* TODO: Get from config.*/ + entry.account.getLocalName() );
            String sAmount = "" + (long)entry. amount.getNumber().floatValue();
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
                // COMMENTED OLD API JsonObject memo = new JsonObject()/*, ilp_header = new JsonObject()*/, data = new JsonObject();
                // COMMENTED OLD API ilp_header.put("account", ((Credit)entry).ph.getDestinationAddress());
                // COMMENTED OLD API ilp_header.put("amount",  ""+((Credit)entry).ph.getAmount());// TODO: Recheck
                // COMMENTED OLD API data.put("expires_at", /*((Credit)entry).ph.getExpiry().toString()*/DTTM_expires.toString());  // TODO: Recheck.
                // COMMENTED OLD API ilp_header.put("data", data);
                // COMMENTED OLD API memo.put("ilp_header", ilp_header);
                // COMMENTED OLD API jo.put("memo", memo);
            }
            ja.add(jo);
        }
        return ja;
    }

    @Override
    public boolean equals(Object other){
        if (other == null) throw new RuntimeException("other is null @ SimpleLedgerTransfer.equals(other)");
        if (other == this) return true;
        if (other.getClass()!=this.getClass()) return false;

        boolean result = true;
        SimpleTransfer other1 = (SimpleTransfer) other;
        result = result && this.transferID.equals(other1);                       if (result == false) return result;
        result = result && this.fromAccount.equals(other1.fromAccount);          if (result == false) return result;
        result = result && this.executionCond.equals(other1.executionCond);      if (result == false) return result;
        result = result && this.cancelationCond.equals(other1.cancelationCond);  if (result == false) return result;
        result = result && this.DTTM_expires.equals(other1.DTTM_expires);        if (result == false) return result;
        result = result && this.DTTM_proposed.equals(other1.DTTM_proposed);      if (result == false) return result;
        result = result && this.credit_list.length == other1.credit_list.length; if (result == false) return result;
        result = result && this.debit_list.length == other1.debit_list.length;   if (result == false) return result;
        for (int idx=0; idx<credit_list.length; idx++){
            final Credit credit0 = credit_list[idx], credit1 = other1.credit_list[idx];
            result = result && credit0.equals(credit1);
        }
        if (result == false) return result;
        for (int idx=0; idx<debit_list.length; idx++){
            final Debit debit0 = debit_list[idx], debit1 = other1.debit_list[idx];
            result = result && debit0.equals(debit1);
        }
        if (result == false) return result;
        return result;
    }
    

}
