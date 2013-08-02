package org.opendaylight.ovsdb.internal;

import java.util.Map;

import org.opendaylight.controller.sal.connection.ConnectionConstants;
import org.opendaylight.controller.sal.core.Node;

public interface IConnectionServiceInternal {
    public Connection getConnection(Node node);
    public Node connect(String identifier, Map<ConnectionConstants, String> params);
}
