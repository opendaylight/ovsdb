/*
 * Copyright (C) 2013 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Madhu Venugopal, Brent Salisbury, Hsin-Yi Shen
 */
package org.opendaylight.ovsdb.openstack.netvirt;

import org.opendaylight.controller.networkconfig.neutron.INeutronNetworkAware;
import org.opendaylight.controller.networkconfig.neutron.INeutronNetworkCRUD;
import org.opendaylight.controller.networkconfig.neutron.NeutronNetwork;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.ovsdb.lib.notation.Row;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.openstack.netvirt.api.BridgeConfigurationManager;
import org.opendaylight.ovsdb.openstack.netvirt.api.TenantNetworkManager;
import org.opendaylight.ovsdb.plugin.api.OvsdbConfigurationService;
import org.opendaylight.ovsdb.plugin.api.OvsdbConnectionService;
import org.opendaylight.ovsdb.plugin.api.OvsdbInventoryListener;
import org.opendaylight.ovsdb.schema.openvswitch.Interface;
import org.opendaylight.ovsdb.schema.openvswitch.Port;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.HttpURLConnection;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

/**
 * Handle requests for Neutron Network.
 */
public class NetworkHandler extends AbstractHandler
                            implements INeutronNetworkAware {

    public static final String NETWORK_TYPE_VXLAN = "vxlan";
    public static final String NETWORK_TYPE_GRE = "gre";
    public static final String NETWORK_TYPE_VLAN = "vlan";

    /**
     * Logger instance.
     */
    static final Logger logger = LoggerFactory.getLogger(NetworkHandler.class);

    // The implementation for each of these services is resolved by the OSGi Service Manager
    private volatile TenantNetworkManager tenantNetworkManager;
    private volatile BridgeConfigurationManager bridgeConfigurationManager;
    private volatile org.opendaylight.ovsdb.openstack.netvirt.api.ConfigurationService configurationService;
    private volatile OvsdbConfigurationService ovsdbConfigurationService;
    private volatile OvsdbConnectionService connectionService;
    private volatile INeutronNetworkCRUD neutronNetworkCache;
    private volatile OvsdbInventoryListener ovsdbInventoryListener;

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
        List <NeutronNetwork> networks;
        if (neutronNetworkCache != null) {
            networks = neutronNetworkCache.getAllNetworks();
            if (networks.isEmpty()) {
                logger.trace("neutronNetworkDeleted: last tenant network, delete tunnel ports...");
                List<Node> nodes = connectionService.getNodes();

                for (Node node : nodes) {
                    List<String> phyIfName = bridgeConfigurationManager.getAllPhysicalInterfaceNames(node);
                    try {
                        ConcurrentMap<String, Row> ports =
                                this.ovsdbConfigurationService.getRows(node,
                                                                ovsdbConfigurationService.getTableName(node, Port.class));
                        if (ports != null) {
                            for (Row portRow : ports.values()) {
                                Port port = ovsdbConfigurationService.getTypedRow(node, Port.class, portRow);
                                for (UUID interfaceUuid : port.getInterfacesColumn().getData()) {
                                    Interface interfaceRow = (Interface) ovsdbConfigurationService
                                            .getRow(node,
                                                    ovsdbConfigurationService.getTableName(node, Interface.class),
                                                    interfaceUuid.toString());

                                    String interfaceType = interfaceRow.getTypeColumn().getData();
                                    if (interfaceType.equalsIgnoreCase(NetworkHandler.NETWORK_TYPE_VXLAN)
                                        || interfaceType.equalsIgnoreCase(
                                            NetworkHandler.NETWORK_TYPE_GRE)) {
                                        /* delete tunnel ports on this node */
                                        logger.trace("Delete tunnel interface {}", interfaceRow);
                                        ovsdbConfigurationService.deleteRow(node,
                                                                     ovsdbConfigurationService.getTableName(node, Port.class),
                                                                     port.getUuid().toString());
                                        break;
                                    } else if (!phyIfName.isEmpty() && phyIfName.contains(interfaceRow.getName())) {
                                        logger.trace("Delete physical interface {}", interfaceRow);
                                        ovsdbConfigurationService.deleteRow(node,
                                                                     ovsdbConfigurationService.getTableName(node, Port.class),
                                                                     port.getUuid().toString());
                                        break;
                                    }
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
