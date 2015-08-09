/*
 * Copyright (c) 2013, 2015 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.plugin.md;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
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

    static final Logger LOGGER = LoggerFactory.getLogger(OvsdbInventoryManager.class);


    /**
     * Called by the framework when the bundle is started
     */
    public void start() {
        //ToDo: Add existing nodes from inventory
        //This case is required for surviving controller reboot
    }

    /**
     * When an AD-SAL node is added by the OVSDB Inventory Service, Add an MD-SAL node
     *
     * @param node    The AD-SAL node
     * @param address The {@link java.net.InetAddress} of the Node
     * @param port    The ephemeral port number used by this connection
     */
    @Override
    public synchronized void nodeAdded(org.opendaylight.controller.sal.core.Node node,
                                       InetAddress address,
                                       int port) {
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
            LOGGER.debug("Removed Node {}", path.toString());
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    /**
     * When an AD-SAL node is removed by the OVSDB Inventory Service, Remove the MD-SAL node
     *
     * @param node The AD-SAL node
     */
    @Override
    public synchronized void nodeRemoved(org.opendaylight.controller.sal.core.Node node) {
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
            LOGGER.debug("Removed Node {}", path.toString());
        } catch (InterruptedException | ExecutionException e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    /**
     * Handle OVSDB row removed When a Bridge row is removed, the OpenFlow Node is deleted The parent OVSDB node is
     * updated and the OpenFlow node removed from it's managed-nodes list
     *
     * @param node      The AD-SAL node
     * @param tableName The name of modified table
     * @param uuid      The UUID of the deleted row
     * @param row       The deleted Row
     */
    @Override
    public synchronized void rowRemoved(org.opendaylight.controller.sal.core.Node node,
                                        String tableName,
                                        String uuid,
                                        Row row,
                                        Object context) {
        if (tableName.equalsIgnoreCase(ovsdbConfigurationService.getTableName(node, Bridge.class))) {
            LOGGER.debug("OVSDB Bridge Row removed on node {}", node.toString());
            DataBroker dataBroker = provider.getDataBroker();
            Preconditions.checkNotNull(dataBroker);

            Bridge bridge = ovsdbConfigurationService.getTypedRow(node, Bridge.class, row);
            Set<String> dpidString = bridge.getDatapathIdColumn().getData();
            Long dpid = HexEncode.stringToLong((String) dpidString.toArray()[0]);

            NodeId openflowNodeId = new NodeId(OPENFLOW_NODE_PREFIX + dpid.toString());
            NodeKey openflowNodeKey = new NodeKey(openflowNodeId);

            InstanceIdentifier<Node> openflowNodePath = InstanceIdentifier.builder(Nodes.class)
                    .child(Node.class, openflowNodeKey)
                    .toInstance();

            NodeId ovsdbNodeId = new NodeId(OVS_NODE_PREFIX + node.getNodeIDString());
            NodeKey ovsdbNodeKey = new NodeKey(ovsdbNodeId);

            InstanceIdentifier<OvsdbCapableNode> ovsdbNodePath = InstanceIdentifier.builder(Nodes.class)
                    .child(Node.class, ovsdbNodeKey)
                    .augmentation(OvsdbCapableNode.class)
                    .toInstance();

            // Read the current OVSDB Node from the DataStore
            ReadWriteTransaction tx = dataBroker.newReadWriteTransaction();
            OvsdbCapableNode ovsdbNode;
            try {
                Optional<OvsdbCapableNode> data = tx.read(LogicalDatastoreType.CONFIGURATION, ovsdbNodePath).get();
                if (!data.isPresent()) {
                    LOGGER.error("OVSDB node not updated. Parent node for {} does not exist", ovsdbNodePath.toString());
                    return;
                }
                ovsdbNode = data.get();
            } catch (InterruptedException | ExecutionException e) {
                LOGGER.error("OVSDB node not updated. Parent node for {} does not exist", ovsdbNodePath.toString());
                return;
            }

            // Update the list of Nodes
            List<NodeId> managedNodesList = ovsdbNode.getManagedNodes();
            managedNodesList.remove(openflowNodeId);

            // Write changes to DataStore
            OvsdbCapableNode updatedNode = new OvsdbCapableNodeBuilder(ovsdbNode)
                    .setManagedNodes(managedNodesList)
                    .build();
            tx.delete(LogicalDatastoreType.CONFIGURATION, openflowNodePath);
            tx.put(LogicalDatastoreType.CONFIGURATION, ovsdbNodePath, updatedNode);

            try {
                tx.submit().get();
                LOGGER.debug("Transaction success for delete of {} and update of {}",
                             openflowNodePath.toString(),
                             ovsdbNodePath.toString());
            } catch (InterruptedException | ExecutionException e) {
                LOGGER.error(e.getMessage(), e);
            }
        }
    }

    /**
     * Handle OVSDB row updates When a Bridge row is updated and it contains a DPID then add a new OpenFlow node to the
     * inventory A relationship is created between the OpenFlow and OVSDB nodes
     *
     * @param node      The AD-SAL node
     * @param tableName The name of the updated table
     * @param uuid      The UUID of the updated row
     * @param old       The old contents of the row
     * @param row       The updated Row
     */
    @Override
    public synchronized void rowUpdated(org.opendaylight.controller.sal.core.Node node,
                                        String tableName,
                                        String uuid,
                                        Row old,
                                        Row row) {
        LOGGER.debug("OVSDB Bridge Row updated on node {}", node.toString());
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
            ReadWriteTransaction tx = dataBroker.newReadWriteTransaction();
            OvsdbCapableNode ovsdbNode;
            try {
                Optional<OvsdbCapableNode> data = tx.read(LogicalDatastoreType.CONFIGURATION, ovsdbNodePath).get();
                if (!data.isPresent()) {
                    LOGGER.error("OVSDB node not updated. Parent node for {} does not exist", ovsdbNodePath.toString());
                    return;
                }
                ovsdbNode = data.get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException("Node does not exist");
            }

            // Update the list of Nodes
            List<NodeId> managedNodesList = ovsdbNode.getManagedNodes();
            managedNodesList.add(openflowNodeId);

            // Create a delta object
            OvsdbCapableNode updatedNode = new OvsdbCapableNodeBuilder(ovsdbNode)
                    .setManagedNodes(managedNodesList)
                    .build();

            // Create parent if we get to this node before openflowplugin
            tx.put(LogicalDatastoreType.CONFIGURATION, openflowNodepath, ovsdbManagedNode, true);
            tx.put(LogicalDatastoreType.CONFIGURATION, ovsdbNodePath, updatedNode);

            try {
                tx.submit().get();
                LOGGER.debug("Transaction success for addition of {} and update of {}",
                             openflowNodepath.toString(),
                             ovsdbNodePath.toString());
            } catch (InterruptedException | ExecutionException e) {
                LOGGER.error(e.getMessage(), e);
            }
        }
    }

    @Override
    public synchronized void rowAdded(org.opendaylight.controller.sal.core.Node node,
                                      String tableName,
                                      String uuid,
                                      Row row) {
        // noop
    }
}
