/*
 * Copyright (c) 2015 Intel Corp. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.southbound.transactions.md;

import java.util.Collection;

import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.openvswitch.Interface;
import org.opendaylight.ovsdb.southbound.OvsdbClientKey;
import org.opendaylight.ovsdb.southbound.SouthboundMapper;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OvsdbInterfaceRemovedCommand extends AbstractTransactionCommand {
    private static final Logger LOG = LoggerFactory.getLogger(OvsdbInterfaceRemovedCommand.class);

    public OvsdbInterfaceRemovedCommand(OvsdbClientKey key, TableUpdates updates,
            DatabaseSchema dbSchema) {
        super(key,updates,dbSchema);
        LOG.info("Interface Removed -- Constructor");
    }

    @Override
    public void execute(ReadWriteTransaction transaction) {
        Collection<Interface> removedRows = TyperUtils.extractRowsRemoved(Interface.class, getUpdates(), getDbSchema()).values();
        for(Interface intface : removedRows) {
            InstanceIdentifier<Node> InterfaceIid = SouthboundMapper.createInstanceIdentifier(getKey(), intface.getUuid());
            //InstanceIdentifier<ManagedNodeEntry> mnIid = SouthboundMapper.createInstanceIndentifier(getKey())
                    //.augmentation(OvsdbTerminationPointAugmentation.class)
                    //.child(ManagedNodeEntry.class, new ManagedNodeEntryKey(new OvsdbBridgeRef(InterfaceeIid)));
            // TODO handle removal of reference to managed node from model
            transaction.delete(LogicalDatastoreType.OPERATIONAL, InterfaceIid);
            //transaction.delete(LogicalDatastoreType.OPERATIONAL, mnIid);
        }
    }

}