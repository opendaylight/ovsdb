package org.opendaylight.ovsdb.internal;

import java.net.InetAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.opendaylight.ovsdb.sal.connection.ConnectionConstants;
import org.opendaylight.ovsdb.sal.connection.IPluginInConnectionService;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.controller.sal.utils.StatusCode;

/**
 * Represents the openflow plugin component in charge of programming the flows
 * the flow programming and relay them to functional modules above SAL.
 */
public class ConnectionService implements IPluginInConnectionService, IConnectionServiceInternal
{
    ConcurrentMap <String, Connection> ovsdbConnections;
    public void init() {
        ovsdbConnections = new ConcurrentHashMap<String, Connection>();
    }

    /**
     * Function called by the dependency manager when at least one dependency
     * become unsatisfied or when the component is shutting down because for
     * example bundle is being stopped.
     *
     */
    void destroy() {
    }

    /**
     * Function called by dependency manager after "init ()" is called and after
     * the services provided by the class are registered in the service registry
     *
     */
    void start() {
    }

    /**
     * Function called by the dependency manager before the services exported by
     * the component are unregistered, this will be followed by a "destroy ()"
     * calls
     *
     */
    void stop() {
    }

    @Override
    public Status disconnect(Node node) {
        String identifier = (String)node.getID();
        Connection connection = ovsdbConnections.get(identifier);
        if (connection != null) {
            ovsdbConnections.remove(identifier);
            return connection.disconnect();
        } else {
            return new Status(StatusCode.NOTFOUND);
        }
    }

    @Override
    public Node connect(String identifier, Map<ConnectionConstants, String> params) {
        InetAddress address;

        try {
            address = InetAddress.getByName(params.get(ConnectionConstants.ADDRESS));
        } catch (Exception e) {
            address = null;
        }

        if (address == null) {
            return null;
        }

        int port = OvsdbIO.defaultOvsdbPort;

        try {
            port = Integer.parseInt(params.get(ConnectionConstants.PORT));
        } catch (Exception e) {
            port = OvsdbIO.defaultOvsdbPort;
        }
        try {
            Connection connection = OvsdbIO.connect(identifier, address, port);
            if (connection != null) {
                ovsdbConnections.put(identifier, connection);
                return connection.getNode();
            }
        } catch (Exception e) {
        }
        return null;
    }

    @Override
    public Connection getConnection(Node node) {
        String identifier = (String)node.getID();
        return ovsdbConnections.get(identifier);
    }
  }