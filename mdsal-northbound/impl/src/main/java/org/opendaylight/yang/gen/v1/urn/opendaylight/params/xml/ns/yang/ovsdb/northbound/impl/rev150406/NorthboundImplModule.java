package org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.northbound.impl.rev150406;

import org.opendaylight.ovsdb.northbound.NorthboundProvider;

public class NorthboundImplModule extends org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.northbound.impl.rev150406.AbstractNorthboundImplModule {
    public NorthboundImplModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public NorthboundImplModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.northbound.impl.rev150406.NorthboundImplModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
//        NorthbounddUtil.setInstanceIdentifierCodec(new InstanceIdentifierCodec(getSchemaServiceDependency(),
//                getBindingNormalizedNodeSerializerDependency()));
        NorthboundProvider provider = new NorthboundProvider();
        return provider;
    }

}
