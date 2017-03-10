/*
 * Copyright © 2013, 2017 EBay Software Foundation and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.lib.message;

import java.util.ArrayList;
import java.util.List;
import org.opendaylight.ovsdb.lib.jsonrpc.Params;
import org.opendaylight.ovsdb.lib.operations.Operation;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;

public class TransactBuilder implements Params {

    private List<Operation> requests = new ArrayList<>();
    private DatabaseSchema dbSchema;

    public TransactBuilder(DatabaseSchema dbSchema) {
        this.dbSchema = dbSchema;
    }

    public List<Operation> getRequests() {
        return requests;
    }

    @Override
    public List<Object> params() {
        List<Object> list = new ArrayList<>(requests.size() + 1);
        list.add(dbSchema.getName());
        list.addAll(requests);
        return list;
    }

    public void addOperations(List<Operation> operation) {
        requests.addAll(operation);
    }

    public void addOperation(Operation operation) {
        requests.add(operation);
    }
}
