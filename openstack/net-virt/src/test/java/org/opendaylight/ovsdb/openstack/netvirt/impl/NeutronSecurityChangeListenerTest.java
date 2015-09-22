/*
 * Copyright (c) 2014, 2015 HP, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt.impl;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.util.concurrent.CheckedFuture;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker.DataChangeScope;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.neutron.spi.INeutronNetworkCRUD;
import org.opendaylight.neutron.spi.INeutronSecurityGroupCRUD;
import org.opendaylight.neutron.spi.NeutronNetwork;
import org.opendaylight.neutron.spi.NeutronSecurityGroup;
import org.opendaylight.neutron.spi.NeutronSecurityRule;
import org.opendaylight.neutron.spi.Neutron_IPs;
import org.opendaylight.ovsdb.openstack.netvirt.api.ConfigurationService;
import org.opendaylight.ovsdb.openstack.netvirt.api.EgressAclProvider;
import org.opendaylight.ovsdb.openstack.netvirt.api.IngressAclProvider;
import org.opendaylight.ovsdb.openstack.netvirt.api.SecurityServicesManager;
import org.opendaylight.ovsdb.openstack.netvirt.api.Southbound;
import org.opendaylight.ovsdb.utils.servicehelper.ServiceHelper;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev130715.Uuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.ports.rev141002.ports.attributes.Ports;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.ports.rev141002.ports.attributes.ports.Port;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.ports.rev141002.ports.attributes.ports.PortKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.rev150325.Neutron;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

/**
 * Unit test fort {@link NeutronSecurityChangeListener}.
 */

@RunWith(PowerMockRunner.class)
@PrepareForTest(ServiceHelper.class)
public class NeutronSecurityChangeListenerTest {
    @Mock private DataBroker dataBroker;
    private NeutronSecurityChangeListener neutronSecurityChangeListener ;
    @Mock private AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> dateChangeEvent;
    @Mock private NeutronSecurityGroup securityGroup1;
    @Mock private NeutronSecurityGroup securityGroup2;
    @Mock private NeutronSecurityGroup securityGroup3;
    @Mock private NeutronSecurityRule portSecurityRule;
    @Mock private volatile INeutronNetworkCRUD neutronNetworkCache;
    @Mock private ListenerRegistration<DataChangeListener> portRegistration;
    @Mock private ListenerRegistration<DataChangeListener> securityGroupRegistration;
    @Mock private ListenerRegistration<DataChangeListener> securityRuleRegistration;
    @Mock private Port originalPort;
    @Mock private Port updatedPort;
    @Mock private INeutronSecurityGroupCRUD securityGroupCache;
    @Mock private Southbound southbound;
    @Mock private NeutronNetwork neutronNetwork;
    @Mock private ConfigurationService configurationService;
    @Mock private SecurityServicesManager securityServicesManager;
    @Mock private ExecutorService executorService;
    @Mock private volatile IngressAclProvider ingressAclProvider;
    @Mock private volatile EgressAclProvider egressAclProvider;

    private static final String MAC_ADDRESS = "87:1D:5E:02:40:B8";
    private static final String PORT_UUID = "15cc3048-abc3-43cc-89b3-377341426ac6";
    private static final String SECURITY_GROUP_UUID1 = "25cc3048-abc3-43cc-89b3-377341426ac5";
    private static final String SECURITY_GROUP_UUID2 = "35cc3048-abc3-43cc-89b3-377341426ac6";
    private static final String SECURITY_GROUP_UUID3 = "45cc3048-abc3-43cc-89b3-377341426ac6";
    private static final String NETWORK_UUID = "55cc3048-abc3-43cc-89b3-377341426ac6";

    private List<OvsdbTerminationPointAugmentation> ovsdbTerminationPointList =
            new ArrayList<OvsdbTerminationPointAugmentation>();
    private OvsdbTerminationPointAugmentation ovsdbTerminationPoint;
    private Map<InstanceIdentifier<?>, DataObject> originalData;
    private Map<InstanceIdentifier<?>, DataObject> updatedData;
    private static final String SRC_IP = "192.168.0.1";
    private List<Neutron_IPs> neutronSrcIpList = new ArrayList<Neutron_IPs>();

