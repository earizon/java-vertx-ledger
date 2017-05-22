package org.interledger.everledger.ifaces.account;

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
    public String getLocalID();

    IfaceLocalAccount setMinimumAllowedBalance(Number balance);

    IfaceLocalAccount setMinimumAllowedBalance(MonetaryAmount balance);

    IfaceLocalAccount setBalance(Number balance);

    IfaceLocalAccount setBalance(MonetaryAmount balance);

    @JsonIgnore
    MonetaryAmount getLocalBalance();

    @JsonProperty("balance")
    String getBalanceAsString();


    String getConnector();

    IfaceLocalAccount credit(Number amount);

    IfaceLocalAccount credit(MonetaryAmount amount);

    IfaceLocalAccount debit(Number amount);

    IfaceLocalAccount debit(MonetaryAmount amount);
}
