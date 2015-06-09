/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound.transactions.md;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.openvswitch.Bridge;
import org.opendaylight.ovsdb.schema.openvswitch.Controller;
import org.opendaylight.ovsdb.southbound.OvsdbConnectionInstance;
import org.opendaylight.ovsdb.southbound.SouthboundConstants;
import org.opendaylight.ovsdb.southbound.SouthboundMapper;
import org.opendaylight.ovsdb.southbound.SouthboundUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ControllerEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ManagedNodeEntry;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NetworkTopology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.Topology;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.TopologyKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;

import com.google.common.base.Optional;

public class OvsdbControllerUpdateCommand extends AbstractTransactionCommand {
    private static final Logger LOG = LoggerFactory.getLogger(OvsdbControllerUpdateCommand.class);
    private Map<UUID, Controller> updatedControllerRows;
    private Map<UUID, Bridge> updatedBridgeRows;

    public OvsdbControllerUpdateCommand(OvsdbConnectionInstance key,
            TableUpdates updates, DatabaseSchema dbSchema) {
        super(key, updates, dbSchema);
        updatedBridgeRows = TyperUtils.extractRowsUpdated(Bridge.class, getUpdates(), getDbSchema());
        updatedControllerRows = TyperUtils.extractRowsUpdated(Controller.class,
                getUpdates(), getDbSchema());
    }

    @Override
    public void execute(ReadWriteTransaction transaction) {
        if (updatedControllerRows != null && !updatedControllerRows.isEmpty()) {
            if (updatedBridgeRows != null && !updatedBridgeRows.isEmpty()) {
                updateController(transaction, updatedControllerRows, updatedBridgeRows);
            } else {
                updateController(transaction, updatedControllerRows);
            }
        }
    }

    /**
     * Update the ControllerEntry values for the given {@link Bridge} list.
     *
     * <p>
     * Controller and Bridge are independent tables in the Open_vSwitch schema
     * but the OVSDB yang model includes the Controller fields in the
     * Bridge data. In some cases the OVSDB will send Bridge and Controller
     * updates together and in other cases independently. This method here
     * assumes the former.
     * </p>
     *
     * @param transaction the {@link ReadWriteTransaction}
     * @param updatedControllerRows updated {@link Controller} rows
     * @param updatedBridgeRows updated {@link Bridge} rows
     */
    private void updateController(ReadWriteTransaction transaction,
                                  Map<UUID, Controller> updatedControllerRows,
                                  Map<UUID, Bridge> updatedBridgeRows) {

        for (Map.Entry<UUID, Bridge> bridgeEntry : updatedBridgeRows.entrySet()) {
            final List<ControllerEntry> controllerEntries =
                    SouthboundMapper.createControllerEntries(bridgeEntry.getValue(), updatedControllerRows);

            for (ControllerEntry controllerEntry : controllerEntries) {
                transaction.merge(LogicalDatastoreType.OPERATIONAL,
                        getControllerEntryIid(controllerEntry, bridgeEntry.getValue().getNameColumn().getData()),
                        controllerEntry);
            }
        }
    }

    /**
     * Update the ControllerEntry values after finding the related {@Bridge} list.
     *
     * <p>
     * Controller and Bridge are independent tables in the Open_vSwitch schema
     * but the OVSDB yang model includes the Controller fields in the
     * Bridge data. In some cases the OVSDB will send Bridge and Controller
     * updates together and in other cases independently. This method here
     * assumes the latter.
     * </p>
     *
     * @param transaction the {@link ReadWriteTransaction}
     * @param updatedControllerRows updated {@link Controller} rows

     */
    private void updateController(ReadWriteTransaction transaction,
                                  Map<UUID, Controller> updatedControllerRows) {

        Map<InstanceIdentifier<Node>, Node> bridgeNodes = getBridgeNodes(transaction);
        for (Map.Entry<InstanceIdentifier<Node>, Node> bridgeNodeEntry : bridgeNodes.entrySet()) {
            final List<ControllerEntry> controllerEntries =
                    SouthboundMapper.createControllerEntries(bridgeNodeEntry.getValue(), updatedControllerRows);

            for (ControllerEntry controllerEntry : controllerEntries) {
                final InstanceIdentifier<Node> bridgeIid = bridgeNodeEntry.getKey();
                InstanceIdentifier<ControllerEntry> iid = bridgeIid
                        .augmentation(OvsdbBridgeAugmentation.class)
                        .child(ControllerEntry.class, controllerEntry.getKey());
                transaction.merge(LogicalDatastoreType.OPERATIONAL,
                        iid, controllerEntry);
            }
        }
    }

    /**
     * Find all the {@link Node} bridge nodes for the given connection.
     *
     * @param transaction the {@link ReadWriteTransaction}
     * @return
     */
    private Map<InstanceIdentifier<Node>, Node> getBridgeNodes(ReadWriteTransaction transaction) {
        Map<InstanceIdentifier<Node>, Node> bridgeNodes = new HashMap<>();
        final InstanceIdentifier<Node> connectionIId = getOvsdbConnectionInstance().getInstanceIdentifier();
        final Optional<Node> ovsdbNode = SouthboundUtil.readNode(transaction, connectionIId);
        if (ovsdbNode.isPresent()) {
            OvsdbNodeAugmentation ovsdbNodeAugmentation = ovsdbNode.get().getAugmentation(OvsdbNodeAugmentation.class);
            if (ovsdbNodeAugmentation != null) {
                final List<ManagedNodeEntry> managedNodeEntries = ovsdbNodeAugmentation.getManagedNodeEntry();
                for (ManagedNodeEntry managedNodeEntry : managedNodeEntries) {
                    final InstanceIdentifier<Node> bridgeIid =
                            (InstanceIdentifier<Node>) managedNodeEntry.getBridgeRef().getValue();
                    @SuppressWarnings("unchecked")
                    final Optional<Node> bridgeNode = SouthboundUtil.readNode(transaction, bridgeIid);
                    if (bridgeNode.isPresent()) {
                        bridgeNodes.put(bridgeIid, bridgeNode.get());
                    } else {
                        LOG.warn("OVSDB bridge node was not found: {}", bridgeIid);
                    }
                }
            } else {
                LOG.warn("OvsdbNodeAugmentation was not found: {}", connectionIId);
            }
        } else {
            LOG.warn("OVSDB node was not found: {}", connectionIId);
        }

        return bridgeNodes;
    }

    /**
     * Create the {@link InstanceIdentifier} for the {@link ControllerEntry}.
     *
     * @param controllerEntry the {@link ControllerEntry}
     * @param bridgeName the name of the bridge
     * @return the {@link InstanceIdentifier}
     */
    private InstanceIdentifier<ControllerEntry> getControllerEntryIid(
            ControllerEntry controllerEntry, String bridgeName) {

        OvsdbConnectionInstance client = getOvsdbConnectionInstance();
        String nodeString = client.getNodeKey().getNodeId().getValue()
                + "/bridge/" + bridgeName;
        NodeId nodeId = new NodeId(new Uri(nodeString));
        NodeKey nodeKey = new NodeKey(nodeId);
        InstanceIdentifier<Node> bridgeIid = InstanceIdentifier.builder(NetworkTopology.class)
                .child(Topology.class,new TopologyKey(SouthboundConstants.OVSDB_TOPOLOGY_ID))
                .child(Node.class,nodeKey)
                .build();

        InstanceIdentifier<ControllerEntry> iid = bridgeIid
                .augmentation(OvsdbBridgeAugmentation.class)
                .child(ControllerEntry.class, controllerEntry.getKey());
        return iid;
    }
}
