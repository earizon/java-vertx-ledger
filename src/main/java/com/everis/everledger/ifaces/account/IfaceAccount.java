package com.everis.everledger.ifaces.account;


import org.interledger.ledger.model.AccountInfo;

/**
 * This interface is the "sum" of local and ILP(remote) interfaces
 *
 */
public interface IfaceAccount extends IfaceLocalAccount, AccountInfo {
}
