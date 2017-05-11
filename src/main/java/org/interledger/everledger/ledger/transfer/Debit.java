package org.interledger.everledger.ledger.transfer;

import javax.money.MonetaryAmount;

import org.interledger.everledger.account.IfaceLocalAccount;

public class Debit extends LedgerPartialEntry {
    public Debit(IfaceLocalAccount account, MonetaryAmount amount){
        super(account, amount);
    }

    public boolean getAuthorized() {
        return true;
    }
}
