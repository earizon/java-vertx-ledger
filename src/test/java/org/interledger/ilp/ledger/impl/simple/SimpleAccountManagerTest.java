package org.interledger.ilp.ledger.impl.simple;

import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.everis.everledger.AccountManagerFactory;
import com.everis.everledger.ifaces.account.IfaceAccount;
import com.everis.everledger.ifaces.account.IfaceAccountManager;
import com.everis.everledger.ifaces.account.IfaceLocalAccount;
import com.everis.everledger.impl.SimpleAccount;

/**
 *
 * @author mrmx
 */
public class SimpleAccountManagerTest {

    IfaceAccountManager instance;
    
    final static String URI_LEDGER_A = "ledger1.example";
    final static String URI_LEDGER_B = "ledger2.example";

    IfaceLocalAccount alice = new SimpleAccount("alice");
    IfaceLocalAccount bob = new SimpleAccount("bob");
    

    @BeforeClass
    public static void init() throws Exception {
    }
    
    @Before
    public void setUp() {        
        instance = AccountManagerFactory.createLedgerAccountManager();
    }

    /**
     * Test of create method, of class SimpleLedgerAccountManager.
     */
    @Test
    public void testCreate() {
        System.out.println("create");
        IfaceAccount result = new SimpleAccount(alice.getLocalID());
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
        assertEquals(1, instance.getTotalAccounts());
        instance.store(new SimpleAccount("alice"));
        instance.store(new SimpleAccount("bob"));
        assertEquals(3, instance.getTotalAccounts());        
    }

    /**
     * Test of store method, of class SimpleLedgerAccountManager.
     */
    @Test
    public void testAddAccount() {
        System.out.println("addAccount");
        assertEquals(1, instance.getTotalAccounts());
        instance.store(new SimpleAccount("alice"));
        assertEquals(2, instance.getTotalAccounts());
    }

    /**
     * Test of getAccountByName method, of class SimpleLedgerAccountManager.
     */
    @Test
    public void testGetAccountByName() {
        System.out.println("getAccountByName");
        IfaceAccount bob = new SimpleAccount("bob");
        instance.store(bob);
        instance.store(new SimpleAccount("alice"));
        assertEquals(2+1, instance.getTotalAccounts());
        IfaceLocalAccount result = instance.getAccountByName("bob");
        assertEquals(bob, result);
    }

    /**
     * Test of getAccounts method, of class SimpleLedgerAccountManager.
     */
    @Test
    public void testGetAccounts() {
        System.out.println("testGetAccounts");
        IfaceAccount bob = new SimpleAccount("bob");
        instance.store(bob);
        instance.store(new SimpleAccount("alice"));
        assertEquals(2+1, instance.getTotalAccounts());
        Collection<IfaceLocalAccount> result = instance.getAccounts(1, 1);
        System.out.println("result:" + result);
        assertEquals(2+1, result.size());        
    }

}
