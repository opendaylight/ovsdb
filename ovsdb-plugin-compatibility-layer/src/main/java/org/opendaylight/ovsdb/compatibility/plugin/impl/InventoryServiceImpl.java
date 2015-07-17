/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.compatibility.plugin.impl;

import java.net.InetAddress;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.Property;
import org.opendaylight.controller.sal.core.UpdateType;
import org.opendaylight.ovsdb.compatibility.plugin.api.NodeUtils;
import org.opendaylight.ovsdb.compatibility.plugin.api.OvsdbInventoryListener;
import org.opendaylight.ovsdb.compatibility.plugin.api.OvsdbInventoryService;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.notation.Row;

import com.google.common.collect.Sets;

/**
 * This is a proxy class for ovsdb plugin's OvsdbInventoryService class
 * It just forward the call to OvsdbInventoryService instance and pass
 * back the response to the caller.
 * It also register as a listener to ovsdb plugin and relay the notification
 * back to all the subscriber of this compatibility layer.
 *
 * @author Anil Vishnoi (vishnoianil@gmail.com)
 *
 */
public class InventoryServiceImpl implements OvsdbInventoryService,
        org.opendaylight.ovsdb.plugin.api.OvsdbInventoryListener {
    private volatile org.opendaylight.ovsdb.plugin.api.OvsdbInventoryService pluginOvsdbInventoryService;

    private Set<OvsdbInventoryListener> ovsdbInventoryListeners = Sets.newCopyOnWriteArraySet();


    /**
     * Function called by the dependency manager when all the required
     * dependencies are satisfied
     *
     */
    public void init() {
    }

    /**
     * Function called by the dependency manager when at least one dependency
     * become unsatisfied or when the component is shutting down because for
     * example bundle is being stopped.
     *
     */
    public void destroy() {
    }

    /**
     * Function called by dependency manager after "init ()" is called and after
     * the services provided by the class are registered in the service registry
     *
     */
    public void start() {
    }

    /**
     * Function called by the dependency manager before the services exported by
     * the component are unregistered, this will be followed by a "destroy ()"
     * calls
     *
     */
    public void stop() {
    }

    public void setOvsdbInventoryService(org.opendaylight.ovsdb.plugin.api.OvsdbInventoryService pluginOvsdbInventoryService){
        this.pluginOvsdbInventoryService = pluginOvsdbInventoryService;
    }

    public void unsetOvsdbInventoryService(org.opendaylight.ovsdb.plugin.api.OvsdbInventoryService pluginOvsdbInventoryService){
        this.pluginOvsdbInventoryService = pluginOvsdbInventoryService;
    }

    //Register listener for ovsdb.compatibility
    public void addOvsdbInventoryListener(OvsdbInventoryListener pluginOvsdbInventoryListener){
        this.ovsdbInventoryListeners.add(pluginOvsdbInventoryListener);
    }

    public void removeOvsdbInventoryListener(OvsdbInventoryListener pluginOvsdbInventoryListener){
        if(this.ovsdbInventoryListeners.contains(ovsdbInventoryListeners)) {
            this.ovsdbInventoryListeners.remove(ovsdbInventoryListeners);
        }
    }

    @Override
    public ConcurrentMap<String, ConcurrentMap<String, Row>> getCache(Node n, String databaseName) {
        return pluginOvsdbInventoryService.getCache(NodeUtils.getMdsalNode(n), databaseName);
    }


    @Override
    public ConcurrentMap<String, Row> getTableCache(Node n, String databaseName, String tableName) {
        return pluginOvsdbInventoryService.getTableCache(NodeUtils.getMdsalNode(n), databaseName, tableName);
    }


    @Override
    public Row getRow(Node n, String databaseName, String tableName, String uuid) {
        return pluginOvsdbInventoryService.getRow(NodeUtils.getMdsalNode(n), databaseName, tableName, uuid);
    }

    @Override
    public void updateRow(Node n, String databaseName, String tableName, String uuid, Row row) {
        pluginOvsdbInventoryService.updateRow(NodeUtils.getMdsalNode(n), databaseName, tableName, uuid, row);
    }

    @Override
    public void removeRow(Node n, String databaseName, String tableName, String uuid) {
        pluginOvsdbInventoryService.removeRow(NodeUtils.getMdsalNode(n), databaseName, tableName, uuid);
    }

    @Override
    public void processTableUpdates(Node n, String databaseName, TableUpdates tableUpdates) {
        pluginOvsdbInventoryService.processTableUpdates(NodeUtils.getMdsalNode(n), databaseName, tableUpdates);
    }

    @Override
    public void printCache(Node n) {
        pluginOvsdbInventoryService.printCache(NodeUtils.getMdsalNode(n));
    }

    @Override
    public void addNode(Node node, Set<Property> props) {
    }

    @Override
    public void notifyNodeAdded(Node node, InetAddress address, int port) {
        pluginOvsdbInventoryService.notifyNodeAdded(NodeUtils.getMdsalNode(node), address, port);
    }

    @Override
    public void addNodeProperty(Node node, UpdateType type, Set<Property> props) {
    }

    @Override
    public void removeNode(Node node) {
        pluginOvsdbInventoryService.removeNode(NodeUtils.getMdsalNode(node));
    }

    @Override
    public void nodeAdded(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node node,
                          InetAddress address, int port) {
        for(OvsdbInventoryListener listener : this.ovsdbInventoryListeners) {
            listener.nodeAdded(NodeUtils.getSalNode(node), address, port);
        }

    }

    @Override
    public void nodeRemoved(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node node) {
        for(OvsdbInventoryListener listener : this.ovsdbInventoryListeners) {
            listener.nodeRemoved(NodeUtils.getSalNode(node));
        }

    }

    @Override
    public void rowAdded(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node node,
                         String tableName, String uuid, Row row) {
        for(OvsdbInventoryListener listener : this.ovsdbInventoryListeners) {
            listener.rowAdded(NodeUtils.getSalNode(node), tableName, uuid, row);
        }

    }

    @Override
    public void rowUpdated(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node node,
                           String tableName, String uuid, Row old,
            Row row) {
        for(OvsdbInventoryListener listener : this.ovsdbInventoryListeners) {
            listener.rowUpdated(NodeUtils.getSalNode(node), tableName, uuid, old, row);
        }

    }

    @Override
    public void rowRemoved(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node node,
                           String tableName, String uuid, Row row,
            Object context) {
        for(OvsdbInventoryListener listener : this.ovsdbInventoryListeners) {
            listener.rowRemoved(NodeUtils.getSalNode(node), tableName, uuid, row, context);
        }
    }
}
