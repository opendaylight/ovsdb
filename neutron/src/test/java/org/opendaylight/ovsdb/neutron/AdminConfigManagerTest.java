/*******************************************************************************
 * Copyright (c) 2013 Hewlett-Packard Development Company, L.P. and others
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Dave Tucker (HP) - Added unit tests for the AdminConfigManager class.
 *    Sam Hague - Added unit tests for getPhysicalInterfaceName.
 *******************************************************************************/

package org.opendaylight.ovsdb.neutron;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.InetAddress;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.utils.ServiceHelper;
import org.opendaylight.ovsdb.lib.notation.OvsDBMap;
import org.opendaylight.ovsdb.lib.table.Open_vSwitch;
import org.opendaylight.ovsdb.lib.table.Table;
import org.opendaylight.ovsdb.plugin.ConfigurationService;
import org.opendaylight.ovsdb.plugin.OVSDBConfigService;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(ServiceHelper.class)
public class AdminConfigManagerTest {

    AdminConfigManager adminConfigManager;
    private OVSDBConfigService ovsdbConfig;
    private Node node;
    private Open_vSwitch ovsTable;
    private ConcurrentMap<String, Table<?>> ovsMap;
    private OvsDBMap map;

    private static String OPENVSWITCH = "Open_vSwitch";
    private static String PROVIDER_MAPPINGS = "provider_mappings";
    private static String PHYSNET1 = "physnet1";
    private static String ETH1 = "eth1";
    private static String PHYSNET2 = "physnet2";
    private static String ETH2 = "eth2";
    private static String PHYSNET3 = "physnet3";
    private static String ETH3 = "eth3";
    private static String LOCAL_IP = "local_ip";
    private static String IPADDR = "10.10.10.10";

    @Before
    public void setUp(){
        adminConfigManager = new AdminConfigManager();

        node = mock(Node.class);
        ovsdbConfig = mock(ConfigurationService.class);
        PowerMockito.mockStatic(ServiceHelper.class);
        when(ServiceHelper.getGlobalInstance(eq(OVSDBConfigService.class), anyObject())).thenReturn(ovsdbConfig);

        ovsTable = new Open_vSwitch();
        ovsMap = new ConcurrentHashMap<>();
        map = new OvsDBMap();
    }

    @Test
    public void testGetTunnelEndpoint() throws Exception {
        InetAddress testAddress = InetAddress.getByName("10.10.10.10");

        Node mockNode = mock(Node.class);

        ConcurrentMap<String, Table<?>> ovsMap = new ConcurrentHashMap<>();

        Open_vSwitch ovsTable = new Open_vSwitch();
        OvsDBMap localIp = new OvsDBMap();
        localIp.put("local_ip", "10.10.10.10");
        ovsTable.setOther_config(localIp);
        ovsMap.put("Open_vSwitch", ovsTable);

        OVSDBConfigService ovsdbConfig = mock(ConfigurationService.class);
        when(ovsdbConfig.getRows(any(Node.class), anyString())).thenReturn(null)
                                                               .thenReturn(ovsMap);

        PowerMockito.mockStatic(ServiceHelper.class);
        when(ServiceHelper.getGlobalInstance(eq(OVSDBConfigService.class), anyObject())).thenReturn(ovsdbConfig);

        // OVSDBConfigService is null
        assertEquals(null, adminConfigManager.getTunnelEndPoint(mockNode));

        // Success...
        assertEquals(testAddress, adminConfigManager.getTunnelEndPoint(mockNode));
    }

    @Test
    public void testGetTunnelEndpointWithNullRows() throws Exception {
        InetAddress testAddress = InetAddress.getByName("10.10.10.10");

        Node mockNode = mock(Node.class);

        ConcurrentMap<String, Table<?>> ovsMap = new ConcurrentHashMap<>();

        Open_vSwitch nullRow = new Open_vSwitch();
        Open_vSwitch ovsRow1 = new Open_vSwitch();
        Open_vSwitch ovsRow2 = new Open_vSwitch();
        OvsDBMap invalidLocalIp = new OvsDBMap();
        OvsDBMap localIp = new OvsDBMap();

        ovsRow1.setOther_config(invalidLocalIp);

        localIp.put("local_ip","10.10.10.10");
        ovsRow2.setOther_config(localIp);

        ovsMap.put("0", nullRow);
        ovsMap.put("1", ovsRow1);
        ovsMap.put("2", ovsRow2);

        OVSDBConfigService ovsdbConfig = mock(ConfigurationService.class);
        when(ovsdbConfig.getRows(any(Node.class), anyString())).thenReturn(ovsMap);

        PowerMockito.mockStatic(ServiceHelper.class);
        when(ServiceHelper.getGlobalInstance(eq(OVSDBConfigService.class), anyObject())).thenReturn(ovsdbConfig);

        // Success...
        assertEquals(testAddress, adminConfigManager.getTunnelEndPoint(mockNode));
    }

