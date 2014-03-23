/*
 * Copyright (C) 2013 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Madhu Venugopal, Brent Salisbury
 */
package org.opendaylight.ovsdb.neutron.impl.provider;

import java.util.Map;

import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.utils.ServiceHelper;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.ovsdb.lib.table.Bridge;
import org.opendaylight.ovsdb.lib.table.Interface;
import org.opendaylight.ovsdb.lib.table.internal.Table;
import org.opendaylight.ovsdb.plugin.OVSDBConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ProviderNetworkManager {
    static final Logger logger = LoggerFactory.getLogger(ProviderNetworkManager.class);
    private static ProviderNetworkManager provider;
    protected static final int LLDP_PRIORITY = 1000;
    protected static final int NORMAL_PRIORITY = 0;
    protected static final String OPENFLOW_10 = "1.0";
    protected static final String OPENFLOW_13 = "1.3";

    public static ProviderNetworkManager getManager() {
        if (provider != null) return provider;
        String ofVersion = System.getProperty("ovsdb.of.version", OPENFLOW_10);
        switch (ofVersion) {
            case OPENFLOW_13:
                provider = new OF13ProviderManager();
                break;
            case OPENFLOW_10:
            default:
                provider = new OF10ProviderManager();
                break;
        }
        return provider;
    }

    protected String getInternalBridgeUUID (Node node, String bridgeName) {
        try {
            OVSDBConfigService ovsdbTable = (OVSDBConfigService)ServiceHelper.getGlobalInstance(OVSDBConfigService.class, this);
            Map<String, Table<?>> bridgeTable = ovsdbTable.getRows(node, Bridge.NAME.getName());
            if (bridgeTable == null) return null;
            for (String key : bridgeTable.keySet()) {
                Bridge bridge = (Bridge)bridgeTable.get(key);
                if (bridge.getName().equals(bridgeName)) return key;
            }
        } catch (Exception e) {
            logger.error("Error getting Bridge Identifier for {} / {}", node, bridgeName, e);
        }
        return null;
    }

    public abstract boolean hasPerTenantTunneling();
    public abstract Status handleInterfaceUpdate(String tunnelType, String tunnelKey);
    public abstract Status handleInterfaceUpdate(String tunnelType, String tunnelKey, Node source, Interface intf);
    public abstract Status handleInterfaceDelete(String tunnelType, String tunnelKey, Node source, Interface intf, boolean isLastInstanceOnNode);
    /*
     * Initialize the Flow rules given the OVSDB node.
     * This method provides a set of common functionalities to initialize the Flow rules of an OVSDB node
     * that are Openflow Version specific. Hence we have this method in addition to the following
     * Openflow Node specific initialization method.
     */
    public abstract void initializeFlowRules(Node node);

    /*
     * Initialize the Flow rules given the Openflow node
     */
    public abstract void initializeOFFlowRules(Node openflowNode);
}
