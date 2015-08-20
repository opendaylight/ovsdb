/*
 * Copyright © 2015 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.library.impl.rev150819;

import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.ovsdb.lib.OvsdbLibraryProvider;
import org.osgi.framework.BundleContext;

public class OvsdbLibraryImplModule extends org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.library.impl.rev150819.AbstractOvsdbLibraryImplModule {
    private BundleContext bundleContext = null;

    public OvsdbLibraryImplModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public OvsdbLibraryImplModule(org.opendaylight.controller.config.api.ModuleIdentifier identifier, org.opendaylight.controller.config.api.DependencyResolver dependencyResolver, org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.library.impl.rev150819.OvsdbLibraryImplModule oldModule, AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
    }

    @Override
    public AutoCloseable createInstance() {
        OvsdbLibraryProvider provider = new OvsdbLibraryProvider(bundleContext);
        BindingAwareBroker localBroker = getBrokerDependency();
        localBroker.registerProvider(provider);
        return provider;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }
}
