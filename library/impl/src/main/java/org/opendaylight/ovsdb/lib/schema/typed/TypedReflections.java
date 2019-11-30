/*
 * Copyright © 2019 PANTHEON.tech, s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.lib.schema.typed;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Range;
import java.lang.reflect.Method;
import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.ovsdb.lib.notation.Version;

/**
 * Utilities for extracting annotation information at runtime.
 */
public final class TypedReflections {
    private static final LoadingCache<Method, Range<Version>> COLUMN_VERSIONS = CacheBuilder.newBuilder().weakKeys()
            .build(new CacheLoader<Method, Range<Version>>() {
                @Override
                public Range<Version> load(final Method key)  {
                    final TypedColumn typedColumn = key.getAnnotation(TypedColumn.class);
                    return typedColumn == null ? Range.all()
                            : createVersionRange(typedColumn.fromVersion(), typedColumn.untilVersion());
                }
            });
    private static final LoadingCache<Class<?>, Range<Version>> TABLE_VERSIONS = CacheBuilder.newBuilder().weakKeys()
            .build(new CacheLoader<Class<?>, Range<Version>>() {
                @Override
                public Range<Version> load(final Class<?> key)  {
                    final TypedTable typedTable = key.getAnnotation(TypedTable.class);
                    return typedTable == null ? Range.all()
                            : createVersionRange(typedTable.fromVersion(), typedTable.untilVersion());
                }
            });


    private TypedReflections() {

    }

    public static @Nullable String getTableDatabase(final Class<?> type) {
        // Pure reflection metadata access -- no need to cache this
        final TypedTable typedTable = type.getAnnotation(TypedTable.class);
        return typedTable != null ? typedTable.database() : null;
    }

    public static @NonNull String getTableName(final Class<?> type) {
        // Pure reflection metadata access -- no need to cache this
        final TypedTable typedTable = type.getAnnotation(TypedTable.class);
        return typedTable != null ? typedTable.name() : type.getSimpleName();
    }

    public static @NonNull Range<Version> getTableVersionRange(final Class<?> type) {
        // Involves String -> Version conversion, use a cache
        return TABLE_VERSIONS.getUnchecked(type);
    }

    public static @NonNull Range<Version> getColumnVersionRange(final Method method) {
        // Involves String -> Version conversion, use a cache
        return COLUMN_VERSIONS.getUnchecked(method);
    }

    static Range<Version> createVersionRange(final String from, final String until) {
        return Version.createRangeOf(from == null ? Version.NULL : Version.fromString(from),
                until == null ? Version.NULL : Version.fromString(until));
    }
}
