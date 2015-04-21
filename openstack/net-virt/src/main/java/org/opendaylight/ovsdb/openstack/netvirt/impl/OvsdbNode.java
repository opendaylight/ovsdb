package org.opendaylight.ovsdb.openstack.netvirt.impl;

import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;

public class OvsdbNode {
    private Node node;

    OvsdbNode(Node node) {
        this.node = node;
    }

    public String getNodeId() {
        return node.getNodeId().getValue();
    }
}
