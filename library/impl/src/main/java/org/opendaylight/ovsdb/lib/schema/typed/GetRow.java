/*
 * Copyright © 2019 PANTHEON.tech, s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.lib.schema.typed;

import org.opendaylight.ovsdb.lib.notation.Row;
import org.opendaylight.ovsdb.lib.schema.GenericTableSchema;
import org.opendaylight.ovsdb.lib.schema.typed.MethodDispatch.Prototype;

final class GetRow extends Prototype {
    private static final MethodDispatch.Invoker INVOKER = new MethodDispatch.Invoker() {
        @Override
        Object invokeMethod(final Row<GenericTableSchema> row, final Object proxy, final Object[] args) {
            return row;
        }
    };

    static final GetRow INSTANCE = new GetRow();

    private GetRow() {

    }

    @Override
    MethodDispatch.Invoker bindTo(final TypedDatabaseSchema dbSchema) {
        return INVOKER;
    }
}
