package org.interledger.ilp.ledger.impl.simple;

import java.util.Collection;

import org.interledger.everledger.LedgerAccountManagerFactory;
import org.interledger.everledger.ledger.account.IfaceAccount;
import org.interledger.everledger.ledger.account.IfaceAccountManager;
import org.interledger.everledger.ledger.account.IfaceLocalAccount;
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

    IfaceAccountManager instance;
    
    final static String URI_LEDGER_A = "ledger1.example";
    final static String URI_LEDGER_B = "ledger2.example";

    IfaceLocalAccount alice = new SimpleLedgerAccount("alice");
    IfaceLocalAccount bob = new SimpleLedgerAccount("bob");
    

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
        IfaceAccount result = instance.create(alice.getLocalName());
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
        IfaceAccount bob = instance.create("bob");
        instance.store(bob);
        instance.store(instance.create("alice"));
        assertEquals(2, instance.getTotalAccounts());
        IfaceLocalAccount result = instance.getAccountByName("bob");
        assertEquals(bob, result);
    }

    /**
     * Test of getAccounts method, of class SimpleLedgerAccountManager.
     */
    @Test
    public void testGetAccounts() {
        System.out.println("testGetAccounts");
        IfaceAccount bob = instance.create("bob");
        instance.store(bob);
        instance.store(instance.create("alice"));
        assertEquals(2, instance.getTotalAccounts());
        Collection<IfaceLocalAccount> result = instance.getAccounts(1, 1);
        System.out.println("result:" + result);
        assertEquals(2, result.size());        
    }

}
