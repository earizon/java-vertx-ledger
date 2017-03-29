package org.interledger.ilp.ledger.impl.simple;

import javax.money.MonetaryAmount;

import org.interledger.ilp.common.config.Config;
import org.interledger.ilp.core.AccountURI;
import org.interledger.ilp.core.ledger.model.LedgerInfo;
import org.interledger.ilp.ledger.Currencies;
import org.interledger.ilp.ledger.LedgerFactory;
import org.interledger.ilp.ledger.LedgerInfoBuilder;
import static org.interledger.ilp.ledger.impl.simple.SimpleLedgerAccountManagerTest.URI_LEDGER_A;
import org.javamoney.moneta.Money;
import static org.junit.Assert.*;

import java.net.URL;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * SimpleLedgerAccount tests
 *
 * @author mrmx
 */
public class SimpleLedgerAccountTest {

    static final String CURRENCY_CODE = "EUR";
    SimpleLedgerAccount instance;
    final String sTestURI = "http://ledgerTest";
    final String sOtherURI = "http://ledgerOther";
    AccountURI testURI = new AccountURI(sTestURI, "test");
    AccountURI otherURI = new AccountURI(sOtherURI, "others");

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
        instance = new SimpleLedgerAccount("test", CURRENCY_CODE);
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
    public void testCredit_String() {
        System.out.println("credit string");
        String amount = "1234567890123";
        MonetaryAmount expResult = Money.of(Double.parseDouble(amount), CURRENCY_CODE);
        MonetaryAmount result = instance.credit(amount).getBalance();
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
     * Test of credit method, of class SimpleLedgerAccount.
     */
    @Test
    public void testCredit_MonetaryAmount() {
        System.out.println("credit");
        MonetaryAmount amount = Money.of(123, CURRENCY_CODE);
        MonetaryAmount expResult = Money.of(123, CURRENCY_CODE);
        MonetaryAmount result = instance.credit(amount).getBalance();
        assertEquals(expResult, result);
    }

    /**
     * Test of debit method, of class SimpleLedgerAccount.
     */
    @Test
    public void testDebit_String() {
        System.out.println("debit String");
        instance.setBalance(100);
        String amount = "50";
        MonetaryAmount expResult = Money.of(Double.valueOf(amount), CURRENCY_CODE);
        MonetaryAmount result = instance.debit(amount).getBalance();
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
        SimpleLedgerAccount other = new SimpleLedgerAccount(sOtherURI, CURRENCY_CODE);
        assertNotEquals(instance, other);
        assertNotEquals(instance, null);
        assertEquals(instance, instance);
    }

}
