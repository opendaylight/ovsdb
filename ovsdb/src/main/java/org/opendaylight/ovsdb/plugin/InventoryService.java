/*
 * Copyright (C) 2013 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Madhu Venugopal, Brent Salisbury
 */
package org.opendaylight.ovsdb.plugin;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.opendaylight.controller.sal.core.ConstructionException;
import org.opendaylight.controller.sal.core.Description;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.core.Property;
import org.opendaylight.controller.sal.core.UpdateType;
import org.opendaylight.controller.sal.inventory.IPluginInInventoryService;
import org.opendaylight.controller.sal.inventory.IPluginOutInventoryService;
import org.opendaylight.controller.sal.utils.HexEncode;
import org.opendaylight.controller.sal.utils.ServiceHelper;
import org.opendaylight.ovsdb.lib.database.DatabaseSchema;
import org.opendaylight.ovsdb.lib.message.TableUpdate;
import org.opendaylight.ovsdb.lib.message.TableUpdate.Row;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.notation.OvsDBSet;
import org.opendaylight.ovsdb.lib.table.Bridge;
import org.opendaylight.ovsdb.lib.table.internal.Table;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;

/**
 * Stub Implementation for IPluginInReadService used by SAL
 *
 *
 */
public class InventoryService implements IPluginInInventoryService, InventoryServiceInternal {
    private static final Logger logger = LoggerFactory
            .getLogger(InventoryService.class);
    private final Set<IPluginOutInventoryService> pluginOutInventoryServices =
            new CopyOnWriteArraySet<IPluginOutInventoryService>();
    private ConcurrentMap<Node, Map<String, Property>> nodeProps;
    private ConcurrentMap<NodeConnector, Map<String, Property>> nodeConnectorProps;
    private Map<Node, NodeDB> dbCache = Maps.newHashMap();
    private ScheduledExecutorService executor;

