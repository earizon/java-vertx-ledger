package org.interledger.ilp.common.config.core;

/**
 * Defines a {@code ConfigKey} using an array of {@code T} keys.
 *
 * @author mrmx
 */
@SuppressWarnings("rawtypes")
public class ArrayConfigKey<T> extends AbstractConfigKey {

    private final T[] keys;

    public ArrayConfigKey(T... keys) {
        this.keys = keys;
    }

    public static <T> ArrayConfigKey of(T... keys) {
        return new ArrayConfigKey(keys);
    }

    @Override
    protected void buildKeyPath() {
        for (T key : keys) {
            append(key);
        }
    }
}
