/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound.transactions.md;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.openvswitch.Bridge;
import org.opendaylight.ovsdb.schema.openvswitch.Controller;
import org.opendaylight.ovsdb.southbound.SouthboundMapper;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ControllerEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ControllerEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.bridge.attributes.ControllerEntryKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ConnectionInfo;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ManagedNodeEntry;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

public class OvsdbControllerUpdateCommand extends AbstractTransactionCommand {
    private static final Logger LOG = LoggerFactory.getLogger(OvsdbControllerUpdateCommand.class);
    private Map<UUID, Controller> updatedControllerRows;
    private Map<UUID, Bridge> updatedBridgeRows;

    public OvsdbControllerUpdateCommand(ConnectionInfo key,
            TableUpdates updates, DatabaseSchema dbSchema) {
        super(key, updates, dbSchema);
        updatedBridgeRows = TyperUtils.extractRowsUpdated(Bridge.class, getUpdates(), getDbSchema());
        updatedControllerRows = TyperUtils.extractRowsUpdated(Controller.class,
                getUpdates(), getDbSchema());
    }

    @Override
    public void execute(ReadWriteTransaction transaction) {
        final InstanceIdentifier<Node> connectionIId = SouthboundMapper.createInstanceIdentifier(getConnectionInfo());
        if ( (updatedBridgeRows == null && updatedControllerRows == null )
                || ( updatedBridgeRows.isEmpty() && updatedControllerRows.isEmpty())) {
            return;
        }
        Optional<Node> node = readNode(transaction, connectionIId);
        if (node.isPresent()) {
            updateController(transaction, node.get());
        }
        for (Bridge bridge: updatedBridgeRows.values()) {
            setController(transaction, bridge);
        }
    }

    private void setController(ReadWriteTransaction transaction, Bridge bridge) {
        for (ControllerEntry controllerEntry: SouthboundMapper.createControllerEntries(bridge, updatedControllerRows)) {
            InstanceIdentifier<ControllerEntry> iid =
                    SouthboundMapper.createInstanceIdentifier(getConnectionInfo(), bridge)
                    .augmentation(OvsdbBridgeAugmentation.class)
                    .child(ControllerEntry.class,controllerEntry.getKey());
            transaction.put(LogicalDatastoreType.OPERATIONAL, iid, controllerEntry);
        }
    }

    private void updateController(ReadWriteTransaction transaction, Node node) {
        for (Entry<UUID, Controller> controllerUpdate : updatedControllerRows.entrySet()) {
            Controller controller = controllerUpdate.getValue();
            Optional<String> bridgeName = getControllerBridge(controllerUpdate.getValue().getUuid());

            if (!bridgeName.isPresent()) {
                bridgeName = getControllerBridge( transaction, node, controllerUpdate.getValue());
            }
            if (bridgeName.isPresent()) {
                ControllerEntryKey controlletEntryKey
                    = new ControllerEntryKey(new Uri(controller.getTargetColumn().getData()));
                ControllerEntry controllerEntry = new ControllerEntryBuilder()
                        .setControllerUuid(new Uuid(controller.getUuid().toString()))
                        .setIsConnected(controllerUpdate.getValue().getIsConnectedColumn().getData())
                        .build();
                InstanceIdentifier<ControllerEntry> controllerPath =
                        SouthboundMapper.createInstanceIdentifier(
                                getConnectionInfo(), new OvsdbBridgeName(bridgeName.get()))
                        .augmentation(OvsdbBridgeAugmentation.class)
                        .child(ControllerEntry.class,controlletEntryKey);
                transaction.merge(LogicalDatastoreType.OPERATIONAL, controllerPath, controllerEntry);
            }
        }
    }

    private Optional<String> getControllerBridge(UUID controllerUUID) {
        for (UUID bridgeUUID : updatedBridgeRows.keySet()) {
            if (this.updatedBridgeRows.get(bridgeUUID).getControllerColumn().getData().contains(controllerUUID)) {
                return Optional.of(this.updatedBridgeRows.get(bridgeUUID).getNameColumn().getData());
            }
        }
        return Optional.absent();
    }

    private Optional<String> getControllerBridge(
            final ReadWriteTransaction transaction, Node node, Controller controller) {
        OvsdbNodeAugmentation ovsdbNode = node.getAugmentation(OvsdbNodeAugmentation.class);
        List<ManagedNodeEntry> managedNodes = ovsdbNode.getManagedNodeEntry();
        for ( ManagedNodeEntry managedNodeEntry : managedNodes ) {
            @SuppressWarnings("unchecked")
            Node managedNode = readNode(transaction
                    ,(InstanceIdentifier<Node>)managedNodeEntry.getBridgeRef().getValue()).get();
            ControllerEntryBuilder controllerBuidler = new ControllerEntryBuilder();
            ControllerEntryKey controllerKey = new ControllerEntryKey(new Uri(controller.getTargetColumn().getData()));
            controllerBuidler.setKey(controllerKey);
            OvsdbBridgeAugmentation ovsdbNodeAugment
                = managedNode.getAugmentation(OvsdbBridgeAugmentation.class);
            return Optional.of(ovsdbNodeAugment.getBridgeName().getValue());
        }
        return Optional.absent();
    }

    private Optional<Node> readNode(final ReadWriteTransaction transaction, final InstanceIdentifier<Node> nodePath) {
        Optional<Node> node = Optional.absent();
        try {
            node = transaction.read(
                    LogicalDatastoreType.OPERATIONAL, nodePath)
                    .checkedGet();
        } catch (final ReadFailedException e) {
            LOG.warn("Read Operational/DS for Node fail! {}",
                    nodePath, e);
        }
        return node;
    }
}
