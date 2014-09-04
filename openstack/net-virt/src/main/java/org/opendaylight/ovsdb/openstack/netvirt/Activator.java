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

import org.opendaylight.controller.networkconfig.neutron.INeutronFirewallAware;
import org.opendaylight.controller.networkconfig.neutron.INeutronFirewallPolicyAware;
import org.opendaylight.controller.networkconfig.neutron.INeutronFirewallRuleAware;
import org.opendaylight.controller.networkconfig.neutron.INeutronFloatingIPAware;
import org.opendaylight.controller.networkconfig.neutron.INeutronLoadBalancerAware;
import org.opendaylight.controller.networkconfig.neutron.INeutronLoadBalancerCRUD;
import org.opendaylight.controller.networkconfig.neutron.INeutronLoadBalancerPoolCRUD;
import org.opendaylight.controller.networkconfig.neutron.INeutronLoadBalancerPoolMemberCRUD;
import org.opendaylight.controller.networkconfig.neutron.INeutronNetworkAware;
import org.opendaylight.controller.networkconfig.neutron.INeutronNetworkCRUD;
import org.opendaylight.controller.networkconfig.neutron.INeutronPortAware;
import org.opendaylight.controller.networkconfig.neutron.INeutronPortCRUD;
import org.opendaylight.controller.networkconfig.neutron.INeutronRouterAware;
import org.opendaylight.controller.networkconfig.neutron.INeutronSecurityGroupAware;
import org.opendaylight.controller.networkconfig.neutron.INeutronSecurityRuleAware;
import org.opendaylight.controller.networkconfig.neutron.INeutronSubnetCRUD;
import org.opendaylight.controller.networkconfig.neutron.INeutronSubnetAware;
import org.opendaylight.controller.sal.core.ComponentActivatorAbstractBase;
import org.opendaylight.controller.switchmanager.IInventoryListener;
import org.opendaylight.controller.switchmanager.ISwitchManager;
import org.opendaylight.ovsdb.openstack.netvirt.api.ArpProvider;
import org.opendaylight.ovsdb.openstack.netvirt.api.BridgeConfigurationManager;
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
import org.apache.felix.dm.Component;

import java.util.Properties;

