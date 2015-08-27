/*
 * Copyright (c) 2015 Inocybe and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.opendaylight.ovsdb.openstack.netvirt.translator.INeutronNetworkCRUD;
import org.opendaylight.ovsdb.openstack.netvirt.translator.INeutronPortCRUD;
import org.opendaylight.ovsdb.openstack.netvirt.translator.NeutronNetwork;
import org.opendaylight.ovsdb.openstack.netvirt.translator.NeutronPort;
import org.opendaylight.ovsdb.openstack.netvirt.api.NetworkingProviderManager;
import org.opendaylight.ovsdb.openstack.netvirt.api.Southbound;
import org.opendaylight.ovsdb.openstack.netvirt.api.VlanConfigurationCache;
import org.opendaylight.ovsdb.utils.servicehelper.ServiceHelper;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * Unit test for {@link TenantNetworkManagerImpl}
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(ServiceHelper.class)
public class TenantNetworkManagerImplTest {

    @InjectMocks private TenantNetworkManagerImpl tenantNetworkManagerImpl;

    @Mock private INeutronPortCRUD neutronPortCache;
    @Mock private INeutronNetworkCRUD neutronNetworkCache;
    @Mock private VlanConfigurationCache vlanConfigurationCache;
    @Mock private NetworkingProviderManager networkingProviderManager;
    @Mock private Southbound southbound;

    private static final String NETWORK_ID= "networkId";
    private static final String SEG_ID = "segId";
    private static final String INTERFACE_ID = "intId";
    /**
     * Test method {@link TenantNetworkManagerImpl#getInternalVlan(Node, String)}
     */
    @Test
    public void testGetInternalVlan() {
        when(vlanConfigurationCache.getInternalVlan(any(Node.class), eq(NETWORK_ID))).thenReturn(10);

        assertEquals("Error, did not return the correct internalVlan" , 10, tenantNetworkManagerImpl.getInternalVlan(mock(Node.class), NETWORK_ID));
        assertEquals("Error, did not return the correct internalVlan", 0, tenantNetworkManagerImpl.getInternalVlan(mock(Node.class), "unexistingNetwork"));

        verify(vlanConfigurationCache, times(2)).getInternalVlan(any(Node.class), anyString());
    }

    /**
     * Test method {@link TenantNetworkManagerImpl#reclaimInternalVlan(Node, String, NeutronNetwork)}
     */
    @Test
    public void testReclaimInternalVlan() {
        when(vlanConfigurationCache.reclaimInternalVlan(any(Node.class), eq(NETWORK_ID))).thenReturn(10);

        tenantNetworkManagerImpl.reclaimInternalVlan(mock(Node.class), mock(NeutronNetwork.class));
        tenantNetworkManagerImpl.reclaimInternalVlan(mock(Node.class), mock(NeutronNetwork.class));

        verify(vlanConfigurationCache, times(2)).reclaimInternalVlan(any(Node.class), anyString());
    }

    /**
     * Test method {@link TenantNetworkManagerImpl#programInternalVlan(Node, String, NeutronNetwork)}
     */
    @Test
    public void testProgramInternalVlan(){
        when(vlanConfigurationCache.getInternalVlan(any(Node.class), anyString())).thenReturn(10);

        tenantNetworkManagerImpl.programInternalVlan(mock(Node.class), mock(OvsdbTerminationPointAugmentation.class), mock(NeutronNetwork.class));

        verify(vlanConfigurationCache, times(1)).getInternalVlan(any(Node.class), anyString());
    }

    /**
     * Test method {@link TenantNetworkManagerImpl#isTenantNetworkPresentInNode(Node, String)}
     */
    @Test
    public void testIsTenantNetworkPresentInNode() {
        NeutronNetwork neutronNetwork = mock(NeutronNetwork.class);
        when(neutronNetwork.getProviderSegmentationID()).thenReturn(SEG_ID);
        when(neutronNetwork.getNetworkUUID()).thenReturn(NETWORK_ID);
        List<NeutronNetwork> listNeutronNetwork = new ArrayList<NeutronNetwork>();
        listNeutronNetwork.add(neutronNetwork);
        when(neutronNetworkCache.getAllNetworks()).thenReturn(listNeutronNetwork);

        when(southbound.getInterfaceExternalIdsValue(any(OvsdbTerminationPointAugmentation.class), anyString())).thenReturn(INTERFACE_ID);
        NeutronPort neutronPort = mock(NeutronPort.class);
        when(neutronPort.getNetworkUUID()).thenReturn(NETWORK_ID);
        when(neutronPortCache.getPort(anyString())).thenReturn(neutronPort);

        List<OvsdbTerminationPointAugmentation> ports = new ArrayList<OvsdbTerminationPointAugmentation>();
        ports.add(mock(OvsdbTerminationPointAugmentation.class));
        when(southbound.getTerminationPointsOfBridge(any(Node.class))).thenReturn(ports);

        assertTrue("Error, did not return correct boolean for isTenantNetworkPresentInNode", tenantNetworkManagerImpl.isTenantNetworkPresentInNode(mock(Node.class), SEG_ID));
    }

    /**
     * Test method {@link TenantNetworkManagerImpl#getNetworkId(String)}
     */
    @Test
    public void testGetNetworkId() {
        NeutronNetwork neutronNetwork = mock(NeutronNetwork.class);
        List<NeutronNetwork> listNeutronNetwork = new ArrayList<NeutronNetwork>();
        listNeutronNetwork.add(neutronNetwork);

        when(neutronNetwork.getProviderSegmentationID()).thenReturn("segId");
        when(neutronNetwork.getNetworkUUID()).thenReturn("networkUUID");
        when(neutronNetworkCache.getAllNetworks()).thenReturn(listNeutronNetwork);

        assertEquals("Error, did not return the UUID of the correct network", listNeutronNetwork.get(0).getNetworkUUID(), tenantNetworkManagerImpl.getNetworkId("segId"));

        verify(neutronNetworkCache, times(1)).getAllNetworks();
    }

    /**
     * Test method {@link TenantNetworkManagerImpl#getTenantNetwork(Interface)}
     */
    @Test
    public void testGetTenantNetwork() {
        when(southbound.getInterfaceExternalIdsValue(any(OvsdbTerminationPointAugmentation.class), anyString())).thenReturn(INTERFACE_ID);
        NeutronPort neutronPort = mock(NeutronPort.class);
        when(neutronPortCache.getPort(anyString())).thenReturn(neutronPort);
        NeutronNetwork neutronNetwork = mock(NeutronNetwork.class);
        when(neutronNetworkCache.getNetwork(anyString())).thenReturn(neutronNetwork);

        assertEquals("Error, did not return the correct tenant", neutronNetwork, tenantNetworkManagerImpl.getTenantNetwork(mock(OvsdbTerminationPointAugmentation.class)));
    }

    @Test
    public void testGetTenantPort() {
        when(southbound.getInterfaceExternalIdsValue(any(OvsdbTerminationPointAugmentation.class), anyString())).thenReturn(INTERFACE_ID);
        NeutronPort neutronPort = mock(NeutronPort.class);
        when(neutronPortCache.getPort(anyString())).thenReturn(neutronPort);

        assertEquals("Error, did not return the correct tenant", neutronPort, tenantNetworkManagerImpl.getTenantPort(mock(OvsdbTerminationPointAugmentation.class)));
    }

    @Test
    public void testNetworkCreated() {
        tenantNetworkManagerImpl.networkCreated(mock(Node.class), NETWORK_ID);
        verify(vlanConfigurationCache, times(1)).assignInternalVlan(any(Node.class), anyString());
    }

    @Test
    public void testSetDependencies() throws Exception {
        VlanConfigurationCache vlanConfigurationCache = mock(VlanConfigurationCache.class);
        Southbound southbound = mock(Southbound.class);

        PowerMockito.mockStatic(ServiceHelper.class);
        PowerMockito.when(ServiceHelper.getGlobalInstance(VlanConfigurationCache.class, tenantNetworkManagerImpl)).thenReturn(vlanConfigurationCache);
        PowerMockito.when(ServiceHelper.getGlobalInstance(Southbound.class, tenantNetworkManagerImpl)).thenReturn(southbound);

        tenantNetworkManagerImpl.setDependencies(mock(BundleContext.class), mock(ServiceReference.class));

        assertEquals("Error, did not return the correct object", getField("vlanConfigurationCache"), vlanConfigurationCache);
        assertEquals("Error, did not return the correct object", getField("southbound"), southbound);
    }

    @Test
    public void testSetDependenciesObject() throws Exception{
        INeutronNetworkCRUD neutronNetworkCache = mock(INeutronNetworkCRUD.class);
        tenantNetworkManagerImpl.setDependencies(neutronNetworkCache);
        assertEquals("Error, did not return the correct object", getField("neutronNetworkCache"), neutronNetworkCache);

        INeutronPortCRUD neutronPortCache = mock(INeutronPortCRUD.class);
        tenantNetworkManagerImpl.setDependencies(neutronPortCache);
        assertEquals("Error, did not return the correct object", getField("neutronPortCache"), neutronPortCache);
    }

    private Object getField(String fieldName) throws Exception {
        Field field = TenantNetworkManagerImpl.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(tenantNetworkManagerImpl);
    }
}
