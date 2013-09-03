import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.*;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

class ServerHandler extends SimpleChannelHandler {

    private static final Logger log = getLogger(ServerHandler.class);
    private final Server server;

    ServerHandler(Server server) {
        this.server = server;
    }

    @Override
    public void channelOpen(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        log.info("Channel Open : " + e);
        server.channelOpen(e.getChannel());
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {

        ChannelBuffer buf = (ChannelBuffer) e.getMessage();
        int readableBytes = buf.readableBytes();
        if (readableBytes > 0) {
            log.info("Returned ReadableBytes" + readableBytes);
            byte[] bytes = new byte[readableBytes];
            buf.getBytes(0, bytes);
            String s = new String(bytes);
            log.info("Received: " + s);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
        logExceptionEventAndClose(e);
    }

    private void logExceptionEventAndClose(ExceptionEvent e) {
        final Throwable throwable = e.getCause();
        log.error("Unexpected: " + throwable, throwable);
        Channel ch = e.getChannel();
        ch.close();
    }
}
