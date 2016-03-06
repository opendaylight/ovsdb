/*
 * Copyright © 2015, 2016 Red Hat, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.netvirt.sfc.rev141210;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Dictionary;

import javax.management.ObjectName;

import org.junit.Test;
import org.opendaylight.controller.config.api.DependencyResolver;
import org.opendaylight.controller.config.api.JmxAttribute;
import org.opendaylight.controller.config.api.ModuleIdentifier;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.ovsdb.openstack.netvirt.api.Southbound;
import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.AbstractServiceInstance;
import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.PipelineOrchestrator;
import org.opendaylight.ovsdb.openstack.netvirt.sfc.NetvirtSfcProvider;
import org.opendaylight.ovsdb.openstack.netvirt.sfc.standalone.openflow13.services.SfcClassifierService;
import org.opendaylight.ovsdb.utils.servicehelper.ServiceHelper;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

public class NetvirtSfcModuleTest {
    @Test
    public void testCustomValidation() {
        NetvirtSfcModule module = new NetvirtSfcModule(mock(ModuleIdentifier.class), mock(DependencyResolver.class));
        // ensure no exceptions on validation
        // currently this method is empty
        module.customValidation();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testCreateInstance() throws Exception {
        // configure mocks
        DependencyResolver dependencyResolver = mock(DependencyResolver.class);
        BindingAwareBroker broker = mock(BindingAwareBroker.class);
        ProviderContext session = mock(ProviderContext.class);
        DataBroker dataBroker = mock(DataBroker.class);
        when(dependencyResolver.resolveInstance(eq(BindingAwareBroker.class),
                any(ObjectName.class), any(JmxAttribute.class))).thenReturn(broker);
        when(session.getSALService(eq(DataBroker.class))).thenReturn(dataBroker);

        // create instance of module with injected mocks
        NetvirtSfcModule module = new NetvirtSfcModule(mock(ModuleIdentifier.class), dependencyResolver);
        // getInstance calls resolveInstance to get the broker dependency and then calls createInstance
        BundleContext bundleContext = mock(BundleContext.class);
        PipelineOrchestrator pipelineOrchestrator = mock(PipelineOrchestrator.class);
        ServiceHelper.overrideGlobalInstance(PipelineOrchestrator.class, pipelineOrchestrator);
        ServiceHelper.overrideGlobalInstance(Southbound.class, mock(Southbound.class));

        doNothing().when(pipelineOrchestrator).registerService(any(ServiceReference.class),
                any(AbstractServiceInstance.class));
        when(bundleContext.registerService(
                eq(new String[]{AbstractServiceInstance.class.getName(), SfcClassifierService.class.getName()}),
                any(),
                any(Dictionary.class)))
                .thenReturn(mock(ServiceRegistration.class));
        when(bundleContext.getServiceReference(SfcClassifierService.class.getName()))
                .thenReturn(mock(ServiceReference.class));
        AutoCloseable closeable = module.getInstance();
        ((NetvirtSfcProvider)closeable).setBundleContext(bundleContext);
        ((NetvirtSfcProvider)closeable).setOf13Provider("standalone");
        ((NetvirtSfcProvider)closeable).onSessionInitiated(session);
        // verify that the module registered the returned provider with the broker
        verify(broker).registerProvider((NetvirtSfcProvider)closeable);

        // ensure no exceptions on close
        closeable.close();
    }
}
