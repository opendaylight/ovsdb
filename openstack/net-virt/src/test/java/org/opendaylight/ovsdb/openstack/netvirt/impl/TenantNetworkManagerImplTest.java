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
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.neutron.spi.INeutronNetworkCRUD;
import org.opendaylight.neutron.spi.INeutronPortCRUD;
import org.opendaylight.neutron.spi.NeutronNetwork;
import org.opendaylight.neutron.spi.NeutronPort;
import org.opendaylight.ovsdb.lib.notation.Column;
import org.opendaylight.ovsdb.lib.notation.Row;
import org.opendaylight.ovsdb.lib.schema.GenericTableSchema;
import org.opendaylight.ovsdb.openstack.netvirt.api.Constants;
import org.opendaylight.ovsdb.openstack.netvirt.api.NetworkingProvider;
import org.opendaylight.ovsdb.openstack.netvirt.api.NetworkingProviderManager;
import org.opendaylight.ovsdb.openstack.netvirt.api.VlanConfigurationCache;
import org.opendaylight.ovsdb.plugin.api.OvsdbConfigurationService;
import org.opendaylight.ovsdb.plugin.api.Status;
import org.opendaylight.ovsdb.schema.openvswitch.Bridge;
import org.opendaylight.ovsdb.schema.openvswitch.Interface;
import org.opendaylight.ovsdb.schema.openvswitch.Port;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;

@RunWith(MockitoJUnitRunner.class)
public class TenantNetworkManagerImplTest {

    @Mock private VlanConfigurationCache vlanConfigurationCache;
    @Mock private INeutronPortCRUD neutronCache;
    @Mock private OvsdbConfigurationService ovsdbConfigurationService;
    @Mock private NetworkingProviderManager networkingProviderManager;

    @InjectMocks private TenantNetworkManagerImpl tenantNetworkManagerImpl;
    @InjectMocks private INeutronPortCRUD neutronPortCache = mock(INeutronPortCRUD.class);
    @InjectMocks private INeutronNetworkCRUD neutronNetworkCache = mock(INeutronNetworkCRUD.class);

    private static final String networkId= "networkId";

    @Test
    public void testGetInternalVlan() {
        when(vlanConfigurationCache.getInternalVlan(any(Node.class), eq(networkId))).thenReturn(10);

        assertEquals(10, tenantNetworkManagerImpl.getInternalVlan(mock(Node.class), networkId));
        assertEquals(0, tenantNetworkManagerImpl.getInternalVlan(mock(Node.class), "unexistingNetwork"));
    }

    @Test
    public void testReclaimInternalVlan() {
        verifyNoMoreInteractions(vlanConfigurationCache);

        when(vlanConfigurationCache.reclaimInternalVlan(any(Node.class), eq(networkId))).thenReturn(10);

        tenantNetworkManagerImpl.reclaimInternalVlan(mock(Node.class), networkId, mock(NeutronNetwork.class));
        tenantNetworkManagerImpl.reclaimInternalVlan(mock(Node.class), "unexistingNetwork", mock(NeutronNetwork.class));
    }

    @Test
    public void testProgramInternalVlan(){
        Port port = mock(Port.class);
        Row row = mock(Row.class);
        GenericTableSchema tableSchema = mock(GenericTableSchema.class);
        Status status = mock(Status.class);

        verifyNoMoreInteractions(vlanConfigurationCache);
        verifyNoMoreInteractions(ovsdbConfigurationService);

        when(port.getRow()).thenReturn(row);
        when(port.getSchema()).thenReturn(tableSchema);

        when(vlanConfigurationCache.getInternalVlan(any(Node.class), anyString())).thenReturn(10);
        when(ovsdbConfigurationService.createTypedRow(any(Node.class), same(Port.class))).thenReturn(port);
        when(ovsdbConfigurationService.updateRow(any(Node.class), anyString(), anyString(), anyString(), any(Row.class))).thenReturn(status);

        tenantNetworkManagerImpl.programInternalVlan(mock(Node.class), networkId, mock(NeutronNetwork.class));
    }

