/*******************************************************************************
 * Copyright (c) 2013 Hewlett-Packard Development Company, L.P.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Dave Tucker (HP) - Added unit tests for the InternalNetworkManager class.
 *******************************************************************************/
package org.opendaylight.ovsdb.neutron;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.utils.ServiceHelper;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.controller.sal.utils.StatusCode;
import org.opendaylight.ovsdb.lib.notation.OvsDBSet;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.table.Bridge;
import org.opendaylight.ovsdb.lib.table.Interface;
import org.opendaylight.ovsdb.lib.table.Port;
import org.opendaylight.ovsdb.lib.table.internal.Table;
import org.opendaylight.ovsdb.neutron.provider.*;
import org.opendaylight.ovsdb.plugin.*;
import org.powermock.reflect.Whitebox;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ServiceHelper.class, InternalNetworkManager.class, ProviderNetworkManager.class, DummyProvider.class})
public class InternalNetworkManagerTest {

    private ConnectionService connectionService;
    private ConfigurationService ovsdbConfigService;
    private Node node;
    private DummyProvider provider;
    private InternalNetworkManager internalNetworkManager;

    @Before
    public void setUp() throws Exception {

        NodeFactory nodeFactory = new NodeFactory();
        node = nodeFactory.fromString("OVS", "1");

        connectionService = mock(ConnectionService.class);
        when(connectionService.setOFController(eq(node), anyString())).thenReturn(true);

        ovsdbConfigService = mock(ConfigurationService.class);

        PowerMockito.mockStatic(ServiceHelper.class);
        when(ServiceHelper.getGlobalInstance(eq(IConnectionServiceInternal.class), anyObject())).thenReturn(connectionService);
        when(ServiceHelper.getGlobalInstance(eq(OVSDBConfigService.class), anyObject())).thenReturn(ovsdbConfigService);


        Constructor<InternalNetworkManager> i = InternalNetworkManager.class.getDeclaredConstructor();
        i.setAccessible(true);
        internalNetworkManager = PowerMockito.spy(i.newInstance());
        Whitebox.setInternalState(InternalNetworkManager.class, "internalNetwork", internalNetworkManager);

        provider = PowerMockito.spy(new DummyProvider());
        Whitebox.setInternalState(ProviderNetworkManager.class, "provider", provider);

    }

    private void generateTestData() throws Exception {

        Map<String, Table<?>> ovsMap = new HashMap<String, Table<?>>();

        Bridge testBridgeOne = new Bridge();
        testBridgeOne.setName("br-int");
        OvsDBSet<String> dpidOne = new OvsDBSet<String>();
        dpidOne.add("00:00:00:00:00:00:00:01");
        testBridgeOne.setDatapath_id(dpidOne);
        ovsMap.put("testUuid1", testBridgeOne);

        Bridge testBridgeTwo = new Bridge();
        testBridgeTwo.setName("br-tun");
        OvsDBSet<String> dpidTwo = new OvsDBSet<String>();
        dpidTwo.add("00:00:00:00:00:00:00:02");
        testBridgeTwo.setDatapath_id(dpidTwo);
        ovsMap.put("testUuid2", testBridgeTwo);

        when(ovsdbConfigService.getRows(eq(node), anyString())).thenReturn(ovsMap);

    }

    @Test
    public void testGetInternalBridgeUUID() throws Exception {

        generateTestData();
        String result = InternalNetworkManager.getManager().getInternalBridgeUUID(node, "br-tun");
        assertEquals("testUuid2", result);

    }

    @Test
    public void testIsInternalNetworkNeutronReady() throws Exception {

        generateTestData();
        assertTrue(InternalNetworkManager.getManager().isInternalNetworkNeutronReady(node));

    }

    @Test
    public void testIsInternalNetworkOverlayReady() throws Exception {

        generateTestData();
        assertTrue(InternalNetworkManager.getManager().isInternalNetworkOverlayReady(node));

    }

    @Test
    public void testCreateInternalNetworkForOverlayWithPerTenantTunneling() throws Exception {

        UUID uuid = new UUID("testUUID");
        StatusWithUuid status = new StatusWithUuid(StatusCode.SUCCESS, uuid);
        Status statusOK = new Status(StatusCode.SUCCESS);

        Port port = new Port();
        OvsDBSet<UUID> interfaces = new OvsDBSet<UUID>();
        UUID portUUID = new UUID("eth0");
        interfaces.add(portUUID);
        port.setInterfaces(interfaces);

        when(ovsdbConfigService.insertRow(any(Node.class), eq(Bridge.NAME.getName()), anyString(), any(Bridge.class))).thenReturn(status);
        when(ovsdbConfigService.insertRow(any(Node.class), eq(Port.NAME.getName()), anyString(), any(Port.class))).thenReturn(status);
        when(ovsdbConfigService.getRow(any(Node.class), eq(Port.NAME.getName()), anyString())).thenReturn((Table)port);
        when(ovsdbConfigService.updateRow(any(Node.class), eq(Interface.NAME.getName()), anyString(), anyString(), any(Port.class))).thenReturn(statusOK);

        InternalNetworkManager.getManager().createInternalNetworkForOverlay(node);

        //Create two bridges (br-int and br-tun)
        verify(ovsdbConfigService, times(2)).insertRow(eq(node), eq(Bridge.NAME.getName()), anyString(), any(Bridge.class));
        //Create two ports for each bridge
        verify(ovsdbConfigService, times(4)).insertRow(eq(node), eq(Port.NAME.getName()), anyString(), any(Port.class));
        //Create tun interface for each bridge
        verify(ovsdbConfigService, times(2)).updateRow(any(Node.class), eq(Interface.NAME.getName()), anyString(), anyString(), any(Port.class));

    }

