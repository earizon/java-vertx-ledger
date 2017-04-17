package org.interledger.ilp.ledger.impl.simple;

import java.util.Collection;

import org.interledger.everledger.ledger.LedgerAccountManagerFactory;
import org.interledger.everledger.ledger.account.LedgerAccount;
import org.interledger.everledger.ledger.account.LedgerAccountManager;
import org.interledger.everledger.ledger.impl.simple.SimpleLedgerAccount;

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

    LedgerAccount alice = new SimpleLedgerAccount("alice");
    LedgerAccount bob = new SimpleLedgerAccount("bob");
    

    @BeforeClass
    public static void init() throws Exception {
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
        LedgerAccount result = instance.create(alice.getName());
        System.out.println("result:" + result);
        assertNotNull(result);
        assertEquals(alice, result);
        assertEquals("Balance",0d, result.getBalance().getNumber().doubleValue(),0d);
    }

    /**
     * Test of addAccounts method, of class SimpleLedgerAccountManager.
     */
    @Test
    public void testAddAccounts() {
        System.out.println("addAccounts");
        assertEquals(0, instance.getTotalAccounts());
        instance.store(new SimpleLedgerAccount("alice"));
        instance.store(new SimpleLedgerAccount("bob"));
        assertEquals(2, instance.getTotalAccounts());        
    }

    /**
     * Test of store method, of class SimpleLedgerAccountManager.
     */
    @Test
    public void testAddAccount() {
        System.out.println("addAccount");
        assertEquals(0, instance.getTotalAccounts());
        instance.store(new SimpleLedgerAccount("alice"));
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
