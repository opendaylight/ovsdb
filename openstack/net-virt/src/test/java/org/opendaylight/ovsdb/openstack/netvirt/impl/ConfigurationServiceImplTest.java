/*
 * Copyright (c) 2015, 2016 Inocybe and others.  All rights reserved.
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.ovsdb.openstack.netvirt.api.Constants;
import org.opendaylight.ovsdb.openstack.netvirt.api.OvsdbTables;
import org.opendaylight.ovsdb.openstack.netvirt.api.Southbound;
import org.opendaylight.ovsdb.utils.config.ConfigProperties;
import org.opendaylight.ovsdb.utils.servicehelper.ServiceHelper;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology
        .Node;
import org.osgi.framework.ServiceReference;

/**
 * Unit test for {@link ConfigurationServiceImpl}
 */
@RunWith(MockitoJUnitRunner.class)
public class ConfigurationServiceImplTest {

    @InjectMocks private ConfigurationServiceImpl configurationServiceImpl;

    @Mock private Southbound southbound;

    private static final String ADDRESS = "127.0.0.1";
    private static final String IS_FOWARDING_ENABLE = "yes";

    /**
     * Test method {@link ConfigurationServiceImpl#getTunnelEndPoint(Node)}
     */
    @Test
    public void testGetTunnelEndPoint() throws Exception {
        when(southbound.getOtherConfig(any(Node.class), any(OvsdbTables.class), anyString())).thenReturn(ADDRESS);

        assertEquals("Error, did not return the expected address",  ADDRESS, configurationServiceImpl.getTunnelEndPoint(mock(Node.class)).getHostAddress());
    }

    @Test
    public void testGetOpenFlowVersion() {
        assertEquals("Error, did not return the correct OF version", Constants.OPENFLOW13, configurationServiceImpl.getOpenflowVersion(mock(Node.class)));
    }

    @Test
    public void testIsL3FowardingEnable() {
        ConfigProperties.overrideProperty("ovsdb.l3.fwd.enabled", IS_FOWARDING_ENABLE);

        assertTrue("Error, l3 fowarding should be activated", configurationServiceImpl.isL3ForwardingEnabled());
    }

    /**
     * Test method {@link ConfigurationServiceImpl#getDefaultGatewayMacAddress(Node)}
     */
    @Test
    public void testGetDefaultGatewayMacAddress(){
        Node node = mock(Node.class);
        NodeId nodeId = mock(NodeId.class);

        when(node.getNodeId()).thenReturn(nodeId);
        when(nodeId.getValue()).thenReturn("nodeIdValue");
        ConfigProperties.overrideProperty("ovsdb.l3gateway.mac." + node.getNodeId().getValue(), "gateway");

        assertEquals("Error, did not return the defaultGatewayMacAddress of the node", "gateway",
                configurationServiceImpl.getDefaultGatewayMacAddress(node));
    }

    @Test
    public void testSetDependencies() throws Exception {
        Southbound southbound = mock(Southbound.class);

        ServiceHelper.overrideGlobalInstance(Southbound.class, southbound);

        configurationServiceImpl.setDependencies(mock(ServiceReference.class));

        assertEquals("Error, did not return the correct object", getField("southbound"), southbound);
    }

    private Object getField(String fieldName) throws Exception {
        Field field = ConfigurationServiceImpl.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(configurationServiceImpl);
    }
}
