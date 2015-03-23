/*
 * Copyright (c) 2015 Inocybe inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound.transactions.md;

import java.util.ArrayList;
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
import org.opendaylight.ovsdb.schema.openvswitch.OpenVSwitch;
import org.opendaylight.ovsdb.southbound.OvsdbClientKey;
import org.opendaylight.ovsdb.southbound.SouthboundMapper;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ManagedNodeEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.ovsdb.node.attributes.ManagedNodeEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

public class OpenVSwitchUpdateCommand extends AbstractTransactionCommand{

    private static final Logger LOG = LoggerFactory.getLogger(OpenVSwitchUpdateCommand.class);

    public OpenVSwitchUpdateCommand(OvsdbClientKey key, TableUpdates updates,
            DatabaseSchema dbSchema){
        super(key,updates,dbSchema);
    }

    @Override
    public void execute(ReadWriteTransaction transaction) {
        Map<UUID,OpenVSwitch> updatedOpenVSwitchRows =
                TyperUtils.extractRowsUpdated(OpenVSwitch.class, getUpdates(), getDbSchema());

        for (Entry<UUID, OpenVSwitch> entry : updatedOpenVSwitchRows.entrySet()){
            OpenVSwitch openVSwitch = entry.getValue();
            final InstanceIdentifier<Node> nodePath = getKey().toInstanceIndentifier();
            Optional<Node> node = Optional.absent();
            try {
                node = transaction.read(LogicalDatastoreType.OPERATIONAL, nodePath).checkedGet();
            } catch (final ReadFailedException e) {
                LOG.debug("Read Operational/DS for Node fail! {}", nodePath, e);
            }
            if (node.isPresent()){
                LOG.debug("Node {} is present",node);
                NodeId manageNodeId = SouthboundMapper.createManagedNodeId(getKey(), new OvsdbBridgeRef(nodePath));
                InstanceIdentifier<Node> managedNodePath = SouthboundMapper.createInstanceIdentifier(manageNodeId);
                NodeBuilder nodeBuilder = new NodeBuilder();
                nodeBuilder.setNodeId(SouthboundMapper.createNodeId(getKey().getIp(),getKey().getPort()));
                OvsdbNodeAugmentationBuilder ovsdbNodeBuilder = new OvsdbNodeAugmentationBuilder();
                ovsdbNodeBuilder.setNodeUuid(new Uuid(openVSwitch.getUuid().toString()));
                List<ManagedNodeEntry> managedNodes = new ArrayList<ManagedNodeEntry>();
                ManagedNodeEntry managedNodeEntry = new ManagedNodeEntryBuilder().setBridgeRef(new OvsdbBridgeRef(managedNodePath)).build();
                managedNodes.add(managedNodeEntry);
                ovsdbNodeBuilder.setManagedNodeEntry(managedNodes);
                nodeBuilder.addAugmentation(OvsdbNodeAugmentation.class, ovsdbNodeBuilder.build());

                LOG.debug("Update node with managed node ref {}",ovsdbNodeBuilder.toString());
                transaction.merge(LogicalDatastoreType.OPERATIONAL, nodePath, nodeBuilder.build());
            }
        }
    }
}