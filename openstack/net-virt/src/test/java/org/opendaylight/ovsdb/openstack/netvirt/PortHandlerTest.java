/*
 * Copyright (c) 2015 Inocybe and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.opendaylight.neutron.spi.NeutronPort;
import org.opendaylight.ovsdb.openstack.netvirt.api.Action;
import org.opendaylight.ovsdb.openstack.netvirt.api.EventDispatcher;
import org.opendaylight.ovsdb.openstack.netvirt.api.NodeCacheManager;
import org.opendaylight.ovsdb.openstack.netvirt.api.Southbound;
import org.opendaylight.ovsdb.openstack.netvirt.impl.NeutronL3Adapter;
import org.opendaylight.ovsdb.utils.servicehelper.ServiceHelper;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * Unit test fort {@link PortHandler}
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(ServiceHelper.class)
public class PortHandlerTest {

    @InjectMocks private PortHandler portHandler;

    @Mock private NeutronL3Adapter neutronL3Adapter;
    @Mock private NodeCacheManager nodeCacheManager;
    @Mock private Southbound southbound;

    @Test
    public void testCanCreatePort() {
        assertEquals("Error, did not return the correct HTTP flag", HttpURLConnection.HTTP_OK, portHandler.canCreatePort(mock(NeutronPort.class)));
    }

    @Test
    public void testCanUpdatePort() {
        assertEquals("Error, did not return the correct HTTP flag", HttpURLConnection.HTTP_OK, portHandler.canUpdatePort(mock(NeutronPort.class), mock(NeutronPort.class)));
    }

    @Test
    public void testCanDeletePort() {
        assertEquals("Error, did not return the correct HTTP flag", HttpURLConnection.HTTP_OK, portHandler.canDeletePort(mock(NeutronPort.class)));
    }

    @Test
    public void testProcessEvent() {
        PortHandler portHandlerSpy = Mockito.spy(portHandler);

        NeutronPort neutronPort = mock(NeutronPort.class);
        when(neutronPort.getTenantID()).thenReturn("tenantID");
        when(neutronPort.getNetworkUUID()).thenReturn("networkUUID");
        when(neutronPort.getID()).thenReturn("ID");
        when(neutronPort.getPortUUID()).thenReturn("portUUID");

        NorthboundEvent ev = mock(NorthboundEvent.class);
        when(ev.getPort()).thenReturn(neutronPort);

        when(ev.getAction()).thenReturn(Action.ADD);
        portHandlerSpy.processEvent(ev);
        verify(neutronL3Adapter, times(1)).handleNeutronPortEvent(neutronPort, Action.ADD);

        when(ev.getAction()).thenReturn(Action.UPDATE);
        portHandlerSpy.processEvent(ev);
        verify(neutronL3Adapter, times(1)).handleNeutronPortEvent(neutronPort, Action.UPDATE);

        List<Node> nodes = new ArrayList<Node>();
        nodes.add(mock(Node.class));
        when(nodeCacheManager.getNodes()).thenReturn(nodes);

        List<OvsdbTerminationPointAugmentation> ports = new ArrayList<OvsdbTerminationPointAugmentation>();
        OvsdbTerminationPointAugmentation port = mock(OvsdbTerminationPointAugmentation.class);
        ports.add(port);
        when(southbound.getTerminationPointsOfBridge(any(Node.class))).thenReturn(ports);

        when(southbound.getInterfaceExternalIdsValue(any(OvsdbTerminationPointAugmentation.class), anyString())).thenReturn("portUUID");

        when(ev.getAction()).thenReturn(Action.DELETE);
        portHandlerSpy.processEvent(ev);
        verify(neutronL3Adapter, times(1)).handleNeutronPortEvent(neutronPort, Action.DELETE);
        verify(southbound, times(1)).deleteTerminationPoint(any(Node.class), anyString());
    }

    @Test
    public void testSetDependencies() throws Exception {
        NodeCacheManager nodeCacheManager = mock(NodeCacheManager.class);
        NeutronL3Adapter neutronL3Adapter = mock(NeutronL3Adapter.class);
        Southbound southbound = mock(Southbound.class);
        EventDispatcher eventDispatcher = mock(EventDispatcher.class);

        PowerMockito.mockStatic(ServiceHelper.class);
        PowerMockito.when(ServiceHelper.getGlobalInstance(NodeCacheManager.class, portHandler)).thenReturn(nodeCacheManager);
        PowerMockito.when(ServiceHelper.getGlobalInstance(NeutronL3Adapter.class, portHandler)).thenReturn(neutronL3Adapter);
        PowerMockito.when(ServiceHelper.getGlobalInstance(Southbound.class, portHandler)).thenReturn(southbound);
        PowerMockito.when(ServiceHelper.getGlobalInstance(EventDispatcher.class, portHandler)).thenReturn(eventDispatcher);

        portHandler.setDependencies(mock(BundleContext.class), mock(ServiceReference.class));

        assertEquals("Error, did not return the correct object", getField("nodeCacheManager"), nodeCacheManager);
        assertEquals("Error, did not return the correct object", getField("neutronL3Adapter"), neutronL3Adapter);
        assertEquals("Error, did not return the correct object", getField("southbound"), southbound);
        assertEquals("Error, did not return the correct object", portHandler.eventDispatcher, eventDispatcher);
    }

    private Object getField(String fieldName) throws Exception {
        Field field = PortHandler.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(portHandler);
    }
}
