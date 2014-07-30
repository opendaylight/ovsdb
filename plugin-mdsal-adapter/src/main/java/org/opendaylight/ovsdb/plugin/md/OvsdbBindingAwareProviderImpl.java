package org.opendaylight.ovsdb.plugin.md;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.binding.api.AbstractBindingAwareProvider;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.ovsdb.lib.notation.Row;
import org.opendaylight.ovsdb.plugin.api.OvsdbInventoryListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRemoved;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRemovedBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.ovsdb.node.inventory.rev140731.OvsdbCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.ovsdb.node.inventory.rev140731.OvsdbCapableNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.ovsdb.node.inventory.rev140731.ovsdb.node.ManagedNodes;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import java.util.ArrayList;


public class OvsdbBindingAwareProviderImpl extends AbstractBindingAwareProvider implements OvsdbInventoryListener {

    private DataBroker dataBroker;
    private NotificationProviderService notificationService;

    @Override
    public void onSessionInitiated(BindingAwareBroker.ProviderContext providerContext) {
        this.dataBroker = providerContext.getSALService(DataBroker.class);
        this.notificationService = providerContext.getSALService(NotificationProviderService.class);
    }

    @Override
    public void nodeAdded(org.opendaylight.controller.sal.core.Node node) {
        NodeId nodeId = new NodeId(new NodeId(node.getNodeIDString()));
        NodeKey nodeKey = new NodeKey(nodeId);

        OvsdbCapableNode ovsdbNode = new OvsdbCapableNodeBuilder()
                .setIpAddress(Utils.getOvsdbIpAddress(node.getNodeIDString()))
                .setManagedNodes(new ArrayList<ManagedNodes>())
                .build();

        Node newNode = new NodeBuilder()
                .setId(nodeId)
                .setKey(nodeKey)
                .addAugmentation(OvsdbCapableNode.class, ovsdbNode)
                .build();

        InstanceIdentifier<Node> path = InstanceIdentifier.builder(Nodes.class)
                .child(Node.class, nodeKey)
                .toInstance();
        WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
        tx.put(LogicalDatastoreType.CONFIGURATION, path, newNode , true);
    }

    @Override
    public void nodeRemoved(org.opendaylight.controller.sal.core.Node node) {
        NodeId nodeId = new NodeId(new NodeId(node.getNodeIDString()));
        NodeKey nodeKey = new NodeKey(nodeId);

        InstanceIdentifier<Node> path = InstanceIdentifier.builder(Nodes.class)
                .child(Node.class, nodeKey)
                .toInstance();

        WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
        tx.delete(LogicalDatastoreType.CONFIGURATION, path);
    }

    @Override
    public void rowRemoved(org.opendaylight.controller.sal.core.Node node, String tableName, String uuid, Row row, Object context) {

    }

    @Override
    public void rowUpdated(org.opendaylight.controller.sal.core.Node node, String tableName, String uuid, Row old, Row row) {
        //noop
    }

    @Override
    public void rowAdded(org.opendaylight.controller.sal.core.Node node, String tableName, String uuid, Row row) {

    }
}
