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

public class Operations {
    public static Operations op = new Operations();

    public Insert insert(TableSchema schema) {
        return new Insert(schema);
    }

    public Insert insert(TypedBaseTable typedTable) {
        return new Insert(typedTable);
    }

    public Insert insert(TableSchema schema, Row row) {
        return new Insert(schema, row);
    }

    public Update update(TableSchema schema) {
        return new Update(schema);
    }

    public Update update(TypedBaseTable typedTable) {
        return new Update(typedTable);
    }

    public Update update(TableSchema schema, Row row) {
        return new Update(schema, row);
    }

    public Delete delete(TableSchema schema) {
        return new Delete(schema);
    }

    public Mutate mutate(TableSchema schema) {
        return new Mutate(schema);
    }

    public Mutate mutate(TypedBaseTable typedTable) {
        return new Mutate(typedTable.getSchema());
    }

    public Commit commit(Boolean durable) {
        return new Commit(durable);
    }

    public Abort abort() {
        return new Abort();
    }

    public Select select(TableSchema schema) {
        return new Select(schema);
    }

    public Comment comment(String comment) {
        return new Comment(comment);
    }

    /*
     * Could not use Java keyword "assert" which clashes with the ovsdb json-rpc method.
     * using assertion instead.
     */
    public Assert assertion(String lock) {
        return new Assert(lock);
    }
}
