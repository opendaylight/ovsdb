/*
 * Copyright (c) 2015 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.hwvtepsouthbound.transactions.md;

import java.util.Collection;
import java.util.Map;
import org.opendaylight.mdsal.binding.api.ReadWriteTransaction;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepConnectionInstance;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepSouthboundMapper;
import org.opendaylight.ovsdb.hwvtepsouthbound.events.PortEvent;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.hardwarevtep.PhysicalPort;
import org.opendaylight.ovsdb.schema.hardwarevtep.PhysicalSwitch;
import org.opendaylight.ovsdb.utils.mdsal.utils.TransactionType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.physical.port.attributes.VlanBindings;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HwvtepPhysicalPortRemoveCommand extends AbstractTransactionCommand {

    private static final Logger LOG = LoggerFactory.getLogger(HwvtepPhysicalPortRemoveCommand.class);

    public HwvtepPhysicalPortRemoveCommand(HwvtepConnectionInstance key, TableUpdates updates,
            DatabaseSchema dbSchema) {
        super(key, updates, dbSchema);
    }

    @Override
    public void execute(ReadWriteTransaction transaction) {
        Collection<PhysicalPort> deletedPortRows =
                TyperUtils.extractRowsRemoved(PhysicalPort.class, getUpdates(), getDbSchema()).values();
        Map<UUID, PhysicalSwitch> updatedPSRows =
                TyperUtils.extractRowsUpdated(PhysicalSwitch.class, getUpdates(), getDbSchema());
        Map<UUID, PhysicalSwitch> oldPSRows =
                TyperUtils.extractRowsOld(PhysicalSwitch.class, getUpdates(), getDbSchema());
        for (PhysicalPort port : deletedPortRows) {
            PhysicalSwitch updatedPSwitchData = null;
            for (Map.Entry<UUID, PhysicalSwitch> updatedEntry : updatedPSRows.entrySet()) {
                UUID switchUUID = updatedEntry.getKey();
                PhysicalSwitch oldPSwitchData = oldPSRows.get(switchUUID);
                if (oldPSwitchData.getPortsColumn() != null
                        && oldPSwitchData.getPortsColumn().getData().contains(port.getUuidColumn().getData())
                        && !updatedPSRows.isEmpty()) {
                    updatedPSwitchData = updatedEntry.getValue();
                    break;
                }
            }
            if (updatedPSwitchData == null) {
                LOG.warn("PhysicalSwitch not found for port {}", port);
            } else {
                String portName = port.getName();
                final InstanceIdentifier<TerminationPoint> nodePath =
                        HwvtepSouthboundMapper.createInstanceIdentifier(getOvsdbConnectionInstance(),
                                updatedPSwitchData).child(TerminationPoint.class,
                                        new TerminationPointKey(new TpId(portName)));
                addToDeleteTx(transaction, VlanBindings.class, nodePath, port.getUuid());
                addToDeviceUpdate(TransactionType.DELETE,
                        new PortEvent(port, nodePath.firstKeyOf(Node.class).getNodeId()));
                getDeviceInfo().clearDeviceOperData(TerminationPoint.class, nodePath);
            }
        }
    }
}
