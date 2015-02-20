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

import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.ovsdb.openstack.netvirt.api.ArpProvider;
import org.opendaylight.ovsdb.openstack.netvirt.api.BridgeConfigurationManager;
import org.opendaylight.ovsdb.openstack.netvirt.api.ClassifierProvider;
import org.opendaylight.ovsdb.openstack.netvirt.api.ConfigurationService;
import org.opendaylight.ovsdb.openstack.netvirt.api.Constants;
import org.opendaylight.ovsdb.openstack.netvirt.api.EgressAclProvider;
import org.opendaylight.ovsdb.openstack.netvirt.api.InboundNatProvider;
import org.opendaylight.ovsdb.openstack.netvirt.api.IngressAclProvider;
import org.opendaylight.ovsdb.openstack.netvirt.api.L2ForwardingProvider;
import org.opendaylight.ovsdb.openstack.netvirt.api.L3ForwardingProvider;
import org.opendaylight.ovsdb.openstack.netvirt.api.LoadBalancerProvider;
import org.opendaylight.ovsdb.openstack.netvirt.api.NetworkingProvider;
import org.opendaylight.ovsdb.openstack.netvirt.api.OutboundNatProvider;
import org.opendaylight.ovsdb.openstack.netvirt.api.RoutingProvider;
import org.opendaylight.ovsdb.openstack.netvirt.api.SecurityServicesManager;
import org.opendaylight.ovsdb.openstack.netvirt.api.TenantNetworkManager;
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
import org.opendaylight.ovsdb.compatibility.plugin.api.OvsdbConfigurationService;
import org.opendaylight.ovsdb.compatibility.plugin.api.OvsdbConnectionService;

import org.osgi.framework.BundleContext;

import java.util.Dictionary;
import java.util.Hashtable;

/**
 * OSGi Bundle Activator for the Neutron providers
 */
public class Activator extends DependencyActivatorBase {

