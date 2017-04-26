package org.interledger.everledger.ledger.account;

import org.interledger.ilp.ledger.model.AccountInfo;


/**
 * This interface is the "sum" of local and ILP(remote) interfaces
 *
 */
public interface IfaceAccount extends IfaceLocalAccount, AccountInfo {
}