    /**
     * Mock the classes retrieved from the bundle context.
     */
    @Before
    public void setUp() {
        originalData = new HashMap<InstanceIdentifier<?>, DataObject>();
        updatedData = new HashMap<InstanceIdentifier<?>, DataObject>();
        Neutron_IPs neutronIpSrc = new Neutron_IPs();
        neutronIpSrc.setIpAddress(SRC_IP);
        neutronSrcIpList.add(neutronIpSrc);

        PowerMockito.mockStatic(ServiceHelper.class);

        when(ServiceHelper.getGlobalInstance(eq(Southbound.class), anyObject())).thenReturn(southbound);
        when(southbound.getDataPathId(any(Node.class))).thenReturn((long) 5);
        when(southbound.getInterfaceExternalIdsValue(any(OvsdbTerminationPointAugmentation.class),
                                                     any(String.class))).thenReturn(PORT_UUID);
        when(southbound.getBridgeName(any(Node.class))).thenReturn("br-int");

        when(ServiceHelper.getGlobalInstance(eq(INeutronNetworkCRUD.class), anyObject()))
            .thenReturn(neutronNetworkCache);
        when(neutronNetworkCache.getNetwork(any(String.class))).thenReturn(neutronNetwork);
        when(neutronNetwork.getProviderSegmentationID()).thenReturn("1000");

        when(ServiceHelper.getGlobalInstance(eq(ConfigurationService.class), anyObject()))
            .thenReturn(configurationService);
        when(configurationService.getIntegrationBridgeName()).thenReturn("br-int");

        when(ServiceHelper.getGlobalInstance(eq(IngressAclProvider.class), anyObject())).thenReturn(ingressAclProvider);
        when(ServiceHelper.getGlobalInstance(eq(EgressAclProvider.class), anyObject())).thenReturn(egressAclProvider);

        when(ServiceHelper.getGlobalInstance(eq(SecurityServicesManager.class), anyObject()))
            .thenReturn(securityServicesManager);
        when(securityServicesManager.getIpAddressList(any(Node.class), any(OvsdbTerminationPointAugmentation.class)))
            .thenReturn(neutronSrcIpList);

        when(ServiceHelper.getGlobalInstance(eq(INeutronSecurityGroupCRUD.class), anyObject()))
            .thenReturn(securityGroupCache);
        when(securityGroupCache.getNeutronSecurityGroup(eq(SECURITY_GROUP_UUID1))).thenReturn(securityGroup1);
        when(securityGroupCache.getNeutronSecurityGroup(eq(SECURITY_GROUP_UUID2))).thenReturn(securityGroup2);
        when(securityGroupCache.getNeutronSecurityGroup(eq(SECURITY_GROUP_UUID3))).thenReturn(securityGroup3);
        when(securityGroupCache.getNeutronSecurityGroup(eq(SECURITY_GROUP_UUID3))).thenReturn(securityGroup3);

        when(originalPort.getUuid()).thenReturn(new Uuid(PORT_UUID));
        when(originalPort.getKey()).thenReturn(new PortKey(new Uuid(PORT_UUID)));
        when(originalPort.getNetworkId()).thenReturn(new Uuid(NETWORK_UUID));
        originalData.put(createInstanceIdentifier(originalPort), originalPort);
        when(dateChangeEvent.getOriginalData()).thenReturn(originalData);

        when(updatedPort.getUuid()).thenReturn(new Uuid(PORT_UUID));
        when(updatedPort.getKey()).thenReturn(new PortKey(new Uuid(PORT_UUID)));
        when(updatedPort.getNetworkId()).thenReturn(new Uuid(NETWORK_UUID));
        updatedData.put(createInstanceIdentifier(updatedPort), updatedPort);
        when(dateChangeEvent.getUpdatedData()).thenReturn(updatedData);

        ovsdbTerminationPointList.add(ovsdbTerminationPoint);
        when(southbound.getTerminationPointsOfBridge(any(Node.class))).thenReturn(ovsdbTerminationPointList);
        when(southbound.getOFPort(any(OvsdbTerminationPointAugmentation.class))).thenReturn((long) 6);
        when(southbound.getInterfaceExternalIdsValue(any(OvsdbTerminationPointAugmentation.class),any(String.class)))
            .thenReturn(MAC_ADDRESS);

        when(dataBroker.registerDataChangeListener(any(LogicalDatastoreType.class), any(InstanceIdentifier.class),
                                                   any(DataChangeListener.class), any(DataChangeScope.class)))
                                                   .thenReturn(portRegistration);
        neutronSecurityChangeListener = new NeutronSecurityChangeListener(dataBroker,executorService);
        when(executorService.submit(any(Runnable.class))).thenAnswer(answer());
    }

