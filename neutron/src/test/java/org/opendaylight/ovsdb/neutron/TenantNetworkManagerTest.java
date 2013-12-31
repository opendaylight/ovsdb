/*******************************************************************************
 * Copyright (c) 2013 Hewlett-Packard Development Company, L.P.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Dave Tucker (HP) - Added unit tests for the TenantNetworkManager class.
 *******************************************************************************/

package org.opendaylight.ovsdb.neutron;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.opendaylight.controller.networkconfig.neutron.INeutronNetworkCRUD;
import org.opendaylight.controller.networkconfig.neutron.INeutronPortCRUD;
import org.opendaylight.controller.networkconfig.neutron.NeutronNetwork;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.utils.ServiceHelper;
import org.opendaylight.ovsdb.lib.notation.OvsDBMap;
import org.opendaylight.ovsdb.lib.table.Interface;
import org.opendaylight.ovsdb.lib.table.Port;
import org.opendaylight.ovsdb.lib.table.internal.Table;
import org.opendaylight.ovsdb.neutron.mocks.DummyNeutronNetworkCRUD;
import org.opendaylight.ovsdb.neutron.mocks.DummyNeutronPortCRUD;
import org.opendaylight.ovsdb.plugin.ConfigurationService;
import org.opendaylight.ovsdb.plugin.NodeFactory;
import org.opendaylight.ovsdb.plugin.OVSDBConfigService;
import org.powermock.reflect.Whitebox;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({TenantNetworkManager.class, ServiceHelper.class})
public class TenantNetworkManagerTest {

    private TenantNetworkManager tenantNetworkManager;
    private Node node;
    private LinkedList internalVlans;
    private HashMap<String, Integer> tenantVlanMap;
    private ConfigurationService ovsdbConfigService;
    private DummyNeutronNetworkCRUD neutronNetworkCRUD;
    private DummyNeutronPortCRUD neutronPortCRUD;

    @Before
    public void setUp() throws Exception {
        Constructor<TenantNetworkManager> c = TenantNetworkManager.class.getDeclaredConstructor();
        c.setAccessible(true);
        Field f = TenantNetworkManager.class.getDeclaredField("internalVlans");
        f.setAccessible(true);

        tenantNetworkManager = PowerMockito.spy(c.newInstance());
        Whitebox.setInternalState(TenantNetworkManager.class, "tenantHelper", tenantNetworkManager);

        internalVlans = (LinkedList)f.get(tenantNetworkManager);

        ovsdbConfigService = mock(ConfigurationService.class);

        neutronNetworkCRUD = new DummyNeutronNetworkCRUD();
        neutronPortCRUD = new DummyNeutronPortCRUD();

        PowerMockito.mockStatic(ServiceHelper.class);
        when(ServiceHelper.getGlobalInstance(eq(OVSDBConfigService.class), anyObject())).thenReturn(ovsdbConfigService);
        when(ServiceHelper.getGlobalInstance(eq(INeutronNetworkCRUD.class), anyObject())).thenReturn(neutronNetworkCRUD);
        when(ServiceHelper.getGlobalInstance(eq(INeutronPortCRUD.class), anyObject())).thenReturn(neutronPortCRUD);

        NodeFactory nodeFactory = new NodeFactory();
        node = nodeFactory.fromString("OVS", "1");

    }

    @Test
    public void testInternalVlanInUse() throws Exception {
        Integer inUse = 100;
        tenantNetworkManager.internalVlanInUse(inUse);

        assertFalse(internalVlans.contains(inUse));
    }

    @Test
    public void testGetInternalVlan() throws Exception {
        int vlanID = 100;
        tenantVlanMap = new HashMap<String, Integer>();
        tenantVlanMap.put("test", vlanID);
        Whitebox.setInternalState(tenantNetworkManager, "tenantVlanMap", tenantVlanMap);

        assertEquals(vlanID, tenantNetworkManager.getInternalVlan("test"));
        assertEquals(0, tenantNetworkManager.getInternalVlan("blank"));

    }

    @Test
    public void testNetworkCreated() throws Exception {
        int first = tenantNetworkManager.networkCreated("test1");
        int second = tenantNetworkManager.networkCreated("test2");

        assertEquals(1, first);
        assertEquals(2, second);
    }