    /**
     * Function called by the dependency manager when all the required
     * dependencies are satisfied
     *
     */
    public void init() {
        nodeProps = new ConcurrentHashMap<Node, Map<String, Property>>();
        nodeConnectorProps = new ConcurrentHashMap<NodeConnector, Map<String, Property>>();
        Node.NodeIDType.registerIDType("OVS", String.class);
        NodeConnector.NodeConnectorIDType.registerIDType("OVS", String.class, "OVS");
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

    public void setPluginOutInventoryServices(IPluginOutInventoryService service) {
        if (this.pluginOutInventoryServices != null) {
            this.pluginOutInventoryServices.add(service);
        }
    }

    public void unsetPluginOutInventoryServices(
            IPluginOutInventoryService service) {
        if (this.pluginOutInventoryServices != null) {
            this.pluginOutInventoryServices.remove(service);
        }
    }

    /**
     * Retrieve nodes from openflow
     */
    @Override
    public ConcurrentMap<Node, Map<String, Property>> getNodeProps() {
        return nodeProps;
    }

    /**
     * Retrieve nodeConnectors from openflow
     */
    @Override
    public ConcurrentMap<NodeConnector, Map<String, Property>> getNodeConnectorProps(
            Boolean refresh) {
        return nodeConnectorProps;
    }


    @Override
    public Map<String, Map<String, Table<?>>> getCache(Node n) {
        NodeDB db = dbCache.get(n);
        if (db == null) return null;
        return db.getTableCache();
    }


    @Override
    public Map<String, Table<?>> getTableCache(Node n, String tableName) {
        NodeDB db = dbCache.get(n);
        if (db == null) return null;
        return db.getTableCache(tableName);
    }


    @Override
    public Table<?> getRow(Node n, String tableName, String uuid) {
        NodeDB db = dbCache.get(n);
        if (db == null) return null;
        return db.getRow(tableName, uuid);
    }

    @Override
    public void updateRow(Node n, String tableName, String uuid, Table<?> row) {
        NodeDB db = dbCache.get(n);
        if (db == null) {
            db = new NodeDB();
            dbCache.put(n, db);
        }
        db.updateRow(tableName, uuid, row);
    }

    @Override
    public void removeRow(Node n, String tableName, String uuid) {
        NodeDB db = dbCache.get(n);
        if (db != null) db.removeRow(tableName, uuid);
    }

    @Override
    public void processTableUpdates(Node n, TableUpdates tableUpdates) {
        NodeDB db = dbCache.get(n);
        if (db == null) {
            db = new NodeDB();
            dbCache.put(n, db);
        }

        OVSDBInventoryListener inventoryListener = (OVSDBInventoryListener)ServiceHelper.getGlobalInstance(OVSDBInventoryListener.class, this);
        Set<Table.Name> available = tableUpdates.availableUpdates();
        for (Table.Name name : available) {
            TableUpdate tableUpdate = tableUpdates.getUpdate(name);
            Collection<TableUpdate.Row<?>> rows = tableUpdate.getRows();
            for (Row<?> row : rows) {
                String uuid = row.getId();
                Table<?> newRow = (Table<?>)row.getNew();
                Table<?> oldRow = (Table<?>)row.getOld();
                if (newRow != null) {
                    db.updateRow(name.getName(), uuid, newRow);
                    if (name.getName().equalsIgnoreCase("bridge")) {
                        logger.debug("Received Bridge Table udpate for node {}", n);
                        // OVSDB has the Bridge name info while OpenFlow Spec is not
                        // Clear on that. From a user/manageability standpoint, it is easier
                        // to handle Bridge names compared to dpids.
                        // Updating the Openflow bridge name via the SAL Description update.
                        updateOFBridgeName(n, (Bridge)newRow);
                    }
                    if ((oldRow == null) && (inventoryListener != null)) {
                        inventoryListener.rowAdded(n, name.getName(), uuid, newRow);
                    } else if (inventoryListener != null) {
                        inventoryListener.rowUpdated(n, name.getName(), uuid, oldRow, newRow);
                    }
                } else if (oldRow != null) {
                    if (inventoryListener != null) {
                        inventoryListener.rowRemoved(n, name.getName(), uuid, oldRow);
                    }
                    db.removeRow(name.getName(), uuid);
                }
            }
        }
    }

    private void updateOFBridgeName(final Node node, final Bridge bridge) {
        Runnable updateNameRunnable = new Runnable() {
            @Override
            public void run() {
                OvsDBSet<String> dpids = bridge.getDatapath_id();
                String bridgeName = bridge.getName();
                if (dpids == null || bridgeName == null) return;
                for (String dpid : dpids) {
                    Long dpidLong = Long.valueOf(HexEncode.stringToLong(dpid));
                    try {
                        Node ofNode = new Node(Node.NodeIDType.OPENFLOW, dpidLong);
                        Description descProp = new Description(bridgeName);
                        Set<Property> props = new HashSet<Property>();
                        props.add(descProp);

                        IPluginOutInventoryService salInventoryService = (IPluginOutInventoryService) ServiceHelper.getInstance(
                                IPluginOutInventoryService.class, "default", this);
                        if (salInventoryService != null) {
                            logger.debug("Updating Bridge Name {} on OF node {}", bridgeName, ofNode);
                            salInventoryService.updateNode(ofNode, UpdateType.CHANGED, props);
                        } else {
                            logger.error("Error accessing SAL Inventory plugin");
                        }
                    } catch (ConstructionException e) {
                        logger.error("Failed to construct node for " + dpid, e);
                    }
                }
            }
        };
        executor.execute(updateNameRunnable);
        // Add a delay & re-execute to compensate for the current OpenFlow plugin bug of
        // overriding the Description with a Null value after the first statistics timer.
        // Hopefully this will be resolved in the newer version of the Openflow plugin.
        executor.schedule(updateNameRunnable, 30, TimeUnit.SECONDS);
    }

    @Override
    public void printCache(Node n) {
        NodeDB db = dbCache.get(n);
        if (db != null) db.printTableCache();
    }

    @Override
    public void addNode(Node node, Set<Property> props) {
        addNodeProperty(node, UpdateType.ADDED, props);
    }

    @Override
    public void notifyNodeAdded(Node node) {
        OVSDBInventoryListener inventoryListener = (OVSDBInventoryListener)ServiceHelper.getGlobalInstance(OVSDBInventoryListener.class, this);
        if (inventoryListener != null) {
            inventoryListener.nodeAdded(node);
        }
    }

    @Override
    public void addNodeProperty(Node node, UpdateType type, Set<Property> props) {
        Map<String, Property> nProp = nodeProps.get(node);
        if (nProp == null) nProp = new HashMap<String, Property>();
        for (Property prop : props) {
            nProp.put(prop.getName(), prop);
        }
        nodeProps.put(node, nProp);
        for (IPluginOutInventoryService service : pluginOutInventoryServices) {
            service.updateNode(node, type, props);
        }
    }

    @Override
    public DatabaseSchema getDatabaseSchema(Node n) {
        NodeDB db = dbCache.get(n);
        if (db != null) return db.getSchema();
        return null;
    }

    @Override
    public void updateDatabaseSchema(Node n, DatabaseSchema schema) {
        NodeDB db = dbCache.get(n);
        if (db == null) {
            db = new NodeDB();
            dbCache.put(n, db);
        }
        db.setSchema(schema);
    }

    @Override
    public void removeNode(Node node) {
        OVSDBInventoryListener inventoryListener = (OVSDBInventoryListener)ServiceHelper.getGlobalInstance(OVSDBInventoryListener.class, this);
        if (inventoryListener != null) {
            inventoryListener.nodeRemoved(node);
        }

        for (IPluginOutInventoryService service : pluginOutInventoryServices) {
            service.updateNode(node, UpdateType.REMOVED, null);
        }
        nodeProps.remove(node);
        dbCache.remove(node);
    }
}
