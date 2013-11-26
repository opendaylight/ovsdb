package org.opendaylight.ovsdb.neutron;

import java.net.HttpURLConnection;

import org.opendaylight.controller.networkconfig.neutron.INeutronPortAware;
import org.opendaylight.controller.networkconfig.neutron.NeutronPort;
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
     * @param port     An instance of proposed new Port Port object.
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

        String tenantID = convertNeutronIDToKey(port.getTenantID());
        String networkID = convertNeutronIDToKey(port.getNetworkUUID());
        String portID = convertNeutronIDToKey(port.getID());
        String portDesc = port.getName();
        Boolean portAdminState = port.getAdminStateUp();

        // Create Full Mesh Tunnels
        /*
         * Is this required ?
         * The Tunnel Creation logic is completely owned by the Southbound handler at this point.

        NeutronNetwork network = this.neutronNetworkCache.getNetwork(port.getNetworkUUID());
        ProviderNetworkManager.getManager().createTunnels(network.getProviderNetworkType(),
                                                          network.getProviderSegmentationID());
         */
        logger.debug(" Port-ADD successful for tenant-id - {}," +
                " network-id - {}, port-id - {}, result - {} ",
                tenantID, networkID, portID, result);
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
        int result = HttpURLConnection.HTTP_OK;
        return result;
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

        String tenantID = convertNeutronIDToKey(port.getTenantID());
        String networkID = convertNeutronIDToKey(port.getNetworkUUID());
        String portID = convertNeutronIDToKey(port.getID());
        logger.debug(" PORT delete successful for tenant-id - {}, " +
                     " network-id - {}, port-id - {}", tenantID, networkID, portID);

    }
}
