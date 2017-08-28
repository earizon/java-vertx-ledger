package org.interledger.ilp.common.api.util;

import io.vertx.core.json.JsonObject;
import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import com.everis.everledger.util.JsonObjectBuilder;

/**
 * {@code JsonObjectBuilder} tests
 *
 * @author mrmx
 */
public class JsonObjectBuilderTest {

////    JsonObjectBuilder instance;
////
////    private static class MyBean {
////
////        String string;
////
////        @SuppressWarnings("unused")
////        public String getString() {
////            return string;
////        }
////
////        public void setString(String string) {
////            this.string = string;
////        }
////
////    }
////
////    @Before
////    public void setUp() {
////        instance = JsonObjectBuilder.create();
////    }
////
////    /**
////     * Test of get method, of class JsonObjectBuilder.
////     */
////    @Test
////    public void testGet() {
////        System.out.println("testGet");
////        JsonObject expResult = new JsonObject("{ \"string\": \"value\"}");
////        JsonObject result = instance.put("string", "value").get();
////        assertEquals(expResult, result);
////    }
////
////    /**
////     * Test of from method, of class JsonObjectBuilder.
////     */
////    @Test
////    public void testFrom() {
////        System.out.println("testFrom");
////        MyBean value = new MyBean();
////        value.setString("value");
////        JsonObject expResult = new JsonObject("{ \"string\": \"value\"}");
////        JsonObject result = instance.from(value).get();
////        assertEquals(expResult, result);
////    }
////
////    /**
////     * Test of with method, of class JsonObjectBuilder.
////     */
////    @Test
////    public void testWith() {
////        System.out.println("testWith");
////        JsonObject expResult = new JsonObject()
////                .put("key1", "value")
////                .put("key2", true);
////        JsonObject result = instance.with(
////                "key1", "value",
////                "key2", true
////        ).get();
////        assertEquals(expResult, result);
////    }
////
////    /**
////     * Test of from and width methods, of class JsonObjectBuilder.
////     */
////    @Test
////    public void testFromWith() {
////        System.out.println("testFromWith");
////        MyBean value = new MyBean();
////        value.setString("value");
////        String expJson = "{ \"string\": \"value\",\"field2\":123}";
////        JsonObject expResult = new JsonObject(expJson);
////        JsonObject result = instance.from(value).with("field2", 123L).get();
////        assertEquals(expResult, result);
////    }
////
////        /**
////     * Test of put method, of class JsonObjectBuilder.
////     */
////    @Test
////    public void testPut() {
////        System.out.println("testPut");
////        JsonObject expResult = new JsonObject().put("string", "value");
////        JsonObject result = instance.put("string", "value").get();
////        assertEquals(expResult, result);
////    }

}
