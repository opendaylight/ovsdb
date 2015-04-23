/*
 * Copyright (c) 2015 Inocybe and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.HttpURLConnection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.neutron.spi.NeutronSubnet;
import org.opendaylight.ovsdb.openstack.netvirt.api.Action;
import org.opendaylight.ovsdb.openstack.netvirt.impl.NeutronL3Adapter;

/**
 * Unit test fort {@link SubnetHandler}
 */
@RunWith(MockitoJUnitRunner.class)
public class SubnetHandlerTest {

    @InjectMocks private SubnetHandler subnetHandler;

    @Mock NeutronL3Adapter neutronl3Adapter;

    @Test
    public void testCanCreateSubnet() {
        assertEquals("Error, canCreateSubnet() did not return the correct HTTP flag", HttpURLConnection.HTTP_OK, subnetHandler.canCreateSubnet(mock(NeutronSubnet.class)));
    }

    @Test
    public void testCanUpdateSubnet() {
        assertEquals("Error, canUpdateSubnet() did not return the correct HTTP flag", HttpURLConnection.HTTP_OK, subnetHandler.canUpdateSubnet(mock(NeutronSubnet.class), mock(NeutronSubnet.class)));
    }

    @Test
    public void testCanDeleteSubnet() {
        assertEquals("Error, canDeleteSubnet() did not return the correct HTTP flag", HttpURLConnection.HTTP_OK, subnetHandler.canDeleteSubnet(mock(NeutronSubnet.class)));
    }

    @Test
    public void testProcessEvent() {
        NorthboundEvent ev = mock(NorthboundEvent.class);
        when(ev.getSubnet()).thenReturn(mock(NeutronSubnet.class));

        when(ev.getAction()).thenReturn(Action.ADD);
        subnetHandler.processEvent(ev);
        verify(neutronl3Adapter, times(1)).handleNeutronSubnetEvent(ev.getSubnet(), ev.getAction());;

        when(ev.getAction()).thenReturn(Action.DELETE);
        subnetHandler.processEvent(ev);
        verify(neutronl3Adapter, times(1)).handleNeutronSubnetEvent(ev.getSubnet(), ev.getAction());;

        when(ev.getAction()).thenReturn(Action.UPDATE);
        subnetHandler.processEvent(ev);
        verify(neutronl3Adapter, times(1)).handleNeutronSubnetEvent(ev.getSubnet(), ev.getAction());;
    }
}
