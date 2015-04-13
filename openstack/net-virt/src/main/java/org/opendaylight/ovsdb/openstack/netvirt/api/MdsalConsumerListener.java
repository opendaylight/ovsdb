package org.opendaylight.ovsdb.openstack.netvirt.api;

import java.net.InetAddress;
import org.opendaylight.ovsdb.lib.notation.Row;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;

/**
 * Created by shague on 4/13/15.
 */
public interface MdsalConsumerListener {
    public void nodeAdded(Node node, InetAddress address, int port );
    public void nodeRemoved(Node node);
    public void rowAdded(Node node, String tableName, String uuid, Row row);
    public void rowUpdated(Node node, String tableName, String uuid, Row old, Row row);
    public void rowRemoved(Node node, String tableName, String uuid, Row row, Object context);
}
