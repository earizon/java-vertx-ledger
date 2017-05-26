package com.everis.everledger.util;

import java.util.Base64;

import org.apache.commons.lang3.math.NumberUtils;
import org.interledger.cryptoconditions.Fulfillment;

/**
 * Various number conversion utility methods
 * 
 * @author mrmx
 */
public class ConversionUtil {
    
    public static Number toNumber(Object value) {
        if (value == null) throw new RuntimeException("value can't be null");
        return toNumber(value, 0);
    }
    
    public static Number toNumber(Object value,Number defaultValue) {
        if (value == null) throw new RuntimeException("value can't be null");
        if (defaultValue == null) throw new RuntimeException("defaultValue can't be null");
        if(value instanceof Number) { return (Number) value; }
        return NumberUtils.createNumber(value.toString());
    }
    
    public static String toString(Number value) {
        return value.toString();
    }
    
    public static String fulfillmentToBase64(Fulfillment FF){
        String response  = Base64.getEncoder().
                encodeToString(FF.getEncoded());
        response = response.substring(0, response.indexOf('='));
        return response;
    }
}
