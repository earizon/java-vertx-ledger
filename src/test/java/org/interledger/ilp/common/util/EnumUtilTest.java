package org.interledger.ilp.common.util;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

/**
 * {@code EnumUtil} tests.
 *
 * @author mrmx
 */
@SuppressWarnings("rawtypes")
public class EnumUtilTest {

    enum MyEnum {
        value1, value2, UPPER_CASE_VALUE3
    }

    EnumUtil instance;

    @Before
    public void setUp() {
        instance = new EnumUtil(MyEnum.class);
    }

    /**
     * Test of getEnumValue method, of class EnumUtil.
     */
    @Test
    public void testGetEnumValueCaseSensitive() {
        System.out.println("testGetEnumValueCaseSensitive");
        Enum expResult = MyEnum.value1;
        Enum result = EnumUtil.getEnumValue(MyEnum.class, "value1", false);
        assertEquals(expResult, result);
    }

    /**
     * Test of getEnumValue method, of class EnumUtil.
     */
    @Test
    public void testGetEnumValueCaseInSensitive() {
        System.out.println("testGetEnumValueCaseSensitive");
        boolean ignoreCase = true;
        {
            Enum expResult = MyEnum.value1;
            Enum result = EnumUtil.getEnumValue(MyEnum.class, "ValUe1", ignoreCase);
            assertEquals(expResult, result);
        }
        {
            Enum expResult = MyEnum.UPPER_CASE_VALUE3;
            Enum result = EnumUtil.getEnumValue(MyEnum.class, "upper_CASE_value3", ignoreCase);
            assertEquals(expResult, result);
        }

    }

    /**
     * Test of getValue method, of class EnumUtil.
     */
    @Test
    public void testGetValueCaseSensitive() {
        System.out.println("testGetValueCaseSensitive");
        boolean ignoreCase = false;
        {
            String enumKeyname = "value2";
            Enum expResult = MyEnum.value2;
            Enum result = instance.getValue(enumKeyname, ignoreCase);
            assertEquals(expResult, result);
        }
        {
            String enumKeyname = "VALue2";
            Enum expResult = null;
            Enum result = instance.getValue(enumKeyname, ignoreCase);
            assertEquals(expResult, result);
        }

    }

    /**
     * Test of getValue method, of class EnumUtil.
     */
    @Test
    public void testGetValueCaseINSensitive() {
        System.out.println("testGetValueCaseInSensitive");
        boolean ignoreCase = true;
        String enumKeyname = "VALue2";
        Enum expResult = MyEnum.value2;
        Enum result = instance.getValue(enumKeyname, ignoreCase);
        assertEquals(expResult, result);
    }

}
