/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.hwvtepsouthbound;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.common.collect.Lists;
import java.util.Iterator;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.opendaylight.mdsal.common.api.LogicalDatastoreType;
import org.opendaylight.ovsdb.hwvtepsouthbound.transact.DependencyQueue;
import org.opendaylight.ovsdb.lib.schema.typed.TypedBaseTable;
import org.opendaylight.ovsdb.schema.hardwarevtep.LogicalSwitch;
import org.opendaylight.ovsdb.schema.hardwarevtep.McastMacsRemote;
import org.opendaylight.ovsdb.schema.hardwarevtep.UcastMacsRemote;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LogicalSwitches;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.RemoteMcastMacs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.RemoteMcastMacsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.RemoteUcastMacs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.physical.locator.set.attributes.LocatorSet;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Unit tests for the data-tree change listener.
 */
@RunWith(MockitoJUnitRunner.Silent.class)
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

    String[][] mcastMac3WithZeroLocators = new String[][]{
            {"FF:FF:FF:FF:FF:FF", "ls0"},
            {"FF:FF:FF:FF:FF:FF", "ls1"}
    };

    HwvtepOperationalDataChangeListener opDataChangeListener;

    @Before
    public void setupListener() throws Exception {
        setFinalStatic(DependencyQueue.class, "EXECUTOR_SERVICE", mock(SameThreadScheduledExecutor.class,
                Mockito.CALLS_REAL_METHODS));
        opDataChangeListener = new HwvtepOperationalDataChangeListener(getDataBroker(), hwvtepConnectionManager,
                connectionInstance);
    }

    @After
    public void cleanupListener() throws Exception {
        opDataChangeListener.close();
    }

    @Test
    public <T extends DataObject> void testLogicalSwitchAdd() throws Exception {
        addData(LogicalDatastoreType.CONFIGURATION, LogicalSwitches.class, logicalSwitches);
        verifyThatLogicalSwitchCreated();
    }

    @Test
    public <T extends DataObject> void testLogicalSwitchDelete() throws Exception {
        addData(LogicalDatastoreType.CONFIGURATION, LogicalSwitches.class, logicalSwitches);
        addData(LogicalDatastoreType.OPERATIONAL, LogicalSwitches.class, logicalSwitches);
        resetOperations();
        deleteData(LogicalDatastoreType.CONFIGURATION, LogicalSwitches.class, logicalSwitches);
        verify(mockOp, times(10)).delete(any());
    }

    @Test
    public <T extends DataObject> void testUcastMacAdd() throws Exception {
        addData(LogicalDatastoreType.CONFIGURATION, LogicalSwitches.class, logicalSwitches);
        addData(LogicalDatastoreType.OPERATIONAL, LogicalSwitches.class, logicalSwitches);
        resetOperations();
        addData(LogicalDatastoreType.CONFIGURATION, TerminationPoint.class, terminationPoints);
        addData(LogicalDatastoreType.CONFIGURATION, RemoteUcastMacs.class, ucastMacs);
        //4 ucast macs + 2 termination points
        verify(mockOp, times(4)).insert(any(UcastMacsRemote.class));
        //TODO add finer grained validation
    }

    @Test
    public <T extends DataObject> void testUcastMacAddWithoutConfigTep() throws Exception {
        addData(LogicalDatastoreType.CONFIGURATION, LogicalSwitches.class, logicalSwitches);
        addData(LogicalDatastoreType.OPERATIONAL, LogicalSwitches.class, logicalSwitches);
        resetOperations();
        addData(LogicalDatastoreType.CONFIGURATION, RemoteUcastMacs.class, ucastMacs);
        //4 ucast macs + 2 termination points
        verify(mockOp, times(4)).insert(any(UcastMacsRemote.class));
        //TODO add finer grained validation
    }

    @Test
    public <T extends DataObject> void testUcastMacDelete() throws Exception {
        addData(LogicalDatastoreType.CONFIGURATION, LogicalSwitches.class, logicalSwitches);
        addData(LogicalDatastoreType.OPERATIONAL, LogicalSwitches.class, logicalSwitches);
        addData(LogicalDatastoreType.CONFIGURATION, TerminationPoint.class, terminationPoints);
        addData(LogicalDatastoreType.CONFIGURATION, RemoteUcastMacs.class, ucastMacs);
        addData(LogicalDatastoreType.OPERATIONAL, RemoteUcastMacs.class, ucastMacs);
        addData(LogicalDatastoreType.OPERATIONAL, TerminationPoint.class, terminationPoints);

        resetOperations();
        deleteData(LogicalDatastoreType.CONFIGURATION, RemoteUcastMacs.class, ucastMacs);
        verify(mockOp, times(4)).delete(any());
        //TODO add finer grained validation
    }

    @Test
    public <T extends DataObject> void testMcastMacAdd() throws Exception {
        addData(LogicalDatastoreType.CONFIGURATION, LogicalSwitches.class, logicalSwitches);
        addData(LogicalDatastoreType.OPERATIONAL, LogicalSwitches.class, logicalSwitches);
        resetOperations();
        addData(LogicalDatastoreType.CONFIGURATION, TerminationPoint.class, terminationPoints);
        addData(LogicalDatastoreType.CONFIGURATION, RemoteMcastMacs.class, mcastMacs);
        //2 mcast macs + 2 locator sets + 3 termination points
        verify(mockOp, times(7)).insert(ArgumentMatchers.<McastMacsRemote>any());
    }

    @Test
    public <T extends DataObject> void testMcastMacAddWithoutConfigTep() throws Exception {
        addData(LogicalDatastoreType.CONFIGURATION, LogicalSwitches.class, logicalSwitches);
        addData(LogicalDatastoreType.OPERATIONAL, LogicalSwitches.class, logicalSwitches);
        resetOperations();
        addData(LogicalDatastoreType.CONFIGURATION, RemoteMcastMacs.class, mcastMacs);
        //2 mcast macs + 2 locator sets + 3 termination points
        verify(mockOp, times(7)).insert(ArgumentMatchers.<McastMacsRemote>any());
    }

    @Test
    public <T extends DataObject> void testMcastMacDelete() throws Exception {
        addData(LogicalDatastoreType.CONFIGURATION, LogicalSwitches.class, logicalSwitches);
        addData(LogicalDatastoreType.OPERATIONAL, LogicalSwitches.class, logicalSwitches);
        addData(LogicalDatastoreType.CONFIGURATION, TerminationPoint.class, terminationPoints);
        addData(LogicalDatastoreType.CONFIGURATION, RemoteMcastMacs.class, mcastMacs);
        addData(LogicalDatastoreType.OPERATIONAL, TerminationPoint.class, terminationPoints);
        addData(LogicalDatastoreType.OPERATIONAL, RemoteMcastMacs.class, mcastMacs);

        resetOperations();
        deleteData(LogicalDatastoreType.CONFIGURATION, RemoteMcastMacs.class, mcastMacs);
        verify(mockOp, times(2)).delete(ArgumentMatchers.any());
    }

    @Test
    public <T extends DataObject> void testAddMacs() throws Exception {
        addData(LogicalDatastoreType.CONFIGURATION, LogicalSwitches.class, logicalSwitches);
        addData(LogicalDatastoreType.OPERATIONAL, LogicalSwitches.class, logicalSwitches);
        resetOperations();
        addData(LogicalDatastoreType.CONFIGURATION, TerminationPoint.class, terminationPoints);
        addData(LogicalDatastoreType.CONFIGURATION, RemoteUcastMacs.class, ucastMacs);
        verify(mockOp, times(4)).insert(any(UcastMacsRemote.class));

        addData(LogicalDatastoreType.OPERATIONAL, TerminationPoint.class, terminationPoints);
        addData(LogicalDatastoreType.OPERATIONAL, RemoteUcastMacs.class, ucastMacs);
        resetOperations();
        addData(LogicalDatastoreType.CONFIGURATION, RemoteMcastMacs.class, mcastMacs);
        //2 mcast mac + 2 locator sets ( termination point already added )
        verify(mockOp, times(4)).insert(ArgumentMatchers.<McastMacsRemote>any());
    }

    @Test
    public <T extends DataObject> void testUpdateMacs() throws Exception {
        addData(LogicalDatastoreType.CONFIGURATION, LogicalSwitches.class, logicalSwitches);
        addData(LogicalDatastoreType.OPERATIONAL, LogicalSwitches.class, logicalSwitches);
        resetOperations();
        addData(LogicalDatastoreType.CONFIGURATION, TerminationPoint.class, terminationPoints);
        addData(LogicalDatastoreType.CONFIGURATION, RemoteUcastMacs.class, ucastMacs);
        verify(mockOp, times(4)).insert(any(UcastMacsRemote.class));

        addData(LogicalDatastoreType.OPERATIONAL, TerminationPoint.class, terminationPoints);
        addData(LogicalDatastoreType.OPERATIONAL, RemoteUcastMacs.class, ucastMacs);
        resetOperations();
        addData(LogicalDatastoreType.CONFIGURATION, RemoteMcastMacs.class, mcastMacs);
        verify(mockOp, times(4)).insert(ArgumentMatchers.<McastMacsRemote>any());
        addData(LogicalDatastoreType.OPERATIONAL, RemoteMcastMacs.class, mcastMacs);

        resetOperations();
        addData(LogicalDatastoreType.CONFIGURATION, RemoteMcastMacs.class, mcastMac2);
        verify(mockOp, times(2)).insert(ArgumentMatchers.<McastMacsRemote>any());
        verify(mockOp, times(2)).update(ArgumentMatchers.<McastMacsRemote>any());
        verify(mockOp, times(0)).delete(ArgumentMatchers.any());
    }

    @Test
    public <T extends DataObject> void testUpdateMacsWithZeroLocators() throws Exception {
        addData(LogicalDatastoreType.CONFIGURATION, LogicalSwitches.class, logicalSwitches);
        addData(LogicalDatastoreType.OPERATIONAL, LogicalSwitches.class, logicalSwitches);
        resetOperations();
        addData(LogicalDatastoreType.CONFIGURATION, TerminationPoint.class, terminationPoints);
        addData(LogicalDatastoreType.CONFIGURATION, RemoteUcastMacs.class, ucastMacs);
        verify(mockOp, times(4)).insert(any(UcastMacsRemote.class));

        addData(LogicalDatastoreType.OPERATIONAL, TerminationPoint.class, terminationPoints);
        addData(LogicalDatastoreType.OPERATIONAL, RemoteUcastMacs.class, ucastMacs);
        resetOperations();
        addData(LogicalDatastoreType.CONFIGURATION, RemoteMcastMacs.class, mcastMacs);
        verify(mockOp, times(4)).insert(ArgumentMatchers.<McastMacsRemote>any());
        addData(LogicalDatastoreType.OPERATIONAL, RemoteMcastMacs.class, mcastMacs);

        resetOperations();
        final MacAddress macAddr = new MacAddress("FF:FF:FF:FF:FF:FF");
        final InstanceIdentifier<HwvtepGlobalAugmentation> augIid =
                nodeIid.augmentation(HwvtepGlobalAugmentation.class);
        deleteData(LogicalDatastoreType.CONFIGURATION,
            augIid.child(RemoteMcastMacs.class,
                new RemoteMcastMacsKey(TestBuilders.buildLogicalSwitchesRef(nodeIid, "ls0"), macAddr))
            .child(LocatorSet.class),
            augIid.child(RemoteMcastMacs.class,
                new RemoteMcastMacsKey(TestBuilders.buildLogicalSwitchesRef(nodeIid, "ls1"), macAddr))
            .child(LocatorSet.class));
        verify(mockOp, times(2)).delete(ArgumentMatchers.any());
    }

    @Test
    public <T extends DataObject> void testBackToBackMacsUpdate() throws Exception {
        addData(LogicalDatastoreType.CONFIGURATION, LogicalSwitches.class, logicalSwitches);
        addData(LogicalDatastoreType.OPERATIONAL, LogicalSwitches.class, logicalSwitches);
        resetOperations();
        addData(LogicalDatastoreType.CONFIGURATION, TerminationPoint.class, terminationPoints);
        addData(LogicalDatastoreType.CONFIGURATION, RemoteUcastMacs.class, ucastMacs);
        verify(mockOp, times(4)).insert(any(UcastMacsRemote.class));

        resetOperations();
        addData(LogicalDatastoreType.CONFIGURATION, RemoteMcastMacs.class, mcastMacs);
        //2 mcast mac + 2 locator sets ( termination point already added )
        verify(mockOp, times(0)).insert(ArgumentMatchers.<McastMacsRemote>any());
        resetOperations();
        addData(LogicalDatastoreType.OPERATIONAL, TerminationPoint.class, terminationPoints);
        addData(LogicalDatastoreType.OPERATIONAL, RemoteUcastMacs.class, ucastMacs);
        connectionInstance.getDeviceInfo().onOperDataAvailable();
        //2 mcast mac + 2 locator sets ( termination point already added )
        verify(mockOp, timeout(2000).times(4)).insert(ArgumentMatchers.<McastMacsRemote>any());

        resetOperations();
        addData(LogicalDatastoreType.CONFIGURATION, RemoteMcastMacs.class, mcastMac2);
        verify(mockOp, times(0)).insert(ArgumentMatchers.<McastMacsRemote>any());
        addData(LogicalDatastoreType.OPERATIONAL, RemoteMcastMacs.class, mcastMacs);
        connectionInstance.getDeviceInfo().onOperDataAvailable();
        verify(mockOp, timeout(2000).times(2)).insert(ArgumentMatchers.<McastMacsRemote>any());
        verify(mockOp, times(2)).update(ArgumentMatchers.<McastMacsRemote>any());
    }

    private void verifyThatLogicalSwitchCreated() {
        //The transactions could be firing in two different mdsal updates intermittently
        //verify(ovsdbClient, times(1)).transact(any(DatabaseSchema.class), any(List.class));
        verify(mockOp, times(2)).insert(any(LogicalSwitch.class));

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
