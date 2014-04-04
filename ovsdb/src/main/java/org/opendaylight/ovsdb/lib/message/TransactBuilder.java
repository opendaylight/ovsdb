/*
 * Copyright (C) 2013 EBay Software Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Ashwin Raveendran, Madhu Venugopal
 */
package org.opendaylight.ovsdb.lib.message;

import com.google.common.collect.Lists;

import org.opendaylight.ovsdb.lib.jsonrpc.Params;
import org.opendaylight.ovsdb.lib.notation.operations.Operation;
import java.util.List;

public class TransactBuilder implements Params {

    List<Operation> requests = Lists.newArrayList();

    public List<Operation> getRequests() {
        return requests;
    }

    @Override
    public List<Object> params() {
        List<Object> lists = Lists.newArrayList((Object)"Open_vSwitch");
        lists.addAll(requests);
        return lists;
    }

    public void addOperations (List<Operation> o) {
        requests.addAll(o);
    }

    public void addOperation (Operation o) {
        requests.add(o);
    }
}
