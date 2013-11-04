package org.opendaylight.ovsdb.plugin;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.CharsetUtil;

import org.opendaylight.controller.sal.connection.ConnectionConstants;
import org.opendaylight.controller.sal.connection.IPluginInConnectionService;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.controller.sal.utils.StatusCode;
import org.opendaylight.ovsdb.lib.database.DatabaseSchema;
import org.opendaylight.ovsdb.lib.jsonrpc.JsonRpcDecoder;
import org.opendaylight.ovsdb.lib.jsonrpc.JsonRpcEndpoint;
import org.opendaylight.ovsdb.lib.jsonrpc.JsonRpcServiceBinderHandler;
import org.opendaylight.ovsdb.lib.jsonrpc.ServiceHandler;
import org.opendaylight.ovsdb.lib.message.*;
import org.opendaylight.ovsdb.lib.table.Open_vSwitch;
import org.opendaylight.ovsdb.lib.table.internal.Table;
import org.opendaylight.ovsdb.lib.table.internal.Tables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.ListenableFuture;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;


/**
 * Represents the openflow plugin component in charge of programming the flows
 * the flow programming and relay them to functional modules above SAL.
 */
public class ConnectionService implements IPluginInConnectionService, IConnectionServiceInternal, OvsdbRPCListener {
    protected static final Logger logger = LoggerFactory.getLogger(ConnectionService.class);

    private static final Integer defaultOvsdbPort = 6640;
    private static Integer ovsdbListenPort = defaultOvsdbPort;
    private ConcurrentMap<String, Connection> ovsdbConnections;
    private List<ChannelHandler> handlers = null;
    private InventoryServiceInternal inventoryServiceInternal;
    private Channel serverListenChannel = null;
    private ServiceHandler serviceHandler;
    private ObjectMapper jacksonObjectMapper;

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
    void stop() {
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
    public void notifyClusterViewChanged() {
    }

    @Override
    public void notifyNodeDisconnectFromMaster(Node arg0) {
    }

    private Node handleNewConnection(String identifier, Channel channel, ConnectionService instance) throws InterruptedException, ExecutionException {
        Connection connection = new Connection(identifier, channel);
        Node node = connection.getNode();

        if (getJacksonObjectMapper() == null) {
            //todo: should we have this?
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            objectMapper.setSerializationInclusion(Include.NON_NULL);
            setJacksonObjectMapper(objectMapper);
        }

        JsonRpcEndpoint factory = new JsonRpcEndpoint(jacksonObjectMapper, channel, serviceHandler);
        JsonRpcServiceBinderHandler binderHandler = new JsonRpcServiceBinderHandler(factory);
        binderHandler.setNode(node);
        channel.pipeline().addLast(binderHandler);

        OvsdbRPC ovsdb = factory.getClient(node, OvsdbRPC.class);
        connection.setRpc(ovsdb);

        // Keeping the Initial inventory update(s) on its own thread.
        new Thread() {
            Connection connection;
            String identifier;

            public void run() {
                try {
                    initializeInventoryForNewNode(connection);
                    ovsdbConnections.put(identifier, connection);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
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

    private void initializeInventoryForNewNode (Connection connection) throws InterruptedException, ExecutionException {
        Channel channel = connection.getChannel();
        InetAddress address = ((InetSocketAddress)channel.remoteAddress()).getAddress();
        int port = ((InetSocketAddress)channel.remoteAddress()).getPort();
        IPAddressProperty addressProp = new IPAddressProperty(address);
        L4PortProperty l4Port = new L4PortProperty(port);
        inventoryServiceInternal.addNodeProperty(connection.getNode(), addressProp);
        inventoryServiceInternal.addNodeProperty(connection.getNode(), l4Port);

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
    }

    private void startOvsdbManager() {
        new Thread() {
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


    // ----------------OVSDBRpcListener impl ---------------//
    // -----------------------------------------------------//

    @Override
    public void update(Node node, UpdateNotification updateNotification) {
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

    @Override
    public String echo(String echo) {
        return echo;
    }


    // ------------------- Dependencies  -------------------//
    // -----------------------------------------------------//


    public ServiceHandler getServiceHandler() {
        return serviceHandler;
    }

    public void setServiceHandler(ServiceHandler serviceHandler) {
        this.serviceHandler = serviceHandler;
    }

    public ObjectMapper getJacksonObjectMapper() {
        return jacksonObjectMapper;
    }

    public void setJacksonObjectMapper(ObjectMapper jacksonObjectMapper) {
        this.jacksonObjectMapper = jacksonObjectMapper;
    }
}
