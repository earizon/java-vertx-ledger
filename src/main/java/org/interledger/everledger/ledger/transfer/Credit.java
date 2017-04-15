package org.interledger.ilp.ledger.transfer;


import javax.money.MonetaryAmount;

import org.interledger.ilp.InterledgerPacketHeader;
import org.interledger.ilp.ledger.account.LedgerAccount;

public class Credit extends LedgerPartialEntry {
    public final InterledgerPacketHeader ph;

    public Credit(LedgerAccount account, MonetaryAmount amount, InterledgerPacketHeader ph){
        super(account, amount);
        this.ph = ph;
    }

}