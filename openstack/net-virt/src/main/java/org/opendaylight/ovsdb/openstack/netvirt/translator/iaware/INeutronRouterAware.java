/*
 * Copyright (c) 2013, 2015 IBM Corporation and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt.translator.iaware;

import org.opendaylight.ovsdb.openstack.netvirt.translator.NeutronRouter;
import org.opendaylight.ovsdb.openstack.netvirt.translator.NeutronRouter_Interface;

/**
 * This interface defines the methods a service that wishes to be aware of Neutron Routers needs to implement
 *
 */

public interface INeutronRouterAware {

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
    int canCreateRouter(NeutronRouter router);

    /**
     * Services provide this interface method for taking action after a router has been created
     *
     * @param router
     *            instance of new Neutron Router object
     */
    void neutronRouterCreated(NeutronRouter router);

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
    int canUpdateRouter(NeutronRouter delta, NeutronRouter original);

    /**
     * Services provide this interface method for taking action after a router has been updated
     *
     * @param router
     *            instance of modified Neutron Router object
     */
    void neutronRouterUpdated(NeutronRouter router);

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
    int canDeleteRouter(NeutronRouter router);

    /**
     * Services provide this interface method for taking action after a router has been deleted
     *
     * @param router
     *            instance of deleted Router Network object
     */
    void neutronRouterDeleted(NeutronRouter router);

    /**
     * Services provide this interface method to indicate if the
     * specified interface can be attached to the specified router
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
    int canAttachInterface(NeutronRouter router, NeutronRouter_Interface routerInterface);

    /**
     * Services provide this interface method for taking action
     * after an interface has been added to a router
     *
     * @param router
     *            instance of the base Neutron Router object
     * @param routerInterface
     *            instance of the NeutronRouter_Interface being attached to the router
     */
    void neutronRouterInterfaceAttached(NeutronRouter router, NeutronRouter_Interface routerInterface);

    /**
     * Services provide this interface method to indicate if the
     * specified interface can be detached from the specified router
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
    int canDetachInterface(NeutronRouter router, NeutronRouter_Interface routerInterface);

    /**
     * Services provide this interface method for taking action after an interface has been removed from a router
     *
     * @param router
     *            instance of the base Neutron Router object
     * @param routerInterface
     *            instance of the NeutronRouter_Interface being detached from the router
     */
    void neutronRouterInterfaceDetached(NeutronRouter router, NeutronRouter_Interface routerInterface);
}
