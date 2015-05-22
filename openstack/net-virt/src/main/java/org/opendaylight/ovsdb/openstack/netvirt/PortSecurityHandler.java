/*
 * Copyright (C) 2013 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Brent Salisbury, Madhu Venugopal
 */

package org.opendaylight.ovsdb.openstack.netvirt;

import org.opendaylight.neutron.spi.INeutronSecurityGroupAware;
import org.opendaylight.neutron.spi.INeutronSecurityRuleAware;
import org.opendaylight.neutron.spi.NeutronSecurityGroup;
import org.opendaylight.neutron.spi.NeutronSecurityRule;

import org.opendaylight.ovsdb.openstack.netvirt.api.EventDispatcher;
import org.opendaylight.ovsdb.utils.servicehelper.ServiceHelper;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.HttpURLConnection;

/**
 * Handle requests for OpenStack Neutron v2.0 Port Security API calls.
 */
public class PortSecurityHandler extends AbstractHandler
        implements INeutronSecurityGroupAware, INeutronSecurityRuleAware, ConfigInterface {

    static final Logger logger = LoggerFactory.getLogger(PortSecurityHandler.class);
    private volatile EventDispatcher eventDispatcher;

    @Override
    public int canCreateNeutronSecurityGroup(NeutronSecurityGroup neutronSecurityGroup) {
        return HttpURLConnection.HTTP_CREATED;
    }

    @Override
    public void neutronSecurityGroupCreated(NeutronSecurityGroup neutronSecurityGroup) {
        int result = HttpURLConnection.HTTP_BAD_REQUEST;

        result = canCreateNeutronSecurityGroup(neutronSecurityGroup);
        if (result != HttpURLConnection.HTTP_CREATED) {
            logger.debug("Neutron Security Group creation failed {} ", result);
            return;
        }
    }

    @Override
    public int canUpdateNeutronSecurityGroup(NeutronSecurityGroup delta, NeutronSecurityGroup original) {
        return HttpURLConnection.HTTP_OK;
    }

    @Override
    public void neutronSecurityGroupUpdated(NeutronSecurityGroup neutronSecurityGroup) {
        return;
    }

    @Override
    public int canDeleteNeutronSecurityGroup(NeutronSecurityGroup neutronSecurityGroup) {
        return HttpURLConnection.HTTP_OK;
    }

    @Override
    public void neutronSecurityGroupDeleted(NeutronSecurityGroup neutronSecurityGroup) {
        //TODO: Trigger flowmod removals
        int result = canDeleteNeutronSecurityGroup(neutronSecurityGroup);
        if  (result != HttpURLConnection.HTTP_OK) {
            logger.error(" delete Neutron Security Rule validation failed for result - {} ", result);
            return;
        }
    }

    /**
     * Invoked when a Security Rules creation is requested
     * to indicate if the specified Rule can be created.
     *
     * @param neutronSecurityRule  An instance of proposed new Neutron Security Rule object.
     * @return A HTTP status code to the creation request.
     */

    @Override
    public int canCreateNeutronSecurityRule(NeutronSecurityRule neutronSecurityRule) {
        return HttpURLConnection.HTTP_CREATED;
    }

    @Override
    public void neutronSecurityRuleCreated(NeutronSecurityRule neutronSecurityRule) {
        int result = HttpURLConnection.HTTP_BAD_REQUEST;

        result = canCreateNeutronSecurityRule(neutronSecurityRule);
        if (result != HttpURLConnection.HTTP_CREATED) {
            logger.debug("Neutron Security Group creation failed {} ", result);
            return;
        }
    }

    @Override
    public int canUpdateNeutronSecurityRule(NeutronSecurityRule delta, NeutronSecurityRule original) {
        return HttpURLConnection.HTTP_OK;
    }

    @Override
    public void neutronSecurityRuleUpdated(NeutronSecurityRule neutronSecurityRule) {
        return;
    }

    @Override
    public int canDeleteNeutronSecurityRule(NeutronSecurityRule neutronSecurityRule) {
        return HttpURLConnection.HTTP_OK;
    }

    @Override
    public void neutronSecurityRuleDeleted(NeutronSecurityRule neutronSecurityRule) {
        int result = canDeleteNeutronSecurityRule(neutronSecurityRule);
        if  (result != HttpURLConnection.HTTP_OK) {
            logger.error(" delete Neutron Security Rule validation failed for result - {} ", result);
            return;
        }
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
            // TODO: add handling of events here, once callbacks do something
            //       other than logging.
            default:
                logger.warn("Unable to process event action " + ev.getAction());
                break;
        }
    }

    @Override
    public void setDependencies(BundleContext bundleContext, ServiceReference serviceReference) {
        eventDispatcher =
                (EventDispatcher) ServiceHelper.getGlobalInstance(EventDispatcher.class, this);
        eventDispatcher.eventHandlerAdded(
                bundleContext.getServiceReference(INeutronSecurityGroupAware.class.getName()), this);
        super.setDispatcher(eventDispatcher);
    }

    @Override
    public void setDependencies(Object impl) {}
}