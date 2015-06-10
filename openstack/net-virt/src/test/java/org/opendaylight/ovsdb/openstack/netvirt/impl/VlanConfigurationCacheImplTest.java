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
import org.opendaylight.neutron.spi.NeutronNetwork;
import org.opendaylight.ovsdb.openstack.netvirt.api.Southbound;
import org.opendaylight.ovsdb.openstack.netvirt.api.TenantNetworkManager;
import org.opendaylight.ovsdb.utils.servicehelper.ServiceHelper;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.VlanId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * Unit test for {@link VlanConfigurationCacheImpl}
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(ServiceHelper.class)
public class VlanConfigurationCacheImplTest {

    @InjectMocks public VlanConfigurationCacheImpl vlanConfigurationCacheImpl;

    @Mock private TenantNetworkManager tenantNetworkManager;
    @Mock private Southbound southbound;

    private static final String NODE_UUID = "nodeUUID";
    private static final String NETWORK_ID= "networkId";
    private static final int VLAN_ID = 4;
    /**
     * Function configuring the node
     */
    @Before
    public void setUp(){
        when(southbound.getOvsdbNodeUUID(any(Node.class))).thenReturn(NODE_UUID);
        List<OvsdbTerminationPointAugmentation> ports = new ArrayList<OvsdbTerminationPointAugmentation>();
        OvsdbTerminationPointAugmentation port = mock(OvsdbTerminationPointAugmentation.class);
        VlanId vlanId = mock(VlanId.class);
        when(vlanId.getValue()).thenReturn(VLAN_ID);
        when(port.getVlanTag()).thenReturn(vlanId);
        when(southbound.getTerminationPointsOfBridge(any(Node.class))).thenReturn(ports );
        NeutronNetwork neutronNetwork = mock(NeutronNetwork.class);
        when(neutronNetwork.getNetworkUUID()).thenReturn(NETWORK_ID);
        when(tenantNetworkManager.getTenantNetwork(any(OvsdbTerminationPointAugmentation.class))).thenReturn(neutronNetwork );
    }

    /**
     * Test method {@link VlanConfigurationCacheImpl#assignInternalVlan(Node, String)}
     */
    @Test
    public void testAssignReclaimAndGetInternalVlan() {
        assertEquals("Error, assignInternalVlan() did not return the correct internalVlanId (first added)", 1, (int) vlanConfigurationCacheImpl.assignInternalVlan(any(Node.class), NETWORK_ID));
        assertEquals("Error, assignInternalVlan () did not return the correct internalVlanId (second added)", 2, (int) vlanConfigurationCacheImpl.assignInternalVlan(any(Node.class), NETWORK_ID + "1"));

        assertEquals("Error, getInternalVlan() did not return the correct internalVlan", 1, (int) vlanConfigurationCacheImpl.getInternalVlan(any(Node.class), NETWORK_ID));

        assertEquals("Error, reclaimInternalVlan() did not return the correct internalVlanId", 1, (int) vlanConfigurationCacheImpl.reclaimInternalVlan(any(Node.class), NETWORK_ID));
        assertEquals("Error, reclaimInternalVlan() did not return the correct internalVlanId", 2, (int) vlanConfigurationCacheImpl.reclaimInternalVlan(any(Node.class), NETWORK_ID + "1"));
    }

    @Test
    public void testSetDependencies() throws Exception {
        TenantNetworkManager tenantNetworkManager = mock(TenantNetworkManager.class);
        Southbound southbound = mock(Southbound.class);

        PowerMockito.mockStatic(ServiceHelper.class);
        PowerMockito.when(ServiceHelper.getGlobalInstance(TenantNetworkManager.class, vlanConfigurationCacheImpl)).thenReturn(tenantNetworkManager);
        PowerMockito.when(ServiceHelper.getGlobalInstance(Southbound.class, vlanConfigurationCacheImpl)).thenReturn(southbound);

        vlanConfigurationCacheImpl.setDependencies(mock(BundleContext.class), mock(ServiceReference.class));

        assertEquals("Error, did not return the correct object", getField("tenantNetworkManager"), tenantNetworkManager);
        assertEquals("Error, did not return the correct object", getField("southbound"), southbound);
    }

    private Object getField(String fieldName) throws Exception {
        Field field = VlanConfigurationCacheImpl.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(vlanConfigurationCacheImpl);
    }
}
