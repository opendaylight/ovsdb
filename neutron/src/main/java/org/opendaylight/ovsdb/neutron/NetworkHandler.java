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
import org.opendaylight.ovsdb.lib.table.Interface;
import org.opendaylight.ovsdb.lib.table.internal.Table;
import org.opendaylight.ovsdb.plugin.IConnectionServiceInternal;
import org.opendaylight.ovsdb.plugin.OVSDBInventoryListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handle requests for Neutron Network.
 */
public class NetworkHandler extends BaseHandler
                            implements INeutronNetworkAware {
    /**
     * Logger instance.
     */
    static final Logger logger = LoggerFactory.getLogger(NetworkHandler.class);

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
        return HttpURLConnection.HTTP_OK;
    }

    /**
     * Invoked to take action after a network has been updated.
     *
     * @param network An instance of modified Neutron Network object.
     */
    @Override
    public void neutronNetworkUpdated(NeutronNetwork network) {
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
                    try {
                        ConcurrentMap<String, Table<?>> interfaces = this.ovsdbConfigService.getRows(node, Interface.NAME.getName());
                        if (interfaces != null) {
                            for (String intfUUID : interfaces.keySet()) {
                                Interface intf = (Interface) interfaces.get(intfUUID);
                                String intfType = intf.getType();
                                if (intfType.equalsIgnoreCase("vxlan") || intfType.equalsIgnoreCase("gre")) {
                                    /* delete tunnel ports on this node */
                                    logger.trace("Delete tunnel intf {}", intf);
                                    inventoryListener.rowRemoved(node, Interface.NAME.getName(), intfUUID,
                                                                 intf, null);
                                }
                            }
                        }
                    } catch (Exception e) {
                        logger.error("Exception during handlingNeutron network delete");
                    }
                }
            }
        }
        TenantNetworkManager.getManager().networkDeleted(network.getID());
    }
}
