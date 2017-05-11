package org.interledger.everledger.util;

public class StringUtils {
    public static boolean isBlank(String s) {
        return s == null || s.trim().equals("");
    }
}
