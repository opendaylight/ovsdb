/*
 * Red Hat, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.netvirt.impl.rev141210;

import org.opendaylight.ovsdb.impl.NetvirtProvider;

public class NetvirtModule extends org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.netvirt.impl.rev141210.AbstractNetvirtModule {
    public NetvirtModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public NetvirtModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.netvirt.impl.rev141210.NetvirtModule oldModule, java.lang.AutoCloseable oldInstance) {
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
