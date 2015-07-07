package org.opendaylight.ovsdb.openstack.netvirt.providers;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.ovsdb.openstack.netvirt.api.*;
import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.AbstractServiceInstance;
import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.OF13Provider;
import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.PipelineOrchestrator;
import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.PipelineOrchestratorImpl;
import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.Service;
import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.services.*;
import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.services.arp.GatewayMacResolverService;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigActivator implements BundleActivator {
    private static final Logger LOG = LoggerFactory.getLogger(ConfigActivator.class);
    private List<ServiceRegistration<?>> registrations = new ArrayList<ServiceRegistration<?>>();
    private ProviderContext providerContext;
    private ServiceTracker NetworkingProviderManagerTracker;

    public ConfigActivator(ProviderContext providerContext) {
        this.providerContext = providerContext;
    }

    @Override
    public void start(BundleContext context) throws Exception {
        LOG.info("ConfigActivator start:");

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

        GatewayMacResolverService gatewayMacResolverService = new GatewayMacResolverService();
        registerService(context, GatewayMacResolver.class.getName(),
                gatewayMacResolverService, Service.GATEWAY_RESOLVER);


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

        @SuppressWarnings("unchecked")
        ServiceTracker NetworkingProviderManagerTracker = new ServiceTracker(context,
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
        NetworkingProviderManagerTracker.open();
        this.NetworkingProviderManagerTracker = NetworkingProviderManagerTracker;
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        LOG.info("ConfigActivator stop");
        /* ServiceTrackers and services are already released when bundle stops
        NetworkingProviderManagerTracker.close();
        for (ServiceRegistration registration : registrations) {
            if (registration != null) {
                registration.unregister();
            }
        }*/
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
}
