package org.interledger.ilp.common.config.core;

import org.apache.commons.lang3.StringUtils;

/**
 * Defines a {@code ConfigKey} compounded of two {@code ConfigKey} instances.
 * The final path is composed as the concatenation of the parts.
 *
 * @author mrmx
 */
public class CompoundConfigKey extends AbstractConfigKey {

    private final ConfigKey a, b;

    public CompoundConfigKey(ConfigKey a, ConfigKey b) {
        this.a = a;
        this.b = b;
    }

    public static CompoundConfigKey of(Object a, ConfigKey b) {
        return of(ObjectConfigKey.of(a), b);
    }

    public static CompoundConfigKey of(ConfigKey a, ConfigKey b) {
        return new CompoundConfigKey(a, b);
    }

    @Override
    protected void buildKeyPath() {
        String prefix = a.getPath();
        if (StringUtils.isNotBlank(prefix)) {
            append(prefix);
        }
        String subfix = b.getPath();
        if (StringUtils.isNotBlank(subfix)) {
            append(subfix);
        }
    }

}
