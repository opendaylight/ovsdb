/*
 * Copyright (c) 2013, 2015 EBay Software Foundation and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.lib.jsonrpc;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.TooLongFrameException;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.ReadTimeoutException;
import java.io.IOException;
import org.opendaylight.ovsdb.lib.OvsdbClient;
import org.opendaylight.ovsdb.lib.OvsdbConnection;
import org.opendaylight.ovsdb.lib.error.InvalidEncodingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExceptionHandler extends ChannelDuplexHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ExceptionHandler.class);

    private final OvsdbConnection ovsdbConnectionService;

    public ExceptionHandler(OvsdbConnection ovsdbConnectionService) {
        this.ovsdbConnectionService = ovsdbConnectionService;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        if (ctx.channel().isActive()) {
            LOG.warn("Exception occurred while processing connection pipeline", cause);
            if ((cause instanceof InvalidEncodingException)
                    || (cause instanceof TooLongFrameException || (cause instanceof DecoderException))) {
                LOG.info("Disconnecting channel to ovsdb {}", ctx.channel());
                ctx.channel().disconnect();
                return;
            }

        /* In cases where a connection is quickly established and the closed
        Catch the IOException and close the channel. Similarly if the peer is
        powered off, Catch the read time out exception and close the channel
         */
            if ((cause instanceof IOException) || (cause instanceof ReadTimeoutException)) {
                LOG.info("Closing channel to ovsdb {}", ctx.channel());
                ctx.channel().close();
                return;
            }

            LOG.error("Exception was not handled by the exception handler, re-throwing it for next handler");
            ctx.fireExceptionCaught(cause);
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        if (evt instanceof IdleStateEvent) {
            LOG.debug("Get idle state event");
            IdleStateEvent event = (IdleStateEvent) evt;
            if (event.state() == IdleState.READER_IDLE) {
                LOG.debug("Reader idle state. Send echo message to peer");
                //Send echo message to peer
                OvsdbClient client = ovsdbConnectionService.getClient(ctx.channel());
                client.echo();
            }
        }
    }
}
