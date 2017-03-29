package org.interledger.ilp.common.config;

import com.google.common.base.Optional;
import java.util.Arrays;
import org.interledger.ilp.common.config.core.ConfigurationException;
import org.interledger.ilp.common.config.core.RequiredKeyException;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

/**
 * Config tests
 *
 * @author mrmx
 */
public class ConfigTest {

    Config config;

    enum Key {
        KEY,
        NOT_FOUND,
        string, string_list, string_list2,
        integer,
        Config, lowercasekeys, override,
        CONNECTOR, HOST, Port, //Any case support per default
        EnumContext, EnumContextUppercase
    }

    enum MyEnum {
        value1, value2, VALUE_UPPERCASE
    }
    
    enum EnumContext {
        string
    }
    
    enum EnumNotPresentContext {
        
    }

    @Before
    public void setUp() {
        config = Config.create();
    }

    @Test
    public void testHasKeyEnum() {
        System.out.println("testHasKeyEnum");
        assertFalse(config.hasKey(Key.NOT_FOUND));
        assertTrue(config.hasKey(Key.string));
    }

    @Test
    public void testHasKeyString() {
        System.out.println("testHasKeyString");
        assertFalse(config.hasKey("key","not","found"));
        assertTrue(config.hasKey("key","string"));
    }
    
    @Test(expected = RequiredKeyException.class)
    public void testRequireKeyWithNoKey() {
        System.out.println("testRequireKeyWithNoKey");
        config.requireKey(Key.NOT_FOUND);
    }

    @Test
    public void testRequireKeyWithKey() {
        System.out.println("testRequireKeyWithKey");
        config.requireKey(Key.string);
    }

    @Test
    public void testGetEnumWithNoKeyContext() {
        System.out.println("testGetEnumWithNoKeyContext");
        assertEquals("Enum with no context", MyEnum.value1, config.getEnum(MyEnum.class));
    }

    @Test
    public void testGetEnumWithKeyContext() {
        System.out.println("testGetEnumWithKeyContext");
        assertEquals("Enum with context", MyEnum.value2, config.getEnum(MyEnum.class, Key.EnumContext));
    }

    @Test(expected = ConfigurationException.class)
    public void testGetEnumShouldThrowConfigurationExceptionIfNotValueFound() {
        System.out.println("testGetEnumShouldThrowRequiredKeyExceptionIfNotValueFound");
        //Notice that key is defined (key.string) in application.conf
        config.getEnum(Key.class);
    }

    @Test(expected = RequiredKeyException.class)
    public void testGetEnumShouldThrowRequiredKeyExceptionIfNotValueFound() {
        System.out.println("testGetEnumShouldThrowRequiredKeyExceptionIfNotValueFound");
        //The resulting key is: KEY.MyEnum => not found
        config.getEnum(MyEnum.class, Key.KEY);
    }

