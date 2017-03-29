package org.interledger.ilp.common.config.core;

import static org.junit.Assert.*;
import org.junit.Test;

/**
 * {@code EnumConfigKey} test
 *
 * @author mrmx
 */
public class EnumConfigKeyTest {

    enum Key {
        a, b, c
    }

    /**
     * Test of of method, of class EnumConfigKey.
     */
    @Test
    public void testOf() {
        System.out.println("testOf");
        String expResult = "a.b.c";
        EnumConfigKey result = EnumConfigKey.of(Key.values());
        assertEquals(expResult, result.getPath());
    }

}
