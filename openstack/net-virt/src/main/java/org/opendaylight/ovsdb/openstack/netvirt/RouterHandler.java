/*
 * Copyright (c) 2014, 2015 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt;

import java.net.HttpURLConnection;

import org.opendaylight.neutron.spi.INeutronRouterAware;
import org.opendaylight.neutron.spi.NeutronRouter;
import org.opendaylight.neutron.spi.NeutronRouter_Interface;
import org.opendaylight.ovsdb.openstack.netvirt.api.Action;
import org.opendaylight.ovsdb.openstack.netvirt.api.EventDispatcher;
import org.opendaylight.ovsdb.openstack.netvirt.impl.NeutronL3Adapter;
import org.opendaylight.ovsdb.utils.servicehelper.ServiceHelper;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handle requests for Neutron Router.
 */
public class RouterHandler extends AbstractHandler implements INeutronRouterAware, ConfigInterface {
    private static final Logger LOG = LoggerFactory.getLogger(RouterHandler.class);

    // The implementation for each of these services is resolved by the OSGi Service Manager
    private volatile NeutronL3Adapter neutronL3Adapter;

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
    public int canCreateRouter(NeutronRouter router) {
        return HttpURLConnection.HTTP_OK;
    }

    /**
     * Services provide this interface method for taking action after a router has been created
     *
     * @param router
     *            instance of new Neutron Router object
     */
    @Override
    public void neutronRouterCreated(NeutronRouter router) {
        enqueueEvent(new NorthboundEvent(router, Action.ADD));
    }

    /**
     * Services provide this interface method to indicate if the specified router can be changed using the specified
     * delta
     *
     * @param delta
     *            updates to the router object using patch semantics
     * @param original
     *            instance of the Neutron Router object to be updated
     * @return integer
     *            the return value is understood to be a HTTP status code.  A return value outside of 200 through 299
     *            results in the update operation being interrupted and the returned status value reflected in the
     *            HTTP response.
     */
    @Override
    public int canUpdateRouter(NeutronRouter delta, NeutronRouter original) {
        return HttpURLConnection.HTTP_OK;
    }

    /**
     * Services provide this interface method for taking action after a router has been updated
     *
     * @param router
     *            instance of modified Neutron Router object
     */
    @Override
    public void neutronRouterUpdated(NeutronRouter router) {
        enqueueEvent(new NorthboundEvent(router, Action.UPDATE));
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
    public int canDeleteRouter(NeutronRouter router) {
        return HttpURLConnection.HTTP_OK;
    }

    /**
     * Services provide this interface method for taking action after a router has been deleted
     *
     * @param router
     *            instance of deleted Router Network object
     */
    @Override
    public void neutronRouterDeleted(NeutronRouter router) {
        enqueueEvent(new NorthboundEvent(router, Action.DELETE));
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
        LOG.debug(" Router {} asked if it can attach interface {}. Subnet {}",
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
        enqueueEvent(new NorthboundEvent(router, routerInterface, Action.ADD));
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
        LOG.debug(" Router {} asked if it can detach interface {}. Subnet {}",
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
        enqueueEvent(new NorthboundEvent(router, routerInterface, Action.DELETE));
    }

    /**
     * Process the event.
     *
     * @param abstractEvent the {@link org.opendaylight.ovsdb.openstack.netvirt.AbstractEvent} event to be handled.
     * @see org.opendaylight.ovsdb.openstack.netvirt.api.EventDispatcher
     */
    @Override
    public void processEvent(AbstractEvent abstractEvent) {
        if (!(abstractEvent instanceof NorthboundEvent)) {
            LOG.error("Unable to process abstract event {}", abstractEvent);
            return;
        }
        NorthboundEvent ev = (NorthboundEvent) abstractEvent;
        switch (ev.getAction()) {
            case ADD:
                // fall through
            case DELETE:
                // fall through
            case UPDATE:
                if (ev.getRouterInterface() == null) {
                    neutronL3Adapter.handleNeutronRouterEvent(ev.getRouter(), ev.getAction());
                } else {
                    neutronL3Adapter.handleNeutronRouterInterfaceEvent(ev.getRouter(),
                                                                      ev.getRouterInterface(),
                                                                      ev.getAction());
                }
                break;
            default:
                LOG.warn("Unable to process event action {}", ev.getAction());
                break;
        }
    }

    @Override
    public void setDependencies(BundleContext bundleContext, ServiceReference serviceReference) {
        neutronL3Adapter =
                (NeutronL3Adapter) ServiceHelper.getGlobalInstance(NeutronL3Adapter.class, this);
        eventDispatcher =
                (EventDispatcher) ServiceHelper.getGlobalInstance(EventDispatcher.class, this);
        eventDispatcher.eventHandlerAdded(
                bundleContext.getServiceReference(INeutronRouterAware.class.getName()), this);
    }

    @Override
    public void setDependencies(Object impl) {}
}
