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
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.opendaylight.neutron.spi.INeutronNetworkCRUD;
import org.opendaylight.neutron.spi.NeutronNetwork;
import org.opendaylight.ovsdb.openstack.netvirt.api.Action;
import org.opendaylight.ovsdb.openstack.netvirt.api.BridgeConfigurationManager;
import org.opendaylight.ovsdb.openstack.netvirt.api.EventDispatcher;
import org.opendaylight.ovsdb.openstack.netvirt.api.NodeCacheManager;
import org.opendaylight.ovsdb.openstack.netvirt.api.Southbound;
import org.opendaylight.ovsdb.openstack.netvirt.api.TenantNetworkManager;
import org.opendaylight.ovsdb.openstack.netvirt.impl.NeutronL3Adapter;
import org.opendaylight.ovsdb.utils.servicehelper.ServiceHelper;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * Unit test for {@link NetworkHandler}
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(ServiceHelper.class)
public class NetworkHandlerTest {

    @InjectMocks private NetworkHandler networkHandler;

    @Mock private NeutronNetwork sharedNeutronNetwork;
    @Mock private NeutronNetwork nonSharedNeutronNetwork;

    @Mock private NeutronL3Adapter neutronL3Adapter;
    @Mock private TenantNetworkManager tenantNetworkManager;
    @Mock private INeutronNetworkCRUD neutronNetworkCache;
    @Mock private BridgeConfigurationManager bridgeConfigurationManager;
    @Mock private NodeCacheManager nodeCacheManager;
    @Mock private Southbound southbound;

    @Before
    public void setup() {
        when(sharedNeutronNetwork.isShared()).thenReturn(true);
        when(nonSharedNeutronNetwork.isShared()).thenReturn(false);
    }

    /**
     * Test method {@link NetworkHandler#canCreateNetwork(NeutronNetwork)}
     */
    @Test
    public void testCanCreateNetwork() {
        assertEquals("Error, did not return the correct HTTP flag", HttpURLConnection.HTTP_NOT_ACCEPTABLE, networkHandler.canCreateNetwork(sharedNeutronNetwork));
        assertEquals("Error, did not return the correct HTTP flag", HttpURLConnection.HTTP_OK, networkHandler.canCreateNetwork(nonSharedNeutronNetwork));
    }

    /**
     * Test method {@link NetworkHandler#canUpdateNetwork(NeutronNetwork, NeutronNetwork)}
     */
    @Test
    public void testCanUpdateNetwork() {
        assertEquals("Error, did not return the correct HTTP flag", HttpURLConnection.HTTP_NOT_ACCEPTABLE, networkHandler.canUpdateNetwork(sharedNeutronNetwork, sharedNeutronNetwork));
        assertEquals("Error, did not return the correct HTTP flag", HttpURLConnection.HTTP_OK, networkHandler.canUpdateNetwork(nonSharedNeutronNetwork, nonSharedNeutronNetwork));
    }

    /**
     * Test method {@link NetworkHandler#canDeleteNetwork(NeutronNetwork)}
     */
    @Test
    public void testCanDeleteNetwork() {
        assertEquals("Error, did not return the correct HTTP flag", HttpURLConnection.HTTP_OK, networkHandler.canDeleteNetwork(nonSharedNeutronNetwork));
    }

