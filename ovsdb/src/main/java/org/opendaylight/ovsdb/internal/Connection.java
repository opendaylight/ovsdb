package org.opendaylight.ovsdb.internal;
import java.io.IOException;
import java.net.Socket;

import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.controller.sal.utils.StatusCode;
import org.opendaylight.ovsdb.database.Uuid;
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
    private Socket socket;
    private JsonRpcClient rpcClient;
    private static final Logger logger = LoggerFactory.getLogger(Connection.class);

    public Connection(String identifier, Socket socket, JsonRpcClient rpcClient) {
        super();
        this.identifier = identifier;
        this.socket = socket;
        this.rpcClient = rpcClient;
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

    public Socket getSocket() {
        return socket;
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
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

    public void sendMessage(OvsdbMessage message) throws IOException{
        try{
            rpcClient.invoke(message.methodName, message.argument, socket.getOutputStream(), message.id);
        }catch(Exception e){
            logger.warn("Could not send RPC for {} ({})", message.methodName, e.getMessage());
        }
    }

    public Object readResponse(Class<?> clazz) throws Throwable{
        try{
            if(clazz.equals(String[].class)){
                String[] result = this.rpcClient.readResponse(String[].class, socket.getInputStream());
                for (String res : result) logger.info(res);
                return result;
            }
            else if(clazz.equals(Uuid[].class)){
                Uuid[] result = this.rpcClient.readResponse(Uuid[].class, socket.getInputStream());
                return result;
            }
            else{
                ObjectNode jsonObject = this.rpcClient.readResponse(ObjectNode.class, socket.getInputStream());
                ObjectMapper mapper = new ObjectMapper();
                JsonParser parser = mapper.treeAsTokens(jsonObject);
                Object result = mapper.readValue(parser, clazz);
                return result;
            }

        }catch(Exception e){
            logger.warn("Could not receive RPC response: {}", e.getMessage());
        }
        return null;
    }

    public Status disconnect() {
        try {
            socket.close();
        } catch (IOException e) {
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
