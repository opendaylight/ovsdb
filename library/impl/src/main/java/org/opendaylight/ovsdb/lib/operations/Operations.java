/*
 * Copyright (c) 2014, 2015 EBay Software Foundation and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.lib.operations;

import org.opendaylight.ovsdb.lib.notation.Row;
import org.opendaylight.ovsdb.lib.schema.TableSchema;
import org.opendaylight.ovsdb.lib.schema.typed.TypedBaseTable;

public interface Operations {

    <E extends TableSchema<E>> Insert<E> insert(TableSchema<E> schema);

    <E extends TableSchema<E>> Insert<E> insert(TypedBaseTable<E> typedTable);

    <E extends TableSchema<E>> Insert<E> insert(TableSchema<E> schema, Row<E> row);

    <E extends TableSchema<E>> Update<E> update(TableSchema<E> schema);

    <E extends TableSchema<E>> Update<E> update(TypedBaseTable<E> typedTable);

    <E extends TableSchema<E>> Update<E> update(TableSchema<E> schema, Row<E> row);

    <E extends TableSchema<E>> Delete<E> delete(TableSchema<E> schema);

    <E extends TableSchema<E>> Mutate<E> mutate(TableSchema<E> schema);

    <E extends TableSchema<E>> Mutate<E> mutate(TypedBaseTable<E> typedTable);

    Commit commit(Boolean durable);

    Abort abort();

    <E extends TableSchema<E>> Select<E> select(TableSchema<E> schema);

    Comment comment(String comment);

    /*
     * Could not use Java keyword "assert" which clashes with the ovsdb json-rpc method. using assertion instead.
     */
    Assert assertion(String lock);
}
