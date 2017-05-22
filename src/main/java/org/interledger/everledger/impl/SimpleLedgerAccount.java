package org.interledger.everledger.impl;


import java.util.Objects;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.PublicKey;

import javax.money.MonetaryAmount;
import javax.money.NumberValue;

import org.interledger.ilp.InterledgerAddress;
import org.interledger.ilp.InterledgerAddressBuilder;
import org.interledger.everledger.Config;
import org.interledger.everledger.ifaces.account.IfaceAccount;
import org.interledger.everledger.ifaces.account.IfaceLocalAccount;
import org.interledger.everledger.util.NumberConversionUtil;
import org.javamoney.moneta.Money;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a simple ledger account.
 *
 */
public class SimpleLedgerAccount implements IfaceAccount {

    public static final String currencyCode = Config.ledgerCurrencyCode;
    // TODO:(0) Convert to "inmutable" object. 
    private final String localID; 
    private MonetaryAmount balance;
    private MonetaryAmount minimumAllowedBalance;
    private boolean disabled;
    private String connector;

    public SimpleLedgerAccount(String name) {
        Objects.nonNull(name);
        this.localID = name;
        this.balance = Money.of(0, currencyCode);
        this.minimumAllowedBalance = Money.of(0, currencyCode);
        this.disabled = false;
    }

    // START IMPLEMENTATION IfaceLocalAccount {

    @Override
    @JsonIgnore
    public String getLocalID() {
        return localID;
    }

    @Override
    public IfaceLocalAccount setMinimumAllowedBalance(Number balance) {
        return setMinimumAllowedBalance(Money.of(balance, currencyCode));
    }

    @Override
    public IfaceLocalAccount setMinimumAllowedBalance(MonetaryAmount balance) {
        this.minimumAllowedBalance = balance;
        return this;
    }

    @Override
    @JsonIgnore
    public MonetaryAmount getMinimumAllowedBalance() {
        return minimumAllowedBalance;
    }

    @JsonProperty("minimum_allowed_balance")
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
    public MonetaryAmount getLocalBalance() {
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

    // } END   IMPLEMENTATION IfaceLocalAccount

    
    // START IMPLEMENTATION IfaceLocalAccount {
    @Override
    @JsonIgnore
    public InterledgerAddress getLedger() {
        return new InterledgerAddressBuilder().value(Config.ilpPrefix).build();
    }

    @Override
    public String getId() {
        String baseURI = Config.publicURL.toString();
        String sURI = baseURI+"accounts/"+getLocalID();
        try {
            URI result = new URI(sURI);
            return result.toString();
        } catch (URISyntaxException e) {
            throw new RuntimeException("Can't create URI from string '"+sURI+"'");
        }
    }
    
    @Override
    public String getName() {
        return getLocalID();
    }
    
    @Override
    @JsonIgnore
    public InterledgerAddress getAddress() {
        return new InterledgerAddressBuilder().value(Config.ilpPrefix+"."+getLocalID()).build();
    }
    
    @Override
    public MonetaryAmount getBalance() {
        return getLocalBalance();
    }

    @Override
    @JsonProperty("is_disabled")
    public boolean isDisabled() {
        return disabled;
    }

    @Override
    @JsonIgnore
    public byte[] getCertificateFingerprint() {
        return new byte[]{}; // TODO:(0) FIXME
    }

    @Override
    public PublicKey getPublicKey() {
        return null; // TODO:(0) FIXME
    }

    // } END   IMPLEMENTATION IfaceLocalAccount 

    // START OVERRIDING Object {

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof SimpleLedgerAccount)) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        SimpleLedgerAccount other =  (SimpleLedgerAccount) obj;
        boolean result = localID.equals(other.getName());
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
}
