/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.hwvtepsouthbound;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.ovsdb.hwvtepsouthbound.transact.DependencyQueue;
import org.opendaylight.ovsdb.hwvtepsouthbound.transact.DependentJob;
import org.opendaylight.ovsdb.lib.operations.Operations;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepLogicalSwitchRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepNodeName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LogicalSwitches;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LogicalSwitchesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.RemoteMcastMacs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.RemoteMcastMacsKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.RemoteUcastMacs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.RemoteUcastMacsKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.TpId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPointKey;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import static org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType.CONFIGURATION;

@RunWith(PowerMockRunner.class)
@PrepareForTest({HwvtepConnectionInstance.class, HwvtepConnectionManager.class, Operations.class})
public class HwvtepConfigTest extends DataChangeListenerTestBase {

    public static final String VXLAN_OVER_IPV4 = "vxlan_over_ipv4:";
    private final static Logger LOG = LoggerFactory.getLogger(HwvtepConfigTest.class);

    private final List<InstanceIdentifier<LogicalSwitches>> logicalSwitchIids = new ArrayList<>();
    private final List<InstanceIdentifier<RemoteUcastMacs>> ucastIids = new ArrayList<>();
    private final List<InstanceIdentifier<RemoteMcastMacs>> mcastIids = new ArrayList<>();
    private final List<InstanceIdentifier<TerminationPoint>> locatorIids = new ArrayList<>();
    private final List<RemoteUcastMacs> ucastData = new ArrayList<>();
    private final List<RemoteMcastMacs> mcastData = new ArrayList<>();

    private final String[][] ucastMacs = new String[][]{
            {"20:00:00:00:00:01", "11.10.10.1", "192.168.122.20", "ls0"},
            {"20:00:00:00:00:02", "11.10.10.2", "192.168.122.20", "ls0"},
            {"20:00:00:00:00:03", "11.10.10.3", "192.168.122.30", "ls1"},
            {"20:00:00:00:00:04", "11.10.10.4", "192.168.122.30", "ls1"}
    };

    private final String[][] logicalSwitches = new String[][]{
            {"ls0", "100"},
            {"ls1", "200"},
    };

    private final String[][] terminationPoints = new String[][]{
            {"192.168.122.10"},
            {"192.168.122.20"},
            {"192.168.122.30"}
    };

    private final String[][] mcastMacs = new String[][]{
            {"FF:FF:FF:FF:FF:FF", "ls0", "192.168.122.20", "192.168.122.30"},
            {"FF:FF:FF:FF:FF:FF", "ls1", "192.168.122.10", "192.168.122.30"}
    };

    private final String[][] mcastMac2 = new String[][]{
            {"FF:FF:FF:FF:FF:FF", "ls0", "192.168.122.20", "192.168.122.10"},
            {"FF:FF:FF:FF:FF:FF", "ls1", "192.168.122.10", "192.168.122.20"}
    };

    private final String[][] mcastMac3WithZeroLocators = new String[][]{
            {"FF:FF:FF:FF:FF:FF", "ls0"},
            {"FF:FF:FF:FF:FF:FF", "ls1"}
    };

    @Before
    public void setupListener() throws Exception {
        logicalSwitchIids.clear();
        for (int i = 0; i < logicalSwitches.length; i++) {
            logicalSwitchIids.add(getLogicalSwitchIid(logicalSwitches[i][0]));
        }
        ucastIids.clear();
        for (int i = 0; i < ucastMacs.length; i++) {
            ucastIids.add(nodeIid.augmentation(HwvtepGlobalAugmentation.class).child(RemoteUcastMacs.class,
                    new RemoteUcastMacsKey(new HwvtepLogicalSwitchRef(getLogicalSwitchIid(ucastMacs[i][3])),
                            new MacAddress(ucastMacs[i][0]))));
        }
        mcastIids.clear();
        for (int i = 0; i < mcastMacs.length; i++) {
            mcastIids.add(nodeIid.augmentation(HwvtepGlobalAugmentation.class).child(RemoteMcastMacs.class,
                    new RemoteMcastMacsKey(new HwvtepLogicalSwitchRef(getLogicalSwitchIid(mcastMacs[i][1])),
                            new MacAddress(mcastMacs[i][0]))));
        }
        locatorIids.clear();
        for (int i = 0; i < terminationPoints.length; i++) {
            locatorIids.add(getTerminationPointIid(terminationPoints[i][0]));
        }
    }

