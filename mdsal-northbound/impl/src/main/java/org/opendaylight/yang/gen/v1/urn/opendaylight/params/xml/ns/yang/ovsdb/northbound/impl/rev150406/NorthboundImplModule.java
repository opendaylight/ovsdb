/*
 * Copyright (c) 2015 Inocybe and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.northbound.impl.rev150406;

import org.opendaylight.ovsdb.northbound.InstanceIdentifierCodec;
import org.opendaylight.ovsdb.northbound.NorthboundProvider;
import org.opendaylight.ovsdb.northbound.NorthboundUtil;

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
        NorthboundUtil.setInstanceIdentifierCodec(new InstanceIdentifierCodec(getSchemaServiceDependency(),
                getBindingNormalizedNodeSerializerDependency()));
        NorthboundProvider provider = new NorthboundProvider();
        getBrokerDependency().registerProvider(provider);
        return provider;
    }

}
