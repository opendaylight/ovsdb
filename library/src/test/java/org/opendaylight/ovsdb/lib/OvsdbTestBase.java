/*
 * [[ Authors will Fill in the Copyright header ]]
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Brent Salisbury, Hugo Trippaers
 */
package org.opendaylight.ovsdb.lib;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.AdaptiveRecvByteBufAllocator;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.CharsetUtil;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

import org.opendaylight.ovsdb.lib.jsonrpc.JsonRpcDecoder;
import org.opendaylight.ovsdb.lib.jsonrpc.JsonRpcEndpoint;
import org.opendaylight.ovsdb.lib.jsonrpc.JsonRpcServiceBinderHandler;
import org.opendaylight.ovsdb.lib.message.OvsdbRPC;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

public abstract class OvsdbTestBase implements OvsdbRPC.Callback{
    private final static String identifier = "TEST";

    public Properties loadProperties() throws IOException {
        InputStream is = this
                .getClass()
                .getClassLoader()
                .getResourceAsStream(
                        "org/opendaylight/ovsdb/lib/message/integration-test.properties");
        if (is == null) {
            throw new IOException("Unable to load integration-test.properties");
        }
        Properties props = new Properties();
        props.load(is);

        return props;
    }

    private Channel connect(String addressStr, String portStr) {
        InetAddress address;
        try {
            address = InetAddress.getByName(addressStr);
        } catch (Exception e) {
            System.out.println("Unable to resolve " + addressStr);
            e.printStackTrace();
            return null;
        }

        Integer port;
        try {
            port = Integer.parseInt(portStr);
        } catch (NumberFormatException e) {
            System.out.println("Invalid port number : " + portStr);
            e.printStackTrace();
            return null;
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
                    channel.pipeline().addLast(
                            //new LoggingHandler(LogLevel.INFO),
                            new JsonRpcDecoder(100000),
                            new StringEncoder(CharsetUtil.UTF_8));
                }
            });

            ChannelFuture future = bootstrap.connect(address, port).sync();
            Channel channel = future.channel();
            return channel;
        } catch (InterruptedException e) {
            System.out.println("Thread was interrupted during connect");
        }
        return null;
    }

    public OvsdbRPC getTestConnection() throws IOException {
        Properties props = loadProperties();
        String address = props.getProperty("ovsdbserver.ipaddress");
        String port = props.getProperty("ovsdbserver.port", "6640");

        Channel channel = this.connect(address, port);
        if (channel == null) {
            throw new IOException("Failed to connecto to ovsdb server");
        }
        try {
            return this.handleNewConnection(channel);
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    private OvsdbRPC handleNewConnection(Channel channel) throws InterruptedException, ExecutionException {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.setSerializationInclusion(Include.NON_NULL);

        JsonRpcEndpoint factory = new JsonRpcEndpoint(objectMapper, channel);
        JsonRpcServiceBinderHandler binderHandler = new JsonRpcServiceBinderHandler(factory);
        binderHandler.setContext(channel);
        channel.pipeline().addLast(binderHandler);

        OvsdbRPC ovsdb = factory.getClient(channel, OvsdbRPC.class);
        ovsdb.registerCallback(this);
        return ovsdb;
    }
}
