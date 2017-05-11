package org.interledger.everledger.ifaces.account;

import java.net.URI;

/**
 * Defines an account manager.
 *
 * @author earizon
 */
//TODO:(0) Rename all ILP related interfaces as IfaceILP
public interface IfaceILPSpecAccountManager { 

    // TODO:(0) Could it be possible to have different HOLD accounts for different type of users/sub-ledgers?
    IfaceLocalAccount getHOLDAccountILP();
    URI getPublicURIForAccount(IfaceLocalAccount name);

}