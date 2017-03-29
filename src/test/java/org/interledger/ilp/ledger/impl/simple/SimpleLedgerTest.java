package org.interledger.ilp.ledger.impl.simple;

import org.interledger.ilp.common.config.Config;
import org.interledger.ilp.core.ledger.model.LedgerInfo;
import org.interledger.ilp.ledger.Currencies;
import org.interledger.ilp.ledger.LedgerFactory;
import org.interledger.ilp.ledger.LedgerInfoBuilder;

import static org.junit.Assert.*;

import java.net.MalformedURLException;
import java.net.URL;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Simple ledger tests
 *
 * @author mrmx
 */
public class SimpleLedgerTest {

    static final Currencies CURRENCY = Currencies.EURO;
    static final LedgerInfo ledgerInfo;
    static {
        try {
            ledgerInfo = new LedgerInfoBuilder()
                .setCurrency(CURRENCY)
                .setBaseUri(new URL("https", "ledger.example", 80, ""))
                .build();
        } catch (MalformedURLException e) {
            throw new RuntimeException(e.toString(), e);
        }
    }

    SimpleLedger instance;

    final String ALICE = "alice";
    final String BOB = "bob";
    
    @BeforeClass
    public static void init() {
        Config config = Config.create();
        LedgerFactory.initialize(ledgerInfo, "test-ledger", config);
    }
    
    @Before
    public void setUp() {
        Config config = Config.create();
        instance = new SimpleLedger(ledgerInfo, "test", config);
    }

    /**
     * Test of getInfo method, of class SimpleLedger.
     */
    @Test
    public void testGetInfo() {
        System.out.println("getInfo");
        LedgerInfo result = instance.getInfo();
        assertNotNull(result);
    }

}
