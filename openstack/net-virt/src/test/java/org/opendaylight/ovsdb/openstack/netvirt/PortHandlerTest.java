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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
import org.opendaylight.neutron.spi.NeutronPort;
import org.opendaylight.ovsdb.openstack.netvirt.api.Action;
import org.opendaylight.ovsdb.openstack.netvirt.api.Constants;
import org.opendaylight.ovsdb.openstack.netvirt.impl.NeutronL3Adapter;
//import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;

/**
 * Unit test fort {@link PortHandler}
 */
@Ignore // TODO SB_MIGRATION
@RunWith(MockitoJUnitRunner.class)
public class PortHandlerTest {

    @InjectMocks PortHandler portHandler;

    @Mock private NeutronL3Adapter neutronL3Adapter;

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

    @Ignore
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


        //Node node = mock(Node.class);
        //List<Node> nodes = new ArrayList();
        //nodes.add(node);
        /* TODO SB_MIGRATION */
        //when(connectionService.getBridgeNodes()).thenReturn(nodes);

        //Row row = mock(Row.class);
        //ConcurrentMap<String, Row> portRows = new ConcurrentHashMap();
        //portRows.put("key", row);
        //when(ovsdbConfigurationService.getRows(any(Node.class), anyString())).thenReturn(portRows );

        //Port port = mock(Port.class);
        //Column<GenericTableSchema, Set<UUID>> itfaceColumns = mock(Column.class);
        //when(port.getInterfacesColumn()).thenReturn(itfaceColumns);
        //Set<UUID> ifaceUUIDs = new HashSet();
        //ifaceUUIDs.add(mock(UUID.class));
        //when(itfaceColumns.getData()).thenReturn(ifaceUUIDs );
        //when(ovsdbConfigurationService.getTypedRow(any(Node.class), same(Port.class), any(Row.class))).thenReturn(port);

        //Interface itface = mock(Interface.class);
        //Column<GenericTableSchema, Map<String, String>> externalIdColumns = mock(Column.class);
        Map<String, String> externalIds = new HashMap();
        externalIds.put(Constants.EXTERNAL_ID_INTERFACE_ID, "portUUID");
        //when(externalIdColumns.getData()).thenReturn(externalIds);
        //when(itface.getExternalIdsColumn()).thenReturn(externalIdColumns);
        //when(ovsdbConfigurationService.getTypedRow(any(Node.class), same(Interface.class), any(Row.class))).thenReturn(itface);


        when(ev.getAction()).thenReturn(Action.DELETE);
        //portHandlerSpy.processEvent(ev);
        verify(neutronL3Adapter, times(1)).handleNeutronPortEvent(neutronPort, Action.DELETE);
    }
}
