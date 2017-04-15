package org.interledger.ilp.common.util;

import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.EnumUtils;

/**
 * Enum util methods
 *
 * @author mrmx
 */
@SuppressWarnings("rawtypes")
public class EnumUtil<T extends Enum<T>> {

    private final static Map<Class, EnumUtil> enumUtilMap = new HashMap<Class, EnumUtil>();
    private final Class<T> enumType;
    private T[] enumKeys;

    public EnumUtil(Class<T> enumType) {
        this.enumType = enumType;
        enumKeys = enumType.getEnumConstants();
    }

    public static <T extends Enum<T>> T getEnumValue(Class<T> enumType,
            String enumKeyname, boolean ignoreCase) {
        EnumUtil<T> enumUtil = enumUtilMap.get(enumType);
        if (enumUtil == null) {
            enumUtilMap.put(enumType, enumUtil = new EnumUtil<T>(enumType));
        }
        return enumUtil.getValue(enumKeyname, ignoreCase);
    }

    public T getValue(String enumKeyname, boolean ignoreCase) {
        T value = EnumUtils.getEnum(enumType, enumKeyname);
        if (value == null) {
            if (ignoreCase) {
                enumKeyname = enumKeyname.toLowerCase();
            }
            String checkEnumKey;
            for (T enumKey : enumKeys) {
                checkEnumKey = enumKey.name();
                if (ignoreCase) {
                    checkEnumKey = checkEnumKey.toLowerCase();
                }
                if (checkEnumKey.equals(enumKeyname)) {
                    value = enumKey;
                    break;
                }
            }
        }
        return value;
    }

}
