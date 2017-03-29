package org.interledger.ilp.common.config.core;

import java.util.Arrays;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 * {@code ObjectConfigKey} tests.
 *
 * @author mrmx
 */
public class ObjectConfigKeyTest {

    /**
     * Test of of method, of class ObjectConfigKey.
     */
    @Test(expected = IllegalArgumentException.class)
    public void testOfNullKey() {
        System.out.println("testOfNullKey");
        ObjectConfigKey.of(null);
    }

    /**
     * Test of of method, of class ObjectConfigKey.
     */
    @Test
    public void testOfStringKey() {
        System.out.println("testOfStringKey");
        Object key = "string_key";
        ConfigKey result = ObjectConfigKey.of(key);
        assertNotNull(result);
        assertEquals(key, result.getPath());
    }

    /**
     * Test of of method, of class ObjectConfigKey.
     */
    @Test
    public void testOfStringArrayKey() {
        System.out.println("testOfStringArrayKey");
        Object[] key = {"a", "string", "key"};
        ConfigKey result = ObjectConfigKey.of(key);
        assertNotNull(result);
        assertEquals("a.string.key", result.getPath());
    }

    /**
     * Test of of method, of class ObjectConfigKey.
     */
    @Test
    public void testOfIterableKey() {
        System.out.println("testOfIterableKey");
        Object key = Arrays.asList("a", "string", "key");
        ConfigKey result = ObjectConfigKey.of(key);
        assertNotNull(result);
        assertEquals("a.string.key", result.getPath());
    }

}
