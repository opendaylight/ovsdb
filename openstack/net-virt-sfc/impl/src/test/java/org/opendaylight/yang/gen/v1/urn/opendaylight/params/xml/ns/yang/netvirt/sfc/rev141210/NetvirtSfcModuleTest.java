/*
 * Copyright © 2015 Red Hat, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.netvirt.sfc.rev141210;

import org.junit.Test;
import org.opendaylight.controller.config.api.DependencyResolver;
import org.opendaylight.controller.config.api.JmxAttribute;
import org.opendaylight.controller.config.api.ModuleIdentifier;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.ovsdb.openstack.netvirt.sfc.NetvirtSfcProvider;
import org.osgi.framework.BundleContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;

import javax.management.ObjectName;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class NetvirtSfcModuleTest {
    @Test
    public void testCustomValidation() {
        NetvirtSfcModule module = new NetvirtSfcModule(mock(ModuleIdentifier.class), mock(DependencyResolver.class));
        // ensure no exceptions on validation
        // currently this method is empty
        module.customValidation();
    }

    @Test
    public void testCreateInstance() throws Exception {
        // configure mocks
        DependencyResolver dependencyResolver = mock(DependencyResolver.class);
        BindingAwareBroker broker = mock(BindingAwareBroker.class);
        ProviderContext session = mock(ProviderContext.class);
        DataBroker dataBroker = mock(DataBroker.class);
        BundleContext bundleContext = mock(BundleContext.class);
        when(dependencyResolver.resolveInstance(eq(BindingAwareBroker.class), any(ObjectName.class), any(JmxAttribute.class))).thenReturn(broker);
        when(session.getSALService(eq(DataBroker.class))).thenReturn(dataBroker);

        // create instance of module with injected mocks
        NetvirtSfcModule module = new NetvirtSfcModule(mock(ModuleIdentifier.class), dependencyResolver);
        module.setBundleContext(bundleContext);
        //module.setDataBroker(mock(ObjectName.class));
        // getInstance calls resolveInstance to get the broker dependency and then calls createInstance
        AutoCloseable closeable = module.getInstance();
        ((NetvirtSfcProvider)closeable).onSessionInitiated(session);
        // verify that the module registered the returned provider with the broker
        verify(broker).registerProvider((NetvirtSfcProvider)closeable);

        // ensure no exceptions on close
        closeable.close();
    }
}
