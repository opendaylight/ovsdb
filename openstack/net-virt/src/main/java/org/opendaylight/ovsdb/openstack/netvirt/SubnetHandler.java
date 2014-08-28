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

import org.opendaylight.controller.networkconfig.neutron.INeutronSubnetAware;
import org.opendaylight.controller.networkconfig.neutron.NeutronSubnet;
import org.opendaylight.ovsdb.openstack.netvirt.impl.NeutronL3Adapter;

import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.HttpURLConnection;

public class SubnetHandler extends AbstractHandler implements INeutronSubnetAware {

    static final Logger logger = LoggerFactory.getLogger(SubnetHandler.class);

    // The implementation for each of these services is resolved by the OSGi Service Manager
    private volatile NeutronL3Adapter neutronL3Adapter;

    @Override
    public int canCreateSubnet(NeutronSubnet subnet) {
        return HttpURLConnection.HTTP_CREATED;
    }

    @Override
    public void neutronSubnetCreated(NeutronSubnet subnet) {
        enqueueEvent(new NorthboundEvent(subnet, NorthboundEvent.Action.ADD));
    }

    @Override
    public int canUpdateSubnet(NeutronSubnet delta, NeutronSubnet original) {
        return HttpURLConnection.HTTP_CREATED;
    }

    @Override
    public void neutronSubnetUpdated(NeutronSubnet subnet) {
        enqueueEvent(new NorthboundEvent(subnet, NorthboundEvent.Action.UPDATE));
    }

    @Override
    public int canDeleteSubnet(NeutronSubnet subnet) {
        // TODO Auto-generated method stub
        return HttpURLConnection.HTTP_CREATED;
    }

    @Override
    public void neutronSubnetDeleted(NeutronSubnet subnet) {
        enqueueEvent(new NorthboundEvent(subnet, NorthboundEvent.Action.DELETE));
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
            logger.error("Unable to process abstract event " + abstractEvent);
            return;
        }
        NorthboundEvent ev = (NorthboundEvent) abstractEvent;
        switch (ev.getAction()) {
            case ADD:
                // fall through
            case DELETE:
                // fall through
            case UPDATE:
                Preconditions.checkNotNull(neutronL3Adapter);
                neutronL3Adapter.handleNeutronSubnetEvent(ev.getSubnet(), ev.getAction());
                break;
            default:
                logger.warn("Unable to process event action " + ev.getAction());
                break;
        }
    }
}
