package org.opendaylight.ovsdb.plugin;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.AdaptiveRecvByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.CharsetUtil;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
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
import org.opendaylight.controller.sal.utils.NetUtils;
import org.opendaylight.controller.sal.utils.ServiceHelper;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.controller.sal.utils.StatusCode;
import org.opendaylight.ovsdb.lib.database.DatabaseSchema;
import org.opendaylight.ovsdb.lib.jsonrpc.JsonRpcDecoder;
import org.opendaylight.ovsdb.lib.jsonrpc.JsonRpcEndpoint;
import org.opendaylight.ovsdb.lib.jsonrpc.JsonRpcServiceBinderHandler;
import org.opendaylight.ovsdb.lib.message.MonitorRequestBuilder;
import org.opendaylight.ovsdb.lib.message.OvsdbRPC;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.message.UpdateNotification;
import org.opendaylight.ovsdb.lib.table.Bridge;
import org.opendaylight.ovsdb.lib.table.Controller;
import org.opendaylight.ovsdb.lib.table.Open_vSwitch;
import org.opendaylight.ovsdb.lib.table.internal.Table;
import org.opendaylight.ovsdb.lib.table.internal.Tables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.ListenableFuture;


/**
 * Represents the openflow plugin component in charge of programming the flows
 * the flow programming and relay them to functional modules above SAL.
 */
public class ConnectionService implements IPluginInConnectionService, IConnectionServiceInternal, OvsdbRPC.Callback {
    protected static final Logger logger = LoggerFactory.getLogger(ConnectionService.class);

    private static final Integer defaultOvsdbPort = 6640;
    private static Integer ovsdbListenPort = defaultOvsdbPort;
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

    public void init() {
        ovsdbConnections = new ConcurrentHashMap<String, Connection>();
        int listenPort = defaultOvsdbPort;
        String portString = System.getProperty("ovsdb.listenPort");
        if (portString != null) {
            listenPort = Integer.decode(portString).intValue();
        }
        ovsdbListenPort = listenPort;
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
        startOvsdbManager();
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
            e.printStackTrace();
            return null;
        }

        try {
            port = Integer.parseInt(params.get(ConnectionConstants.PORT));
            if (port == 0) port = defaultOvsdbPort;
        } catch (Exception e) {
            port = defaultOvsdbPort;
        }

        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(new NioEventLoopGroup());
            bootstrap.channel(NioSocketChannel.class);
            bootstrap.option(ChannelOption.TCP_NODELAY, true);
            bootstrap.option(ChannelOption.RCVBUF_ALLOCATOR, new AdaptiveRecvByteBufAllocator(65535, 65535, 65535));

