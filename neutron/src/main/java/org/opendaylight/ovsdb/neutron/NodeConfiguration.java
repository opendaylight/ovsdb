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
import org.opendaylight.ovsdb.lib.notation.Row;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.plugin.OvsdbConfigService;
import org.opendaylight.ovsdb.schema.openvswitch.Interface;
import org.opendaylight.ovsdb.schema.openvswitch.Port;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NodeConfiguration {
    static final Logger logger = LoggerFactory.getLogger(NodeConfiguration.class);
    private static final int MAX_VLAN = 4096;
    private java.util.Queue<Integer> internalVlans = new LinkedList<>();
    private ConcurrentMap<String, Integer> tenantVlanMap = new ConcurrentHashMap<>();
    private ITenantNetworkManager tenantNetworkManager;

    public NodeConfiguration(Node node, ITenantNetworkManager tenantNetworkManager) {
        for (int i = 1; i < MAX_VLAN ; i++) {
            internalVlans.add(i);
        }
        setTenantNetworkManager(tenantNetworkManager);
        initializeNodeConfiguration(node);
    }


    private void initializeNodeConfiguration(Node node) {
        int vlan = 0;
        String networkId = new String();
        OvsdbConfigService ovsdbTable = (OvsdbConfigService) ServiceHelper.getGlobalInstance(OvsdbConfigService.class, this);

        try {
            Map<String, Row> portRows = ovsdbTable.getRows(node, ovsdbTable.getTableName(node, Port.class));

            if (portRows == null){
                logger.info("Interface table is null for Node {}", node);
                return;
            }

            for (Row row : portRows.values()) {
                Port port = ovsdbTable.getTypedRow(node, Port.class, row);

                if (port.getTagColumn() == null) continue;
                BigInteger[] tags = port.getTagColumn().getData().toArray(new BigInteger[0]);
                if (tags.length == 1)
                {
                    //There is only one tag here
                    vlan = tags[0].intValue();
                }
                else {
                   logger.debug("This port ({}) has {} tags", port.getName(), tags.length);
                   continue;
                }

                for (UUID ifaceId : port.getInterfacesColumn().getData()) {
                    Row ifaceRow = ovsdbTable.getRow(node, ovsdbTable.getTableName(node, Interface.class), ifaceId.toString());
                    Interface iface = ovsdbTable.getTypedRow(node, Interface.class, ifaceRow);

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

                } else {
                    logger.debug("Node: {} initialized without a vlan", node);
                }
            }
        }
        catch (Exception e) {
            logger.error("Error getting Port table for Node {}: {}", node, e);
        }
    }

    /*
     * Return the currently mapped internal vlan or get the next
     * free internal vlan from the available pool and map it to the networkId.
     */
    public int assignInternalVlan (String networkId) {
        Integer mappedVlan = tenantVlanMap.get(networkId);
        if (mappedVlan != null) return mappedVlan;
        mappedVlan = internalVlans.poll();
        if (mappedVlan != null) tenantVlanMap.put(networkId, mappedVlan);
        return mappedVlan;
    }

    /*
     * Return the mapped internal vlan to the available pool.
     */
    public int reclaimInternalVlan (String networkId) {
        Integer mappedVlan = tenantVlanMap.get(networkId);
        if (mappedVlan != null) {
            tenantVlanMap.remove(mappedVlan);
            internalVlans.add(mappedVlan);
            return mappedVlan;
        }
        return 0;
    }

    /*
     * Remove the internal vlan from the available pool.
     */
    public void internalVlanInUse (int vlan) {
        internalVlans.remove(vlan);
    }

    /*
     * Return a vlan from the mapped pool keyed by the networkId.
     */
    public int getInternalVlan (String networkId) {
        Integer vlan = tenantVlanMap.get(networkId);
        if (vlan == null) return 0;
        return vlan.intValue();
    }

    public void setTenantNetworkManager(ITenantNetworkManager tenantNetworkManager) {
        this.tenantNetworkManager = tenantNetworkManager;
    }
}
