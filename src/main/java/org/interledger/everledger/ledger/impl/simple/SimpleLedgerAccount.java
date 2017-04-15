package org.interledger.everledger.ledger.impl.simple;

import javax.money.MonetaryAmount;
import javax.money.NumberValue;

import org.interledger.everledger.common.util.NumberConversionUtil;
import org.interledger.everledger.ledger.LedgerFactory;
import org.interledger.everledger.ledger.MoneyUtils;
import org.interledger.everledger.ledger.account.LedgerAccount;
import org.javamoney.moneta.Money;

/**
 * Represents a simple ledger account.
 *
 * @author mrmx
 */
public class SimpleLedgerAccount implements LedgerAccount {

    public static final String currencyCode = LedgerFactory.getDefaultLedger().getInfo().getCurrencyUnit().getCurrencyCode();

    // TODO:(0) Rename as ID and add {first name, second name, ...} if needed.
    //    (Check commented @JsonProperty("id") in parent class)
    private final String name; 
    private MonetaryAmount balance;
    private MonetaryAmount minimumAllowedBalance;
    private Boolean admin;
    private boolean disabled;
    private String connector;

    public SimpleLedgerAccount(String name) {
        if (name == null) {
            throw new IllegalArgumentException("name         null at SimpleLedgerAccount constructor");
        }

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
        // TODO: FIXME: Check balance > 0
        return setBalance(Money.of(balance, currencyCode));
    }

    @Override
    public SimpleLedgerAccount setBalance(MonetaryAmount balance) {
        // TODO: FIXME: Check balance > 0
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

    public SimpleLedgerAccount credit(String amount) {
        return credit(toMonetaryAmount(amount));
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

    public SimpleLedgerAccount debit(String amount) {
        // TODO: FIXME: Check amount > 0
        return debit(toMonetaryAmount(amount));
    }

    @Override
    public SimpleLedgerAccount debit(Number amount) {
        return debit(Money.of(amount, currencyCode));
    }

    @Override
    public SimpleLedgerAccount debit(MonetaryAmount amount) {
        // TODO: FIXME: Check amount > 0
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
