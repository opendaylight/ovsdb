package org.opendaylight.ovsdb.lib.message;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import org.opendaylight.ovsdb.lib.jsonrpc.Params;
import org.opendaylight.ovsdb.lib.message.operations.Operation;
import org.opendaylight.ovsdb.lib.table.Bridge;
import org.opendaylight.ovsdb.lib.table.internal.Table;

import java.util.List;
import java.util.Map;

public class TransactBuilder implements Params {

    List<Operation> requests = Lists.newArrayList();

    @Override
    public List<Object> params() {
        List<Object> lists = Lists.newArrayList((Object)"Open_vSwitch");
        lists.addAll(requests);
        return lists;
    }

    public void addOperation (Operation o) {
        requests.add(o);
    }
}
