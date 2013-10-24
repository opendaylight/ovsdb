package org.opendaylight.ovsdb.internal.jsonrpc;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.util.concurrent.TimeUnit;

public class NettyBootStrapper {

    EventLoopGroup bossGroup = null;
    EventLoopGroup workerGroup = null;
    ChannelFuture f = null;

    public ChannelFuture startServer(int localPort, final ChannelHandler... handlers) throws Exception {
        // Configure the server.
        bossGroup = new NioEventLoopGroup();
        workerGroup = new NioEventLoopGroup();
        ServerBootstrap b = new ServerBootstrap();
        b.group(bossGroup, workerGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, 100)
                .localAddress(localPort)
                .childOption(ChannelOption.TCP_NODELAY, true)
                .childHandler(new ChannelInitializer<SocketChannel>() {

                    @Override
                    public void initChannel(SocketChannel ch) throws Exception {
                        for (ChannelHandler handler : handlers) {
                            ch.pipeline().addLast(handler);
                        }
                    }
                });

        // Start the server.
        f = b.bind().sync();
        return f;
    }

    public void stopServer() throws InterruptedException {
        try {

            ChannelFuture channelFuture = f.channel().closeFuture();
            channelFuture.get(1000, TimeUnit.MILLISECONDS);
            if (!channelFuture.isDone()) {
                f.channel().unsafe().closeForcibly();
            }

            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();

            // Wait until all threads are terminated.
            bossGroup.terminationFuture().sync();
            workerGroup.terminationFuture().sync();
        } catch (Exception e) {
            //ignore
        }
    }

}
