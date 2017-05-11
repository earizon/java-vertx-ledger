package org.interledger.everledger.ifaces.account;


public interface IfaceAccountManager extends IfaceLocalAccountManager, IfaceILPSpecAccountManager{
    void store(IfaceAccount account);
}
