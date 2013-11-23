package org.opendaylight.ovsdb.plugin;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.opendaylight.controller.sal.connection.ConnectionConstants;
import org.opendaylight.controller.sal.core.Node;

public interface IConnectionServiceInternal {
    public Connection getConnection(Node node);
    public List<Node> getNodes();
    public Node connect(String identifier, Map<ConnectionConstants, String> params);
    public Boolean setOFController(Node node, String bridgeUUID) throws InterruptedException, ExecutionException;
}
