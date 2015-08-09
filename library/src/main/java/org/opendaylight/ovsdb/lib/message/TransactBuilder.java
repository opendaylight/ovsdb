/*
 * Copyright (c) 2013, 2015 EBay Software Foundation and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.lib.message;

import java.util.List;

import org.opendaylight.ovsdb.lib.jsonrpc.Params;
import org.opendaylight.ovsdb.lib.operations.Operation;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;

import com.google.common.collect.Lists;

public class TransactBuilder implements Params {

    List<Operation> requests = Lists.newArrayList();
    DatabaseSchema dbSchema;
    public TransactBuilder(DatabaseSchema dbSchema) {
        this.dbSchema = dbSchema;
    }

    public List<Operation> getRequests() {
        return requests;
    }

    @Override
    public List<Object> params() {
        List<Object> lists = Lists.newArrayList((Object)dbSchema.getName());
        lists.addAll(requests);
        return lists;
    }

    public void addOperations(List<Operation> operation) {
        requests.addAll(operation);
    }

    public void addOperation(Operation operation) {
        requests.add(operation);
    }
}
