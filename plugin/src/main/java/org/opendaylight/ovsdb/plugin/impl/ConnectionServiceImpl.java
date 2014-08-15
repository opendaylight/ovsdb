/*
 * Copyright (C) 2013 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Madhu Venugopal, Brent Salisbury, Evan Zeller
 */
package org.opendaylight.ovsdb.plugin.impl;

import io.netty.channel.ChannelHandler;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;

import org.opendaylight.controller.sal.connection.ConnectionConstants;
import org.opendaylight.controller.sal.connection.IPluginInConnectionService;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.Property;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.controller.sal.utils.StatusCode;
import org.opendaylight.ovsdb.lib.MonitorCallBack;
import org.opendaylight.ovsdb.lib.OvsdbClient;
import org.opendaylight.ovsdb.lib.OvsdbConnection;
import org.opendaylight.ovsdb.lib.OvsdbConnectionInfo;
import org.opendaylight.ovsdb.lib.OvsdbConnectionListener;
import org.opendaylight.ovsdb.lib.message.MonitorRequest;
import org.opendaylight.ovsdb.lib.message.MonitorRequestBuilder;
import org.opendaylight.ovsdb.lib.message.MonitorSelect;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.lib.schema.GenericTableSchema;
import org.opendaylight.ovsdb.lib.schema.TableSchema;
import org.opendaylight.ovsdb.plugin.IConnectionServiceInternal;
import org.opendaylight.ovsdb.plugin.api.Connection;
import org.opendaylight.ovsdb.plugin.internal.IPAddressProperty;
import org.opendaylight.ovsdb.plugin.internal.L4PortProperty;
import org.opendaylight.ovsdb.plugin.api.OvsdbConnectionService;
import org.opendaylight.ovsdb.plugin.api.OvsdbInventoryService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;


/**
 * Represents the openflow plugin component in charge of programming the flows
 * the flow programming and relay them to functional modules above SAL.
 */
