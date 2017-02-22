/*
 * Copyright (c) 2014, 2015 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.lib.impl;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
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
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.util.CharsetUtil;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLPeerUnverifiedException;
import org.opendaylight.aaa.cert.api.ICertificateManager;
import org.opendaylight.ovsdb.lib.OvsdbClient;
import org.opendaylight.ovsdb.lib.OvsdbConnection;
import org.opendaylight.ovsdb.lib.OvsdbConnectionInfo.ConnectionType;
import org.opendaylight.ovsdb.lib.OvsdbConnectionInfo.SocketConnectionType;
import org.opendaylight.ovsdb.lib.OvsdbConnectionListener;
import org.opendaylight.ovsdb.lib.jsonrpc.ExceptionHandler;
import org.opendaylight.ovsdb.lib.jsonrpc.JsonRpcDecoder;
import org.opendaylight.ovsdb.lib.jsonrpc.JsonRpcEndpoint;
import org.opendaylight.ovsdb.lib.jsonrpc.JsonRpcServiceBinderHandler;
import org.opendaylight.ovsdb.lib.message.OvsdbRPC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * OvsDBConnectionService provides OVSDB connection management functionality which includes
 * both Active and Passive connections.
 * From the Library perspective, Active OVSDB connections are those that are initiated from
 * the Controller towards the ovsdb-manager.
 * While Passive OVSDB connections are those that are initiated from the ovs towards
 * the controller.
 *
 * <p>Applications that use OvsDBConnectionService can use the OvsDBConnection class' connect APIs
 * to initiate Active connections and can listen to the asynchronous Passive connections via
 * registerConnectionListener listener API.
 *
 * <p>The library is designed as Java modular component that can work in both OSGi and non-OSGi
 * environment. Hence a single instance of the service will be active (via Service Registry in OSGi)
 * and a Singleton object in a non-OSGi environment.
 */
public class OvsdbConnectionService implements AutoCloseable, OvsdbConnection {
    private static final Logger LOG = LoggerFactory.getLogger(OvsdbConnectionService.class);

    private static ThreadFactory passiveConnectionThreadFactory = new ThreadFactoryBuilder()
            .setNameFormat("OVSDBPassiveConnServ-%d").build();
    private static ScheduledExecutorService executorService
            = Executors.newScheduledThreadPool(10, passiveConnectionThreadFactory);

    private static ThreadFactory connectionNotifierThreadFactory = new ThreadFactoryBuilder()
            .setNameFormat("OVSDBConnNotifSer-%d").build();
    private static ExecutorService connectionNotifierService
            = Executors.newCachedThreadPool(connectionNotifierThreadFactory);

    private static Set<OvsdbConnectionListener> connectionListeners = Sets.newHashSet();
    private static Map<OvsdbClient, Channel> connections = new ConcurrentHashMap<>();
    private static OvsdbConnection connectionService;
    private static volatile boolean singletonCreated = false;
    private static final int IDLE_READER_TIMEOUT = 30;
    private static final int READ_TIMEOUT = 180;
    private static final String OVSDB_RPC_TASK_TIMEOUT_PARAM = "ovsdb-rpc-task-timeout";
    private static final String USE_SSL = "use-ssl";
    private static boolean useSSL = false;
    private static ICertificateManager certManagerSrv = null;

    private static int jsonRpcDecoderMaxFrameLength = 100000;

    private static final StalePassiveConnectionService STALE_PASSIVE_CONNECTION_SERVICE =
            new StalePassiveConnectionService(executorService);

    private static int retryPeriod = 100; // retry after 100 milliseconds


    public static OvsdbConnection getService() {
        if (connectionService == null) {
            connectionService = new OvsdbConnectionService();
        }
        return connectionService;
    }

    /**
     * If the SSL flag is enabled, the method internally will establish TLS communication using the default
     * ODL certificateManager SSLContext and attributes.
     */
    @Override
    public OvsdbClient connect(final InetAddress address, final int port) {
        if (useSSL) {
            if (certManagerSrv == null) {
                LOG.error("Certificate Manager service is not available cannot establish the SSL communication.");
                return null;
            }
            return connectWithSsl(address, port, certManagerSrv.getServerContext());
        } else {
            return connectWithSsl(address, port, null /* SslContext */);
        }
    }