            bootstrap.handler(new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(SocketChannel channel) throws Exception {
                    if (handlers == null) {
                        channel.pipeline().addLast(
                                //new LoggingHandler(LogLevel.INFO),
                                new JsonRpcDecoder(100000),
                                new StringEncoder(CharsetUtil.UTF_8));
                    } else {
                        for (ChannelHandler handler : handlers) {
                            channel.pipeline().addLast(handler);
                        }
                    }
                }
            });

            ChannelFuture future = bootstrap.connect(address, port).sync();
            Channel channel = future.channel();
            return handleNewConnection(identifier, channel, this);
        } catch (Exception e) {
            e.printStackTrace();
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

    private Node handleNewConnection(String identifier, Channel channel, ConnectionService instance) throws InterruptedException, ExecutionException {
        Connection connection = new Connection(identifier, channel);
        Node node = connection.getNode();

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.setSerializationInclusion(Include.NON_NULL);

        JsonRpcEndpoint factory = new JsonRpcEndpoint(objectMapper, channel);
        JsonRpcServiceBinderHandler binderHandler = new JsonRpcServiceBinderHandler(factory);
        binderHandler.setNode(node);
        channel.pipeline().addLast(binderHandler);

        OvsdbRPC ovsdb = factory.getClient(node, OvsdbRPC.class);
        connection.setRpc(ovsdb);
        ovsdb.registerCallback(instance);
        ovsdbConnections.put(identifier, connection);

        ChannelConnectionHandler handler = new ChannelConnectionHandler();
        handler.setNode(node);
        handler.setConnectionService(this);
        ChannelFuture closeFuture = channel.closeFuture();
        closeFuture.addListener(handler);
        // Keeping the Initial inventory update(s) on its own thread.
        new Thread() {
            Connection connection;
            String identifier;

            @Override
            public void run() {
                try {
                    initializeInventoryForNewNode(connection);
                } catch (Exception e) {
                    e.printStackTrace();
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
        logger.error("Connection to Node : {} closed", node);
        inventoryServiceInternal.removeNode(node);
    }

    private void initializeInventoryForNewNode (Connection connection) throws InterruptedException, ExecutionException {
        Channel channel = connection.getChannel();
        InetAddress address = ((InetSocketAddress)channel.remoteAddress()).getAddress();
        int port = ((InetSocketAddress)channel.remoteAddress()).getPort();
        IPAddressProperty addressProp = new IPAddressProperty(address);
        L4PortProperty l4Port = new L4PortProperty(port);
        Set<Property> props = new HashSet<Property>();
        props.add(addressProp);
        props.add(l4Port);
        inventoryServiceInternal.addNode(connection.getNode(), props);

        List<String> dbNames = Arrays.asList(Open_vSwitch.NAME.getName());
        ListenableFuture<DatabaseSchema> dbSchemaF = connection.getRpc().get_schema(dbNames);
        DatabaseSchema databaseSchema = dbSchemaF.get();
        inventoryServiceInternal.updateDatabaseSchema(connection.getNode(), databaseSchema);

        MonitorRequestBuilder monitorReq = new MonitorRequestBuilder();
        for (Table<?> table : Tables.getTables()) {
            if (databaseSchema.getTables().keySet().contains(table.getTableName().getName())) {
                monitorReq.monitor(table);
            } else {
                logger.warn("We know about table {} but it is not in the schema of {}", table.getTableName().getName(), connection.getNode().getNodeIDString());
            }
        }

        ListenableFuture<TableUpdates> monResponse = connection.getRpc().monitor(monitorReq);
        TableUpdates updates = monResponse.get();
        if (updates.getError() != null) {
            logger.error("Error configuring monitor, error : {}, details : {}",
                    updates.getError(),
                    updates.getDetails());
            /* FIXME: This should be cause for alarm */
            throw new RuntimeException("Failed to setup a monitor in OVSDB");
        }
        UpdateNotification monitor = new UpdateNotification();
        monitor.setUpdate(updates);
        this.update(connection.getNode(), monitor);
        // With the existing bridges learnt, now it is time to update the OF Controller connections.
        this.updateOFControllers(connection.getNode());
        inventoryServiceInternal.notifyNodeAdded(connection.getNode());
    }

    private void startOvsdbManager() {
        new Thread() {
            @Override
            public void run() {
                ovsdbManager();
            }
        }.start();
    }

    private void ovsdbManager() {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
             .channel(NioServerSocketChannel.class)
             .option(ChannelOption.SO_BACKLOG, 100)
             .handler(new LoggingHandler(LogLevel.INFO))
             .childHandler(new ChannelInitializer<SocketChannel>() {
                 @Override
                 public void initChannel(SocketChannel channel) throws Exception {
                     logger.debug("New Passive channel created : "+ channel.toString());
                     InetAddress address = channel.remoteAddress().getAddress();
                     int port = channel.remoteAddress().getPort();
                     String identifier = address.getHostAddress()+":"+port;
                     channel.pipeline().addLast(
                             new LoggingHandler(LogLevel.INFO),
                             new JsonRpcDecoder(100000),
                             new StringEncoder(CharsetUtil.UTF_8));

                     Node node = handleNewConnection(identifier, channel, ConnectionService.this);
                     logger.debug("Connected Node : "+node.toString());
                 }
             });
            b.option(ChannelOption.TCP_NODELAY, true);
            b.option(ChannelOption.RCVBUF_ALLOCATOR, new AdaptiveRecvByteBufAllocator(65535, 65535, 65535));
            // Start the server.
            ChannelFuture f = b.bind(ovsdbListenPort).sync();
            serverListenChannel =  f.channel();
            // Wait until the server socket is closed.
            serverListenChannel.closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            // Shut down all event loops to terminate all threads.
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
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

    private List<InetAddress> getControllerIPAddresses() {
        List<InetAddress> controllers = null;
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

        controllers = new ArrayList<InetAddress>();
        String addressString = System.getProperty("ovsdb.controller.address");
        if (addressString == null) addressString = System.getProperty("of.address");

        if (addressString != null) {
            InetAddress controllerIP = null;
            try {
                controllerIP = InetAddress.getByName(addressString);
                if (controllerIP != null) {
                    controllers.add(controllerIP);
                    return controllers;
                }
            } catch (Exception e) {
                logger.debug("Invalid IP: {}, use wildcard *", addressString);
            }
        }

        Enumeration<NetworkInterface> nets;
        try {
            nets = NetworkInterface.getNetworkInterfaces();
            for (NetworkInterface netint : Collections.list(nets)) {
                Enumeration<InetAddress> inetAddresses = netint.getInetAddresses();
                for (InetAddress inetAddress : Collections.list(inetAddresses)) {
                    if (!inetAddress.isLoopbackAddress() &&
                            NetUtils.isIPv4AddressValid(inetAddress.getHostAddress())) {
                        controllers.add(inetAddress);
                    }
                }
            }
        } catch (SocketException e) {
            controllers.add(InetAddress.getLoopbackAddress());
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

        if (connection != null) {
            List<InetAddress> ofControllerAddrs = this.getControllerIPAddresses();
            short ofControllerPort = getControllerOFPort();
            for (InetAddress ofControllerAddress : ofControllerAddrs) {
                String newController = "tcp:"+ofControllerAddress.getHostAddress()+":"+ofControllerPort;
                Controller controllerRow = new Controller();
                controllerRow.setTarget(newController);
                OVSDBConfigService ovsdbTable = (OVSDBConfigService)ServiceHelper.getGlobalInstance(OVSDBConfigService.class, this);
                if (ovsdbTable != null) {
                    ovsdbTable.insertRow(node, Controller.NAME.getName(), bridgeUUID, controllerRow);
                }
            }
        }
        return true;
    }

    private void updateOFControllers (Node node) {
        Map<String, Table<?>> bridges = inventoryServiceInternal.getTableCache(node, Bridge.NAME.getName());
        for (String bridgeUUID : bridges.keySet()) {
            try {
                this.setOFController(node, bridgeUUID);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void update(Node node, UpdateNotification updateNotification) {
        if (updateNotification == null) return;
        inventoryServiceInternal.processTableUpdates(node, updateNotification.getUpdate());
    }

    @Override
    public void locked(Node node, List<String> ids) {
        // TODO Auto-generated method stub
    }

    @Override
    public void stolen(Node node, List<String> ids) {
        // TODO Auto-generated method stub
    }

}
