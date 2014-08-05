package org.opendaylight.ovsdb.plugin.md;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.sal.utils.HexEncode;
import org.opendaylight.ovsdb.lib.notation.Row;
import org.opendaylight.ovsdb.plugin.api.OvsdbConfigurationService;
import org.opendaylight.ovsdb.plugin.api.OvsdbInventoryListener;
import org.opendaylight.ovsdb.schema.openvswitch.Bridge;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.ovsdb.node.inventory.rev140731.OvsdbCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.ovsdb.node.inventory.rev140731.OvsdbCapableNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.ovsdb.node.inventory.rev140731.OvsdbManagedNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.ovsdb.node.inventory.rev140731.OvsdbManagedNodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.ovsdb.node.inventory.rev140731.nodes.node.OvsdbBridge;
import org.opendaylight.yang.gen.v1.urn.opendaylight.ovsdb.node.inventory.rev140731.nodes.node.OvsdbBridgeBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;

/**
 * Handle OVSDB Inventory Updates and create the necessary entries in the MD-SAL config datastore
 */
public class OvsdbInventoryManager implements OvsdbInventoryListener {

    // Dependencies injected by OSGi
    private volatile OvsdbBindingAwareProvider provider;
    private volatile OvsdbConfigurationService ovsdbConfigurationService;

    static final String OVS_NODE_PREFIX = "openvswitch:";
    static final String OPENFLOW_NODE_PREFIX = "openflow:";

    static final Logger logger = LoggerFactory.getLogger(OvsdbInventoryManager.class);


    /**
     * Called by the framework when the bundle is started
     */
    public void start(){
        //ToDo: Add existing nodes from inventory
        //This case is required for surviving controller reboot
    }

