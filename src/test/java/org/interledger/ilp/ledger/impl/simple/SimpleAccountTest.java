package org.interledger.ilp.ledger.impl.simple;

import javax.money.MonetaryAmount;

import org.javamoney.moneta.Money;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.everis.everledger.ifaces.account.IfaceLocalAccount;
import com.everis.everledger.impl.SimpleAccount;

/**
 * SimpleLedgerAccount tests
 *
 */
public class SimpleAccountTest {

    static final String CURRENCY_CODE = "EUR";
    SimpleAccount instance;
    final String sTestURI = "http://ledgerTest";
    final String sOtherURI = "http://ledgerOther";
    IfaceLocalAccount testURI = new SimpleAccount(sTestURI);
    IfaceLocalAccount otherURI = new SimpleAccount(sOtherURI);

    @BeforeClass
    public static void init() throws Exception {
    }

    @Before
    public void setUp() {
        instance = new SimpleAccount("test");
    }

    /**
     * Test of getAccountUri method, of class SimpleLedgerAccount.
     */
    @Test
    public void testGetName() {
        System.out.println("getName");
        assertEquals("test", instance.getName());
    }

    /**
     * Test of setBalance method, of class SimpleLedgerAccount.
     */
    @Test
    public void testSetBalance_Number() {
        System.out.println("setBalance");
        Number balance = 123f;
        MonetaryAmount expResult = Money.of(balance, CURRENCY_CODE);
        MonetaryAmount result = instance.setBalance(balance).getBalance();
        assertEquals(expResult, result);
    }

    /**
     * Test of setBalance method, of class SimpleLedgerAccount.
     */
    @Test
    public void testSetBalance_MonetaryAmount() {
        System.out.println("setBalance");
        MonetaryAmount balance = Money.of(1, CURRENCY_CODE);
        MonetaryAmount expResult = Money.of(1, CURRENCY_CODE);
        MonetaryAmount result = instance.setBalance(balance).getBalance();
        assertEquals(expResult, result);
    }

    /**
     * Test of getBalance method, of class SimpleLedgerAccount.
     */
    @Test
    public void testGetBalance() {
        System.out.println("getBalance");
        MonetaryAmount expResult = Money.of(0, CURRENCY_CODE);
        MonetaryAmount result = instance.getBalance();
        assertEquals(expResult, result);
    }

    /**
     * Test of credit method, of class SimpleLedgerAccount.
     */
    @Test
    public void testCredit_Number() {
        System.out.println("credit");
        Number amount = 123;
        MonetaryAmount expResult = Money.of(amount, CURRENCY_CODE);
        MonetaryAmount result = instance.credit(amount).getBalance();
        assertEquals(expResult, result);
    }

    /**
     * Test of debit method, of class SimpleLedgerAccount.
     */
    @Test
    public void testDebit_Number() {
        System.out.println("debit");
        instance.setBalance(100);
        Number amount = 50;
        MonetaryAmount expResult = Money.of(amount, CURRENCY_CODE);
        MonetaryAmount result = instance.debit(amount).getBalance();
        assertEquals(expResult, result);
    }

    /**
     * Test of debit method, of class SimpleLedgerAccount.
     */
    @Test
    public void testDebit_MonetaryAmount() {
        System.out.println("debit");
        instance.setBalance(100);
        MonetaryAmount amount = Money.of(25, CURRENCY_CODE);
        MonetaryAmount expResult = Money.of(75, CURRENCY_CODE);
        MonetaryAmount result = instance.debit(amount).getBalance();
        assertEquals(expResult, result);
    }

    /**
     * Test of equals method, of class SimpleLedgerAccount.
     */
    @Test
    public void testEquals() {
        System.out.println("equals");
        SimpleAccount other = new SimpleAccount(sOtherURI);
        assertNotEquals(instance, other);
        assertNotEquals(instance, null);
        assertEquals(instance, instance);
    }

}
