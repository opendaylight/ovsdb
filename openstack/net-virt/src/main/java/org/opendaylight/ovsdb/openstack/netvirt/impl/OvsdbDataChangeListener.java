package org.opendaylight.ovsdb.openstack.netvirt.impl;

import java.util.Map;
import java.util.Set;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.ovsdb.openstack.netvirt.api.MdsalConsumer;
import org.opendaylight.ovsdb.openstack.netvirt.api.MdsalConsumerListener;
import org.opendaylight.ovsdb.southbound.SouthboundConstants;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TopologyId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Integration tests for southbound-impl
 *
 * @author Sam Hague (shague@redhat.com)
 */
public class OvsdbDataChangeListener implements DataChangeListener, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(OvsdbDataChangeListener.class);
    private DataBroker dataBroker = null;
    private static final TopologyId OVSDB_TOPOLOGY_ID = new TopologyId(new Uri("ovsdb:1"));
    private ListenerRegistration<DataChangeListener> registration;

    public OvsdbDataChangeListener (DataBroker dataBroker) {
        LOG.info(">>>>> Registering OvsdbNodeDataChangeListener");
        this.dataBroker = dataBroker;
        InstanceIdentifier<Node> path = InstanceIdentifier
                .create(NetworkTopology.class)
                .child(Topology.class, new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID))
                .child(Node.class);
        registration =
                dataBroker.registerDataChangeListener(LogicalDatastoreType.OPERATIONAL, path, this,
                        AsyncDataBroker.DataChangeScope.SUBTREE);
    }

    @Override
    public void close () throws Exception {
        registration.close();
    }

    @Override
    public void onDataChanged(AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes) {
        for (Map.Entry<InstanceIdentifier<?>, DataObject> created : changes.getCreatedData().entrySet()) {
            if (created.getValue() instanceof Node) {

            }
        }
    }

    private void notifyNodeAdded(Node node) {
        Set<MdsalConsumerListener> mdsalConsumerListeners = MdsalConsumerImpl.getMdsalConsumerListeners();
        for (MdsalConsumerListener mdsalConsumerListener : mdsalConsumerListeners) {
            mdsalConsumerListener.ovsdbNodeAdded(inventoryNodeFromTopology(node));
        }
    }

    private org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node
            inventoryNodeFromTopology(Node topologyNode) {
        OvsdbNodeAugmentation ovsdbNodeAugmentation = topologyNode.getAugmentation(OvsdbNodeAugmentation.class);
        String addrPort = ovsdbNodeAugmentation.getConnectionInfo().getRemoteIp().getValue() + ":"
                + ovsdbNodeAugmentation.getConnectionInfo().getRemotePort().getValue();
        NodeId nodeId = new NodeId("OVS" + "|" + addrPort);
        NodeKey nodeKey = new NodeKey(nodeId);
        org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node inventoryNode = new NodeBuilder()
                .setId(nodeId)
                .setKey(nodeKey)
                .build();
        return inventoryNode;
    }
}
