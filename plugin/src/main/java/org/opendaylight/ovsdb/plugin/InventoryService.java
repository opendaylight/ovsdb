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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutionException;
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
import org.opendaylight.ovsdb.lib.message.TableUpdate;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.notation.Row;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.schema.openvswitch.Bridge;
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
    private ConcurrentMap<Node, Map<String, Property>> nodeProps = new ConcurrentHashMap<Node, Map<String, Property>>();
    private ConcurrentMap<NodeConnector, Map<String, Property>> nodeConnectorProps = new ConcurrentHashMap<NodeConnector, Map<String, Property>>();
    private ConcurrentMap<Node, NodeDB> dbCache = Maps.newConcurrentMap();
    private ScheduledExecutorService executor;
    private OVSDBConfigService configurationService;

    /**
     * Function called by the dependency manager when all the required
     * dependencies are satisfied
     *
     */
    public void init() {
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
            this.pluginOutInventoryServices.add(service);
    }

    public void unsetPluginOutInventoryServices(IPluginOutInventoryService service) {
            this.pluginOutInventoryServices.remove(service);
    }

    public void setConfigurationService(OVSDBConfigService service) {
        configurationService = service;
    }

    public void unsetConfigurationService(OVSDBConfigService service) {
        configurationService = null;
    }

    @Override
    public ConcurrentMap<Node, Map<String, Property>> getNodeProps() {
        return nodeProps;
    }

    @Override
    public ConcurrentMap<NodeConnector, Map<String, Property>> getNodeConnectorProps(
            Boolean refresh) {
        return nodeConnectorProps;
    }


    @Override
    public ConcurrentMap<String, ConcurrentMap<String, Row>> getCache(Node n, String databaseName) {
        NodeDB db = dbCache.get(n);
        if (db == null) return null;
        return db.getDatabase(databaseName);
    }


    @Override
    public ConcurrentMap<String, Row> getTableCache(Node n, String databaseName, String tableName) {
        NodeDB db = dbCache.get(n);
        if (db == null) return null;
        return db.getTableCache(databaseName, tableName);
    }


    @Override
    public Row getRow(Node n, String databaseName, String tableName, String uuid) {
        NodeDB db = dbCache.get(n);
        if (db == null) return null;
        return db.getRow(databaseName, tableName, uuid);
    }

    @Override
    public void updateRow(Node n, String databaseName, String tableName, String uuid, Row row) {
        NodeDB db = dbCache.get(n);
        if (db == null) {
            db = new NodeDB();
            dbCache.put(n, db);
        }
        db.updateRow(databaseName, tableName, uuid, row);
    }

    @Override
    public void removeRow(Node n, String databaseName, String tableName, String uuid) {
        NodeDB db = dbCache.get(n);
        if (db != null) db.removeRow(databaseName, tableName, uuid);
    }

    @Override
    public void processTableUpdates(Node n, String databaseName, TableUpdates tableUpdates) {
        NodeDB db = dbCache.get(n);
        if (db == null) {
            db = new NodeDB();
            dbCache.put(n, db);
        }

        OVSDBInventoryListener inventoryListener = (OVSDBInventoryListener)ServiceHelper.getGlobalInstance(OVSDBInventoryListener.class, this);
        for (String tableName : tableUpdates.getUpdates().keySet()) {
            Map<String, Row> tCache = db.getTableCache(databaseName, tableName);
            TableUpdate update = tableUpdates.getUpdates().get(tableName);
            for (UUID uuid : (Set<UUID>)update.getRows().keySet()) {

            if (update.getNew(uuid) != null) {
                boolean isNewRow = (tCache == null || tCache.get(uuid.toString()) == null) ? true : false;
                db.updateRow(databaseName, tableName, uuid.toString(), update.getNew(uuid));
                if (isNewRow) {
                    this.handleOpenVSwitchSpecialCase(n, databaseName, tableName, uuid);
                    if (inventoryListener != null) inventoryListener.rowAdded(n, tableName, uuid.toString(), update.getNew(uuid));
                } else {
                    if (inventoryListener != null) inventoryListener.rowUpdated(n, tableName, uuid.toString(), update.getOld(uuid), update.getNew(uuid));
                }
            } else if (update.getOld(uuid) != null){
                if (tCache != null) {
                    if (inventoryListener != null) inventoryListener.rowRemoved(n, tableName, uuid.toString(), update.getOld(uuid), update.getNew(uuid));
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
                        if (configurationService != null) configurationService.setOFController(node, uuid.toString());
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }
                }
            };
            executor.execute(updateControllerRunnable);
        }
    }

    private void updateOFBridgeName(final Node node, final Bridge bridge) {
        Runnable updateNameRunnable = new Runnable() {
            @Override
            public void run() {
                Set<String> dpids = bridge.getDatapathIdColumn().getData();
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

    @Override
    public Set<Node> getConfiguredNotConnectedNodes() {
        return Collections.emptySet();
    }
}