    @Override
    public void init(BundleContext context, DependencyManager manager) throws Exception {

        manager.add(createComponent()
                .setInterface(MdsalConsumer.class.getName(), null)
                .setImplementation(MdsalConsumerImpl.class)
                .add(createServiceDependency().setService(BindingAwareBroker.class).setRequired(true)));

        Dictionary<String, Object> props1 = new Hashtable<>();
        props1.put(Constants.SOUTHBOUND_PROTOCOL_PROPERTY, "ovsdb");
        props1.put(Constants.OPENFLOW_VERSION_PROPERTY, Constants.OPENFLOW13);

        manager.add(createComponent()
                .setInterface(NetworkingProvider.class.getName(), props1)
                .setImplementation(OF13Provider.class)
                .add(createServiceDependency().setService(ConfigurationService.class).setRequired(true))
                .add(createServiceDependency().setService(BridgeConfigurationManager.class).setRequired(true))
                .add(createServiceDependency().setService(TenantNetworkManager.class).setRequired(true))
                .add(createServiceDependency().setService(SecurityServicesManager.class).setRequired(true))
                .add(createServiceDependency().setService(OvsdbConfigurationService.class).setRequired(true))
                .add(createServiceDependency().setService(OvsdbConnectionService.class).setRequired(true))
                .add(createServiceDependency().setService(MdsalConsumer.class).setRequired(true))
                .add(createServiceDependency().setService(ClassifierProvider.class).setRequired(true))
                .add(createServiceDependency().setService(IngressAclProvider.class).setRequired(true))
                .add(createServiceDependency().setService(EgressAclProvider.class).setRequired(true))
                .add(createServiceDependency().setService(L2ForwardingProvider.class).setRequired(true)));

        manager.add(createComponent()
                .setInterface(PipelineOrchestrator.class.getName(), null)
                .setImplementation(PipelineOrchestratorImpl.class)
                .add(createServiceDependency().setService(AbstractServiceInstance.class)
                        .setCallbacks("registerService", "unregisterService"))
                .add(createServiceDependency().setService(MdsalConsumer.class).setRequired(true)));

        Dictionary<String, Object> props2 = new Hashtable<>();
        props2.put(AbstractServiceInstance.SERVICE_PROPERTY, Service.CLASSIFIER);
        props2.put(Constants.PROVIDER_NAME_PROPERTY, OF13Provider.NAME);

        manager.add(createComponent()
                .setInterface(new String[]{AbstractServiceInstance.class.getName(), ClassifierProvider.class.getName()},
                        props2)
                .setImplementation(ClassifierService.class)
                .add(createServiceDependency().setService(OvsdbConfigurationService.class).setRequired(true))
                .add(createServiceDependency().setService(OvsdbConnectionService.class).setRequired(true))
                .add(createServiceDependency().setService(PipelineOrchestrator.class).setRequired(true))
                .add(createServiceDependency().setService(MdsalConsumer.class).setRequired(true)));

        Dictionary<String, Object> props3 = new Hashtable<>();
        props3.put(AbstractServiceInstance.SERVICE_PROPERTY, Service.ARP_RESPONDER);
        props3.put(Constants.PROVIDER_NAME_PROPERTY, OF13Provider.NAME);

        manager.add(createComponent()
                .setInterface(new String[]{AbstractServiceInstance.class.getName(), ArpProvider.class.getName()},
                        props3)
                .setImplementation(ArpResponderService.class)
                .add(createServiceDependency().setService(OvsdbConfigurationService.class).setRequired(true))
                .add(createServiceDependency().setService(OvsdbConnectionService.class).setRequired(true))
                .add(createServiceDependency().setService(PipelineOrchestrator.class).setRequired(true))
                .add(createServiceDependency().setService(MdsalConsumer.class).setRequired(true)));

        Dictionary<String, Object> props4 = new Hashtable<>();
        props4.put(AbstractServiceInstance.SERVICE_PROPERTY, Service.INBOUND_NAT);
        props4.put(Constants.PROVIDER_NAME_PROPERTY, OF13Provider.NAME);

        manager.add(createComponent()
                .setInterface(new String[]{AbstractServiceInstance.class.getName(),
                        InboundNatProvider.class.getName()}, props4)
                .setImplementation(InboundNatService.class)
                .add(createServiceDependency().setService(OvsdbConfigurationService.class).setRequired(true))
                .add(createServiceDependency().setService(OvsdbConnectionService.class).setRequired(true))
                .add(createServiceDependency().setService(PipelineOrchestrator.class).setRequired(true))
                .add(createServiceDependency().setService(MdsalConsumer.class).setRequired(true)));

        Dictionary<String, Object> props5 = new Hashtable<>();
        props5.put(AbstractServiceInstance.SERVICE_PROPERTY, Service.INGRESS_ACL);
        props5.put(Constants.PROVIDER_NAME_PROPERTY, OF13Provider.NAME);

        manager.add(createComponent()
                .setInterface(new String[]{AbstractServiceInstance.class.getName(), IngressAclProvider.class.getName()},
                        props5)
                .setImplementation(IngressAclService.class)
                .add(createServiceDependency().setService(OvsdbConfigurationService.class).setRequired(true))
                .add(createServiceDependency().setService(OvsdbConnectionService.class).setRequired(true))
                .add(createServiceDependency().setService(PipelineOrchestrator.class).setRequired(true))
                .add(createServiceDependency().setService(MdsalConsumer.class).setRequired(true)));

        Dictionary<String, Object> props6 = new Hashtable<>();
        props6.put(AbstractServiceInstance.SERVICE_PROPERTY, Service.LOAD_BALANCER);
        props6.put(Constants.PROVIDER_NAME_PROPERTY, OF13Provider.NAME);

        manager.add(createComponent()
                .setInterface(new String[] {AbstractServiceInstance.class.getName(),
                                LoadBalancerProvider.class.getName()}, props6)
                .setImplementation(LoadBalancerService.class)
                .add(createServiceDependency().setService(OvsdbConfigurationService.class).setRequired(true))
                .add(createServiceDependency().setService(OvsdbConnectionService.class).setRequired(true))
                .add(createServiceDependency().setService(PipelineOrchestrator.class).setRequired(true))
                .add(createServiceDependency().setService(MdsalConsumer.class).setRequired(true)));

        Dictionary<String, Object> props7 = new Hashtable<>();
        props7.put(AbstractServiceInstance.SERVICE_PROPERTY, Service.ROUTING);
        props7.put(Constants.PROVIDER_NAME_PROPERTY, OF13Provider.NAME);

        manager.add(createComponent()
                .setInterface(new String[] {AbstractServiceInstance.class.getName(), RoutingProvider.class.getName()},
                        props7)
                .setImplementation(RoutingService.class)
                .add(createServiceDependency().setService(OvsdbConfigurationService.class).setRequired(true))
                .add(createServiceDependency().setService(OvsdbConnectionService.class).setRequired(true))
                .add(createServiceDependency().setService(PipelineOrchestrator.class).setRequired(true))
                .add(createServiceDependency().setService(MdsalConsumer.class).setRequired(true)));

        Dictionary<String, Object> props8 = new Hashtable<>();
        props8.put(AbstractServiceInstance.SERVICE_PROPERTY, Service.L3_FORWARDING);
        props8.put(Constants.PROVIDER_NAME_PROPERTY, OF13Provider.NAME);

        manager.add(createComponent()
                .setInterface(new String[] {AbstractServiceInstance.class.getName(),
                                L3ForwardingProvider.class.getName()}, props8)
                .setImplementation(L3ForwardingService.class)
                .add(createServiceDependency().setService(OvsdbConfigurationService.class).setRequired(true))
                .add(createServiceDependency().setService(OvsdbConnectionService.class).setRequired(true))
                .add(createServiceDependency().setService(PipelineOrchestrator.class).setRequired(true))
                .add(createServiceDependency().setService(MdsalConsumer.class).setRequired(true)));

        Dictionary<String, Object> props9 = new Hashtable<>();
        props9.put(AbstractServiceInstance.SERVICE_PROPERTY, Service.L2_REWRITE);
        props9.put(Constants.PROVIDER_NAME_PROPERTY, OF13Provider.NAME);

        manager.add(createComponent()
                .setInterface(AbstractServiceInstance.class.getName(), props9)
                .setImplementation(L2RewriteService.class)
                .add(createServiceDependency().setService(OvsdbConfigurationService.class).setRequired(true))
                .add(createServiceDependency().setService(OvsdbConnectionService.class).setRequired(true))
                .add(createServiceDependency().setService(PipelineOrchestrator.class).setRequired(true))
                .add(createServiceDependency().setService(MdsalConsumer.class).setRequired(true)));

        Dictionary<String, Object> props10 = new Hashtable<>();
        props10.put(AbstractServiceInstance.SERVICE_PROPERTY, Service.L2_FORWARDING);
        props10.put(Constants.PROVIDER_NAME_PROPERTY, OF13Provider.NAME);

        manager.add(createComponent()
                .setInterface(new String[] {AbstractServiceInstance.class.getName(),
                                L2ForwardingProvider.class.getName()},
                        props10)
                .setImplementation(L2ForwardingService.class)
                .add(createServiceDependency().setService(OvsdbConfigurationService.class).setRequired(true))
                .add(createServiceDependency().setService(OvsdbConnectionService.class).setRequired(true))
                .add(createServiceDependency().setService(PipelineOrchestrator.class).setRequired(true))
                .add(createServiceDependency().setService(MdsalConsumer.class).setRequired(true)));

        Dictionary<String, Object> props11 = new Hashtable<>();
        props11.put(AbstractServiceInstance.SERVICE_PROPERTY, Service.EGRESS_ACL);
        props11.put(Constants.PROVIDER_NAME_PROPERTY, OF13Provider.NAME);

        manager.add(createComponent()
                .setInterface(new String[]{AbstractServiceInstance.class.getName(), EgressAclProvider.class.getName()},
                        props11)
                .setImplementation(EgressAclService.class)
                .add(createServiceDependency().setService(OvsdbConfigurationService.class).setRequired(true))
                .add(createServiceDependency().setService(OvsdbConnectionService.class).setRequired(true))
                .add(createServiceDependency().setService(PipelineOrchestrator.class).setRequired(true))
                .add(createServiceDependency().setService(MdsalConsumer.class).setRequired(true)));

        Dictionary<String, Object> props12 = new Hashtable<>();
        props12.put(AbstractServiceInstance.SERVICE_PROPERTY, Service.OUTBOUND_NAT);
        props12.put(Constants.PROVIDER_NAME_PROPERTY, OF13Provider.NAME);

        manager.add(createComponent()
                .setInterface(new String[]{AbstractServiceInstance.class.getName(),
                                OutboundNatProvider.class.getName()},
                        props12)
                .setImplementation(OutboundNatService.class)
                .add(createServiceDependency().setService(OvsdbConfigurationService.class).setRequired(true))
                .add(createServiceDependency().setService(OvsdbConnectionService.class).setRequired(true))
                .add(createServiceDependency().setService(PipelineOrchestrator.class).setRequired(true))
                .add(createServiceDependency().setService(MdsalConsumer.class).setRequired(true)));
    }

    @Override
    public void destroy(BundleContext context, DependencyManager manager) throws Exception {
    }
}