    @Test
    public void testIsTenantNetworkPresentInNode() {
        NetworkingProvider networkingProvider = mock(NetworkingProvider.class);

        Interface intf = mock(Interface.class);
        Column<GenericTableSchema, Map<String, String>> columnMock = mock(Column.class);
        Map<String, String> externalIds = new HashMap<String, String>();
        externalIds.put(Constants.EXTERNAL_ID_INTERFACE_ID, "interfaceId");

        Row row = mock(Row.class);
        Bridge bridge = mock(Bridge.class, RETURNS_DEEP_STUBS);


        ConcurrentHashMap<String, Row> map;
        map = new ConcurrentHashMap<>();
        map.put("row", row);

        NeutronNetwork neutronNetwork = mock(NeutronNetwork.class);
        NeutronPort neutronPort = mock(NeutronPort.class);
        ArrayList<NeutronNetwork> listNeutronNetwork = new ArrayList<NeutronNetwork>();
        listNeutronNetwork.add(neutronNetwork);

        verifyNoMoreInteractions(ovsdbConfigurationService);
        verifyNoMoreInteractions(neutronNetworkCache);
        verifyNoMoreInteractions(neutronPortCache);
        verifyNoMoreInteractions(networkingProviderManager);
        verifyNoMoreInteractions(vlanConfigurationCache);

        when(neutronNetwork.getProviderSegmentationID()).thenReturn("segId");
        when(neutronNetwork.getNetworkUUID()).thenReturn("networkUUID");
        when(neutronNetworkCache.getAllNetworks()).thenReturn(listNeutronNetwork);

        assertEquals(listNeutronNetwork.get(0).getNetworkUUID(), tenantNetworkManagerImpl.getNetworkId("segId"));

        when(networkingProviderManager.getProvider(any(Node.class))).thenReturn(networkingProvider);
        when(networkingProvider.hasPerTenantTunneling()).thenReturn(true);
        when(vlanConfigurationCache.getInternalVlan(any(Node.class), anyString())).thenReturn(10);

        when(ovsdbConfigurationService.getRows(any(Node.class), anyString())).thenReturn(map);
        when(ovsdbConfigurationService.getTypedRow(any(Node.class), same(Interface.class),
                any(Row.class))).thenReturn(intf);

        when(intf.getExternalIdsColumn()).thenReturn(columnMock);
        when(columnMock.getData()).thenReturn(externalIds);

        when(neutronPortCache.getPort(anyString())).thenReturn(neutronPort);
        when(neutronPort.getNetworkUUID()).thenReturn("networkUUID");

        assertTrue(tenantNetworkManagerImpl.isTenantNetworkPresentInNode(mock(Node.class), "segId"));

        verify(ovsdbConfigurationService, times(1)).getTypedRow(any(Node.class), any(Class.class), any(Row.class));
    }

    @Test
    public void testGetNetworkId() {
        NeutronNetwork neutronNetwork = mock(NeutronNetwork.class);
        ArrayList<NeutronNetwork> listNeutronNetwork = new ArrayList<NeutronNetwork>();
        listNeutronNetwork.add(neutronNetwork);

        verifyNoMoreInteractions(neutronNetworkCache);

        when(neutronNetwork.getProviderSegmentationID()).thenReturn("segId");
        when(neutronNetwork.getNetworkUUID()).thenReturn("networkUUID");
        when(neutronNetworkCache.getAllNetworks()).thenReturn(listNeutronNetwork);

        assertEquals(listNeutronNetwork.get(0).getNetworkUUID(), tenantNetworkManagerImpl.getNetworkId("segId"));
    }

    @Test
    public void testGetTenantNetwork() {
        Interface intf = mock(Interface.class);
        Column<GenericTableSchema, Map<String, String>> columnMock = mock(Column.class);
        Map<String, String> externalIds = new HashMap<String, String>();
        externalIds.put(Constants.EXTERNAL_ID_INTERFACE_ID, "tenantValue");
        NeutronPort neutronPort = mock(NeutronPort.class);
        NeutronNetwork neutronNetwork = mock(NeutronNetwork.class);

        verifyNoMoreInteractions(neutronPortCache);
        verifyNoMoreInteractions(neutronNetworkCache);

        when(intf.getExternalIdsColumn()).thenReturn(columnMock);

        when(columnMock.getData()).thenReturn(externalIds);

        when(neutronPort.getNetworkUUID()).thenReturn("neutronUUID");
        when(neutronPortCache.getPort(anyString())).thenReturn(neutronPort);
        when(neutronNetworkCache.getNetwork(anyString())).thenReturn(neutronNetwork);

        assertEquals(neutronNetwork, tenantNetworkManagerImpl.getTenantNetwork(intf));
    }

    @Test
    public void testNetworkCreated() {
        verifyNoMoreInteractions(vlanConfigurationCache);

        when(vlanConfigurationCache.assignInternalVlan(any(Node.class), anyString())).thenReturn(10);

        assertEquals(10,tenantNetworkManagerImpl.networkCreated(mock(Node.class), networkId));
    }
}
