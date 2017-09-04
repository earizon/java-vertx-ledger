package com.everis.everledger.ifaces.account;

import com.everis.everledger.AuthInfo;

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

    int getTotalAccounts();

    boolean hasAccount(String id);

    IfaceAccount getAccountById(String id);

    Collection<IfaceAccount> getAccounts(int page, int pageSize);

    // If update == false and account already registered => throw exception
    IfaceAccount store(IfaceAccount account, boolean update);

    boolean authInfoMatchAccount(IfaceAccount account, AuthInfo ai);
}
