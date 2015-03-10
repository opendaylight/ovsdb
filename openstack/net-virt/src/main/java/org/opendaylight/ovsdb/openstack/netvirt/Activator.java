/*
 * Copyright (C) 2013 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Madhu Venugopal, Brent Salisbury, Dave Tucker
 */

package org.opendaylight.ovsdb.openstack.netvirt;

import java.util.Dictionary;
import java.util.Hashtable;

import org.apache.felix.dm.DependencyActivatorBase;
import org.apache.felix.dm.DependencyManager;
import org.opendaylight.neutron.spi.INeutronFirewallAware;
import org.opendaylight.neutron.spi.INeutronFirewallPolicyAware;
import org.opendaylight.neutron.spi.INeutronFirewallRuleAware;
import org.opendaylight.neutron.spi.INeutronFloatingIPAware;
import org.opendaylight.neutron.spi.INeutronLoadBalancerAware;
import org.opendaylight.neutron.spi.INeutronLoadBalancerCRUD;
import org.opendaylight.neutron.spi.INeutronLoadBalancerPoolAware;
import org.opendaylight.neutron.spi.INeutronLoadBalancerPoolCRUD;
import org.opendaylight.neutron.spi.INeutronLoadBalancerPoolMemberAware;
import org.opendaylight.neutron.spi.INeutronNetworkAware;
import org.opendaylight.neutron.spi.INeutronNetworkCRUD;
import org.opendaylight.neutron.spi.INeutronPortAware;
import org.opendaylight.neutron.spi.INeutronPortCRUD;
import org.opendaylight.neutron.spi.INeutronRouterAware;
import org.opendaylight.neutron.spi.INeutronSecurityGroupAware;
import org.opendaylight.neutron.spi.INeutronSecurityRuleAware;
import org.opendaylight.neutron.spi.INeutronSubnetAware;
import org.opendaylight.neutron.spi.INeutronSubnetCRUD;
import org.opendaylight.controller.switchmanager.IInventoryListener;
import org.opendaylight.controller.switchmanager.ISwitchManager;
import org.opendaylight.ovsdb.openstack.netvirt.api.ArpProvider;
import org.opendaylight.ovsdb.openstack.netvirt.api.BridgeConfigurationManager;
import org.opendaylight.ovsdb.openstack.netvirt.api.ConfigurationService;
import org.opendaylight.ovsdb.openstack.netvirt.api.Constants;
import org.opendaylight.ovsdb.openstack.netvirt.api.EventDispatcher;
import org.opendaylight.ovsdb.openstack.netvirt.api.InboundNatProvider;
import org.opendaylight.ovsdb.openstack.netvirt.api.L3ForwardingProvider;
import org.opendaylight.ovsdb.openstack.netvirt.api.LoadBalancerProvider;
import org.opendaylight.ovsdb.openstack.netvirt.api.MultiTenantAwareRouter;
import org.opendaylight.ovsdb.openstack.netvirt.api.NetworkingProvider;
import org.opendaylight.ovsdb.openstack.netvirt.api.NetworkingProviderManager;
import org.opendaylight.ovsdb.openstack.netvirt.api.OutboundNatProvider;
import org.opendaylight.ovsdb.openstack.netvirt.api.RoutingProvider;
import org.opendaylight.ovsdb.openstack.netvirt.api.SecurityServicesManager;
import org.opendaylight.ovsdb.openstack.netvirt.api.TenantNetworkManager;
import org.opendaylight.ovsdb.openstack.netvirt.api.VlanConfigurationCache;
import org.opendaylight.ovsdb.openstack.netvirt.impl.BridgeConfigurationManagerImpl;
import org.opendaylight.ovsdb.openstack.netvirt.impl.ConfigurationServiceImpl;
import org.opendaylight.ovsdb.openstack.netvirt.impl.EventDispatcherImpl;
import org.opendaylight.ovsdb.openstack.netvirt.impl.NeutronL3Adapter;
import org.opendaylight.ovsdb.openstack.netvirt.impl.OpenstackRouter;
import org.opendaylight.ovsdb.openstack.netvirt.impl.ProviderNetworkManagerImpl;
import org.opendaylight.ovsdb.openstack.netvirt.impl.SecurityServicesImpl;
import org.opendaylight.ovsdb.openstack.netvirt.impl.TenantNetworkManagerImpl;
import org.opendaylight.ovsdb.openstack.netvirt.impl.VlanConfigurationCacheImpl;
import org.opendaylight.ovsdb.plugin.api.OvsdbConfigurationService;
import org.opendaylight.ovsdb.plugin.api.OvsdbConnectionService;
import org.opendaylight.ovsdb.plugin.api.OvsdbInventoryListener;

