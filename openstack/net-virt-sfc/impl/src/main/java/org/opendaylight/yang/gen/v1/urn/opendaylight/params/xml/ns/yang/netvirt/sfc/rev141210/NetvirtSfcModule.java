package org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.netvirt.sfc.rev141210;

import org.opendaylight.ovsdb.openstack.netvirt.sfc.NetvirtSfcProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.osgi.framework.BundleContext;

public class NetvirtSfcModule extends org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.netvirt.sfc.rev141210.AbstractNetvirtSfcModule {

    private static final Logger LOG = LoggerFactory.getLogger(NetvirtSfcModule.class);

    public NetvirtSfcModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public NetvirtSfcModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.netvirt.sfc.rev141210.NetvirtSfcModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        LOG.info("Netvirt SFC module initialization.");
        NetvirtSfcProvider sfcProvider = new NetvirtSfcProvider();
        getBrokerDependency().registerProvider(sfcProvider);
        return sfcProvider;
    }
}
