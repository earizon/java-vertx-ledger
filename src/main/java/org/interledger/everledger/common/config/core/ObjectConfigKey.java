package org.interledger.ilp.common.config.core;

/**
 * ConfigKey wrapper for any {@code Object} instance.
 *
 * @author mrmx
 */
@SuppressWarnings("rawtypes")
public class ObjectConfigKey extends AbstractConfigKey {

    private final Object key;

    private ObjectConfigKey(Object key) {
        this.key = key;
    }

    public static ConfigKey of(Object key) {
        if (key == null) {
            throw new IllegalArgumentException("null key");
        }
        if (ConfigKey.class.isAssignableFrom(key.getClass())) {
            return (ConfigKey) key;
        }
        return new ObjectConfigKey(key);
    }

    @Override
    protected void buildKeyPath() {
        if (Object[].class.isAssignableFrom(key.getClass())) {
            for (Object keyPart : ((Object[]) key)) {
                append(keyPart);
            }
        } else if (Iterable.class.isAssignableFrom(key.getClass())) {
            for (Object keyPart : ((Iterable) key)) {
                append(keyPart);
            }
        } else {
            append(key);
        }
    }

}
