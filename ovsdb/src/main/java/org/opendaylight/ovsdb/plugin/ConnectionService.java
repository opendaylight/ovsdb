package org.opendaylight.ovsdb.plugin;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
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
import org.opendaylight.ovsdb.lib.message.MonitorRequestBuilder;
import org.opendaylight.ovsdb.lib.message.OvsdbRPC;
import org.opendaylight.ovsdb.lib.message.TableUpdates;
import org.opendaylight.ovsdb.lib.message.UpdateNotification;
import org.opendaylight.ovsdb.lib.table.Open_vSwitch;
import org.opendaylight.ovsdb.lib.table.internal.Table;
import org.opendaylight.ovsdb.lib.table.internal.Tables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.ListenableFuture;

import java.net.InetAddress;
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
public class ConnectionService implements IPluginInConnectionService, IConnectionServiceInternal, OvsdbRPC.Callback {
    protected static final Logger logger = LoggerFactory.getLogger(ConnectionService.class);

    private static final Integer defaultOvsdbPort = 6632;
    private ConcurrentMap<String, Connection> ovsdbConnections;
    private List<ChannelHandler> handlers = null;
    private InventoryServiceInternal inventoryServiceInternal;

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
    void stop() {
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
            Connection connection = new Connection(identifier, channel);
            Node node = connection.getNode();

            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

            JsonRpcEndpoint factory = new JsonRpcEndpoint(objectMapper, channel);
            JsonRpcServiceBinderHandler binderHandler = new JsonRpcServiceBinderHandler(factory);
            binderHandler.setNode(node);
            channel.pipeline().addLast(binderHandler);

            OvsdbRPC ovsdb = factory.getClient(node, OvsdbRPC.class);
            connection.setRpc(ovsdb);
            ovsdb.registerCallback(this);

            handleNewConnection(connection, address, port);
            ovsdbConnections.put(identifier, connection);
            return node;
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

    private void handleNewConnection (Connection connection, InetAddress address, int port) throws InterruptedException, ExecutionException {
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
            monitorReq.monitor(table);
        }

        ListenableFuture<TableUpdates> monResponse = connection.getRpc().monitor(monitorReq);
        TableUpdates updates = monResponse.get();
        UpdateNotification monitor = new UpdateNotification();
        monitor.setUpdate(updates);
        this.update(connection.getNode(), monitor);
    }

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

}
