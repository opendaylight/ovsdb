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
import com.google.common.collect.Maps;

import org.opendaylight.ovsdb.lib.jsonrpc.Params;
import org.opendaylight.ovsdb.lib.table.Table;

import java.util.List;
import java.util.Map;

public class MonitorRequestBuilder implements Params {

    Map<String, MonitorRequest> requests = Maps.newLinkedHashMap();

    @Override
    public List<Object> params() {
        return Lists.newArrayList("Open_vSwitch", null, requests);
    }

    public <T extends Table> MonitorRequest<T> monitor(T table) {
        MonitorRequest<T> req = new MonitorRequest<T>();
        requests.put(table.getTableName().getName(), req);
        return req;
    }
}
