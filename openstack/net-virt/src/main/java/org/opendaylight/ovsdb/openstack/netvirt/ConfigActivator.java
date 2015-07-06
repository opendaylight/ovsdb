package org.opendaylight.ovsdb.openstack.netvirt;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.neutron.spi.*;
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
    private List<ServiceRegistration<?>> registrations = new ArrayList<ServiceRegistration<?>>();
    private ProviderContext providerContext;
    private ServiceTracker INeutronNetworkCRUDTracker;
    private ServiceTracker INeutronPortCRUDTracker;
    private ServiceTracker INeutronLoadBalancerCRUDTracker;
    private ServiceTracker INeutronLoadBalancerPoolCRUDTracker;
    private ServiceTracker INeutronSubnetCRUDTracker;
    private ServiceTracker LoadBalancerProviderTracker;
    private ServiceTracker ArpProviderTracker;
    private ServiceTracker InboundNatProviderTracker;
    private ServiceTracker OutboundNatProviderTracker;
    private ServiceTracker RoutingProviderTracker;
    private ServiceTracker L3ForwardingProviderTracker;

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

        Dictionary<String, Object> floatingIPHandlerProperties = new Hashtable<>();
        floatingIPHandlerProperties.put(Constants.EVENT_HANDLER_TYPE_PROPERTY,
                AbstractEvent.HandlerType.NEUTRON_FLOATING_IP);
        FloatingIPHandler floatingIPHandler = new FloatingIPHandler();
        registerService(context,
                new String[] {INeutronFloatingIPAware.class.getName(), AbstractHandler.class.getName()},
                floatingIPHandlerProperties, floatingIPHandler);

        Dictionary<String, Object> networkHandlerProperties = new Hashtable<>();
        networkHandlerProperties.put(Constants.EVENT_HANDLER_TYPE_PROPERTY, AbstractEvent.HandlerType.NEUTRON_NETWORK);
        final NetworkHandler networkHandler = new NetworkHandler();
        registerService(context,
                new String[]{INeutronNetworkAware.class.getName(), AbstractHandler.class.getName()},
                networkHandlerProperties, networkHandler);

        Dictionary<String, Object> subnetHandlerProperties = new Hashtable<>();
        subnetHandlerProperties.put(Constants.EVENT_HANDLER_TYPE_PROPERTY, AbstractEvent.HandlerType.NEUTRON_SUBNET);
        SubnetHandler subnetHandler = new SubnetHandler();
        registerService(context,
                new String[] {INeutronSubnetAware.class.getName(), AbstractHandler.class.getName()},
                subnetHandlerProperties, subnetHandler);

        Dictionary<String, Object> portHandlerProperties = new Hashtable<>();
        portHandlerProperties.put(Constants.EVENT_HANDLER_TYPE_PROPERTY, AbstractEvent.HandlerType.NEUTRON_PORT);
        PortHandler portHandler = new PortHandler();
        registerService(context,
                new String[]{INeutronPortAware.class.getName(), AbstractHandler.class.getName()},
                portHandlerProperties, portHandler);

        Dictionary<String, Object> routerHandlerProperties = new Hashtable<>();
        routerHandlerProperties.put(Constants.EVENT_HANDLER_TYPE_PROPERTY, AbstractEvent.HandlerType.NEUTRON_ROUTER);
        RouterHandler routerHandler = new RouterHandler();
        registerService(context,
                new String[]{INeutronRouterAware.class.getName(), AbstractHandler.class.getName()},
                routerHandlerProperties, routerHandler);

        Dictionary<String, Object> southboundHandlerProperties = new Hashtable<>();
        southboundHandlerProperties.put(Constants.EVENT_HANDLER_TYPE_PROPERTY, AbstractEvent.HandlerType.SOUTHBOUND);
        SouthboundHandler southboundHandler = new SouthboundHandler();
        registerService(context,
                new String[]{OvsdbInventoryListener.class.getName(),
                        NodeCacheListener.class.getName(),
                        AbstractHandler.class.getName()},
                southboundHandlerProperties, southboundHandler);

        Dictionary<String, Object> lbaasHandlerProperties = new Hashtable<>();
        lbaasHandlerProperties.put(Constants.EVENT_HANDLER_TYPE_PROPERTY,
                AbstractEvent.HandlerType.NEUTRON_LOAD_BALANCER);
        final LBaaSHandler lBaaSHandler = new LBaaSHandler();
        registerService(context,
                new String[]{INeutronLoadBalancerAware.class.getName(),
                        NodeCacheListener.class.getName(), AbstractHandler.class.getName()},
                lbaasHandlerProperties, lBaaSHandler);

        Dictionary<String, Object> lbaasPoolHandlerProperties = new Hashtable<>();
        lbaasPoolHandlerProperties.put(Constants.EVENT_HANDLER_TYPE_PROPERTY,
                AbstractEvent.HandlerType.NEUTRON_LOAD_BALANCER_POOL);
        final LBaaSPoolHandler lBaaSPoolHandler = new LBaaSPoolHandler();
        registerService(context,
                new String[]{INeutronLoadBalancerPoolAware.class.getName(),
                        AbstractHandler.class.getName()}, lbaasPoolHandlerProperties, lBaaSPoolHandler);

        Dictionary<String, Object> lbaasPoolMemberHandlerProperties = new Hashtable<>();
        lbaasPoolMemberHandlerProperties.put(Constants.EVENT_HANDLER_TYPE_PROPERTY,
                AbstractEvent.HandlerType.NEUTRON_LOAD_BALANCER_POOL_MEMBER);
        final LBaaSPoolMemberHandler lBaaSPoolMemberHandler = new LBaaSPoolMemberHandler();
        registerService(context,
                new String[]{INeutronLoadBalancerPoolMemberAware.class.getName(),
                        AbstractHandler.class.getName()}, lbaasPoolMemberHandlerProperties, lBaaSPoolMemberHandler);

        Dictionary<String, Object> portSecurityHandlerProperties = new Hashtable<>();
        portSecurityHandlerProperties.put(Constants.EVENT_HANDLER_TYPE_PROPERTY,
                AbstractEvent.HandlerType.NEUTRON_PORT_SECURITY);
        PortSecurityHandler portSecurityHandler = new PortSecurityHandler();
        registerService(context,
                new String[]{INeutronSecurityRuleAware.class.getName(),
                        INeutronSecurityGroupAware.class.getName(), AbstractHandler.class.getName()},
                portSecurityHandlerProperties, portSecurityHandler);

        final SecurityServicesImpl securityServices = new SecurityServicesImpl();
        registerService(context,
                new String[]{SecurityServicesManager.class.getName()}, null, securityServices);

        Dictionary<String, Object> fWaasHandlerProperties = new Hashtable<>();
        fWaasHandlerProperties.put(Constants.EVENT_HANDLER_TYPE_PROPERTY, AbstractEvent.HandlerType.NEUTRON_FWAAS);
        FWaasHandler fWaasHandler = new FWaasHandler();
        registerService(context,
                new String[]{INeutronFirewallAware.class.getName(),
                        INeutronFirewallRuleAware.class.getName(), INeutronFirewallPolicyAware.class.getName(),
                        AbstractHandler.class.getName()}, fWaasHandlerProperties, fWaasHandler);

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

        Dictionary<String, Object> nodeCacheManagerProperties = new Hashtable<>();
        nodeCacheManagerProperties.put(Constants.EVENT_HANDLER_TYPE_PROPERTY, AbstractEvent.HandlerType.NODE);
        NodeCacheManagerImpl nodeCacheManager = new NodeCacheManagerImpl();
        registerService(context,
                new String[]{NodeCacheManager.class.getName(), AbstractHandler.class.getName()},
                nodeCacheManagerProperties, nodeCacheManager);

        OvsdbInventoryServiceImpl ovsdbInventoryService = new OvsdbInventoryServiceImpl(providerContext);
        registerService(context,
                new String[] {OvsdbInventoryService.class.getName()}, null, ovsdbInventoryService);

        ovsdbInventoryService.setDependencies(context, null);
        nodeCacheManager.setDependencies(context, null);
        openstackRouter.setDependencies(context, null);
        neutronL3Adapter.setDependencies(context, null);
        eventDispatcher.setDependencies(context, null);
        providerNetworkManager.setDependencies(context, null);
        fWaasHandler.setDependencies(context, null);
        securityServices.setDependencies(context, null);
        portSecurityHandler.setDependencies(context, null);
        lBaaSPoolMemberHandler.setDependencies(context, null);
        lBaaSPoolHandler.setDependencies(context, null);
        lBaaSHandler.setDependencies(context, null);
        southboundHandler.setDependencies(context, null);
        routerHandler.setDependencies(context, null);
        portHandler.setDependencies(context, null);
        subnetHandler.setDependencies(context, null);
        networkHandler.setDependencies(context, null);
        floatingIPHandler.setDependencies(context, null);
        vlanConfigurationCache.setDependencies(context, null);
        tenantNetworkManager.setDependencies(context, null);
        bridgeConfigurationManager.setDependencies(context, null);
        configurationService.setDependencies(context, null);

        // TODO check if services are already available and setDependencies
        // addingService may not be called if the service is already available when the ServiceTracker
        // is started
        @SuppressWarnings("unchecked")
        ServiceTracker INeutronNetworkCRUDTracker = new ServiceTracker(context, INeutronNetworkCRUD.class, null) {
            @Override
            public Object addingService(ServiceReference reference) {
                LOG.info("addingService INeutronNetworkCRUD");
                INeutronNetworkCRUD service = (INeutronNetworkCRUD)context.getService(reference);
                if (service != null) {
                    tenantNetworkManager.setDependencies(service);
                    networkHandler.setDependencies(service);
                    lBaaSHandler.setDependencies(service);
                    lBaaSPoolHandler.setDependencies(service);
                    lBaaSPoolMemberHandler.setDependencies(service);
                    neutronL3Adapter.setDependencies(service);
                }
                return service;
            }

            /*@Override
            public void modifiedService(ServiceReference reference, Object service) {
                LOG.info("modifiedService INeutronNetworkCRUD");
                if (service != null) {
                    tenantNetworkManager.setDependencies(service);
                    networkHandler.setDependencies(service);
                    lBaaSHandler.setDependencies(service);
                    lBaaSPoolHandler.setDependencies(service);
                    lBaaSPoolMemberHandler.setDependencies(service);
                    neutronL3Adapter.setDependencies(service);
                }
            }*/

        };
        INeutronNetworkCRUDTracker.open();
        this.INeutronNetworkCRUDTracker = INeutronNetworkCRUDTracker;

        @SuppressWarnings("unchecked")
        ServiceTracker INeutronSubnetCRUDTracker = new ServiceTracker(context, INeutronSubnetCRUD.class, null) {
            @Override
            public Object addingService(ServiceReference reference) {
                LOG.info("addingService INeutronSubnetCRUD");
                INeutronSubnetCRUD service = (INeutronSubnetCRUD) context.getService(reference);
                if (service != null) {
                    lBaaSHandler.setDependencies(service);
                    lBaaSPoolHandler.setDependencies(service);
                    lBaaSPoolMemberHandler.setDependencies(service);
                    securityServices.setDependencies(service);
                    neutronL3Adapter.setDependencies(service);
                }
                return service;
            }
        };
        INeutronSubnetCRUDTracker.open();
        this.INeutronSubnetCRUDTracker = INeutronSubnetCRUDTracker;

        @SuppressWarnings("unchecked")
        ServiceTracker INeutronPortCRUDTracker = new ServiceTracker(context, INeutronPortCRUD.class, null) {
            @Override
            public Object addingService(ServiceReference reference) {
                LOG.info("addingService INeutronPortCRUD");
                INeutronPortCRUD service = (INeutronPortCRUD) context.getService(reference);
                if (service != null) {
                    tenantNetworkManager.setDependencies(service);
                    lBaaSHandler.setDependencies(service);
                    lBaaSPoolHandler.setDependencies(service);
                    lBaaSPoolMemberHandler.setDependencies(service);
                    securityServices.setDependencies(service);
                    neutronL3Adapter.setDependencies(service);
                }
                return service;
            }
        };
        INeutronPortCRUDTracker.open();
        this.INeutronPortCRUDTracker = INeutronPortCRUDTracker;

        @SuppressWarnings("unchecked")
        ServiceTracker INeutronLoadBalancerCRUDTracker = new ServiceTracker(context,
                INeutronLoadBalancerCRUD.class, null) {
            @Override
            public Object addingService(ServiceReference reference) {
                LOG.info("addingService INeutronLoadBalancerCRUD");
                INeutronLoadBalancerCRUD service = (INeutronLoadBalancerCRUD) context.getService(reference);
                if (service != null) {
                    lBaaSHandler.setDependencies(service);
                    lBaaSPoolHandler.setDependencies(service);
                    lBaaSPoolMemberHandler.setDependencies(service);
                }
                return service;
            }
        };
        INeutronLoadBalancerCRUDTracker.open();
        this.INeutronLoadBalancerCRUDTracker = INeutronLoadBalancerCRUDTracker;

        @SuppressWarnings("unchecked")
        ServiceTracker INeutronLoadBalancerPoolCRUDTracker = new ServiceTracker(context,
                INeutronLoadBalancerPoolCRUD.class, null) {
            @Override
            public Object addingService(ServiceReference reference) {
                LOG.info("addingService INeutronLoadBalancerPoolCRUD");
                INeutronLoadBalancerPoolCRUD service =
                        (INeutronLoadBalancerPoolCRUD) context.getService(reference);
                if (service != null) {
                    lBaaSHandler.setDependencies(service);
                    lBaaSPoolMemberHandler.setDependencies(service);
                }
                return service;
            }
        };
        INeutronLoadBalancerPoolCRUDTracker.open();
        this.INeutronLoadBalancerPoolCRUDTracker = INeutronLoadBalancerPoolCRUDTracker;

        @SuppressWarnings("unchecked")
        ServiceTracker LoadBalancerProviderTracker = new ServiceTracker(context,
                LoadBalancerProvider.class, null) {
            @Override
            public Object addingService(ServiceReference reference) {
                LOG.info("addingService LoadBalancerProvider");
                LoadBalancerProvider service =
                        (LoadBalancerProvider) context.getService(reference);
                if (service != null) {
                    lBaaSHandler.setDependencies(service);
                    lBaaSPoolHandler.setDependencies(service);
                    lBaaSPoolMemberHandler.setDependencies(service);
                }
                return service;
            }
        };
        LoadBalancerProviderTracker.open();
        this.LoadBalancerProviderTracker = LoadBalancerProviderTracker;

        @SuppressWarnings("unchecked")
        ServiceTracker ArpProviderTracker = new ServiceTracker(context,
                ArpProvider.class, null) {
            @Override
            public Object addingService(ServiceReference reference) {
                LOG.info("addingService ArpProvider");
                ArpProvider service =
                        (ArpProvider) context.getService(reference);
                if (service != null) {
                    neutronL3Adapter.setDependencies(service);
                }
                return service;
            }
        };
        ArpProviderTracker.open();
        this.ArpProviderTracker = ArpProviderTracker;

        @SuppressWarnings("unchecked")
        ServiceTracker InboundNatProviderTracker = new ServiceTracker(context,
                InboundNatProvider.class, null) {
            @Override
            public Object addingService(ServiceReference reference) {
                LOG.info("addingService InboundNatProvider");
                InboundNatProvider service =
                        (InboundNatProvider) context.getService(reference);
                if (service != null) {
                    neutronL3Adapter.setDependencies(service);
                }
                return service;
            }
        };
        InboundNatProviderTracker.open();
        this.InboundNatProviderTracker = InboundNatProviderTracker;

        @SuppressWarnings("unchecked")
        ServiceTracker OutboundNatProviderTracker = new ServiceTracker(context,
                OutboundNatProvider.class, null) {
            @Override
            public Object addingService(ServiceReference reference) {
                LOG.info("addingService OutboundNatProvider");
                OutboundNatProvider service =
                        (OutboundNatProvider) context.getService(reference);
                if (service != null) {
                    neutronL3Adapter.setDependencies(service);
                }
                return service;
            }
        };
        OutboundNatProviderTracker.open();
        this.OutboundNatProviderTracker = OutboundNatProviderTracker;

        @SuppressWarnings("unchecked")
        ServiceTracker RoutingProviderTracker = new ServiceTracker(context,
                RoutingProvider.class, null) {
            @Override
            public Object addingService(ServiceReference reference) {
                LOG.info("addingService RoutingProvider");
                RoutingProvider service =
                        (RoutingProvider) context.getService(reference);
                if (service != null) {
                    neutronL3Adapter.setDependencies(service);
                }
                return service;
            }
        };
        RoutingProviderTracker.open();
        this.RoutingProviderTracker = RoutingProviderTracker;

        @SuppressWarnings("unchecked")
        ServiceTracker L3ForwardingProviderTracker = new ServiceTracker(context,
                L3ForwardingProvider.class, null) {
            @Override
            public Object addingService(ServiceReference reference) {
                LOG.info("addingService L3ForwardingProvider");
                L3ForwardingProvider service =
                        (L3ForwardingProvider) context.getService(reference);
                if (service != null) {
                    neutronL3Adapter.setDependencies(service);
                }
                return service;
            }
        };
        L3ForwardingProviderTracker.open();
        this.L3ForwardingProviderTracker = L3ForwardingProviderTracker;
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        LOG.info("ConfigActivator stop");
        /* ServiceTrackers and services are already released when bundle stops
        INeutronNetworkCRUDTracker.close();
        INeutronPortCRUDTracker.close();
        INeutronSubnetCRUDTracker.close();
        INeutronLoadBalancerCRUDTracker.close();
        INeutronLoadBalancerPoolCRUDTracker.close();
        LoadBalancerProviderTracker.close();

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
}
