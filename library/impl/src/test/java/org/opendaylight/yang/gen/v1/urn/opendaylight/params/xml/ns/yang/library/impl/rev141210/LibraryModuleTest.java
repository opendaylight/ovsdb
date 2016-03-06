/*
 * Copyright © 2015 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.library.impl.rev141210;

import org.junit.Test;
import org.opendaylight.controller.config.api.DependencyResolver;
import org.opendaylight.controller.config.api.JmxAttribute;
import org.opendaylight.controller.config.api.ModuleIdentifier;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.osgi.framework.BundleContext;

import javax.management.ObjectName;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class LibraryModuleTest {
    @Test
    public void testCustomValidation() {
        LibraryModule module = new LibraryModule(mock(ModuleIdentifier.class), mock(DependencyResolver.class));
        module.setBundleContext(mock(BundleContext.class));

        // ensure no exceptions on validation
        // currently this method is empty
        module.customValidation();
    }

    // TODO Need to migrate all users to SAL RPC
    @Test
    public void testCreateInstance() throws Exception {
        // configure mocks
        DependencyResolver dependencyResolver = mock(DependencyResolver.class);
        BindingAwareBroker broker = mock(BindingAwareBroker.class);
        when(dependencyResolver.resolveInstance(eq(BindingAwareBroker.class), any(ObjectName.class), any(JmxAttribute.class))).thenReturn(broker);

        // create instance of module with injected mocks
        LibraryModule module = new LibraryModule(mock(ModuleIdentifier.class), dependencyResolver);
        module.setBundleContext(mock(BundleContext.class));

        // getInstance calls resolveInstance to get the broker dependency and then calls createInstance
        AutoCloseable closeable = module.getInstance();

        // ensure no exceptions on close
        closeable.close();
    }
}
