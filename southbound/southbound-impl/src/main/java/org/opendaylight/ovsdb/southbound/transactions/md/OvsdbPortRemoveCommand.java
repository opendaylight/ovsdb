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
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.openvswitch.Port;
import org.opendaylight.ovsdb.southbound.OvsdbClientKey;
import org.opendaylight.ovsdb.southbound.SouthboundMapper;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeName;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class OvsdbPortRemoveCommand extends AbstractTransactionCommand {
    public OvsdbPortRemoveCommand(OvsdbClientKey key, TableUpdates updates,
            DatabaseSchema dbSchema) {
        super(key,updates,dbSchema);
    }

    @Override
    public void execute(ReadWriteTransaction transaction) {
        String bridgeName = null;
        String portName = null;
        Collection<Port> portRemovedRows = TyperUtils.extractRowsRemoved(Port.class, getUpdates(), getDbSchema()).values();
        Collection<Port> portUpdatedRows = TyperUtils.extractRowsUpdated(Port.class, getUpdates(), getDbSchema()).values();
        for (Port bridge : portUpdatedRows) {
            bridgeName = bridge.getName();
            for (Port port : portRemovedRows) {
                portName = port.getName();
                TpId portId = SouthboundMapper.createTerminationPointId(getKey(), new OvsdbBridgeName(bridgeName), portName);
                final InstanceIdentifier<TerminationPoint> nodePath = SouthboundMapper.createInstanceIdentifier(getKey(),
                        new OvsdbBridgeName(bridgeName)).child(TerminationPoint.class, new TerminationPointKey(portId));
                transaction.delete(LogicalDatastoreType.OPERATIONAL, nodePath);
            }
        }
    }

}
