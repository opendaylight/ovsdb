package org.opendaylight.ovsdb.internal.jsonrpc;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: araveendrann
 * Date: 10/6/13
 * Time: 11:19 PM
 * To change this template use File | Settings | File Templates.
 */
public class JsonRpcEncoder extends MessageToMessageEncoder<Object> {
    @Override
    protected void encode(ChannelHandlerContext ctx, Object msg, List<Object> out) throws Exception {

    }
}
