package org.interledger.everledger.ledger.transfer;

import javax.money.MonetaryAmount;

import org.interledger.everledger.ledger.account.LedgerAccount;

public class Debit extends LedgerPartialEntry {
    public Debit(LedgerAccount account, MonetaryAmount amount, boolean authorized){
        super(account, amount);
    }

    public boolean getAuthorized() {
        return true;
    }
}
