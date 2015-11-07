/*
 * Copyright © 2015 Dell, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt.sfc;

import java.util.Dictionary;
import java.util.Hashtable;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.opendaylight.ovsdb.openstack.netvirt.api.Constants;
import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.AbstractServiceInstance;
import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.OF13Provider;
import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.Service;
import org.opendaylight.ovsdb.openstack.netvirt.sfc.openflow13.INetvirtSfcOF13Provider;
import org.opendaylight.ovsdb.openstack.netvirt.sfc.openflow13.NetvirtSfcOF13Provider;
import org.opendaylight.ovsdb.openstack.netvirt.sfc.openflow13.Services.SfcClassifierService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetvirtSfcProvider implements BindingAwareProvider, AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(NetvirtSfcProvider.class);
    private NetvirtSfcAclListener aclListener;
    private NetvirtSfcClassifierListener classfierListener;
    private BundleContext bundleContext;

    public NetvirtSfcProvider(BundleContext bundleContext) {
        LOG.info("NetvirtSfcProvider: bundleContext: {}", bundleContext);
        this.bundleContext = bundleContext;
    }

    @Override
    public void onSessionInitiated(ProviderContext session) {
        LOG.info("NetvirtSfcProvider Session Initiated");
        DataBroker dataBroker = session.getSALService(DataBroker.class);

        INetvirtSfcOF13Provider provider = new NetvirtSfcOF13Provider(dataBroker);
        aclListener = new NetvirtSfcAclListener(provider, dataBroker);
        classfierListener = new NetvirtSfcClassifierListener(provider, dataBroker);

        addToPipeline();
    }

    @Override
    public void close() throws Exception {
        LOG.info("NetvirtSfcProvider Closed");
        aclListener.close();
        classfierListener.close();
    }

    private void addToPipeline() {
        SfcClassifierService sfcClassifierService = new SfcClassifierService();
        registerService(bundleContext, SfcClassifierService.class.getName(),
                        sfcClassifierService, Service.SFC_CLASSIFIER);
        sfcClassifierService.setDependencies(bundleContext, null);
        }

    private ServiceRegistration<?> registerService(BundleContext bundleContext, String[] interfaces,
                                                   Dictionary<String, Object> properties, Object impl) {
        ServiceRegistration<?> serviceRegistration = bundleContext.registerService(interfaces, impl, properties);
        return serviceRegistration;
    }

    private ServiceRegistration<?> registerService(BundleContext bundleContext, String interfaceClassName,
                                                       Object impl, Object serviceProperty) {
        Dictionary<String, Object> properties = new Hashtable<>();
        properties.put(AbstractServiceInstance.SERVICE_PROPERTY, serviceProperty);
        properties.put(Constants.PROVIDER_NAME_PROPERTY, OF13Provider.NAME);
        return registerService(bundleContext,
                new String[] {AbstractServiceInstance.class.getName(),
                interfaceClassName}, properties, impl);
    }
}
