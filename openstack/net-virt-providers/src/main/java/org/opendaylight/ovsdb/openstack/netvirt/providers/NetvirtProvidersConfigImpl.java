/*
 * Copyright (c) 2016 Red Hat, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.openstack.netvirt.providers;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataChangeListener;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataBroker;
import org.opendaylight.controller.md.sal.common.api.data.AsyncDataChangeEvent;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.ovsdb.openstack.netvirt.api.Action;
import org.opendaylight.ovsdb.openstack.netvirt.api.OvsdbInventoryListener;
import org.opendaylight.ovsdb.utils.mdsal.utils.MdsalUtils;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.netvirt.providers.config.rev160109.*;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.concepts.ListenerRegistration;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NetvirtProvidersConfigImpl implements AutoCloseable, ConfigInterface, DataChangeListener {
    private static final Logger LOG = LoggerFactory.getLogger(NetvirtProvidersConfigImpl.class);
    private final DataBroker dataBroker;
    private final ListenerRegistration<DataChangeListener> registration;
    private final ExecutorService executorService = Executors.newFixedThreadPool(1);
    private final MdsalUtils mdsalUtils;

    public NetvirtProvidersConfigImpl(final DataBroker dataBroker, final short tableOffset) {
        this.dataBroker = dataBroker;
        mdsalUtils = new MdsalUtils(dataBroker);

        InstanceIdentifier<NetvirtProvidersConfig> path =
                InstanceIdentifier.builder(NetvirtProvidersConfig.class).build();
        registration = dataBroker.registerDataChangeListener(LogicalDatastoreType.CONFIGURATION, path, this,
                AsyncDataBroker.DataChangeScope.SUBTREE);

        NetvirtProvidersConfigBuilder netvirtProvidersConfigBuilder = new NetvirtProvidersConfigBuilder();
        NetvirtProvidersConfig netvirtProvidersConfig =
                mdsalUtils.read(LogicalDatastoreType.CONFIGURATION, path);
        if (netvirtProvidersConfig != null) {
            netvirtProvidersConfigBuilder = new NetvirtProvidersConfigBuilder(netvirtProvidersConfig);
        }
        if (netvirtProvidersConfigBuilder.getTableOffset() == null) {
            netvirtProvidersConfigBuilder.setTableOffset(tableOffset);
        }
        boolean result = mdsalUtils.merge(LogicalDatastoreType.CONFIGURATION, path,
                netvirtProvidersConfigBuilder.build());

        LOG.info("NetvirtProvidersConfigImpl: dataBroker= {}, registration= {}, tableOffset= {}, result= {}",
                dataBroker, registration, tableOffset, result);
    }

    @Override
    public void close() throws Exception {
        registration.close();
        executorService.shutdown();
    }

    @Override
    public void onDataChanged(final AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> asyncDataChangeEvent) {
        executorService.submit(new Runnable() {

            @Override
            public void run() {
                LOG.info("onDataChanged: {}", asyncDataChangeEvent);
                processConfigCreate(asyncDataChangeEvent);
                processConfigUpdate(asyncDataChangeEvent);
            }
        });
    }

    private void processConfigCreate(AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes) {
        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : changes.getCreatedData().entrySet()) {
            if (entry.getValue() instanceof NetvirtProvidersConfig) {
                NetvirtProvidersConfig netvirtProvidersConfig = (NetvirtProvidersConfig) entry.getValue();
                applyConfig(netvirtProvidersConfig);
            }
        }
    }

    private void processConfigUpdate(AsyncDataChangeEvent<InstanceIdentifier<?>, DataObject> changes) {
        for (Map.Entry<InstanceIdentifier<?>, DataObject> entry : changes.getUpdatedData().entrySet()) {
            if (entry.getValue() instanceof NetvirtProvidersConfig) {
                LOG.info("processConfigUpdate: {}", entry);
                NetvirtProvidersConfig netvirtProvidersConfig = (NetvirtProvidersConfig) entry.getValue();
                applyConfig(netvirtProvidersConfig);
            }
        }
    }

    private void applyConfig(NetvirtProvidersConfig netvirtProvidersConfig) {
        LOG.info("processConfigUpdate: {}", netvirtProvidersConfig);
        if (netvirtProvidersConfig.getTableOffset() != null) {
            NetvirtProvidersProvider.setTableOffset(netvirtProvidersConfig.getTableOffset());
        }
    }

    @Override
    public void setDependencies(BundleContext bundleContext, ServiceReference serviceReference) {

    }

    @Override
    public void setDependencies(Object impl) {

    }
}
