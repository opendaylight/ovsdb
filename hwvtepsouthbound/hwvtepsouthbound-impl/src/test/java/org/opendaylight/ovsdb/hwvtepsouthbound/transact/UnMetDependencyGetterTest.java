/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.hwvtepsouthbound.transact;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.opendaylight.ovsdb.hwvtepsouthbound.DataChangeListenerTestBase;
import org.opendaylight.ovsdb.hwvtepsouthbound.TestBuilders;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepNodeName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LogicalSwitches;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LogicalSwitchesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.RemoteMcastMacs;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.binding.EntryObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

@RunWith(MockitoJUnitRunner.Silent.class)
public class UnMetDependencyGetterTest extends DataChangeListenerTestBase {

    UnMetDependencyGetter<RemoteMcastMacs> mcastMacDataValidator;
    HwvtepOperationalState opState;
    RemoteMcastMacs mac;
    InstanceIdentifier<LogicalSwitches> lsIid;
    Map<Class<? extends EntryObject<?, ?>>, List<InstanceIdentifier>> unMetDependencies;

    void setupForTest() {
        mcastMacDataValidator = McastMacsRemoteUpdateCommand.MCAST_MAC_DATA_VALIDATOR;
        opState = new HwvtepOperationalState(connectionInstance);
        mac = TestBuilders.buildRemoteMcastMacs(nodeIid, "FF:FF:FF:FF:FF:FF", "ls0",
                new String[] {"192.168.122.20", "192.168.122.30"});
        lsIid = nodeIid.augmentation(HwvtepGlobalAugmentation.class)
                .child(LogicalSwitches.class, new LogicalSwitchesKey(new HwvtepNodeName("ls0")));
    }

    @Test
    public void testLogicalSwitchConfigDependency() throws Exception {
        setupForTest();
        unMetDependencies = mcastMacDataValidator.getInTransitDependencies(opState, mac);
        assertEquals(0, unMetDependencies.size());

        unMetDependencies = mcastMacDataValidator.getUnMetConfigDependencies(opState, mac);
        assertEquals(1, unMetDependencies.get(LogicalSwitches.class).size());
        assertEquals(2, unMetDependencies.get(TerminationPoint.class).size());

        opState.getDeviceInfo().updateConfigData(LogicalSwitches.class, lsIid, "ls0");
        unMetDependencies = mcastMacDataValidator.getUnMetConfigDependencies(opState, mac);
        assertNull(unMetDependencies.get(LogicalSwitches.class));
    }

    @Test
    public void testLogicalSwitchInTransitDependency() throws Exception {
        setupForTest();
        unMetDependencies = mcastMacDataValidator.getInTransitDependencies(opState, mac);
        assertEquals(0, unMetDependencies.size());

        opState.getDeviceInfo().markKeyAsInTransit(LogicalSwitches.class, lsIid);
        unMetDependencies = mcastMacDataValidator.getInTransitDependencies(opState, mac);
        assertEquals(1, unMetDependencies.size());

        opState.getDeviceInfo().updateDeviceOperData(LogicalSwitches.class, lsIid, new UUID("ls0"), "ls0");
        unMetDependencies = mcastMacDataValidator.getInTransitDependencies(opState, mac);
        assertEquals(0, unMetDependencies.size());
    }
}
