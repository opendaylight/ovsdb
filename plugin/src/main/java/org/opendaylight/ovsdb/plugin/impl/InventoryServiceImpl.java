/*
 * Copyright (c) 2013, 2015 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.plugin.impl;

import java.net.InetAddress;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.opendaylight.ovsdb.lib.message.TableUpdate;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.notation.Row;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.plugin.internal.NodeDatabase;
import org.opendaylight.ovsdb.plugin.api.OvsVswitchdSchemaConstants;
import org.opendaylight.ovsdb.plugin.api.OvsdbConfigurationService;
import org.opendaylight.ovsdb.plugin.api.OvsdbInventoryListener;
import org.opendaylight.ovsdb.plugin.api.OvsdbInventoryService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;

import com.google.common.collect.Sets;

import com.google.common.collect.Maps;

/**
 * Stub Implementation for IPluginInReadService used by SAL
 *
 *
 */
public class InventoryServiceImpl implements OvsdbInventoryService {
    private ConcurrentMap<Node, NodeDatabase> dbCache = Maps.newConcurrentMap();
    private ScheduledExecutorService executor;
    private OvsdbConfigurationService ovsdbConfigurationService;

    private Set<OvsdbInventoryListener> ovsdbInventoryListeners = Sets.newCopyOnWriteArraySet();

    /**
     * Function called by the dependency manager when all the required
     * dependencies are satisfied
     *
     */
    public void init() {
        this.executor = Executors.newSingleThreadScheduledExecutor();
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
        this.executor.shutdownNow();
    }

    public void setOvsdbConfigurationService(OvsdbConfigurationService service) {
        ovsdbConfigurationService = service;
    }

    public void unsetConfigurationService(OvsdbConfigurationService service) {
        ovsdbConfigurationService = null;
    }

    @Override
    public ConcurrentMap<String, ConcurrentMap<String, Row>> getCache(Node n, String databaseName) {
        NodeDatabase db = dbCache.get(n);
        if (db == null) {
            return null;
        }
        return db.getDatabase(databaseName);
    }


    @Override
    public ConcurrentMap<String, Row> getTableCache(Node n, String databaseName, String tableName) {
        NodeDatabase db = dbCache.get(n);
        if (db == null) {
            return null;
        }
        return db.getTableCache(databaseName, tableName);
    }


    @Override
    public Row getRow(Node n, String databaseName, String tableName, String uuid) {
        NodeDatabase db = dbCache.get(n);
        if (db == null) {
            return null;
        }
        return db.getRow(databaseName, tableName, uuid);
    }

    @Override
    public void updateRow(Node n, String databaseName, String tableName, String uuid, Row row) {
        NodeDatabase db = dbCache.get(n);
        if (db == null) {
            db = new NodeDatabase();
            dbCache.put(n, db);
        }
        db.updateRow(databaseName, tableName, uuid, row);
    }

    @Override
    public void removeRow(Node n, String databaseName, String tableName, String uuid) {
        NodeDatabase db = dbCache.get(n);
        if (db != null) {
            db.removeRow(databaseName, tableName, uuid);
        }
    }

    @Override
    public void processTableUpdates(Node n, String databaseName, TableUpdates tableUpdates) {
        NodeDatabase db = dbCache.get(n);
        if (db == null) {
            db = new NodeDatabase();
            dbCache.put(n, db);
        }

        for (String tableName : tableUpdates.getUpdates().keySet()) {
            Map<String, Row> tCache = db.getTableCache(databaseName, tableName);
            TableUpdate update = tableUpdates.getUpdates().get(tableName);
            for (UUID uuid : (Set<UUID>)update.getRows().keySet()) {

            if (update.getNew(uuid) != null) {
                boolean isNewRow = (tCache == null || tCache.get(uuid.toString()) == null);
                db.updateRow(databaseName, tableName, uuid.toString(), update.getNew(uuid));
                if (isNewRow) {
                    this.handleOpenVSwitchSpecialCase(n, databaseName, tableName, uuid);
                    if (!ovsdbInventoryListeners.isEmpty()) {
                        for (OvsdbInventoryListener listener : ovsdbInventoryListeners) {
                            listener.rowAdded(n, tableName, uuid.toString(), update.getNew(uuid));
                        }
                    }
                } else {
                    if (!ovsdbInventoryListeners.isEmpty()) {
                        for (OvsdbInventoryListener listener : ovsdbInventoryListeners) {
                            listener.rowUpdated(n, tableName, uuid.toString(), update.getOld(uuid), update.getNew(uuid));
                        }
                    }
                }
            } else if (update.getOld(uuid) != null){
                if (tCache != null) {
                    if (!ovsdbInventoryListeners.isEmpty()) {
                        for (OvsdbInventoryListener listener : ovsdbInventoryListeners) {
                            listener.rowRemoved(n, tableName, uuid.toString(), update.getOld(uuid), update.getNew(uuid));
                        }
                    }
                }
                db.removeRow(databaseName, tableName, uuid.toString());
            }
            }
        }
    }

    private void handleOpenVSwitchSpecialCase(final Node node, final String databaseName, final String tableName, final UUID uuid) {
        if (OvsVswitchdSchemaConstants.shouldConfigureController(databaseName, tableName)) {
            Runnable updateControllerRunnable = new Runnable() {
                @Override
                public void run() {
                    try {
                        if (ovsdbConfigurationService != null) {
                            ovsdbConfigurationService.setOFController(node, uuid.toString());
                        }
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }
                }
            };
            executor.execute(updateControllerRunnable);
        }
    }

    @Override
    public void printCache(Node n) {
        if ((dbCache != null) && (!dbCache.isEmpty())) {
            NodeDatabase db = dbCache.get(n);
            if (db != null) {
                db.printTableCache();
            }
        }
    }

    @Override
    public void notifyNodeAdded(Node node, InetAddress address, int port) {
        if (!ovsdbInventoryListeners.isEmpty()) {
            for (OvsdbInventoryListener listener : ovsdbInventoryListeners) {
                listener.nodeAdded(node, address, port);
            }
        }
    }

    @Override
    public void removeNode(Node node) {
        if (!ovsdbInventoryListeners.isEmpty()) {
            for (OvsdbInventoryListener listener : ovsdbInventoryListeners) {
                listener.nodeRemoved(node);
            }
        }

        dbCache.remove(node);
    }

    private void listenerAdded(OvsdbInventoryListener listener) {
        this.ovsdbInventoryListeners.add(listener);
    }

    private void listenerRemoved(OvsdbInventoryListener listener) {
        this.ovsdbInventoryListeners.remove(listener);
    }
}
