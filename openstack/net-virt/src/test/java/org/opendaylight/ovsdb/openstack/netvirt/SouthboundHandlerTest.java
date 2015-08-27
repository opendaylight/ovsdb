/*
 * Copyright (c) 2015 Inocybe and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.opendaylight.ovsdb.openstack.netvirt.translator.NeutronNetwork;
import org.opendaylight.ovsdb.openstack.netvirt.SouthboundEvent.Type;
import org.opendaylight.ovsdb.openstack.netvirt.api.Action;
import org.opendaylight.ovsdb.openstack.netvirt.api.BridgeConfigurationManager;
import org.opendaylight.ovsdb.openstack.netvirt.api.ConfigurationService;
import org.opendaylight.ovsdb.openstack.netvirt.api.EventDispatcher;
import org.opendaylight.ovsdb.openstack.netvirt.api.NetworkingProvider;
import org.opendaylight.ovsdb.openstack.netvirt.api.NetworkingProviderManager;
import org.opendaylight.ovsdb.openstack.netvirt.api.NodeCacheManager;
import org.opendaylight.ovsdb.openstack.netvirt.api.OvsdbInventoryListener.OvsdbType;
import org.opendaylight.ovsdb.openstack.netvirt.api.OvsdbInventoryService;
import org.opendaylight.ovsdb.openstack.netvirt.api.Southbound;
import org.opendaylight.ovsdb.openstack.netvirt.api.TenantNetworkManager;
import org.opendaylight.ovsdb.openstack.netvirt.impl.NeutronL3Adapter;
import org.opendaylight.ovsdb.utils.servicehelper.ServiceHelper;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbBridgeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.node.TerminationPoint;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * Unit test fort {@link SouthboundHandler}
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(ServiceHelper.class)
public class SouthboundHandlerTest {

    @InjectMocks private SouthboundHandler southboundHandler;
    private SouthboundHandler southboundHandlerSpy;

    @Mock private ConfigurationService configurationService;
    @Mock private BridgeConfigurationManager bridgeConfigurationManager;
    @Mock private TenantNetworkManager tenantNetworkManager;
    @Mock private NetworkingProviderManager networkingProviderManager;
    @Mock private NeutronL3Adapter neutronL3Adapter;
    @Mock private NodeCacheManager nodeCacheManager;
    @Mock private OvsdbInventoryService ovsdbInventoryService;
    @Mock private Southbound southbound;

    @Before
    public void setUp() {
        southboundHandler.eventDispatcher = mock(EventDispatcher.class);
        southboundHandlerSpy = Mockito.spy(southboundHandler);
    }

    @Test
    public void testTriggerUpdates() {
        Node node = mock(Node.class);
        when(node.getAugmentation(OvsdbNodeAugmentation.class)).thenReturn(mock(OvsdbNodeAugmentation.class));

        List<Node> nodes = new ArrayList<Node>();
        nodes.add(mock(Node.class));
        when(southbound.readOvsdbTopologyNodes()).thenReturn(nodes);

        southboundHandlerSpy.triggerUpdates();
        verify(southboundHandlerSpy, times(1)).ovsdbUpdate(any(Node.class), any(DataObject.class), any(OvsdbType.class), any(Action.class));;
    }

    @Test
    public void testNotifyNode() {
        when(southbound.getBridge(any(Node.class))).thenReturn(mock(OvsdbBridgeAugmentation.class));

        NetworkingProvider networkingProvider = mock(NetworkingProvider.class);
        when(networkingProviderManager.getProvider(any(Node.class))).thenReturn(networkingProvider);

        southboundHandlerSpy.notifyNode(mock(Node.class), Action.ADD);

        verify(networkingProvider, times(1)).initializeOFFlowRules(any(Node.class));
    }

    @Test
    public void testProcessEvent() {
        SouthboundEvent ev = mock(SouthboundEvent.class);
        Node node = mock(Node.class);

        // NODE
        when(ev.getNode()).thenReturn(node);
        when(ev.getAugmentationData()).thenReturn(mock(OvsdbNodeAugmentation.class));
        when(ev.getType()).thenReturn(Type.NODE);

        when(ev.getAction()).thenReturn(Action.ADD);
        southboundHandler.processEvent(ev);
        verify(nodeCacheManager, times(1)).nodeAdded(any(Node.class));
        verify(bridgeConfigurationManager, times(1)).prepareNode(any(Node.class));
        Mockito.reset(nodeCacheManager);

        when(ev.getAction()).thenReturn(Action.UPDATE);
        southboundHandler.processEvent(ev);
        verify(nodeCacheManager, times(1)).nodeAdded(any(Node.class));
        Mockito.reset(nodeCacheManager);

        when(ev.getAction()).thenReturn(Action.DELETE);
        southboundHandler.processEvent(ev);
        verify(nodeCacheManager, times(1)).nodeRemoved(any(Node.class));
        Mockito.reset(nodeCacheManager);

        Mockito.reset(ev);

        // BRIDGE
        when(ev.getNode()).thenReturn(node);
        when(ev.getAugmentationData()).thenReturn(mock(OvsdbBridgeAugmentation.class));
        when(ev.getType()).thenReturn(Type.BRIDGE);

        when(southbound.getDatapathId(any(OvsdbBridgeAugmentation.class))).thenReturn("45");

        when(ev.getAction()).thenReturn(Action.ADD);
        southboundHandler.processEvent(ev);
        verify(nodeCacheManager, times(1)).nodeAdded(any(Node.class));
        Mockito.reset(nodeCacheManager);

        when(ev.getAction()).thenReturn(Action.UPDATE);
        southboundHandler.processEvent(ev);
        verify(nodeCacheManager, times(1)).nodeAdded(any(Node.class));
        Mockito.reset(nodeCacheManager);

        Mockito.reset(nodeCacheManager);
        when(ev.getAction()).thenReturn(Action.DELETE);
        southboundHandler.processEvent(ev);
        verify(nodeCacheManager, times(1)).nodeRemoved(any(Node.class));
        verify(southbound, times(1)).deleteBridge(any(Node.class));
        Mockito.reset(nodeCacheManager);
        Mockito.reset(southbound);

        Mockito.reset(ev);

        // PORT
        OvsdbTerminationPointAugmentation ovsdbTerminationPointAugmentation = mock(OvsdbTerminationPointAugmentation.class);
        when(ovsdbTerminationPointAugmentation.getName()).thenReturn("network");
//        Mockito.<Class<?>>when(ovsdbTerminationPointAugmentation.getInterfaceType()).thenReturn(InterfaceTypeBase.class);
//        when(ovsdbTerminationPointAugmentation.getInterfaceType()).thenReturn(n);
        when(ev.getNode()).thenReturn(node);
        when(ev.getAugmentationData()).thenReturn(ovsdbTerminationPointAugmentation);
        when(ev.getType()).thenReturn(Type.PORT);

        NetworkingProvider networkingProvider = mock(NetworkingProvider.class);
        NeutronNetwork neutronNetwork = mock(NeutronNetwork.class);
        when(neutronNetwork.getRouterExternal()).thenReturn(false);
        when(tenantNetworkManager.getTenantNetwork(any(OvsdbTerminationPointAugmentation.class))).thenReturn(neutronNetwork);
        when(networkingProviderManager.getProvider(any(Node.class))).thenReturn(networkingProvider);
        when(bridgeConfigurationManager.createLocalNetwork(any(Node.class), any(NeutronNetwork.class))).thenReturn(true);

        when(ev.getAction()).thenReturn(Action.ADD);
        southboundHandler.processEvent(ev);
        verify(neutronL3Adapter, times(1)).handleInterfaceEvent(any(Node.class), any(OvsdbTerminationPointAugmentation.class), any(NeutronNetwork.class), any(Action.class));
        verify(networkingProvider, times(1)).handleInterfaceUpdate(any(NeutronNetwork.class), any(Node.class), any(OvsdbTerminationPointAugmentation.class));
        Mockito.reset(neutronL3Adapter);
        Mockito.reset(networkingProvider);

        when(ev.getAction()).thenReturn(Action.UPDATE);
        southboundHandler.processEvent(ev);
        verify(neutronL3Adapter, times(1)).handleInterfaceEvent(any(Node.class), any(OvsdbTerminationPointAugmentation.class), any(NeutronNetwork.class), any(Action.class));
        verify(networkingProvider, times(1)).handleInterfaceUpdate(any(NeutronNetwork.class), any(Node.class), any(OvsdbTerminationPointAugmentation.class));
        Mockito.reset(neutronL3Adapter);
        Mockito.reset(networkingProvider);

//        List<String> phyIfName = new ArrayList<String>();
//        phyIfName.add("network");
//        when(bridgeConfigurationManager.getAllPhysicalInterfaceNames(any(Node.class))).thenReturn(phyIfName);
//
//        when(ev.getAction()).thenReturn(Action.DELETE); //isInterfaceOfIntereset true
//        southboundHandler.processEvent(ev);
//        verify(neutronL3Adapter, times(1)).handleInterfaceEvent(any(Node.class), any(OvsdbTerminationPointAugmentation.class), any(NeutronNetwork.class), any(Action.class));
//        verify(networkingProvider, times(1)).handleInterfaceDelete(anyString(), any(NeutronNetwork.class), any(Node.class), any(OvsdbTerminationPointAugmentation.class), anyBoolean());
//
//        Mockito.reset(networkingProvider);
//
//        when(southbound.getBridge(any(Node.class))).thenReturn(mock(OvsdbBridgeAugmentation.class));
//        List<TerminationPoint> terminationPoints = new ArrayList<TerminationPoint>();
//        TerminationPoint terminationPoint =  mock(TerminationPoint.class);
//        OvsdbTerminationPointAugmentation ovsdbTerminationPointAugmentation2 = mock(OvsdbTerminationPointAugmentation.class);
//        when(terminationPoint.getAugmentation(any(Class.class))).thenReturn(ovsdbTerminationPointAugmentation);
//        Uuid uuid = mock(Uuid.class);
//        when(ovsdbTerminationPointAugmentation.getInterfaceUuid()).thenReturn(uuid);
//        when(ovsdbTerminationPointAugmentation2.getInterfaceUuid()).thenReturn(uuid);
//        terminationPoints.add(terminationPoint);
//        when(node.getTerminationPoint()).thenReturn(terminationPoints);
//
//        when(ev.getAction()).thenReturn(Action.DELETE); //isInterfaceOfIntereset false - network != null
//        southboundHandler.processEvent(ev);
//        verify(networkingProvider, times(1)).handleInterfaceDelete(anyString(), any(NeutronNetwork.class), any(Node.class), any(OvsdbTerminationPointAugmentation.class), anyBoolean());

        // OPENVSWITCH
        when(ev.getNode()).thenReturn(node);
        when(ev.getAugmentationData()).thenReturn(ovsdbTerminationPointAugmentation);
        when(ev.getType()).thenReturn(Type.OPENVSWITCH);

        when(ovsdbTerminationPointAugmentation.getName()).thenReturn("network");
        List<TerminationPoint> terminationPoints = new ArrayList<TerminationPoint>();
        terminationPoints.add(mock(TerminationPoint.class));
        when(southbound.extractTerminationPoints(any(Node.class))).thenReturn(terminationPoints);

        when(ev.getAction()).thenReturn(Action.ADD);
        southboundHandler.processEvent(ev);
        verify(neutronL3Adapter, times(1)).handleInterfaceEvent(any(Node.class), any(OvsdbTerminationPointAugmentation.class), any(NeutronNetwork.class), any(Action.class));
        verify(networkingProvider, times(1)).handleInterfaceUpdate(any(NeutronNetwork.class), any(Node.class), any(OvsdbTerminationPointAugmentation.class));
        Mockito.reset(neutronL3Adapter);
        Mockito.reset(networkingProvider);
    }

    @Test
    public void testSetDependencies() throws Exception {
        ConfigurationService configurationService = mock(ConfigurationService.class);
        NetworkingProviderManager networkingProviderManager = mock(NetworkingProviderManager.class);
        TenantNetworkManager tenantNetworkManager = mock(TenantNetworkManager.class);
        BridgeConfigurationManager bridgeConfigurationManager = mock(BridgeConfigurationManager.class);
        NodeCacheManager nodeCacheManager = mock(NodeCacheManager.class);
        NeutronL3Adapter neutronL3Adapter = mock(NeutronL3Adapter.class);
        Southbound southbound = mock(Southbound.class);
        EventDispatcher eventDispatcher = mock(EventDispatcher.class);
        OvsdbInventoryService ovsdbInventoryService = mock(OvsdbInventoryService.class);

        PowerMockito.mockStatic(ServiceHelper.class);
        PowerMockito.when(ServiceHelper.getGlobalInstance(ConfigurationService.class, southboundHandler)).thenReturn(configurationService);
        PowerMockito.when(ServiceHelper.getGlobalInstance(NetworkingProviderManager.class, southboundHandler)).thenReturn(networkingProviderManager);
        PowerMockito.when(ServiceHelper.getGlobalInstance(TenantNetworkManager.class, southboundHandler)).thenReturn(tenantNetworkManager);
        PowerMockito.when(ServiceHelper.getGlobalInstance(BridgeConfigurationManager.class, southboundHandler)).thenReturn(bridgeConfigurationManager);
        PowerMockito.when(ServiceHelper.getGlobalInstance(NodeCacheManager.class, southboundHandler)).thenReturn(nodeCacheManager);
        PowerMockito.when(ServiceHelper.getGlobalInstance(NeutronL3Adapter.class, southboundHandler)).thenReturn(neutronL3Adapter);
        PowerMockito.when(ServiceHelper.getGlobalInstance(Southbound.class, southboundHandler)).thenReturn(southbound);
        PowerMockito.when(ServiceHelper.getGlobalInstance(EventDispatcher.class, southboundHandler)).thenReturn(eventDispatcher);
        PowerMockito.when(ServiceHelper.getGlobalInstance(OvsdbInventoryService.class, southboundHandler)).thenReturn(ovsdbInventoryService);

        southboundHandler.setDependencies(mock(BundleContext.class), mock(ServiceReference.class));
        assertEquals("Error, did not return the correct object", getField("configurationService"), configurationService);
        assertEquals("Error, did not return the correct object", getField("networkingProviderManager"), networkingProviderManager);
        assertEquals("Error, did not return the correct object", getField("tenantNetworkManager"), tenantNetworkManager);
        assertEquals("Error, did not return the correct object", getField("bridgeConfigurationManager"), bridgeConfigurationManager);
        assertEquals("Error, did not return the correct object", getField("nodeCacheManager"), nodeCacheManager);
        assertEquals("Error, did not return the correct object", getField("neutronL3Adapter"), neutronL3Adapter);
        assertEquals("Error, did not return the correct object", getField("southbound"), southbound);
        assertEquals("Error, did not return the correct object", getField("ovsdbInventoryService"), ovsdbInventoryService);
        assertEquals("Error, did not return the correct object", southboundHandler.eventDispatcher, eventDispatcher);
    }

    private Object getField(String fieldName) throws Exception {
        Field field = SouthboundHandler.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(southboundHandler);
    }
}