import org.osgi.framework.BundleContext;

/**
 * OSGi bundle activator for the OVSDB Neutron Interface.
 */
public class Activator extends DependencyActivatorBase {

    @Override
    public void init(BundleContext context, DependencyManager manager) throws Exception {

        manager.add(createComponent()
                .setInterface(ConfigurationService.class.getName(), null)
                .setImplementation(ConfigurationServiceImpl.class)
                .add(createServiceDependency().setService(OvsdbConfigurationService.class)));

        manager.add(createComponent()
                .setInterface(BridgeConfigurationManager.class.getName(), null)
                .setImplementation(BridgeConfigurationManagerImpl.class)
                .add(createServiceDependency().setService(ConfigurationService.class).setRequired(true))
                .add(createServiceDependency().setService(NetworkingProviderManager.class))
                .add(createServiceDependency().setService(OvsdbConfigurationService.class)));

        manager.add(createComponent()
                .setInterface(TenantNetworkManager.class.getName(), null)
                .setImplementation(TenantNetworkManagerImpl.class)
                .add(createServiceDependency().setService(NetworkingProviderManager.class))
                .add(createServiceDependency().setService(OvsdbConfigurationService.class))
                .add(createServiceDependency().setService(OvsdbConnectionService.class))
                .add(createServiceDependency().setService(INeutronNetworkCRUD.class).setRequired(true))
                .add(createServiceDependency().setService(INeutronPortCRUD.class).setRequired(true))
                .add(createServiceDependency().setService(VlanConfigurationCache.class)));

        manager.add(createComponent()
                .setInterface(VlanConfigurationCache.class.getName(), null)
                .setImplementation(VlanConfigurationCacheImpl.class)
                .add(createServiceDependency().setService(OvsdbConfigurationService.class))
                .add(createServiceDependency().setService(TenantNetworkManager.class)));

        Dictionary<String, Object> floatingIPHandlerPorperties = new Hashtable<>();
        floatingIPHandlerPorperties.put(Constants.EVENT_HANDLER_TYPE_PROPERTY,
                AbstractEvent.HandlerType.NEUTRON_FLOATING_IP);

        manager.add(createComponent()
                .setInterface(new String[]{INeutronFloatingIPAware.class.getName(), AbstractHandler.class.getName()},
                        floatingIPHandlerPorperties)
                .setImplementation(FloatingIPHandler.class)
                .add(createServiceDependency().setService(EventDispatcher.class).setRequired(true))
                .add(createServiceDependency().setService(NeutronL3Adapter.class).setRequired(true)));

        Dictionary<String, Object> networkHandlerProperties = new Hashtable<>();
        networkHandlerProperties.put(Constants.EVENT_HANDLER_TYPE_PROPERTY, AbstractEvent.HandlerType.NEUTRON_NETWORK);

        manager.add(createComponent()
                .setInterface(new String[]{INeutronNetworkAware.class.getName(), AbstractHandler.class.getName()},
                        networkHandlerProperties)
                .setImplementation(NetworkHandler.class)
                .add(createServiceDependency().setService(TenantNetworkManager.class).setRequired(true))
                .add(createServiceDependency().setService(BridgeConfigurationManager.class).setRequired(true))
                .add(createServiceDependency().setService(ConfigurationService.class).setRequired(true))
                .add(createServiceDependency().setService(OvsdbConfigurationService.class).setRequired(true))
                .add(createServiceDependency().setService(OvsdbConnectionService.class).setRequired(true))
                .add(createServiceDependency().setService(INeutronNetworkCRUD.class).setRequired(true))
                .add(createServiceDependency().setService(OvsdbInventoryListener.class).setRequired(true))
                .add(createServiceDependency().setService(EventDispatcher.class).setRequired(true))
                .add(createServiceDependency().setService(NeutronL3Adapter.class).setRequired(true)));

        Dictionary<String, Object> subnetHandlerProperties = new Hashtable<>();
        subnetHandlerProperties.put(Constants.EVENT_HANDLER_TYPE_PROPERTY, AbstractEvent.HandlerType.NEUTRON_SUBNET);

        manager.add(createComponent()
                .setInterface(new String[]{INeutronSubnetAware.class.getName(), AbstractHandler.class.getName()},
                        subnetHandlerProperties)
                .setImplementation(SubnetHandler.class)
                .add(createServiceDependency().setService(EventDispatcher.class).setRequired(true))
                .add(createServiceDependency().setService(NeutronL3Adapter.class).setRequired(true)));

        Dictionary<String, Object> portHandlerProperties = new Hashtable<>();
        portHandlerProperties.put(Constants.EVENT_HANDLER_TYPE_PROPERTY, AbstractEvent.HandlerType.NEUTRON_PORT);

        manager.add(createComponent()
                .setInterface(new String[]{INeutronPortAware.class.getName(), AbstractHandler.class.getName()},
                        portHandlerProperties)
                .setImplementation(PortHandler.class)
                .add(createServiceDependency().setService(OvsdbConfigurationService.class).setRequired(true))
                .add(createServiceDependency().setService(OvsdbConnectionService.class).setRequired(true))
                .add(createServiceDependency().setService(OvsdbInventoryListener.class).setRequired(true))
                .add(createServiceDependency().setService(EventDispatcher.class).setRequired(true))
                .add(createServiceDependency().setService(NeutronL3Adapter.class).setRequired(true)));

        Dictionary<String, Object> routerHandlerProperties = new Hashtable<>();
        routerHandlerProperties.put(Constants.EVENT_HANDLER_TYPE_PROPERTY, AbstractEvent.HandlerType.NEUTRON_ROUTER);

        manager.add(createComponent()
                .setInterface(new String[]{INeutronRouterAware.class.getName(), AbstractHandler.class.getName()},
                        routerHandlerProperties)
                .setImplementation(RouterHandler.class)
                .add(createServiceDependency().setService(EventDispatcher.class).setRequired(true))
                .add(createServiceDependency().setService(NeutronL3Adapter.class).setRequired(true)));

        Dictionary<String, Object> southboundHandlerProperties = new Hashtable<>();
        southboundHandlerProperties.put(Constants.EVENT_HANDLER_TYPE_PROPERTY, AbstractEvent.HandlerType.SOUTHBOUND);

        manager.add(createComponent()
                .setInterface(new String[]{OvsdbInventoryListener.class.getName(), IInventoryListener.class.getName(),
                                AbstractHandler.class.getName()},
                        southboundHandlerProperties)
                .setImplementation(SouthboundHandler.class)
                .add(createServiceDependency().setService(ConfigurationService.class).setRequired(true))
                .add(createServiceDependency().setService(BridgeConfigurationManager.class).setRequired(true))
                .add(createServiceDependency().setService(TenantNetworkManager.class).setRequired(true))
                .add(createServiceDependency().setService(NetworkingProviderManager.class).setRequired(true))
                .add(createServiceDependency().setService(OvsdbConfigurationService.class).setRequired(true))
                .add(createServiceDependency().setService(OvsdbConnectionService.class).setRequired(true))
                .add(createServiceDependency().setService(EventDispatcher.class).setRequired(true))
                .add(createServiceDependency().setService(NeutronL3Adapter.class).setRequired(true)));

        Dictionary<String, Object> lbaasHandlerProperties = new Hashtable<>();
        lbaasHandlerProperties.put(Constants.EVENT_HANDLER_TYPE_PROPERTY,
                AbstractEvent.HandlerType.NEUTRON_LOAD_BALANCER);

        manager.add(createComponent()
                .setInterface(new String[]{INeutronLoadBalancerAware.class.getName(),
                                IInventoryListener.class.getName(), AbstractHandler.class.getName()},
                        lbaasHandlerProperties)
                .setImplementation(LBaaSHandler.class)
                .add(createServiceDependency().setService(EventDispatcher.class).setRequired(true))
                .add(createServiceDependency().setService(INeutronPortCRUD.class).setRequired(true))
                .add(createServiceDependency().setService(INeutronLoadBalancerCRUD.class).setRequired(true))
                .add(createServiceDependency().setService(INeutronLoadBalancerPoolCRUD.class).setRequired(true))
                .add(createServiceDependency().setService(LoadBalancerProvider.class).setRequired(true))
                .add(createServiceDependency().setService(INeutronNetworkCRUD.class).setRequired(true))
                .add(createServiceDependency().setService(INeutronSubnetCRUD.class).setRequired(true)));

        Dictionary<String, Object> lbaasPoolHandlerProperties = new Hashtable<>();
        lbaasPoolHandlerProperties.put(Constants.EVENT_HANDLER_TYPE_PROPERTY,
                AbstractEvent.HandlerType.NEUTRON_LOAD_BALANCER_POOL);

        manager.add(createComponent()
                .setInterface(new String[]{INeutronLoadBalancerPoolAware.class.getName(),
                        AbstractHandler.class.getName()}, lbaasPoolHandlerProperties)
                .setImplementation(LBaaSPoolHandler.class)
                .add(createServiceDependency().setService(EventDispatcher.class).setRequired(true))
                .add(createServiceDependency().setService(INeutronPortCRUD.class).setRequired(true))
                .add(createServiceDependency().setService(INeutronLoadBalancerCRUD.class).setRequired(true))
                .add(createServiceDependency().setService(LoadBalancerProvider.class).setRequired(true))
                .add(createServiceDependency().setService(INeutronNetworkCRUD.class).setRequired(true))
                .add(createServiceDependency().setService(INeutronSubnetCRUD.class).setRequired(true)));

        Dictionary<String, Object> lbaasPoolMemberHandlerProperties = new Hashtable<>();
        lbaasPoolMemberHandlerProperties.put(Constants.EVENT_HANDLER_TYPE_PROPERTY,
                AbstractEvent.HandlerType.NEUTRON_LOAD_BALANCER_POOL_MEMBER);

        manager.add(createComponent()
                .setInterface(new String[] {INeutronLoadBalancerPoolMemberAware.class.getName(),
                        AbstractHandler.class.getName()}, lbaasPoolMemberHandlerProperties)
                .setImplementation(LBaaSPoolMemberHandler.class)
                .add(createServiceDependency().setService(EventDispatcher.class).setRequired(true))
                .add(createServiceDependency().setService(INeutronPortCRUD.class).setRequired(true))
                .add(createServiceDependency().setService(INeutronLoadBalancerCRUD.class).setRequired(true))
                .add(createServiceDependency().setService(INeutronLoadBalancerPoolCRUD.class).setRequired(true))
                .add(createServiceDependency().setService(LoadBalancerProvider.class).setRequired(true))
                .add(createServiceDependency().setService(INeutronNetworkCRUD.class).setRequired(true))
                .add(createServiceDependency().setService(INeutronSubnetCRUD.class).setRequired(true)));

        Dictionary<String, Object> portSecurityHandlerProperties = new Hashtable<>();
        portSecurityHandlerProperties.put(Constants.EVENT_HANDLER_TYPE_PROPERTY,
                AbstractEvent.HandlerType.NEUTRON_PORT_SECURITY);

        manager.add(createComponent()
                .setInterface(new String[]{INeutronSecurityRuleAware.class.getName(),
                                INeutronSecurityGroupAware.class.getName(), AbstractHandler.class.getName()},
                        portSecurityHandlerProperties)
                .setImplementation(PortSecurityHandler.class)
                .add(createServiceDependency().setService(EventDispatcher.class).setRequired(true))
                .add(createServiceDependency().setService(SecurityServicesManager.class).setRequired(true)));

        manager.add(createComponent()
                .setInterface(new String[]{SecurityServicesManager.class.getName()}, null)
                .setImplementation(SecurityServicesImpl.class)
                .add(createServiceDependency().setService(INeutronPortCRUD.class).setRequired(true)));

        Dictionary<String, Object> fWaasHandlerProperties = new Hashtable<>();
        fWaasHandlerProperties.put(Constants.EVENT_HANDLER_TYPE_PROPERTY, AbstractEvent.HandlerType.NEUTRON_FWAAS);

        manager.add(createComponent()
                .setInterface(new String[] {INeutronFirewallAware.class.getName(),
                                INeutronFirewallRuleAware.class.getName(), INeutronFirewallPolicyAware.class.getName(),
                                AbstractHandler.class.getName()}, fWaasHandlerProperties)
                .setImplementation(FWaasHandler.class)
                .add(createServiceDependency().setService(EventDispatcher.class).setRequired(true)));

        manager.add(createComponent()
                .setInterface(NetworkingProviderManager.class.getName(), null)
                .setImplementation(ProviderNetworkManagerImpl.class)
                .add(createServiceDependency().setService(ConfigurationService.class).setRequired(true))
                .add(createServiceDependency().setService(NetworkingProvider.class)
                        .setCallbacks("providerAdded", "providerRemoved")));

        manager.add(createComponent()
                .setInterface(EventDispatcher.class.getName(), null)
                .setImplementation(EventDispatcherImpl.class)
                .add(createServiceDependency()
                        .setService(AbstractHandler.class)
                        .setCallbacks("eventHandlerAdded", "eventHandlerRemoved")));

        manager.add(createComponent()
                .setInterface(NeutronL3Adapter.class.getName(), null)
                .setImplementation(NeutronL3Adapter.class)
                .add(createServiceDependency().setService(ConfigurationService.class).setRequired(true))
                .add(createServiceDependency().setService(TenantNetworkManager.class).setRequired(true))
                .add(createServiceDependency().setService(NetworkingProviderManager.class).setRequired(true))
                .add(createServiceDependency().setService(OvsdbConfigurationService.class).setRequired(true))
                .add(createServiceDependency().setService(OvsdbConnectionService.class).setRequired(true))
                .add(createServiceDependency().setService(INeutronNetworkCRUD.class).setRequired(true))
                .add(createServiceDependency().setService(INeutronSubnetCRUD.class).setRequired(true))
                .add(createServiceDependency().setService(INeutronPortCRUD.class).setRequired(true))
                .add(createServiceDependency().setService(MultiTenantAwareRouter.class).setRequired(true))
                /* ToDo, we should probably just use the NetworkingProvider interface
                 * This should provide a way of getting service implementations
                 * Either that, or we should do service lookup at runtime based on getProvider().getName()
                 * This is a shortcut as for now there will only be one implementation of these classes.
                 */
                .add(createServiceDependency().setService(ArpProvider.class).setRequired(false))
                .add(createServiceDependency().setService(InboundNatProvider.class).setRequired(false))
                .add(createServiceDependency().setService(OutboundNatProvider.class).setRequired(false))
                .add(createServiceDependency().setService(RoutingProvider.class).setRequired(false))
                .add(createServiceDependency().setService(L3ForwardingProvider.class).setRequired(false)));

        manager.add(createComponent()
                .setInterface(MultiTenantAwareRouter.class.getName(), null)
                .setImplementation(OpenstackRouter.class));
    }

    @Override
    public void destroy(BundleContext context, DependencyManager manager) throws Exception {
    }
}
