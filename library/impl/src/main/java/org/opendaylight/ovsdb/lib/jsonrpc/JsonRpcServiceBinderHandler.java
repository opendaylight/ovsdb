/*
 * Copyright (c) 2013, 2015 EBay Software Foundation and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.lib.jsonrpc;

import static java.util.Objects.requireNonNull;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Strings;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JsonRpcServiceBinderHandler extends ChannelInboundHandlerAdapter {
    private static final Logger LOG = LoggerFactory.getLogger(JsonRpcServiceBinderHandler.class);

    private final JsonRpcEndpoint factory;
    private final Object context;

    public JsonRpcServiceBinderHandler(final JsonRpcEndpoint factory, final Object context) {
        this.factory = requireNonNull(factory);
        this.context = context;
    }

    @Override
    public void channelRead(final ChannelHandlerContext ctx, final Object msg) throws Exception {
        if (msg instanceof JsonNode) {
            JsonNode jsonNode = (JsonNode) msg;
            if (jsonNode.has("result")) {
                try {
                    factory.processResult(jsonNode);
                } catch (NoSuchMethodException e) {
                     /*
                       ChannelRead is a method invoked during Netty message receive event.
                       The only sane thing we can do is to print a meaningful error message.
                     */
                    LOG.error("NoSuchMethodException when handling {}", msg, e);
                }
            } else if (jsonNode.hasNonNull("method")) {
                if (jsonNode.has("id") && !Strings.isNullOrEmpty(jsonNode.get("id").asText())) {
                    factory.processRequest(context, jsonNode);
                } else {
                    LOG.debug("Request with null or empty id field: {} {}", jsonNode.get("method"),
                            jsonNode.get("params"));
                }
            }

            return;
        }
        ctx.channel().close();
    }

    @Override
    public void channelReadComplete(final ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }
}
