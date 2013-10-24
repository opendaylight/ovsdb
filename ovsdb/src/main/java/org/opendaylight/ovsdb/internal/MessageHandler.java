package org.opendaylight.ovsdb.internal;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

import org.opendaylight.ovsdb.message.EchoResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;

public class MessageHandler extends ChannelInboundHandlerAdapter {
    protected static final Logger logger = LoggerFactory.getLogger(MessageHandler.class);

    private Map<Long, MessageHandlerFuture> responseFutures = new HashMap<Long, MessageHandlerFuture>();

    public Future<Object> getResponse(long id) {
        MessageHandlerFuture responseFuture = new MessageHandlerFuture(Long.valueOf(id));
        responseFutures.put(Long.valueOf(id), responseFuture);
        return responseFuture;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
     //   logger.debug(ctx.channel().alloc().buffer().alloc().directBuffer().toString());

        logger.info("ChannRead ==> " + msg.toString());
        JsonNode jsonNode;
        ObjectMapper mapper = new ObjectMapper();
        String strmsg = msg.toString();
        try {
            jsonNode = mapper.readTree(strmsg);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        if (jsonNode.has("method")) {
            String method = jsonNode.get("method").toString();
        //    if (method.contains("echo")) {
                EchoResponse echoreply = new EchoResponse();
                JsonNode echoReplyJnode = mapper.valueToTree(echoreply);
                logger.debug("Echo Reply DP ==>" + msg);
                ctx.writeAndFlush(echoReplyJnode.toString());
            }
        }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }
}