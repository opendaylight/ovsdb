/*
 * Copyright © 2016 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.netvirt.sfc.impl.rev141210;

import org.opendaylight.ovsdb.openstack.netvirt.sfc.NetvirtSfcProvider;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetvirtSfcImplModule extends org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.netvirt.sfc.impl.rev141210.AbstractNetvirtSfcImplModule {
    private static final Logger LOG = LoggerFactory.getLogger(NetvirtSfcImplModule.class);
    private BundleContext bundleContext;

    public NetvirtSfcImplModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public NetvirtSfcImplModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.netvirt.sfc.impl.rev141210.NetvirtSfcImplModule oldModule, java.lang.AutoCloseable oldInstance) {
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
        sfcProvider.setAddSfFlows(getAddsfflows());
        getBrokerDependency().registerProvider(sfcProvider);
        return sfcProvider;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }
}
