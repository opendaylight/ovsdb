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
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.neutron.spi.INeutronNetworkCRUD;
import org.opendaylight.neutron.spi.NeutronNetwork;
import org.opendaylight.ovsdb.lib.notation.Column;
import org.opendaylight.ovsdb.lib.notation.Row;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.schema.GenericTableSchema;
import org.opendaylight.ovsdb.openstack.netvirt.api.Action;
import org.opendaylight.ovsdb.openstack.netvirt.api.BridgeConfigurationManager;
import org.opendaylight.ovsdb.openstack.netvirt.api.TenantNetworkManager;
import org.opendaylight.ovsdb.openstack.netvirt.impl.NeutronL3Adapter;
import org.opendaylight.ovsdb.schema.openvswitch.Interface;
import org.opendaylight.ovsdb.schema.openvswitch.Port;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;

/**
 * Unit test for {@link NetworkHandler}
 */
@RunWith(MockitoJUnitRunner.class)
public class NetworkHandlerTest {

    @InjectMocks NetworkHandler networkHandler;

    @Mock NeutronNetwork neutronNetwork;

    @Mock private NeutronL3Adapter neutronL3Adapter;
    @Mock private TenantNetworkManager tenantNetworkManager;
    @Mock private INeutronNetworkCRUD neutronNetworkCache;
    @Mock private BridgeConfigurationManager bridgeConfigurationManager;

    /**
     * Test method {@link NetworkHandler#canCreateNetwork(NeutronNetwork)}
     */
    @Test
    public void testCanCreateNetwork() {
        when(neutronNetwork.isShared())
                                    .thenReturn(true)
                                    .thenReturn(false);
        assertEquals("Error, did not return the correct HTTP flag", HttpURLConnection.HTTP_NOT_ACCEPTABLE, networkHandler.canCreateNetwork(neutronNetwork));
        assertEquals("Error, did not return the correct HTTP flag", HttpURLConnection.HTTP_CREATED, networkHandler.canCreateNetwork(neutronNetwork));
    }

    /**
     * Test method {@link NetworkHandler#canUpdateNetwork(NeutronNetwork, NeutronNetwork)}
     */
    @Test
    public void testCanUpdateNetwork() {
        assertEquals("Error, did not return the correct HTTP flag", HttpURLConnection.HTTP_OK, networkHandler.canUpdateNetwork(neutronNetwork, neutronNetwork));
    }

    /**
     * Test method {@link NetworkHandler#canDeleteNetwork(NeutronNetwork)}
     */
    @Test
    public void testCanDeleteNetwork() {
        assertEquals("Error, did not return the correct HTTP flag", HttpURLConnection.HTTP_OK, networkHandler.canDeleteNetwork(neutronNetwork));
    }

    /**
     * Test method {@link NetworkHandler#processEvent(AbstractEvent)}
     */
    /* TODO SB_MIGRATION */ @Ignore
    @Test
    public void testProcessEvent() {
        NetworkHandler networkHandlerSpy = Mockito.spy(networkHandler);

        NorthboundEvent ev = mock(NorthboundEvent.class);
        when(ev.getNeutronNetwork()).thenReturn(neutronNetwork);

        when(ev.getAction()).thenReturn(Action.ADD);
        networkHandlerSpy.processEvent(ev);
        verify(neutronL3Adapter, times(1)).handleNeutronNetworkEvent(any(NeutronNetwork.class), same(Action.ADD));

        when(ev.getAction()).thenReturn(Action.UPDATE);
        networkHandlerSpy.processEvent(ev);
        verify(neutronL3Adapter, times(1)).handleNeutronNetworkEvent(any(NeutronNetwork.class), same(Action.UPDATE));

        /* configuration needed to pass doNeutronNetworkDeleted() function*/
        Node node = mock(Node.class);
        List<Node> nodes = new ArrayList();
        nodes.add(node);
        /* TODO SB_MIGRATION */
        //when(connectionService.getNodes()).thenReturn(nodes);

        ConcurrentMap<String, Row> ports = new ConcurrentHashMap<>();
        Row row = mock(Row.class);
        ports.put("key", row);
        //when(ovsdbConfigurationService.getRows(any(Node.class), anyString())).thenReturn(ports);

        Port port = mock(Port.class);
        Column<GenericTableSchema, Set<UUID>> portColumns = mock(Column.class);
        Set<UUID> interfaceUUIDs = new HashSet<UUID>();
        UUID uuid = mock(UUID.class);
        interfaceUUIDs.add(uuid);
        when(port.getInterfacesColumn()).thenReturn(portColumns);
        when(portColumns.getData()).thenReturn(interfaceUUIDs);
        //when(ovsdbConfigurationService.getTypedRow(any(Node.class), same(Port.class), any(Row.class))).thenReturn(port);

        Interface iface = mock(Interface.class);
        Column<GenericTableSchema, String> ifaceColumns = mock(Column.class);
        when(iface.getTypeColumn()).thenReturn(ifaceColumns);
        when(ifaceColumns.getData()).thenReturn(NetworkHandler.NETWORK_TYPE_VXLAN);
        //when(ovsdbConfigurationService.getTypedRow(any(Node.class), same(Interface.class), any(Row.class))).thenReturn(iface);
        /**/

        when(ev.getAction()).thenReturn(Action.DELETE);
        networkHandlerSpy.processEvent(ev);
        /* TODO SB_MIGRATION */
        //verify(neutronL3Adapter, times(1)).handleNeutronNetworkEvent(any(NeutronNetwork.class), same(Action.DELETE));
        verify(networkHandlerSpy, times(1)).canDeleteNetwork(any(NeutronNetwork.class));
        verify(tenantNetworkManager, times(1)).networkDeleted(anyString());
    }
}
