package org.opendaylight.ovsdb.openstack.netvirt;

import org.opendaylight.controller.networkconfig.neutron.INeutronRouterAware;
import org.opendaylight.controller.networkconfig.neutron.NeutronRouter;
import org.opendaylight.controller.networkconfig.neutron.NeutronRouter_Interface;
import org.opendaylight.ovsdb.plugin.api.OvsdbConfigurationService;
import org.opendaylight.ovsdb.plugin.api.OvsdbConnectionService;
import org.opendaylight.ovsdb.plugin.api.OvsdbInventoryListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.HttpURLConnection;

/**
 * Handle requests for Neutron Router.
 */
public class RouterHandler extends AbstractHandler
        implements INeutronRouterAware {

    /**
     * Logger instance.
     */
    static final Logger logger = LoggerFactory.getLogger(RouterHandler.class);

    private volatile OvsdbConfigurationService ovsdbConfigurationService;
    private volatile OvsdbConnectionService connectionService;
    private volatile OvsdbInventoryListener ovsdbInventoryListener;

    /**
     * Services provide this interface method to indicate if the specified router can be created
     *
     * @param router
     *            instance of proposed new Neutron Router object
     * @return integer
     *            the return value is understood to be a HTTP status code.  A return value outside of 200 through 299
     *            results in the create operation being interrupted and the returned status value reflected in the
     *            HTTP response.
     */
    @Override
    public int canCreateRouter(NeutronRouter router) { return HttpURLConnection.HTTP_OK; }

    /**
     * Services provide this interface method for taking action after a router has been created
     *
     * @param router
     *            instance of new Neutron Router object
     */
    @Override
    public void neutronRouterCreated(NeutronRouter router) {
        logger.debug(" Router created {}, uuid {}", router.getName(), router.getRouterUUID());
    }

    /**
     * Services provide this interface method to indicate if the specified router can be changed using the specified
     * delta
     *
     * @param delta
     *            updates to the router object using patch semantics
     * @param router
     *            instance of the Neutron Router object to be updated
     * @return integer
     *            the return value is understood to be a HTTP status code.  A return value outside of 200 through 299
     *            results in the update operation being interrupted and the returned status value reflected in the
     *            HTTP response.
     */
    @Override
    public int canUpdateRouter(NeutronRouter delta, NeutronRouter original) { return HttpURLConnection.HTTP_OK; }

    /**
     * Services provide this interface method for taking action after a router has been updated
     *
     * @param router
     *            instance of modified Neutron Router object
     */
    @Override
    public void neutronRouterUpdated(NeutronRouter router) {
        logger.debug(" Router updated {}", router.getName());
    }

    /**
     * Services provide this interface method to indicate if the specified router can be deleted
     *
     * @param router
     *            instance of the Neutron Router object to be deleted
     * @return integer
     *            the return value is understood to be a HTTP status code.  A return value outside of 200 through 299
     *            results in the delete operation being interrupted and the returned status value reflected in the
     *            HTTP response.
     */
    @Override
    public int canDeleteRouter(NeutronRouter router) { return HttpURLConnection.HTTP_OK; }

    /**
     * Services provide this interface method for taking action after a router has been deleted
     *
     * @param router
     *            instance of deleted Router Network object
     */
    @Override
    public void neutronRouterDeleted(NeutronRouter router) {
        logger.debug(" Router deleted {}, uuid {}", router.getName(), router.getRouterUUID());
    }

    /**
     * Services provide this interface method to indicate if the specified interface can be attached to the specified
     * route
     *
     * @param router
     *            instance of the base Neutron Router object
     * @param routerInterface
     *            instance of the NeutronRouter_Interface to be attached to the router
     * @return integer
     *            the return value is understood to be a HTTP status code.  A return value outside of 200 through 299
     *            results in the attach operation being interrupted and the returned status value reflected in the
     *            HTTP response.
     */
    @Override
    public int canAttachInterface(NeutronRouter router, NeutronRouter_Interface routerInterface) {
        logger.debug(" Router {} asked if it can attach interface {}. Subnet {}",
                     router.getName(),
                     routerInterface.getPortUUID(),
                     routerInterface.getSubnetUUID());
        return HttpURLConnection.HTTP_OK;
    }

    /**
     * Services provide this interface method for taking action after an interface has been added to a router
     *
     * @param router
     *            instance of the base Neutron Router object
     * @param routerInterface
     *            instance of the NeutronRouter_Interface being attached to the router
     */
    @Override
    public void neutronRouterInterfaceAttached(NeutronRouter router, NeutronRouter_Interface routerInterface) {
        logger.debug(" Router {} interface {} attached. Subnet {}", router.getName(), routerInterface.getPortUUID(),
                     routerInterface.getSubnetUUID());
    }

    /**
     * Services provide this interface method to indicate if the specified interface can be detached from the specified
     * router
     *
     * @param router
     *            instance of the base Neutron Router object
     * @param routerInterface
     *            instance of the NeutronRouter_Interface to be detached to the router
     * @return integer
     *            the return value is understood to be a HTTP status code.  A return value outside of 200 through 299
     *            results in the detach operation being interrupted and the returned status value reflected in the
     *            HTTP response.
     */
    @Override
    public int canDetachInterface(NeutronRouter router, NeutronRouter_Interface routerInterface) {
        logger.debug(" Router {} asked if it can detach interface {}. Subnet {}",
                     router.getName(),
                     routerInterface.getPortUUID(),
                     routerInterface.getSubnetUUID());

        return HttpURLConnection.HTTP_OK;
    }

    /**
     * Services provide this interface method for taking action after an interface has been removed from a router
     *
     * @param router
     *            instance of the base Neutron Router object
     * @param routerInterface
     *            instance of the NeutronRouter_Interface being detached from the router
     */
    @Override
    public void neutronRouterInterfaceDetached(NeutronRouter router, NeutronRouter_Interface routerInterface) {
        logger.debug(" Router {} interface {} detached. Subnet {}", router.getName(), routerInterface.getPortUUID(),
                     routerInterface.getSubnetUUID());
    }
}
