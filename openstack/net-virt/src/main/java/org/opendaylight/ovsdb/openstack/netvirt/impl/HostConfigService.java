/*
 * Copyright (c) 2015 Intel Corporation.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt.impl;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.ovsdb.openstack.netvirt.ClusterAwareMdsalUtils;
import org.opendaylight.ovsdb.openstack.netvirt.ConfigInterface;
import org.opendaylight.ovsdb.openstack.netvirt.api.*;
import org.opendaylight.ovsdb.utils.servicehelper.ServiceHelper;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.hostconfig.rev150712.host.config.attributes.HostConfigs;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.hostconfig.rev150712.host.config.attributes.host.configs.HostConfig;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.rev150712.Neutron;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbNodeAugmentation;
import org.opendaylight.yang.gen.v1.urn.opendaylight.neutron.hostconfig.rev150712.host.config.attributes.host.configs.HostConfigBuilder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class HostConfigService implements OvsdbInventoryListener, ConfigInterface {
    private static final Logger LOG = LoggerFactory.getLogger(HostConfigService.class);

    private final DataBroker databroker;
    private final ClusterAwareMdsalUtils mdsalUtils;

    public static final String OS_HOST_CONFIG_HOST_ID_KEY = "odl_os_hostconfig_hostid";
    public static final String OS_HOST_CONFIG_HOST_TYPE_KEY = "odl_os_hostconfig_hosttype";
    public static final String OS_HOST_CONFIG_CONFIG_KEY = "odl_os_hostconfig_config";

    private String hostConfigHostIdKey;
    private String hostConfigHostTypeKey;
    private String hostConfigConfigKey;

    private volatile OvsdbInventoryService ovsdbInventoryService;
    private volatile Southbound southbound;

    public HostConfigService(DataBroker dataBroker) {
        this.databroker = dataBroker;
        mdsalUtils = new ClusterAwareMdsalUtils(dataBroker);

        hostConfigHostIdKey = OS_HOST_CONFIG_HOST_ID_KEY;
        hostConfigHostTypeKey = OS_HOST_CONFIG_HOST_TYPE_KEY;
        hostConfigConfigKey = OS_HOST_CONFIG_CONFIG_KEY;
    }

    @Override
    public void ovsdbUpdate(Node node, DataObject resourceAugmentationData, OvsdbType ovsdbType, Action action) {
        LOG.info("ovsdbUpdate: {} - {} - <<{}>> <<{}>>", ovsdbType, action, node, resourceAugmentationData);
        boolean result;
        HostConfig hostConfig;
        InstanceIdentifier<HostConfig> hostConfigId;

        if (ovsdbType != OvsdbType.NODE) {
            return;
        }
        hostConfig = extractHostConfigInfo(node);
        if (hostConfig == null) {
              return;
        }
        switch (action) {
            case ADD:
            case UPDATE:
                    hostConfigId = createInstanceIdentifier(hostConfig);
                    result = mdsalUtils.put(LogicalDatastoreType.OPERATIONAL, hostConfigId, hostConfig);
                    LOG.info("Add Node: result: {}", result);
                break;
            case DELETE:
                    hostConfigId = createInstanceIdentifier(hostConfig);
                    result = mdsalUtils.delete(LogicalDatastoreType.OPERATIONAL, hostConfigId);
                    LOG.info("Delete Node: result: {}", result);
                break;
        }
    }

    @Override
    public void triggerUpdates() {
        LOG.info("triggerUpdates");
    }

    private HostConfig extractHostConfigInfo(Node node) {
        HostConfigBuilder hostConfigBuilder = new HostConfigBuilder();
        String value;

        value = southbound.getOtherConfig(node, OvsdbTables.OPENVSWITCH, hostConfigHostIdKey);
        if (value == null){
            LOG.info("Host Config not defined for the node");
            return null;
        }
        hostConfigBuilder.setHostId(value);
        value = southbound.getOtherConfig(node, OvsdbTables.OPENVSWITCH, hostConfigHostTypeKey);
        if (value == null) {
            LOG.warn("Host Config Missing Host type");
            return null;
        }
        hostConfigBuilder.setHostType(value);
        value = southbound.getOtherConfig(node, OvsdbTables.OPENVSWITCH, hostConfigConfigKey);
        if (value == null) {
            LOG.warn("Host Config Missing Host config");
            return null;
        }
        hostConfigBuilder.setConfig(value);
        return hostConfigBuilder.build();
    }

    InstanceIdentifier<HostConfig> createInstanceIdentifier() {
        return InstanceIdentifier.create(Neutron.class)
                .child(HostConfigs.class)
                .child(HostConfig.class);
    }

    InstanceIdentifier<HostConfig> createInstanceIdentifier(HostConfig hostConfig) {
        return InstanceIdentifier.create(Neutron.class)
                .child(HostConfigs.class)
                .child(HostConfig.class, hostConfig.getKey());
    }

    @Override
    public void setDependencies(ServiceReference serviceReference) {
        southbound =
                (Southbound) ServiceHelper.getGlobalInstance(Southbound.class, this);
        ovsdbInventoryService =
                (OvsdbInventoryService) ServiceHelper.getGlobalInstance(OvsdbInventoryService.class, this);
        ovsdbInventoryService.listenerAdded(this);
    }

    @Override
    public void setDependencies(Object impl) {
    }
}
