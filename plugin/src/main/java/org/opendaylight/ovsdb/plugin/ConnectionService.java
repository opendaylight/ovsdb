/*
 * Copyright (C) 2013 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Madhu Venugopal, Brent Salisbury, Evan Zeller
 */
package org.opendaylight.ovsdb.plugin;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;

import org.opendaylight.controller.clustering.services.IClusterGlobalServices;
import org.opendaylight.controller.sal.connection.ConnectionConstants;
import org.opendaylight.controller.sal.connection.IPluginInConnectionService;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.Property;
import org.opendaylight.controller.sal.utils.ServiceHelper;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.controller.sal.utils.StatusCode;
import org.opendaylight.ovsdb.lib.OvsdbClient;
import org.opendaylight.ovsdb.lib.OvsdbConnection;
import org.opendaylight.ovsdb.lib.OvsdbConnectionInfo;
import org.opendaylight.ovsdb.lib.OvsdbConnectionListener;
import org.opendaylight.ovsdb.lib.message.OvsdbRPC;
import org.opendaylight.ovsdb.lib.message.UpdateNotification;
import org.opendaylight.ovsdb.lib.notation.OvsDBSet;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.lib.table.Bridge;
import org.opendaylight.ovsdb.lib.table.Controller;
import org.opendaylight.ovsdb.lib.table.Open_vSwitch;
import org.opendaylight.ovsdb.lib.table.Table;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ListenableFuture;


/**
 * Represents the openflow plugin component in charge of programming the flows
 * the flow programming and relay them to functional modules above SAL.
 */
public class ConnectionService implements IPluginInConnectionService, IConnectionServiceInternal, OvsdbRPC.Callback, OvsdbConnectionListener {
    protected static final Logger logger = LoggerFactory.getLogger(ConnectionService.class);

    // Properties that can be set in config.ini
    private static final String OVSDB_LISTENPORT = "ovsdb.listenPort";
    private static final String OVSDB_AUTOCONFIGURECONTROLLER = "ovsdb.autoconfigurecontroller";
    protected static final String OPENFLOW_10 = "1.0";
    protected static final String OPENFLOW_13 = "1.3";

    private static final Integer defaultOvsdbPort = 6640;
    private static final boolean defaultAutoConfigureController = true;

    private OvsdbConnection connectionLib;
    private static Integer ovsdbListenPort = defaultOvsdbPort;
    private static boolean autoConfigureController = defaultAutoConfigureController;
    private ConcurrentMap<String, Connection> ovsdbConnections;
    private List<ChannelHandler> handlers = null;
    private InventoryServiceInternal inventoryServiceInternal;
    private Channel serverListenChannel = null;

    public InventoryServiceInternal getInventoryServiceInternal() {
        return inventoryServiceInternal;
    }

    public void setInventoryServiceInternal(InventoryServiceInternal inventoryServiceInternal) {
        this.inventoryServiceInternal = inventoryServiceInternal;
    }

    public void unsetInventoryServiceInternal(InventoryServiceInternal inventoryServiceInternal) {
        if (this.inventoryServiceInternal == inventoryServiceInternal) {
            this.inventoryServiceInternal = null;
        }
    }

    public void setOvsdbConnection(OvsdbConnection connectionService) {
        connectionLib = connectionService;
        // It is not correct to register the service here. Rather, we should depend on the
        // Service created by createServiceDependency() and hook to it via Apache DM.
        // Using this temporarily till the Service Dependency is resolved.
        connectionLib.registerForPassiveConnection(this);
    }

    public void unsetOvsdbConnection(OvsdbConnection connectionService) {
        connectionLib = null;
    }

    public void init() {
        ovsdbConnections = new ConcurrentHashMap<String, Connection>();
        int listenPort = defaultOvsdbPort;
        String portString = System.getProperty(OVSDB_LISTENPORT);
        if (portString != null) {
            listenPort = Integer.decode(portString).intValue();
        }
        ovsdbListenPort = listenPort;

        // Keep the default value if the property is not set
        if (System.getProperty(OVSDB_AUTOCONFIGURECONTROLLER) != null)
            autoConfigureController = Boolean.getBoolean(OVSDB_AUTOCONFIGURECONTROLLER);
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
        serverListenChannel.disconnect();
    }

