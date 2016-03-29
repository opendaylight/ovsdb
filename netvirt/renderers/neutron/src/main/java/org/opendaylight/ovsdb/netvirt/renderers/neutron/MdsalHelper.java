/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.netvirt.renderers.neutron;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableBiMap;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.ports.rev151227.Ports;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.ports.rev151227.ports.Port;
import org.opendaylight.yang.gen.v1.urn.opendaylight.netvirt.ports.rev151227.ports.PortKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.netvirt.neutron.rev160308.NetvirtNeutron;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MdsalHelper {
    private static final Logger LOG = LoggerFactory.getLogger(MdsalHelper.class);
    //Do we need this?
    //public static final TopologyId NETVIRT_TOPOLOGY_ID = new TopologyId(new Uri("netvirt:1"));
    public static final String NETVIRT_URI_PREFIX = "netvirt";
    public static final String PORT_URI_PREFIX = "port";
    public static final String NETWORK_URI_PREFIX = "network";

    /*
    public static NodeId createManagedNodeId(InstanceIdentifier<Node> iid) {
        NodeKey nodeKey = iid.firstKeyOf(Node.class, NodeKey.class);
        return nodeKey.getNodeId();
    }
    */

    public static InstanceIdentifier<Port> createInstanceIdentifier() {

        return InstanceIdentifier
                .create(Ports.class)
                .child(Port.class, new PortKey(new Uuid("12345678-1234-1224-1234-123456789012")));
    }

    /*
    public static InstanceIdentifier<Port> createInstanceIdentifier(Port port) {
        return InstanceIdentifier
                .create(NetvirtNeutron.class)
                .child(Ports.class)
                .child(Ports.class, port.getKey());
    }
    */

    /*
    public static InstanceIdentifier<Node> createInstanceIdentifier(NodeKey ovsdbNodeKey, String bridgeName) {
       return createInstanceIdentifier(createManagedNodeId(ovsdbNodeKey.getNodeId(), bridgeName));
    }

    public static NodeId createManagedNodeId(NodeId ovsdbNodeId, String bridgeName) {
        return new NodeId(ovsdbNodeId.getValue()
                + "/" + BRIDGE_URI_PREFIX + "/" + bridgeName);
    }

    public static InstanceIdentifier<TerminationPoint> createTerminationPointInstanceIdentifier(Node node, String portName){

        InstanceIdentifier<TerminationPoint> terminationPointPath = InstanceIdentifier
                .create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(OVSDB_TOPOLOGY_ID))
                .child(Node.class,node.getKey())
                .child(TerminationPoint.class, new TerminationPointKey(new TpId(portName)));

        LOG.debug("Termination point InstanceIdentifier generated : {}",terminationPointPath);
        return terminationPointPath;
    }

    public static String createOvsdbInterfaceType(Class<? extends InterfaceTypeBase> mdsaltype) {
        Preconditions.checkNotNull(mdsaltype);
        ImmutableBiMap<Class<? extends InterfaceTypeBase>, String> mapper =
                OVSDB_INTERFACE_TYPE_MAP.inverse();
        return mapper.get(mdsaltype);
    }
    */
}
