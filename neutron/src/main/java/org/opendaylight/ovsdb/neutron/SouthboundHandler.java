package org.opendaylight.ovsdb.neutron;

import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.ovsdb.lib.table.internal.Table;
import org.opendaylight.ovsdb.plugin.OVSDBInventoryListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SouthboundHandler extends BaseHandler implements OVSDBInventoryListener {
    static final Logger logger = LoggerFactory.getLogger(SouthboundHandler.class);
    @Override
    public void nodeAdded(Node node) {
        logger.debug("NODE ADDED {}", node);
        AdminConfigManager.getManager().populateTunnelEndpoint(node);
        InternalNetworkManager.getManager().prepareInternalNetwork(node);
    }

    @Override
    public void nodeRemoved(Node node) {
        logger.debug("NODE REMOVED {}", node);
    }

    @Override
    public void rowAdded(Node node, String tableName, Table<?> row) {
        logger.debug("ROW ADDED {} , {}", node, row);
        /*
         * Should we support dynamic update of the Tunnel endpoint configuration ?
         *
        if (AdminConfigManager.getManager().isInterested(tableName)) {
            AdminConfigManager.getManager().populateTunnelEndpoint(node, tableName, row);
        }
        */
    }

    @Override
    public void rowRemoved(Node node, String tableName, Table<?> row) {
        logger.debug("ROW REMOVED {} , {}", node, row);
    }
}
