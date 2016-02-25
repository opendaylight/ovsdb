/*
 * Copyright © 2016 Red Hat, Inc. and others.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.utils.yang;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.google.common.base.Preconditions;

/**
 * YANG utility functions.
 */
public final class YangUtils {
    /**
     * Prevent instantiation.
     */
    private YangUtils() {
        // Nothing to do
    }

    /**
     * Copies a list of YANG key-value items to the given map. Any {@code null} key or value will cause an error.
     *
     * @param map The map to fill.
     * @param yangList The list of YANG key-value items.
     * @param keyExtractor The key extractor function to use.
     * @param valueExtractor The value extractor function to use.
     * @param <T> The YANG item type.
     * @param <K> The key type.
     * @param <V> The value type.
     * @return The map.
     */
    @Nonnull
    public static <T, K, V> Map<K, V> copyYangKeyValueListToMap(@Nonnull Map<K, V> map, @Nullable Iterable<T> yangList,
                                                                @Nonnull Function<T, K> keyExtractor,
                                                                @Nonnull Function<T, V> valueExtractor) {
        if (yangList != null) {
            for (T yangValue : yangList) {
                K key = keyExtractor.apply(yangValue);
                V value = valueExtractor.apply(yangValue);
                Preconditions.checkNotNull(key);
                Preconditions.checkNotNull(value);
                map.put(key, value);
            }
        }
        return map;
    }

    /**
     * Converts a list of YANG key-value items to a map.
     *
     * @param yangList The list of YANG key-value items.
     * @param keyExtractor The key extractor function to use.
     * @param valueExtractor The value extractor function to use.
     * @param <T> The YANG item type.
     * @param <K> The key type.
     * @param <V> The value type.
     * @return The map.
     */
    @Nonnull
    public static <T, K, V> Map<K, V> convertYangKeyValueListToMap(@Nullable Iterable<T> yangList,
                                                                   @Nonnull Function<T, K> keyExtractor,
                                                                   @Nonnull Function<T, V> valueExtractor) {
        return copyYangKeyValueListToMap(new HashMap<>(), yangList, keyExtractor, valueExtractor);
    }
}
