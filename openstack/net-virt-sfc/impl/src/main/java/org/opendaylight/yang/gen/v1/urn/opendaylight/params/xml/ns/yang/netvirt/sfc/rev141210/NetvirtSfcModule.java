/*
 * Copyright © 2015 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.netvirt.sfc.rev141210;

import org.opendaylight.controller.config.api.DependencyResolver;
import org.opendaylight.controller.config.api.ModuleIdentifier;
import org.opendaylight.ovsdb.openstack.netvirt.sfc.NetvirtSfcProvider;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetvirtSfcModule extends AbstractNetvirtSfcModule {
    private static final Logger LOG = LoggerFactory.getLogger(NetvirtSfcModule.class);
    private BundleContext bundleContext;

    public NetvirtSfcModule(ModuleIdentifier identifier, DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public NetvirtSfcModule(ModuleIdentifier identifier, DependencyResolver dependencyResolver,
                            NetvirtSfcModule oldModule, java.lang.AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        LOG.info("Netvirt SFC module initialization.");
        NetvirtSfcProvider sfcProvider = new NetvirtSfcProvider(bundleContext);
        sfcProvider.setOf13Provider(getOf13provider());
        getBrokerDependency().registerProvider(sfcProvider);
        return sfcProvider;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }
}
