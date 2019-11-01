/*
 * Copyright © 2014, 2017 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.lib.impl;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLPeerUnverifiedException;
import org.apache.aries.blueprint.annotation.service.Reference;
import org.apache.aries.blueprint.annotation.service.Service;
import org.eclipse.jdt.annotation.Nullable;
import org.opendaylight.aaa.cert.api.ICertificateManager;
import org.opendaylight.ovsdb.lib.OvsdbClient;
import org.opendaylight.ovsdb.lib.OvsdbConnection;
import org.opendaylight.ovsdb.lib.OvsdbConnectionInfo.ConnectionType;
import org.opendaylight.ovsdb.lib.OvsdbConnectionInfo.SocketConnectionType;
import org.opendaylight.ovsdb.lib.OvsdbConnectionListener;
import org.opendaylight.ovsdb.lib.jsonrpc.ExceptionHandler;
import org.opendaylight.ovsdb.lib.jsonrpc.JsonRpcDecoder;
import org.opendaylight.ovsdb.lib.jsonrpc.JsonRpcEndpoint;
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
@Singleton
@Service(classes = OvsdbConnection.class)
public class OvsdbConnectionService implements AutoCloseable, OvsdbConnection {
    private static final Logger LOG = LoggerFactory.getLogger(OvsdbConnectionService.class);
    private static final int IDLE_READER_TIMEOUT = 30;
    private static final int READ_TIMEOUT = 180;
    private static final String OVSDB_RPC_TASK_TIMEOUT_PARAM = "ovsdb-rpc-task-timeout";
    private static final String USE_SSL = "use-ssl";
    private static final int RETRY_PERIOD = 100; // retry after 100 milliseconds

    private static final ScheduledExecutorService EXECUTOR_SERVICE = Executors.newScheduledThreadPool(10,
            new ThreadFactoryBuilder().setNameFormat("OVSDBPassiveConnServ-%d").build());

    private static final ExecutorService CONNECTION_NOTIFIER_SERVICE = Executors
            .newCachedThreadPool(new ThreadFactoryBuilder().setNameFormat("OVSDBConnNotifSer-%d").build());

    private static final StalePassiveConnectionService STALE_PASSIVE_CONNECTION_SERVICE =
            new StalePassiveConnectionService((client) -> {
                notifyListenerForPassiveConnection(client);
                return null;
            });

    private static final Set<OvsdbConnectionListener> CONNECTION_LISTENERS = ConcurrentHashMap.newKeySet();
    private static final Map<OvsdbClient, Channel> CONNECTIONS = new ConcurrentHashMap<>();

    private volatile boolean useSSL = false;
    private final ICertificateManager certManagerSrv;

    private volatile int jsonRpcDecoderMaxFrameLength = 100000;
    private volatile Channel serverChannel;

    private final AtomicBoolean singletonCreated = new AtomicBoolean(false);
    private volatile String listenerIp = "0.0.0.0";
    private volatile int listenerPort = 6640;

    @Inject
    public OvsdbConnectionService(@Reference(filter = "type=default-certificate-manager")
            final ICertificateManager certManagerSrv) {
        this.certManagerSrv = certManagerSrv;
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
            return connectWithSsl(address, port, certManagerSrv);
        } else {
            return connectWithSsl(address, port, null /* SslContext */);
        }
    }

    @Override
    @SuppressWarnings("checkstyle:IllegalCatch")
    public OvsdbClient connectWithSsl(final InetAddress address, final int port,
            final ICertificateManager certificateManagerSrv) {
        try {
            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(new NioEventLoopGroup());
            bootstrap.channel(NioSocketChannel.class);
            bootstrap.option(ChannelOption.TCP_NODELAY, true);
            bootstrap.option(ChannelOption.RCVBUF_ALLOCATOR, new AdaptiveRecvByteBufAllocator(65535, 65535, 65535));

            bootstrap.handler(new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(final SocketChannel channel) throws Exception {
                    if (certificateManagerSrv != null && certificateManagerSrv.getServerContext() != null) {
                        SSLContext sslContext = certificateManagerSrv.getServerContext();
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
                            new ExceptionHandler(OvsdbConnectionService.this));
                }
            });

            ChannelFuture future = bootstrap.connect(address, port).sync();
            Channel channel = future.channel();
            return getChannelClient(channel, ConnectionType.ACTIVE, SocketConnectionType.SSL);
        } catch (InterruptedException e) {
            LOG.warn("Failed to connect {}:{}", address, port, e);
        } catch (Throwable throwable) {
            // sync() re-throws exceptions declared as Throwable, so the compiler doesn't see them
            LOG.error("Error while binding to address {}, port {}", address, port, throwable);
            throw throwable;
        }
        return null;
    }

    @Override
    public void disconnect(final OvsdbClient client) {
        if (client == null) {
            return;
        }
        Channel channel = CONNECTIONS.get(client);
        if (channel != null) {
            //It's an explicit disconnect from user, so no need to notify back
            //to user about the disconnect.
            client.setConnectionPublished(false);
            channel.disconnect();
        }
        CONNECTIONS.remove(client);
    }

    @Override
    public void registerConnectionListener(final OvsdbConnectionListener listener) {
        LOG.info("registerConnectionListener: registering {}", listener.getClass().getSimpleName());
        CONNECTION_LISTENERS.add(listener);
        notifyAlreadyExistingConnectionsToListener(listener);
    }

    private void notifyAlreadyExistingConnectionsToListener(final OvsdbConnectionListener listener) {
        for (final OvsdbClient client : getConnections()) {
            CONNECTION_NOTIFIER_SERVICE.execute(() -> {
                LOG.trace("Connection {} notified to listener {}", client.getConnectionInfo(), listener);
                listener.connected(client);
            });
        }
    }

    @Override
    public void unregisterConnectionListener(final OvsdbConnectionListener listener) {
        CONNECTION_LISTENERS.remove(listener);
    }

    private static OvsdbClient getChannelClient(final Channel channel, final ConnectionType type,
            final SocketConnectionType socketConnType) {

        JsonRpcEndpoint endpoint = new JsonRpcEndpoint(channel);
        channel.pipeline().addLast(endpoint);

        OvsdbClientImpl client = new OvsdbClientImpl(endpoint, channel, type, socketConnType);
        client.setConnectionPublished(true);
        CONNECTIONS.put(client, channel);
        channel.closeFuture().addListener(new ChannelConnectionHandler(client));
        return client;
    }

    /**
     * Method that initiates the Passive OVSDB channel listening functionality.
     * By default the ovsdb passive connection will listen in port 6640 which can
     * be overridden using the ovsdb.listenPort system property.
     */
    @Override
    public synchronized boolean startOvsdbManager() {
        final int ovsdbListenerPort = this.listenerPort;
        final String ovsdbListenerIp = this.listenerIp;
        if (!singletonCreated.getAndSet(true)) {
            LOG.info("startOvsdbManager: Starting");
            new Thread(() -> ovsdbManager(ovsdbListenerIp, ovsdbListenerPort), "ovsdb-manager").start();
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
    public synchronized boolean startOvsdbManagerWithSsl(final String ovsdbListenIp, final int ovsdbListenPort,
                                                         final ICertificateManager certificateManagerSrv,
                                                         final String[] protocols, final String[] cipherSuites) {
        if (!singletonCreated.getAndSet(true)) {
            new Thread(() -> ovsdbManagerWithSsl(ovsdbListenIp, ovsdbListenPort,
                    certificateManagerSrv, protocols, cipherSuites), "ovsdb-manager-ssl-thread").start();
            return true;
        } else {
            return false;
        }
    }

    @Override
    public synchronized boolean restartOvsdbManagerWithSsl(final String ovsdbListenIp,
        final int ovsdbListenPort,
        final ICertificateManager certificateManagerSrv,
        final String[] protocols,
        final String[] cipherSuites) {
        if (singletonCreated.getAndSet(false) && serverChannel != null) {
            serverChannel.close();
            LOG.info("Server channel closed");
        }
        serverChannel = null;
        return startOvsdbManagerWithSsl(ovsdbListenIp, ovsdbListenPort,
            certificateManagerSrv, protocols, cipherSuites);
    }

    /**
     * OVSDB Passive listening thread that uses Netty ServerBootstrap to open
     * passive connection handle channel callbacks.
     * If the SSL flag is enabled, the method internally will establish TLS communication using the default
     * ODL certificateManager SSLContext and attributes.
     */
    private void ovsdbManager(final String ip, final int port) {
        if (useSSL) {
            if (certManagerSrv == null) {
                LOG.error("Certificate Manager service is not available cannot establish the SSL communication.");
                return;
            }
            ovsdbManagerWithSsl(ip, port, certManagerSrv, certManagerSrv.getTlsProtocols(),
                    certManagerSrv.getCipherSuites());
        } else {
            ovsdbManagerWithSsl(ip, port, null /* SslContext */, null, null);
        }
    }

    /**
     * OVSDB Passive listening thread that uses Netty ServerBootstrap to open
     * passive connection with Ssl and handle channel callbacks.
     */
    @SuppressWarnings("checkstyle:IllegalCatch")
    private void ovsdbManagerWithSsl(final String ip, final int port, final ICertificateManager certificateManagerSrv,
                                            final String[] protocols, final String[] cipherSuites) {
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
                        public void initChannel(final SocketChannel channel) throws Exception {
                            LOG.debug("New Passive channel created : {}", channel);
                            if (certificateManagerSrv != null && certificateManagerSrv.getServerContext() != null) {
                                /* Add SSL handler first if SSL context is provided */
                                SSLContext sslContext = certificateManagerSrv.getServerContext();
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
                                 new ExceptionHandler(OvsdbConnectionService.this));

                            handleNewPassiveConnection(channel);
                        }
                    });
            serverBootstrap.option(ChannelOption.TCP_NODELAY, true);
            serverBootstrap.option(ChannelOption.RCVBUF_ALLOCATOR,
                    new AdaptiveRecvByteBufAllocator(65535, 65535, 65535));
            // Start the server.
            ChannelFuture channelFuture = serverBootstrap.bind(ip, port).sync();
            Channel serverListenChannel = channelFuture.channel();
            serverChannel = serverListenChannel;
            // Wait until the server socket is closed.
            serverListenChannel.closeFuture().sync();
        } catch (InterruptedException e) {
            LOG.error("Thread interrupted", e);
        } catch (Throwable throwable) {
            // sync() re-throws exceptions declared as Throwable, so the compiler doesn't see them
            LOG.error("Error while binding to address {}, port {}", ip, port, throwable);
            throw throwable;
        } finally {
            // Shut down all event loops to terminate all threads.
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    private static void handleNewPassiveConnection(final OvsdbClient client) {
        ListenableFuture<List<String>> echoFuture = client.echo();
        LOG.debug("Send echo message to probe the OVSDB switch {}",client.getConnectionInfo());
        Futures.addCallback(echoFuture, new FutureCallback<List<String>>() {
            @Override
            public void onSuccess(@Nullable final List<String> result) {
                LOG.debug("Probe was successful to OVSDB switch {}",client.getConnectionInfo());
                List<OvsdbClient> clientsFromSameNode = getPassiveClientsFromSameNode(client);
                if (clientsFromSameNode.size() == 0) {
                    notifyListenerForPassiveConnection(client);
                } else {
                    STALE_PASSIVE_CONNECTION_SERVICE.handleNewPassiveConnection(client, clientsFromSameNode);
                }
            }

            @Override
            public void onFailure(final Throwable failureException) {
                LOG.error("Probe failed to OVSDB switch. Disconnecting the channel {}", client.getConnectionInfo());
                client.disconnect();
            }
        }, CONNECTION_NOTIFIER_SERVICE);
    }

    @SuppressFBWarnings(value = "UPM_UNCALLED_PRIVATE_METHOD",
            justification = "https://github.com/spotbugs/spotbugs/issues/811")
    private static void handleNewPassiveConnection(final Channel channel) {
        if (!channel.isOpen()) {
            LOG.warn("Channel {} is not open, skipped further processing of the connection.",channel);
            return;
        }
        SslHandler sslHandler = (SslHandler) channel.pipeline().get("ssl");
        if (sslHandler != null) {
            class HandleNewPassiveSslRunner implements Runnable {
                private int retryTimes = 3;

                private void retry() {
                    if (retryTimes > 0) {
                        EXECUTOR_SERVICE.schedule(this,  RETRY_PERIOD, TimeUnit.MILLISECONDS);
                    } else {
                        LOG.debug("channel closed {}", channel);
                        channel.disconnect();
                    }
                    retryTimes--;
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
                                retry();
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
                                    retry();
                                }
                            }
                            break;

                        case NEED_UNWRAP:
                        case NEED_TASK:
                            //Handshake still ongoing. Retry later.
                            LOG.debug("handshake not done yet {}", status);
                            retry();
                            break;

                        case NEED_WRAP:
                            if (sslHandler.engine().getSession().getCipherSuite()
                                    .equals("SSL_NULL_WITH_NULL_NULL")) {
                                /* peer not authenticated. No need to notify listener in this case. */
                                LOG.error("Ssl handshake fail. channel {}", channel);
                                channel.disconnect();
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
                                retry();
                            }
                            break;

                        default:
                            LOG.error("unknown hadshake status {}", status);
                    }
                }
            }

            EXECUTOR_SERVICE.schedule(new HandleNewPassiveSslRunner(),
                    RETRY_PERIOD, TimeUnit.MILLISECONDS);
        } else {
            EXECUTOR_SERVICE.execute(() -> {
                OvsdbClient client = getChannelClient(channel, ConnectionType.PASSIVE,
                    SocketConnectionType.NON_SSL);
                handleNewPassiveConnection(client);
            });
        }
    }

    public static void channelClosed(final OvsdbClient client) {
        LOG.info("Connection closed {}", client.getConnectionInfo());
        CONNECTIONS.remove(client);
        if (client.isConnectionPublished()) {
            for (OvsdbConnectionListener listener : CONNECTION_LISTENERS) {
                listener.disconnected(client);
            }
        }
        STALE_PASSIVE_CONNECTION_SERVICE.clientDisconnected(client);
    }

    @Override
    public Collection<OvsdbClient> getConnections() {
        return CONNECTIONS.keySet();
    }

    @Override
    public void close() throws Exception {
        LOG.info("OvsdbConnectionService closed");
        JsonRpcEndpoint.close();
    }

    @Override
    public OvsdbClient getClient(final Channel channel) {
        for (Entry<OvsdbClient, Channel> entry : CONNECTIONS.entrySet()) {
            OvsdbClient client = entry.getKey();
            Channel ctx = entry.getValue();
            if (ctx.equals(channel)) {
                return client;
            }
        }
        return null;
    }

    @SuppressFBWarnings(value = "UPM_UNCALLED_PRIVATE_METHOD",
            justification = "https://github.com/spotbugs/spotbugs/issues/811")
    private static List<OvsdbClient> getPassiveClientsFromSameNode(final OvsdbClient ovsdbClient) {
        List<OvsdbClient> passiveClients = new ArrayList<>();
        for (OvsdbClient client : CONNECTIONS.keySet()) {
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
        for (final OvsdbConnectionListener listener : CONNECTION_LISTENERS) {
            CONNECTION_NOTIFIER_SERVICE.execute(() -> {
                LOG.trace("Connection {} notified to listener {}", client.getConnectionInfo(), listener);
                listener.connected(client);
            });
        }
    }

    public void setOvsdbRpcTaskTimeout(final int timeout) {
        JsonRpcEndpoint.setReaperInterval(timeout);
    }

    /**
     * Set useSSL flag.
     *
     * @param flag boolean for using ssl
     */
    public void setUseSsl(final boolean flag) {
        useSSL = flag;
    }

    /**
     * Blueprint property setter method. Blueprint call this method and set the value of json rpc decoder
     * max frame length to the value configured for config option (json-rpc-decoder-max-frame-length) in
     * the configuration file. This option is only configured at the  boot time of the controller. Any
     * change at the run time will have no impact.
     * @param maxFrameLength Max frame length (default : 100000)
     */
    public void setJsonRpcDecoderMaxFrameLength(final int maxFrameLength) {
        jsonRpcDecoderMaxFrameLength = maxFrameLength;
        LOG.info("Json Rpc Decoder Max Frame Length set to : {}", jsonRpcDecoderMaxFrameLength);
    }

    public void setOvsdbListenerIp(final String ip) {
        LOG.info("OVSDB IP for listening connection is set to : {}", ip);
        listenerIp = ip;
    }

    public void setOvsdbListenerPort(final int portNumber) {
        LOG.info("OVSDB port for listening connection is set to : {}", portNumber);
        listenerPort = portNumber;
    }

    public void updateConfigParameter(final Map<String, Object> configParameters) {
        if (configParameters != null && !configParameters.isEmpty()) {
            LOG.debug("Config parameters received : {}", configParameters.entrySet());
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
