package org.opendaylight.ovsdb.plugin;

import org.opendaylight.controller.sal.core.Node;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;

public class ChannelConnectionHandler implements ChannelFutureListener {
    Node node;
    ConnectionService connectionService;
    public Node getNode() {
        return node;
    }
    public void setNode(Node node) {
        this.node = node;
    }
    public ConnectionService getConnectionService() {
        return connectionService;
    }
    public void setConnectionService(ConnectionService connectionService) {
        this.connectionService = connectionService;
    }
    @Override
    public void operationComplete(ChannelFuture arg0) throws Exception {
        connectionService.channelClosed(node);
    }
}
