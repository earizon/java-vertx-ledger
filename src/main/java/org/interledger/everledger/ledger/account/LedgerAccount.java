package org.interledger.everledger.ledger.account;

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
// TODO:(0) Remove and use java-ilp-core org.interledger.ilp.ledger.model.AccountInfo
public interface LedgerAccount {

    // @JsonProperty("id") // TODO:(0)
    String getName();
    

    LedgerAccount setMinimumAllowedBalance(Number balance);

    LedgerAccount setMinimumAllowedBalance(MonetaryAmount balance);

    @JsonIgnore
    MonetaryAmount getMinimumAllowedBalance();

    @JsonProperty("minimum_allowed_balance")
    String getMinimumAllowedBalanceAsString();

    LedgerAccount setBalance(Number balance);

    LedgerAccount setBalance(MonetaryAmount balance);

    @JsonIgnore
    MonetaryAmount getBalance();

    @JsonProperty("balance")
    String getBalanceAsString();
    
    @JsonProperty("is_admin")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    Boolean isAdmin();

    @JsonProperty("is_disabled")
    boolean isDisabled();
    
    String getConnector();

    LedgerAccount credit(Number amount);

    LedgerAccount credit(MonetaryAmount amount);

    LedgerAccount debit(Number amount);

    LedgerAccount debit(MonetaryAmount amount);
}
