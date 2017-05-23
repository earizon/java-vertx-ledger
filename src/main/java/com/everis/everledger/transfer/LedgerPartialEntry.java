package com.everis.everledger.transfer;

import javax.money.MonetaryAmount;

import com.everis.everledger.ifaces.account.IfaceLocalAccount;


/*
 * Represent a part of the Ledger resitry.
 * Code must use subclasses of this class to make it 
 * clear whether its a Debit or a Credit
 */
public abstract class LedgerPartialEntry {
    public final IfaceLocalAccount account;
    public final MonetaryAmount amount;
    public LedgerPartialEntry(IfaceLocalAccount account, MonetaryAmount amount){
        this.account = account;
        this.amount = amount;
    }
    
    @Override
    public boolean equals(Object other) {
        if (other == null) throw new RuntimeException("comparing to null @ LedgerPartialEntry");
        if (other == this) return true;
        if (!(other instanceof LedgerPartialEntry))return false;
        return account.equals(((LedgerPartialEntry)other).account) &&
                amount.equals(((LedgerPartialEntry)other).amount);
    }
}