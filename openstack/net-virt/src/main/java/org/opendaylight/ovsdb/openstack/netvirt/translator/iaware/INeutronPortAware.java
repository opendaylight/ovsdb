/*
 * Copyright (c) 2013, 2015 IBM Corporation and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt.translator.iaware;

import org.opendaylight.ovsdb.openstack.netvirt.translator.NeutronPort;

/**
 * This interface defines the methods a service that wishes to be aware of Neutron Ports needs to implement
 *
 */

public interface INeutronPortAware {

    /**
     * Services provide this interface method to indicate if the specified port can be created
     *
     * @param port
     *            instance of proposed new Neutron Port object
     * @return integer
     *            the return value is understood to be a HTTP status code.  A return value outside of 200 through 299
     *            results in the create operation being interrupted and the returned status value reflected in the
     *            HTTP response.
     */
    int canCreatePort(NeutronPort port);

    /**
     * Services provide this interface method for taking action after a port has been created
     *
     * @param port
     *            instance of new Neutron Port object
     */
    void neutronPortCreated(NeutronPort port);

    /**
     * Services provide this interface method to indicate if the specified port can be changed using the specified
     * delta
     *
     * @param delta
     *            updates to the port object using patch semantics
     * @param original
     *            instance of the Neutron Port object to be updated
     * @return integer
     *            the return value is understood to be a HTTP status code.  A return value outside of 200 through 299
     *            results in the update operation being interrupted and the returned status value reflected in the
     *            HTTP response.
     */
    int canUpdatePort(NeutronPort delta, NeutronPort original);

    /**
     * Services provide this interface method for taking action after a port has been updated
     *
     * @param port
     *            instance of modified Neutron Port object
     */
    void neutronPortUpdated(NeutronPort port);

    /**
     * Services provide this interface method to indicate if the specified port can be deleted
     *
     * @param port
     *            instance of the Neutron Port object to be deleted
     * @return integer
     *            the return value is understood to be a HTTP status code.  A return value outside of 200 through 299
     *            results in the delete operation being interrupted and the returned status value reflected in the
     *            HTTP response.
     */
    int canDeletePort(NeutronPort port);

    /**
     * Services provide this interface method for taking action after a port has been deleted
     *
     * @param port
     *            instance of deleted Port Network object
     */
    void neutronPortDeleted(NeutronPort port);
}
