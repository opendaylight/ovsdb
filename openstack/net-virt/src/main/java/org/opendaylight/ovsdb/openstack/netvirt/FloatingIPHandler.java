/*
 * Copyright (c) 2014, 2015 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt;

import java.net.HttpURLConnection;

import org.opendaylight.ovsdb.openstack.netvirt.translator.INeutronFloatingIPAware;
import org.opendaylight.ovsdb.openstack.netvirt.translator.NeutronFloatingIP;
import org.opendaylight.ovsdb.openstack.netvirt.api.Action;
import org.opendaylight.ovsdb.openstack.netvirt.api.EventDispatcher;
import org.opendaylight.ovsdb.openstack.netvirt.impl.NeutronL3Adapter;
import org.opendaylight.ovsdb.utils.servicehelper.ServiceHelper;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handle requests for Neutron Floating IP.
 */
public class FloatingIPHandler extends AbstractHandler
        implements INeutronFloatingIPAware, ConfigInterface {

    private static final Logger LOG = LoggerFactory.getLogger(FloatingIPHandler.class);

    // The implementation for each of these services is resolved by the OSGi Service Manager
    private volatile NeutronL3Adapter neutronL3Adapter;

    /**
     * Services provide this interface method to indicate if the specified floatingIP can be created
     *
     * @param floatingIP
     *            instance of proposed new Neutron FloatingIP object
     * @return integer
     *            the return value is understood to be a HTTP status code.  A return value outside of 200 through 299
     *            results in the create operation being interrupted and the returned status value reflected in the
     *            HTTP response.
     */
    @Override
    public int canCreateFloatingIP(NeutronFloatingIP floatingIP) {
        return HttpURLConnection.HTTP_OK;
    }


    /**
     * Services provide this interface method for taking action after a floatingIP has been created
     *
     * @param floatingIP
     *            instance of new Neutron FloatingIP object
     */
    @Override
    public void neutronFloatingIPCreated(NeutronFloatingIP floatingIP) {
        enqueueEvent(new NorthboundEvent(floatingIP, Action.ADD));
    }

    /**
     * Services provide this interface method to indicate if the specified floatingIP can be changed using the specified
     * delta
     *
     * @param delta
     *            updates to the floatingIP object using patch semantics
     * @param original
     *            instance of the Neutron FloatingIP object to be updated
     * @return integer
     *            the return value is understood to be a HTTP status code.  A return value outside of 200 through 299
     *            results in the update operation being interrupted and the returned status value reflected in the
     *            HTTP response.
     */
    @Override
    public int canUpdateFloatingIP(NeutronFloatingIP delta, NeutronFloatingIP original) {
        return HttpURLConnection.HTTP_OK;
    }

    /**
     * Services provide this interface method for taking action after a floatingIP has been updated
     *
     * @param floatingIP
     *            instance of modified Neutron FloatingIP object
     */
    @Override
    public void neutronFloatingIPUpdated(NeutronFloatingIP floatingIP) {
        enqueueEvent(new NorthboundEvent(floatingIP, Action.UPDATE));
    }

    /**
     * Services provide this interface method to indicate if the specified floatingIP can be deleted
     *
     * @param floatingIP
     *            instance of the Neutron FloatingIP object to be deleted
     * @return integer
     *            the return value is understood to be a HTTP status code.  A return value outside of 200 through 299
     *            results in the delete operation being interrupted and the returned status value reflected in the
     *            HTTP response.
     */
    @Override
    public int canDeleteFloatingIP(NeutronFloatingIP floatingIP) {
        return HttpURLConnection.HTTP_OK;
    }

    /**
     * Services provide this interface method for taking action after a floatingIP has been deleted
     *
     * @param floatingIP
     *            instance of deleted Neutron FloatingIP object
     */
    @Override
    public void neutronFloatingIPDeleted(NeutronFloatingIP floatingIP) {
        enqueueEvent(new NorthboundEvent(floatingIP, Action.DELETE));
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
                neutronL3Adapter.handleNeutronFloatingIPEvent(ev.getNeutronFloatingIP(), ev.getAction());
                break;
            default:
                LOG.warn("Unable to process event action {}", ev.getAction());
                break;
        }
    }

    @Override
    public void setDependencies(BundleContext bundleContext, ServiceReference serviceReference) {
        eventDispatcher =
                (EventDispatcher) ServiceHelper.getGlobalInstance(EventDispatcher.class, this);
        eventDispatcher.eventHandlerAdded(
                bundleContext.getServiceReference(INeutronFloatingIPAware.class.getName()), this);
        neutronL3Adapter =
                (NeutronL3Adapter) ServiceHelper.getGlobalInstance(NeutronL3Adapter.class, this);
    }

    @Override
    public void setDependencies(Object impl) {}
}
