package org.opendaylight.controller.config.yang.config.netvirt.impl;

import org.opendaylight.ovsdb.openstack.netvirt.NetVirtProvider;
import org.osgi.framework.BundleContext;


public class NetVirtImplModule extends org.opendaylight.controller.config.yang.config.netvirt.impl.AbstractNetVirtImplModule {

    private BundleContext bundleContext;

    public NetVirtImplModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public NetVirtImplModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.controller.config.yang.config.netvirt.impl.NetVirtImplModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        NetVirtProvider provider = new NetVirtProvider(bundleContext);
        getBrokerDependency().registerProvider(provider);
        return provider;
    }
    
    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }
}
