package org.opendaylight.ovsdb.plugin.md;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.ovsdb.lib.notation.Row;
import org.opendaylight.ovsdb.plugin.api.OvsdbConfigurationService;
import org.opendaylight.ovsdb.plugin.api.OvsdbInventoryListener;
import org.opendaylight.ovsdb.schema.openvswitch.Bridge;
import org.opendaylight.ovsdb.schema.openvswitch.Interface;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.ovsdb.node.inventory.rev140731.OvsdbCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.ovsdb.node.inventory.rev140731.OvsdbCapableNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.ovsdb.node.inventory.rev140731.ovsdb.node.ManagedNodes;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

/**
 * Handle OVSDB Inventory Updates and create the necessary entries in the MD-SAL config datastore
 */
public class OvsdbInventoryManager implements OvsdbInventoryListener {

    private volatile OvsdbBindingAwareProvider provider;
    private volatile OvsdbConfigurationService ovsdbConfigurationService;

    static final Logger logger = LoggerFactory.getLogger(OvsdbInventoryManager.class);

    /**
     * When an AD-SAL node is added by the OVSDB Inventory Service, Add an MD-SAL node
     * @param node
     */
    @Override
    public void nodeAdded(org.opendaylight.controller.sal.core.Node node) {
        logger.debug("OVSDB MD-SAL Inventory Adapter: Got nodeAddedCallback for node {}", node.toString());
        DataBroker dataBroker = provider.getDataBroker();
        Preconditions.checkNotNull(dataBroker);

        NodeId nodeId = new NodeId(new NodeId("openvswitch:" + node.getNodeIDString()));
        NodeKey nodeKey = new NodeKey(nodeId);

        OvsdbCapableNode ovsdbNode = new OvsdbCapableNodeBuilder()
                .setIpAddress(Utils.getOvsdbIpAddress(node.getNodeIDString()))
                .setManagedNodes(new ArrayList<ManagedNodes>())
                .build();

        org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node newNode = new NodeBuilder()
                .setId(nodeId)
                .setKey(nodeKey)
                .addAugmentation(OvsdbCapableNode.class, ovsdbNode)
                .build();

        InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node>
                path = InstanceIdentifier.builder(Nodes.class)
                .child(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node.class, nodeKey)
                .toInstance();

        WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
        tx.put(LogicalDatastoreType.CONFIGURATION, path, newNode, true);
        try {
            tx.submit().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    /**
     * When an AD-SAL node is removed by the OVSDB Inventory Service, Remove the MD-SAL node
     * @param node
     */
    @Override
    public void nodeRemoved(org.opendaylight.controller.sal.core.Node node) {

        DataBroker dataBroker = provider.getDataBroker();
        Preconditions.checkNotNull(dataBroker);

        NodeId nodeId = new NodeId(new NodeId("openvswitch:" + node.getNodeIDString()));
        NodeKey nodeKey = new NodeKey(nodeId);

        InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node> path = InstanceIdentifier.builder(Nodes.class)
                .child(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node.class, nodeKey)
                .toInstance();

        WriteTransaction tx = dataBroker.newWriteOnlyTransaction();
        tx.delete(LogicalDatastoreType.CONFIGURATION, path);
        try {
            tx.submit().get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    /**
     * When a Bridge Row is removed, delete the node
     * @param node
     * @param tableName
     * @param uuid
     * @param row
     * @param context
     */
    @Override
    public void rowRemoved(org.opendaylight.controller.sal.core.Node node, String tableName, String uuid, Row row, Object context) {

    }

    /**
     * If the Bridge is updated, updated the OVSDB information in the data store
     * @param node
     * @param tableName
     * @param uuid
     * @param old
     * @param row
     */
    @Override
    public void rowUpdated(org.opendaylight.controller.sal.core.Node node, String tableName, String uuid, Row old, Row row) {

    }

    /**
     * When a new Bridge Row is added, augment the Flow Capable Node with OVSDB information.
     * @param node
     * @param tableName
     * @param uuid
     * @param row
     */
    @Override
    public void rowAdded(org.opendaylight.controller.sal.core.Node node, String tableName, String uuid, Row row) {
        if (tableName.equalsIgnoreCase(ovsdbConfigurationService.getTableName(node, Bridge.class))) {

        }
    }
}
