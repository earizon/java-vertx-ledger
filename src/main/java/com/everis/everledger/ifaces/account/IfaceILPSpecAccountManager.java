package com.everis.everledger.ifaces.account;

/**
 * Defines an account manager.
 *
 * @author earizon
 */
public interface IfaceILPSpecAccountManager { 

    // TODO:(RFC) Could it be possible to have different HOLD accounts for different type of users/sub-ledgers?
    IfaceLocalAccount getHOLDAccountILP();
}
