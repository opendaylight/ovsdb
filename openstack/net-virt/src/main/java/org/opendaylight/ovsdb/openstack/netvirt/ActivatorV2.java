package org.opendaylight.ovsdb.openstack.netvirt;

import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import org.opendaylight.neutron.spi.INeutronFirewallAware;
import org.opendaylight.neutron.spi.INeutronFloatingIPAware;
import org.opendaylight.neutron.spi.INeutronLoadBalancerAware;
import org.opendaylight.neutron.spi.INeutronLoadBalancerPoolAware;
import org.opendaylight.neutron.spi.INeutronLoadBalancerPoolMemberAware;
import org.opendaylight.neutron.spi.INeutronNetworkAware;
import org.opendaylight.neutron.spi.INeutronPortAware;
import org.opendaylight.neutron.spi.INeutronRouterAware;
import org.opendaylight.neutron.spi.INeutronSecurityRuleAware;
import org.opendaylight.neutron.spi.INeutronSubnetAware;
import org.opendaylight.ovsdb.openstack.netvirt.api.Constants;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ActivatorV2 implements BundleActivator {
    
    protected static final Logger LOG = LoggerFactory.getLogger(ActivatorV2.class);
    
    private List<ServiceRegistration<?>> registrations = new ArrayList<ServiceRegistration<?>>();

    @Override
    public void start(BundleContext context) throws Exception {         
        Dictionary<String, Object> floatingIPHandlerPorperties = new Hashtable<>();
        floatingIPHandlerPorperties.put(Constants.EVENT_HANDLER_TYPE_PROPERTY, AbstractEvent.HandlerType.NEUTRON_FLOATING_IP);
        registerGlobalServiceWReg(context, INeutronFloatingIPAware.class, floatingIPHandlerPorperties);
        
        Dictionary<String, Object> networkHandlerProperties = new Hashtable<>();
        networkHandlerProperties.put(Constants.EVENT_HANDLER_TYPE_PROPERTY, AbstractEvent.HandlerType.NEUTRON_NETWORK);
        registerGlobalServiceWReg(context, INeutronNetworkAware.class, networkHandlerProperties);

        Dictionary<String, Object> subnetHandlerProperties = new Hashtable<>();
        subnetHandlerProperties.put(Constants.EVENT_HANDLER_TYPE_PROPERTY, AbstractEvent.HandlerType.NEUTRON_SUBNET);
        registerGlobalServiceWReg(context, INeutronSubnetAware.class, subnetHandlerProperties);

        Dictionary<String, Object> portHandlerProperties = new Hashtable<>();
        portHandlerProperties.put(Constants.EVENT_HANDLER_TYPE_PROPERTY, AbstractEvent.HandlerType.NEUTRON_PORT);
        registerGlobalServiceWReg(context, INeutronPortAware.class, portHandlerProperties);

        Dictionary<String, Object> routerHandlerProperties = new Hashtable<>();
        routerHandlerProperties.put(Constants.EVENT_HANDLER_TYPE_PROPERTY, AbstractEvent.HandlerType.NEUTRON_ROUTER);
        registerGlobalServiceWReg(context, INeutronRouterAware.class, routerHandlerProperties);

        Dictionary<String, Object> lbaasHandlerProperties = new Hashtable<>();
        lbaasHandlerProperties.put(Constants.EVENT_HANDLER_TYPE_PROPERTY, AbstractEvent.HandlerType.NEUTRON_LOAD_BALANCER);
        registerGlobalServiceWReg(context, INeutronLoadBalancerAware.class, lbaasHandlerProperties);

        Dictionary<String, Object> lbaasPoolHandlerProperties = new Hashtable<>();
        lbaasPoolHandlerProperties.put(Constants.EVENT_HANDLER_TYPE_PROPERTY, AbstractEvent.HandlerType.NEUTRON_LOAD_BALANCER_POOL);
        registerGlobalServiceWReg(context, INeutronLoadBalancerPoolAware.class, lbaasPoolHandlerProperties);

        Dictionary<String, Object> lbaasPoolMemberHandlerProperties = new Hashtable<>();
        lbaasPoolMemberHandlerProperties.put(Constants.EVENT_HANDLER_TYPE_PROPERTY, AbstractEvent.HandlerType.NEUTRON_LOAD_BALANCER_POOL_MEMBER);
        registerGlobalServiceWReg(context, INeutronLoadBalancerPoolMemberAware.class, lbaasPoolMemberHandlerProperties);

        Dictionary<String, Object> portSecurityHandlerProperties = new Hashtable<>();
        portSecurityHandlerProperties.put(Constants.EVENT_HANDLER_TYPE_PROPERTY, AbstractEvent.HandlerType.NEUTRON_PORT_SECURITY);
        registerGlobalServiceWReg(context, INeutronSecurityRuleAware.class, portSecurityHandlerProperties);

        Dictionary<String, Object> fWaasHandlerProperties = new Hashtable<>();
        fWaasHandlerProperties.put(Constants.EVENT_HANDLER_TYPE_PROPERTY, AbstractEvent.HandlerType.NEUTRON_FWAAS);
        registerGlobalServiceWReg(context, INeutronFirewallAware.class, fWaasHandlerProperties);   
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        for (ServiceRegistration registration : registrations) {
            if (registration != null) {
                registration.unregister();
            }
        }
    }
    
    private void registerGlobalServiceWReg(BundleContext bCtx, Class<?> clazz, Dictionary<String, Object> properties) {
        ServiceRegistration registration = bCtx.registerService(clazz.getName(), getInstance(bCtx, clazz), properties);
        if (registration != null) {
            registrations.add(registration);
        }
        else {
            LOG.error("Failed to register class", clazz);
        }
    }

    public static Object getInstance(BundleContext bCtx, Class<?> clazz) {
        Object[] instances = getInstances(bCtx, clazz);
        if (instances != null) {
            return instances[0];
        }
        return null;
    }
    
    public static Object[] getInstances(BundleContext bCtx, Class<?> clazz) {
        Object instances[] = null;
        try {
            ServiceReference<?>[] services = null;
            services = bCtx.getServiceReferences(clazz.getName(), null);
        
            if (services != null) {
                instances = new Object[services.length];
                for (int i = 0; i < services.length; i++) {
                    instances[i] = bCtx.getService(services[i]);
                }
            }
        } catch (Exception e) {
            LOG.error("Instance reference is NULL");
        }
        return instances;
    }
}
