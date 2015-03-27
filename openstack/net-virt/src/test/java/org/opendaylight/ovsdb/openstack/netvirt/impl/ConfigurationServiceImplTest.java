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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.opendaylight.ovsdb.lib.notation.Column;
import org.opendaylight.ovsdb.lib.notation.Row;
import org.opendaylight.ovsdb.lib.schema.GenericTableSchema;
import org.opendaylight.ovsdb.openstack.netvirt.api.Constants;
import org.opendaylight.ovsdb.plugin.api.OvsdbConfigurationService;
import org.opendaylight.ovsdb.schema.openvswitch.OpenVSwitch;
import org.opendaylight.ovsdb.utils.config.ConfigProperties;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * Unit test for class ConfigurationServiceImpl
 */
@PrepareForTest(ConfigProperties.class)
@RunWith(PowerMockRunner.class)
public class ConfigurationServiceImplTest {

    @Mock
    private OvsdbConfigurationService ovsdbConfigurationService;

    @InjectMocks
    private ConfigurationServiceImpl configurationServiceImpl;

    /**
     * Test method {@link ConfigurationServiceImpl#getTunnelEndPoint(Node)}
     */
    @Test
    public void testGetTunnelEndPoint() throws Exception {
        Row row = mock(Row.class);
        ConcurrentMap<String, Row> ovsTable = new ConcurrentHashMap();
        ovsTable.put("key", row);

        OpenVSwitch ovsRow = mock(OpenVSwitch.class);
        Map<String, String> configs = new HashMap();
        configs.put(Constants.TUNNEL_ENDPOINT_KEY, "127.0.0.1");
        Column<GenericTableSchema, Map<String, String>> otherConfigColumn = mock(Column.class);

        when(ovsRow.getOtherConfigColumn()).thenReturn(otherConfigColumn);
        when(otherConfigColumn.getData()).thenReturn(configs);

        when(ovsdbConfigurationService.getRows(any(Node.class), anyString())).thenReturn(ovsTable);
        when(ovsdbConfigurationService.getTypedRow(any(Node.class),same(OpenVSwitch.class), any(Row.class))).thenReturn(ovsRow);

        assertEquals("Error, did not return adress of tunnelEndPoint", "127.0.0.1", configurationServiceImpl.getTunnelEndPoint(mock(Node.class)).getHostAddress());
    }

    /**
     * Test method {@link ConfigurationServiceImpl#getDefaultGatewayMacAddress(Node)}
     */
    @Test
    public void testGetDefaultGatewayMacAddress(){
        Node node = mock(Node.class);
        NodeId nodeId = mock(NodeId.class);
        PowerMockito.mockStatic(ConfigProperties.class);

        when(node.getId()).thenReturn(nodeId);
        when(nodeId.getValue()).thenReturn("nodeIdValue");
        PowerMockito.when(ConfigProperties.getProperty(configurationServiceImpl.getClass(), "ovsdb.l3gateway.mac." + node.getId().getValue())).thenReturn("gateway");

        assertEquals("Error, did nor return the defaultGatewayMacAddress of the ndoe", "gateway", configurationServiceImpl.getDefaultGatewayMacAddress(node));
    }
}