    @Test
    public void testIsTenantNetworkPresentInNode() throws Exception {

        assertFalse(tenantNetworkManager.isTenantNetworkPresentInNode(node, "fakeSegmentationId"));

        // No vlan assigned
        assertFalse(tenantNetworkManager.isTenantNetworkPresentInNode(node, "testSegmentationId"));

        tenantVlanMap = new HashMap<String, Integer>();
        tenantVlanMap.put("testNetworkUUID", 100);
        Whitebox.setInternalState(tenantNetworkManager, "tenantVlanMap", tenantVlanMap);

        Map<String, Table<?>> ifTable1 = new HashMap<String, Table<?>>();
        Interface iface1 = new Interface();
        OvsDBMap<String, String> external_ids1 = new OvsDBMap<String, String>();
        external_ids1.put("iface-id", "fakePortId");
        iface1.setExternal_ids(external_ids1);
        ifTable1.put("test-iface", iface1);

        Map<String, Table<?>> ifTable2 = new HashMap<String, Table<?>>();
        Interface iface2 = new Interface();
        OvsDBMap<String, String> external_ids2 = new OvsDBMap<String, String>();
        external_ids2.put("iface-id", "testPortId");
        iface2.setExternal_ids(external_ids2);
        ifTable2.put("test-iface", iface2);

        when(ovsdbConfigService.getRows(eq(node), eq(Interface.NAME.getName())))
                .thenReturn(null)
                .thenReturn(ifTable1)
                .thenReturn(ifTable2);

        // ifTable is null
        assertFalse(tenantNetworkManager.isTenantNetworkPresentInNode(node, "testSegmentationId"));

        // Network ID's don't match
        assertFalse(tenantNetworkManager.isTenantNetworkPresentInNode(node, "testSegmentationId"));

        // This works!
        assertTrue(tenantNetworkManager.isTenantNetworkPresentInNode(node, "testSegmentationId"));

    }

    @Test
    public void testGetNetworkIdForSegmentationId() throws Exception {

        assertEquals("testNetworkUUID", tenantNetworkManager.getNetworkIdForSegmentationId("testSegmentationId"));
        assertEquals(null, tenantNetworkManager.getNetworkIdForSegmentationId("invalid"));
    }

    @Test
    public void testGetTenantNetworkForInterface() throws Exception {

        Interface iface = mock(Interface.class);

        OvsDBMap<String, String> bad_external_ids1 = new OvsDBMap<String, String>();
        bad_external_ids1.put("key", "value");

        OvsDBMap<String, String> bad_external_ids2 = new OvsDBMap<String, String>();
        bad_external_ids2.put("iface-id", "nullPortId");

        OvsDBMap<String, String> external_ids = new OvsDBMap<String, String>();
        external_ids.put("iface-id", "testPortId");

        when(iface.getExternal_ids())
                .thenReturn(null)
                .thenReturn(bad_external_ids1)
                .thenReturn(bad_external_ids2)
                .thenReturn(external_ids);

        assertEquals(null, tenantNetworkManager.getTenantNetworkForInterface(null));
        // external_ids == null
        assertEquals(null, tenantNetworkManager.getTenantNetworkForInterface(iface));
        // no "iface-id" in external_ids
        assertEquals(null, tenantNetworkManager.getTenantNetworkForInterface(iface));
        // neutronPort null
        assertEquals(null, tenantNetworkManager.getTenantNetworkForInterface(iface));

        NeutronNetwork result = tenantNetworkManager.getTenantNetworkForInterface(iface);
        assertEquals("testNetworkUUID", result.getNetworkUUID());

    }

    @Test
    public void testProgramTenantNetworkInternalVlan() throws Exception {

        NeutronNetwork network = new NeutronNetwork();
        network.setNetworkUUID("testNetworkUUID");

        tenantVlanMap = new HashMap<String, Integer>();
        tenantVlanMap.put("testNetworkUUID", 100);
        Whitebox.setInternalState(tenantNetworkManager, "tenantVlanMap", tenantVlanMap);

        tenantNetworkManager.programTenantNetworkInternalVlan(node, "testPortId", network);

        ArgumentCaptor<Port> port = ArgumentCaptor.forClass(Port.class);
        verify(ovsdbConfigService).updateRow(eq(node), eq(Port.NAME.getName()), anyString(), eq("testPortId"), port.capture());
        assertTrue(port.getValue().getTag().contains(BigInteger.valueOf(100)));

    }

}
