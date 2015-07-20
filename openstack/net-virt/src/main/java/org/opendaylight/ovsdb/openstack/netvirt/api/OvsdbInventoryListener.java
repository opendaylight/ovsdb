package org.opendaylight.ovsdb.openstack.netvirt.api;

import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.DataObject;

public interface OvsdbInventoryListener {
    enum OvsdbType {
        NODE,
        ROW,
        OPENVSWITCH,
        BRIDGE,
        CONTROLLER,
        PORT
    }
    void ovsdbUpdate(Node node, DataObject augmentationDataChanges, OvsdbType type, Action action);
    void triggerUpdates();
}
