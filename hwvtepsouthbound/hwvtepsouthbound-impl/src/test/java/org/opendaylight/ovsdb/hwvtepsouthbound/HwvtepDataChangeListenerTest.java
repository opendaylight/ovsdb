/*
 * Copyright (c) 2016 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.hwvtepsouthbound;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.MoreExecutors;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.opendaylight.ovsdb.hwvtepsouthbound.transact.DependencyQueue;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.operations.Operations;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.lib.schema.typed.TypedBaseTable;
import org.opendaylight.ovsdb.schema.hardwarevtep.LogicalSwitch;
import org.opendaylight.ovsdb.schema.hardwarevtep.McastMacsRemote;
import org.opendaylight.ovsdb.schema.hardwarevtep.UcastMacsRemote;
import org.opendaylight.ovsdb.utils.mdsal.utils.MdsalUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepNodeName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LogicalSwitches;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LogicalSwitchesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.RemoteMcastMacs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.RemoteUcastMacs;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.List;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType.CONFIGURATION;
import static org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType.OPERATIONAL;

/**
 * Unit tests for the data-tree change listener.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({HwvtepConnectionInstance.class, HwvtepConnectionManager.class, Operations.class})
public class HwvtepDataChangeListenerTest extends DataChangeListenerTestBase {

    static Logger LOG = LoggerFactory.getLogger(HwvtepDataChangeListenerTest.class);

    String[][] ucastMacs = new String[][]{
            {"20:00:00:00:00:01", "11.10.10.1", "192.168.122.20", "ls0"},
            {"20:00:00:00:00:02", "11.10.10.2", "192.168.122.20", "ls0"},
            {"20:00:00:00:00:03", "11.10.10.3", "192.168.122.30", "ls1"},
            {"20:00:00:00:00:04", "11.10.10.4", "192.168.122.30", "ls1"}
    };

    String[][] logicalSwitches = new String[][]{
            {"ls0", "100"},
            {"ls1", "200"},
    };

    String[][] terminationPoints = new String[][]{
            {"192.168.122.10"},
            {"192.168.122.20"},
            {"192.168.122.30"},
            {"192.168.122.40"}
    };

    String[][] mcastMacs = new String[][]{
            {"FF:FF:FF:FF:FF:FF", "ls0", "192.168.122.20", "192.168.122.30"},
            {"FF:FF:FF:FF:FF:FF", "ls1", "192.168.122.10", "192.168.122.30"}
    };

    String[][] mcastMac2 = new String[][]{
            {"FF:FF:FF:FF:FF:FF", "ls0", "192.168.122.20", "192.168.122.10"},
            {"FF:FF:FF:FF:FF:FF", "ls1", "192.168.122.10", "192.168.122.20"}
    };

    @Test
    public <T extends DataObject> void testLogicalSwitchAdd() throws Exception {
        addData(CONFIGURATION, LogicalSwitches.class, logicalSwitches);
        verifyThatLogicalSwitchCreated();
    }

    @Test
    public <T extends DataObject> void testUcastMacAdd() throws Exception {
        addData(CONFIGURATION, LogicalSwitches.class, logicalSwitches);
        addData(OPERATIONAL, LogicalSwitches.class, logicalSwitches);
        connectionInstance.getDeviceInfo().updateDeviceOpData(LogicalSwitches.class, ls0Iid, new UUID("ls0"), "ls0");
        connectionInstance.getDeviceInfo().updateDeviceOpData(LogicalSwitches.class, ls1Iid, new UUID("ls1"), "ls1");
        resetOperations();
        addData(CONFIGURATION, TerminationPoint.class, terminationPoints);
        addData(CONFIGURATION, RemoteUcastMacs.class, ucastMacs);
        //4 ucast macs + 2 termination points
        verify(Operations.op,  times(6)).insert(any(UcastMacsRemote.class));
        //TODO add finer grained validation
    }

    @Test
    public <T extends DataObject> void testMcastMacAdd() throws Exception {
        addData(CONFIGURATION, LogicalSwitches.class, logicalSwitches);
        addData(OPERATIONAL, LogicalSwitches.class, logicalSwitches);
        connectionInstance.getDeviceInfo().updateDeviceOpData(LogicalSwitches.class, ls0Iid, new UUID("ls0"), "ls0");
        connectionInstance.getDeviceInfo().updateDeviceOpData(LogicalSwitches.class, ls1Iid, new UUID("ls1"), "ls0");
        resetOperations();
        addData(CONFIGURATION, TerminationPoint.class, terminationPoints);
        addData(CONFIGURATION, RemoteMcastMacs.class, mcastMacs);
        //2 mcast macs + 2 locator sets + 3 termination points
        verify(Operations.op,  times(7)).insert(Matchers.<McastMacsRemote>any());
    }

    @Test
    public <T extends DataObject> void testAddMacs() throws Exception {
        Node node = addData(CONFIGURATION, LogicalSwitches.class, logicalSwitches);
        addData(OPERATIONAL, LogicalSwitches.class, logicalSwitches);
        connectionInstance.getDeviceInfo().updateDeviceOpData(LogicalSwitches.class, ls0Iid, new UUID("ls0"), "ls0");
        connectionInstance.getDeviceInfo().updateDeviceOpData(LogicalSwitches.class, ls1Iid, new UUID("ls1"), "ls0");
        resetOperations();
        addData(CONFIGURATION, TerminationPoint.class, terminationPoints);
        addData(CONFIGURATION, RemoteUcastMacs.class, ucastMacs);
        verify(Operations.op,  times(6)).insert(any(UcastMacsRemote.class));

        addData(OPERATIONAL, TerminationPoint.class, terminationPoints);
        addData(OPERATIONAL, RemoteUcastMacs.class, ucastMacs);
        node = new MdsalUtils(dataBroker).read(CONFIGURATION, nodeIid);
        for ( TerminationPoint tp : node.getTerminationPoint()) {
            InstanceIdentifier<TerminationPoint> tpIid = nodeIid.child(TerminationPoint.class, tp.getKey());
            connectionInstance.getDeviceInfo().updateDeviceOpData(TerminationPoint.class, tpIid,
                    new UUID("tp1"), "tp");
        }
        resetOperations();
        addData(CONFIGURATION, RemoteMcastMacs.class, mcastMacs);
        //2 mcast mac + 2 locator sets ( termination point already added )
        verify(Operations.op,  times(4)).insert(Matchers.<McastMacsRemote>any());
    }

    @Test
    public <T extends DataObject> void testBackToBackMacsUpdate() throws Exception {
        Whitebox.setInternalState(DependencyQueue.class, "executorService", MoreExecutors.sameThreadExecutor());

        Node node = addData(CONFIGURATION, LogicalSwitches.class, logicalSwitches);
        addData(OPERATIONAL, LogicalSwitches.class, logicalSwitches);
        connectionInstance.getDeviceInfo().updateDeviceOpData(LogicalSwitches.class, ls0Iid, new UUID("ls0"), "ls0");
        connectionInstance.getDeviceInfo().updateDeviceOpData(LogicalSwitches.class, ls1Iid, new UUID("ls1"), "ls0");
        resetOperations();
        addData(CONFIGURATION, TerminationPoint.class, terminationPoints);
        addData(CONFIGURATION, RemoteUcastMacs.class, ucastMacs);
        verify(Operations.op,  times(6)).insert(any(UcastMacsRemote.class));

        addData(OPERATIONAL, TerminationPoint.class, terminationPoints);
        addData(OPERATIONAL, RemoteUcastMacs.class, ucastMacs);
        node = new MdsalUtils(dataBroker).read(CONFIGURATION, nodeIid);

        resetOperations();
        addData(CONFIGURATION, RemoteMcastMacs.class, mcastMacs);
        //2 mcast mac + 2 locator sets ( termination point already added )
        verify(Operations.op,  times(0)).insert(Matchers.<McastMacsRemote>any());
        resetOperations();
        for ( TerminationPoint tp : node.getTerminationPoint()) {
            InstanceIdentifier<TerminationPoint> tpIid = nodeIid.child(TerminationPoint.class, tp.getKey());
            connectionInstance.getDeviceInfo().updateDeviceOpData(TerminationPoint.class, tpIid,
                    new UUID("tp1"), "tp");
        }
        connectionInstance.getDeviceInfo().onOpDataAvailable();
        //2 mcast mac + 2 locator sets ( termination point already added )
        verify(Operations.op,  times(4)).insert(Matchers.<McastMacsRemote>any());

        resetOperations();
        node = addData(CONFIGURATION, RemoteMcastMacs.class, mcastMac2);
        verify(Operations.op,  times(0)).insert(Matchers.<McastMacsRemote>any());
        HwvtepGlobalAugmentation augmentation = node.getAugmentation(HwvtepGlobalAugmentation.class);
        for (RemoteMcastMacs mac : augmentation.getRemoteMcastMacs()) {
            InstanceIdentifier<RemoteMcastMacs> macIid = nodeIid.augmentation(HwvtepGlobalAugmentation.class)
                    .child(RemoteMcastMacs.class, mac.getKey());
            connectionInstance.getDeviceInfo().updateDeviceOpData(RemoteMcastMacs.class, macIid,
                    new UUID("tp1"), "tp");
        }
        connectionInstance.getDeviceInfo().onOpDataAvailable();
        verify(Operations.op,  times(4)).insert(Matchers.<McastMacsRemote>any());
    }

    private void verifyThatLogicalSwitchCreated() {
        verify(ovsdbClient, times(1)).transact(any(DatabaseSchema.class), any(List.class));
        verify(Operations.op, times(2)).insert(any(LogicalSwitch.class));

        assertNotNull(insertOpCapture.getAllValues());
        assertTrue(insertOpCapture.getAllValues().size() == 2);

        List<String> expected = Lists.newArrayList("ls0", "ls1");
        Iterator<TypedBaseTable> it = insertOpCapture.getAllValues().iterator();
        while (it.hasNext()) {
            TypedBaseTable table = it.next();
            assertTrue(table instanceof LogicalSwitch);
            LogicalSwitch ls = (LogicalSwitch)table;
            assertTrue(expected.contains(ls.getName()));
            expected.remove(ls.getName());
            it.next();
        }
    }
}
