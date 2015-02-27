/*
* Copyright (c) 2014 Intel Corp. and others.  All rights reserved.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v1.0 which accompanies this distribution,
* and is available at http://www.eclipse.org/legal/epl-v10.html
*/

package org.opendaylight.ovsdb.southbound.transactions.md;


import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeBase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.InterfaceTypeVxlan;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentationBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointBuilder;
import org.opendaylight.yangtools.yang.binding.Augmentation;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;

public class OvsdbPortUpdateCommand extends AbstractTransactionCommand {
    private static final Logger LOG = LoggerFactory.getLogger(OvsdbPortUpdateCommand.class);

    public OvsdbPortUpdateCommand(OvsdbClientKey key, TableUpdates updates,
            DatabaseSchema dbSchema) {
        super(key,updates,dbSchema);
    }

    @Override
    public void execute(ReadWriteTransaction transaction) {
        Collection<Port> updatedRows = TyperUtils.extractRowsUpdated(Port.class, getUpdates(), getDbSchema()).values();
        for(Port port : updatedRows) {
        	final InstanceIdentifier<OvsdbNodeAugmentation> nodePath = getKey().toInstanceIndentifier().augmentation(OvsdbNodeAugmentation.class);
                Optional<OvsdbNodeAugmentation> node = Optional.absent();
            try{
                node = transaction.read(LogicalDatastoreType.OPERATIONAL, nodePath).checkedGet();
            }catch (final ReadFailedException e) {
                LOG.debug("Read Operational/DS for Node fail! {}", nodePath, e);
            }
            if(node.isPresent()){            	
            	NodeBuilder nodeBuilder = new NodeBuilder();
            	nodeBuilder.setNodeId(SouthboundMapper.createNodeId(getKey().getIp(),getKey().getPort()));

                OvsdbTerminationPointAugmentationBuilder ovsdbTerminationPointBuilder = new OvsdbTerminationPointAugmentationBuilder();
                List<TerminationPoint> tpList = new ArrayList<TerminationPoint>();
                TerminationPointBuilder entry = new TerminationPointBuilder();
             //   entry.setKey(value);
             //   entry.setTpId(value);
             //   entry.setTpRef(value);
                ovsdbTerminationPointBuilder.setName(port.getName());
                ovsdbTerminationPointBuilder.setPortUuid(new Uuid(port.getUuid().toString())); 
              //  ovsdbTerminationPointBuilder.setInterfaceUuid(new Uuid(port.getUuid().toString()));
              //  ovsdbTerminationPointBuilder.setInterfaceType(new InterfaceTypeBase());
                entry.addAugmentation((Class<? extends Augmentation<TerminationPoint>>) OvsdbTerminationPointAugmentation.class, ovsdbTerminationPointBuilder.build());
                
                tpList.add(entry.build());
                nodeBuilder.setTerminationPoint(tpList);
                transaction.merge(LogicalDatastoreType.OPERATIONAL, getKey().toInstanceIndentifier(), nodeBuilder.build());
            }            
        }
    }
}
