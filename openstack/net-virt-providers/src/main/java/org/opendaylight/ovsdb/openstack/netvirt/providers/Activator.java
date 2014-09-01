/*
 * Copyright (C) 2013 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Madhu Venugopal, Brent Salisbury, Dave Tucker
 */

package org.opendaylight.ovsdb.openstack.netvirt.providers;

import java.util.Properties;

import org.apache.felix.dm.Component;
import org.opendaylight.controller.forwardingrulesmanager.IForwardingRulesManager;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.core.ComponentActivatorAbstractBase;
import org.opendaylight.controller.switchmanager.ISwitchManager;
import org.opendaylight.ovsdb.openstack.netvirt.api.ArpProvider;
import org.opendaylight.ovsdb.openstack.netvirt.api.BridgeConfigurationManager;
import org.opendaylight.ovsdb.openstack.netvirt.api.Constants;
import org.opendaylight.ovsdb.openstack.netvirt.api.InboundNatProvider;
import org.opendaylight.ovsdb.openstack.netvirt.api.L3ForwardingProvider;
import org.opendaylight.ovsdb.openstack.netvirt.api.NetworkingProvider;
import org.opendaylight.ovsdb.openstack.netvirt.api.OutboundNatProvider;
import org.opendaylight.ovsdb.openstack.netvirt.api.RoutingProvider;
import org.opendaylight.ovsdb.openstack.netvirt.api.TenantNetworkManager;
import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow10.OF10Provider;
import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.AbstractServiceInstance;
import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.MdsalConsumer;
import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.MdsalConsumerImpl;
import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.OF13Provider;
import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.PipelineOrchestrator;
import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.PipelineOrchestratorImpl;
import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.Service;
import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.services.ArpResponderService;
import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.services.ClassifierService;
import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.services.EgressAclService;
import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.services.InboundNatService;
import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.services.IngressAclService;
import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.services.L2ForwardingService;
import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.services.L2RewriteService;
import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.services.L3ForwardingService;
import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.services.LoadBalancerService;
import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.services.OutboundNatService;
import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.services.RoutingService;
import org.opendaylight.ovsdb.plugin.api.OvsdbConfigurationService;
import org.opendaylight.ovsdb.plugin.api.OvsdbConnectionService;

/**
 * OSGi Bundle Activator for the Neutron providers
 */
public class Activator extends ComponentActivatorAbstractBase {
    /**
     * Function called when the activator starts just after some
     * initializations are done by the
     * ComponentActivatorAbstractBase.
     */
    @Override
    public void init() {
    }

    /**
     * Function called when the activator stops just before the
     * cleanup done by ComponentActivatorAbstractBase.
     *
     */
    @Override
    public void destroy() {
    }

    /**
     * Function that is used to communicate to dependency manager the
     * list of known implementations for services inside a container.
     *
     * @return An array containing all the CLASS objects that will be
     * instantiated in order to get an fully working implementation
     * Object
     */
    @Override
    public Object[] getImplementations() {
        Object[] res = {MdsalConsumerImpl.class,
                        OF10Provider.class,
                        OF13Provider.class,
                        PipelineOrchestratorImpl.class,
                        ClassifierService.class,
                        ArpResponderService.class,
                        InboundNatService.class,
                        IngressAclService.class,
                        LoadBalancerService.class,
                        RoutingService.class,
                        L3ForwardingService.class,
                        L2RewriteService.class,
                        L2ForwardingService.class,
                        EgressAclService.class,
                        OutboundNatService.class};
        return res;
    }

