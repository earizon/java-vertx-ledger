package org.interledger.everledger.ledger.account;

import java.net.URI;

/**
 * Defines an account manager.
 *
 * @author earizon
 */
public interface ILPAccountSupport { // TODO:(0) Rename all interfaces as Iface....

    LedgerAccount getHOLDAccountILP();
    URI getPublicURIForAccount(LedgerAccount name);

}
