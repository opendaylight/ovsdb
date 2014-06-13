/*
 *
 *  * Copyright (C) 2014 EBay Software Foundation
 *  *
 *  * This program and the accompanying materials are made available under the
 *  * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 *  * and is available at http://www.eclipse.org/legal/epl-v10.html
 *  *
 *  * Authors : Ashwin Raveendran
 *
 */

package org.opendaylight.ovsdb.lib.operations;

import java.util.ArrayList;
import java.util.List;

import org.opendaylight.ovsdb.lib.OvsDBClientImpl;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ListenableFuture;

public class TransactionBuilder {

    private DatabaseSchema eDatabaseSchema;
    OvsDBClientImpl ovs;
    ArrayList<Operation> operations = Lists.newArrayList();

    public TransactionBuilder(OvsDBClientImpl ovs) {
        this.ovs = ovs;
    }

    public TransactionBuilder(DatabaseSchema eDatabaseSchema) {
        this.eDatabaseSchema = eDatabaseSchema;
    }

    public ArrayList<Operation> getOperations() {
        return operations;
    }

    public TransactionBuilder add(Operation operation) {
        operations.add(operation);
        return this;
    }

    public List<Operation> build() {
        return operations;
    }

    public ListenableFuture<List<OperationResult>> execute() {
        return ovs.transact(operations);
    }
}
