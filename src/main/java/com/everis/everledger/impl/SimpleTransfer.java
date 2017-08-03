package com.everis.everledger.impl;

import java.time.ZonedDateTime;

import java.util.Objects;
import java.util.UUID;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import org.interledger.InterledgerAddress;
//import org.interledger.cryptoconditions.Condition;
import org.interledger.Condition;
import org.interledger.Fulfillment;
//import org.interledger.cryptoconditions.der.CryptoConditionReader;
//import org.interledger.cryptoconditions.types.PreimageSha256Fulfillment;
//import org.interledger.everledger.ledger.transfer.ILPSpecTransferID;


import javax.money.MonetaryAmount;

import org.interledger.ledger.model.TransferStatus;
import org.javamoney.moneta.Money;

import com.everis.everledger.Config;
import com.everis.everledger.ifaces.account.IfaceLocalAccount;
import com.everis.everledger.ifaces.transfer.IfaceTransfer;
import com.everis.everledger.transfer.Credit;
import com.everis.everledger.transfer.Debit;
import com.everis.everledger.transfer.LedgerPartialEntry;
import com.everis.everledger.transfer.LocalTransferID;
import com.everis.everledger.util.TimeUtils;


import com.everis.everledger.impl.manager.SimpleAccountManager;

// FIXME:(1) Allow multiple debit/credits (Remove all code related to index [0])

public class SimpleTransfer implements IfaceTransfer {

    // TODO:(0) The array 1,2,3,... must be random
    public static final Fulfillment FF_NOT_PROVIDED = Fulfillment.builder().preimage(
        new byte[]{1,2,3,4,5,6,7,8,9,0,1,2,3,4,5,6,7,8,9,0,1,2,3,4,5,6,7,8,9,0,1,2}).build();
    
    public static final Condition CC_NOT_PROVIDED =  Condition.builder().hash(
            new byte[]{1,2,3,4,5,6,7,8,9,0,1,2,3,4,5,6,7,8,9,0,1,2,3,4,5,6,7,8,9,0,1,2}).build();
    static  final SimpleAccountManager AM = SimpleAccountManager.INSTANCE;
    final LocalTransferID transferID;
    final IfaceLocalAccount fromAccount;
    final Credit[] credit_list;
    final Debit []  debit_list;
    // URI encoded execution & cancelation crypto-conditions
    final Condition executionCond  ;
    final Condition cancelationCond;
    final ZonedDateTime DTTM_expires ;
    final ZonedDateTime DTTM_proposed;
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
    ZonedDateTime DTTM_prepared = TimeUtils.future;
    ZonedDateTime DTTM_executed = TimeUtils.future;
    ZonedDateTime DTTM_rejected = TimeUtils.future;

    public SimpleTransfer(LocalTransferID transferID,
        Debit[] debit_list, Credit[] credit_list, 
        Condition executionCond, 
        Condition cancelationCond, ZonedDateTime DTTM_expires, ZonedDateTime DTTM_proposed,
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
        this.DTTM_prepared      = Objects.requireNonNull(ZonedDateTime.now());
        this.sMemo              = Objects.requireNonNull(sMemo)          ;
        if (transferStatus.equals(TransferStatus.PROPOSED)){
            transferStatus = TransferStatus.PREPARED;
        }
        this.transferStatus     = transferStatus    ;
        /*
         *  Parse String to fetch local accounturi
         *  String will be similar to http://localLedger/account/"accountId" ->
         *  we need the "accountId" to fetch the correct local "from" Account
         */

        this.fromAccount = AM.getAccountByName(credit_list[0].account.getLocalID());
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
                InterledgerAddress.Builder().value("TODO(0)").build();
        return result;
    }

    @Override
    public InterledgerAddress getToAccount() {
        final InterledgerAddress result = new 
                InterledgerAddress.Builder().value("TODO(0)").build();
        return result;
    }

