package org.interledger.ilp.ledger.impl.simple;

import java.util.Currency;
import javax.money.CurrencyUnit;
import javax.money.MonetaryAmount;
import javax.money.NumberValue;
import org.interledger.ilp.common.util.NumberConversionUtil;
import org.interledger.ilp.core.AccountURI;
import org.interledger.ilp.ledger.LedgerAccountManagerFactory;
import org.interledger.ilp.ledger.MoneyUtils;
import org.interledger.ilp.ledger.account.LedgerAccount;
import org.javamoney.moneta.Money;

/**
 * Represents a simple ledger account.
 *
 * @author mrmx
 */
public class SimpleLedgerAccount implements LedgerAccount {

    private AccountURI accountUri;
    private final String name;
    private final String currencyCode;
    private MonetaryAmount balance;
    private MonetaryAmount minimumAllowedBalance;
    private Boolean admin;
    private boolean disabled;
    private String connector;

    public SimpleLedgerAccount(String name, Currency currency) {
        this(name, currency.getCurrencyCode());
    }

    public SimpleLedgerAccount(String name, CurrencyUnit currencyUnit) {
        this(name, currencyUnit.getCurrencyCode());
    }

    public SimpleLedgerAccount(String name, String currencyCode) {
        if (name == null) {
            throw new IllegalArgumentException("name         null at SimpleLedgerAccount constructor");
        }
        if (currencyCode == null) {
            throw new IllegalArgumentException("currencyCode null at SimpleLedgerAccount constructor");
        }

        this.name = name;
        this.currencyCode = currencyCode;
        this.balance = Money.of(0, currencyCode);
        this.minimumAllowedBalance = Money.of(0, currencyCode);
        this.disabled = false;
    }

    @Override
    public String getUri() { // FIXME: rename to getAccountUri
        return getAccountUri().getUri();
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
    public String getCurrencyCode() {
        return currencyCode;
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
        return getUri().equalsIgnoreCase(((SimpleLedgerAccount) obj).getUri());
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

    private AccountURI getAccountUri() {
        if (accountUri == null) {
            accountUri = LedgerAccountManagerFactory.getLedgerAccountManagerSingleton().getAccountUri(this);
        }
        return accountUri;
    }
}
