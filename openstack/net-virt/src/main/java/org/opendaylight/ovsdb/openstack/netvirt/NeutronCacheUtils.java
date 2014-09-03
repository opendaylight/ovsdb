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

import org.opendaylight.controller.networkconfig.neutron.INeutronPortCRUD;
import org.opendaylight.controller.networkconfig.neutron.NeutronPort;
import org.opendaylight.controller.networkconfig.neutron.Neutron_IPs;
import java.util.Iterator;
import java.util.List;

public class NeutronCacheUtils {

    /**
     * Look up in the NeutronPortsCRUD cache and return the MAC address for a corresponding IP address
     * @param ipAddr IP address of a member or VM
     * @return MAC address registered with that IP address
     */
    public static String getMacAddress(INeutronPortCRUD neutronPortsCache, String ipAddr) {
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
                     if (ip.getIpAddress().equals(ipAddr))
                         return port.getMacAddress();
                 }
             }
         }
        return null;
    }
}
