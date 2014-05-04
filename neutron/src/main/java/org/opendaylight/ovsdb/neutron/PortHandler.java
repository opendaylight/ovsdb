/*
 * Copyright (C) 2013 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Madhu Venugopal, Brent Salisbury
 */
package org.opendaylight.ovsdb.neutron;

import java.net.HttpURLConnection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import org.opendaylight.controller.networkconfig.neutron.INeutronNetworkCRUD;
import org.opendaylight.controller.networkconfig.neutron.INeutronPortAware;
import org.opendaylight.controller.networkconfig.neutron.NeutronNetwork;
import org.opendaylight.controller.networkconfig.neutron.NeutronPort;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.utils.ServiceHelper;
import org.opendaylight.ovsdb.lib.table.Interface;
import org.opendaylight.ovsdb.lib.table.internal.Table;
import org.opendaylight.ovsdb.plugin.IConnectionServiceInternal;
import org.opendaylight.ovsdb.plugin.OVSDBInventoryListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handle requests for Neutron Port.
 */
public class PortHandler extends BaseHandler
                         implements INeutronPortAware {

    /**
     * Logger instance.
     */
    static final Logger logger = LoggerFactory.getLogger(PortHandler.class);

    /**
     * Invoked when a port creation is requested
     * to indicate if the specified port can be created.
     *
     * @param port     An instance of proposed new Port object.
     * @return A HTTP status code to the creation request.
     */
    @Override
    public int canCreatePort(NeutronPort port) {
        return HttpURLConnection.HTTP_CREATED;
    }

    /**
     * Invoked to take action after a port has been created.
     *
     * @param port An instance of new Neutron Port object.
     */
    @Override
    public void neutronPortCreated(NeutronPort port) {
        int result = canCreatePort(port);
        if (result != HttpURLConnection.HTTP_CREATED) {
            logger.error(" Port create validation failed result - {} ", result);
            return;
        }

        logger.debug(" Port-ADD successful for tenant-id - {}," +
                     " network-id - {}, port-id - {}, result - {} ",
                     port.getTenantID(), port.getNetworkUUID(),
                     port.getID(), result);
        //logger.trace("shague: neutronPortCreated {}", port);
    }

    /**
     * Invoked when a port update is requested
     * to indicate if the specified port can be changed
     * using the specified delta.
     *
     * @param delta    Updates to the port object using patch semantics.
     * @param original An instance of the Neutron Port object
     *                  to be updated.
     * @return A HTTP status code to the update request.
     */
    @Override
    public int canUpdatePort(NeutronPort delta,
                             NeutronPort original) {
        int result = HttpURLConnection.HTTP_OK;
        /**
         * To basic validation of the request
         */

        if ((original == null) || (delta == null)) {
            logger.error("port object not specified");
            return HttpURLConnection.HTTP_BAD_REQUEST;
        }
        logger.trace("shague: canUpdatePort delta: {}, original: {}", delta, original);
        return result;
    }

    /**
     * Invoked to take action after a port has been updated.
     *
     * @param port An instance of modified Neutron Port object.
     */
    @Override
    public void neutronPortUpdated(NeutronPort port) {
        logger.trace("shague: neutronPortUpdated {}", port);
    }

    /**
     * Invoked when a port deletion is requested
     * to indicate if the specified port can be deleted.
     *
     * @param port     An instance of the Neutron Port object to be deleted.
     * @return A HTTP status code to the deletion request.
     */
    @Override
    public int canDeletePort(NeutronPort port) {
        return HttpURLConnection.HTTP_OK;
    }

    /**
     * Invoked to take action after a port has been deleted.
     *
     * @param port  An instance of deleted Neutron Port object.
     */
    @Override
    public void neutronPortDeleted(NeutronPort port) {

        int result = canDeletePort(port);
        if  (result != HttpURLConnection.HTTP_OK) {
            logger.error(" deletePort validation failed - result {} ", result);
            return;
        }

        logger.trace("hshen: : neutronPortDeleted: port {}", port);
        IConnectionServiceInternal connectionService = (IConnectionServiceInternal)ServiceHelper.getGlobalInstance(IConnectionServiceInternal.class, this);
        INeutronNetworkCRUD neutronNetworkService = (INeutronNetworkCRUD)ServiceHelper.getGlobalInstance(INeutronNetworkCRUD.class, this);
        NeutronNetwork neutronNetwork = neutronNetworkService.getNetwork(port.getNetworkUUID());
        OVSDBInventoryListener inventoryListener = (OVSDBInventoryListener)ServiceHelper.getGlobalInstance(OVSDBInventoryListener.class, this);
        List<Node> nodes = connectionService.getNodes();
        for (Node node : nodes) {
            logger.trace("hshen: : neutronPortDeleted check node {}", node);
            try {
                ConcurrentMap<String, Table<?>> interfaces = this.ovsdbConfigService.getRows(node, Interface.NAME.getName());
                if (interfaces != null) {
                    for (String intfUUID : interfaces.keySet()) {
                        Interface intf = (Interface) interfaces.get(intfUUID);
                        Map<String, String> externalIds = intf.getExternal_ids();
                        if (externalIds == null) {
                            logger.trace("No external_ids seen in {}", intf);
                            continue;
                        }
                        /* Compare Neutron port uuid */
                        String neutronPortId = externalIds.get(TenantNetworkManager.EXTERNAL_ID_INTERFACE_ID);
                        if (neutronPortId == null) {
                            continue;
                        }
                        logger.trace("hshen: neutronPortDeleted check intf {}", intf);
                        if (neutronPortId.equalsIgnoreCase(port.getPortUUID())) {
                            logger.trace("hshen : neutronPortDeleted: Delete interface {}", intf.getName());
                            inventoryListener.rowRemoved(node, Interface.NAME.getName(), intfUUID,
                                                         intf, neutronNetwork);
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                logger.error("Exception during handlingNeutron network delete");
            }
        }
        logger.debug(" PORT delete successful for tenant-id - {}, " +
                     " network-id - {}, port-id - {}", port.getTenantID(),
                     port.getNetworkUUID(),port.getID());

    }
}