    @Override
    public OvsdbClient connectWithSsl(final InetAddress address, final int port,
                               final SSLContext sslContext) {
        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(new NioEventLoopGroup());
            bootstrap.channel(NioSocketChannel.class);
            bootstrap.option(ChannelOption.TCP_NODELAY, true);
            bootstrap.option(ChannelOption.RCVBUF_ALLOCATOR, new AdaptiveRecvByteBufAllocator(65535, 65535, 65535));

            bootstrap.handler(new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(SocketChannel channel) throws Exception {
                    if (sslContext != null) {
                        /* First add ssl handler if ssl context is given */
                        SSLEngine engine =
                            sslContext.createSSLEngine(address.toString(), port);
                        engine.setUseClientMode(true);
                        channel.pipeline().addLast("ssl", new SslHandler(engine));
                    }
                    channel.pipeline().addLast(
                            //new LoggingHandler(LogLevel.INFO),
                            new JsonRpcDecoder(jsonRpcDecoderMaxFrameLength),
                            new StringEncoder(CharsetUtil.UTF_8),
                            new IdleStateHandler(IDLE_READER_TIMEOUT, 0, 0),
                            new ReadTimeoutHandler(READ_TIMEOUT),
                            new ExceptionHandler());
                }
            });

            ChannelFuture future = bootstrap.connect(address, port).sync();
            Channel channel = future.channel();
            return getChannelClient(channel, ConnectionType.ACTIVE, SocketConnectionType.SSL);
        } catch (InterruptedException e) {
            LOG.warn("Failed to connect {}:{}", address, port, e);
        }
        return null;
    }

    @Override
    public void disconnect(OvsdbClient client) {
        if (client == null) {
            return;
        }
        Channel channel = connections.get(client);
        if (channel != null) {
            channel.disconnect();
        }
        connections.remove(client);
    }

    @Override
    public void registerConnectionListener(OvsdbConnectionListener listener) {
        LOG.info("registerConnectionListener: registering {}", listener.getClass().getSimpleName());
        connectionListeners.add(listener);
        notifyAlreadyExistingConnectionsToListener(listener);
    }

    private void notifyAlreadyExistingConnectionsToListener(final OvsdbConnectionListener listener) {
        for (final OvsdbClient client : getConnections()) {
            connectionNotifierService.submit(new Runnable() {
                @Override
                public void run() {
                    LOG.trace("Connection {} notified to listener {}", client.getConnectionInfo(), listener);
                    listener.connected(client);
                }
            });
        }
    }

    @Override
    public void unregisterConnectionListener(OvsdbConnectionListener listener) {
        connectionListeners.remove(listener);
    }

    private static OvsdbClient getChannelClient(Channel channel, ConnectionType type,
        SocketConnectionType socketConnType) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.setSerializationInclusion(Include.NON_NULL);

        JsonRpcEndpoint factory = new JsonRpcEndpoint(objectMapper, channel);
        JsonRpcServiceBinderHandler binderHandler = new JsonRpcServiceBinderHandler(factory);
        binderHandler.setContext(channel);
        channel.pipeline().addLast(binderHandler);

        OvsdbRPC rpc = factory.getClient(channel, OvsdbRPC.class);
        OvsdbClientImpl client = new OvsdbClientImpl(rpc, channel, type, socketConnType);
        client.setConnectionPublished(true);
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
    @Override
    public synchronized boolean startOvsdbManager(final int ovsdbListenPort) {
        if (!singletonCreated) {
            LOG.info("startOvsdbManager: Starting");
            new Thread() {
                @Override
                public void run() {
                    ovsdbManager(ovsdbListenPort);
                }
            }.start();
            singletonCreated = true;
            return true;
        } else {
            return false;
        }
    }

    /**
     * Method that initiates the Passive OVSDB channel listening functionality
     * with ssl.By default the ovsdb passive connection will listen in port
     * 6640 which can be overridden using the ovsdb.listenPort system property.
     */
    @Override
    public synchronized boolean startOvsdbManagerWithSsl(final int ovsdbListenPort,
                                     final SSLContext sslContext, String[] protocols, String[] cipherSuites) {
        if (!singletonCreated) {
            new Thread() {
                @Override
                public void run() {
                    ovsdbManagerWithSsl(ovsdbListenPort, sslContext, protocols, cipherSuites);
                }
            }.start();
            singletonCreated = true;
            return true;
        } else {
            return false;
        }
    }

    /**
     * OVSDB Passive listening thread that uses Netty ServerBootstrap to open
     * passive connection handle channel callbacks.
     * If the SSL flag is enabled, the method internally will establish TLS communication using the default
     * ODL certificateManager SSLContext and attributes.
     */
    private static void ovsdbManager(int port) {
        if (useSSL) {
            if (certManagerSrv == null) {
                LOG.error("Certificate Manager service is not available cannot establish the SSL communication.");
                return;
            }
            ovsdbManagerWithSsl(port, certManagerSrv.getServerContext(), certManagerSrv.getTlsProtocols(),
                    certManagerSrv.getCipherSuites());
        } else {
            ovsdbManagerWithSsl(port, null /* SslContext */, null, null);
        }
    }

    /**
     * OVSDB Passive listening thread that uses Netty ServerBootstrap to open
     * passive connection with Ssl and handle channel callbacks.
     */
    private static void ovsdbManagerWithSsl(int port, final SSLContext sslContext, final String[] protocols,
            final String[] cipherSuites) {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap serverBootstrap = new ServerBootstrap();
            serverBootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 100)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        public void initChannel(SocketChannel channel) throws Exception {
                            LOG.debug("New Passive channel created : {}", channel);
                            if (sslContext != null) {
                                /* Add SSL handler first if SSL context is provided */
                                SSLEngine engine = sslContext.createSSLEngine();
                                engine.setUseClientMode(false); // work in a server mode
                                engine.setNeedClientAuth(true); // need client authentication
                                if (protocols != null && protocols.length > 0) {
                                    //Set supported protocols
                                    engine.setEnabledProtocols(protocols);
                                    LOG.debug("Supported ssl protocols {}",
                                            Arrays.toString(engine.getSupportedProtocols()));
                                    LOG.debug("Enabled ssl protocols {}",
                                            Arrays.toString(engine.getEnabledProtocols()));
                                }
                                if (cipherSuites != null && cipherSuites.length > 0) {
                                    //Set supported cipher suites
                                    engine.setEnabledCipherSuites(cipherSuites);
                                    LOG.debug("Enabled cipher suites {}",
                                            Arrays.toString(engine.getEnabledCipherSuites()));
                                }
                                channel.pipeline().addLast("ssl", new SslHandler(engine));
                            }

                            channel.pipeline().addLast(
                                 new JsonRpcDecoder(jsonRpcDecoderMaxFrameLength),
                                 new StringEncoder(CharsetUtil.UTF_8),
                                 new IdleStateHandler(IDLE_READER_TIMEOUT, 0, 0),
                                 new ReadTimeoutHandler(READ_TIMEOUT),
                                 new ExceptionHandler());

                            handleNewPassiveConnection(channel);
                        }
                    });
            serverBootstrap.option(ChannelOption.TCP_NODELAY, true);
            serverBootstrap.option(ChannelOption.RCVBUF_ALLOCATOR,
                    new AdaptiveRecvByteBufAllocator(65535, 65535, 65535));
            // Start the server.
            ChannelFuture channelFuture = serverBootstrap.bind(port).sync();
            Channel serverListenChannel = channelFuture.channel();
            // Wait until the server socket is closed.
            serverListenChannel.closeFuture().sync();
        } catch (InterruptedException e) {
            LOG.error("Thread interrupted", e);
        } finally {
            // Shut down all event loops to terminate all threads.
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    private static void handleNewPassiveConnection(OvsdbClient client) {
        ListenableFuture<List<String>> echoFuture = client.echo();
        LOG.debug("Send echo message to probe the OVSDB switch {}",client.getConnectionInfo());
        Futures.addCallback(echoFuture, new FutureCallback<List<String>>() {
            @Override
            public void onSuccess(@Nullable List<String> result) {
                LOG.debug("Probe was successful to OVSDB switch {}",client.getConnectionInfo());
                List<OvsdbClient> clientsFromSameNode = getPassiveClientsFromSameNode(client);
                if (clientsFromSameNode.size() == 0) {
                    notifyListenerForPassiveConnection(client);
                } else {
                    STALE_PASSIVE_CONNECTION_SERVICE.handleNewPassiveConnection(client, clientsFromSameNode);
                }
            }

            @Override
            public void onFailure(Throwable failureException) {
                LOG.error("Probe failed to OVSDB switch. Disconnecting the channel {}", client.getConnectionInfo());
                client.disconnect();
            }
        }, connectionNotifierService);
    }

    private static void handleNewPassiveConnection(final Channel channel) {
        if (!channel.isOpen()) {
            LOG.warn("Channel {} is not open, skipped further processing of the connection.",channel);
            return;
        }
        SslHandler sslHandler = (SslHandler) channel.pipeline().get("ssl");
        if (sslHandler != null) {
            class HandleNewPassiveSslRunner implements Runnable {
                public SslHandler sslHandler;
                public final Channel channel;
                private int retryTimes;

                HandleNewPassiveSslRunner(Channel channel, SslHandler sslHandler) {
                    this.channel = channel;
                    this.sslHandler = sslHandler;
                    this.retryTimes = 3;
                }

                @Override
                public void run() {
                    HandshakeStatus status = sslHandler.engine().getHandshakeStatus();
                    LOG.debug("Handshake status {}", status);
                    switch (status) {
                        case FINISHED:
                        case NOT_HANDSHAKING:
                            if (sslHandler.engine().getSession().getCipherSuite()
                                    .equals("SSL_NULL_WITH_NULL_NULL")) {
                                // Not begin handshake yet. Retry later.
                                LOG.debug("handshake not begin yet {}", status);
                                executorService.schedule(this, retryPeriod, TimeUnit.MILLISECONDS);
                            } else {
                              //Check if peer is trusted before notifying listeners
                                try {
                                    sslHandler.engine().getSession().getPeerCertificates();
                                    //Handshake done. Notify listener.
                                    OvsdbClient client = getChannelClient(channel, ConnectionType.PASSIVE,
                                        SocketConnectionType.SSL);
                                    handleNewPassiveConnection(client);
                                } catch (SSLPeerUnverifiedException e) {
                                    //Trust manager is still checking peer certificate. Retry later
                                    LOG.debug("Peer certifiacte is not verified yet {}", status);
                                    executorService.schedule(this, retryPeriod, TimeUnit.MILLISECONDS);
                                }
                            }
                            break;

                        case NEED_UNWRAP:
                        case NEED_TASK:
                            //Handshake still ongoing. Retry later.
                            LOG.debug("handshake not done yet {}", status);
                            executorService.schedule(this,  retryPeriod, TimeUnit.MILLISECONDS);
                            break;

                        case NEED_WRAP:
                            if (sslHandler.engine().getSession().getCipherSuite()
                                    .equals("SSL_NULL_WITH_NULL_NULL")) {
                                /* peer not authenticated. No need to notify listener in this case. */
                                LOG.error("Ssl handshake fail. channel {}", channel);
                            } else {
                                /*
                                 * peer is authenticated. Give some time to wait for completion.
                                 * If status is still NEED_WRAP, client might already disconnect.
                                 * This happens when the first time client connects to controller in two-way handshake.
                                 * After obtaining controller certificate, client will disconnect and start
                                 * new connection with controller certificate it obtained.
                                 * In this case no need to do anything for the first connection attempt. Just skip
                                 * since client will reconnect later.
                                 */
                                LOG.debug("handshake not done yet {}", status);
                                if (retryTimes > 0) {
                                    executorService.schedule(this,  retryPeriod, TimeUnit.MILLISECONDS);
                                } else {
                                    LOG.debug("channel closed {}", channel);
                                }
                                retryTimes--;
                            }
                            break;

                        default:
                            LOG.error("unknown hadshake status {}", status);
                    }
                }
            }

            executorService.schedule(new HandleNewPassiveSslRunner(channel, sslHandler),
                    retryPeriod, TimeUnit.MILLISECONDS);
        } else {
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    OvsdbClient client = getChannelClient(channel, ConnectionType.PASSIVE,
                        SocketConnectionType.NON_SSL);
                    handleNewPassiveConnection(client);
                }
            });
        }
    }

    public static void channelClosed(final OvsdbClient client) {
        LOG.info("Connection closed {}", client.getConnectionInfo().toString());
        connections.remove(client);
        if (client.isConnectionPublished()) {
            for (OvsdbConnectionListener listener : connectionListeners) {
                listener.disconnected(client);
            }
        }
        STALE_PASSIVE_CONNECTION_SERVICE.clientDisconnected(client);
    }

    @Override
    public Collection<OvsdbClient> getConnections() {
        return connections.keySet();
    }

    @Override
    public void close() throws Exception {
        LOG.info("OvsdbConnectionService closed");
        JsonRpcEndpoint.close();
    }

    @Override
    public OvsdbClient getClient(Channel channel) {
        for (OvsdbClient client : connections.keySet()) {
            Channel ctx = connections.get(client);
            if (ctx.equals(channel)) {
                return client;
            }
        }
        return null;
    }

    private static List<OvsdbClient> getPassiveClientsFromSameNode(OvsdbClient ovsdbClient) {
        List<OvsdbClient> passiveClients = new ArrayList<>();
        for (OvsdbClient client : connections.keySet()) {
            if (!client.equals(ovsdbClient)
                    && client.getConnectionInfo().getRemoteAddress()
                            .equals(ovsdbClient.getConnectionInfo().getRemoteAddress())
                    && client.getConnectionInfo().getType() == ConnectionType.PASSIVE) {
                passiveClients.add(client);
            }
        }
        return passiveClients;
    }

    public static void notifyListenerForPassiveConnection(final OvsdbClient client) {
        client.setConnectionPublished(true);
        for (final OvsdbConnectionListener listener : connectionListeners) {
            connectionNotifierService.submit(new Runnable() {
                @Override
                public void run() {
                    LOG.trace("Connection {} notified to listener {}", client.getConnectionInfo(), listener);
                    listener.connected(client);
                }
            });
        }
    }

    public void setOvsdbRpcTaskTimeout(int timeout) {
        JsonRpcEndpoint.setReaperInterval(timeout);
    }

    /**
     * Set useSSL flag.
     *
     * @param flag boolean for using ssl
     */
    public void setUseSsl(boolean flag) {
        useSSL = flag;
    }

    /**
     * Set default Certificate manager service.
     *
     * @param certificateManagerSrv reference
     */
    public void setCertificatManager(ICertificateManager certificateManagerSrv) {
        certManagerSrv = certificateManagerSrv;
    }

    /**
     * Read the value of json-rpc-decoder-max-frame-length config from configuration file
     * and set json rpc decoder  max frame length. This option is only configured at the
     * boot time of the controller. Any change at the run time will have no impact.
     * @param maxFrameLength Max frame length (default : 100000)
     */
    public void setJsonRpcDecoderMaxFrameLength(int maxFrameLength) {
        jsonRpcDecoderMaxFrameLength = maxFrameLength;
        LOG.info("Json Rpc Decoder Max Frame Length set to : {}", jsonRpcDecoderMaxFrameLength);
    }

    public void updateConfigParameter(Map<String, Object> configParameters) {
        LOG.debug("Config parameters received : {}", configParameters.entrySet());
        if (configParameters != null && !configParameters.isEmpty()) {
            for (Map.Entry<String, Object> paramEntry : configParameters.entrySet()) {
                if (paramEntry.getKey().equalsIgnoreCase(OVSDB_RPC_TASK_TIMEOUT_PARAM)) {
                    setOvsdbRpcTaskTimeout(Integer.parseInt((String)paramEntry.getValue()));
                } else if (paramEntry.getKey().equalsIgnoreCase(USE_SSL)) {
                    useSSL = Boolean.parseBoolean(paramEntry.getValue().toString());
                }
            }
        }
    }
}
