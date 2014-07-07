/*
 * Copyright (C) 2013 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Madhu Venugopal, Brent Salisbury, Hsin-Yi Shen
 */
package org.opendaylight.ovsdb.neutron;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

import org.opendaylight.controller.networkconfig.neutron.INeutronNetworkAware;
import org.opendaylight.controller.networkconfig.neutron.INeutronNetworkCRUD;
import org.opendaylight.controller.networkconfig.neutron.NeutronNetwork;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.utils.ServiceHelper;
import org.opendaylight.ovsdb.lib.notation.Row;
import org.opendaylight.ovsdb.plugin.IConnectionServiceInternal;
import org.opendaylight.ovsdb.plugin.OVSDBInventoryListener;
import org.opendaylight.ovsdb.schema.openvswitch.Interface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handle requests for Neutron Network.
 */
public class NetworkHandler extends BaseHandler
                            implements INeutronNetworkAware {

    public static final String NETWORK_TYPE_VXLAN = "vxlan";
    public static final String NETWORK_TYPE_GRE = "gre";
    public static final String NETWORK_TYPE_VLAN = "vlan";

    /**
     * Logger instance.
     */
    static final Logger logger = LoggerFactory.getLogger(NetworkHandler.class);

    // The implementation for each of these services is resolved by the OSGi Service Manager
    private volatile ITenantNetworkManager tenantNetworkManager;
    private volatile IAdminConfigManager adminConfigManager;

    /**
     * Invoked when a network creation is requested
     * to indicate if the specified network can be created.
     *
     * @param network  An instance of proposed new Neutron Network object.
     * @return A HTTP status code to the creation request.
     */
    @Override
    public int canCreateNetwork(NeutronNetwork network) {
        if (network.isShared()) {
            logger.error(" Network shared attribute not supported ");
            return HttpURLConnection.HTTP_NOT_ACCEPTABLE;
        }

        return HttpURLConnection.HTTP_CREATED;
    }

    /**
     * Invoked to take action after a network has been created.
     *
     * @param network  An instance of new Neutron Network object.
     */
    @Override
    public void neutronNetworkCreated(NeutronNetwork network) {
        int result = HttpURLConnection.HTTP_BAD_REQUEST;
        logger.trace("neutronNetworkCreated: network: {}", network);
        result = canCreateNetwork(network);
        if (result != HttpURLConnection.HTTP_CREATED) {
            logger.debug("Network creation failed {} ", result);
            return;
        }

    }

    /**
     * Invoked when a network update is requested
     * to indicate if the specified network can be changed
     * using the specified delta.
     *
     * @param delta     Updates to the network object using patch semantics.
     * @param original  An instance of the Neutron Network object
     *                  to be updated.
     * @return A HTTP status code to the update request.
     */
    @Override
    public int canUpdateNetwork(NeutronNetwork delta,
                                NeutronNetwork original) {
        logger.trace("canUpdateNetwork: network delta {} --- original {}", delta, original);
        return HttpURLConnection.HTTP_OK;
    }

    /**
     * Invoked to take action after a network has been updated.
     *
     * @param network An instance of modified Neutron Network object.
     */
    @Override
    public void neutronNetworkUpdated(NeutronNetwork network) {
        logger.trace("neutronNetworkUpdated: network: {}", network);
        return;
    }

    /**
     * Invoked when a network deletion is requested
     * to indicate if the specified network can be deleted.
     *
     * @param network  An instance of the Neutron Network object to be deleted.
     * @return A HTTP status code to the deletion request.
     */
    @Override
    public int canDeleteNetwork(NeutronNetwork network) {
        return HttpURLConnection.HTTP_OK;
    }

    /**
     * Invoked to take action after a network has been deleted.
     *
     * @param network  An instance of deleted Neutron Network object.
     */
    @Override
    public void neutronNetworkDeleted(NeutronNetwork network) {

        int result = canDeleteNetwork(network);
        logger.trace("canDeleteNetwork: network: {}", network);
        if  (result != HttpURLConnection.HTTP_OK) {
            logger.error(" deleteNetwork validation failed for result - {} ",
                    result);
            return;
        }
        /* Is this the last Neutron tenant network */
        INeutronNetworkCRUD neutronNetworkService = (INeutronNetworkCRUD)ServiceHelper.getGlobalInstance(INeutronNetworkCRUD.class, this);
        List <NeutronNetwork> networks = new ArrayList<NeutronNetwork>();
        if (neutronNetworkService != null) {
            networks = neutronNetworkService.getAllNetworks();
            OVSDBInventoryListener inventoryListener = (OVSDBInventoryListener)ServiceHelper.getGlobalInstance(OVSDBInventoryListener.class, this);
            if (networks.isEmpty()) {
                logger.trace("neutronNetworkDeleted: last tenant network, delete tunnel ports...");
                IConnectionServiceInternal connectionService = (IConnectionServiceInternal)
                                        ServiceHelper.getGlobalInstance(IConnectionServiceInternal.class, this);
                List<Node> nodes = connectionService.getNodes();

                for (Node node : nodes) {
                    List<String> phyIfName = adminConfigManager.getAllPhysicalInterfaceNames(node);
                    try {
                        ConcurrentMap<String, Row> interfaces = this.ovsdbConfigService.getRows(node, ovsdbConfigService.getTableName(node, Interface.class));
                        if (interfaces != null) {
                            for (String intfUUID : interfaces.keySet()) {
                                Interface intf = ovsdbConfigService.getTypedRow(node, Interface.class, interfaces.get(intfUUID));
                                String intfType = intf.getTypeColumn().getData();
                                if (intfType.equalsIgnoreCase(NetworkHandler.NETWORK_TYPE_VXLAN) || intfType.equalsIgnoreCase(NetworkHandler.NETWORK_TYPE_GRE)) {
                                    /* delete tunnel ports on this node */
                                    logger.trace("Delete tunnel intf {}", intf);
                                    inventoryListener.rowRemoved(node, intf.getSchema().getName(), intfUUID,
                                            intf.getRow(), null);
                                } else if (!phyIfName.isEmpty() && phyIfName.contains(intf.getName())) {
                                    logger.trace("Delete physical intf {}", intf);
                                    inventoryListener.rowRemoved(node, intf.getSchema().getName(), intfUUID,
                                            intf.getRow(), null);
                                }
                            }
                        }
                    } catch (Exception e) {
                        logger.error("Exception during handlingNeutron network delete");
                    }
                }
            }
        }
        tenantNetworkManager.networkDeleted(network.getID());
    }
}