    /**
     * Test method {@link NetworkHandler#processEvent(AbstractEvent)}
     */
    @Test
    public void testProcessEvent() {
        NetworkHandler networkHandlerSpy = Mockito.spy(networkHandler);

        NorthboundEvent ev = mock(NorthboundEvent.class);
        when(ev.getNeutronNetwork()).thenReturn(nonSharedNeutronNetwork);

        when(ev.getAction()).thenReturn(Action.ADD);
        networkHandlerSpy.processEvent(ev);
        verify(neutronL3Adapter, times(1)).handleNeutronNetworkEvent(any(NeutronNetwork.class), same(Action.ADD));

        when(ev.getAction()).thenReturn(Action.UPDATE);
        networkHandlerSpy.processEvent(ev);
        verify(neutronL3Adapter, times(1)).handleNeutronNetworkEvent(any(NeutronNetwork.class), same(Action.UPDATE));

        String portName = "portName";

        List<NeutronNetwork> networks = new ArrayList<>();
        when(neutronNetworkCache.getAllNetworks()).thenReturn(networks);

        List<Node> nodes = new ArrayList<>();
        nodes.add(mock(Node.class));
        when(nodeCacheManager.getNodes()).thenReturn(nodes);

        List<String> phyIfName = new ArrayList<>();
        phyIfName.add(portName);
        when(bridgeConfigurationManager.getAllPhysicalInterfaceNames(any(Node.class))).thenReturn(phyIfName );

        List<OvsdbTerminationPointAugmentation> ports = new ArrayList<>();
        OvsdbTerminationPointAugmentation port = mock(OvsdbTerminationPointAugmentation.class);
        when(port.getName()).thenReturn(portName);
        ports.add(port);
        when(southbound.getTerminationPointsOfBridge(any(Node.class))).thenReturn(ports);

        when(southbound.isTunnel(any(OvsdbTerminationPointAugmentation.class))).thenReturn(true, false);

        when(ev.getAction()).thenReturn(Action.DELETE);
        networkHandlerSpy.processEvent(ev); // test delete with southbound.isTunnel(true)
        networkHandlerSpy.processEvent(ev); // test delete with southbound.isTunnel(false)
        // the functions are called once per call to processEvent()
        verify(neutronL3Adapter, times(2)).handleNeutronNetworkEvent(any(NeutronNetwork.class), same(Action.DELETE));
        verify(tenantNetworkManager, times(2)).networkDeleted(anyString());
        // depending on the southbound.isTunnel()
        verify(southbound, times(2)).deleteTerminationPoint(any(Node.class), anyString());
    }

    @Test
    public void testSetDependencies() throws Exception {
        TenantNetworkManager tenantNetworkManager = mock(TenantNetworkManager.class);
        BridgeConfigurationManager bridgeConfigurationManager = mock(BridgeConfigurationManager.class);
        NodeCacheManager nodeCacheManager = mock(NodeCacheManager.class);
        NeutronL3Adapter neutronL3Adapter = mock(NeutronL3Adapter.class);
        Southbound southbound = mock(Southbound.class);
        EventDispatcher eventDispatcher = mock(EventDispatcher.class);


        PowerMockito.mockStatic(ServiceHelper.class);
        PowerMockito.when(ServiceHelper.getGlobalInstance(TenantNetworkManager.class, networkHandler)).thenReturn(tenantNetworkManager);
        PowerMockito.when(ServiceHelper.getGlobalInstance(BridgeConfigurationManager.class, networkHandler)).thenReturn(bridgeConfigurationManager);
        PowerMockito.when(ServiceHelper.getGlobalInstance(NodeCacheManager.class, networkHandler)).thenReturn(nodeCacheManager);
        PowerMockito.when(ServiceHelper.getGlobalInstance(NeutronL3Adapter.class, networkHandler)).thenReturn(neutronL3Adapter);
        PowerMockito.when(ServiceHelper.getGlobalInstance(Southbound.class, networkHandler)).thenReturn(southbound);
        PowerMockito.when(ServiceHelper.getGlobalInstance(EventDispatcher.class, networkHandler)).thenReturn(eventDispatcher);

        networkHandler.setDependencies(mock(BundleContext.class), mock(ServiceReference.class));

        assertEquals("Error, did not return the correct object", getField("tenantNetworkManager"), tenantNetworkManager);
        assertEquals("Error, did not return the correct object", getField("bridgeConfigurationManager"), bridgeConfigurationManager);
        assertEquals("Error, did not return the correct object", getField("nodeCacheManager"), nodeCacheManager);
        assertEquals("Error, did not return the correct object", getField("neutronL3Adapter"), neutronL3Adapter);
        assertEquals("Error, did not return the correct object", getField("southbound"), southbound);
        assertEquals("Error, did not return the correct object", networkHandler.eventDispatcher, eventDispatcher);
    }

    @Test
    public void testSetDependenciesObject() throws Exception{
        INeutronNetworkCRUD iNeutronNetworkCRUD = mock(INeutronNetworkCRUD.class);
        networkHandler.setDependencies(iNeutronNetworkCRUD);
        assertEquals("Error, did not return the correct object", getField("neutronNetworkCache"), iNeutronNetworkCRUD);
}

    private Object getField(String fieldName) throws Exception {
        Field field = NetworkHandler.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(networkHandler);
    }
}
