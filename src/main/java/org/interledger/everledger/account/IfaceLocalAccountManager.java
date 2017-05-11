package org.interledger.everledger.account;

import java.util.Collection;

/**
 * This interface defines a simple NON-normative
 * example contract that an AccountManger must
 * implement for local NON-ILP related operations.
 * Most probably any ledger will implement other interfaces
 * 
 * The concrete implementation, on the otherside, must
 * also implement the standard/normative 
 * IfaceILPSpecAccountManager in order to provide support 
 * for the Interledger ILP protocol.
 * See SimpleLedgerAccountManager for more info.
 *
 */
public interface IfaceLocalAccountManager {

    IfaceAccount create(String name);

    int getTotalAccounts();

    boolean hasAccount(String name);

    IfaceLocalAccount getAccountByName(String name);

    Collection<IfaceLocalAccount> getAccounts(int page, int pageSize);


}
