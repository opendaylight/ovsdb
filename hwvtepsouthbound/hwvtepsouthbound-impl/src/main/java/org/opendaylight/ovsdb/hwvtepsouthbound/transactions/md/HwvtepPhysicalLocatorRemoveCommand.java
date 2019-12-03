/*
 * Copyright (c) 2015 - 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
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
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepSouthboundMapper;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.schema.hardwarevtep.PhysicalLocator;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class HwvtepPhysicalLocatorRemoveCommand extends AbstractTransactionCommand {
    public HwvtepPhysicalLocatorRemoveCommand(final HwvtepConnectionInstance key, final TableUpdates updates,
            final DatabaseSchema dbSchema) {
        super(key, updates, dbSchema);
    }

    @Override
    public void execute(final ReadWriteTransaction transaction) {
        Collection<PhysicalLocator> deletedPLRows = extractRowsRemoved(PhysicalLocator.class).values();
        for (PhysicalLocator locator : deletedPLRows) {
            final InstanceIdentifier<Node> connectionIId = getOvsdbConnectionInstance().getInstanceIdentifier();
            final InstanceIdentifier<TerminationPoint> nodePath = HwvtepSouthboundMapper
                    .createInstanceIdentifier(connectionIId, locator);
            transaction.delete(LogicalDatastoreType.OPERATIONAL, nodePath);
            getOvsdbConnectionInstance().getDeviceInfo().clearDeviceOperData(TerminationPoint.class, nodePath);

            //TODO: Check if any cleanup is required
        }
    }
}
