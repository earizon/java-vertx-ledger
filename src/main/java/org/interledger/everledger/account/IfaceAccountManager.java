package org.interledger.everledger.account;

public interface IfaceAccountManager extends IfaceLocalAccountManager, IfaceILPSpecAccountManager{
    void store(IfaceAccount account);
}
