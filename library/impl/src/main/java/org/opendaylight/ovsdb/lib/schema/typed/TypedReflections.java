/*
 * Copyright © 2019 PANTHEON.tech, s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.lib.schema.typed;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;

/**
 * Utilities for extracting annotation information at runtime.
 */
public final class TypedReflections {
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
}
