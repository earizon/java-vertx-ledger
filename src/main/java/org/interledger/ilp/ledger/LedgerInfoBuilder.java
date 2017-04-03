package org.interledger.ilp.ledger;

import java.net.URL;
import java.security.PublicKey;
import java.util.Currency;

import javax.money.CurrencyUnit;
import javax.money.format.MonetaryAmountFormat;

import org.interledger.ilp.InterledgerAddress;
import org.interledger.ilp.common.util.StringUtils;
import org.interledger.ilp.ledger.model.LedgerInfo;

/**
 * LedgerInfo builder
 *
 * @author mrmx
 */
public class LedgerInfoBuilder {
    // By default use same values defined at JS implementation 
    // (five-bells-ledger/src/controllers/metadata.js)
    private int precission = 10;
    private int scale = 2;
    private String currencyCode;
    private String currencySymbol;
    private String baseUri;

    public LedgerInfoBuilder() {
    }

    public LedgerInfoBuilder setPrecission(int precission) {
        this.precission = precission;
        return this;
    }    

    public LedgerInfoBuilder setScale(int scale) {
        this.scale = scale;
        return this;
    }    
    
    public LedgerInfoBuilder setCurrency(Currencies currency) {        
        return setCurrencyCodeAndSymbol(currency.code());
    }    

    public LedgerInfoBuilder setCurrency(Currency currency) {        
        return setCurrencyCodeAndSymbol(currency.getCurrencyCode());
    }    

    public LedgerInfoBuilder setCurrency(CurrencyUnit currency) {        
        return setCurrencyCodeAndSymbol(currency.getCurrencyCode());
    }   
    
    public LedgerInfoBuilder setCurrencyCodeAndSymbol(String currencyCode) {
        this.currencyCode = currencyCode;
        this.currencySymbol = CurrencyUtils.getSymbol(currencyCode);
        return this;
    } 
    
    public LedgerInfoBuilder setBaseUri(URL baseUri) {
        this.baseUri = baseUri.toString().replace(":80", "");
        System.out.println(">>>>"+this.baseUri);
        return this;
    }

    
    public LedgerInfo build() {
        if(StringUtils.isBlank(currencyCode)) {
            throw new IllegalArgumentException("currencyCode");
        }
        if(StringUtils.isBlank(currencySymbol)) {
            throw new IllegalArgumentException("currencySymbol");
        }        
        if(StringUtils.isBlank(baseUri)) {
            throw new IllegalArgumentException("baseUri");
        }
        //TODO precission scale
        return new LedgerInfoImpl(precission, scale, currencyCode, currencySymbol);
    }

    static final class LedgerInfoImpl implements LedgerInfo {

        private final int precision;
        private final int scale;
        private final String currencyCode;
        private final String currencySymbol;

        public LedgerInfoImpl(int precision, int scale, String currencyCode, String currencySymbol) {
            this.precision = precision;
            this.scale = scale;
            this.currencyCode = currencyCode;
            this.currencySymbol = currencySymbol;
        }

        /**
         * @return the precision
         */
        public int getPrecision() {
            return precision;
        }

        /**
         * @return the scale
         */
        public int getScale() {
            return scale;
        }

        /**
         * @return the currencyCode
         */
        public String getCurrencyCode() {
            return currencyCode;
        }

        /**
         * @return the currencySymbol
         */
        public String getCurrencySymbol() {
            return currencySymbol;
        }

        @Override
        public PublicKey getConditionSignPublicKey() {
            // FIXME Implement TODO
            throw new RuntimeException("Not implemented");
        }

        @Override
        public String getId() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public InterledgerAddress getAddressPrefix() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public CurrencyUnit getCurrencyUnit() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public MonetaryAmountFormat getMonetaryAmountFormat() {
            // TODO Auto-generated method stub
            return null;
        }


        @Override
        public PublicKey getNotificationSignPublicKey() {
            // TODO:(0) FIXME. Implement
            throw new RuntimeException("Not implemented");
        }

    }
}