/**
 * OSGi bundle activator for the OVSDB Neutron Interface.
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
        Object[] res = {ConfigurationServiceImpl.class,
                        BridgeConfigurationManagerImpl.class,
                        TenantNetworkManagerImpl.class,
                        VlanConfigurationCacheImpl.class,
                        FloatingIPHandler.class,
                        NetworkHandler.class,
                        SubnetHandler.class,
                        PortHandler.class,
                        RouterHandler.class,
                        SouthboundHandler.class,
                        PortSecurityHandler.class,
                        ProviderNetworkManagerImpl.class,
                        EventDispatcherImpl.class,
                        FWaasHandler.class,
                        LBaaSHandler.class,
                        LBaaSPoolMemberHandler.class,
                        NeutronL3Adapter.class,
                        OpenstackRouter.class,
                        SecurityServicesImpl.class};
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
        if (imp.equals(ConfigurationServiceImpl.class)) {
            c.setInterface(org.opendaylight.ovsdb.openstack.netvirt.api.ConfigurationService.class.getName(), null);
            c.add(createServiceDependency().setService(OvsdbConfigurationService.class));
        }

        if (imp.equals(BridgeConfigurationManagerImpl.class)) {
            c.setInterface(BridgeConfigurationManager.class.getName(), null);
            c.add(createServiceDependency().setService(
                    org.opendaylight.ovsdb.openstack.netvirt.api.ConfigurationService.class).setRequired(true));
            c.add(createServiceDependency().setService(NetworkingProviderManager.class));
            c.add(createServiceDependency().setService(OvsdbConfigurationService.class));
        }

        if (imp.equals(TenantNetworkManagerImpl.class)) {
            c.setInterface(TenantNetworkManager.class.getName(), null);
            c.add(createServiceDependency().setService(NetworkingProviderManager.class));
            c.add(createServiceDependency().setService(OvsdbConfigurationService.class));
            c.add(createServiceDependency().setService(OvsdbConnectionService.class));
            c.add(createServiceDependency().
                    setService(INeutronNetworkCRUD.class).
                    setRequired(true));
            c.add(createServiceDependency().
                    setService(INeutronPortCRUD.class).
                    setRequired(true));
            c.add(createServiceDependency().setService(VlanConfigurationCache.class));
        }

        if (imp.equals(VlanConfigurationCacheImpl.class)) {
            c.setInterface(VlanConfigurationCache.class.getName(), null);
            c.add(createServiceDependency().setService(OvsdbConfigurationService.class));
            c.add(createServiceDependency().setService(TenantNetworkManager.class));
        }

        if (imp.equals(FloatingIPHandler.class)) {
            Properties floatingIPHandlerPorperties = new Properties();
            floatingIPHandlerPorperties.put(Constants.EVENT_HANDLER_TYPE_PROPERTY,
                                            AbstractEvent.HandlerType.NEUTRON_FLOATING_IP);
            c.setInterface(new String[]{INeutronFloatingIPAware.class.getName(),
                                        AbstractHandler.class.getName()},
                           floatingIPHandlerPorperties);
            c.add(createServiceDependency().setService(EventDispatcher.class).setRequired(true));
            c.add(createServiceDependency().setService(NeutronL3Adapter.class).setRequired(true));
        }

        if (imp.equals(NetworkHandler.class)) {
            Properties networkHandlerProperties = new Properties();
            networkHandlerProperties.put(Constants.EVENT_HANDLER_TYPE_PROPERTY,
                                         AbstractEvent.HandlerType.NEUTRON_NETWORK);
            c.setInterface(new String[]{INeutronNetworkAware.class.getName(),
                                        AbstractHandler.class.getName()},
                           networkHandlerProperties);
            c.add(createServiceDependency().setService(TenantNetworkManager.class).setRequired(true));
            c.add(createServiceDependency().setService(BridgeConfigurationManager.class).setRequired(true));
            c.add(createServiceDependency().setService(
                    org.opendaylight.ovsdb.openstack.netvirt.api.ConfigurationService.class).setRequired(true));
            c.add(createServiceDependency().setService(OvsdbConfigurationService.class).setRequired(true));
            c.add(createServiceDependency().setService(OvsdbConnectionService.class).setRequired(true));
            c.add(createServiceDependency().setService(INeutronNetworkCRUD.class).setRequired(true));
            c.add(createServiceDependency().setService(OvsdbInventoryListener.class).setRequired(true));
            c.add(createServiceDependency().setService(EventDispatcher.class).setRequired(true));
            c.add(createServiceDependency().setService(NeutronL3Adapter.class).setRequired(true));
        }

        if (imp.equals(SubnetHandler.class)) {
            Properties subnetHandlerProperties = new Properties();
            subnetHandlerProperties.put(Constants.EVENT_HANDLER_TYPE_PROPERTY, AbstractEvent.HandlerType.NEUTRON_SUBNET);
            c.setInterface(new String[]{INeutronSubnetAware.class.getName(),
                                        AbstractHandler.class.getName()},
                           subnetHandlerProperties);
            c.add(createServiceDependency().setService(EventDispatcher.class).setRequired(true));
            c.add(createServiceDependency().setService(NeutronL3Adapter.class).setRequired(true));
        }

        if (imp.equals(PortHandler.class)) {
            Properties portHandlerProperties = new Properties();
            portHandlerProperties.put(Constants.EVENT_HANDLER_TYPE_PROPERTY, AbstractEvent.HandlerType.NEUTRON_PORT);
            c.setInterface(new String[]{INeutronPortAware.class.getName(),
                                        AbstractHandler.class.getName()},
                           portHandlerProperties);
            c.add(createServiceDependency().setService(OvsdbConfigurationService.class).setRequired(true));
            c.add(createServiceDependency().setService(OvsdbConnectionService.class).setRequired(true));
            c.add(createServiceDependency().setService(OvsdbInventoryListener.class).setRequired(true));
            c.add(createServiceDependency().setService(EventDispatcher.class).setRequired(true));
            c.add(createServiceDependency().setService(NeutronL3Adapter.class).setRequired(true));
        }

        if (imp.equals(RouterHandler.class)) {
            Properties routerHandlerProperties = new Properties();
            routerHandlerProperties.put(Constants.EVENT_HANDLER_TYPE_PROPERTY, AbstractEvent.HandlerType.NEUTRON_ROUTER);
            c.setInterface(new String[]{INeutronRouterAware.class.getName(),
                                        AbstractHandler.class.getName()},
                           routerHandlerProperties);
            c.add(createServiceDependency().setService(EventDispatcher.class).setRequired(true));
            c.add(createServiceDependency().setService(NeutronL3Adapter.class).setRequired(true));
        }

        if (imp.equals(SouthboundHandler.class)) {
            Properties southboundHandlerProperties = new Properties();
            southboundHandlerProperties.put(Constants.EVENT_HANDLER_TYPE_PROPERTY, AbstractEvent.HandlerType.SOUTHBOUND);
            c.setInterface(new String[]{OvsdbInventoryListener.class.getName(),
                                        IInventoryListener.class.getName(),
                                        AbstractHandler.class.getName()},
                           southboundHandlerProperties);
            c.add(createServiceDependency().setService(
                    org.opendaylight.ovsdb.openstack.netvirt.api.ConfigurationService.class).setRequired(true));
            c.add(createServiceDependency().setService(BridgeConfigurationManager.class).setRequired(true));
            c.add(createServiceDependency().setService(TenantNetworkManager.class).setRequired(true));
            c.add(createServiceDependency().setService(NetworkingProviderManager.class).setRequired(true));
            c.add(createServiceDependency().setService(OvsdbConfigurationService.class).setRequired(true));
            c.add(createServiceDependency().setService(OvsdbConnectionService.class).setRequired(true));
            c.add(createServiceDependency().setService(EventDispatcher.class).setRequired(true));
            c.add(createServiceDependency().setService(NeutronL3Adapter.class).setRequired(true));
        }

        if (imp.equals(LBaaSHandler.class)) {
            Properties lbaasHandlerProperties = new Properties();
            lbaasHandlerProperties.put(Constants.EVENT_HANDLER_TYPE_PROPERTY,
                    AbstractEvent.HandlerType.NEUTRON_LOAD_BALANCER);
            c.setInterface(new String[] {INeutronLoadBalancerAware.class.getName()},
                                         lbaasHandlerProperties);
            c.add(createServiceDependency().setService(EventDispatcher.class).setRequired(true));
            c.add(createServiceDependency().setService(INeutronPortCRUD.class).setRequired(true));
            c.add(createServiceDependency().setService(INeutronLoadBalancerCRUD.class).setRequired(true));
            c.add(createServiceDependency().setService(INeutronLoadBalancerPoolCRUD.class).setRequired(true));
            c.add(createServiceDependency().setService(INeutronLoadBalancerPoolMemberCRUD.class).setRequired(true));
            c.add(createServiceDependency().setService(LoadBalancerProvider.class).setRequired(true));
            c.add(createServiceDependency().setService(ISwitchManager.class).setRequired(true));
        }

        if (imp.equals(LBaaSPoolMemberHandler.class)) {
            Properties lbaasPoolMemberHandlerProperties = new Properties();
            lbaasPoolMemberHandlerProperties.put(Constants.EVENT_HANDLER_TYPE_PROPERTY,
                    AbstractEvent.HandlerType.NEUTRON_LOAD_BALANCER_POOL_MEMBER);
            c.setInterface(new String[] {INeutronLoadBalancerAware.class.getName()},
                                         lbaasPoolMemberHandlerProperties);
            c.add(createServiceDependency().setService(EventDispatcher.class).setRequired(true));
            c.add(createServiceDependency().setService(INeutronPortCRUD.class).setRequired(true));
            c.add(createServiceDependency().setService(INeutronLoadBalancerCRUD.class).setRequired(true));
            c.add(createServiceDependency().setService(INeutronLoadBalancerPoolCRUD.class).setRequired(true));
            c.add(createServiceDependency().setService(LoadBalancerProvider.class).setRequired(true));
            c.add(createServiceDependency().setService(ISwitchManager.class).setRequired(true));
        }

        if (imp.equals(PortSecurityHandler.class)) {
            Properties portSecurityHandlerProperties = new Properties();
            portSecurityHandlerProperties.put(Constants.EVENT_HANDLER_TYPE_PROPERTY,
                                              AbstractEvent.HandlerType.NEUTRON_PORT_SECURITY);
            c.setInterface(new String[]{INeutronSecurityRuleAware.class.getName(),
                                        INeutronSecurityGroupAware.class.getName(),
                            AbstractHandler.class.getName()},
                           portSecurityHandlerProperties);
            c.add(createServiceDependency().setService(EventDispatcher.class).setRequired(true));
            c.add(createServiceDependency().setService(SecurityServicesManager.class).setRequired(true));
        }

        if (imp.equals(SecurityServicesImpl.class)) {
            c.setInterface(new String[] {SecurityServicesManager.class.getName()}, null);
        }

        if (imp.equals(FWaasHandler.class)) {
            Properties fWaasHandlerProperties = new Properties();
            fWaasHandlerProperties.put(Constants.EVENT_HANDLER_TYPE_PROPERTY, AbstractEvent.HandlerType.NEUTRON_FWAAS);
            c.setInterface(new String[] {INeutronFirewallAware.class.getName(),
                            INeutronFirewallRuleAware.class.getName(),
                            INeutronFirewallPolicyAware.class.getName(),
                            AbstractHandler.class.getName()},
                    fWaasHandlerProperties);
            c.add(createServiceDependency().setService(EventDispatcher.class).setRequired(true));
        }

        if (imp.equals(ProviderNetworkManagerImpl.class)) {
            c.setInterface(NetworkingProviderManager.class.getName(), null);
            c.add(createServiceDependency()
                    .setService(org.opendaylight.ovsdb.openstack.netvirt.api.ConfigurationService.class)
                    .setRequired(true));
            c.add(createServiceDependency()
                    .setService(NetworkingProvider.class)
                    .setCallbacks("providerAdded", "providerRemoved"));
        }

        if (imp.equals(EventDispatcherImpl.class)) {
            c.setInterface(EventDispatcher.class.getName(), null);
            c.add(createServiceDependency()
                          .setService(AbstractHandler.class)
                          .setCallbacks("eventHandlerAdded", "eventHandlerRemoved"));
        }

        if (imp.equals(NeutronL3Adapter.class)) {
            c.setInterface(NeutronL3Adapter.class.getName(), null);
            c.add(createServiceDependency()
                          .setService(org.opendaylight.ovsdb.openstack.netvirt.api.ConfigurationService.class)
                          .setRequired(true));
            c.add(createServiceDependency().setService(TenantNetworkManager.class).setRequired(true));
            c.add(createServiceDependency().setService(NetworkingProviderManager.class).setRequired(true));
            c.add(createServiceDependency().setService(OvsdbConfigurationService.class).setRequired(true));
            c.add(createServiceDependency().setService(OvsdbConnectionService.class).setRequired(true));
            c.add(createServiceDependency().setService(INeutronNetworkCRUD.class).setRequired(true));
            c.add(createServiceDependency().setService(INeutronSubnetCRUD.class).setRequired(true));
            c.add(createServiceDependency().setService(INeutronPortCRUD.class).setRequired(true));
            c.add(createServiceDependency().setService(MultiTenantAwareRouter.class).setRequired(true));
            /* ToDo, we should probably just use the NetworkingProvider interface
             * This should provide a way of getting service implementations
             * Either that, or we should do service lookup at runtime based on getProvider().getName()
             * This is a shortcut as for now there will only be one implementation of these classes.
             */
            c.add(createServiceDependency().setService(MultiTenantAwareRouter.class).setRequired(false));
            c.add(createServiceDependency().setService(ArpProvider.class).setRequired(false));
            c.add(createServiceDependency().setService(InboundNatProvider.class).setRequired(false));
            c.add(createServiceDependency().setService(OutboundNatProvider.class).setRequired(false));
            c.add(createServiceDependency().setService(RoutingProvider.class).setRequired(false));
            c.add(createServiceDependency().setService(L3ForwardingProvider.class).setRequired(false));
        }

        if (imp.equals(OpenstackRouter.class)) {
            c.setInterface(MultiTenantAwareRouter.class.getName(), null);
        }
    }
}
