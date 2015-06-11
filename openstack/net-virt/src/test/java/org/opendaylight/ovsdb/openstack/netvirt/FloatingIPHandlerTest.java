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

import java.lang.reflect.Field;
import java.net.HttpURLConnection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.opendaylight.neutron.spi.NeutronFloatingIP;
import org.opendaylight.ovsdb.openstack.netvirt.api.Action;
import org.opendaylight.ovsdb.openstack.netvirt.api.EventDispatcher;
import org.opendaylight.ovsdb.openstack.netvirt.impl.NeutronL3Adapter;
import org.opendaylight.ovsdb.utils.servicehelper.ServiceHelper;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * Unit test for {@link FloatingIPHandler}
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(ServiceHelper.class)
public class FloatingIPHandlerTest {

    @InjectMocks FloatingIPHandler floatingHandler;
    @Mock NeutronL3Adapter neutronL3Adapter;

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
        floatingHandler.processEvent((AbstractEvent) ev);
        verify(neutronL3Adapter, times(1)).handleNeutronFloatingIPEvent(ev.getNeutronFloatingIP(), ev.getAction());;

        when(ev.getAction()).thenReturn(Action.ADD);
        floatingHandler.processEvent((AbstractEvent) ev);
        verify(neutronL3Adapter, times(1)).handleNeutronFloatingIPEvent(ev.getNeutronFloatingIP(), ev.getAction());;

        when(ev.getAction()).thenReturn(Action.DELETE);
        floatingHandler.processEvent((AbstractEvent) ev);
        verify(neutronL3Adapter, times(1)).handleNeutronFloatingIPEvent(ev.getNeutronFloatingIP(), ev.getAction());;
    }

    @Test
    public void testSetDependencies() throws Exception {
        EventDispatcher eventDispatcher = mock(EventDispatcher.class);
        NeutronL3Adapter neutronL3Adapter = mock(NeutronL3Adapter.class);

        PowerMockito.mockStatic(ServiceHelper.class);
        PowerMockito.when(ServiceHelper.getGlobalInstance(EventDispatcher.class, floatingHandler)).thenReturn(eventDispatcher);
        PowerMockito.when(ServiceHelper.getGlobalInstance(NeutronL3Adapter.class, floatingHandler)).thenReturn(neutronL3Adapter);

        floatingHandler.setDependencies(mock(BundleContext.class), mock(ServiceReference.class));

        assertEquals("Error, did not return the correct object", floatingHandler.eventDispatcher, eventDispatcher);
        assertEquals("Error, did not return the correct object", getNeutronL3Adapter(), neutronL3Adapter);
    }

    private NeutronL3Adapter getNeutronL3Adapter() throws Exception {
        Field field = FloatingIPHandler.class.getDeclaredField("neutronL3Adapter");
        field.setAccessible(true);
        return (NeutronL3Adapter) field.get(floatingHandler);
    }
}
