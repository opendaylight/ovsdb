/*
 * Copyright © 2019 PANTHEON.tech, s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.lib.schema;

import com.google.common.collect.Range;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.ovsdb.lib.notation.Row;

final class GetTable extends MethodDispatch.TablePrototype {
    private static final class Invoker extends MethodDispatch.TableInvoker {
        Invoker(final GenericTableSchema tableSchema) {
            super(tableSchema);
        }

        @Override
        @Nullable
        Object invokeMethod(final Row<GenericTableSchema> row, final Object proxy, final Object[] args) {
            return tableSchema();
        }
    }

    GetTable(final String tableName) {
        super(Range.all(), tableName);
    }

    @Override
    Invoker bindToImpl(final DatabaseSchema dbSchema) {
        return new Invoker(findTableSchema(dbSchema));
    }
}

