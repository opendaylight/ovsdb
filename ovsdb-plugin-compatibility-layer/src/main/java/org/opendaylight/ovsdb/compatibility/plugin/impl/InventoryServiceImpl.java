/*
 * Copyright (C) 2013 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Madhu Venugopal, Brent Salisbury
 */
package org.opendaylight.ovsdb.compatibility.plugin.impl;

import java.net.InetAddress;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.Property;
import org.opendaylight.controller.sal.core.UpdateType;
import org.opendaylight.ovsdb.compatibility.plugin.api.OvsdbInventoryListener;
import org.opendaylight.ovsdb.compatibility.plugin.api.OvsdbInventoryService;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.notation.Row;

import com.google.common.collect.Sets;

/**
 * Stub Implementation for IPluginInReadService used by SAL
 *
 *
 */
public class InventoryServiceImpl implements OvsdbInventoryService, org.opendaylight.ovsdb.plugin.api.OvsdbInventoryListener {
    org.opendaylight.ovsdb.plugin.api.OvsdbInventoryService pluginOvsdbInventoryService;

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
        if(this.ovsdbInventoryListeners.contains(ovsdbInventoryListeners))
            this.ovsdbInventoryListeners.remove(ovsdbInventoryListeners);
    }

    @Override
    public ConcurrentMap<String, ConcurrentMap<String, Row>> getCache(Node n, String databaseName) {
        return pluginOvsdbInventoryService.getCache(n, databaseName);
    }


    @Override
    public ConcurrentMap<String, Row> getTableCache(Node n, String databaseName, String tableName) {
        return pluginOvsdbInventoryService.getTableCache(n, databaseName, tableName);
    }


    @Override
    public Row getRow(Node n, String databaseName, String tableName, String uuid) {
        return pluginOvsdbInventoryService.getRow(n, databaseName, tableName, uuid);
    }

    @Override
    public void updateRow(Node n, String databaseName, String tableName, String uuid, Row row) {
        pluginOvsdbInventoryService.updateRow(n, databaseName, tableName, uuid, row);
    }

    @Override
    public void removeRow(Node n, String databaseName, String tableName, String uuid) {
        pluginOvsdbInventoryService.removeRow(n, databaseName, tableName, uuid);
    }

    @Override
    public void processTableUpdates(Node n, String databaseName, TableUpdates tableUpdates) {
        pluginOvsdbInventoryService.processTableUpdates(n, databaseName, tableUpdates);
    }

    @Override
    public void printCache(Node n) {
        pluginOvsdbInventoryService.printCache(n);
    }

    @Override
    public void addNode(Node node, Set<Property> props) {
        pluginOvsdbInventoryService.addNode(node, props);
    }

    @Override
    public void notifyNodeAdded(Node node, InetAddress address, int port) {
        pluginOvsdbInventoryService.notifyNodeAdded(node, address, port);
    }

    @Override
    public void addNodeProperty(Node node, UpdateType type, Set<Property> props) {
        pluginOvsdbInventoryService.addNodeProperty(node, type, props);
    }

    @Override
    public void removeNode(Node node) {
        pluginOvsdbInventoryService.removeNode(node);
    }

    @Override
    public void nodeAdded(Node node, InetAddress address, int port) {
        for(OvsdbInventoryListener listener : this.ovsdbInventoryListeners)
            listener.nodeAdded(node, address, port);

    }

    @Override
    public void nodeRemoved(Node node) {
        for(OvsdbInventoryListener listener : this.ovsdbInventoryListeners)
            listener.nodeRemoved(node);

    }

    @Override
    public void rowAdded(Node node, String tableName, String uuid, Row row) {
        for(OvsdbInventoryListener listener : this.ovsdbInventoryListeners)
            listener.rowAdded(node, tableName, uuid, row);

    }

    @Override
    public void rowUpdated(Node node, String tableName, String uuid, Row old,
            Row row) {
        for(OvsdbInventoryListener listener : this.ovsdbInventoryListeners)
            listener.rowUpdated(node, tableName, uuid, old, row);

    }

    @Override
    public void rowRemoved(Node node, String tableName, String uuid, Row row,
            Object context) {
        for(OvsdbInventoryListener listener : this.ovsdbInventoryListeners)
            listener.rowRemoved(node, tableName, uuid, row, context);
    }
}
