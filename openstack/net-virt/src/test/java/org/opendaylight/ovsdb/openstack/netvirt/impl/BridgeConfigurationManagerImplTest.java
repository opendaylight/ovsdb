/*
* Copyright (c) 2014 Intel Corp. and others.  All rights reserved.
*
* This program and the accompanying materials are made available under the
* terms of the Eclipse Public License v1.0 which accompanies this distribution,
* and is available at http://www.eclipse.org/legal/epl-v10.html
*
* Authors : Marcus Koontz
*/
package org.opendaylight.ovsdb.openstack.netvirt.impl;

import org.junit.runner.RunWith;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.opendaylight.neutron.spi.NeutronNetwork;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.ovsdb.lib.notation.Column;
import org.opendaylight.ovsdb.lib.notation.Row;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.schema.GenericTableSchema;
import org.opendaylight.ovsdb.openstack.netvirt.api.ConfigurationService;
import org.opendaylight.ovsdb.openstack.netvirt.api.NetworkingProvider;
import org.opendaylight.ovsdb.openstack.netvirt.api.NetworkingProviderManager;
import org.opendaylight.ovsdb.plugin.api.OvsdbConfigurationService;
import org.opendaylight.ovsdb.schema.openvswitch.Bridge;
import org.opendaylight.ovsdb.schema.openvswitch.OpenVSwitch;
import org.opendaylight.ovsdb.schema.openvswitch.Port;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.RETURNS_MOCKS;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BridgeConfigurationManagerImplTest {

    private Node nodeMock = mock(Node.class, RETURNS_DEEP_STUBS);
    private Bridge bridgeMock = mock(Bridge.class, RETURNS_DEEP_STUBS);

    @Mock private OvsdbConfigurationService ovsdbConfigurationService;
    @Mock private ConfigurationService configurationService;
    @Mock private NetworkingProviderManager networkingProviderManager;


    @InjectMocks public static BridgeConfigurationManagerImpl bridgeConfigurationManagerImpl;
    @InjectMocks public static BridgeConfigurationManagerImpl bridgeConfigMock =
            mock(BridgeConfigurationManagerImpl.class, RETURNS_DEEP_STUBS);
    @Spy public static BridgeConfigurationManagerImpl bridgeConfigurationManagerImplSpy;

    @Test
    public void testGetBridgeUuid() throws Exception {
        Row row = mock(Row.class);
        Bridge bridge = mock(Bridge.class, RETURNS_DEEP_STUBS);

        ConcurrentHashMap<String, Row> hashMap;
        hashMap = new ConcurrentHashMap<>();
        hashMap.put("mockUUID", row);

        verifyNoMoreInteractions(ovsdbConfigurationService);
        verifyNoMoreInteractions(configurationService);
        verifyNoMoreInteractions(networkingProviderManager);

        when(ovsdbConfigurationService.getRows(any(Node.class), anyString())).thenReturn(hashMap);
        when(ovsdbConfigurationService.getTypedRow(any(Node.class), same(Bridge.class),
                any(Row.class))).thenReturn(bridge);
        when(bridge.getName()).thenReturn("test-bridge");

        assertEquals("Error, did not return UUID of correct bridge", "mockUUID",
                bridgeConfigurationManagerImpl.getBridgeUuid(nodeMock, "test-bridge"));

        verify(ovsdbConfigurationService).getTableName(nodeMock, Bridge.class);
        verifyNoMoreInteractions(configurationService);
        verifyNoMoreInteractions(networkingProviderManager);
    }

    @Test
    public void testIsNodeNeutronReady() throws Exception {
        Row row = mock(Row.class);
        Bridge bridge = mock(Bridge.class, RETURNS_DEEP_STUBS);

        ConcurrentHashMap<String, Row> hashMap;
        hashMap = new ConcurrentHashMap<>();
        hashMap.put("mockUUID", row);

        verifyNoMoreInteractions(ovsdbConfigurationService);
        verifyNoMoreInteractions(configurationService);
        verifyNoMoreInteractions(networkingProviderManager);

        assertEquals("Error, did not return correct boolean from isNodeNeutronReady", false,
                bridgeConfigurationManagerImpl.isNodeNeutronReady(nodeMock));

        when(ovsdbConfigurationService.getRows(any(Node.class), anyString())).thenReturn(hashMap);
        when(ovsdbConfigurationService.getTypedRow(any(Node.class), same(Bridge.class),
                any(Row.class))).thenReturn(bridge);
        when(bridge.getName()).thenReturn("test-bridge");
        when(configurationService.getIntegrationBridgeName()).thenReturn("test-bridge");

        assertEquals("Error, did not return correct boolean from isNodeNeutronReady", true,
                bridgeConfigurationManagerImpl.isNodeNeutronReady(nodeMock));

        verify(configurationService, times(2)).getIntegrationBridgeName();
        verifyNoMoreInteractions(networkingProviderManager);
    }

    @Test
    public void testIsNodeOverlayReady() throws Exception {
        Row row = mock(Row.class);
        Bridge bridge = mock(Bridge.class, RETURNS_DEEP_STUBS);

        ConcurrentHashMap<String, Row> hashMap;
        hashMap = new ConcurrentHashMap<>();
        hashMap.put("mockUUID", row);

        verifyNoMoreInteractions(ovsdbConfigurationService);
        verifyNoMoreInteractions(configurationService);
        verifyNoMoreInteractions(networkingProviderManager);

        assertEquals("Error, did not return correct boolean from isNodeOverlayReady", false,
                bridgeConfigurationManagerImpl.isNodeOverlayReady(nodeMock));

        when(ovsdbConfigurationService.getRows(any(Node.class), anyString())).thenReturn(hashMap);
        when(ovsdbConfigurationService.getTypedRow(any(Node.class), same(Bridge.class),
                any(Row.class))).thenReturn(bridge);
        when(bridge.getName()).thenReturn("test-bridge");
        when(configurationService.getIntegrationBridgeName()).thenReturn("test-bridge");
        when(configurationService.getNetworkBridgeName()).thenReturn("test-bridge");

        assertEquals("Error, did not return correct boolean from isNodeOverlayReady", true,
                bridgeConfigurationManagerImpl.isNodeOverlayReady(nodeMock));

        verify(configurationService, times(2)).getIntegrationBridgeName();
        verify(configurationService, times(1)).getNetworkBridgeName();
        verify(ovsdbConfigurationService, times(3)).getTableName(nodeMock, Bridge.class);
        verifyNoMoreInteractions(networkingProviderManager);
    }

    @Test
    public void testIsPortOnBridge() throws Exception {
        UUID uuid = mock(UUID.class);
        Set<UUID> uuidSet = new HashSet<>();
        uuidSet.add(uuid);
        Column<GenericTableSchema, Set<UUID>> columnMock = mock(Column.class);
        Port port = mock(Port.class, RETURNS_DEEP_STUBS);
        String portName = "portNameMock";

        verifyNoMoreInteractions(ovsdbConfigurationService);
        verifyNoMoreInteractions(configurationService);
        verifyNoMoreInteractions(networkingProviderManager);

        when(bridgeMock.getPortsColumn()).thenReturn(columnMock);
        when(columnMock.getData()).thenReturn(uuidSet);
        assertEquals("Error, did not return correct boolean from isPortOnBridge", false,
                bridgeConfigurationManagerImpl.isPortOnBridge(nodeMock, bridgeMock, portName));

        when(port.getName()).thenReturn(portName);

        when(ovsdbConfigurationService.getTypedRow(any(Node.class), any(Class.class), any(Row.class))).thenReturn(port);
        when(port.getName()).thenReturn(portName);

        assertEquals("Error, did not return correct boolean from isPortOnBridge", true,
                bridgeConfigurationManagerImpl.isPortOnBridge(nodeMock, bridgeMock, portName));

        verify(bridgeMock, times(2)).getPortsColumn();
        verify(ovsdbConfigurationService, times(2)).getRow(any(Node.class), anyString(), anyString());
        verify(ovsdbConfigurationService, times(2)).getTableName(any(Node.class), any(Class.class));
        verify(ovsdbConfigurationService, times(2)).getTypedRow(any(Node.class), any(Class.class), any(Row.class));
        verifyNoMoreInteractions(networkingProviderManager);
        verifyNoMoreInteractions(configurationService);
    }

    @Test
    public void testIsNodeTunnelReady() throws Exception {
        String bridgeMockName = "BridgeMockName";

        verifyNoMoreInteractions(ovsdbConfigurationService);
        verifyNoMoreInteractions(configurationService);
        verifyNoMoreInteractions(networkingProviderManager);

        when(configurationService.getIntegrationBridgeName()).thenReturn(bridgeMockName);
        // getBridge() is private method - cannot be mocked with mockito
        // when(bridgeConfigurationManagerImpl.getBridge(any(Node.class), anyString())).thenReturn(bridgeMock);

        // Negative testing only due to private method call
        assertEquals("Error, did not return correct boolean from isNodeTunnelReady", false,
                bridgeConfigurationManagerImpl.isNodeTunnelReady(nodeMock));

        verify(configurationService, times(1)).getIntegrationBridgeName();
        verify(networkingProviderManager, times(0)).getProvider(nodeMock);
        verify(configurationService, times(0)).getNetworkBridgeName();
        verify(ovsdbConfigurationService, times(1)).getRows(any(Node.class), anyString());
        verify(ovsdbConfigurationService, times(1)).getTableName(any(Node.class), any(Class.class));
        verify(ovsdbConfigurationService, times(0)).getTypedRow(any(Node.class), any(Class.class), any(Row.class));
    }

    @Test
    public void testIsNodeVlanReady() throws Exception {
        NeutronNetwork neutronNetworkMock = mock(NeutronNetwork.class);

        verifyNoMoreInteractions(ovsdbConfigurationService);
        verifyNoMoreInteractions(configurationService);
        verifyNoMoreInteractions(networkingProviderManager);

        // getBridge() is private method - cannot be mocked with mockito
        // Negative testing only due to private method call
        assertEquals("Error, did not return correct boolean from isNodeVlanReady", false,
                bridgeConfigurationManagerImpl.isNodeVlanReady(nodeMock, neutronNetworkMock));

        verify(configurationService, times(1)).getIntegrationBridgeName();
        verify(networkingProviderManager, times(0)).getProvider(any(Node.class));
        verify(configurationService, times(0)).getNetworkBridgeName();
        verify(ovsdbConfigurationService, times(1)).getRows(any(Node.class), anyString());
        verify(ovsdbConfigurationService, times(1)).getTableName(any(Node.class), any(Class.class));
        verify(ovsdbConfigurationService, times(0)).getTypedRow(any(Node.class), any(Class.class), any(Row.class));
        verify(neutronNetworkMock, times(0)).getProviderPhysicalNetwork();
    }

    @Test
    public void testPrepareNode() throws Exception {
        NetworkingProvider netProvider = mock(NetworkingProvider.class);
        when(configurationService.getIntegrationBridgeName()).thenReturn("intBridgeName");
        when(networkingProviderManager.getProvider(any(Node.class))).thenReturn(netProvider);

        // createIntegrationBridge() is private method - cannot be mocked with mockito
        // Negative testing only due to private method call
        bridgeConfigurationManagerImpl.prepareNode(nodeMock);

        verify(configurationService, times(1)).getIntegrationBridgeName();
        verify(networkingProviderManager, times(0)).getProvider(any(Node.class));
        verify(netProvider, times(0)).initializeFlowRules(any(Node.class));
    }

    @Test
    public void testCreateLocalNetwork() throws Exception {
        NeutronNetwork neutronNetworkMock = mock(NeutronNetwork.class, RETURNS_MOCKS);
        String networkTypes[] = {"vlan", "vxlan", "gre"};

        for (String networkType : networkTypes) {
            when(neutronNetworkMock.getProviderNetworkType()).thenReturn(networkType);

            doAnswer(new Answer<Boolean>() {
                @Override
                public Boolean answer(InvocationOnMock invocation) {
                    return Boolean.TRUE;
                }
            }).when(bridgeConfigurationManagerImplSpy).isNodeVlanReady(any(Node.class), any(NeutronNetwork.class));

            doAnswer(new Answer<Boolean>() {
                @Override
                public Boolean answer(InvocationOnMock invocation) {
                    return Boolean.TRUE;
                }
            }).when(bridgeConfigurationManagerImplSpy).isNodeTunnelReady(any(Node.class));

            assertTrue("bridgeConfigMock.isNodeVlanReady is not true",
                    bridgeConfigurationManagerImplSpy.isNodeVlanReady(nodeMock, neutronNetworkMock));
            assertTrue("bridgeConfigMock.isNodeTunnelReady is not true",
                    bridgeConfigurationManagerImplSpy.isNodeTunnelReady(nodeMock));

            assertTrue("Error, isCreated is not true for " + networkType,
                    bridgeConfigurationManagerImplSpy.createLocalNetwork(nodeMock, neutronNetworkMock));
            if (networkType == "vlan") {
                verify(neutronNetworkMock, times(1)).getProviderNetworkType();
            } else if (networkType == "vxlan") {
                verify(neutronNetworkMock, times(2)).getProviderNetworkType();
            } else if (networkType == "gre") {
                verify(neutronNetworkMock, times(3)).getProviderNetworkType();
            }
            reset(neutronNetworkMock);
            reset(nodeMock);
            reset(bridgeConfigurationManagerImplSpy);
        }
    }

    @Test
    public void testGetPhysicalInterfaceName() throws Exception {
        ConcurrentHashMap ovsTable = mock(ConcurrentHashMap.class, RETURNS_DEEP_STUBS);
        Row row = mock(Row.class);
        OpenVSwitch ovsRowOVS = mock(OpenVSwitch.class);
        Column<GenericTableSchema, Map<String, String>> col = mock(Column.class);
        Map<String, String> colMap = mock(Map.class);

        HashMap<String, OpenVSwitch> hashMapOVS = new HashMap<>();
        hashMapOVS.put("ovsRow", ovsRowOVS);
        ConcurrentHashMap<String, Row> hashMap;
        hashMap = new ConcurrentHashMap<>();
        hashMap.put("row1", row);

        String networkNames[] = {"network-0", "network-1", "network-2", "network-3"};
        String interfaceNames[] = {"interfaceName-0", "interfaceName-1", "interfaceName-2", "interfaceName-3"};
        int count = 0;

        for (String networkName : networkNames) {
            when(ovsdbConfigurationService.getRows(any(Node.class), anyString())).thenReturn(hashMap);
            when(ovsTable.values()).thenReturn(hashMapOVS.values());

            when(ovsRowOVS.getOtherConfigColumn()).thenReturn(col);
            when(col.getData()).thenReturn(colMap);
            when(configurationService.getProviderMappingsKey()).thenReturn("network-0:interfaceName-0," +
                    "network-1:interfaceName-1,network-2:interfaceName-2,network-3:interfaceName-3");
            when(colMap.get(anyString())).thenReturn("network-0:interfaceName-0,network-1:interfaceName-1," +
                    "network-2:interfaceName-2,network-3:interfaceName-3");

            when(configurationService.getDefaultProviderMapping()).thenReturn("network-0:interfaceName-0," +
                    "network-1:interfaceName-1,network-2:interfaceName-2,network-3:interfaceName-3");

            when(ovsdbConfigurationService.getTypedRow(any(Node.class), same(OpenVSwitch.class),
                    any(Row.class))).thenReturn(ovsRowOVS);

            assertEquals("Error, network: " + networkName + ", did not match interface: " + interfaceNames[count],
                    interfaceNames[count], bridgeConfigurationManagerImpl.getPhysicalInterfaceName(nodeMock,
                            networkName));

            verify(ovsdbConfigurationService, times(count + 1)).getRows(any(Node.class), anyString());
            verify(ovsdbConfigurationService, times(count + 1)).getTableName(any(Node.class), any(Class.class));
            verify(ovsdbConfigurationService, times(count + 1)).getTypedRow(any(Node.class), any(Class.class),
                    any(Row.class));
            verify(configurationService, times(count + 1)).getProviderMappingsKey();
            verify(configurationService, times(0)).getDefaultProviderMapping();
            count++;
        }
    }

    @Test
    public void testGetAllPhysicalInterfaceNames() throws Exception {
        String interfaceNames[] = {"interfaceName-0", "interfaceName-1", "interfaceName-2", "interfaceName-3"};
        List<String> intNameList = new ArrayList<>();
        for (String name: interfaceNames){
            intNameList.add(name);
        }
        Row row = mock(Row.class);
        OpenVSwitch ovsRowOVS = mock(OpenVSwitch.class);
        Column<GenericTableSchema, Map<String, String>> col = mock(Column.class);
        Map<String, String> colMap = mock(Map.class);
        ConcurrentHashMap<String, Row> hashMap;
        hashMap = new ConcurrentHashMap<>();
        hashMap.put("row1", row);

        when(ovsdbConfigurationService.getRows(any(Node.class), anyString())).thenReturn(hashMap);
        when(ovsdbConfigurationService.getTypedRow(any(Node.class), same(OpenVSwitch.class),
                any(Row.class))).thenReturn(ovsRowOVS);
        when(ovsRowOVS.getOtherConfigColumn()).thenReturn(col);
        when(col.getData()).thenReturn(colMap);
        when(colMap.get(anyString())).thenReturn("network-0:interfaceName-0,network-1:interfaceName-1," +
                "network-2:interfaceName-2,network-3:interfaceName-3");

        assertEquals("Error, did not get all interface names", intNameList,
                bridgeConfigurationManagerImpl.getAllPhysicalInterfaceNames(nodeMock));
        verify(ovsdbConfigurationService, times(1)).getRows(any(Node.class), anyString());
        verify(ovsdbConfigurationService, times(1)).getTableName(any(Node.class), any(Class.class));
        verify(ovsdbConfigurationService, times(1)).getTypedRow(any(Node.class), any(Class.class), any(Row.class));
        verify(configurationService, times(1)).getProviderMappingsKey();
        verify(configurationService, times(0)).getDefaultProviderMapping();
    }

    @Test
    public void testGetBridge() throws Exception {
        Row row = mock(Row.class);
        Bridge bridge = mock(Bridge.class);
        ConcurrentHashMap<String, Row> hashMap;
        hashMap = new ConcurrentHashMap<>();
        hashMap.put("row1", row);

        when(ovsdbConfigurationService.getRows(any(Node.class), anyString())).thenReturn(hashMap);
        when(ovsdbConfigurationService.getTypedRow(any(Node.class), same(Bridge.class),
                any(Row.class))).thenReturn(bridge);
        when(bridge.getName()).thenReturn("test-bridge");

        assertEquals("Error, did not get correct bridge", bridge,
                bridgeConfigurationManagerImpl.getBridge(nodeMock, "test-bridge"));
        verify(ovsdbConfigurationService, times(1)).getRows(any(Node.class), anyString());
        verify(ovsdbConfigurationService, times(1)).getTableName(any(Node.class), any(Class.class));
        verify(ovsdbConfigurationService, times(1)).getTypedRow(any(Node.class), any(Class.class), any(Row.class));
    }
}