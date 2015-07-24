/*
 * Copyright (C) 2013 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.ovsdb.openstack.netvirt;

import java.net.HttpURLConnection;

import org.opendaylight.neutron.spi.INeutronFirewallAware;
import org.opendaylight.neutron.spi.INeutronFirewallPolicyAware;
import org.opendaylight.neutron.spi.INeutronFirewallRuleAware;
import org.opendaylight.neutron.spi.NeutronFirewall;
import org.opendaylight.neutron.spi.NeutronFirewallPolicy;
import org.opendaylight.neutron.spi.NeutronFirewallRule;
import org.opendaylight.ovsdb.openstack.netvirt.api.EventDispatcher;
import org.opendaylight.ovsdb.utils.servicehelper.ServiceHelper;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handle requests for OpenStack Neutron v2.0 Port Firewall API calls.
 */
public class FWaasHandler extends AbstractHandler
        implements INeutronFirewallAware, INeutronFirewallRuleAware,
        INeutronFirewallPolicyAware, ConfigInterface {

    private static final Logger LOG = LoggerFactory.getLogger(FWaasHandler.class);

    /**
     * Invoked when a Firewall Rules creation is requested
     * to indicate if the specified Rule can be created.
     *
     * @param neutronFirewall  An instance of proposed new Neutron Firewall object.
     * @return HttpURLConnection A HTTP status code to the creation request.
     */
    @Override
    public int canCreateNeutronFirewall(NeutronFirewall neutronFirewall) {
        return HttpURLConnection.HTTP_CREATED;
    }

    @Override
    public void neutronFirewallCreated(NeutronFirewall neutronFirewall) {
        LOG.debug("Neutron Firewall created by Neutron: {}", neutronFirewall);

        int result = canCreateNeutronFirewall(neutronFirewall);
        if (result != HttpURLConnection.HTTP_CREATED) {
            LOG.error("Neutron Firewall creation failed: {} ", result);
        }
    }

    @Override
    public int canUpdateNeutronFirewall(NeutronFirewall delta, NeutronFirewall original) {
        return HttpURLConnection.HTTP_OK;
    }

    @Override
    public void neutronFirewallUpdated(NeutronFirewall neutronFirewall) {
        LOG.debug("NeutronFirewall updated from Neutron: {}", neutronFirewall);
    }

    @Override
    public int canDeleteNeutronFirewall(NeutronFirewall neutronFirewall) {
        return HttpURLConnection.HTTP_OK;
    }

    @Override
    public void neutronFirewallDeleted(NeutronFirewall neutronFirewall) {
        //TODO: Trigger flowmod removals
        int result = canDeleteNeutronFirewall(neutronFirewall);
        if  (result != HttpURLConnection.HTTP_OK) {
            LOG.error(" delete Neutron Firewall validation failed for result - {} ", result);
        }
    }

    /**
     * Invoked when a Firewall Rule creation is requested
     * to indicate if the specified Rule can be created.
     *
     * @param neutronFirewallRule  An instance of proposed new Neutron Firewall Rule object.
     * @return HttpURLConnection A HTTP status code to the creation request.
     */
    @Override
    public int canCreateNeutronFirewallRule(NeutronFirewallRule neutronFirewallRule) {
        return HttpURLConnection.HTTP_CREATED;
    }

    @Override
    public void neutronFirewallRuleCreated(NeutronFirewallRule neutronFirewallRule) {
        LOG.debug("NeutronFirewallRule created by Neutron: {}", neutronFirewallRule);

        int result = canCreateNeutronFirewallRule(neutronFirewallRule);
        if (result != HttpURLConnection.HTTP_CREATED) {
            LOG.error("Neutron Firewall Rule creation failed {} ", result);
        }
    }

    @Override
    public int canUpdateNeutronFirewallRule(NeutronFirewallRule delta, NeutronFirewallRule original) {
        return HttpURLConnection.HTTP_OK;
    }

    @Override
    public void neutronFirewallRuleUpdated(NeutronFirewallRule neutronFirewallRule) {
        LOG.debug("Neutron Firewall Rule updated from Neutron: {}", neutronFirewallRule);
    }

    @Override
    public int canDeleteNeutronFirewallRule(NeutronFirewallRule neutronFirewallRule) {
        return HttpURLConnection.HTTP_OK;
    }

    @Override
    public void neutronFirewallRuleDeleted(NeutronFirewallRule neutronFirewallRule) {
        int result = canDeleteNeutronFirewallRule(neutronFirewallRule);
        if  (result != HttpURLConnection.HTTP_OK) {
            LOG.error(" delete Neutron Firewall Rule validation failed for result - {} ", result);
        }
    }

    /**
     * Invoked when a Firewall Policy creation is requested
     * to indicate if the specified Rule can be created.
     *
     * @param neutronFirewallPolicy  An instance of proposed new Neutron Firewall Policy object.
     * @return HttpURLConnection A HTTP status code to the creation request.
     */
    @Override
    public int canCreateNeutronFirewallPolicy(NeutronFirewallPolicy neutronFirewallPolicy) {
        return HttpURLConnection.HTTP_CREATED;
    }

    @Override
    public void neutronFirewallPolicyCreated(NeutronFirewallPolicy neutronFirewallPolicy) {
        LOG.debug("Neutron Firewall Policy created by Neutron: {}", neutronFirewallPolicy);

        int result = canCreateNeutronFirewallPolicy(neutronFirewallPolicy);
        if (result != HttpURLConnection.HTTP_CREATED) {
            LOG.debug("Neutron Firewall Policy creation failed: {} ", result);
        }
    }

    @Override
    public int canUpdateNeutronFirewallPolicy(NeutronFirewallPolicy delta, NeutronFirewallPolicy original) {
        return HttpURLConnection.HTTP_OK;
    }

    @Override
    public void neutronFirewallPolicyUpdated(NeutronFirewallPolicy neutronFirewallPolicy) {
        LOG.debug("Neutron Firewall Policy updated from Neutron: {}", neutronFirewallPolicy);
    }

    @Override
    public int canDeleteNeutronFirewallPolicy(NeutronFirewallPolicy neutronFirewallPolicy) {
        return HttpURLConnection.HTTP_OK;
    }

    @Override
    public void neutronFirewallPolicyDeleted(NeutronFirewallPolicy neutronFirewallPolicy) {
        int result = canDeleteNeutronFirewallPolicy(neutronFirewallPolicy);
        if  (result != HttpURLConnection.HTTP_OK) {
            LOG.error(" delete Neutron Firewall Policy validation failed for result - {} ", result);
        }
    }

    /**
     * Process the event.
     *
     * @param abstractEvent@see org.opendaylight.ovsdb.openstack.netvirt.api.EventDispatcher
     */
    @Override
    public void processEvent(AbstractEvent abstractEvent) {
        if (!(abstractEvent instanceof NorthboundEvent)) {
            LOG.error("Unable to process abstract event {}", abstractEvent);
            return;
        }
        NorthboundEvent ev = (NorthboundEvent) abstractEvent;
        switch (ev.getAction()) {
            // TODO: add handling of events here, once callbacks do something
            //       other than logging.
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
                bundleContext.getServiceReference(INeutronFirewallAware.class.getName()), this);
    }

    @Override
    public void setDependencies(Object impl) {}
}