import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.*;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.ChannelGroupFuture;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.slf4j.Logger;

import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

import static org.slf4j.LoggerFactory.getLogger;


public class Server {


    private static final Logger log = getLogger(Server.class);
    private final ChannelGroup allChannels = new DefaultChannelGroup("example-server");
    private ChannelFactory factory;
    private final int port;

    public Server(int port) {
        this.port = port;
    }

    void start() {
        log.info("Server Starting...");
        final Server serverinstance = this;
        factory = new NioServerSocketChannelFactory(
                Executors.newCachedThreadPool(),
                Executors.newCachedThreadPool());
        ServerBootstrap bootstrap = new ServerBootstrap(factory);
        bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
            public ChannelPipeline getPipeline() throws Exception {
                return Channels.pipeline(
                        new ServerHandler(serverinstance)
                );
            }
        });
        bootstrap.setOption("child.tcpNoDelay", true);
        bootstrap.setOption("child.keepAlive", true);
        Channel channel = bootstrap.bind(new InetSocketAddress(port));
        allChannels.add(channel);
        log.info("Svr listening on " + port);
    }

    public void stop() {
        ChannelGroupFuture future = allChannels.close();
        future.awaitUninterruptibly();
        factory.releaseExternalResources();
    }

    public void channelOpen(Channel channel) {
        allChannels.add(channel);
    }
}