    /**
     * Test SecurityGroup Added to existing.
     */
    @Test
    public void testOnDataChangedAdded1() {
        List<Uuid> originalUuidList = new ArrayList<Uuid>();
        originalUuidList.add(new Uuid(SECURITY_GROUP_UUID1));
        when(originalPort.getSecurityGroups()).thenReturn(originalUuidList);

        List<Uuid> updatedUuidList = new ArrayList<Uuid>();
        updatedUuidList.add(new Uuid(SECURITY_GROUP_UUID1));
        updatedUuidList.add(new Uuid(SECURITY_GROUP_UUID2));
        updatedUuidList.add(new Uuid(SECURITY_GROUP_UUID3));
        when(updatedPort.getSecurityGroups()).thenReturn(updatedUuidList);

        List<Uuid> uuidList = new ArrayList<Uuid>();
        neutronSecurityChangeListener.onDataChanged(dateChangeEvent);
        verify(ingressAclProvider, times(1)).programPortSecurityAcl( eq((long)5), eq("1000"), eq(MAC_ADDRESS),
                                                                     eq((long)6), eq(securityGroup2),
                                                                     eq(neutronSrcIpList), eq(true));
        verify(ingressAclProvider, times(1)).programPortSecurityAcl( eq((long)5), eq("1000"), eq(MAC_ADDRESS),
                                                                     eq((long)6), eq(securityGroup3),
                                                                     eq(neutronSrcIpList), eq(true));
        verify(ingressAclProvider, times(1)).programPortSecurityAcl( eq((long)5), eq("1000"), eq(MAC_ADDRESS),
                                                                     eq((long)6), eq(securityGroup2),
                                                                     eq(neutronSrcIpList), eq(true));
        verify(ingressAclProvider, times(1)).programPortSecurityAcl( eq((long)5), eq("1000"), eq(MAC_ADDRESS),
                                                                     eq((long)6), eq(securityGroup3),
                                                                     eq(neutronSrcIpList), eq(true));
    }

    /**
     * Test SecurityGroup Added to empty.
     */
    @Test
    public void testOnDataChangedAdded2() {
        List<Uuid> originalUuidList = new ArrayList<Uuid>();
        when(originalPort.getSecurityGroups()).thenReturn(originalUuidList);

        List<Uuid> updatedUuidList = new ArrayList<Uuid>();
        updatedUuidList.add(new Uuid(SECURITY_GROUP_UUID1));
        updatedUuidList.add(new Uuid(SECURITY_GROUP_UUID2));
        when(updatedPort.getSecurityGroups()).thenReturn(updatedUuidList);

        List<Uuid> uuidList = new ArrayList<Uuid>();
        neutronSecurityChangeListener.onDataChanged(dateChangeEvent);
        verify(ingressAclProvider, times(1)).programPortSecurityAcl( eq((long)5), eq("1000"), eq(MAC_ADDRESS),
                                                                     eq((long)6), eq(securityGroup1),
                                                                     eq(neutronSrcIpList), eq(true));
        verify(ingressAclProvider, times(1)).programPortSecurityAcl( eq((long)5), eq("1000"), eq(MAC_ADDRESS),
                                                                     eq((long)6), eq(securityGroup2),
                                                                     eq(neutronSrcIpList), eq(true));
        verify(egressAclProvider, times(1)).programPortSecurityAcl( eq((long)5), eq("1000"), eq(MAC_ADDRESS),
                                                                    eq((long)6), eq(securityGroup1),
                                                                    eq(neutronSrcIpList), eq(true));
        verify(egressAclProvider, times(1)).programPortSecurityAcl( eq((long)5), eq("1000"), eq(MAC_ADDRESS),
                                                                    eq((long)6), eq(securityGroup2),
                                                                    eq(neutronSrcIpList), eq(true));
    }

