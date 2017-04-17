/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.hwvtepsouthbound;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.ovsdb.hwvtepsouthbound.transact.HwvtepOperationalState;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.operations.Operations;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepNodeName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LogicalSwitches;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LogicalSwitchesKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({HwvtepConnectionInstance.class, HwvtepConnectionManager.class, Operations.class})
public class HwvtepOperationalStateTest extends DataChangeListenerTestBase {

    UUID uuid = new UUID("ls0");

    @Test
    public void testUpdateCurrentTxData() throws Exception {
        InstanceIdentifier<LogicalSwitches> lsIid = nodeIid.augmentation(HwvtepGlobalAugmentation.class).
                child(LogicalSwitches.class, new LogicalSwitchesKey(new HwvtepNodeName("ls0")));

        HwvtepOperationalState opState = new HwvtepOperationalState(connectionInstance);

        UUID resultUuid = opState.getUUIDFromCurrentTx(LogicalSwitches.class, lsIid);
        assertNull(resultUuid);

        opState.updateCurrentTxData(LogicalSwitches.class, lsIid, uuid);
        resultUuid = opState.getUUIDFromCurrentTx(LogicalSwitches.class, lsIid);
        assertEquals(uuid, resultUuid);

        boolean result = opState.getDeviceInfo().isKeyInTransit(LogicalSwitches.class, lsIid);
        assertFalse(result);

        opState.getDeviceInfo().markKeyAsInTransit(LogicalSwitches.class, lsIid);
        result = opState.getDeviceInfo().isKeyInTransit(LogicalSwitches.class, lsIid);
        assertTrue(result);

        opState.getDeviceInfo().updateDeviceOperData(LogicalSwitches.class, lsIid, uuid, lsIid);
        result = opState.getDeviceInfo().isKeyInTransit(LogicalSwitches.class, lsIid);
        assertFalse(result);

        result = opState.getDeviceInfo().isConfigDataAvailable(LogicalSwitches.class, lsIid);
        assertFalse(result);

        opState.getDeviceInfo().updateConfigData(LogicalSwitches.class, lsIid, null);
        result = opState.getDeviceInfo().isConfigDataAvailable(LogicalSwitches.class, lsIid);
        assertTrue(result);
    }
}
