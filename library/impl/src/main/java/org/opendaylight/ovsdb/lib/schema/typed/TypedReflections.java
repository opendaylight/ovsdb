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
    private abstract static class VersionRangeLoader<T> extends CacheLoader<T, Range<Version>> {
        static Range<Version> createVersionRange(final String from, final String until) {
            if (from != null) {
                final Version ver = Version.fromString(from);
                return until != null ? Range.closed(ver, Version.fromString(until)) : Range.atLeast(ver);
            }
            return until != null ? Range.atMost(Version.fromString(until)) : Range.all();
        }
    }

    private static final LoadingCache<Method, Range<Version>> COLUMNT_VERSIONS = CacheBuilder.newBuilder().weakKeys()
            .build(new VersionRangeLoader<Method>() {
                @Override
                public Range<Version> load(final Method key)  {
                    final TypedColumn typedColumn = key.getAnnotation(TypedColumn.class);
                    return typedColumn == null ? Range.all()
                            : createVersionRange(typedColumn.fromVersion(), typedColumn.untilVersion());
                }
            });

    private static final LoadingCache<Class<?>, Range<Version>> TABLE_VERSIONS = CacheBuilder.newBuilder().weakKeys()
            .build(new VersionRangeLoader<Class<?>>() {
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
        final TypedTable typedTable = type.getAnnotation(TypedTable.class);
        return typedTable != null ? typedTable.database() : null;
    }

    public static @NonNull String getTableName(final Class<?> type) {
        final TypedTable typedTable = type.getAnnotation(TypedTable.class);
        return typedTable != null ? typedTable.name() : type.getSimpleName();
    }

    public static @NonNull Range<Version> getTableVersionRange(final Class<?> type) {
        return TABLE_VERSIONS.getUnchecked(type);
    }

    public static @NonNull Range<Version> getColumnVersionRange(final Method method) {
        return COLUMNT_VERSIONS.getUnchecked(method);
    }
}
