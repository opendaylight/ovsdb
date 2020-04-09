/*
 * Copyright (c) 2016, 2017 Ericsson India Global Services Pvt Ltd. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.hwvtepsouthbound;

import static org.junit.Assert.assertEquals;
import static org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType.CONFIGURATION;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.opendaylight.ovsdb.hwvtepsouthbound.transact.DependencyQueue;
import org.opendaylight.ovsdb.hwvtepsouthbound.transact.DependentJob;
import org.opendaylight.ovsdb.hwvtepsouthbound.transact.HwvtepOperationalState;
import org.opendaylight.ovsdb.hwvtepsouthbound.transact.McastMacsRemoteUpdateCommand;
import org.opendaylight.ovsdb.hwvtepsouthbound.transact.UnMetDependencyGetter;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepGlobalAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.HwvtepNodeName;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LogicalSwitches;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.LogicalSwitchesKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.RemoteMcastMacs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.hwvtep.rev150901.hwvtep.global.attributes.RemoteMcastMacsKey;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.yang.binding.Identifiable;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.powermock.reflect.Whitebox;

@RunWith(MockitoJUnitRunner.Silent.class)
public class DependencyQueueTest extends DataChangeListenerTestBase {

    String[][] terminationPoints = new String[][]{
            {"192.168.122.20"},
            {"192.168.122.30"}
    };

    UnMetDependencyGetter<RemoteMcastMacs> mcastMacDataValidator;
    HwvtepOperationalState opState;
    RemoteMcastMacs mac;
    InstanceIdentifier<RemoteMcastMacs> macIid;
    InstanceIdentifier<LogicalSwitches> lsIid;
    Map<Class<? extends Identifiable>, List<InstanceIdentifier>> unMetDependencies;

    void setupForTest() throws Exception {
        mcastMacDataValidator = Whitebox.getInternalState(McastMacsRemoteUpdateCommand.class,
                UnMetDependencyGetter.class);
        opState = new HwvtepOperationalState(connectionInstance);
        mac = TestBuilders.buildRemoteMcastMacs(nodeIid,"FF:FF:FF:FF:FF:FF", "ls0",
                new String[]{"192.168.122.20", "192.168.122.30"});
        lsIid = nodeIid.augmentation(HwvtepGlobalAugmentation.class)
                .child(LogicalSwitches.class, new LogicalSwitchesKey(new HwvtepNodeName("ls0")));
        macIid = nodeIid.augmentation(HwvtepGlobalAugmentation.class)
                .child(RemoteMcastMacs.class, new RemoteMcastMacsKey(mac.key()));
        setFinalStatic(DependencyQueue.class, "EXECUTOR_SERVICE", Mockito.mock(SameThreadScheduledExecutor.class,
                Mockito.CALLS_REAL_METHODS));
    }

    @Test
    public void testLogicalSwitchConfigDependency() throws Exception {
        setupForTest();
        unMetDependencies = mcastMacDataValidator.getUnMetConfigDependencies(opState, mac);
        unMetDependencies.remove(TerminationPoint.class);

        final CountDownLatch latch = new CountDownLatch(1);
        opState.getDeviceInfo().addJobToQueue(new DependentJob.ConfigWaitingJob(macIid, mac, unMetDependencies) {
            @Override
            protected void onDependencyResolved(HwvtepOperationalState operationalState,
                    TransactionBuilder transactionBuilder) {
                latch.countDown();
            }
        });
        assertEquals(1, latch.getCount());
        addData(CONFIGURATION, LogicalSwitches.class, new String[]{"ls0", "100"});
        addData(CONFIGURATION, TerminationPoint.class, terminationPoints);
        assertEquals(0, latch.getCount());
    }

    @Test
    public void testLogicalSwitchInTransitDependency() throws Exception {
        setupForTest();
        opState.getDeviceInfo().markKeyAsInTransit(LogicalSwitches.class, lsIid);
        unMetDependencies = mcastMacDataValidator.getInTransitDependencies(opState, mac);

        final CountDownLatch latch = new CountDownLatch(1);
        opState.getDeviceInfo().addJobToQueue(new DependentJob.OpWaitingJob<RemoteMcastMacs>(
                macIid, mac, (Map)unMetDependencies, 0) {
            @Override
            protected void onDependencyResolved(HwvtepOperationalState operationalState,
                    TransactionBuilder transactionBuilder) {
                latch.countDown();
            }
        });
        opState.getDeviceInfo().onOperDataAvailable();
        assertEquals(1, latch.getCount());

        opState.getDeviceInfo().updateDeviceOperData(LogicalSwitches.class, lsIid, new UUID("ls0"), "ls0");
        opState.getDeviceInfo().onOperDataAvailable();
        //wait for sometime so that the onDependencyResolved is triggered
        Thread.sleep(500);
        assertEquals(0, latch.getCount());

    }
}
