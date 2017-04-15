package org.interledger.ilp.common.config.core;

/**
 * ConfigKey wrapper for any {@code Class} instance.
 *
 * @author mrmx
 */
public class ClassConfigKey extends AbstractConfigKey {

    private Class<?> key;
    private boolean useFQN;

    private ClassConfigKey(Class<?> key, boolean useFQN) {
        this.key = key;
        this.useFQN = useFQN;
    }
    
    public static ConfigKey of(Class<?> key) {
        return of(key,false);
    }    

    public static ConfigKey of(Class<?> key,boolean useFQN) {
        if (key == null) {
            throw new IllegalArgumentException("null key");
        }
        return new ClassConfigKey(key,useFQN);
    }

    @Override
    protected void buildKeyPath() {
        String path = useFQN ? key.getCanonicalName() : key.getSimpleName();
        if (path == null) {
            path = key.getSimpleName();
        }
        append(path);
    }

}
