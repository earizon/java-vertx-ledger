package org.interledger.everledger.ledger.account;

public interface IfaceAccountManager extends IfaceLocalAccountManager, IfaceILPSpecAccountManager{
    void store(IfaceAccount account);
}
