package org.interledger.everledger.common.util;

public class StringUtils {
    public static boolean isBlank(String s) {
        return s == null || s.trim().equals("");
    }
}
