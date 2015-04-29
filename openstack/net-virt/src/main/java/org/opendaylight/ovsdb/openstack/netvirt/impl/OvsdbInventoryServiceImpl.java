/*
 * Copyright (c) 2015 Red Hat, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.openstack.netvirt.impl;

import com.google.common.collect.Sets;
import java.net.InetAddress;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import org.apache.felix.dm.Component;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
//import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ConsumerContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareConsumer;
import org.opendaylight.ovsdb.lib.notation.Row;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.schema.GenericTableSchema;
import org.opendaylight.ovsdb.lib.schema.typed.TypedBaseTable;
import org.opendaylight.ovsdb.openstack.netvirt.api.OvsdbConfigurationService;
import org.opendaylight.ovsdb.openstack.netvirt.api.OvsdbInventoryService;
import org.opendaylight.ovsdb.openstack.netvirt.api.OvsdbInventoryListener;
import org.opendaylight.ovsdb.openstack.netvirt.api.OvsdbPluginException;
import org.opendaylight.ovsdb.openstack.netvirt.api.Status;
import org.opendaylight.ovsdb.openstack.netvirt.api.StatusWithUuid;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * MdsalConsumerImpl is the implementation for {@link OvsdbInventoryService}
 *
 * @author Sam Hague (shague@redhat.com)
 */
public class OvsdbInventoryServiceImpl implements BindingAwareConsumer,
        OvsdbConfigurationService, OvsdbInventoryService {
    private static final Logger LOG = LoggerFactory.getLogger(OvsdbInventoryServiceImpl.class);
    private static DataBroker dataBroker = null;

    private static Set<OvsdbInventoryListener> mdsalConsumerListeners = Sets.newCopyOnWriteArraySet();
    private OvsdbDataChangeListener ovsdbDataChangeListener = null;
    private static MdsalUtils mdsalUtils = null;
    private volatile BindingAwareBroker broker; // dependency injection
    private ConsumerContext consumerContext = null;

    void init(Component c) {
        LOG.info(">>>>> init OvsdbInventoryServiceImpl");
        LOG.info(">>>>> Netvirt Provider Registered with MD-SAL");
        broker.registerConsumer(this, c.getDependencyManager().getBundleContext());
    }

    void start() {
        LOG.info(">>>>> start OvsdbInventoryServiceImpl");
    }

    void destroy() {
        // Now lets close MDSAL session
        if (this.consumerContext != null) {
            //this.consumerContext.close();
            this.dataBroker = null;
            this.consumerContext = null;
        }
    }
    @Override
    public void onSessionInitialized (ConsumerContext consumerContext) {
        this.consumerContext = consumerContext;
        dataBroker = consumerContext.getSALService(DataBroker.class);
        LOG.info("netvirt MdsalConsumer initialized");
        ovsdbDataChangeListener = new OvsdbDataChangeListener(dataBroker);
        mdsalUtils = new MdsalUtils(dataBroker);
    }

    //@Override
    public static DataBroker getDataBroker () {
        return dataBroker;
    }

    private void listenerAdded(OvsdbInventoryListener listener) {
        mdsalConsumerListeners.add(listener);
        LOG.info("listenerAdded: {}", listener);
    }

    private void listenerRemoved(OvsdbInventoryListener listener) {
        mdsalConsumerListeners.remove(listener);
        LOG.info("listenerRemoved: {}", listener);
    }

    public InetAddress getTunnelEndPoint(Node node) {
        return null;
    }

    public String getNodeUUID(Node node) {
        return null;
    }

    @Override
    public String getBridgeUUID(String bridgeName) {
        return null;
    }

    // get vlan and network id

    public static Set<OvsdbInventoryListener> getMdsalConsumerListeners () {
        return mdsalConsumerListeners;
    }

    @Override
    public StatusWithUuid insertRow(Node node, String tableName, String parentUuid, Row<GenericTableSchema> row) {
        return null;
    }

    @Override
    public Status updateRow(Node node, String tableName, String parentUuid, String rowUuid, Row row) {
        return null;
    }

    @Override
    public Status deleteRow(Node node, String tableName, String rowUuid) {
        return null;
    }

    @Override
    public Row getRow(Node node, String tableName, String uuid) {
        return null;
    }

    @Override
    public Row<GenericTableSchema> getRow(Node node, String databaseName, String tableName, UUID uuid) throws OvsdbPluginException {
        return null;
    }

    @Override
    public ConcurrentMap<String, Row> getRows(Node node, String tableName) {
        return null;
    }

    @Override
    public ConcurrentMap<UUID, Row<GenericTableSchema>> getRows(Node node, String databaseName, String tableName) throws OvsdbPluginException {
        return null;
    }

    @Override
    public ConcurrentMap<UUID, Row<GenericTableSchema>> getRows(Node node, String databaseName, String tableName, String fiqlQuery) throws OvsdbPluginException {
        return null;
    }

    @Override
    public List<String> getTables(Node node) {
        return null;
    }

    @Override
    public Boolean setOFController(Node node, String bridgeUUID) throws InterruptedException, ExecutionException {
        return null;
    }

    @Override
    public <T extends TypedBaseTable<?>> String getTableName(Node node, Class<T> typedClass) {
        return null;
    }

    @Override
    public <T extends TypedBaseTable<?>> T getTypedRow(Node node, Class<T> typedClass, Row row) {
        return null;
    }

    @Override
    public <T extends TypedBaseTable<?>> T createTypedRow(Node node, Class<T> typedClass) {
        return null;
    }

    @Override
    public ConcurrentMap<String, OvsdbTerminationPointAugmentation> getInterfaces(Node node) {
        return null;
    }
}
