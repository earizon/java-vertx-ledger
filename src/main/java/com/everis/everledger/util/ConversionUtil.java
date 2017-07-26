package com.everis.everledger.util;

import java.net.URI;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.math.NumberUtils;
import org.interledger.Condition;
import org.interledger.Fulfillment;

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
                encodeToString(FF.getPreimage());
        response = response.substring(0, response.indexOf('='));
        return response;
    }
    
    /**
     * Parses a URI formatted crypto-condition
     *
     * @param uri
     *  The crypto-condition formatted as a uri.
     * @return
     *  The crypto condition
     */
    public static Condition parseURI(final URI uri) {
      //based strongly on the five bells implementation at 
      //https://github.com/interledgerjs/five-bells-condition (7b6a97990cd3a51ee41b276c290e4ae65feb7882)
      
      if (!"ni".equals(uri.getScheme())) {
        throw new RuntimeException("Serialized condition must start with 'ni:'");
      }
      
      final String CONDITION_REGEX_STRICT = "^ni://([A-Za-z0-9_-]?)/sha-256;([a-zA-Z0-9_-]{0,86})\\?(.+)$";

      //the regex covers the entire uri format including the 'ni:' scheme
      final Matcher m = Pattern.compile(CONDITION_REGEX_STRICT).matcher(uri.toString());
      
      if (!m.matches()) {
        throw new RuntimeException("Invalid condition format");
      }

      byte[] fingerprint = Base64.getUrlDecoder().decode(m.group(2));
      return new Condition(fingerprint);
    }

}
