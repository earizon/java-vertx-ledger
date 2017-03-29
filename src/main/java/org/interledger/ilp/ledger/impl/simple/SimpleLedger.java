package org.interledger.ilp.ledger.impl.simple;

import org.interledger.ilp.core.Ledger;
import org.interledger.ilp.core.ledger.model.LedgerInfo;
import org.interledger.ilp.common.config.Config;

/**
 * Simple in-memory ledger implementation
 *
 * @author mrmx
 */
public class SimpleLedger implements Ledger {

    private LedgerInfo info;
    private String name;
    private Config config;

    public SimpleLedger(LedgerInfo info, String name, Config config) {
        this.info = info;
        this.name = name;
        this.config = config;
    }

    public LedgerInfo getInfo() {
        return info;
    }

    public String getName() {
        return name;
    }

    /**
     * @return internal implementation server config parameters (public/private keys, ...)
     */
    public Config getConfig() {
        return config;
    }

}
