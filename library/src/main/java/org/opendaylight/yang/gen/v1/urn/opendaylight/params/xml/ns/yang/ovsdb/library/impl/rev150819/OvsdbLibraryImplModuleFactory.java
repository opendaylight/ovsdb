/*
 * Copyright © 2015 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.library.impl.rev150819;

import org.opendaylight.controller.config.api.DependencyResolver;
import org.opendaylight.controller.config.api.DynamicMBeanWithInstance;
import org.opendaylight.controller.config.spi.Module;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OvsdbLibraryImplModuleFactory extends org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.library.impl.rev150819.AbstractOvsdbLibraryImplModuleFactory {
    private static final Logger LOG = LoggerFactory.getLogger(OvsdbLibraryImplModuleFactory.class);

    @Override
    public Module createModule(String instanceName,
                               DependencyResolver dependencyResolver,
                               DynamicMBeanWithInstance old, BundleContext bundleContext)
            throws Exception {
        Module module =  super.createModule(instanceName, dependencyResolver, old, bundleContext);
        setModuleBundleContext(bundleContext, module);
        return module;
    }

    @Override
    public Module createModule(String instanceName,
                               DependencyResolver dependencyResolver, BundleContext bundleContext) {
        Module module = super.createModule(instanceName, dependencyResolver, bundleContext);
        setModuleBundleContext(bundleContext, module);
        return module;
    }

    private void setModuleBundleContext(BundleContext bundleContext,
                                        Module module) {
        if (module instanceof OvsdbLibraryImplModule) {
            ((OvsdbLibraryImplModule)module).setBundleContext(bundleContext);
        } else {
            LOG.warn("Module is of type {} expected type {}",
                    module.getClass(), OvsdbLibraryImplModule.class);
        }
    }
}
