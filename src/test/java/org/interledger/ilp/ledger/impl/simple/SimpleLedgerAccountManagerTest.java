package org.interledger.ilp.ledger.impl.simple;

import java.net.URL;
import java.util.Collection;

import org.interledger.ilp.common.config.Config;
import org.interledger.ilp.core.AccountURI;
import org.interledger.ilp.core.ledger.model.LedgerInfo;
import org.interledger.ilp.ledger.Currencies;
import org.interledger.ilp.ledger.LedgerAccountManagerFactory;
import org.interledger.ilp.ledger.LedgerFactory;
import org.interledger.ilp.ledger.LedgerInfoBuilder;
import org.interledger.ilp.ledger.account.LedgerAccount;
import org.interledger.ilp.ledger.account.LedgerAccountManager;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author mrmx
 */
public class SimpleLedgerAccountManagerTest {

    LedgerAccountManager instance;
    
    final static String URI_LEDGER_A = "ledger1.example";
    final static String URI_LEDGER_B = "ledger2.example";

    AccountURI aliceURI = new AccountURI("https://"+URI_LEDGER_A, "alice");
    AccountURI bobURI = new AccountURI("https://"+URI_LEDGER_B, "bob");
    

    @BeforeClass
    public static void init() throws Exception {
        LedgerInfo ledgerInfo = new LedgerInfoBuilder()
            .setCurrency(Currencies.EURO)
            .setBaseUri(new URL("https", URI_LEDGER_A, 80, ""))
            .build();        
        LedgerFactory.initialize(ledgerInfo, "test-ledger", Config.create());
    }
    
    @Before
    public void setUp() {        
        instance = LedgerAccountManagerFactory.createLedgerAccountManager();
    }

    /**
     * Test of create method, of class SimpleLedgerAccountManager.
     */
    @Test
    public void testCreate() {
        System.out.println("create");
        LedgerAccount result = instance.create(aliceURI.getAccountId());        
        System.out.println("result:" + result);
        assertNotNull(result);
        assertEquals(aliceURI, instance.getAccountUri(result));
        assertEquals("EUR", result.getCurrencyCode());
        assertEquals("Balance",0d, result.getBalance().getNumber().doubleValue(),0d);
    }

    /**
     * Test of addAccounts method, of class SimpleLedgerAccountManager.
     */
    @Test
    public void testAddAccounts() {
        System.out.println("addAccounts");
        assertEquals(0, instance.getTotalAccounts());
        instance.store(new SimpleLedgerAccount("alice", "EUR"));
        instance.store(new SimpleLedgerAccount("bob", "EUR"));
        assertEquals(2, instance.getTotalAccounts());        
    }

    /**
     * Test of store method, of class SimpleLedgerAccountManager.
     */
    @Test
    public void testAddAccount() {
        System.out.println("addAccount");
        assertEquals(0, instance.getTotalAccounts());
        instance.store(new SimpleLedgerAccount("alice", "EUR"));
        assertEquals(1, instance.getTotalAccounts());
    }

    /**
     * Test of getAccountByName method, of class SimpleLedgerAccountManager.
     */
    @Test
    public void testGetAccountByName() {
        System.out.println("getAccountByName");
        LedgerAccount bob = instance.create("bob");
        instance.store(bob);
        instance.store(instance.create("alice"));
        assertEquals(2, instance.getTotalAccounts());
        LedgerAccount result = instance.getAccountByName("bob");
        assertEquals(bob, result);
    }

    /**
     * Test of getAccounts method, of class SimpleLedgerAccountManager.
     */
    @Test
    public void testGetAccounts() {
        System.out.println("testGetAccounts");
        LedgerAccount bob = instance.create("bob");
        instance.store(bob);
        instance.store(instance.create("alice"));
        assertEquals(2, instance.getTotalAccounts());
        Collection<LedgerAccount> result = instance.getAccounts(1, 1);
        System.out.println("result:" + result);
        assertEquals(2, result.size());        
    }

}
