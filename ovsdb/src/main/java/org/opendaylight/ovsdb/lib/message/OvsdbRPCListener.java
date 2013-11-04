package org.opendaylight.ovsdb.lib.message;

import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.ovsdb.lib.jsonrpc.JsonRpcOperation;

import java.util.List;

public interface OvsdbRPCListener {

    @JsonRpcOperation
    public void update(Node node, UpdateNotification upadateNotification);

    @JsonRpcOperation
    public void locked(Node node, List<String> ids);

    @JsonRpcOperation
    public void stolen(Node node, List<String> ids);

    @JsonRpcOperation
    public String echo(String echo);
}