    @Test
    public void testGetEnumWithKeyContextAndUppercaseValue() {
        System.out.println("testGetEnumWithKeyContextAndUppercaseValue");
        assertEquals("Enum with uppercase value", MyEnum.VALUE_UPPERCASE, config.getEnum(MyEnum.class, Key.EnumContextUppercase));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetStringDefaultWithNoKey() {
        System.out.println("testGetStringDefaultWithNoKey");
        config.getString("default");
    }

    @Test(expected = RequiredKeyException.class)
    public void testGetStringForWithNoKey() {
        System.out.println("testGetStringForWithNoKey");
        config.getStringFor("no key");
    }

    @Test
    public void testGetStringForWithADefinedKey() {
        System.out.println("testGetStringForWithADefinedKey");
        assertEquals("a string value", config.getStringFor("string"));
    }

    @Test(expected = RequiredKeyException.class)
    public void testGetStringShouldThrowRequiredKeyExceptionIfNotValueFound() {
        System.out.println("testGetStringShouldThrowRequiredKeyExceptionIfNotValueFound");
        config.getString(Key.NOT_FOUND);
    }

    @Test
    public void testGetStringWithADefinedKey() {
        System.out.println("testGetStringWithADefinedKey");
        assertNotNull("should be not null Key." + Key.string, config.getString(Key.string));
    }

    @Test(expected = RequiredKeyException.class)
    public void testGetStringListShouldThrowRequiredKeyExceptionIfNotValueFound() {
        System.out.println("testGetStringListShouldThrowRequiredKeyExceptionIfNotValueFound");
        config.getStringList(Key.NOT_FOUND);
    }

    @Test
    public void testGetStringListWithADefinedKey() {
        System.out.println("testGetStringListWithADefinedKey");
        assertEquals("String list", Arrays.asList("a", "string", "list"), config.getStringList(Key.string_list));
    }

    @Test
    public void testGetStringListWithAnEmptyValue() {
        System.out.println("testGetStringListWithAnEmptyValue");
        assertEquals("Empty string list", Arrays.asList(), config.getStringList(Key.string_list2));
    }

    @Test
    public void testGetStringWithADefinedKeyButIntValue() {
        System.out.println("testGetStringWithADefinedKeyButIntValue");
        assertEquals("should be Key." + Key.integer, "1234", config.getString(Key.integer));
    }

    @Test
    public void testGetStringWithHOCON() {
        System.out.println("testGetStringWithHOCON");
        String value = config.getString(Key.CONNECTOR, Key.HOST);
        assertEquals("connector.host", "localhost", value);
    }

    //Fixme
    //@Test
    public void testGetStringWithJavaSystemOverrides() {
        System.out.println("testGetStringWithEnvOverrides");
        {
            String value = config.getString(Key.CONNECTOR, Key.HOST);
            assertEquals("connector.host", "localhost", value);
        }
        //Override:        
        System.setProperty("connector.host", "127.0.0.1");
        config = Config.create();
        {
            String value = config.getString(Key.CONNECTOR, Key.HOST);
            assertEquals("connector.host", "127.0.0.1", value);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGetIntDefaultWithNoKey() {
        System.out.println("testGetIntDefaultWithNoKey");
        config.getInt(1234);
    }

    @Test(expected = RequiredKeyException.class)
    public void testGetRequiredIntWithNoKey() {
        System.out.println("testGetRequiredIntWithNoKey");
        config.getInt(Key.NOT_FOUND);
    }

    @Test
    public void testGetIntWithADefinedKey() {
        System.out.println("testGetIntWithADefinedKey");
        assertEquals("should be positive", 31415, config.getInt(Key.CONNECTOR, Key.Port));
    }

    @Test(expected = ConfigurationException.class)
    public void testGetIntShouldThrowConfigurationExceptionIfValueFoundCanNotBeConverted() {
        System.out.println("testGetIntShouldThrowConfigurationExceptionIfValueFoundCanNotBeConverted");
        config.getInt(Key.CONNECTOR, Key.HOST);
    }

    @Test
    public void testGetBooleanWithADefinedKey() {
        System.out.println("testGetBooleanWithADefinedKey");
        assertTrue("should be true", config.getBoolean(Key.Config, Key.override));
    }

    @Test(expected = RequiredKeyException.class)
    public void testGetBooleanWithAnUndefinedKey() {
        System.out.println("testGetBooleanWithAnUndefinedKey");
        config.getBoolean(Key.Config, Key.string);
    }

    @Test
    public void testGetConfig() {
        System.out.println("testGetConfig");
        Config child = config.getConfig(Key.KEY);
        assertEquals("key." + Key.string, "a string value in a compound path", child.getString(Key.string));
    }

    @Test(expected = ConfigurationException.class)
    public void testGetConfigWithFullPath() {
        System.out.println("testGetConfigWithFullPath");
        config.getConfig(Key.KEY, Key.string);
    }

    @Test(expected = RequiredKeyException.class)
    public void testGetConfigWithPathNotFoundShouldThrowRequiredKeyException() {
        System.out.println("testGetConfigWithPathNotFoundShouldThrowRequiredKeyException");
        config.getConfig(Key.NOT_FOUND);
    }
    
   @Test
    public void testGetConfigWithEnumClassContext() {
        System.out.println("testGetConfigWithEnumClassContext");
        Config child = config.getConfig(EnumContext.class);
        assertEquals(MyEnum.value2.name(), MyEnum.value2, child.getEnum(MyEnum.class));
        assertEquals(EnumContext.string.name(), "in context!", child.getString(EnumContext.string));
    }
    
    @Test
    public void testGetOptionalPresentConfigWithEnumClassContext() {
        System.out.println("testGetOptionalPresentConfigWithEnumClassContext");
        Optional<Config> child = config.getOptionalConfig(EnumContext.class);
        assertTrue("Optional config present",child.isPresent());
        assertEquals(MyEnum.value2.name(), MyEnum.value2, child.get().getEnum(MyEnum.class));
        assertEquals(EnumContext.string.name(), "in context!", child.get().getString(EnumContext.string));
    }
    
    @Test
    public void testGetOptionalNotPresentConfigWithEnumClassContext() {
        System.out.println("testGetOptionalNotPresentConfigWithEnumClassContext");
        Optional<Config> child = config.getOptionalConfig(EnumNotPresentContext.class);
        assertFalse("Optional config not present",child.isPresent());
    }

}