    /**
     * Function that is called when configuration of the dependencies
     * is required.
     *
     * @param c dependency manager Component object, used for
     * configuring the dependencies exported and imported
     * @param imp Implementation class that is being configured,
     * needed as long as the same routine can configure multiple
     * implementations
     * @param containerName The containerName being configured, this allow
     * also optional per-container different behavior if needed, usually
     * should not be the case though.
     */
    @Override
    public void configureInstance(Component c, Object imp,
                                  String containerName) {

        if (imp.equals(MdsalConsumerImpl.class)) {
            c.setInterface(MdsalConsumer.class.getName(), null);
            c.add(createServiceDependency().setService(BindingAwareBroker.class).setRequired(true));
        }

        if (imp.equals(OF10Provider.class)) {
            Properties of10Properties = new Properties();
            of10Properties.put(Constants.SOUTHBOUND_PROTOCOL_PROPERTY, "ovsdb");
            of10Properties.put(Constants.OPENFLOW_VERSION_PROPERTY, Constants.OPENFLOW10);

            c.setInterface(NetworkingProvider.class.getName(), of10Properties);
            c.add(createServiceDependency()
                          .setService(org.opendaylight.ovsdb.openstack.netvirt.api.ConfigurationService.class)
                          .setRequired(true));
            c.add(createServiceDependency()
                          .setService(BridgeConfigurationManager.class)
                          .setRequired(true));
            c.add(createServiceDependency()
                          .setService(TenantNetworkManager.class)
                          .setRequired(true));
            c.add(createServiceDependency().setService(OvsdbConfigurationService.class).setRequired(true));
            c.add(createServiceDependency().setService(OvsdbConnectionService.class).setRequired(true));
            c.add(createServiceDependency().
                    setService(IForwardingRulesManager.class).
                    setRequired(true));
            c.add(createServiceDependency().
                    setService(ISwitchManager.class).
                    setRequired(true));
        }

        if (imp.equals(OF13Provider.class)) {
            Properties of13Properties = new Properties();
            of13Properties.put(Constants.SOUTHBOUND_PROTOCOL_PROPERTY, "ovsdb");
            of13Properties.put(Constants.OPENFLOW_VERSION_PROPERTY, Constants.OPENFLOW13);

            c.setInterface(NetworkingProvider.class.getName(), of13Properties);
            c.add(createServiceDependency()
                          .setService(org.opendaylight.ovsdb.openstack.netvirt.api.ConfigurationService.class)
                          .setRequired(true));
            c.add(createServiceDependency()
                          .setService(BridgeConfigurationManager.class)
                          .setRequired(true));
            c.add(createServiceDependency()
                          .setService(TenantNetworkManager.class)
                          .setRequired(true));
            c.add(createServiceDependency().setService(OvsdbConfigurationService.class).setRequired(true));
            c.add(createServiceDependency().setService(OvsdbConnectionService.class).setRequired(true));
            c.add(createServiceDependency().setService(MdsalConsumer.class).setRequired(true));
        }

        if (imp.equals(PipelineOrchestratorImpl.class)) {
            c.setInterface(PipelineOrchestrator.class.getName(), null);
            c.add(createServiceDependency()
                           .setService(AbstractServiceInstance.class)
                           .setCallbacks("registerService", "unregisterService"));
        }

        if (AbstractServiceInstance.class.isAssignableFrom((Class)imp)) {
            c.add(createServiceDependency()
                    .setService(PipelineOrchestrator.class)
                    .setRequired(true));
            c.add(createServiceDependency().setService(MdsalConsumer.class).setRequired(true));
        }

        if (imp.equals(ClassifierService.class)) {
            Properties properties = new Properties();
            properties.put(AbstractServiceInstance.SERVICE_PROPERTY, Service.CLASSIFIER);
            properties.put(Constants.PROVIDER_NAME_PROPERTY, OF13Provider.NAME);
            c.setInterface(AbstractServiceInstance.class.getName(), properties);
        }

        if (imp.equals(ArpResponderService.class)) {
            Properties properties = new Properties();
            properties.put(AbstractServiceInstance.SERVICE_PROPERTY, Service.ARP_RESPONDER);
            properties.put(Constants.PROVIDER_NAME_PROPERTY, OF13Provider.NAME);
            c.setInterface(new String[] {AbstractServiceInstance.class.getName(), ArpProvider.class.getName()},
                           properties);
        }

        if (imp.equals(InboundNatService.class)) {
            Properties properties = new Properties();
            properties.put(AbstractServiceInstance.SERVICE_PROPERTY, Service.INBOUND_NAT);
            properties.put(Constants.PROVIDER_NAME_PROPERTY, OF13Provider.NAME);
            c.setInterface(new String[] {AbstractServiceInstance.class.getName(), InboundNatProvider.class.getName()},
                           properties);
        }

        if (imp.equals(IngressAclService.class)) {
            Properties properties = new Properties();
            properties.put(AbstractServiceInstance.SERVICE_PROPERTY, Service.INGRESS_ACL);
            properties.put(Constants.PROVIDER_NAME_PROPERTY, OF13Provider.NAME);
            c.setInterface(AbstractServiceInstance.class.getName(), properties);
        }

        if (imp.equals(LoadBalancerService.class)) {
            Properties properties = new Properties();
            properties.put(AbstractServiceInstance.SERVICE_PROPERTY, Service.LOAD_BALANCER);
            properties.put(Constants.PROVIDER_NAME_PROPERTY, OF13Provider.NAME);
            c.setInterface(AbstractServiceInstance.class.getName(), properties);
        }

        if (imp.equals(RoutingService.class)) {
            Properties properties = new Properties();
            properties.put(AbstractServiceInstance.SERVICE_PROPERTY, Service.ROUTING);
            properties.put(Constants.PROVIDER_NAME_PROPERTY, OF13Provider.NAME);
            c.setInterface(new String[] {AbstractServiceInstance.class.getName(), RoutingProvider.class.getName()},
                           properties);
        }

        if (imp.equals(L3ForwardingService.class)) {
            Properties properties = new Properties();
            properties.put(AbstractServiceInstance.SERVICE_PROPERTY, Service.L3_FORWARDING);
            properties.put(Constants.PROVIDER_NAME_PROPERTY, OF13Provider.NAME);
            c.setInterface(new String[] {AbstractServiceInstance.class.getName(), L3ForwardingProvider.class.getName()},
                           properties);
        }

        if (imp.equals(L2RewriteService.class)) {
            Properties properties = new Properties();
            properties.put(AbstractServiceInstance.SERVICE_PROPERTY, Service.L2_REWRITE);
            properties.put(Constants.PROVIDER_NAME_PROPERTY, OF13Provider.NAME);
            c.setInterface(AbstractServiceInstance.class.getName(), properties);
        }

        if (imp.equals(L2ForwardingService.class)) {
            Properties properties = new Properties();
            properties.put(AbstractServiceInstance.SERVICE_PROPERTY, Service.L2_FORWARDING);
            properties.put(Constants.PROVIDER_NAME_PROPERTY, OF13Provider.NAME);
            c.setInterface(AbstractServiceInstance.class.getName(), properties);
        }

        if (imp.equals(IngressAclService.class)) {
            Properties properties = new Properties();
            properties.put(AbstractServiceInstance.SERVICE_PROPERTY, Service.INGRESS_ACL);
            properties.put(Constants.PROVIDER_NAME_PROPERTY, OF13Provider.NAME);
            c.setInterface(AbstractServiceInstance.class.getName(), properties);
        }

        if (imp.equals(OutboundNatService.class)) {
            Properties properties = new Properties();
            properties.put(AbstractServiceInstance.SERVICE_PROPERTY, Service.OUTBOUND_NAT);
            properties.put(Constants.PROVIDER_NAME_PROPERTY, OF13Provider.NAME);
            c.setInterface(new String[] {AbstractServiceInstance.class.getName(), OutboundNatProvider.class.getName()},
                           properties);
        }
    }
}
