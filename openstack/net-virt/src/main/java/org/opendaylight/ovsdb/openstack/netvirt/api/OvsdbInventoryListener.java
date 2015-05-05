package org.opendaylight.ovsdb.openstack.netvirt.api;

import org.opendaylight.ovsdb.lib.notation.Row;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;

public interface OvsdbInventoryListener {
    public enum OvsdbType {
        NODE,
        ROW,
        OPENVSWITCH,
        BRIDGE,
        CONTROLLER,
        PORT
    }
    public void ovsdbUpdate(Node node, OvsdbType type, Action action);
}
