package org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.netvirt._new.impl.rev151230;

import org.opendaylight.ovsdb.netvirt.impl.NetvirtProvider;

public class NetvirtModule extends org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.netvirt._new.impl.rev151230.AbstractNetvirtModule {
    public NetvirtModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public NetvirtModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.netvirt._new.impl.rev151230.NetvirtModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        NetvirtProvider provider = new NetvirtProvider();
        getBrokerDependency().registerProvider(provider);
        return provider;
    }

}
