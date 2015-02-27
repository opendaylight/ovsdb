/*
* Copyright (c) 2014 Intel Corp. and others.  All rights reserved.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v1.0 which accompanies this distribution,
* and is available at http://www.eclipse.org/legal/epl-v10.html
*/

package org.opendaylight.ovsdb.southbound.transactions.md;

import java.util.Collection;

import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.openvswitch.Port;
import org.opendaylight.ovsdb.southbound.OvsdbClientKey;
import org.opendaylight.ovsdb.southbound.SouthboundMapper;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

public class OvsdbPortUpdateCommand extends AbstractTransactionCommand {
    private static final Logger LOG = LoggerFactory.getLogger(OvsdbPortUpdateCommand.class);

    public OvsdbPortUpdateCommand(OvsdbClientKey key, TableUpdates updates,
            DatabaseSchema dbSchema) {
        super(key,updates,dbSchema);
        LOG.info("sharad - calling port update constructor");
    }

    @Override
    public void execute(ReadWriteTransaction transaction) {
        LOG.info("sharad - calling port update execute");
        Collection<Port> updatedRows = TyperUtils.extractRowsUpdated(Port.class, getUpdates(), getDbSchema()).values();
        LOG.info("sharad - updated rows are {} ", updatedRows);
        for(Port port : updatedRows) {
            LOG.info("sharad - port is  {} ", port);
            final InstanceIdentifier<Node> nodePath = getKey().toInstanceIndentifier();
            LOG.info("sharad - node paths are {} ", nodePath);
            Optional<Node> node = Optional.absent();
            try{
                node = transaction.read(LogicalDatastoreType.OPERATIONAL, nodePath).checkedGet();
                LOG.info("sharad - node is {} ", node);
            }catch (final ReadFailedException e) {
                LOG.debug("Read Operational/DS for Node fail! {}", nodePath, e);
            }
            LOG.info("sharad - check if node present");
            if(node.isPresent()){
                LOG.info("Node {} is present",node);
                NodeBuilder managedNodeBuilder = new NodeBuilder();
                NodeId manageNodeId = SouthboundMapper.createManagedNodeId(getKey(), port.getUuid());
                managedNodeBuilder.setNodeId(manageNodeId);

                OvsdbTerminationPointAugmentationBuilder ovsdbTerminationPointBuilder = new OvsdbTerminationPointAugmentationBuilder();
                ovsdbTerminationPointBuilder.setName(port.getName());
                ovsdbTerminationPointBuilder.setPortUuid(new Uuid(port.getUuid().toString()));
             //   ovsdbTerminationPointBuilder.setManagedBy(new OvsdbNodeRef(nodePath));
             //   managedNodeBuilder.addAugmentation(OvsdbTerminationPointAugmentation.class, ovsdbTerminationPointBuilder.build());

                InstanceIdentifier<Node> managedNodePath = SouthboundMapper.createInstanceIdentifier(manageNodeId);

          //      LOG.debug("Store managed node augmentation data {}",ovsdbManagedNodeBuilder.toString());
                transaction.put(LogicalDatastoreType.OPERATIONAL, managedNodePath, managedNodeBuilder.build());

                //Update node with managed node reference
                NodeBuilder nodeBuilder = new NodeBuilder();
                nodeBuilder.setNodeId(SouthboundMapper.createNodeId(getKey().getIp(),getKey().getPort()));

                OvsdbTerminationPointAugmentationBuilder ovsdbNodeBuilder = new OvsdbTerminationPointAugmentationBuilder();
            //    List<ManagedNodeEntry> managedNodes = new ArrayList<ManagedNodeEntry>();
           //     ManagedNodeEntry entry = new ManagedNodeEntryBuilder().setBridgeRef(new OvsdbBridgeRef(managedNodePath)).build();
           //     managedNodes.add(entry);
         //       ovsdbNodeBuilder.setManagedNodeEntry(managedNodes);
                LOG.info("sharad - ovsdbnodebuilder is {} ", ovsdbNodeBuilder);
            //    nodeBuilder.addAugmentation(OvsdbTerminationPointAugmentation.class, ovsdbNodeBuilder.build());

                LOG.debug("Update node with managed node ref {}",ovsdbNodeBuilder.toString());
                transaction.merge(LogicalDatastoreType.OPERATIONAL, nodePath, nodeBuilder.build());

            }
        }
    }
}
