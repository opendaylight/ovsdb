/*
 * Copyright (C) 2014 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Madhu Venugopal
 */

package org.opendaylight.ovsdb.lib.impl;

import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.AdaptiveRecvByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
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
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.opendaylight.ovsdb.lib.OvsdbClient;
import org.opendaylight.ovsdb.lib.OvsdbConnection;
import org.opendaylight.ovsdb.lib.OvsdbConnectionInfo.ConnectionType;
import org.opendaylight.ovsdb.lib.OvsdbConnectionListener;
import org.opendaylight.ovsdb.lib.jsonrpc.JsonRpcDecoder;
import org.opendaylight.ovsdb.lib.jsonrpc.JsonRpcEndpoint;
import org.opendaylight.ovsdb.lib.jsonrpc.JsonRpcServiceBinderHandler;
import org.opendaylight.ovsdb.lib.message.OvsdbRPC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

/**
 * OvsDBConnectionService provides OVSDB connection management functionality which includes
 * both Active and Passive connections.
 * From the Library perspective, Active OVSDB connections are those that are initiated from
 * the Controller towards the ovsdb-manager.
 * While Passive OVSDB connections are those that are initiated from the ovs towards
 * the controller.
 *
 * Applications that use OvsDBConnectionService can use the OvsDBConnection class' connect APIs
 * to initiate Active connections and can listen to the asynchronous Passive connections via
 * registerForPassiveConnection listener API.
 *
 * The library is designed as Java modular component that can work in both OSGi and non-OSGi
 * environment. Hence a single instance of the service will be active (via Service Registry in OSGi)
 * and a Singleton object in a non-OSGi environment.
 */
public class OvsdbConnectionService implements OvsdbConnection {
    private static final Logger logger = LoggerFactory.getLogger(OvsdbConnectionService.class);
    private final static int DEFAULT_SERVER_PORT = 6640;
    private static final String OVSDB_LISTENPORT = "ovsdb.listenPort";
    private final static int NUM_THREADS = 3;

    // Singleton Service object that can be used in Non-OSGi environment
    private static Set<OvsdbConnectionListener> connectionListeners = Sets.newHashSet();
    private static Map<OvsdbClient, Channel> connections = Maps.newHashMap();
    private static OvsdbConnection connectionService;
    private static int ovsdbListenPort = DEFAULT_SERVER_PORT;

    public static OvsdbConnection getService() {
        if (connectionService == null) {
            connectionService = new OvsdbConnectionService();
        }
        return connectionService;
    }
    @Override
    public OvsdbClient connect(InetAddress address, int port) {
        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(new NioEventLoopGroup());
            bootstrap.channel(NioSocketChannel.class);
            bootstrap.option(ChannelOption.TCP_NODELAY, true);
            bootstrap.option(ChannelOption.RCVBUF_ALLOCATOR, new AdaptiveRecvByteBufAllocator(65535, 65535, 65535));

            bootstrap.handler(new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(SocketChannel channel) throws Exception {
                    channel.pipeline().addLast(
                            //new LoggingHandler(LogLevel.INFO),
                            new JsonRpcDecoder(100000),
                            new StringEncoder(CharsetUtil.UTF_8));
                }
            });

            ChannelFuture future = bootstrap.connect(address, port).sync();
            Channel channel = future.channel();
            OvsdbClient client = getChannelClient(channel, ConnectionType.ACTIVE, Executors.newFixedThreadPool(NUM_THREADS));
            return client;
        } catch (InterruptedException e) {
            System.out.println("Thread was interrupted during connect");
        }
        return null;
    }

    @Override
    public void disconnect(OvsdbClient client) {
        if (client == null) return;
        Channel channel = connections.get(client);
        if (channel != null) {
            channel.disconnect();
        }
        connections.remove(client);
    }

    @Override
    public void registerForPassiveConnection(OvsdbConnectionListener listener) {
        connectionListeners.add(listener);
    }

    private static OvsdbClient getChannelClient(Channel channel, ConnectionType type,
                                                ExecutorService executorService) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.setSerializationInclusion(Include.NON_NULL);

        JsonRpcEndpoint factory = new JsonRpcEndpoint(objectMapper, channel);
        JsonRpcServiceBinderHandler binderHandler = new JsonRpcServiceBinderHandler(factory);
        binderHandler.setContext(channel);
        channel.pipeline().addLast(binderHandler);

        OvsdbRPC rpc = factory.getClient(channel, OvsdbRPC.class);
        OvsdbClientImpl client = new OvsdbClientImpl(rpc, channel, type, executorService);
        connections.put(client, channel);
        ChannelFuture closeFuture = channel.closeFuture();
        closeFuture.addListener(new ChannelConnectionHandler(client));
        return client;
    }

    /**
     * Method that initiates the Passive OVSDB channel listening functionality.
     * By default the ovsdb passive connection will listen in port 6640 which can
     * be overridden using the ovsdb.listenPort system property.
     */
    private static void startOvsdbManager() {
        String portString = System.getProperty(OVSDB_LISTENPORT);
        if (portString != null) {
            ovsdbListenPort = Integer.decode(portString).intValue();
        }

        new Thread() {
            @Override
            public void run() {
                ovsdbManager();
            }
        }.start();
    }


    /**
     * OVSDB Passive listening thread that uses Netty ServerBootstrap to open passive connection
     * and handle channel callbacks.
     */
    private static void ovsdbManager() {
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
                     channel.pipeline().addLast(
                             new JsonRpcDecoder(100000),
                             new StringEncoder(CharsetUtil.UTF_8));

                     handleNewPassiveConnection(channel);
                 }
             });
            b.option(ChannelOption.TCP_NODELAY, true);
            b.option(ChannelOption.RCVBUF_ALLOCATOR, new AdaptiveRecvByteBufAllocator(65535, 65535, 65535));
            // Start the server.
            ChannelFuture f = b.bind(ovsdbListenPort).sync();
            Channel serverListenChannel =  f.channel();
            // Wait until the server socket is closed.
            serverListenChannel.closeFuture().sync();
        } catch (InterruptedException e) {
            logger.error("Thread interrupted", e);
        } finally {
            // Shut down all event loops to terminate all threads.
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    static {
        startOvsdbManager();
    }

    private static void handleNewPassiveConnection(Channel channel) {
        OvsdbClient client = getChannelClient(channel, ConnectionType.PASSIVE, Executors.newFixedThreadPool(NUM_THREADS));
        for (OvsdbConnectionListener listener : connectionListeners) {
            listener.connected(client);
        }
    }

    public static void channelClosed(OvsdbClient client) {
        logger.info("Connection closed {}", client.getConnectionInfo().toString());
        connections.remove(client);
        for (OvsdbConnectionListener listener : connectionListeners) {
            listener.disconnected(client);
        }
    }
}