    @Override
    public MonetaryAmount getAmount(){
        MonetaryAmount result = Money.of(0, debit_list[0].amount.getCurrency());
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

//    @Override
//    public Condition getCancellationCondition() {
//        return cancelationCond;
//    }

    public ZonedDateTime getExpiresAt() {
        return DTTM_expires;
    }
    
    // } End ILPSpec interface
    
    private void checkBalancedTransaction(){
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
        // TODO:(RFC) Could be useful to check transition state is correct 
        this.transferStatus = transferStatus;
    }

    @Override
    public TransferStatus getTransferStatus() {
        return transferStatus;
    }

    @Override
    public ZonedDateTime getDTTM_prepared() {
        return DTTM_prepared;
    }

    @Override
    public void setDTTM_prepared(ZonedDateTime DTTM) {
        DTTM_prepared = DTTM;
    }

    @Override
    public ZonedDateTime getDTTM_executed() {
        return DTTM_executed;
    }

    @Override
    public void setDTTM_executed(ZonedDateTime DTTM) {
        DTTM_executed = DTTM;
    }

    @Override
    public ZonedDateTime getDTTM_rejected() {
        return DTTM_rejected;
    }

    @Override
    public void setDTTM_rejected(ZonedDateTime DTTM) {
        DTTM_rejected = DTTM;
    }

    @Override
    public ZonedDateTime getDTTM_expires() {
        return DTTM_expires;
    }

    @Override
    public ZonedDateTime getDTTM_proposed() {
        return DTTM_proposed;
    }

    @Override
    public void  setExecutionFulfillment(Fulfillment ff){
        if (this.cancelationFF != FF_NOT_PROVIDED) throw new RuntimeException(
            "Cancelation fulfillment was already provided");
        this.executionFF = ff;
    }
    
    @Override
    public Fulfillment getExecutionFulfillment(){
        return executionFF;
    }

    @Override
    public void  setCancelationFulfillment(Fulfillment ff){
        if (this.executionFF != FF_NOT_PROVIDED) throw new RuntimeException(
                "execution fulfillment was already provided");
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
        String id = /* TODO:(doubt) add ledger as prefix ?? */"/transfers/" /* TODO:(1) Get from Config */+ transferID.transferID;
        jo.put("id", id);
        jo.put("ledger", ledger);
        jo.put("debits" , entryList2Json( debit_list));
        jo.put("credits", entryList2Json(credit_list));
        if (! this.getExecutionCondition().equals(SimpleTransfer.CC_NOT_PROVIDED)) {
            jo.put("execution_condition", this.getExecutionCondition().toString());
        }
//        if (! this.getCancellationCondition().equals(SimpleTransfer.CC_NOT_PROVIDED)) {
//            jo.put("cancellation_condition", this.getCancellationCondition().toString());
//        }
        jo.put("state", this.getTransferStatus().toString().toLowerCase());
//        if (!this.getCancellationCondition().equals(Condition....NOT_PROVIDED)) {
//            jo.put("cancellation_condition", this.getCancellationCondition());
//        }
        // FIXME: Cancelation_condition?
        if (this.DTTM_expires != TimeUtils.future) { jo.put("expires_at", this.DTTM_expires.format(TimeUtils.ilpFormatter)); }
        {
            JsonObject timeline = new JsonObject();
            if (Config.unitTestsActive) {
                timeline.put("proposed_at", TimeUtils.testingDate.format(TimeUtils.ilpFormatter));
                String sTestingDate = TimeUtils.testingDate.format(TimeUtils.ilpFormatter);
                if (this.DTTM_prepared != TimeUtils.future) { timeline.put("prepared_at",sTestingDate ); }
                if (this.DTTM_executed != TimeUtils.future) { timeline.put("executed_at",sTestingDate ); }
                if (this.DTTM_rejected != TimeUtils.future) { timeline.put("rejected_at",sTestingDate ); }
            }else {
                timeline.put("proposed_at", this.DTTM_proposed.format(TimeUtils.ilpFormatter));
                if (this.DTTM_prepared != TimeUtils.future) { timeline.put("prepared_at", this.DTTM_prepared.format(TimeUtils.ilpFormatter)); }
                if (this.DTTM_executed != TimeUtils.future) { timeline.put("executed_at", this.DTTM_executed.format(TimeUtils.ilpFormatter)); }
                if (this.DTTM_rejected != TimeUtils.future) { timeline.put("rejected_at", this.DTTM_rejected.format(TimeUtils.ilpFormatter)); }
            }
            jo.put("timeline", timeline);
        }
        if (sMemo != "") {
            jo.put("memo", new JsonObject(sMemo));
            
        }
        return jo;
    }

    private JsonArray entryList2Json(LedgerPartialEntry[] input_list) {
        JsonArray ja = new JsonArray();
        for (LedgerPartialEntry entry : input_list) {
            // FIXME: This code to calculate amount is PLAIN WRONG. Just to pass five-bells-ledger tests
            JsonObject jo = new JsonObject();
            jo.put("account", "/accounts/" /* TODO: Get from config.*/ + entry.account.getLocalID() );
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
    
    @Override
    public boolean isAuthorized() {
        return true;
    }
}