    /**
     * When an AD-SAL node is added by the OVSDB Inventory Service, Add an MD-SAL node
     */
    @Override
    public synchronized void nodeAdded(org.opendaylight.controller.sal.core.Node node, InetAddress address, int port) {
        logger.debug("OVSDB MD-SAL Inventory Adapter: Got node added for node {}", node.toString());
        DataBroker dataBroker = provider.getDataBroker();
        Preconditions.checkNotNull(dataBroker);

        NodeId nodeId = new NodeId(OVS_NODE_PREFIX + node.getNodeIDString());
        NodeKey nodeKey = new NodeKey(nodeId);

        OvsdbCapableNode ovsdbNode = new OvsdbCapableNodeBuilder()
                .setIpAddress(Utils.convertIpAddress(address))
                .setPort(new PortNumber(port))
                .setManagedNodes(new ArrayList<NodeId>())
                .build();

        Node newNode = new NodeBuilder()
                .setId(nodeId)
                .setKey(nodeKey)
                .addAugmentation(OvsdbCapableNode.class, ovsdbNode)
                .build();

        InstanceIdentifier<Node> path = InstanceIdentifier.builder(Nodes.class)
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
     */
    @Override
    public synchronized void nodeRemoved(org.opendaylight.controller.sal.core.Node node) {
        logger.debug("OVSDB MD-SAL Inventory Adapter: Got node removed for node {}", node.toString());
        DataBroker dataBroker = provider.getDataBroker();
        Preconditions.checkNotNull(dataBroker);

        NodeId nodeId = new NodeId(new NodeId(OVS_NODE_PREFIX + node.getNodeIDString()));
        NodeKey nodeKey = new NodeKey(nodeId);

        InstanceIdentifier<Node> path = InstanceIdentifier.builder(Nodes.class)
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
     */
    @Override
    public synchronized void rowRemoved(org.opendaylight.controller.sal.core.Node node, String tableName, String uuid, Row row,
                           Object context) {
        if (tableName.equalsIgnoreCase(ovsdbConfigurationService.getTableName(node, Bridge.class))) {
            logger.debug("OVSDB Bridge Row removed on node {}", node.toString());
            DataBroker dataBroker = provider.getDataBroker();

            Bridge bridge = ovsdbConfigurationService.getTypedRow(node, Bridge.class, row);
            Set<String> dpidString = bridge.getDatapathIdColumn().getData();
            Long dpid = HexEncode.stringToLong((String) dpidString.toArray()[0]);

            NodeId openflowNodeId = new NodeId(OPENFLOW_NODE_PREFIX + dpid.toString());
            NodeKey openflowNodeKey = new NodeKey(openflowNodeId);

            InstanceIdentifier<OvsdbManagedNode> openflowNodepath = InstanceIdentifier.builder(Nodes.class)
                    .child(Node.class, openflowNodeKey)
                    .augmentation(OvsdbManagedNode.class)
                    .toInstance();

            NodeId ovsdbNodeId = new NodeId(OVS_NODE_PREFIX + node.getNodeIDString());
            NodeKey ovsdbNodeKey = new NodeKey(ovsdbNodeId);

            InstanceIdentifier<OvsdbCapableNode> ovsdbNodePath = InstanceIdentifier.builder(Nodes.class)
                    .child(Node.class, ovsdbNodeKey)
                    .augmentation(OvsdbCapableNode.class)
                    .toInstance();

            // Read the current OVSDB Node from the DataStore
            ReadOnlyTransaction readTx = dataBroker.newReadOnlyTransaction();
            OvsdbCapableNode ovsdbNode;
            try {
                Optional<OvsdbCapableNode> data = readTx.read(LogicalDatastoreType.CONFIGURATION, ovsdbNodePath).get();
                ovsdbNode = data.get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException("Node does not exist");
            }

            // Update the list of Nodes
            List<NodeId> managedNodesList = ovsdbNode.getManagedNodes();
            managedNodesList.remove(openflowNodeId);

            // Write changes to DataStore
            OvsdbCapableNode updatedNode = new OvsdbCapableNodeBuilder(ovsdbNode)
                    .setManagedNodes(managedNodesList)
                    .build();
            WriteTransaction writeTx = dataBroker.newWriteOnlyTransaction();
            writeTx.delete(LogicalDatastoreType.CONFIGURATION, openflowNodepath);
            writeTx.put(LogicalDatastoreType.CONFIGURATION, ovsdbNodePath, updatedNode);
            writeTx.submit();
        }
    }

    /**
     * Handle OVSDB row updates
     */
    @Override
    public synchronized void rowUpdated(org.opendaylight.controller.sal.core.Node node, String tableName, String uuid, Row old,
                           Row row) {
        logger.debug("OVSDB Bridge Row updated on node {}", node.toString());
        if (tableName.equalsIgnoreCase(ovsdbConfigurationService.getTableName(node, Bridge.class))) {
            DataBroker dataBroker = provider.getDataBroker();
            Bridge bridge = ovsdbConfigurationService.getTypedRow(node, Bridge.class, row);

            Set<String> dpidString = bridge.getDatapathIdColumn().getData();
            Long dpid;
            try {
               dpid = HexEncode.stringToLong((String) dpidString.toArray()[0]);
            } catch (ArrayIndexOutOfBoundsException e) {
                return;
            }

            NodeId openflowNodeId = new NodeId(OPENFLOW_NODE_PREFIX + dpid.toString());
            NodeKey openflowNodeKey = new NodeKey(openflowNodeId);

            InstanceIdentifier<OvsdbManagedNode> openflowNodepath = InstanceIdentifier.builder(Nodes.class)
                    .child(Node.class, openflowNodeKey)
                    .augmentation(OvsdbManagedNode.class)
                    .toInstance();

            NodeId ovsdbNodeId = new NodeId(OVS_NODE_PREFIX + node.getNodeIDString());
            NodeKey ovsdbNodeKey = new NodeKey(ovsdbNodeId);

            InstanceIdentifier<OvsdbCapableNode> ovsdbNodePath = InstanceIdentifier.builder(Nodes.class)
                    .child(Node.class, ovsdbNodeKey)
                    .augmentation(OvsdbCapableNode.class)
                    .toInstance();

            // Create an OvsdbBridge object using the information from the update
            OvsdbBridge ovsdbBridge = new OvsdbBridgeBuilder()
                    .setBridgeName(bridge.getName())
                    .setBridgeUuid(uuid)
                    .setManagedBy(ovsdbNodeId)
                    .build();

            // Add the bridge to the OvsdbManagedNode
            OvsdbManagedNode ovsdbManagedNode = new OvsdbManagedNodeBuilder()
                    .setOvsdbBridge(ovsdbBridge)
                    .build();

            // Read the current OVSDB Node from the DataStore
            ReadOnlyTransaction readTx = dataBroker.newReadOnlyTransaction();
            OvsdbCapableNode ovsdbNode = null;
            try {
                Optional<OvsdbCapableNode>
                        data =
                        readTx.read(LogicalDatastoreType.CONFIGURATION, ovsdbNodePath).get();
                ovsdbNode = data.get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException("Node does not exist");
            }

            // Update the list of Nodes
            List<NodeId> managedNodesList = ovsdbNode.getManagedNodes();
            Boolean updated = false;
            managedNodesList.add(openflowNodeId);

            // Create a delta object
            OvsdbCapableNode updatedNode = new OvsdbCapableNodeBuilder(ovsdbNode)
                    .setManagedNodes(managedNodesList)
                    .build();

            // Write changes to DataStore
            WriteTransaction writeTx = dataBroker.newWriteOnlyTransaction();
            // Create parent if we get to this node before openflowplugin
            writeTx.put(LogicalDatastoreType.CONFIGURATION, openflowNodepath, ovsdbManagedNode, true);
            writeTx.put(LogicalDatastoreType.CONFIGURATION, ovsdbNodePath, updatedNode);
            writeTx.submit();
        }
    }

    @Override
    public synchronized void rowAdded(org.opendaylight.controller.sal.core.Node node, String tableName, String uuid, Row row) {
        // noop
    }
}
