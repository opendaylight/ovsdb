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

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.mockito.Matchers.any;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.ovsdb.lib.notation.OvsDBMap;
import org.opendaylight.ovsdb.lib.table.Open_vSwitch;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.core.classloader.annotations.PrepareForTest;

import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.ovsdb.plugin.ConfigurationService;
import org.opendaylight.ovsdb.plugin.OVSDBConfigService;
import org.opendaylight.controller.sal.utils.ServiceHelper;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

import org.opendaylight.ovsdb.lib.table.internal.Table;

@RunWith(PowerMockRunner.class)
@PrepareForTest(ServiceHelper.class)
public class AdminConfigManagerTest {
    @Test
    public void testPopulateTunnelEndpoint() throws Exception {
        InetAddress testAddress = InetAddress.getByName("10.10.10.10");

        Node mockNode = mock(Node.class);

        Map<String, Table<?>> ovsMap = new HashMap<String, Table<?>>();

        Open_vSwitch ovsTable = new Open_vSwitch();
        OvsDBMap localIp = new OvsDBMap();
        localIp.put("local_ip", "10.10.10.10");
        ovsTable.setOther_config(localIp);
        ovsMap.put("Open_vSwitch", ovsTable);

        OVSDBConfigService ovsdbConfig = mock(ConfigurationService.class);
        when(ovsdbConfig.getRows(any(Node.class), anyString())).thenReturn(ovsMap);

        PowerMockito.mockStatic(ServiceHelper.class);
        when(ServiceHelper.getGlobalInstance(eq(OVSDBConfigService.class), anyObject())).thenReturn(ovsdbConfig);

        AdminConfigManager.getManager().populateTunnelEndpoint(mockNode);

        assertEquals(testAddress, AdminConfigManager.getManager().getTunnelEndPoint(mockNode));
    }
}
