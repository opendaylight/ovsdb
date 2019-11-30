/*
 * Copyright © 2019 PANTHEON.tech, s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.lib.schema.typed;

import static java.util.Objects.requireNonNull;

import java.util.Map;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.notation.Row;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.lib.schema.ForwardingDatabaseSchema;
import org.opendaylight.ovsdb.lib.schema.GenericTableSchema;

final class TypedDatabaseSchemaImpl extends ForwardingDatabaseSchema implements TypedDatabaseSchema {
    private final DatabaseSchema delegate;

    TypedDatabaseSchemaImpl(final DatabaseSchema delegate) {
        this.delegate = requireNonNull(delegate);
    }

    @Override
    protected DatabaseSchema delegate() {
        return delegate;
    }

    @Override
    public TypedDatabaseSchema withInternallyGeneratedColumns() {
        final DatabaseSchema newDelegate = delegate.withInternallyGeneratedColumns();
        return newDelegate == delegate ? this : new TypedDatabaseSchemaImpl(newDelegate);
    }

    @Override
    public GenericTableSchema getTableSchema(final Class<?> klazz) {
        return TyperUtils.getTableSchema(this, klazz);
    }

    @Override
    public <T> T getTypedRowWrapper(final Class<T> klazz) {
        return TyperUtils.getTypedRowWrapper(this, klazz);
    }

    @Override
    public <T> T getTypedRowWrapper(final Class<T> klazz, final Row<GenericTableSchema> row) {
        return TyperUtils.getTypedRowWrapper(this, klazz, row);
    }

    @Override
    public <T> Map<UUID, T> extractRowsOld(final Class<T> klazz, final TableUpdates updates) {
        return TyperUtils.extractRowsOld(klazz, updates, this);
    }

    @Override
    public <T> Map<UUID, T> extractRowsUpdated(final Class<T> klazz, final TableUpdates updates) {
        return TyperUtils.extractRowsUpdated(klazz, updates, this);
    }

    @Override
    public <T> Map<UUID, T> extractRowsRemoved(final Class<T> klazz, final TableUpdates updates) {
        return TyperUtils.extractRowsRemoved(klazz, updates, this);
    }
}
