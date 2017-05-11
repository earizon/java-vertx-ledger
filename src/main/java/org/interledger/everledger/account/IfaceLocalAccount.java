package org.interledger.everledger.account;

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

    public String getLocalName();

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
