/*
 * Copyright (c) 2015 Inocybe and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.openstack.netvirt.impl;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.neutron.spi.NeutronNetwork;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;

/**
 * Unit test for {@link VlanConfigurationCacheImpl}
 */
@Ignore // TODO SB_MIGRATION
@RunWith(MockitoJUnitRunner.class)
public class VlanConfigurationCacheImplTest {

    @InjectMocks public VlanConfigurationCacheImpl vlanConfigurationCacheImpl;
    @InjectMocks private TenantNetworkManagerImpl tenantNetworkManagerImpl = mock(TenantNetworkManagerImpl.class);

    private static final String NODE_UUID = "nodeUUID";
    private static final String NETWORK_ID= "networkId";

    /**
     * Function configuring the node
     */
    @Before
    public void setUp(){
        //Row row = mock(Row.class);
        //Port port = mock(Port.class);

        //ConcurrentHashMap<String, Row> ovsTable;
        //ovsTable = new ConcurrentHashMap<>();
        //ovsTable.put(NODE_UUID, row);

        Set<Long> tags = new HashSet<Long>();
        tags.add(Long.valueOf(1));

        //UUID uuid = mock(UUID.class);
        //Set<UUID> uuidSet = new HashSet<>();
        //uuidSet.add(uuid);

        //Column<GenericTableSchema, Set<Long>> longColumnMock = mock(Column.class);
        //Column<GenericTableSchema, Set<UUID>> uuidColumnMock = mock(Column.class);

        //Interface iface = mock(Interface.class);
        NeutronNetwork neutronNetwork = mock(NeutronNetwork.class);

        /* TODO SB_MIGRATION */
        //when(ovsdbConfigurationService.getRows(any(Node.class), anyString())).thenReturn(ovsTable);
        //when(ovsdbConfigurationService.getTypedRow(any(Node.class), same(Port.class), any(Row.class))).thenReturn(port);

        //when(port.getTagColumn()).thenReturn(longColumnMock);
        //when(longColumnMock.getData()).thenReturn(tags);
        //when(port.getInterfacesColumn()).thenReturn(uuidColumnMock);
        //when(uuidColumnMock.getData()).thenReturn(uuidSet);

        //when(ovsdbConfigurationService.getRow(any(Node.class), anyString(), anyString())).thenReturn(row);
        //when(ovsdbConfigurationService.getTypedRow(any(Node.class), same(Interface.class), any(Row.class))).thenReturn(iface);

        //when(tenantNetworkManagerImpl.getTenantNetwork(any(Interface.class))).thenReturn(neutronNetwork);
        when(neutronNetwork.getNetworkUUID()).thenReturn(NETWORK_ID);
    }

    /**
     * Test method {@link VlanConfigurationCacheImpl#assignInternalVlan(Node, String)}
     */
    /* TODO SB_MIGRATION */
    @Ignore
    @Test
    public void testAssignInternalVlan() {
        assertEquals("Error, did not return the correct internalVlanId (first added)", 1, (int) vlanConfigurationCacheImpl.assignInternalVlan(any(Node.class), NETWORK_ID));
        assertEquals("Error, did not return the correct internalVlanId (second added)", 2, (int) vlanConfigurationCacheImpl.assignInternalVlan(any(Node.class), NETWORK_ID + "1"));
    }

    /**
     * Test method {@link VlanConfigurationCacheImpl#reclaimInternalVlan(Node, String)}
     */
    @Test
    public void testReclaimInternalVlan(){
        /* TODO SB_MIGRATION */
        //assertEquals("Error, did not return the correct internalVlanId", 1, (int) vlanConfigurationCacheImpl.reclaimInternalVlan(any(Node.class), NETWORK_ID));
    }

    /**
     * Test method {@link VlanConfigurationCacheImpl#getInternalVlan(Node, String)}
     */
    @Test
    public void testGetInternalVlan(){
        /* TODO SB_MIGRATION */
        //assertEquals("Error, did not return the correct internalVlan", 1, (int) vlanConfigurationCacheImpl.getInternalVlan(any(Node.class), NETWORK_ID));
    }
}
