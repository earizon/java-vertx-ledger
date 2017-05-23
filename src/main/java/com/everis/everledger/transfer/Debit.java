package com.everis.everledger.transfer;

import javax.money.MonetaryAmount;

import com.everis.everledger.ifaces.account.IfaceLocalAccount;

public class Debit extends LedgerPartialEntry {
    public Debit(IfaceLocalAccount account, MonetaryAmount amount){
        super(account, amount);
    }

    public boolean getAuthorized() {
        return true;
    }
}
