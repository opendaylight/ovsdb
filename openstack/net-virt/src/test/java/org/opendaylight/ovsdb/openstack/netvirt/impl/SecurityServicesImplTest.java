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
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.neutron.spi.INeutronPortCRUD;
import org.opendaylight.neutron.spi.NeutronPort;
import org.opendaylight.neutron.spi.NeutronSecurityGroup;
import org.opendaylight.ovsdb.lib.notation.Column;
import org.opendaylight.ovsdb.lib.schema.GenericTableSchema;
import org.opendaylight.ovsdb.openstack.netvirt.api.Constants;
import org.opendaylight.ovsdb.schema.openvswitch.Interface;

/**
 * Unit test for {@link SecurityServicesImpl}
 */
@RunWith(MockitoJUnitRunner.class)
public class SecurityServicesImplTest {

    @InjectMocks private SecurityServicesImpl securityServicesImpl;
    @InjectMocks private INeutronPortCRUD neutronPortService = mock(INeutronPortCRUD.class);

    @Mock Interface intf;

    private List<NeutronSecurityGroup> securityGroups = new ArrayList<NeutronSecurityGroup>();

    @Before
    public void setUp(){
        NeutronPort neutronPort = mock(NeutronPort.class);

        Map<String, String> externalIds =new HashMap<String, String>();
        externalIds.put(Constants.EXTERNAL_ID_INTERFACE_ID, "mapValue");
        Column<GenericTableSchema, Map<String, String>> columnMock = mock(Column.class);

        securityGroups.add(mock(NeutronSecurityGroup.class));

        when(intf.getExternalIdsColumn()).thenReturn(columnMock);
        when(columnMock.getData()).thenReturn(externalIds);

        when(neutronPort.getSecurityGroups()).thenReturn(securityGroups);
        when(neutronPort.getDeviceOwner()).thenReturn("deviceOwner");
        when(neutronPortService.getPort(anyString())).thenReturn(neutronPort);
    }

    /**
     * Test method {@link SecurityServicesImpl#isPortSecurityReady(Interface)}
     */
    @Test
    public void testIsPortSecurityReady(){
        assertTrue("Error, did not return expected boolean for isPortSecurityReady", securityServicesImpl.isPortSecurityReady(intf));
    }

    /**
     * Test method {@link SecurityServicesImpl#getSecurityGroupInPort(Interface)}
     */
    @Test
    public void testSecurityGroupInPort(){
        assertEquals("Error, did not return the good neutronSecurityGroup of securityGroups", securityGroups.toArray()[0], securityServicesImpl.getSecurityGroupInPort(intf));
    }
}
