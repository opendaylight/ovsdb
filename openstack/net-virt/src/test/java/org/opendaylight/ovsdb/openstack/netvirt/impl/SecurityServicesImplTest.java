/*
 * Copyright (c) 2015 Inocybe and others.  All rights reserved.
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
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.opendaylight.neutron.spi.INeutronPortCRUD;
import org.opendaylight.neutron.spi.NeutronPort;
import org.opendaylight.neutron.spi.NeutronSecurityGroup;
import org.opendaylight.ovsdb.openstack.netvirt.api.Southbound;
import org.opendaylight.ovsdb.utils.servicehelper.ServiceHelper;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * Unit test for {@link SecurityServicesImpl}
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(ServiceHelper.class)
public class SecurityServicesImplTest {

    @InjectMocks private SecurityServicesImpl securityServicesImpl;

    @Mock private INeutronPortCRUD neutronPortCache;
    @Mock private Southbound southbound;

    @Mock private NeutronSecurityGroup neutronSecurityGroup;

    private static final String NEUTRON_PORT_ID = "neutronID";
    private static final String DEVICE_OWNER = "compute";

    @Before
    public void setUp(){
        NeutronPort neutronPort = mock(NeutronPort.class);

        List<NeutronSecurityGroup> securityGroups = new ArrayList<NeutronSecurityGroup>();
        securityGroups.add(neutronSecurityGroup);

        when(neutronPort.getSecurityGroups()).thenReturn(securityGroups);
        when(neutronPort.getDeviceOwner()).thenReturn(DEVICE_OWNER);

        when(southbound.getInterfaceExternalIdsValue(any(OvsdbTerminationPointAugmentation.class), anyString())).thenReturn(NEUTRON_PORT_ID);
        when(neutronPortCache.getPort(anyString())).thenReturn(neutronPort);
    }

    /**
     * Test method {@link SecurityServicesImpl#isPortSecurityReady(Interface)}
     */
    @Test
    public void testIsPortSecurityReady(){
        assertTrue("Error, did not return expected boolean for isPortSecurityReady", securityServicesImpl.isPortSecurityReady(mock(OvsdbTerminationPointAugmentation.class)));
    }

    /**
     * Test method {@link SecurityServicesImpl#getSecurityGroupInPortList(Interface)}
     */
    @Test
    public void testSecurityGroupInPort(){
        assertEquals("Error, did not return the good neutronSecurityGroup of securityGroups",
                     neutronSecurityGroup, securityServicesImpl.getSecurityGroupInPortList(mock(OvsdbTerminationPointAugmentation.class)).get(0));
    }

    @Test
    public void testSetDependencies() throws Exception {
        Southbound southbound = mock(Southbound.class);

        PowerMockito.mockStatic(ServiceHelper.class);
        PowerMockito.when(ServiceHelper.getGlobalInstance(Southbound.class, securityServicesImpl)).thenReturn(southbound);

        securityServicesImpl.setDependencies(mock(BundleContext.class), mock(ServiceReference.class));

        assertEquals("Error, did not return the correct object", getField("southbound"), southbound);
    }

    @Test
    public void testSetDependenciesObject() throws Exception{
        INeutronPortCRUD neutronPortCache = mock(INeutronPortCRUD.class);
        securityServicesImpl.setDependencies(neutronPortCache);
        assertEquals("Error, did not return the correct object", getField("neutronPortCache"), neutronPortCache);
    }

    private Object getField(String fieldName) throws Exception {
        Field field = SecurityServicesImpl.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(securityServicesImpl);
    }
}