    private InstanceIdentifier<TerminationPoint> getTerminationPointIid(String tep){
        return nodeIid.child(TerminationPoint.class,
                new TerminationPointKey(new TpId(VXLAN_OVER_IPV4 + tep)));
    }

    private void readData() {
        ucastIids.forEach( (iid) -> {
            if (DATA_AVAILABLE.test(iid)) {
                ucastData.add(HwvtepSouthboundUtil.readNode(dataBroker.newReadWriteTransaction(), iid).get());
            }
        });
        mcastIids.forEach( (iid) -> {
            if (DATA_AVAILABLE.test(iid)) {
                mcastData.add(HwvtepSouthboundUtil.readNode(dataBroker.newReadWriteTransaction(), iid).get());
            }
        });
    }

    private InstanceIdentifier<LogicalSwitches> getLogicalSwitchIid(final String logicalSwitchName) {
        return nodeIid.augmentation(HwvtepGlobalAugmentation.class).child(LogicalSwitches.class,
                new LogicalSwitchesKey(new HwvtepNodeName(logicalSwitchName)));
    }

    @Test
    public void testLogicalSwitchAdd() {
        addData(CONFIGURATION, LogicalSwitches.class, logicalSwitches);
        waitForData(logicalSwitchIids);
    }

    @Test
    public <T extends DataObject> void testLogicalSwitchDelete() throws Exception {
        testLogicalSwitchAdd();
        deleteData(CONFIGURATION, LogicalSwitches.class, logicalSwitches);
        waitForDataDelete(logicalSwitchIids);
    }

    @Test
    public <T extends DataObject> void testUcastMacAdd() throws Exception {
        testLogicalSwitchAdd();
        addData(CONFIGURATION, TerminationPoint.class, terminationPoints);
        addData(CONFIGURATION, RemoteUcastMacs.class, ucastMacs);
        waitForData(ucastIids);
        waitForUcastLocators(ucastIids);
    }

    @Test
    public <T extends DataObject> void testUcastMacAddWithoutConfigTep() throws Exception {
        testLogicalSwitchAdd();
        addData(CONFIGURATION, RemoteUcastMacs.class, ucastMacs);
        waitForData(ucastIids);
        waitForUcastLocators(ucastIids);
    }

    @Test
    public <T extends DataObject> void testUcastMacDelete() throws Exception {
        testUcastMacAdd();
        waitForData(ucastIids);
        readData();
        deleteData(CONFIGURATION, RemoteUcastMacs.class, ucastMacs);
        waitForDataDelete(ucastIids);
        waitForUcastLocatorsDelete(ucastData);
    }

    public <T extends DataObject> void testMcastMacAdd() throws Exception {
        testLogicalSwitchAdd();
        addData(CONFIGURATION, TerminationPoint.class, terminationPoints);
        addData(CONFIGURATION, RemoteMcastMacs.class, mcastMacs);
        waitForData(mcastIids);
        waitForMcastLocators(mcastIids);
    }

    @Test
    public <T extends DataObject> void testMcastMacAddWithoutConfigTep() throws Exception {
        testLogicalSwitchAdd();
        addData(CONFIGURATION, RemoteMcastMacs.class, mcastMacs);
        waitForData(mcastIids);
        waitForMcastLocators(mcastIids);
    }

    @Test
    public <T extends DataObject> void testMcastMacDelete() throws Exception {
        testMcastMacAdd();
        readData();
        deleteData(CONFIGURATION, RemoteMcastMacs.class, mcastMacs);
        waitForDataDelete(mcastIids);
        waitForMcastLocatorsDelete(mcastData);
    }

    @Test
    public <T extends DataObject> void testAddMcastAfterUcast() throws Exception {
        testUcastMacAdd();
        addData(CONFIGURATION, RemoteMcastMacs.class, mcastMacs);
        waitForData(mcastIids);
        waitForMcastLocators(mcastIids);
    }

