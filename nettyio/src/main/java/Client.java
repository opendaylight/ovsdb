import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.slf4j.Logger;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

import static org.slf4j.LoggerFactory.getLogger;

public class Client {

    private static final Logger log = getLogger(Client.class);
    private ChannelFactory factory;
    private Channel channel;
    private final String host;
    private final int port;


    public Client(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void start() {
        ClientBootstrap bootstrap = new ClientBootstrap(new NioClientSocketChannelFactory(
                Executors.newCachedThreadPool(),
                Executors.newCachedThreadPool()));

        // ClientBootstrap bootstrap = new ClientBootstrap(factory);
        bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
            public ChannelPipeline getPipeline() throws Exception {
                return Channels.pipeline(
                        new JDecoder());
            }
        });
        IClientPipelineFactory ovsdbcFact = new IClientPipelineFactory();
        bootstrap.setPipelineFactory(ovsdbcFact);
        bootstrap.setOption("reuseAddr", true);
        bootstrap.setOption("child.keepAlive", true);
        bootstrap.setOption("child.tcpNoDelay", true);
        ChannelFuture future = bootstrap.connect(new InetSocketAddress(host, port));
        // ChannelFuture future = bootstrap.connect(new InetSocketAddress(PORT));
        future.awaitUninterruptibly();
        if (!future.isSuccess()) {
            log.error("Oh Noes, is your host running?: " + future.getCause(), future.getCause());
        } else {
            log.info("Connected ");
        }
        channel = future.getChannel();
    }

    public void stop() {
        channel.close().awaitUninterruptibly();
        factory.releaseExternalResources();
    }

    public void write(String s) {
        if (channel.isWritable()) {
            log.info("Client: writing '" + s);
            ChannelBuffer buf = ChannelBuffers.dynamicBuffer(s.length());
            buf.writeBytes(s.getBytes());
            ChannelFuture future = channel.write(buf);
            log.info("Still writing.. ");
            future.awaitUninterruptibly();
            log.info("Write finished..");
        } else
            log.info("Client: channel not good.");
    }
}


//    public void start() {
//        log.info("Client.start()");
//        factory = new NioClientSocketChannelFactory(
//                Executors.newCachedThreadPool(),
//                Executors.newCachedThreadPool());
//        ClientBootstrap bootstrap = new ClientBootstrap(factory);
//        bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
//            public ChannelPipeline getPipeline() throws Exception {
//                return Channels.pipeline(
//                        new ClientHandler());
//            }
//        });
//        bootstrap.setOption("child.tcpNoDelay", true);
//        bootstrap.setOption("child.keepAlive", true);
//        log.info("Client connecting to " + port + " ...");
//        ChannelFuture future = bootstrap.connect(new InetSocketAddress(host, port));
//        // ChannelFuture future = bootstrap.connect(new InetSocketAddress(PORT));
//        future.awaitUninterruptibly();
//        if (!future.isSuccess()) {
//            log.error("Oh Noes, is your host running?: " + future.getCause(), future.getCause());
//        } else {
//            log.info("Connected ");
//        }
//        channel = future.getChannel();
//    }