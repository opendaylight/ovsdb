/*
 * Copyright (c) 2015 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.hwvtepsouthbound.transactions.md;

import java.util.Collection;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepConnectionInstance;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.hardwarevtep.PhysicalPort;
import org.opendaylight.ovsdb.utils.mdsal.utils.TransactionType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.physical.port.attributes.VlanBindings;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HwvtepPhysicalPortRemoveCommand extends AbstractTransactionCommand {

    private static final Logger LOG = LoggerFactory.getLogger(HwvtepPhysicalPortRemoveCommand.class);

    public HwvtepPhysicalPortRemoveCommand(HwvtepConnectionInstance key, TableUpdates updates,
            DatabaseSchema dbSchema) {
        super(key, updates, dbSchema);
    }

    InstanceIdentifier<TerminationPoint> getDeviceOpKey(UUID uuid) {
        return (InstanceIdentifier<TerminationPoint>)getOvsdbConnectionInstance()
                .getDeviceInfo().getDeviceOperKey(VlanBindings.class, uuid);
    }

    @Override
    public void execute(ReadWriteTransaction transaction) {
        Collection<PhysicalPort> deletedPortRows =
                TyperUtils.extractRowsRemoved(PhysicalPort.class, getUpdates(), getDbSchema()).values();
        for (PhysicalPort physicalPort : deletedPortRows) {
            InstanceIdentifier<TerminationPoint> nodePath = getDeviceOpKey(physicalPort.getUuid());
            if (nodePath != null) {
                addToDeviceUpdate(TransactionType.DELETE, physicalPort);
                addToDeviceUpdate(TransactionType.DELETE, nodePath.firstKeyOf(Node.class).getNodeId().getValue());
                transaction.delete(LogicalDatastoreType.OPERATIONAL, nodePath);
            } else {
                LOG.error("Failed to find the node path for deleted port {} for node {}", physicalPort,
                        getOvsdbConnectionInstance().getInstanceIdentifier());
            }
        }
    }
}
