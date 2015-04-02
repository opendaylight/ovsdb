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
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.net.HttpURLConnection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.neutron.spi.NeutronFloatingIP;
import org.opendaylight.ovsdb.openstack.netvirt.api.Action;
import org.opendaylight.ovsdb.openstack.netvirt.impl.NeutronL3Adapter;

/**
 * Unit test for {@link FloatingIPHandler}
 */
@RunWith(MockitoJUnitRunner.class)
public class FloatingIPHandlerTest {

    @InjectMocks private FloatingIPHandler floatingHandler = mock(FloatingIPHandler.class, Mockito.CALLS_REAL_METHODS);
    @InjectMocks private NeutronL3Adapter neutronL3Adapter = mock(NeutronL3Adapter.class);

    @Test
    public void testCanCreateFloatingIP(){
        assertEquals("Error, did not return the correct HTTP status code", HttpURLConnection.HTTP_OK, floatingHandler.canCreateFloatingIP(mock(NeutronFloatingIP.class)));
    }

    @Test
    public void testCanUpdateFloatingIP(){
        assertEquals("Error, did not return the correct HTTP status code", HttpURLConnection.HTTP_OK, floatingHandler.canUpdateFloatingIP(mock(NeutronFloatingIP.class), mock(NeutronFloatingIP.class)));
    }

    @Test
    public void testCanDeleteFloatingIP(){
        assertEquals("Error, did not return the correct HTTP status code", HttpURLConnection.HTTP_OK, floatingHandler.canDeleteFloatingIP(mock(NeutronFloatingIP.class)));
    }

    @Test
    public void testProcessEvent(){
        NorthboundEvent ev = mock(NorthboundEvent.class);

        when(ev.getNeutronFloatingIP()).thenReturn(mock(NeutronFloatingIP.class));
        when(ev.getAction()).thenReturn(Action.UPDATE);

        verifyNoMoreInteractions(neutronL3Adapter);

        floatingHandler.processEvent((AbstractEvent) ev);

        verify(neutronL3Adapter, times(1)).handleNeutronFloatingIPEvent(ev.getNeutronFloatingIP(), ev.getAction());;

        when(ev.getAction()).thenReturn(Action.ADD);
        verify(neutronL3Adapter, times(0)).handleNeutronFloatingIPEvent(ev.getNeutronFloatingIP(), ev.getAction());;

        when(ev.getAction()).thenReturn(Action.DELETE);
        verify(neutronL3Adapter, times(0)).handleNeutronFloatingIPEvent(ev.getNeutronFloatingIP(), ev.getAction());;
    }
}
