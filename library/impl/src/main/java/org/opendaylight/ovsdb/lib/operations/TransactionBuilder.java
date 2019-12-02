/*
 * Copyright © 2014, 2017 EBay Software Foundation and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.lib.operations;

import static java.util.Objects.requireNonNull;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.List;
import org.eclipse.jdt.annotation.NonNull;
import org.opendaylight.ovsdb.lib.OvsdbClient;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;

public class TransactionBuilder {
    private final List<Operation> operations = new ArrayList<>();
    private final @NonNull DatabaseSchema databaseSchema;
    private final OvsdbClient ovs;

    public TransactionBuilder(final OvsdbClient ovs, final DatabaseSchema schema) {
        this.ovs = ovs;
        this.databaseSchema = requireNonNull(schema);
    }

    public List<Operation> getOperations() {
        return operations;
    }

    public TransactionBuilder add(final Operation operation) {
        operations.add(operation);
        return this;
    }

    public List<Operation> build() {
        return operations;
    }

    public ListenableFuture<List<OperationResult>> execute() {
        return ovs.transact(databaseSchema, operations);
    }

    public @NonNull DatabaseSchema getDatabaseSchema() {
        return databaseSchema;
    }

    public <T> T getTypedRowWrapper(final Class<T> klazz) {
        return TyperUtils.getTypedRowWrapper(databaseSchema, klazz);
    }

    public <T> T getTypedRowSchema(final Class<T> klazz) {
        return TyperUtils.getTypedRowWrapper(databaseSchema, klazz, null);
    }
}
