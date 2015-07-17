/*
 * Copyright (C) 2014 SDN Hub, LLC.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Srini Seetharaman
 */

package org.opendaylight.ovsdb.openstack.netvirt;

import org.opendaylight.neutron.spi.INeutronNetworkCRUD;
import org.opendaylight.neutron.spi.INeutronPortCRUD;
import org.opendaylight.neutron.spi.INeutronSubnetCRUD;
import org.opendaylight.neutron.spi.NeutronNetwork;
import org.opendaylight.neutron.spi.NeutronPort;
import org.opendaylight.neutron.spi.NeutronSubnet;
import org.opendaylight.neutron.spi.Neutron_IPs;

import java.util.AbstractMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class NeutronCacheUtils {

    /**
     * Look up in the NeutronPortsCRUD cache and return the MAC address for a corresponding IP address
     * @param ipAddr IP address of a member or VM
     * @return MAC address registered with that IP address
     */
    public static String getMacAddress(INeutronPortCRUD neutronPortsCache, String subnetID, String ipAddr) {
        if (ipAddr == null || subnetID == null) {
            return null;
        }

        List<Neutron_IPs> fixedIPs;
        Iterator<Neutron_IPs> fixedIPIterator;
        Neutron_IPs ip;

        List<NeutronPort> allPorts = neutronPortsCache.getAllPorts();
        Iterator<NeutronPort> i = allPorts.iterator();
        while (i.hasNext()) {
            NeutronPort port = i.next();
            fixedIPs = port.getFixedIPs();
            if (fixedIPs != null && fixedIPs.size() > 0) {
                fixedIPIterator = fixedIPs.iterator();
                while (fixedIPIterator.hasNext()) {
                    ip = fixedIPIterator.next();
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
                Map.Entry<String,String> entry = new AbstractMap.SimpleEntry<String, String>(
                        network.getProviderNetworkType(), network.getProviderSegmentationID());
                return entry;
            }
        }
        return null;
    }
}