    // Add key:value pairs to the map.
    // Calling again with the same key will overwrite the current pair.
    private void initMap (String key, String value) {
        map.put(key, value);
        ovsTable.setOther_config(map);
        ovsMap.put(OPENVSWITCH, ovsTable);
    }

    @Test
    public void testGetPhysicalInterfaceName () throws Exception {
        when(ovsdbConfig.getRows(any(Node.class), anyString())).thenReturn(ovsMap);

        // Check if match can be found with a single pair
        initMap(PROVIDER_MAPPINGS, PHYSNET1 + ":" + ETH1);
        assertEquals("Failed to find " + ETH1 + " in " + map.toString(),
                ETH1, adminConfigManager.getPhysicalInterfaceName(node, PHYSNET1));

        // Check if match can be found with different pairs
        initMap(PROVIDER_MAPPINGS, PHYSNET1 + ":" + ETH1 + "," + PHYSNET2 + ":" + ETH2);
        assertEquals("Failed to find " + ETH2 + " in " + map.toString(),
                ETH2, adminConfigManager.getPhysicalInterfaceName(node, PHYSNET2));

        // Check if match can be found with duplicate pairs
        initMap(PROVIDER_MAPPINGS, PHYSNET1 + ":" + ETH1 + "," + PHYSNET2 + ":" + ETH2 + "," + PHYSNET2 + ":" + ETH2);
        assertEquals("Failed to find " + ETH2 + " in " + map.toString(),
                ETH2, adminConfigManager.getPhysicalInterfaceName(node, PHYSNET2));

        // Check if match can be found with multiple pairs and extra other_config
        initMap(LOCAL_IP, IPADDR);
        assertEquals("Failed to find " + ETH2 + " in " + map.toString(),
                ETH2, adminConfigManager.getPhysicalInterfaceName(node, PHYSNET2));
    }

    @Test
    public void testGetPhysicalInterfaceNameNegative () throws Exception {
        when(ovsdbConfig.getRows(any(Node.class), anyString())).thenReturn(null)
                .thenReturn(ovsMap);

        // Add a null row, an empty row and a good row to the table
        Open_vSwitch nullRow = new Open_vSwitch();
        Open_vSwitch emptyRow = new Open_vSwitch();
        OvsDBMap emptyProviderMap = new OvsDBMap();
        emptyRow.setOther_config(emptyProviderMap);
        ovsMap.put("0", nullRow);
        ovsMap.put("1", emptyRow);
        initMap(PROVIDER_MAPPINGS, PHYSNET1 + ":" + ETH1);

        // Check if no rows/no table is handled
        assertEquals("Failed to return null when ovsdb table is null",
                null, adminConfigManager.getTunnelEndPoint(node));

        // Check if the null and empty rows are ignored
        System.out.println("map = " + map.toString());
        System.out.println("ovsMap = " + ovsMap.toString());
        assertEquals("Failed to find " + ETH1 + " in " + map.toString(),
                ETH1, adminConfigManager.getPhysicalInterfaceName(node, PHYSNET1));

        // Should not be able to find match
        initMap(PROVIDER_MAPPINGS, PHYSNET1 + ":" + ETH1 + "," + PHYSNET2 + ":" + ETH2);
        assertNull("Found " + ETH3 + " in " + map.toString(),
                adminConfigManager.getPhysicalInterfaceName(node, PHYSNET3));

        // Should not be able to find match with mal-formed values
        initMap(PROVIDER_MAPPINGS, PHYSNET1 + "-" + ETH1);
        assertNull("Found " + ETH1 + " in " + map.toString(),
                adminConfigManager.getPhysicalInterfaceName(node, PHYSNET1));
    }
}
