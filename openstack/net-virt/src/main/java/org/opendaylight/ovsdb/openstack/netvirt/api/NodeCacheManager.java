package org.opendaylight.ovsdb.openstack.netvirt.api;

import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;

import java.util.List;

/**
 * This interface is used to cache ids of nodes that are needed by net-virt.
 * The nodes are added and removed by an external listener.
 */
public interface NodeCacheManager {
    void nodeAdded(String nodeIdentifier);
    void nodeRemoved(String nodeIdentifier);

    List<Node> getNodes();
}
