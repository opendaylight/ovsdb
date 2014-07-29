/*
 * Copyright (C) 2013 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Madhu Venugopal, Brent Salisbury
 */
package org.opendaylight.ovsdb.openstack.netvirt;

import org.opendaylight.controller.networkconfig.neutron.INeutronPortAware;
import org.opendaylight.controller.networkconfig.neutron.NeutronPort;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.ovsdb.lib.notation.Row;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.openstack.netvirt.api.Constants;
import org.opendaylight.ovsdb.plugin.IConnectionServiceInternal;
import org.opendaylight.ovsdb.plugin.OvsdbConfigService;
import org.opendaylight.ovsdb.plugin.OvsdbInventoryListener;
import org.opendaylight.ovsdb.schema.openvswitch.Interface;
import org.opendaylight.ovsdb.schema.openvswitch.Port;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.HttpURLConnection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

/**
 * Handle requests for Neutron Port.
 */
public class PortHandler extends AbstractHandler
                         implements INeutronPortAware {

    /**
     * Logger instance.
     */
    static final Logger logger = LoggerFactory.getLogger(PortHandler.class);

    private volatile OvsdbConfigService ovsdbConfigService;
    private volatile IConnectionServiceInternal connectionService;
    private volatile OvsdbInventoryListener ovsdbInventoryListener;

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
        return result;
    }

    /**
     * Invoked to take action after a port has been updated.
     *
     * @param port An instance of modified Neutron Port object.
     */
    @Override
    public void neutronPortUpdated(NeutronPort port) {
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
     * @param neutronPort  An instance of deleted Neutron Port object.
     */
    @Override
    public void neutronPortDeleted(NeutronPort neutronPort) {

        int result = canDeletePort(neutronPort);
        if  (result != HttpURLConnection.HTTP_OK) {
            logger.error(" deletePort validation failed - result {} ", result);
            return;
        }

        List<Node> nodes = connectionService.getNodes();
        for (Node node : nodes) {
            try {
                ConcurrentMap<String, Row> portRows =
                        this.ovsdbConfigService.getRows(node,
                                                        ovsdbConfigService.getTableName(node, Port.class));
                if (portRows != null) {
                    for (Row portRow : portRows.values()) {
                        Port port = ovsdbConfigService.getTypedRow(node, Port.class, portRow);
                        for (UUID interfaceUuid : port.getInterfacesColumn().getData()) {
                            Interface interfaceRow = (Interface) ovsdbConfigService
                                    .getRow(node,
                                            ovsdbConfigService.getTableName(node, Interface.class),
                                            interfaceUuid.toString());

                            Map<String, String> externalIds = interfaceRow.getExternalIdsColumn().getData();

                            if (externalIds == null) {
                                logger.trace("No external_ids seen in {}", interfaceRow);
                                continue;
                            }

                            /* Compare Neutron port uuid */
                            String neutronPortId = externalIds.get(Constants.EXTERNAL_ID_INTERFACE_ID);
                            if (neutronPortId == null) {
                                continue;
                            }

                            if (neutronPortId.equalsIgnoreCase(neutronPort.getPortUUID())) {
                                logger.trace("neutronPortDeleted: Delete interface {}", interfaceRow.getName());
                                ovsdbConfigService.deleteRow(node,
                                                             ovsdbConfigService.getTableName(node, Port.class),
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
        logger.debug(" PORT delete successful for tenant-id - {}, " +
                     " network-id - {}, port-id - {}",
                     neutronPort.getTenantID(), neutronPort.getNetworkUUID(),
                     neutronPort.getID());

    }
}