    @Override
    public Status disconnect(Node node) {
        String identifier = (String) node.getID();
        Connection connection = ovsdbConnections.get(identifier);
        if (connection != null) {
            ovsdbConnections.remove(identifier);
            return connection.disconnect();
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
        // Keeping the Initial inventory update(s) on its own thread.
        new Thread() {
            Connection connection;
            String identifier;

            @Override
            public void run() {
                try {
                    initializeInventoryForNewNode(connection);
                } catch (InterruptedException | ExecutionException e) {
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
        inventoryServiceInternal.removeNode(node);
    }

    private void initializeInventoryForNewNode (Connection connection) throws InterruptedException, ExecutionException {
        OvsdbClient client = connection.getClient();
        InetAddress address = client.getConnectionInfo().getRemoteAddress();
        int port = client.getConnectionInfo().getRemotePort();
        IPAddressProperty addressProp = new IPAddressProperty(address);
        L4PortProperty l4Port = new L4PortProperty(port);
        Set<Property> props = new HashSet<Property>();
        props.add(addressProp);
        props.add(l4Port);
        inventoryServiceInternal.addNode(connection.getNode(), props);

        List<String> dbNames = Arrays.asList(Open_vSwitch.NAME.getName());
        ListenableFuture<DatabaseSchema> dbSchemaF = client.getSchema("Open_vSwitch");
        DatabaseSchema databaseSchema = dbSchemaF.get();
        inventoryServiceInternal.updateDatabaseSchema(connection.getNode(), databaseSchema);
/*
        MonitorRequestBuilder monitorReq = null; //ashwin(not sure if we need) : new MonitorRequestBuilder();
        for (Table<?> table : Tables.getTables()) {
            if (databaseSchema.getTables().contains(table.getTableName().getName())) {
                //ashwin(not sure if we need) monitorReq.monitor(table);
            } else {
                logger.debug("We know about table {} but it is not in the schema of {}", table.getTableName().getName(), connection.getNode().getNodeIDString());
            }
        }

        ListenableFuture<TableUpdates> monResponse = null; //TODO : ashwin(not sure if we need)connection.getRpc().monitor(monitorReq);
        TableUpdates updates = monResponse.get();
        if (updates.getError() != null) {
            logger.error("Error configuring monitor, error : {}, details : {}",
                    updates.getError(),
                    updates.getDetails());
            throw new RuntimeException("Failed to setup a monitor in OVSDB");
        }
        UpdateNotification monitor = new UpdateNotification();
        monitor.setUpdate(updates);
        this.update(connection.getNode(), monitor);
        if (autoConfigureController) {
            this.updateOFControllers(connection.getNode());
        }
        */
        inventoryServiceInternal.notifyNodeAdded(connection.getNode());
    }

    private IClusterGlobalServices clusterServices;

    public void setClusterServices(IClusterGlobalServices i) {
        this.clusterServices = i;
    }

    public void unsetClusterServices(IClusterGlobalServices i) {
        if (this.clusterServices == i) {
            this.clusterServices = null;
        }
    }

    private List<InetAddress> getControllerIPAddresses(Connection connection) {
        List<InetAddress> controllers = null;
        InetAddress controllerIP = null;

        controllers = new ArrayList<InetAddress>();
        String addressString = System.getProperty("ovsdb.controller.address");

        if (addressString != null) {
            try {
                controllerIP = InetAddress.getByName(addressString);
                if (controllerIP != null) {
                    controllers.add(controllerIP);
                    return controllers;
                }
            } catch (UnknownHostException e) {
                logger.error("Host {} is invalid", addressString);
            }
        }

        if (clusterServices != null) {
            controllers = clusterServices.getClusteredControllers();
            if (controllers != null && controllers.size() > 0) {
                if (controllers.size() == 1) {
                    InetAddress controller = controllers.get(0);
                    if (!controller.equals(InetAddress.getLoopbackAddress())) {
                        return controllers;
                    }
                } else {
                    return controllers;
                }
            }
        }

        addressString = System.getProperty("of.address");

        if (addressString != null) {
            try {
                controllerIP = InetAddress.getByName(addressString);
                if (controllerIP != null) {
                    controllers.add(controllerIP);
                    return controllers;
                }
            } catch (UnknownHostException e) {
                logger.error("Host {} is invalid", addressString);
            }
        }

        try {
            controllerIP = connection.getClient().getConnectionInfo().getLocalAddress();
            controllers.add(controllerIP);
            return controllers;
        } catch (Exception e) {
            logger.debug("Invalid connection provided to getControllerIPAddresses", e);
        }
        return controllers;
    }

    private short getControllerOFPort() {
        Short defaultOpenFlowPort = 6633;
        Short openFlowPort = defaultOpenFlowPort;
        String portString = System.getProperty("of.listenPort");
        if (portString != null) {
            try {
                openFlowPort = Short.decode(portString).shortValue();
            } catch (NumberFormatException e) {
                logger.warn("Invalid port:{}, use default({})", portString,
                        openFlowPort);
            }
        }
        return openFlowPort;
    }

    @Override
    public Boolean setOFController(Node node, String bridgeUUID) throws InterruptedException, ExecutionException {
        Connection connection = this.getConnection(node);
        if (connection == null) {
            return false;
        }

        OVSDBConfigService ovsdbTable = (OVSDBConfigService)ServiceHelper.getGlobalInstance(OVSDBConfigService.class, this);
        OvsDBSet<String> protocols = new OvsDBSet<String>();

        String ofVersion = System.getProperty("ovsdb.of.version", OPENFLOW_10);
        switch (ofVersion) {
            case OPENFLOW_13:
                protocols.add("OpenFlow13");
                break;
            case OPENFLOW_10:
            default:
                protocols.add("OpenFlow10");
                break;
        }

        Bridge bridge = new Bridge();
        bridge.setProtocols(protocols);
        Status status = ovsdbTable.updateRow(node, Bridge.NAME.getName(), null, bridgeUUID, bridge);
        logger.debug("Bridge {} updated to {} with Status {}", bridgeUUID, protocols.toArray()[0], status);

        List<InetAddress> ofControllerAddrs = this.getControllerIPAddresses(connection);
        short ofControllerPort = getControllerOFPort();
        for (InetAddress ofControllerAddress : ofControllerAddrs) {
            String newController = "tcp:"+ofControllerAddress.getHostAddress()+":"+ofControllerPort;
            Controller controllerRow = new Controller();
            controllerRow.setTarget(newController);
            if (ovsdbTable != null) {
                ovsdbTable.insertRow(node, Controller.NAME.getName(), bridgeUUID, controllerRow);
            }
        }
        return true;
    }

    private void updateOFControllers (Node node) {
        Map<String, Table<?>> bridges = inventoryServiceInternal.getTableCache(node, Bridge.NAME.getName());
        if (bridges == null) return;
        for (String bridgeUUID : bridges.keySet()) {
            try {
                this.setOFController(node, bridgeUUID);
            } catch (Exception e) {
                logger.error("Failed updateOFControllers", e);
            }
        }
    }

    @Override
    public void update(Object context, UpdateNotification updateNotification) {
        if (updateNotification == null) return;
        inventoryServiceInternal.processTableUpdates((Node)context, updateNotification.getUpdate());
    }

    @Override
    public void locked(Object context, List<String> ids) {
        // TODO Auto-generated method stub
    }

    @Override
    public void stolen(Object context, List<String> ids) {
        // TODO Auto-generated method stub
    }

    private String getConnectionIdentifier(OvsdbClient client) {
        OvsdbConnectionInfo info = client.getConnectionInfo();
        return info.getRemoteAddress().getHostAddress()+":"+info.getRemotePort();
    }


    @Override
    public void connected(OvsdbClient client) {
        String identifier = getConnectionIdentifier(client);
        try {
            ConnectionService connection = (ConnectionService)ServiceHelper.getGlobalInstance(IConnectionServiceInternal.class, this);
            Node node = connection.handleNewConnection(identifier, client);
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void disconnected(OvsdbClient client) {
    }
}
