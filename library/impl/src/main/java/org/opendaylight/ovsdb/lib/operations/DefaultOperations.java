/*
 * Copyright (c) 2024 PANTHEON.tech, s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.lib.operations;

import javax.inject.Inject;
import javax.inject.Singleton;
import org.opendaylight.ovsdb.lib.notation.Row;
import org.opendaylight.ovsdb.lib.schema.TableSchema;
import org.opendaylight.ovsdb.lib.schema.typed.TypedBaseTable;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;

/**
 * Default implementation of {@link Operations}.
 */
@Singleton
@Component
public final class DefaultOperations implements Operations {
    @Inject
    @Activate
    public DefaultOperations() {
        // Nothing else
    }

    @Override
    public <E extends TableSchema<E>> Insert<E> insert(final TableSchema<E> schema) {
        return new Insert<>(schema);
    }

    @Override
    public <E extends TableSchema<E>> Insert<E> insert(final TypedBaseTable<E> typedTable) {
        return new Insert<>(typedTable);
    }

    @Override
    public <E extends TableSchema<E>> Insert<E> insert(final TableSchema<E> schema, final Row<E> row) {
        return new Insert<>(schema, row);
    }

    @Override
    public <E extends TableSchema<E>> Update<E> update(final TableSchema<E> schema) {
        return new Update<>(schema);
    }

    @Override
    public <E extends TableSchema<E>> Update<E> update(final TypedBaseTable<E> typedTable) {
        return new Update<>(typedTable);
    }

    @Override
    public <E extends TableSchema<E>> Update<E> update(final TableSchema<E> schema, final Row<E> row) {
        return new Update<>(schema, row);
    }

    @Override
    public <E extends TableSchema<E>> Delete<E> delete(final TableSchema<E> schema) {
        return new Delete<>(schema);
    }

    @Override
    public <E extends TableSchema<E>> Mutate<E> mutate(final TableSchema<E> schema) {
        return new Mutate<>(schema);
    }

    @Override
    public <E extends TableSchema<E>> Mutate<E> mutate(final TypedBaseTable<E> typedTable) {
        return new Mutate<>(typedTable.getSchema());
    }

    @Override
    public Commit commit(final Boolean durable) {
        return new Commit(durable);
    }

    @Override
    public Abort abort() {
        return new Abort();
    }

    @Override
    public <E extends TableSchema<E>> Select<E> select(final TableSchema<E> schema) {
        return new Select<>(schema);
    }

    @Override
    public Comment comment(final String comment) {
        return new Comment(comment);
    }

    @Override
    public Assert assertion(final String lock) {
        return new Assert(lock);
    }
}
