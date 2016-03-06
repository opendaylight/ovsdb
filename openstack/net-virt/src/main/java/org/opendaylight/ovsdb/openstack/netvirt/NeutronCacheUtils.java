/*
 * Copyright (c) 2014, 2015 SDN Hub, LLC. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt;

import org.opendaylight.ovsdb.openstack.netvirt.translator.NeutronNetwork;
import org.opendaylight.ovsdb.openstack.netvirt.translator.NeutronPort;
import org.opendaylight.ovsdb.openstack.netvirt.translator.NeutronSubnet;
import org.opendaylight.ovsdb.openstack.netvirt.translator.Neutron_IPs;
import org.opendaylight.ovsdb.openstack.netvirt.translator.crud.INeutronNetworkCRUD;
import org.opendaylight.ovsdb.openstack.netvirt.translator.crud.INeutronPortCRUD;
import org.opendaylight.ovsdb.openstack.netvirt.translator.crud.INeutronSubnetCRUD;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;

public class NeutronCacheUtils {

    /**
     * Look up in the NeutronPortsCRUD cache and return the MAC address for a corresponding IP address
     * @param neutronPortsCache Reference to port cache to get existing port related data. This interface
     * basically read data from the md-sal data store.
     * @param subnetID subnet to which given port is attached
     * @param ipAddr IP address of a member or VM
     * @return MAC address registered with that IP address
     */
    public static String getMacAddress(INeutronPortCRUD neutronPortsCache, String subnetID, String ipAddr) {
        if (ipAddr == null || subnetID == null) {
            return null;
        }

        List<NeutronPort> allPorts = neutronPortsCache.getAllPorts();
        for (NeutronPort port : allPorts) {
            List<Neutron_IPs> fixedIPs = port.getFixedIPs();
            if (fixedIPs != null && !fixedIPs.isEmpty()) {
                for (Neutron_IPs ip : fixedIPs) {
                    if (ip.getIpAddress().equals(ipAddr) && ip.getSubnetUUID().equals(subnetID)) {
                        return port.getMacAddress();
                    }
                }
            }
        }
        return null;
    }

    /**
     * Look up in the NeutronNetworkCRUD cache and NeutronSubnetCRUD cache for
     * extracting the provider segmentation_type and segmentation_id
     * @param neutronNetworkCache Reference to neutron network cache to get existing network related data.
     * This interface basically read data from the md-sal data store.
     * @param neutronSubnetCache Reference to neutron subnet cache to get existing subnet related data.
     * This interface basically read data from the md-sal data store.
     * @param subnetID Subnet UUID
     * @return {Type: ID} pair for that subnet ID
     */
    public static Map.Entry<String,String> getProviderInformation(INeutronNetworkCRUD neutronNetworkCache,
                INeutronSubnetCRUD neutronSubnetCache, String subnetID) {

        String networkID = null;

        List<NeutronSubnet> allSubnets = neutronSubnetCache.getAllSubnets();
        for (NeutronSubnet subnet: allSubnets) {
            if (subnet.getID().equals(subnetID)) {
                networkID = subnet.getNetworkUUID();
                break;
            }
        }
        if (networkID == null) {
            return null;
        }

        List<NeutronNetwork> allNetworks = neutronNetworkCache.getAllNetworks();
        for (NeutronNetwork network: allNetworks) {
            if (network.getID().equals(networkID)) {
                return new AbstractMap.SimpleEntry<>(
                        network.getProviderNetworkType(), network.getProviderSegmentationID());
            }
        }
        return null;
    }
}
