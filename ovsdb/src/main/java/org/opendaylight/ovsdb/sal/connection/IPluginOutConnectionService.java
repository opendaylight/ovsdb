package org.opendaylight.ovsdb.sal.connection;

import org.opendaylight.controller.sal.core.Node;

public interface IPluginOutConnectionService {
    /**
     * Query SAL if a specified Node is allowed to be connected to this Controller.
     *
     * @param node the network node
     */
    public boolean isConnectionAllowed(Node node);
}