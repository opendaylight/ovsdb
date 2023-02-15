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
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import java.util.concurrent.ThreadFactory;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A globally-instantiated context for use with OvsdbConnectionService.
 */
@Singleton
@Component(service = NettyBootstrapFactory.class)
public final class NettyBootstrapFactoryImpl implements NettyBootstrapFactory, AutoCloseable {
    private abstract static class Provider {
        /**
         * Return user friendly name, suitable for system operators.
         *
         * @return An admin-friendly name.
         */
        abstract String name();

        abstract EventLoopGroup createGroup(ThreadFactory threadFactory);

        abstract Bootstrap createBootstrap();

        abstract ServerBootstrap createServerBootstrap();
    }

    private static final class EpollProvider extends Provider {
        @Override
        String name() {
            return "epoll(7)";
        }

        @Override
        EventLoopGroup createGroup(final ThreadFactory threadFactory) {
            return new EpollEventLoopGroup(0, threadFactory);
        }

        @Override
        Bootstrap createBootstrap() {
            return new Bootstrap()
                    .channel(EpollSocketChannel.class);
        }

        @Override
        ServerBootstrap createServerBootstrap() {
            return new ServerBootstrap()
                    .channel(EpollServerSocketChannel.class);
        }
    }

    private static final class NioProvider extends Provider {
        @Override
        String name() {
            return "java.nio";
        }

        @Override
        EventLoopGroup createGroup(final ThreadFactory threadFactory) {
            return new NioEventLoopGroup(0, threadFactory);
        }

        @Override
        Bootstrap createBootstrap() {
            return new Bootstrap()
                    .channel(NioSocketChannel.class);
        }

        @Override
        ServerBootstrap createServerBootstrap() {
            return new ServerBootstrap()
                    .channel(NioServerSocketChannel.class);
        }
    }

    private static final Logger LOG = LoggerFactory.getLogger(NettyBootstrapFactoryImpl.class);

    // Minimum footprint runtime-constant
    private static final Provider PROVIDER = Epoll.isAvailable() ? new EpollProvider() : new NioProvider();

    private final EventLoopGroup bossGroup;
    private final EventLoopGroup workerGroup;

    @Inject
    @Activate
    public NettyBootstrapFactoryImpl() {
        bossGroup = PROVIDER.createGroup(new ThreadFactoryBuilder().setNameFormat("OVSDB listener-%d").build());
        workerGroup = PROVIDER.createGroup(new ThreadFactoryBuilder().setNameFormat("OVSDB connection-%d").build());
        LOG.info("OVSDB global Netty context started with {}", PROVIDER.name());
    }

    @PreDestroy
    @Deactivate
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

    @Override
    public Bootstrap newClient() {
        return PROVIDER.createBootstrap()
                .group(workerGroup)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.RCVBUF_ALLOCATOR, new AdaptiveRecvByteBufAllocator(65535, 65535, 65535));
    }

    @Override
    public ServerBootstrap newServer() {
        return PROVIDER.createServerBootstrap()
                .group(bossGroup, workerGroup)
                .option(ChannelOption.TCP_NODELAY, true)
                .option(ChannelOption.RCVBUF_ALLOCATOR, new AdaptiveRecvByteBufAllocator(65535, 65535, 65535))
                .option(ChannelOption.SO_BACKLOG, 100);
    }
}
