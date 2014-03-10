/*
 * Copyright (C) 2013 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Madhu Venugopal, Brent Salisbury
 */
package org.opendaylight.ovsdb.testbundle;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
//import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.core.Property;
import org.opendaylight.controller.sal.core.UpdateType;
import org.opendaylight.controller.switchmanager.IInventoryListener;
//import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.table.Interface;
import org.opendaylight.ovsdb.lib.table.Port;
import org.opendaylight.ovsdb.lib.table.internal.Table;
import org.opendaylight.ovsdb.plugin.IConnectionServiceInternal;
import org.opendaylight.ovsdb.plugin.OVSDBConfigService;
import org.opendaylight.ovsdb.testbundle.provider.ProviderNetworkManager;
import org.opendaylight.ovsdb.plugin.OVSDBInventoryListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SouthboundHandler implements OVSDBInventoryListener, IInventoryListener {
    static final Logger logger = LoggerFactory.getLogger(SouthboundHandler.class);
    //private Thread eventThread;
    private ExecutorService eventHandler;
    private BlockingQueue<SouthboundEvent> events;
    List<Node> nodeCache;

    protected OVSDBConfigService ovsdbConfigService;

    public OVSDBConfigService getOVSDBConfigService() {
        return ovsdbConfigService;
    }

    protected IConnectionServiceInternal connectionService;

    public IConnectionServiceInternal getConnectionService() {
        return connectionService;
    }

    public void unsetConnectionService(IConnectionServiceInternal s) {
        if (s == this.connectionService) {
            this.connectionService = null;
        }
    }

    void init() {
        eventHandler = Executors.newSingleThreadExecutor();
        this.events = new LinkedBlockingQueue<SouthboundEvent>();
        nodeCache = new ArrayList<>();
    }

    void start() {
        eventHandler.submit(new Runnable()  {
            @Override
            public void run() {
                while (true) {
                    SouthboundEvent ev;
                    try {
                        ev = events.take();
                    } catch (InterruptedException e) {
                        logger.info("The event handler thread was interrupted, shutting down", e);
                        return;
                    }
                    switch (ev.getType()) {
                    case NODE:
                        try {
                            processNodeUpdate(ev.getNode(), ev.getAction());
                        } catch (Exception e) {
                            logger.error("Exception caught in ProcessNodeUpdate for node " + ev.getNode(), e);
                        }
                        break;
//                    case ROW:
//                        try {
//                            processRowUpdate(ev.getNode(), ev.getTableName(), ev.getUuid(), ev.getRow(), ev.getAction());
//                        } catch (Exception e) {
//                            logger.error("Exception caught in ProcessRowUpdate for node " + ev.getNode(), e);
//                        }
//                        break;
                    default:
//                        logger.warn("Unable to process action " + ev.getAction() + " for node " + ev.getNode());
                    }
                }
            }
        });
        this.triggerUpdates();
    }

    void stop() {
        eventHandler.shutdownNow();
    }

    @Override
    public void nodeAdded(Node node) {
        this.enqueueEvent(new SouthboundEvent(node, SouthboundEvent.Action.ADD));
    }

    @Override
    public void nodeRemoved(Node node) {
        this.enqueueEvent(new SouthboundEvent(node, SouthboundEvent.Action.DELETE));
    }

    @Override
    public void rowAdded(Node node, String tableName, String uuid, Table<?> row) {
        this.enqueueEvent(new SouthboundEvent(node, tableName, uuid, row, SouthboundEvent.Action.ADD));
    }

    @Override
    public void rowUpdated(Node node, String tableName, String uuid, Table<?> oldRow, Table<?> newRow) {
        if (this.isUpdateOfInterest(oldRow, newRow)) {
            this.enqueueEvent(new SouthboundEvent(node, tableName, uuid, newRow, SouthboundEvent.Action.UPDATE));
        }
    }

    /*
     * Ignore unneccesary updates to be even considered for processing.
     * (Especially stats update are fast and furious).
     */

    private boolean isUpdateOfInterest(Table<?> oldRow, Table<?> newRow) {
        if (oldRow == null) return true;
        if (newRow.getTableName().equals(Interface.NAME)) {
            // We are NOT interested in Stats only updates
            Interface oldIntf = (Interface)oldRow;
            if (oldIntf.getName() == null && oldIntf.getExternal_ids() == null && oldIntf.getMac() == null &&
                oldIntf.getOfport() == null && oldIntf.getOptions() == null && oldIntf.getOther_config() == null &&
                oldIntf.getType() == null) {
                logger.trace("IGNORING Interface Update : "+newRow.toString());
                return false;
            }
        } else if (newRow.getTableName().equals(Port.NAME)) {
            // We are NOT interested in Stats only updates
            Port oldPort = (Port)oldRow;
            if (oldPort.getName() == null && oldPort.getExternal_ids() == null && oldPort.getMac() == null &&
                oldPort.getInterfaces() == null && oldPort.getTag() == null && oldPort.getTrunks() == null) {
                logger.trace("IGNORING Port Update : "+newRow.toString());
                return false;
            }
        }

        return true;
    }

    @Override
    public void rowRemoved(Node node, String tableName, String uuid, Table<?> row) {
        this.enqueueEvent(new SouthboundEvent(node, tableName, uuid, row, SouthboundEvent.Action.DELETE));
    }

    private void enqueueEvent (SouthboundEvent event) {
        try {
            events.put(event);
        } catch (InterruptedException e) {
            logger.error("Thread was interrupted while trying to enqueue event ", e);
        }

    }

    public void processNodeUpdate(Node node, SouthboundEvent.Action action) {
        if (action == SouthboundEvent.Action.DELETE) return;
        logger.trace("Process Node added {}", node);
        InternalNetworkManager.getManager().prepareInternalNetwork(node);
    }

    @Override
    public void notifyNode(Node node, UpdateType type, Map<String, Property> propMap) {
        logger.debug("Node {} update {} from Controller's inventory Service", node, type);

        // Add the Node Type check back once the Consistency issue is resolved between MD-SAL and AD-SAL
        if (!type.equals(UpdateType.REMOVED) && !nodeCache.contains(node)) {
            nodeCache.add(node);
            ProviderNetworkManager.getManager().initializeOFFlowRules(node);
        } else if (type.equals(UpdateType.REMOVED)){
            nodeCache.remove(node);
        }
    }

    @Override
    public void notifyNodeConnector(NodeConnector nodeConnector, UpdateType type, Map<String, Property> propMap) {
        //We are not interested in the nodeConnectors at this moment
    }

    private void triggerUpdates() {
        List<Node> nodes = this.getConnectionService().getNodes();
        if (nodes == null) return;
        for (Node node : nodes) {
            try {
                List<String> tableNames = this.getOVSDBConfigService().getTables(node);
                if (tableNames == null) continue;
                for (String tableName : tableNames) {
                    Map<String, Table<?>> rows = this.getOVSDBConfigService().getRows(node, tableName);
                    if (rows == null) continue;
                    for (String uuid : rows.keySet()) {
                        Table<?> row = rows.get(uuid);
                        this.rowAdded(node, tableName, uuid, row);
                    }
                }
            } catch (Exception e) {
                logger.error("Exception during OVSDB Southbound update trigger", e);
            }
        }
    }
}
