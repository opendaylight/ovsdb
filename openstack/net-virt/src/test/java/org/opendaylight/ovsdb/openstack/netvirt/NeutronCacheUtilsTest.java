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
import static org.mockito.Mockito.when;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.opendaylight.ovsdb.openstack.netvirt.translator.INeutronNetworkCRUD;
import org.opendaylight.ovsdb.openstack.netvirt.translator.INeutronPortCRUD;
import org.opendaylight.ovsdb.openstack.netvirt.translator.INeutronSubnetCRUD;
import org.opendaylight.ovsdb.openstack.netvirt.translator.NeutronNetwork;
import org.opendaylight.ovsdb.openstack.netvirt.translator.NeutronPort;
import org.opendaylight.ovsdb.openstack.netvirt.translator.NeutronSubnet;
import org.opendaylight.ovsdb.openstack.netvirt.translator.Neutron_IPs;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * Unit test for {@link NeutronCacheUtils}
 */
@RunWith(PowerMockRunner.class)
public class NeutronCacheUtilsTest {

    /**
     * Test method {@link NeutronCacheUtils#getMacAddress(INeutronPortCRUD, String, String)}
     */
    @Test
    public void testGetMacAddress(){
        INeutronPortCRUD neutronPortsCache = mock(INeutronPortCRUD.class);
        NeutronPort port = mock(NeutronPort.class);
        Neutron_IPs ip = mock(Neutron_IPs.class);
        when(ip.getIpAddress()).thenReturn("ip_address");
        when(ip.getSubnetUUID()).thenReturn("subnetUUID");
        List<Neutron_IPs> list_fixedIP = new ArrayList<Neutron_IPs>();
        list_fixedIP.add(ip);
        when(port.getFixedIPs()).thenReturn(list_fixedIP);
        when(port.getMacAddress()).thenReturn("mac_address");
        List<NeutronPort> list_port = new ArrayList<NeutronPort>();
        list_port.add(port);

        when(neutronPortsCache.getAllPorts()).thenReturn(list_port);

        assertEquals("Error, getMacAddress() did not return the correct value", "mac_address", NeutronCacheUtils.getMacAddress(neutronPortsCache, "subnetUUID", "ip_address"));
    }

    /**
     * Test method {@link NeutronCacheUtils#getProviderInformation(INeutronNetworkCRUD, INeutronSubnetCRUD, String)}
     */
    @Test
    public void testGetProviderInformation() {
        INeutronSubnetCRUD neutronSubnetCache = mock(INeutronSubnetCRUD.class);
        NeutronSubnet subnet = mock(NeutronSubnet.class);
        when(subnet.getID()).thenReturn("subnetUUID");
        when(subnet.getNetworkUUID()).thenReturn("networkUUID");
        List<NeutronSubnet> list_subnet = new ArrayList<NeutronSubnet>();
        list_subnet.add(subnet);

        when(neutronSubnetCache.getAllSubnets()).thenReturn(list_subnet );

        INeutronNetworkCRUD neutronNetworkCache = mock(INeutronNetworkCRUD.class);
        NeutronNetwork network = mock(NeutronNetwork.class);
        when(network.getID()).thenReturn("networkUUID");
        when(network.getProviderNetworkType()).thenReturn("network_type_1");
        when(network.getProviderSegmentationID()).thenReturn("network_segID");
        List<NeutronNetwork> list_network = new ArrayList<NeutronNetwork>();
        list_network.add(network);

        when(neutronNetworkCache.getAllNetworks()).thenReturn(list_network);

        Map.Entry<String,String> entry = new AbstractMap.SimpleEntry<String, String>(
                network.getProviderNetworkType(), network.getProviderSegmentationID());

        assertEquals("Error, getProviderInformation() did not return the correct values", entry, NeutronCacheUtils.getProviderInformation(neutronNetworkCache, neutronSubnetCache, "subnetUUID"));
    }
}
