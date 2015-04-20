package org.opendaylight.ovsdb.openstack.netvirt.api;

import java.net.InetAddress;
import org.opendaylight.ovsdb.lib.notation.Row;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
//import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;

public interface MdsalConsumerListener {
    public void ovsdbNodeAdded(Node node);
    public void ovsdbNodeRemoved(Node node);
    public void rowAdded(Node node, String tableName, String uuid, Row row);
    public void rowUpdated(Node node, String tableName, String uuid, Row old, Row row);
    public void rowRemoved(Node node, String tableName, String uuid, Row row, Object context);
}
