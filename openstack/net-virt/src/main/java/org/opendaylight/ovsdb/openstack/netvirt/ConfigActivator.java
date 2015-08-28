/*
 * Copyright (c) 2015 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.ovsdb.openstack.netvirt.translator.*;
import org.opendaylight.ovsdb.openstack.netvirt.translator.crud.INeutronLoadBalancerCRUD;
import org.opendaylight.ovsdb.openstack.netvirt.translator.crud.INeutronLoadBalancerPoolCRUD;
import org.opendaylight.ovsdb.openstack.netvirt.translator.crud.INeutronNetworkCRUD;
import org.opendaylight.ovsdb.openstack.netvirt.translator.crud.INeutronPortCRUD;
import org.opendaylight.ovsdb.openstack.netvirt.translator.crud.INeutronSubnetCRUD;
import org.opendaylight.ovsdb.openstack.netvirt.translator.iaware.INeutronFirewallAware;
import org.opendaylight.ovsdb.openstack.netvirt.translator.iaware.INeutronFirewallPolicyAware;
import org.opendaylight.ovsdb.openstack.netvirt.translator.iaware.INeutronFirewallRuleAware;
import org.opendaylight.ovsdb.openstack.netvirt.translator.iaware.INeutronFloatingIPAware;
import org.opendaylight.ovsdb.openstack.netvirt.translator.iaware.INeutronLoadBalancerAware;
import org.opendaylight.ovsdb.openstack.netvirt.translator.iaware.INeutronLoadBalancerPoolAware;
import org.opendaylight.ovsdb.openstack.netvirt.translator.iaware.INeutronLoadBalancerPoolMemberAware;
import org.opendaylight.ovsdb.openstack.netvirt.translator.iaware.INeutronNetworkAware;
import org.opendaylight.ovsdb.openstack.netvirt.translator.iaware.INeutronPortAware;
import org.opendaylight.ovsdb.openstack.netvirt.translator.iaware.INeutronRouterAware;
import org.opendaylight.ovsdb.openstack.netvirt.translator.iaware.INeutronSecurityGroupAware;
import org.opendaylight.ovsdb.openstack.netvirt.translator.iaware.INeutronSecurityRuleAware;
import org.opendaylight.ovsdb.openstack.netvirt.translator.iaware.INeutronSubnetAware;
import org.opendaylight.ovsdb.openstack.netvirt.api.*;
import org.opendaylight.ovsdb.openstack.netvirt.impl.*;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigActivator implements BundleActivator {
    private static final Logger LOG = LoggerFactory.getLogger(ConfigActivator.class);
    private List<ServiceRegistration<?>> registrations = new ArrayList<>();
    private List<Object> services = new ArrayList<>();
    private ProviderContext providerContext;

    public ConfigActivator(ProviderContext providerContext) {
        this.providerContext = providerContext;
    }

    @Override
    public void start(BundleContext context) throws Exception {
        LOG.info("ConfigActivator start:");

        ConfigurationServiceImpl configurationService = new ConfigurationServiceImpl();
        registerService(context, new String[] {ConfigurationService.class.getName()},
                null, configurationService);

        BridgeConfigurationManagerImpl bridgeConfigurationManager = new BridgeConfigurationManagerImpl();
        registerService(context, new String[] {BridgeConfigurationManager.class.getName()},
                null, bridgeConfigurationManager);

        final TenantNetworkManagerImpl tenantNetworkManager = new TenantNetworkManagerImpl();
        registerService(context, new String[] {TenantNetworkManager.class.getName()},
                null, tenantNetworkManager);

        VlanConfigurationCacheImpl vlanConfigurationCache = new VlanConfigurationCacheImpl();
        registerService(context, new String[] {VlanConfigurationCache.class.getName()},
                null, vlanConfigurationCache);

        FloatingIPHandler floatingIPHandler = new FloatingIPHandler();
        registerAbstractHandlerService(context, new Class[] {INeutronFloatingIPAware.class},
                AbstractEvent.HandlerType.NEUTRON_FLOATING_IP, floatingIPHandler);

        final NetworkHandler networkHandler = new NetworkHandler();
        registerAbstractHandlerService(context, new Class[] {INeutronNetworkAware.class},
                AbstractEvent.HandlerType.NEUTRON_NETWORK, networkHandler);

        SubnetHandler subnetHandler = new SubnetHandler();
        registerAbstractHandlerService(context, new Class[] {INeutronSubnetAware.class},
                AbstractEvent.HandlerType.NEUTRON_SUBNET, subnetHandler);

        PortHandler portHandler = new PortHandler();
        registerAbstractHandlerService(context, new Class[] {INeutronPortAware.class},
                AbstractEvent.HandlerType.NEUTRON_PORT, portHandler);

        RouterHandler routerHandler = new RouterHandler();
        registerAbstractHandlerService(context, new Class[] {INeutronRouterAware.class},
                AbstractEvent.HandlerType.NEUTRON_ROUTER, routerHandler);

        SouthboundHandler southboundHandler = new SouthboundHandler();
        registerAbstractHandlerService(context, new Class[] {OvsdbInventoryListener.class, NodeCacheListener.class},
                AbstractEvent.HandlerType.SOUTHBOUND, southboundHandler);

        final LBaaSHandler lBaaSHandler = new LBaaSHandler();
        registerAbstractHandlerService(context, new Class[] {INeutronLoadBalancerAware.class, NodeCacheListener.class},
                AbstractEvent.HandlerType.NEUTRON_LOAD_BALANCER, lBaaSHandler);

        final LBaaSPoolHandler lBaaSPoolHandler = new LBaaSPoolHandler();
        registerAbstractHandlerService(context, new Class[] {INeutronLoadBalancerPoolAware.class},
                AbstractEvent.HandlerType.NEUTRON_LOAD_BALANCER_POOL, lBaaSPoolHandler);

        final LBaaSPoolMemberHandler lBaaSPoolMemberHandler = new LBaaSPoolMemberHandler();
        registerAbstractHandlerService(context, new Class[] {INeutronLoadBalancerPoolMemberAware.class},
                AbstractEvent.HandlerType.NEUTRON_LOAD_BALANCER_POOL_MEMBER, lBaaSPoolMemberHandler);

        PortSecurityHandler portSecurityHandler = new PortSecurityHandler();
        registerAbstractHandlerService(context,
                new Class[] {INeutronSecurityRuleAware.class, INeutronSecurityGroupAware.class},
                AbstractEvent.HandlerType.NEUTRON_PORT_SECURITY, portSecurityHandler);

        final SecurityServicesImpl securityServices = new SecurityServicesImpl();
        registerService(context,
                new String[]{SecurityServicesManager.class.getName()}, null, securityServices);

        FWaasHandler fWaasHandler = new FWaasHandler();
        registerAbstractHandlerService(context,
                new Class[] {INeutronFirewallAware.class, INeutronFirewallRuleAware.class, INeutronFirewallPolicyAware.class},
                AbstractEvent.HandlerType.NEUTRON_FWAAS, fWaasHandler);

        ProviderNetworkManagerImpl providerNetworkManager = new ProviderNetworkManagerImpl();
        registerService(context,
                new String[]{NetworkingProviderManager.class.getName()}, null, providerNetworkManager);

        EventDispatcherImpl eventDispatcher = new EventDispatcherImpl();
        registerService(context,
                new String[]{EventDispatcher.class.getName()}, null, eventDispatcher);

        final NeutronL3Adapter neutronL3Adapter = new NeutronL3Adapter();
        registerService(context,
                new String[]{NeutronL3Adapter.class.getName()}, null, neutronL3Adapter);

        OpenstackRouter openstackRouter = new OpenstackRouter();
        registerService(context,
                new String[]{MultiTenantAwareRouter.class.getName()}, null, openstackRouter);

        Southbound southbound = new SouthboundImpl(providerContext.getSALService(DataBroker.class));
        registerService(context,
                new String[]{Southbound.class.getName()}, null, southbound);

        NodeCacheManagerImpl nodeCacheManager = new NodeCacheManagerImpl();
        registerAbstractHandlerService(context, new Class[] {NodeCacheManager.class},
                AbstractEvent.HandlerType.NODE, nodeCacheManager);

        OvsdbInventoryServiceImpl ovsdbInventoryService = new OvsdbInventoryServiceImpl(providerContext);
        registerService(context,
                new String[] {OvsdbInventoryService.class.getName()}, null, ovsdbInventoryService);

        // Call .setDependencies() starting with the last service registered
        for (int i = services.size() - 1; i >= 0; i--) {
            Object service = services.get(i);
            if (service instanceof ConfigInterface) {
                ((ConfigInterface) service).setDependencies(context, null);
            }
        }

        // TODO check if services are already available and setDependencies
        // addingService may not be called if the service is already available when the ServiceTracker
        // is started
        trackService(context, INeutronNetworkCRUD.class, tenantNetworkManager, networkHandler, lBaaSHandler,
                lBaaSPoolHandler, lBaaSPoolMemberHandler, neutronL3Adapter);
        trackService(context, INeutronSubnetCRUD.class, lBaaSHandler, lBaaSPoolHandler, lBaaSPoolMemberHandler,
                securityServices, neutronL3Adapter);
        trackService(context, INeutronPortCRUD.class, tenantNetworkManager, lBaaSHandler, lBaaSPoolHandler,
                lBaaSPoolMemberHandler, securityServices, neutronL3Adapter);
        trackService(context, INeutronLoadBalancerCRUD.class, lBaaSHandler, lBaaSPoolHandler, lBaaSPoolMemberHandler);
        trackService(context, INeutronLoadBalancerPoolCRUD.class, lBaaSHandler, lBaaSPoolMemberHandler);
        trackService(context, LoadBalancerProvider.class, lBaaSHandler, lBaaSPoolHandler, lBaaSPoolMemberHandler);
        trackService(context, ArpProvider.class, neutronL3Adapter);
        trackService(context, InboundNatProvider.class, neutronL3Adapter);
        trackService(context, OutboundNatProvider.class, neutronL3Adapter);
        trackService(context, RoutingProvider.class, neutronL3Adapter);
        trackService(context, L3ForwardingProvider.class, neutronL3Adapter);
        trackService(context, GatewayMacResolver.class, neutronL3Adapter);

        // We no longer need to track the services, avoid keeping references around
        services.clear();
    }

    private void trackService(BundleContext context, final Class<?> clazz, final ConfigInterface... dependents) {
        @SuppressWarnings("unchecked")
        ServiceTracker tracker = new ServiceTracker(context, clazz, null) {
            @Override
            public Object addingService(ServiceReference reference) {
                LOG.info("addingService " + clazz.getName());
                Object service = context.getService(reference);
                if (service != null) {
                    for (ConfigInterface dependent : dependents) {
                        dependent.setDependencies(service);
                    }
                }
                return service;
            }
        };
        tracker.open();
    }

    private void registerAbstractHandlerService(BundleContext context, Class[] interfaces,
                                                AbstractEvent.HandlerType handlerType, AbstractHandler handler) {
        Dictionary<String, Object> properties = new Hashtable<>();
        properties.put(Constants.EVENT_HANDLER_TYPE_PROPERTY, handlerType);
        String[] interfaceNames = new String[interfaces.length + 1];
        for (int i = 0; i < interfaces.length; i++) {
            interfaceNames[i] = interfaces[i].getName();
        }
        interfaceNames[interfaces.length] = AbstractHandler.class.getName();
        registerService(context, interfaceNames, properties, handler);
    }


    @Override
    public void stop(BundleContext context) throws Exception {
        LOG.info("ConfigActivator stop");
        // ServiceTrackers and services are already released when bundle stops,
        // so we don't need to close the trackers or unregister the services
    }

    private ServiceRegistration<?> registerService(BundleContext bundleContext, String[] interfaces,
                                                   Dictionary<String, Object> properties, Object impl) {
        services.add(impl);
        ServiceRegistration<?> serviceRegistration = bundleContext.registerService(interfaces, impl, properties);
        if (serviceRegistration != null) {
            registrations.add(serviceRegistration);
        }
        return serviceRegistration;
    }
}
