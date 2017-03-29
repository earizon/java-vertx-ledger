package org.interledger.ilp.ledger.impl.simple;

/**
 * Parses addresses into account and ledger
 * 
 * @author mrmx
 */
interface LedgerAddressParser {
   void parse(String address);
   String getAccountName();
   String getLedgerName();
}
