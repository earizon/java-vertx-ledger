package org.interledger.ilp.ledger.account;

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
public interface LedgerAccount {

    @JsonProperty("id")
    String getUri(); // FIXME: Convert String to org.interledger.ilp.core.AccountUri

    String getName();
    
    @JsonIgnore
    String getCurrencyCode();

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
    
    // FIXME: credit & debit not needed must be associated 
    //  to transactions, not Accounts. Accounts must keep only
    // the balance.
    LedgerAccount credit(Number amount);

    LedgerAccount credit(MonetaryAmount amount);

    LedgerAccount debit(Number amount);

    LedgerAccount debit(MonetaryAmount amount);
}
