package org.opendaylight.ovsdb.lib.message;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import org.opendaylight.ovsdb.lib.jsonrpc.Params;
import org.opendaylight.ovsdb.lib.table.Bridge;
import org.opendaylight.ovsdb.lib.table.internal.Table;

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