    /**
     * Test SecurityGroup Deleted partially.
     */
    @Test
    public void testOnDataChangedDeleted1() {
        List<Uuid> originalUuidList = new ArrayList<Uuid>();
        originalUuidList.add(new Uuid(SECURITY_GROUP_UUID1));
        originalUuidList.add(new Uuid(SECURITY_GROUP_UUID2));
        originalUuidList.add(new Uuid(SECURITY_GROUP_UUID3));
        when(originalPort.getSecurityGroups()).thenReturn(originalUuidList);

        List<Uuid> updatedUuidList = new ArrayList<Uuid>();
        updatedUuidList.add(new Uuid(SECURITY_GROUP_UUID1));
        when(updatedPort.getSecurityGroups()).thenReturn(updatedUuidList);

        List<Uuid> uuidList = new ArrayList<Uuid>();
        neutronSecurityChangeListener.onDataChanged(dateChangeEvent);
        verify(ingressAclProvider, times(1)).programPortSecurityAcl( eq((long)5), eq("1000"), eq(MAC_ADDRESS),
                                                                     eq((long)6), eq(securityGroup2),
                                                                     eq(neutronSrcIpList), eq(false));
        verify(ingressAclProvider, times(1)).programPortSecurityAcl( eq((long)5), eq("1000"), eq(MAC_ADDRESS),
                                                                     eq((long)6), eq(securityGroup3),
                                                                     eq(neutronSrcIpList), eq(false));
        verify(egressAclProvider, times(1)).programPortSecurityAcl( eq((long)5), eq("1000"), eq(MAC_ADDRESS),
                                                                    eq((long)6), eq(securityGroup2),
                                                                    eq(neutronSrcIpList), eq(false));
        verify(egressAclProvider, times(1)).programPortSecurityAcl( eq((long)5), eq("1000"), eq(MAC_ADDRESS),
                                                                    eq((long)6), eq(securityGroup3),
                                                                    eq(neutronSrcIpList), eq(false));
    }

    /**
     * Test SecurityGroup Deleted all.
     */
    @Test
    public void testOnDataChangedDeleted2() {
        List<Uuid> originalUuidList = new ArrayList<Uuid>();
        originalUuidList.add(new Uuid(SECURITY_GROUP_UUID1));
        originalUuidList.add(new Uuid(SECURITY_GROUP_UUID2));
        originalUuidList.add(new Uuid(SECURITY_GROUP_UUID3));
        when(originalPort.getSecurityGroups()).thenReturn(originalUuidList);

        List<Uuid> updatedUuidList = new ArrayList<Uuid>();
        when(updatedPort.getSecurityGroups()).thenReturn(updatedUuidList);

        List<Uuid> uuidList = new ArrayList<Uuid>();
        neutronSecurityChangeListener.onDataChanged(dateChangeEvent);
        verify(ingressAclProvider, times(1)).programPortSecurityAcl( eq((long)5), eq("1000"), eq(MAC_ADDRESS),
                                                                     eq((long)6), eq(securityGroup1),
                                                                     eq(neutronSrcIpList), eq(false));
        verify(ingressAclProvider, times(1)).programPortSecurityAcl( eq((long)5), eq("1000"), eq(MAC_ADDRESS),
                                                                     eq((long)6), eq(securityGroup2),
                                                                     eq(neutronSrcIpList), eq(false));
        verify(ingressAclProvider, times(1)).programPortSecurityAcl( eq((long)5), eq("1000"), eq(MAC_ADDRESS),
                                                                     eq((long)6), eq(securityGroup3),
                                                                     eq(neutronSrcIpList), eq(false));
        verify(egressAclProvider, times(1)).programPortSecurityAcl( eq((long)5), eq("1000"), eq(MAC_ADDRESS),
                                                                    eq((long)6), eq(securityGroup1),
                                                                    eq(neutronSrcIpList), eq(false));
        verify(egressAclProvider, times(1)).programPortSecurityAcl( eq((long)5), eq("1000"), eq(MAC_ADDRESS),
                                                                    eq((long)6), eq(securityGroup2),
                                                                    eq(neutronSrcIpList), eq(false));
        verify(egressAclProvider, times(1)).programPortSecurityAcl( eq((long)5), eq("1000"), eq(MAC_ADDRESS),
                                                                    eq((long)6), eq(securityGroup3),
                                                                    eq(neutronSrcIpList), eq(false));
    }

