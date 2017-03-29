package org.interledger.ilp.ledger.impl.simple;

import org.interledger.ilp.common.util.StringUtils;
import org.interledger.ilp.core.InterledgerException;

/**
 * Simple local account address parser
 * 
 * @author mrmx
 */
public class SimpleLedgerAddressParser implements LedgerAddressParser {
    private final static String DEFAULT_LEDGER_HOST_ACCOUNT_SEPARATOR = "@";
    
    private String separator;
    private String accountName;
    private String ledgerName;
    
    public SimpleLedgerAddressParser() {
        this(DEFAULT_LEDGER_HOST_ACCOUNT_SEPARATOR);
    }

    public SimpleLedgerAddressParser(String separator) {
        if(separator == null) {
            separator = DEFAULT_LEDGER_HOST_ACCOUNT_SEPARATOR;
        }
        this.separator = separator;
    }
    
    @Override
    public void parse(String address) {
        if(StringUtils.isBlank(address)) {
            throw new InterledgerException(InterledgerException.RegisteredException.InternalError, "empty address");
        }
        if(!address.contains(separator)) {
            throw new InterledgerException(InterledgerException.RegisteredException.InternalError, address);
        }
        try {
            String parts[] = address.trim().split(separator);
            accountName = parts[0];
            if(StringUtils.isBlank(accountName)) {
                throw new IllegalArgumentException("empty account name");
            }
            ledgerName = parts[1];
            if(StringUtils.isBlank(ledgerName)) {
                throw new IllegalArgumentException("empty ledger name");
            }            
        }catch(Exception ex) {
            throw new InterledgerException(InterledgerException.RegisteredException.InternalError, address, ex);
        }
    }

    @Override
    public String getAccountName() {
        return accountName;
    }
    
    @Override
    public String getLedgerName() {
        return ledgerName;
    }

    public String getSeparator() {
        return separator;
    }
    
}