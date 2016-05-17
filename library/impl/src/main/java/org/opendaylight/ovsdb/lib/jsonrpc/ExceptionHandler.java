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
import io.netty.handler.codec.TooLongFrameException;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.ReadTimeoutException;

import org.opendaylight.ovsdb.lib.OvsdbClient;
import org.opendaylight.ovsdb.lib.error.InvalidEncodingException;
import org.opendaylight.ovsdb.lib.impl.OvsdbClientImpl;
import org.opendaylight.ovsdb.lib.impl.OvsdbConnectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class ExceptionHandler extends ChannelDuplexHandler {
    private static final Logger LOG = LoggerFactory.getLogger(ExceptionHandler.class);
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        if ((cause instanceof InvalidEncodingException)
                || (cause instanceof TooLongFrameException)) {
            ctx.channel().disconnect();
        }
        /* In cases where a connection is quickly established and the closed
        Catch the IOException and close the channel
         */
        if (cause instanceof IOException) {
            ctx.channel().close();
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            LOG.debug("Get idle state event");
            IdleStateEvent event = (IdleStateEvent) evt;
            if (event.state() == IdleState.READER_IDLE) {
                LOG.debug("Reader idle state. Send echo message to peer");
                //Send echo message to peer
                OvsdbClient client =
                             OvsdbConnectionService.getService().getClient(ctx.channel());
                if (client instanceof OvsdbClientImpl) {
                    OvsdbClientImpl ovsdbClientImpl = (OvsdbClientImpl)client;
                    if (!ovsdbClientImpl.echoSync()) {
                        // close channel if fail to send echo messge
                        ctx.channel().close();
                    }
                }
            }
        }
    }
}
