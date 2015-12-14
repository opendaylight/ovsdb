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
import org.opendaylight.ovsdb.hwvtepsouthbound.HwvtepSouthboundMapper;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.lib.schema.typed.TyperUtils;
import org.opendaylight.ovsdb.schema.hardwarevtep.PhysicalSwitch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepPhysicalSwitchRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.Switches;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.SwitchesKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class PhysicalSwitchRemoveCommand extends AbstractTransactionCommand {

    public PhysicalSwitchRemoveCommand(HwvtepConnectionInstance key, TableUpdates updates, DatabaseSchema dbSchema) {
        super(key, updates, dbSchema);
    }

    @Override
    public void execute(ReadWriteTransaction transaction) {
        Collection<PhysicalSwitch> deletedPSRows =
                TyperUtils.extractRowsRemoved(PhysicalSwitch.class, getUpdates(), getDbSchema()).values();
        for (PhysicalSwitch pSwitch : deletedPSRows) {
            InstanceIdentifier<Node> nodeIid = HwvtepSouthboundMapper.createInstanceIdentifier(
                    getOvsdbConnectionInstance(), pSwitch);
            InstanceIdentifier<Switches> switchIid = getOvsdbConnectionInstance().getInstanceIdentifier()
                    .augmentation(HwvtepGlobalAugmentation.class)
                    .child(Switches.class, new SwitchesKey(new HwvtepPhysicalSwitchRef(nodeIid)));
            // TODO handle removal of reference to managed switch from model
            transaction.delete(LogicalDatastoreType.OPERATIONAL, nodeIid);
            transaction.delete(LogicalDatastoreType.OPERATIONAL, switchIid);
        }
    }

}
