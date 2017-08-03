package com.everis.everledger.ifaces.account;

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

    MonetaryAmount getLocalBalance();

    String getBalanceAsString();

    IfaceLocalAccount credit(MonetaryAmount amount);

    IfaceLocalAccount debit(MonetaryAmount amount);

}
