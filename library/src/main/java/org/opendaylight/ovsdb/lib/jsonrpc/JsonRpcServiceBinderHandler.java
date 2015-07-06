/*
 * Copyright (C) 2013 EBay Software Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Ashwin Raveendran, Madhu Venugopal
 */
package org.opendaylight.ovsdb.lib.jsonrpc;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Strings;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.SettableFuture;

public class JsonRpcServiceBinderHandler extends ChannelInboundHandlerAdapter {
    protected static final Logger logger = LoggerFactory.getLogger(JsonRpcServiceBinderHandler.class);
    Map<Object, SettableFuture<Object>> waitingForReply = Maps.newHashMap();
    ExecutorService executorService = Executors.newFixedThreadPool(10);
    JsonRpcEndpoint factory = null;
    Object context = null;

    public Object getContext() {
        return context;
    }

    public void setContext(Object context) {
        this.context = context;
    }

    public JsonRpcServiceBinderHandler(JsonRpcEndpoint factory) {
        this.factory = factory;
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
                    logger.error("NoSuchMethodException when handling {}", msg, e);
                }
            } else if (jsonNode.hasNonNull("method")) {
                if (jsonNode.has("id") && !Strings.isNullOrEmpty(jsonNode.get("id").asText())) {
                    factory.processRequest(context, jsonNode);
                } else {
                    logger.error("Request with null or empty id field: {} {}", jsonNode.get("method"),
                            jsonNode.get("params"));
                }
            }

            return;
        }
        ctx.channel().close();
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }
}