public class ConnectionServiceImpl implements IPluginInConnectionService,
                                              OvsdbConnectionService,
                                              IConnectionServiceInternal,
                                              OvsdbConnectionListener {
    protected static final Logger logger = LoggerFactory.getLogger(ConnectionServiceImpl.class);

    // Properties that can be set in config.ini
    private static final Integer defaultOvsdbPort = 6640;


    private ConcurrentMap<String, Connection> ovsdbConnections = new ConcurrentHashMap<String, Connection>();
    private List<ChannelHandler> handlers = null;

    private volatile OvsdbInventoryService ovsdbInventoryService;
    private volatile OvsdbConnection connectionLib;

    public void setOvsdbInventoryService(OvsdbInventoryService inventoryService) {
        this.ovsdbInventoryService = inventoryService;
    }

    public void setOvsdbConnection(OvsdbConnection ovsdbConnection) {
        this.connectionLib = ovsdbConnection;
    }

    public void init() {
        connectionLib.registerConnectionListener(this);
    }

    /**
     * Function called by the dependency manager when at least one dependency
     * become unsatisfied or when the component is shutting down because for
     * example bundle is being stopped.
     */
    void destroy() {
    }

    /**
     * Function called by dependency manager after "init ()" is called and after
     * the services provided by the class are registered in the service registry
     */
    void start() {
        Collection<OvsdbClient> connections = connectionLib.getConnections();
        for (OvsdbClient client : connections) {
            this.connected(client);
        }
    }

    /**
     * Function called by the dependency manager before the services exported by
     * the component are unregistered, this will be followed by a "destroy ()"
     * calls
     */
    void stopping() {
        for (Connection connection : ovsdbConnections.values()) {
            connection.disconnect();
        }
    }

    @Override
    public Status disconnect(Node node) {
        String identifier = (String) node.getID();
        Connection connection = ovsdbConnections.get(identifier);
        if (connection != null) {
            ovsdbConnections.remove(identifier);
            connection.disconnect();
            ovsdbInventoryService.removeNode(node);
            return new Status(StatusCode.SUCCESS);
        } else {
            return new Status(StatusCode.NOTFOUND);
        }
    }

    @Override
    public Node connect(String identifier, Map<ConnectionConstants, String> params) {
        InetAddress address;
        Integer port;

        try {
            address = InetAddress.getByName(params.get(ConnectionConstants.ADDRESS));
        } catch (Exception e) {
            logger.error("Unable to resolve " + params.get(ConnectionConstants.ADDRESS), e);
            return null;
        }

        try {
            port = Integer.parseInt(params.get(ConnectionConstants.PORT));
            if (port == 0) port = defaultOvsdbPort;
        } catch (Exception e) {
            port = defaultOvsdbPort;
        }

        try {
            OvsdbClient client = connectionLib.connect(address, port);
            return handleNewConnection(identifier, client);
        } catch (InterruptedException e) {
            logger.error("Thread was interrupted during connect", e);
        } catch (ExecutionException e) {
            logger.error("ExecutionException in handleNewConnection for identifier " + identifier, e);
        }
        return null;
    }

    public List<ChannelHandler> getHandlers() {
        return handlers;
    }

    public void setHandlers(List<ChannelHandler> handlers) {
        this.handlers = handlers;
    }

    @Override
    public Connection getConnection(Node node) {
        String identifier = (String) node.getID();
        return ovsdbConnections.get(identifier);
    }

    @Override
    public List<Node> getNodes() {
        List<Node> nodes = new ArrayList<Node>();
        for (Connection connection : ovsdbConnections.values()) {
            nodes.add(connection.getNode());
        }
        return nodes;
    }

    @Override
    public void notifyClusterViewChanged() {
    }

    @Override
    public void notifyNodeDisconnectFromMaster(Node arg0) {
    }

    private Node handleNewConnection(String identifier, OvsdbClient client) throws InterruptedException, ExecutionException {
        Connection connection = new Connection(identifier, client);
        Node node = connection.getNode();
        ovsdbConnections.put(identifier, connection);
        List<String> dbs = client.getDatabases().get();
        for (String db : dbs) {
            client.getSchema(db).get();
        }
        // Keeping the Initial inventory update(s) on its own thread.
        new Thread() {
            Connection connection;
            String identifier;

            @Override
            public void run() {
                try {
                    logger.info("Initialize inventory for {}", connection.toString());
                    initializeInventoryForNewNode(connection);
                } catch (InterruptedException | ExecutionException | IOException e) {
                    logger.error("Failed to initialize inventory for node with identifier " + identifier, e);
                    ovsdbConnections.remove(identifier);
                }
            }
            public Thread initializeConnectionParams(String identifier, Connection connection) {
                this.identifier = identifier;
                this.connection = connection;
                return this;
            }
        }.initializeConnectionParams(identifier, connection).start();
        return node;
    }

    public void channelClosed(Node node) throws Exception {
        logger.info("Connection to Node : {} closed", node);
        disconnect(node);
        ovsdbInventoryService.removeNode(node);
    }

    private void initializeInventoryForNewNode (Connection connection) throws InterruptedException, ExecutionException, IOException {
        OvsdbClient client = connection.getClient();
        InetAddress address = client.getConnectionInfo().getRemoteAddress();
        int port = client.getConnectionInfo().getRemotePort();
        IPAddressProperty addressProp = new IPAddressProperty(address);
        L4PortProperty l4Port = new L4PortProperty(port);
        Set<Property> props = new HashSet<Property>();
        props.add(addressProp);
        props.add(l4Port);
        logger.info("Add node to ovsdb inventory service {}", connection.getNode().toString());
        ovsdbInventoryService.addNode(connection.getNode(), props);

        List<String> databases = client.getDatabases().get();
        if (databases == null) {
            logger.error("Unable to get Databases for the ovsdb connection : {}", client.getConnectionInfo());
            return;
        }
        for (String database : databases) {
            DatabaseSchema dbSchema = client.getSchema(database).get();
            TableUpdates updates = this.monitorTables(connection.getNode(), dbSchema);
            ovsdbInventoryService.processTableUpdates(connection.getNode(), dbSchema.getName(), updates);
        }
        logger.info("Notifying Inventory Listeners for Node Added: {}", connection.getNode().toString());
        ovsdbInventoryService.notifyNodeAdded(connection.getNode(), address, port);
    }

    public TableUpdates monitorTables(Node node, DatabaseSchema dbSchema) throws ExecutionException, InterruptedException, IOException {
        String identifier = (String) node.getID();
        Connection connection = ovsdbConnections.get(identifier);
        OvsdbClient client = connection.getClient();
        if (dbSchema == null) {
            logger.error("Unable to get Database Schema for the ovsdb connection : {}", client.getConnectionInfo());
            return null;
        }
        Set<String> tables = dbSchema.getTables();
        if (tables == null) {
            logger.warn("Database {} without any tables. Strange !", dbSchema.getName());
            return null;
        }
        List<MonitorRequest<GenericTableSchema>> monitorRequests = Lists.newArrayList();
        for (String tableName : tables) {
            GenericTableSchema tableSchema = dbSchema.table(tableName, GenericTableSchema.class);
            monitorRequests.add(this.getAllColumnsMonitorRequest(tableSchema));
        }
        return client.monitor(dbSchema, monitorRequests, new UpdateMonitor(node));
    }

    /**
     * As per RFC 7047, section 4.1.5, if a Monitor request is sent without any columns, the update response will not include
     * the _uuid column.
     * ----------------------------------------------------------------------------------------------------------------------------------
     * Each <monitor-request> specifies one or more columns and the manner in which the columns (or the entire table) are to be monitored.
     * The "columns" member specifies the columns whose values are monitored. It MUST NOT contain duplicates.
     * If "columns" is omitted, all columns in the table, except for "_uuid", are monitored.
     * ----------------------------------------------------------------------------------------------------------------------------------
     * In order to overcome this limitation, this method
     *
     * @return MonitorRequest that includes all the Bridge Columns including _uuid
     */
    public <T extends TableSchema<T>> MonitorRequest<T> getAllColumnsMonitorRequest (T tableSchema) {
        Set<String> columns = tableSchema.getColumns();
        MonitorRequestBuilder<T> monitorBuilder = MonitorRequestBuilder.builder(tableSchema);
        for (String column : columns) {
            monitorBuilder.addColumn(column);
        }
        return monitorBuilder.with(new MonitorSelect(true, true, true, true)).build();
    }

    private class UpdateMonitor implements MonitorCallBack {
        Node node = null;
        public UpdateMonitor(Node node) {
            this.node = node;
        }

        @Override
        public void update(TableUpdates result, DatabaseSchema dbSchema) {
            ovsdbInventoryService.processTableUpdates(node, dbSchema.getName(), result);
        }

        @Override
        public void exception(Throwable t) {
            System.out.println("Exception t = " + t);
        }
    }

    private String getConnectionIdentifier(OvsdbClient client) {
        OvsdbConnectionInfo info = client.getConnectionInfo();
        return info.getRemoteAddress().getHostAddress()+":"+info.getRemotePort();
    }


    @Override
    public void connected(OvsdbClient client) {
        String identifier = getConnectionIdentifier(client);
        try {
            this.handleNewConnection(identifier, client);
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void disconnected(OvsdbClient client) {
        Connection connection = ovsdbConnections.get(this.getConnectionIdentifier(client));
        if (connection == null) return;
        this.disconnect(connection.getNode());
    }
}
