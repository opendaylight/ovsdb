/*
 * Copyright (c) 2013, 2015 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.plugin.impl;

import io.netty.channel.ChannelHandler;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;

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
import org.opendaylight.ovsdb.plugin.api.Connection;
import org.opendaylight.ovsdb.plugin.api.ConnectionConstants;
import org.opendaylight.ovsdb.plugin.api.Status;
import org.opendaylight.ovsdb.plugin.api.StatusCode;
import org.opendaylight.ovsdb.plugin.api.OvsdbConnectionService;
import org.opendaylight.ovsdb.plugin.api.OvsdbInventoryService;
import org.opendaylight.ovsdb.utils.config.ConfigProperties;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;


/**
 * Represents the openflow plugin component in charge of programming the flows
 * the flow programming and relay them to functional modules above SAL.
 */
public class ConnectionServiceImpl implements OvsdbConnectionService,
                                              OvsdbConnectionListener {
    private static final Logger LOG = LoggerFactory.getLogger(ConnectionServiceImpl.class);

    // Properties that can be set in config.ini
    private static final Integer DEFAULT_OVSDB_PORT = 6640;
    private static final String OVSDB_LISTENPORT = "ovsdb.listenPort";


    public void putOvsdbConnection (String identifier, Connection connection) {
        ovsdbConnections.put(identifier, connection);
    }

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
        /* Start ovsdb server before getting connection clients */
        String portString = ConfigProperties.getProperty(OvsdbConnectionService.class, OVSDB_LISTENPORT);
        int ovsdbListenPort = DEFAULT_OVSDB_PORT;
        if (portString != null) {
            ovsdbListenPort = Integer.parseInt(portString);
        }

        if (!connectionLib.startOvsdbManager(ovsdbListenPort)) {
            LOG.warn("Start OVSDB manager call from ConnectionService was not necessary");
        }

        /* Then get connection clients */
        Collection<OvsdbClient> connections = connectionLib.getConnections();
        for (OvsdbClient client : connections) {
            LOG.info("CONNECT start connected clients client = {}", client);
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

    public Status disconnect(Node node) {
        Connection connection = getConnection(node);
        if (connection != null) {
            ovsdbConnections.remove(normalizeId(node.getId().getValue()));
            connection.disconnect();
            ovsdbInventoryService.removeNode(node);
            return new Status(StatusCode.SUCCESS);
        } else {
            return new Status(StatusCode.NOTFOUND);
        }
    }

    public Node connect(String identifier, Map<ConnectionConstants, String> params) {
        InetAddress address;
        Integer port;

        try {
            address = InetAddress.getByName(params.get(ConnectionConstants.ADDRESS));
        } catch (Exception e) {
            LOG.error("Unable to resolve {}", params.get(ConnectionConstants.ADDRESS), e);
            return null;
        }

        try {
            port = Integer.parseInt(params.get(ConnectionConstants.PORT));
            if (port == 0) {
                port = DEFAULT_OVSDB_PORT;
            }
        } catch (Exception e) {
            port = DEFAULT_OVSDB_PORT;
        }

        try {
            OvsdbClient client = connectionLib.connect(address, port);
            return handleNewConnection(identifier, client);
        } catch (InterruptedException e) {
            LOG.error("Thread was interrupted during connect", e);
        } catch (ExecutionException e) {
            LOG.error("ExecutionException in handleNewConnection for identifier " + identifier, e);
        }
        return null;
    }

    public List<ChannelHandler> getHandlers() {
        return handlers;
    }

    public void setHandlers(List<ChannelHandler> handlers) {
        this.handlers = handlers;
    }

    private String normalizeId (String identifier) {
        String id = identifier;

        String[] pair = identifier.split("\\|");
        if (pair[0].equals("OVS")) {
            id = pair[1];
        }

        return id;
    }

    @Override
    public Connection getConnection(Node node) {
        return ovsdbConnections.get(normalizeId(node.getId().getValue()));
    }

    @Override
    public Node getNode (String identifier) {
        Connection connection = ovsdbConnections.get(normalizeId(identifier));
        if (connection != null) {
            return connection.getNode();
        } else {
            return null;
        }
    }

    @Override
    public List<Node> getNodes() {
        List<Node> nodes = new ArrayList<>();
        for (Connection connection : ovsdbConnections.values()) {
            nodes.add(connection.getNode());
        }
        return nodes;
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
                    LOG.info("Initialize inventory for {}", connection.toString());
                    initializeInventoryForNewNode(connection);
                } catch (InterruptedException | ExecutionException | IOException e) {
                    LOG.error("Failed to initialize inventory for node with identifier {}", identifier, e);
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
        LOG.info("Connection to Node : {} closed", node);
        disconnect(node);
        ovsdbInventoryService.removeNode(node);
    }

    private void initializeInventoryForNewNode (Connection connection) throws InterruptedException, ExecutionException, IOException {
        OvsdbClient client = connection.getClient();
        InetAddress address = client.getConnectionInfo().getRemoteAddress();
        int port = client.getConnectionInfo().getRemotePort();

        List<String> databases = client.getDatabases().get();
        if (databases == null) {
            LOG.error("Unable to get Databases for the ovsdb connection : {}", client.getConnectionInfo());
            return;
        }
        for (String database : databases) {
            DatabaseSchema dbSchema = client.getSchema(database).get();
            TableUpdates updates = this.monitorTables(connection.getNode(), dbSchema);
            ovsdbInventoryService.processTableUpdates(connection.getNode(), dbSchema.getName(), updates);
        }
        LOG.info("Notifying Inventory Listeners for Node Added: {}", connection.getNode().toString());
        ovsdbInventoryService.notifyNodeAdded(connection.getNode(), address, port);
    }

    public TableUpdates monitorTables(Node node, DatabaseSchema dbSchema) throws ExecutionException, InterruptedException, IOException {
        Connection connection = getConnection(node);
        OvsdbClient client = connection.getClient();
        if (dbSchema == null) {
            LOG.error("Unable to get Database Schema for the ovsdb connection : {}", client.getConnectionInfo());
            return null;
        }
        Set<String> tables = dbSchema.getTables();
        if (tables == null) {
            LOG.warn("Database {} without any tables. Strange !", dbSchema.getName());
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
     * Each &lt;monitor-request&gt; specifies one or more columns and the manner in which the columns (or the entire table) are to be monitored.
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
        if (connection == null) {
            return;
        }
        this.disconnect(connection.getNode());
    }
}
