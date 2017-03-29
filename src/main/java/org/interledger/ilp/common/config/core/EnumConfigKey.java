package org.interledger.ilp.common.config.core;

/**
 * Defines a {@code ConfigKey} using enums.
 *
 * @author mrmx
 */
public class EnumConfigKey extends ArrayConfigKey<Enum<?>> {

    public EnumConfigKey(Enum<?>... keys) {
        super(keys);
    }

    public static EnumConfigKey of(Enum<?>... keys) {
        return new EnumConfigKey(keys);
    }
}
