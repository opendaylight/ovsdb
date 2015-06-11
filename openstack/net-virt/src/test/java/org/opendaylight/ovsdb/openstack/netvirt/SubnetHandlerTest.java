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
import org.opendaylight.neutron.spi.NeutronSubnet;
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
 * Unit test fort {@link SubnetHandler}
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(ServiceHelper.class)
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

    @Test
    public void testSetDependencies() throws Exception {
        NeutronL3Adapter neutronL3Adapter = mock(NeutronL3Adapter.class);
        EventDispatcher eventDispatcher = mock(EventDispatcher.class);

        PowerMockito.mockStatic(ServiceHelper.class);
        PowerMockito.when(ServiceHelper.getGlobalInstance(NeutronL3Adapter.class, subnetHandler)).thenReturn(neutronL3Adapter);
        PowerMockito.when(ServiceHelper.getGlobalInstance(EventDispatcher.class, subnetHandler)).thenReturn(eventDispatcher);

        subnetHandler.setDependencies(mock(BundleContext.class), mock(ServiceReference.class));

        assertEquals("Error, did not return the correct object", getField("neutronL3Adapter"), neutronL3Adapter);
        assertEquals("Error, did not return the correct object", subnetHandler.eventDispatcher, eventDispatcher);
    }

    private Object getField(String fieldName) throws Exception {
        Field field = SubnetHandler.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(subnetHandler);
    }
}
