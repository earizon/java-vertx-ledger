package org.interledger.everledger.ledger.transfer;


import javax.money.MonetaryAmount;

import org.interledger.everledger.ledger.account.IfaceLocalAccount;
import org.interledger.ilp.InterledgerPacketHeader;

public class Credit extends LedgerPartialEntry {
    public final InterledgerPacketHeader ph;

    public Credit(IfaceLocalAccount account, MonetaryAmount amount, InterledgerPacketHeader ph){
        super(account, amount);
        this.ph = ph;
    }

}