/*
 * Copyright © 2019 PANTHEON.tech, s.r.o. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.lib.impl;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import io.netty.bootstrap.Bootstrap;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.AdaptiveRecvByteBufAllocator;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A globally-instantiated context for use with OvsdbConnectionService.
 */
@Singleton
public class NettyBootstrapFactory implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(NettyBootstrapFactory.class);

    private final EventLoopGroup bossGroup = new NioEventLoopGroup(0,
        new ThreadFactoryBuilder().setNameFormat("OVSDB listener-%d").build());
    private final EventLoopGroup workerGroup = new NioEventLoopGroup(0,
        new ThreadFactoryBuilder().setNameFormat("OVSDB connection-%d").build());

    @Inject
    public NettyBootstrapFactory() {
        LOG.info("OVSDB global Netty context instantiated");
    }

    Bootstrap newClient() {
        return new Bootstrap()
                .group(workerGroup)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.RCVBUF_ALLOCATOR, new AdaptiveRecvByteBufAllocator(65535, 65535, 65535));
    }

    ServerBootstrap newServer() {
        return new ServerBootstrap()
                .group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.RCVBUF_ALLOCATOR, new AdaptiveRecvByteBufAllocator(65535, 65535, 65535))
                .option(ChannelOption.SO_BACKLOG, 100);
    }

    @PreDestroy
    @Override
    public void close() {
        LOG.info("OVSDB global Netty context terminating");
        bossGroup.shutdownGracefully().addListener(ignore -> {
            LOG.info("OVSDB global server group terminated");
        });
        workerGroup.shutdownGracefully().addListener(ignore -> {
            LOG.info("OVSDB global channel group terminated");
        });
    }
}