    @Test
    public <T extends DataObject> void testUpdateMacsWithZeroLocators() throws Exception {
        testMcastMacAdd();
        readData();
        //update mcasts with zero locators
        addData(CONFIGURATION, RemoteMcastMacs.class, mcastMac3WithZeroLocators);
        //They should be removed
        waitForDataDelete(mcastIids);
        //Their orphaned locator refs should be removed
        waitForMcastLocatorsDelete(mcastData);
    }

    @Test
    public <T extends DataObject> void testSameMcastUpdate() throws Exception {
        testLogicalSwitchAdd();
        deviceTransactionInvoker.holdUpdates();
        //add the mcast
        addData(CONFIGURATION, RemoteMcastMacs.class, mcastMacs);
        //it is in transit now
        getAwaiter().until(() -> hwvtepDeviceInfo.isKeyInTransit(RemoteMcastMacs.class, mcastIids.get(0)));
        //update the same mcast
        addData(CONFIGURATION, RemoteMcastMacs.class, mcastMac2);
        //it should be added to wait queue
        waitUntilIidIsAddedToWaitQueue(mcastIids.get(0), RemoteMcastMacs.class, mcastIids.get(0));
        deviceTransactionInvoker.releaseUpdates();
        //it should be removed from queue now
        waitUntilIidIsRemovedFromWaitQueue(mcastIids.get(0));
    }

    private void waitUntilIidIsAddedToWaitQueue(final InstanceIdentifier dependent,
                                                final Class<? extends DataObject> targetCls,
                                                final InstanceIdentifier target) {
        DependencyQueue dependencyQueue = hwvtepDeviceInfo.getDependencyQueue();
        getAwaiter().until(() -> {
            Iterator<DependentJob> iterator = dependencyQueue.getOperationalWaitingJobs();
            while (iterator.hasNext()) {
                DependentJob job = iterator.next();
                if (Objects.equals(job.getKey(), dependent)) {
                    List<InstanceIdentifier> iids = (List<InstanceIdentifier>)job.getDependencies().get(targetCls);
                    if (iids != null) {
                        return iids.contains(target);
                    }
                }
            }
            return false;
        });
    }

    private void waitUntilIidIsRemovedFromWaitQueue(final InstanceIdentifier dependent) {
        DependencyQueue dependencyQueue = hwvtepDeviceInfo.getDependencyQueue();
        getAwaiter().until(() -> {
            Iterator<DependentJob> iterator = dependencyQueue.getOperationalWaitingJobs();
            while (iterator.hasNext()) {
                if (Objects.equals(iterator.next().getKey(), dependent)) {
                    return false;
                }
            }
            return true;
        });
    }

    @Test
    public <T extends DataObject> void testBackToBackMacsLocatorsUpdate() throws Exception {
        testLogicalSwitchAdd();
        deviceTransactionInvoker.holdUpdates();
        //add the ucasts
        addData(CONFIGURATION, RemoteUcastMacs.class, ucastMacs);
        //locators are in transit now
        getAwaiter().until(() -> hwvtepDeviceInfo.isKeyInTransit(TerminationPoint.class, locatorIids.get(1)));
        //add the mcasts
        addData(CONFIGURATION, RemoteMcastMacs.class, mcastMacs);
        //release the updates from device
        deviceTransactionInvoker.releaseUpdates();
        //mcast should be processed now
        waitForData(mcastIids);
    }

    @Test
    public <T extends DataObject> void testLocatorInTransitStateExpiry() throws Exception {
        testMcastMacAdd();
        deviceTransactionInvoker.holdUpdates();
        //add the mcasts
        deleteData(CONFIGURATION, RemoteMcastMacs.class, mcastMacs);
        //locators are in transit now
        getAwaiter().until(() -> hwvtepDeviceInfo.isKeyInTransit(TerminationPoint.class, locatorIids.get(1)));
        //add the ucasts now
        addData(CONFIGURATION, RemoteUcastMacs.class, ucastMacs);
        //They should be held in wait queue
        waitUntilIidIsAddedToWaitQueue(ucastIids.get(0), TerminationPoint.class, locatorIids.get(1));
        //They should be released from wait queue after the intransit state expires
        waitUntilIidIsRemovedFromWaitQueue(ucastIids.get(0));
        deviceTransactionInvoker.releaseUpdates();
        waitForData(ucastIids);
        waitForUcastLocators(ucastIids);
    }
}
