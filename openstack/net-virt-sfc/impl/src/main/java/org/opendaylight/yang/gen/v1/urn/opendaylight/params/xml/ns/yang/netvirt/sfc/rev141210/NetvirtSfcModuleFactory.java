/*
 * Copyright © 2015 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.netvirt.sfc.rev141210;

import org.opendaylight.controller.config.api.DependencyResolver;
import org.osgi.framework.BundleContext;

public class NetvirtSfcModuleFactory extends AbstractNetvirtSfcModuleFactory {
    @Override
    public NetvirtSfcModule instantiateModule(String instanceName, DependencyResolver dependencyResolver,
                                              BundleContext bundleContext) {
        NetvirtSfcModule module = super.instantiateModule(instanceName, dependencyResolver, bundleContext);
        module.setBundleContext(bundleContext);
        return module;
    }

    @Override
    public NetvirtSfcModule instantiateModule(String instanceName, DependencyResolver dependencyResolver,
                                              NetvirtSfcModule oldModule, AutoCloseable oldInstance,
                                              BundleContext bundleContext) {
        NetvirtSfcModule module = super.instantiateModule(instanceName, dependencyResolver,
                oldModule, oldInstance, bundleContext);
        module.setBundleContext(bundleContext);
        return module;
    }
}
