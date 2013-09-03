package org.opendaylight.ovsdb.internal;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.*;
import io.netty.channel.*;
import io.netty.handler.codec.string.StringEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.Charset;

public class MessageHandler extends SimpleChannelInboundHandler<String> {
    protected static final Logger logger = LoggerFactory
            .getLogger(ConnectionService.class);
    @Override
    public void channelRead0(ChannelHandlerContext ctx, String msg){

        JsonNode jsonNode;
        ObjectMapper mapper = new ObjectMapper();

        try{
            jsonNode = mapper.readTree(msg);
        }catch(IOException e){
            e.printStackTrace();
            return;
        }

        if(jsonNode.has("method")){
            String method = jsonNode.get("method").toString();
            if(method.contains("echo")){
                String reply = "{\"result\":[], \"id\":\"echo\"}";
                ctx.writeAndFlush(reply);
            }
        }


    }
    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception{
        ctx.flush();
    }
}
