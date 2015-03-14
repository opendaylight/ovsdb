package org.opendaylight.ovsdb.openstack.netvirt.api;

import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;

/**
 * When this interface is used, instance owner will get callbacks on
 * changes that occur in NodeCacheManager
 */
public interface NodeCacheListener {

    public void notifyNode(Node node, Action action);
}
