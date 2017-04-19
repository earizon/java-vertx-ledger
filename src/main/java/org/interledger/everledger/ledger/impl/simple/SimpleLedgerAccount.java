package org.interledger.everledger.ledger.impl.simple;


import java.util.Objects;

import javax.money.MonetaryAmount;
import javax.money.NumberValue;

import org.interledger.everledger.common.config.Config;
import org.interledger.everledger.common.util.NumberConversionUtil;
import org.interledger.everledger.ledger.MoneyUtils;
import org.interledger.everledger.ledger.account.LedgerAccount;
import org.javamoney.moneta.Money;

/**
 * Represents a simple ledger account.
 *
 * @author mrmx
 */
// TODO:(0) Fixme must implement org.interledger.ilp.ledger.model.AccountInfo,
//    (not custom org.interledger.everledger.ledger.account.LedgerAccount)
public class SimpleLedgerAccount implements LedgerAccount {

    public static final String currencyCode = Config.ledgerCurrencyCode;
    // TODO:(0) Convert to "inmutable" object. 
    // TODO:(0) Rename as ID and add {first name, second name, ...} if needed.
    //    (Check commented @JsonProperty("id") in parent class)
    private final String name; 
    private MonetaryAmount balance;
    private MonetaryAmount minimumAllowedBalance;
    private Boolean admin;
    private boolean disabled;
    private String connector;

    public SimpleLedgerAccount(String name) {
        Objects.nonNull(name);
        this.name = name;
        this.balance = Money.of(0, currencyCode);
        this.minimumAllowedBalance = Money.of(0, currencyCode);
        this.disabled = false;
    }

    @Override
    public String getName() {
        return name;
    }

    public LedgerAccount setAdmin(boolean admin) {
        this.admin = admin;
        return this;
    }

    @Override
    public Boolean isAdmin() {
        return admin;
    }

    public LedgerAccount setDisabled(boolean disabled) {
        this.disabled = disabled;
        return this;
    }

    @Override
    public boolean isDisabled() {
        return disabled;
    }

    @Override
    public LedgerAccount setMinimumAllowedBalance(Number balance) {
        return setMinimumAllowedBalance(Money.of(balance, currencyCode));
    }

    @Override
    public LedgerAccount setMinimumAllowedBalance(MonetaryAmount balance) {
        this.minimumAllowedBalance = balance;
        return this;
    }

    @Override
    public MonetaryAmount getMinimumAllowedBalance() {
        return minimumAllowedBalance;
    }

    @Override
    public String getMinimumAllowedBalanceAsString() {
        return NumberConversionUtil.toString(getMinimumAllowedBalance().getNumber());
    }

    @Override
    public SimpleLedgerAccount setBalance(Number balance) {
        Objects.nonNull(balance);
        return setBalance(Money.of(balance, currencyCode));
    }

    @Override
    public SimpleLedgerAccount setBalance(MonetaryAmount balance) {
        Objects.nonNull(balance);
        this.balance = balance;
        return this;
    }

    @Override
    public MonetaryAmount getBalance() {
        return balance;
    }

    @Override
    public String getBalanceAsString() {
        NumberValue balance = getBalance().getNumber();
        if(balance.getAmountFractionDenominator() == 0) {
            return String.valueOf(balance.longValueExact());
        }
        return String.valueOf(balance.doubleValueExact());
    }

    public void setConnector(String connector) {
        this.connector = connector;
    }

    @Override
    public String getConnector() {
        return connector;
    }

    @Override
    public SimpleLedgerAccount credit(Number amount) {
        return credit(Money.of(amount, currencyCode));
    }

    @Override
    public SimpleLedgerAccount credit(MonetaryAmount amount) {
        // TODO: FIXME: Check amount > 0
        setBalance(getBalance().add(amount));
        return this;
    }

    @Override
    public SimpleLedgerAccount debit(Number amount) {
        return debit(Money.of(amount, currencyCode));
    }

    @Override
    public SimpleLedgerAccount debit(MonetaryAmount amount) {
        Objects.nonNull(amount);
        setBalance(getBalance().subtract(amount));
        return this;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof SimpleLedgerAccount)) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        SimpleLedgerAccount other =  (SimpleLedgerAccount) obj;
        boolean result = name.equals(other.getName());
        // Extra checks while refactoring name -> account_id;
        assert(balance.equals(other.getBalance()));
        return result;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(getClass().getSimpleName());
        sb.append("[");
        sb.append("name:").append(getName()).append(",");
        sb.append("balance:").append(getBalance());
        sb.append("]");
        return sb.toString();

    }

    protected MonetaryAmount toMonetaryAmount(String amount) {
        return MoneyUtils.toMonetaryAmount(amount, currencyCode);
    }

}
