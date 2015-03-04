/*
 * Copyright (c) 2015 Intel Corp. and others.  All rights reserved.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.southbound.transactions.md;

//import java.util.ArrayList;
import java.util.Collection;
//import java.util.List;

import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.openvswitch.Port;
import org.opendaylight.ovsdb.schema.openvswitch.Interface;
import org.opendaylight.ovsdb.southbound.OvsdbClientKey;
import org.opendaylight.ovsdb.southbound.SouthboundMapper;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbInterfaceRef;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

public class OvsdbInterfaceUpdateCommand extends AbstractTransactionCommand {
    private static final Logger LOG = LoggerFactory.getLogger(OvsdbInterfaceUpdateCommand.class);

    public OvsdbInterfaceUpdateCommand(OvsdbClientKey key, TableUpdates updates,
            DatabaseSchema dbSchema) {
        super(key,updates,dbSchema);
        LOG.info("Interface update constructor");
    }

    @Override
    public void execute(ReadWriteTransaction transaction) {
        Collection<Interface> updatedRows = TyperUtils.extractRowsUpdated(Interface.class, getUpdates(), getDbSchema()).values();       
        for(Interface interface : updatedRows) {
            final InstanceIdentifier<Node> nodePath = getKey().toInstanceIndentifier();
            Optional<Node> node = Optional.absent();
            try{
                LOG.info("Interface Before Read");
                node = transaction.read(LogicalDatastoreType.OPERATIONAL, nodePath).checkedGet();
                LOG.info("Interface After read");
            }catch (final ReadFailedException e) {
                LOG.debug("Read Operational/DS for Node fail! {}", nodePath, e);
            }
            if(node.isPresent()){
                LOG.info("Node {} is present",node);
                NodeBuilder managedNodeBuilder = new NodeBuilder();
                NodeId manageNodeId = SouthboundMapper.createManagedNodeId(getKey(), Interface.getUuid());
                managedNodeBuilder.setNodeId(manageNodeId);
                
                OvsdbTerminationPointAugmentationBuilder ovsdbTerminationPointBuilder = new OvsdbTermnationPointAugmentationBuilder();
                ovsdbTerminationPointBuilder.setName(interface.getName());
                ovsdbTerminationPointBuilder.setInterfaceUuid(new Uuid(interface.getUuid().toString()));
                ovsdbTerminationPointBuilder.setInterfaceType(new org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeBase(interface.getInterfaceType()));                //
                //org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbPortInterfaceAttributes)arg).getInterfaceType();

                InstanceIdentifier<Node> managedNodePath = SouthboundMapper.createInstanceIdentifier(manageNodeId); 
                LOG.debug("Store managed node augmentation data {}",ovsdbManagedNodeBuilder.toString());
                transaction.put(LogicalDatastoreType.OPERATIONAL, managedNodePath, managedNodeBuilder.build());
                //Update node with managed node reference
                NodeBuilder nodeBuilder = new NodeBuilder();
                nodeBuilder.setNodeId(SouthboundMapper.createNodeId(getKey().getIp(),getKey().getPort()));

                OvsdbTerminationPointAugmentationBuilder ovsdbTerminationBuilder = new OvsdbTerminationPointAugmentationBuilder();
                LOG.info("ovsdbTerminationBuilder is {} ", ovsdbTerminationBuilder);
                //ovsdbTerminationBuilder.setManagedNodeEntry(managedNodes);

                //nodeBuilder.addAugmentation(OvsdbNodeAugmentation.class, ovsdbNodeBuilder.build());

                LOG.debug("Update node with TerminationPoint Interface ref {}",ovsdbNodeBuilder.toString());
                transaction.merge(LogicalDatastoreType.OPERATIONAL, nodePath, nodeBuilder.build());

            }
        }
    }
}
