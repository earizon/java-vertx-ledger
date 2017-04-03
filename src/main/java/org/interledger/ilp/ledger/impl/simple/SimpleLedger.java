package org.interledger.ilp.ledger.impl.simple;

import org.interledger.ilp.ledger.Ledger;
import org.interledger.ilp.ledger.model.LedgerInfo;
import org.interledger.ilp.common.config.Config;

/**
 * Simple in-memory ledger implementation
 *
 * @author mrmx
 */
public class SimpleLedger implements Ledger {

    private LedgerInfo info;
    private String name; // TODO:(?) Place "inside" config?
    private Config config;

    public SimpleLedger(LedgerInfo info, String name, Config config) {
        this.info = info;
        this.name = name;
        this.config = config;
    }

    @Override
    public LedgerInfo getInfo() {
        return info;
    }

    public String getName() {
        return name;
    }

    /**
     * @return internal implementation server config parameters (public/private keys, ...)
     */
    @Override
    public Config getConfig() {
        return config;
    }

}
