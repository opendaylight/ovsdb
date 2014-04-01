/*******************************************************************************
 * Copyright (c) 2013 Hewlett-Packard Development Company, L.P.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Dave Tucker (HP) - Replace tenantVlanMap with a per Node cache
 *******************************************************************************/

package org.opendaylight.ovsdb.neutron;

import java.math.BigInteger;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.utils.ServiceHelper;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.table.Interface;
import org.opendaylight.ovsdb.lib.table.Port;
import org.opendaylight.ovsdb.lib.table.internal.Table;
import org.opendaylight.ovsdb.plugin.OVSDBConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NodeConfiguration {
    static final Logger logger = LoggerFactory.getLogger(NodeConfiguration.class);
    private static final int MAX_VLAN = 4096;
    private java.util.Queue<Integer> internalVlans = new LinkedList<>();
    private ConcurrentMap<String, Integer> tenantVlanMap = new ConcurrentHashMap<>();
    private volatile ITenantNetworkManager tenantNetworkManager;

    public NodeConfiguration(Node node) {
        for (int i = 1; i < MAX_VLAN ; i++) {
            internalVlans.add(i);
        }

        initializeNodeConfiguration(node);
    }


    private void initializeNodeConfiguration(Node node) {

        int vlan = 0;
        String networkId = new String();
        OVSDBConfigService ovsdbTable = (OVSDBConfigService) ServiceHelper.getGlobalInstance(OVSDBConfigService.class, this);

        try {
            Map<String, Table<?>> portRows = ovsdbTable.getRows(node, Port.NAME.getName());

            if (portRows == null){
                logger.info("Interface table is null for Node {}", node);
                return;
            }

            for (Table<?> row : portRows.values()) {
                Port port = (Port)row;

                BigInteger[] tags = port.getTag().toArray(new BigInteger[0]);
                if (tags.length == 1)
                {
                    //There is only one tag here
                    vlan = tags[0].intValue();
                }
                else {
                   logger.debug("This port has more {} interfaces", tags.length);
                   continue;
                }

                for (UUID ifaceId : port.getInterfaces()) {
                    Interface iface = (Interface)ovsdbTable.getRow(node, Interface.NAME.getName(), ifaceId.toString());

                    if (iface == null) {
                        logger.error("Interface table is null for Po");
                        continue;
                    }

                    networkId = tenantNetworkManager.getTenantNetworkForInterface(iface).getNetworkUUID();

                    if (networkId != null) break;
                }

                if (vlan != 0 && networkId != null) {

                    this.internalVlanInUse(vlan);
                    this.tenantVlanMap.put(networkId, vlan);

                }
            }
        }
        catch (Exception e) {
            logger.error("Error getting Port table for Node {}: {}", node, e);
        }
    }

    public int assignInternalVlan (String networkId) {
        Integer mappedVlan = tenantVlanMap.get(networkId);
        if (mappedVlan != null) return mappedVlan;
        mappedVlan = internalVlans.poll();
        if (mappedVlan != null) tenantVlanMap.put(networkId, mappedVlan);
        return mappedVlan;
    }

    public int reclaimInternalVlan (String networkId) {
        Integer mappedVlan = tenantVlanMap.get(networkId);
        if (mappedVlan != null) {
            tenantVlanMap.remove(mappedVlan);
            internalVlans.add(mappedVlan);
            return mappedVlan;
        }
        return 0;
    }

    public void internalVlanInUse (int vlan) {
        internalVlans.remove(vlan);
    }

    public int getInternalVlan (String networkId) {
        Integer vlan = tenantVlanMap.get(networkId);
        if (vlan == null) return 0;
        return vlan.intValue();
    }

}
