package org.opendaylight.ovsdb.internal;
import java.io.IOException;
import java.net.Socket;
import java.util.Map;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.controller.sal.utils.StatusCode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.googlecode.jsonrpc4j.JsonRpcClient;
import com.googlecode.jsonrpc4j.JsonRpcClient.RequestListener;

public class Connection implements RequestListener {
    private Node node;
    private String identifier;
    private Channel channel;

    public Long getIdCounter() {
        return idCounter;
    }

    public void setIdCounter(Long idCounter) {
        this.idCounter = idCounter;
    }

    private Long idCounter;

    public Socket getSocket() {
        return socket;
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    private Socket socket;
    private JsonRpcClient rpcClient;
    private static final Logger logger = LoggerFactory.getLogger(Connection.class);

    public Connection(String identifier, Channel channel, JsonRpcClient rpcClient) {
        super();
        this.identifier = identifier;
        this.channel = channel;
        this.rpcClient = rpcClient;
        this.idCounter = 0L;
        rpcClient.setRequestListener(this);
        try {
        node = new Node("OVS", identifier);
        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Error creating Node {}", e.getMessage());
        }
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }
    public Channel getChannel(){
        return this.channel;
    }
    public void setChannel(Channel channel){
        this.channel = channel;
    }
    public JsonRpcClient getRpcClient() {
        return rpcClient;
    }
    public void setRpcClient(JsonRpcClient rpcClient) {
        this.rpcClient = rpcClient;
    }

    public Node getNode() {
        return node;
    }

    public void setNode(Node node) {
        this.node = node;
    }

    @Override
    public void onBeforeRequestSent(JsonRpcClient client, ObjectNode request) {
        request.remove("jsonrpc"); //ovsdb-server expects JSON-RPC v1.0
    }

    @Override
    public void onBeforeResponseProcessed(JsonRpcClient client,
            ObjectNode response) {

    }

    public void sendMessage(String message) throws IOException{
        try{
            channel.writeAndFlush(message);
            this.idCounter++;
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    public Status disconnect() {
        try {
            channel.pipeline().get("messageHandler");
            channel.close();
        } catch (Exception e) {
            e.printStackTrace();
            return new Status(StatusCode.INTERNALERROR, e.getMessage());
        }

        return new Status(StatusCode.SUCCESS);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((identifier == null) ? 0 : identifier.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Connection other = (Connection) obj;
        if (identifier == null) {
            if (other.identifier != null)
                return false;
        } else if (!identifier.equals(other.identifier))
            return false;
        return true;
    }
}