    /**
     * Test SecurityGroup Modified added one deleted one.
     */
    @Test
    public void testOnDataChangedModified() {
        List<Uuid> originalUuidList = new ArrayList<Uuid>();
        originalUuidList.add(new Uuid(SECURITY_GROUP_UUID1));
        originalUuidList.add(new Uuid(SECURITY_GROUP_UUID2));
        when(originalPort.getSecurityGroups()).thenReturn(originalUuidList);

        List<Uuid> updatedUuidList = new ArrayList<Uuid>();
        updatedUuidList.add(new Uuid(SECURITY_GROUP_UUID1));
        updatedUuidList.add(new Uuid(SECURITY_GROUP_UUID3));
        when(updatedPort.getSecurityGroups()).thenReturn(updatedUuidList);

        List<Uuid> uuidList = new ArrayList<Uuid>();
        neutronSecurityChangeListener.onDataChanged(dateChangeEvent);
        verify(ingressAclProvider, times(1)).programPortSecurityAcl( eq((long)5), eq("1000"), eq(MAC_ADDRESS),
                                                                     eq((long)6), eq(securityGroup2),
                                                                     eq(neutronSrcIpList), eq(false));
        verify(ingressAclProvider, times(1)).programPortSecurityAcl( eq((long)5), eq("1000"), eq(MAC_ADDRESS),
                                                                     eq((long)6), eq(securityGroup3),
                                                                     eq(neutronSrcIpList), eq(true));
        verify(egressAclProvider, times(1)).programPortSecurityAcl( eq((long)5), eq("1000"), eq(MAC_ADDRESS),
                                                                    eq((long)6), eq(securityGroup2),
                                                                    eq(neutronSrcIpList), eq(false));
        verify(egressAclProvider, times(1)).programPortSecurityAcl( eq((long)5), eq("1000"), eq(MAC_ADDRESS),
                                                                    eq((long)6), eq(securityGroup3),
                                                                    eq(neutronSrcIpList), eq(true));
    }

    /**
     * Test SecurityGroup Modified added two deleted one.
     */
    @Test
    public void testOnDataChangedModified2() {
        List<Uuid> originalUuidList = new ArrayList<Uuid>();
        originalUuidList.add(new Uuid(SECURITY_GROUP_UUID1));
        when(originalPort.getSecurityGroups()).thenReturn(originalUuidList);

        List<Uuid> updatedUuidList = new ArrayList<Uuid>();
        updatedUuidList.add(new Uuid(SECURITY_GROUP_UUID2));
        updatedUuidList.add(new Uuid(SECURITY_GROUP_UUID3));
        when(updatedPort.getSecurityGroups()).thenReturn(updatedUuidList);

        List<Uuid> uuidList = new ArrayList<Uuid>();
        neutronSecurityChangeListener.onDataChanged(dateChangeEvent);
        verify(ingressAclProvider, times(1)).programPortSecurityAcl( eq((long)5), eq("1000"), eq(MAC_ADDRESS),
                                                                     eq((long)6), eq(securityGroup1),
                                                                     eq(neutronSrcIpList), eq(false));
        verify(ingressAclProvider, times(1)).programPortSecurityAcl( eq((long)5), eq("1000"), eq(MAC_ADDRESS),
                                                                     eq((long)6), eq(securityGroup2),
                                                                     eq(neutronSrcIpList), eq(true));
        verify(ingressAclProvider, times(1)).programPortSecurityAcl( eq((long)5), eq("1000"), eq(MAC_ADDRESS),
                                                                     eq((long)6), eq(securityGroup3),
                                                                     eq(neutronSrcIpList), eq(true));
        verify(egressAclProvider, times(1)).programPortSecurityAcl( eq((long)5), eq("1000"), eq(MAC_ADDRESS),
                                                                    eq((long)6), eq(securityGroup1),
                                                                    eq(neutronSrcIpList), eq(false));
        verify(egressAclProvider, times(1)).programPortSecurityAcl( eq((long)5), eq("1000"), eq(MAC_ADDRESS),
                                                                    eq((long)6), eq(securityGroup2),
                                                                    eq(neutronSrcIpList), eq(true));
        verify(egressAclProvider, times(1)).programPortSecurityAcl( eq((long)5), eq("1000"), eq(MAC_ADDRESS),
                                                                    eq((long)6), eq(securityGroup3),
                                                                    eq(neutronSrcIpList), eq(true));
    }

    private InstanceIdentifier<Port> createInstanceIdentifier(Port port) {
        return InstanceIdentifier.create(Neutron.class)
                .child(Ports.class)
                .child(Port.class,port.getKey());
    }

    private static Answer<Future<?>> answer() {
        return new Answer<Future<?>>() {
            @Override
            public CheckedFuture<Void, TransactionCommitFailedException> answer(InvocationOnMock invocation)
                    throws Throwable {
                Runnable runnnable  = (Runnable) invocation.getArguments()[0];
                runnnable.run();
                return null;
            }
        };
    }
}
