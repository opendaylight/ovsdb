package org.opendaylight.ovsdb.internal.jsonrpc;

import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.TooLongFrameException;

public class ExceptionHandler extends ChannelHandlerAdapter {

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if ((cause instanceof InvalidEncodingException)
                || (cause instanceof TooLongFrameException)) {

            ctx.channel().disconnect();
        }
    }
}