    @Test
    public void testCreateInternalNetworkForOverlayWithoutPerTenantTunneling() throws Exception {

        UUID uuid = new UUID("testUUID");
        StatusWithUuid status = new StatusWithUuid(StatusCode.SUCCESS, uuid);
        Status statusOK = new Status(StatusCode.SUCCESS);

        Port port = new Port();
        OvsDBSet<UUID> interfaces = new OvsDBSet<UUID>();
        UUID portUUID = new UUID("eth0");
        interfaces.add(portUUID);
        port.setInterfaces(interfaces);

        when(ovsdbConfigService.insertRow(any(Node.class), eq(Bridge.NAME.getName()), anyString(), any(Bridge.class))).thenReturn(status);
        when(ovsdbConfigService.insertRow(any(Node.class), eq(Port.NAME.getName()), anyString(), any(Port.class))).thenReturn(status);
        when(ovsdbConfigService.getRow(any(Node.class), eq(Port.NAME.getName()), anyString())).thenReturn((Table)port);
        when(ovsdbConfigService.updateRow(any(Node.class), eq(Interface.NAME.getName()), anyString(), anyString(), any(Port.class))).thenReturn(statusOK);

        //No per tenant tunneling
        when(provider.hasPerTenantTunneling()).thenReturn(false);
        Whitebox.setInternalState(ProviderNetworkManager.class, "provider", provider);

        InternalNetworkManager.getManager().createInternalNetworkForOverlay(node);

        //Create one bridge, br-int
        verify(ovsdbConfigService, times(1)).insertRow(eq(node), eq(Bridge.NAME.getName()), anyString(), any(Bridge.class));
        //Create a single port
        verify(ovsdbConfigService, times(1)).insertRow(eq(node), eq(Port.NAME.getName()), anyString(), any(Port.class));

    }

    @Test
    public void testPrepareInternalNetwork() throws Exception {

        UUID uuid = new UUID("testUUID");
        StatusWithUuid status = new StatusWithUuid(StatusCode.SUCCESS, uuid);
        Status statusOK = new Status(StatusCode.SUCCESS);

        Port port = new Port();
        OvsDBSet<UUID> interfaces = new OvsDBSet<UUID>();
        UUID portUUID = new UUID("eth0");
        interfaces.add(portUUID);
        port.setInterfaces(interfaces);

        when(ovsdbConfigService.insertRow(any(Node.class), eq(Bridge.NAME.getName()), anyString(), any(Bridge.class))).thenReturn(status);
        when(ovsdbConfigService.insertRow(any(Node.class), eq(Port.NAME.getName()), anyString(), any(Port.class))).thenReturn(status);
        when(ovsdbConfigService.getRow(any(Node.class), eq(Port.NAME.getName()), anyString())).thenReturn((Table)port);
        when(ovsdbConfigService.updateRow(any(Node.class), eq(Interface.NAME.getName()), anyString(), anyString(), any(Port.class))).thenReturn(statusOK);

        InternalNetworkManager.getManager().prepareInternalNetwork(node);

        PowerMockito.verifyPrivate(internalNetworkManager).invoke("addInternalBridge", eq(node), eq("br-int"), anyString(), anyString());
        PowerMockito.verifyPrivate(internalNetworkManager).invoke("addInternalBridge", eq(node), eq("br-tun"), anyString(), anyString());
        verify(provider).initializeFlowRules(node);

    }



    @Test
    public void testCreateInternalNetworkForNeutron() throws Exception {

        UUID uuid = new UUID("testUUID");
        StatusWithUuid status = new StatusWithUuid(StatusCode.SUCCESS, uuid);
        when(ovsdbConfigService.insertRow(eq(node), eq(Bridge.NAME.getName()), anyString(), any(Bridge.class))).thenReturn(status);

        InternalNetworkManager.getManager().createInternalNetworkForNeutron(node);

        PowerMockito.verifyPrivate(internalNetworkManager).invoke("addInternalBridge", eq(node), eq("br-int"), anyString(), anyString());
    }
}
