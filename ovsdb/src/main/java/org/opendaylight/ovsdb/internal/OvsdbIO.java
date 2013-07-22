package org.opendaylight.ovsdb.internal;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;

import org.opendaylight.ovsdb.internal.OvsdbIO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.googlecode.jsonrpc4j.JsonRpcClient;
import com.googlecode.jsonrpc4j.JsonRpcClient.RequestListener;


public class OvsdbIO {
    private static final Logger logger = LoggerFactory
            .getLogger(OvsdbIO.class);
    public static final short defaultOvsdbPort = 6634;

    public static Connection connect (String identifier, InetAddress address, int port) {
        try{
            Socket clientSocket = new Socket(address, port);
            clientSocket.setReuseAddress(true);
            Connection connection = new Connection(identifier, clientSocket, new JsonRpcClient());
            return connection;
        } catch(Exception e){
            logger.warn("Failed to connect to server on {} : {} ({})", address.toString(), defaultOvsdbPort, e.getMessage());
        }

        return null;
    }

    public static Connection connect (String identifier, InetAddress address) throws IOException, Throwable {
        return connect(identifier, address, defaultOvsdbPort);
    }
}
