package org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.netvirt.impl.rev150513;

import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.ovsdb.openstack.netvirt.NetvirtProvider;
import org.osgi.framework.BundleContext;

public class NetvirtImplModule extends org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.netvirt.impl.rev150513.AbstractNetvirtImplModule {
    private BundleContext bundleContext = null;

    public NetvirtImplModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public NetvirtImplModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.netvirt.impl.rev150513.NetvirtImplModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        NetvirtProvider provider = new NetvirtProvider(bundleContext, getClusteringEntityOwnershipServiceDependency());
        BindingAwareBroker localBroker = getBrokerDependency();
        localBroker.registerProvider(provider);
        return provider;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }
}
