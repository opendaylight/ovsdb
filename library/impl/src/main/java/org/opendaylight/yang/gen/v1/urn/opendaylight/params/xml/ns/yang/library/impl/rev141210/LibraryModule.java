/*
 * Copyright © 2015 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.library.impl.rev141210;

import org.opendaylight.controller.config.api.DependencyResolver;
import org.opendaylight.controller.config.api.ModuleIdentifier;
import org.opendaylight.ovsdb.lib.impl.LibraryProvider;
import org.osgi.framework.BundleContext;

import com.google.common.base.Preconditions;

public class LibraryModule extends org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.library.impl.rev141210.AbstractLibraryModule {
    private BundleContext bundleContext;

    public LibraryModule(ModuleIdentifier identifier, DependencyResolver dependencyResolver) {
        super(identifier, dependencyResolver);
    }

    public LibraryModule(ModuleIdentifier identifier, DependencyResolver dependencyResolver, LibraryModule oldModule, AutoCloseable oldInstance) {
        super(identifier, dependencyResolver, oldModule, oldInstance);
    }

    @Override
    public void customValidation() {
        // add custom validation form module attributes here.
        Preconditions.checkNotNull(bundleContext);
    }

    @Override
    public java.lang.AutoCloseable createInstance() {
        LibraryProvider provider = new LibraryProvider(bundleContext);
        getBrokerDependency().registerProvider(provider);
        return provider;
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }
}
