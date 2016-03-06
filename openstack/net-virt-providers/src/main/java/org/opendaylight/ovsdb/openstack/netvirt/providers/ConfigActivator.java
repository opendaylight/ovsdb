/*
 * Copyright (c) 2015, 2016 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt.providers;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.NotificationProviderService;
import org.opendaylight.ovsdb.openstack.netvirt.api.ArpProvider;
import org.opendaylight.ovsdb.openstack.netvirt.api.ClassifierProvider;
import org.opendaylight.ovsdb.openstack.netvirt.api.ConfigurationService;
import org.opendaylight.ovsdb.openstack.netvirt.api.Constants;
import org.opendaylight.ovsdb.openstack.netvirt.api.EgressAclProvider;
import org.opendaylight.ovsdb.openstack.netvirt.api.GatewayMacResolver;
import org.opendaylight.ovsdb.openstack.netvirt.api.IcmpEchoProvider;
import org.opendaylight.ovsdb.openstack.netvirt.api.InboundNatProvider;
import org.opendaylight.ovsdb.openstack.netvirt.api.IngressAclProvider;
import org.opendaylight.ovsdb.openstack.netvirt.api.L2ForwardingProvider;
import org.opendaylight.ovsdb.openstack.netvirt.api.L2RewriteProvider;
import org.opendaylight.ovsdb.openstack.netvirt.api.L3ForwardingProvider;
import org.opendaylight.ovsdb.openstack.netvirt.api.LoadBalancerProvider;
import org.opendaylight.ovsdb.openstack.netvirt.api.NetworkingProvider;
import org.opendaylight.ovsdb.openstack.netvirt.api.NetworkingProviderManager;
import org.opendaylight.ovsdb.openstack.netvirt.api.NodeCacheListener;
import org.opendaylight.ovsdb.openstack.netvirt.api.NodeCacheManager;
import org.opendaylight.ovsdb.openstack.netvirt.api.OutboundNatProvider;
import org.opendaylight.ovsdb.openstack.netvirt.api.RoutingProvider;
import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.AbstractServiceInstance;
import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.OF13Provider;
import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.PipelineOrchestrator;
import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.PipelineOrchestratorImpl;
import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.Service;
import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.services.ArpResponderService;
import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.services.ClassifierService;
import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.services.EgressAclService;
import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.services.IcmpEchoResponderService;
import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.services.InboundNatService;
import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.services.IngressAclService;
import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.services.L2ForwardingService;
import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.services.L2RewriteService;
import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.services.L3ForwardingService;
import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.services.LoadBalancerService;
import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.services.OutboundNatService;
import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.services.RoutingService;
import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.services.arp.GatewayMacResolverService;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

public class ConfigActivator implements BundleActivator {
    private static final Logger LOG = LoggerFactory.getLogger(ConfigActivator.class);
    private List<ServiceRegistration<?>> registrations = new ArrayList<>();
    private ProviderContext providerContext;

    public ConfigActivator(ProviderContext providerContext) {
        this.providerContext = providerContext;
    }

    @Override
    public void start(BundleContext context) throws Exception {
        LOG.info("ConfigActivator start:");

        NetvirtProvidersConfigImpl netvirtProvidersConfig =
                new NetvirtProvidersConfigImpl(providerContext.getSALService(DataBroker.class),
                        NetvirtProvidersProvider.getTableOffset());
        registerService(context,
                new String[] {NetvirtProvidersConfigImpl.class.getName()},
                null, netvirtProvidersConfig);

        PipelineOrchestratorImpl pipelineOrchestrator = new PipelineOrchestratorImpl();
        registerService(context,
                new String[] {PipelineOrchestrator.class.getName(),NodeCacheListener.class.getName()},
                null, pipelineOrchestrator);

        Dictionary<String, Object> of13ProviderProperties = new Hashtable<>();
        of13ProviderProperties.put(Constants.SOUTHBOUND_PROTOCOL_PROPERTY, "ovsdb");
        of13ProviderProperties.put(Constants.OPENFLOW_VERSION_PROPERTY, Constants.OPENFLOW13);
        final OF13Provider of13Provider = new OF13Provider();
        registerService(context,
                new String[] {NetworkingProvider.class.getName()},
                of13ProviderProperties, of13Provider);

        ClassifierService classifierService = new ClassifierService();
        registerService(context, ClassifierProvider.class.getName(),
                classifierService, Service.CLASSIFIER);

        ArpResponderService arpResponderService = new ArpResponderService();
        registerService(context, ArpProvider.class.getName(),
                arpResponderService, Service.ARP_RESPONDER);

        InboundNatService inboundNatService = new InboundNatService();
        registerService(context, InboundNatProvider.class.getName(),
                inboundNatService, Service.INBOUND_NAT);

        IngressAclService ingressAclService = new IngressAclService();
        registerService(context, IngressAclProvider.class.getName(),
                ingressAclService, Service.INGRESS_ACL);

        LoadBalancerService loadBalancerService = new LoadBalancerService();
        registerService(context, LoadBalancerProvider.class.getName(),
                loadBalancerService, Service.LOAD_BALANCER);

        RoutingService routingService = new RoutingService();
        registerService(context, RoutingProvider.class.getName(),
                routingService, Service.ROUTING);

        L3ForwardingService l3ForwardingService = new L3ForwardingService();
        registerService(context, L3ForwardingProvider.class.getName(),
                l3ForwardingService, Service.L3_FORWARDING);

        L2RewriteService l2RewriteService = new L2RewriteService();
        registerService(context, L2RewriteProvider.class.getName(),
                l2RewriteService, Service.L2_REWRITE);

        L2ForwardingService l2ForwardingService = new L2ForwardingService();
        registerService(context, L2ForwardingProvider.class.getName(),
                l2ForwardingService, Service.L2_FORWARDING);

        EgressAclService egressAclService = new EgressAclService();
        registerService(context, EgressAclProvider.class.getName(),
                egressAclService, Service.EGRESS_ACL);

        OutboundNatService outboundNatService = new OutboundNatService();
        registerService(context, OutboundNatProvider.class.getName(),
                outboundNatService, Service.OUTBOUND_NAT);

        final GatewayMacResolverService gatewayMacResolverService = new GatewayMacResolverService();
        registerService(context, GatewayMacResolver.class.getName(),
                gatewayMacResolverService, Service.GATEWAY_RESOLVER);
        getNotificationProviderService().registerNotificationListener(gatewayMacResolverService);

        IcmpEchoResponderService icmpEchoResponderService = new IcmpEchoResponderService();
        registerService(context, IcmpEchoProvider.class.getName(),
                                            icmpEchoResponderService, Service.ICMP_ECHO);

        netvirtProvidersConfig.setDependencies(context, null);
        pipelineOrchestrator.setDependencies(context, null);
        outboundNatService.setDependencies(context, null);
        egressAclService.setDependencies(context, null);
        l2ForwardingService.setDependencies(context, null);
        l2RewriteService.setDependencies(context, null);
        l3ForwardingService.setDependencies(context, null);
        routingService.setDependencies(context, null);
        loadBalancerService.setDependencies(context, null);
        ingressAclService.setDependencies(context, null);
        inboundNatService.setDependencies(context, null);
        arpResponderService.setDependencies(context, null);
        classifierService.setDependencies(context, null);
        of13Provider.setDependencies(context, null);
        gatewayMacResolverService.setDependencies(context, null);
        icmpEchoResponderService.setDependencies(context, null);

        @SuppressWarnings("unchecked")
        ServiceTracker networkingProviderManagerTracker = new ServiceTracker(context,
                NetworkingProviderManager.class, null) {
            @Override
            public Object addingService(ServiceReference reference) {
                LOG.info("addingService NetworkingProviderManager");
                NetworkingProviderManager service =
                        (NetworkingProviderManager) context.getService(reference);
                if (service != null) {
                    of13Provider.setDependencies(service);
                }
                return service;
            }
        };
        networkingProviderManagerTracker.open();

        @SuppressWarnings("unchecked")
        ServiceTracker ConfigurationServiceTracker = new ServiceTracker(context,
                ConfigurationService.class, null) {
            @Override
            public Object addingService(ServiceReference reference) {
                LOG.info("addingService ConfigurationService");
                ConfigurationService service =
                        (ConfigurationService) context.getService(reference);
                if (service != null) {
                    gatewayMacResolverService.setDependencies(service);
                }
                return service;
            }
        };
        ConfigurationServiceTracker.open();

        @SuppressWarnings("unchecked")
        ServiceTracker NodeCacheManagerTracker = new ServiceTracker(context,
                NodeCacheManager.class, null) {
            @Override
            public Object addingService(ServiceReference reference) {
                LOG.info("addingService NodeCacheManager");
                NodeCacheManager service =
                        (NodeCacheManager) context.getService(reference);
                if (service != null) {
                    gatewayMacResolverService.setDependencies(service);
                }
                return service;
            }
        };
        NodeCacheManagerTracker.open();
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        LOG.info("ConfigActivator stop");
        // ServiceTrackers and services are already released when bundle stops,
        // so we don't need to close the trackers or unregister the services
    }

    private ServiceRegistration<?> registerService(BundleContext bundleContext, String[] interfaces,
                                                   Dictionary<String, Object> properties, Object impl) {
        ServiceRegistration<?> serviceRegistration = bundleContext.registerService(interfaces, impl, properties);
        if (serviceRegistration != null) {
            registrations.add(serviceRegistration);
        }
        return serviceRegistration;
    }

    private ServiceRegistration<?> registerService(BundleContext bundleContext, String interfaceClassName,
                                                   Object impl, Object serviceProperty) {
        Dictionary<String, Object> properties = new Hashtable<>();
        properties.put(AbstractServiceInstance.SERVICE_PROPERTY, serviceProperty);
        properties.put(Constants.PROVIDER_NAME_PROPERTY, OF13Provider.NAME);
        return registerService(bundleContext,
                new String[] {AbstractServiceInstance.class.getName(), interfaceClassName},
                properties, impl);
    }

    private NotificationProviderService getNotificationProviderService(){
        return this.providerContext.getSALService(NotificationProviderService.class);
    }
}
