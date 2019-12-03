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
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.ServerSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import java.util.concurrent.ThreadFactory;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A globally-instantiated context for use with OvsdbConnectionService.
 */
@Singleton
public class GlobalNettyContext implements AutoCloseable {
    private abstract static class Provider {
        abstract String name();

        abstract EventLoopGroup createGroup(ThreadFactory threadFactory);

        abstract Class<? extends SocketChannel> channelClass();

        abstract Class<? extends ServerSocketChannel> serverChannelClass();
    }

    private static final class EpollProvider extends Provider {
        @Override
        String name() {
            return "Epoll";
        }

        @Override
        EventLoopGroup createGroup(final ThreadFactory threadFactory) {
            return new EpollEventLoopGroup(0, threadFactory);
        }

        @Override
        Class<? extends SocketChannel> channelClass() {
            return EpollSocketChannel.class;
        }

        @Override
        Class<? extends ServerSocketChannel> serverChannelClass() {
            return EpollServerSocketChannel.class;
        }
    }

    private static final class NioProvider extends Provider {
        @Override
        String name() {
            return "NIO";
        }

        @Override
        EventLoopGroup createGroup(final ThreadFactory threadFactory) {
            return new NioEventLoopGroup(0, threadFactory);
        }

        @Override
        Class<? extends SocketChannel> channelClass() {
            return NioSocketChannel.class;
        }

        @Override
        Class<? extends ServerSocketChannel> serverChannelClass() {
            return NioServerSocketChannel.class;
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(GlobalNettyContext.class);

    private final Provider provider;
    private final EventLoopGroup bossGroup;
    private final EventLoopGroup workerGroup;

    @Inject
    public GlobalNettyContext() {
        provider = Epoll.isAvailable() ? new EpollProvider() : new NioProvider();
        bossGroup = provider.createGroup(new ThreadFactoryBuilder().setNameFormat("OVSDB listener-%d").build());
        workerGroup = provider.createGroup(new ThreadFactoryBuilder().setNameFormat("OVSDB connection-%d").build());
        LOG.info("OVSDB global Netty context instantiated with {} provider", provider.name());
    }

    Bootstrap newClient() {
        return new Bootstrap()
                .group(workerGroup)
                .channel(provider.channelClass())
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.RCVBUF_ALLOCATOR, new AdaptiveRecvByteBufAllocator(65535, 65535, 65535));
    }

    ServerBootstrap newServer() {
        return new ServerBootstrap()
                .group(bossGroup, workerGroup)
                .channel(provider.serverChannelClass())
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.RCVBUF_ALLOCATOR, new AdaptiveRecvByteBufAllocator(65535, 65535, 65535))
                .option(ChannelOption.SO_BACKLOG, 100);
    }

    @PreDestroy
    @Override
    public void close() {
        LOG.info("OVSDB global Netty context instantiated");
        bossGroup.shutdownGracefully().addListener(ignore -> {
            LOG.info("OVSDB global server group terminated");
        });
        workerGroup.shutdownGracefully().addListener(ignore -> {
            LOG.info("OVSDB global channel group terminated");
        });
    }
}
