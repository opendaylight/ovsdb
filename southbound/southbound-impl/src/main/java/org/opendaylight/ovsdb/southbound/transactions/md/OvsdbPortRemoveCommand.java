/*
 * Copyright (c) 2015 Intel Corp. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound.transactions.md;

import java.util.Map;
import java.util.Map.Entry;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.schema.openvswitch.Bridge;
import org.opendaylight.ovsdb.schema.openvswitch.Port;
import org.opendaylight.ovsdb.southbound.SouthboundMapper;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OvsdbPortRemoveCommand extends AbstractTransactionComponent {
    private static final Logger LOG = LoggerFactory.getLogger(OvsdbPortRemoveCommand.class);

    public OvsdbPortRemoveCommand() {
        super(Port.class, Bridge.class);
    }

    @Override
    public void execute(final OvsdbTransactionContext context) {
        final Map<UUID, Port> portRemovedRows = context.getRemovedRows(Port.class);
        if (portRemovedRows.isEmpty()) {
            LOG.debug("No ports have been updated");
            return;
        }


        Map<UUID, Port> portUpdatedRows = context.getUpdatedRows(Port.class);
        Map<UUID,Bridge> bridgeUpdatedRows = context.getUpdatedRows(Bridge.class);
        Map<UUID,Bridge> bridgeUpdatedOldRows = context.getOldRows(Bridge.class);
        for (Port port : portRemovedRows.values()) {
            final String portName = port.getName();
            boolean isPortInUpdatedRows = portUpdatedRows.values()
                .stream().anyMatch(updatedPort -> portName.equals(updatedPort.getName()));
            if (isPortInUpdatedRows) {
                LOG.debug("port {} present in updated rows, skipping delete", portName);
                continue;
            }
            Bridge updatedBridgeData = null;
            for (Entry<UUID, Bridge> entry : bridgeUpdatedOldRows.entrySet()) {
                UUID bridgeUuid = entry.getKey();
                Bridge oldBridgeData = entry.getValue();
                if (oldBridgeData.getPortsColumn() != null
                        && oldBridgeData.getPortsColumn().getData().contains(port.getUuidColumn().getData())
                        && ! bridgeUpdatedRows.isEmpty()) {
                    updatedBridgeData = bridgeUpdatedRows.get(bridgeUuid);
                    break;
                }
            }
            if (updatedBridgeData == null) {
                LOG.warn("Bridge not found for port {}",port);
                continue;
            }
            final InstanceIdentifier<TerminationPoint> nodePath = SouthboundMapper
                    .createInstanceIdentifier(instanceIdentifierCodec, getOvsdbConnectionInstance(),
                            updatedBridgeData).child(
                            TerminationPoint.class,
                            new TerminationPointKey(new TpId(portName)));
            transaction.delete(LogicalDatastoreType.OPERATIONAL, nodePath);
        }
    }

}
