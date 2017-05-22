package org.interledger.everledger.util;

import org.apache.commons.lang3.math.NumberUtils;

/**
 * Various number conversion utility methods
 * 
 * @author mrmx
 */
public class NumberConversionUtil {
    public static final String STRING_NEGATIVE_INFINITY = "-infinity";
    
    public static Number toNumber(Object value) {
        if (value == null) throw new RuntimeException("value can't be null");
        return toNumber(value, 0);
    }
    
    public static Number toNumber(Object value,Number defaultValue) {
        if (value == null) throw new RuntimeException("value can't be null");
        if (defaultValue == null) throw new RuntimeException("defaultValue can't be null");

        if(value instanceof Number) { return (Number) value; }
        if(STRING_NEGATIVE_INFINITY.equals(value)) {
            //No string value as JS 5bells reference so a number contract:
            return Double.MIN_VALUE;
        }
        return NumberUtils.createNumber(value.toString());
    }
    
    public static String toString(Number value) {
        if(Double.MIN_VALUE == value.doubleValue()) {
            return STRING_NEGATIVE_INFINITY;
        }
        return value.toString();
    }
}
