/*******************************************************************************
 * Copyright (c) 2013 Hewlett-Packard Development Company, L.P.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Dave Tucker (HP) - Added unit tests for the AdminConfigManager class.
 *******************************************************************************/

package org.opendaylight.ovsdb.neutron;

import static org.junit.Assert.assertEquals;
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
import org.opendaylight.ovsdb.lib.table.internal.Table;
import org.opendaylight.ovsdb.plugin.ConfigurationService;
import org.opendaylight.ovsdb.plugin.OVSDBConfigService;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(ServiceHelper.class)
public class AdminConfigManagerTest {

    AdminConfigManager adminConfigManager;

    @Before
    public void setUp(){
        adminConfigManager = new AdminConfigManager();
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
}
