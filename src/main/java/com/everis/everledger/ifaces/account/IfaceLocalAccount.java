package com.everis.everledger.ifaces.account;

import com.everis.everledger.AuthInfo;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import javax.money.MonetaryAmount;


/**
 * This interface defines a ledger account.
 *
 * @author mrmx
 */
@JsonInclude(JsonInclude.Include.NON_ABSENT)
public interface IfaceLocalAccount {

    /**
     * localID can be a public blockchain address (hash of the public key),
     * and internal or mapped database id, ...
     * @return
     */
    String getLocalID();

    @Deprecated
    // Use *Manager instead
    MonetaryAmount getLocalBalance();

    @Deprecated
    // Use *Manager instead
    String getBalanceAsString();

    AuthInfo getAuthInfo();
 // @Deprecated
 // // Use *Manager instead
 // IfaceLocalAccount credit(MonetaryAmount amount);

 // @Deprecated
 // // Use *Manager instead
 // IfaceLocalAccount debit(MonetaryAmount amount);

}
