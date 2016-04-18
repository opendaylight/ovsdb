/*
 * Copyright (c) 2016 Intel Corp. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.lib.impl;

import io.netty.channel.EventLoopGroup;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generic place holder class for netty transport Epoll / Nio etc.
 */
public final class NettyTransportContainer {
    private static final Logger LOG = LoggerFactory.getLogger(OvsdbConnectionService.class);
    private Class<? extends ServerSocketChannel> serverSocketChannelClass;
    private Class<? extends SocketChannel> socketChannelClass;
    private EventLoopGroup workerGroup;
    private EventLoopGroup bossGroup;

    /**
     * Uses the {@link BootstrapType} to initialize {@link EventLoopGroup} and Channel class type.
     * @param bootstrap {@link BootstrapType}
     */
    public NettyTransportContainer(BootstrapType bootstrap) {
        boolean isEpollEnabled = Epoll.isAvailable();
        initiateTransport(isEpollEnabled, bootstrap);
    }
    /**
     * Initiate netty transpprt
     */
    private void initiateTransport(boolean isEpollEnabled, BootstrapType bootstrap) {
        if (isEpollEnabled) {
            initiateEpollTransport(bootstrap);
        } else {
            initiateNioTransport(bootstrap);
        }
    }

    /**
     * Initiate Nio transport groups
     */
    private void initiateNioTransport(BootstrapType bootstrap) {
        if (bootstrap.equals(BootstrapType.SERVER)) {
            this.serverSocketChannelClass = NioServerSocketChannel.class;
            this.bossGroup = new NioEventLoopGroup();
            this.workerGroup = new NioEventLoopGroup();
        } else if (bootstrap.equals(BootstrapType.CLIENT)) {
            this.socketChannelClass = NioSocketChannel.class;
            this.workerGroup = new NioEventLoopGroup();
        }
    }

    /**
     * Initiate Epoll native transport with Nio as fall back
     */
    private void initiateEpollTransport(BootstrapType bootstrap) {
        try {
            if (bootstrap.equals(BootstrapType.SERVER)) {
                this.serverSocketChannelClass = EpollServerSocketChannel.class;
                this.bossGroup = new EpollEventLoopGroup();
                this.workerGroup = new EpollEventLoopGroup();
            } else if (bootstrap.equals(BootstrapType.CLIENT)) {
                this.socketChannelClass = EpollSocketChannel.class;
                this.workerGroup = new EpollEventLoopGroup();
            }
            return;
        } catch (Throwable ex) {
            LOG.debug("Epoll initiation failed", ex);
        }

        //Fallback mechanism
        initiateNioTransport(bootstrap);
    }

    /**
     * Returns the channel class for bootstrap
     *
     * @return socketChannelClass
     */
    public Class<? extends ServerSocketChannel> getServerSocketChannelClass() {
        return this.serverSocketChannelClass;
    }

    /**
     * Returns the channel class for bootstrap
     *
     * @return socketChannelClass
     */
    public Class<? extends SocketChannel> getSocketChannelClass() {
        return this.socketChannelClass;
    }

    /**
     * Returns the EventLoopGroup for bossGroup
     *
     * @return EventLoopGroup
     */
    public EventLoopGroup getBossGroup() {
        return this.bossGroup;
    }

    /**
     * Returns the EventLoopGroup for workerGroup
     *
     * @return EventLoopGroup
     */
    public EventLoopGroup getWorkerGroup() {
        return this.workerGroup;
    }
}