/*
 * Copyright (c) 2015 Inocybe and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.openstack.netvirt.impl;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.opendaylight.ovsdb.openstack.netvirt.api.Constants;
import org.opendaylight.ovsdb.utils.config.ConfigProperties;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * Unit test for {@link ConfigurationServiceImpl}
 */
@Ignore // TODO SB_MIGRATION
@PrepareForTest(ConfigProperties.class)
@RunWith(PowerMockRunner.class)
public class ConfigurationServiceImplTest {

    @InjectMocks
    private ConfigurationServiceImpl configurationServiceImpl;

    private static final String HOST_ADDRESS = "127.0.0.1";

    /**
     * Test method {@link ConfigurationServiceImpl#getTunnelEndPoint(Node)}
     */
    @Test
    public void testGetTunnelEndPoint() throws Exception {
        //Row row = mock(Row.class);
        //ConcurrentMap<String, Row> ovsTable = new ConcurrentHashMap();
        //ovsTable.put("key", row);

        //OpenVSwitch ovsRow = mock(OpenVSwitch.class);
        Map<String, String> configs = new HashMap();
        configs.put(Constants.TUNNEL_ENDPOINT_KEY, HOST_ADDRESS);
        //Column<GenericTableSchema, Map<String, String>> otherConfigColumn = mock(Column.class);

        //when(ovsRow.getOtherConfigColumn()).thenReturn(otherConfigColumn);
        //when(otherConfigColumn.getData()).thenReturn(configs);

        /* TODO SB_MIGRATION */
        //when(ovsdbConfigurationService.getRows(any(Node.class), anyString())).thenReturn(ovsTable);
        //when(ovsdbConfigurationService.getTypedRow(any(Node.class),same(OpenVSwitch.class), any(Row.class))).thenReturn(ovsRow);

        //assertEquals("Error, did not return address of tunnelEndPoint", HOST_ADDRESS, configurationServiceImpl.getTunnelEndPoint(mock(Node.class)).getHostAddress());
    }

    /**
     * Test method {@link ConfigurationServiceImpl#getDefaultGatewayMacAddress(Node)}
     */
    @Test
    public void testGetDefaultGatewayMacAddress(){
        Node node = mock(Node.class);
        NodeId nodeId = mock(NodeId.class);
        PowerMockito.mockStatic(ConfigProperties.class);

        when(node.getNodeId()).thenReturn(nodeId);
        when(nodeId.getValue()).thenReturn("nodeIdValue");
        PowerMockito.when(ConfigProperties.getProperty(configurationServiceImpl.getClass(),
                "ovsdb.l3gateway.mac." + node.getNodeId().getValue())).thenReturn("gateway");

        assertEquals("Error, did not return the defaultGatewayMacAddress of the node", "gateway",
                configurationServiceImpl.getDefaultGatewayMacAddress(node));
    }
}
