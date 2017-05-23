package com.everis.everledger.transfer;


import javax.money.MonetaryAmount;

import com.everis.everledger.ifaces.account.IfaceLocalAccount;

public class Credit extends LedgerPartialEntry {
//    public final InterledgerPacketHeader ph;

    public Credit(IfaceLocalAccount account, MonetaryAmount amount/*, InterledgerPacketHeader ph*/){
        super(account, amount);
//        this.ph = ph;
    }

}