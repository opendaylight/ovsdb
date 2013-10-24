package org.opendaylight.ovsdb.message;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import org.opendaylight.ovsdb.internal.jsonrpc.Params;
import org.opendaylight.ovsdb.table.Bridge;
import org.opendaylight.ovsdb.table.internal.Table;

import java.util.List;
import java.util.Map;

public class MonitorRequestBuilder implements Params {

    Map<String, MonitorRequest> requests = Maps.newLinkedHashMap();

    @Override
    public List<Object> params() {
        requests.put("Bridge", new MonitorRequest<Bridge>());
        return Lists.newArrayList("Open_vSwitch", null, requests);
    }


    public <T extends Table> MonitorRequest<T> monitor(T table) {
        MonitorRequest<T> req = new MonitorRequest<T>();
        requests.put(table.getTableName().getName(), req);
        return req;
    }
}
