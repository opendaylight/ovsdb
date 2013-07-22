package org.opendaylight.ovsdb.internal;

import java.net.InetAddress;
import java.util.*;

import org.opendaylight.ovsdb.sal.configuration.IPluginInNetworkConfigurationService;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.ovsdb.database.Uuid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigurationService implements IPluginInNetworkConfigurationService
{
    private static final Logger logger = LoggerFactory
            .getLogger(ConfigurationService.class);

    IConnectionServiceInternal connectionService;

    void init() {
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

    public void setConnectionServiceInternal(IConnectionServiceInternal connectionService) {
        this.connectionService = connectionService;
    }

    public void unsetConnectionServiceInternal(IConnectionServiceInternal connectionService) {
        if (this.connectionService == connectionService) {
            this.connectionService = null;
        }
    }

    @Override
    public boolean createBridgeDomain(Node node, String bridgeIdentifier){
        if (connectionService == null) {
            logger.error("Couldnt refer to the ConnectionService");
            return false;
        }
        Connection connection = connectionService.getConnection(node);
        String identifier = "TEST";

        if (connection != null) {
            String newBridge = "new_bridge";
            String newInterface = "new_interface";
            String newPort = "new_port";
            String newSwitch = "new_switch";

            Map<String, Object> bridgeRow = new HashMap<String, Object>();
            bridgeRow.put("name", "br1");
            ArrayList<String> ports = new ArrayList<String>();
            ports.add("named-uuid");
            ports.add(newPort);
            bridgeRow.put("ports", ports);
            InsertRequest addBridgeRequest = new InsertRequest("insert", "Bridge", newBridge, bridgeRow);

            Map<String, Object> portRow = new HashMap<String, Object>();
            portRow.put("name", "br1");
            ArrayList<String> interfaces = new ArrayList<String>();
            interfaces.add("named-uuid");
            interfaces.add(newInterface);
            portRow.put("interfaces", interfaces);
            InsertRequest addPortRequest = new InsertRequest("insert", "Port", newPort, portRow);

            Map<String, Object> interfaceRow = new HashMap<String, Object>();
            interfaceRow.put("name", "br1");
            interfaceRow.put("type", "internal");
            InsertRequest addIntfRequest = new InsertRequest("insert", "Interface", newInterface, interfaceRow);

            Map<String, Object> vswitchRow = new HashMap<String, Object>();
            ArrayList<String> bridges = new ArrayList<String>();
            bridges.add("named-uuid");
            bridges.add(newBridge);
            vswitchRow.put("bridges", bridges);
            InsertRequest addSwitchRequest = new InsertRequest("insert", "Open_vSwitch", newSwitch, vswitchRow);

            Object[] params = {"Open_vSwitch", addSwitchRequest, addIntfRequest, addPortRequest, addBridgeRequest};
            OvsdbMessage msg = new OvsdbMessage("transact", params);
            try {
                connection.sendMessage(msg);
                connection.readResponse(String[].class);
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                return false;
            } catch (Throwable e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean deleteBridgeDomain(Node node, String bridgeIdentifier) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public List<String> getBridgeDomains(Node node) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean addBridgeDomainConfig(Node node, String bridgeIdentifier, Map<String, String> config) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean removeBridgeDomainConfig(Node node, String bridgeIdentifier, Map<String, String> config) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Map<String, String> getBridgeDomainConfigs(Node node, String bridgeIdentifier) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean createBridgeConnector(Node node, String bridgeConnectorIdentifier) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean deleteBridgeConnector(Node node, String bridgeConnectorIdentifier) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean associateBridgeConnector(Node node, String bridgeIdentifier, String bridgeConnectorIdentifier) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean disassociateBridgeConnector(Node node, String bridgeIdentifier, String bridgeConnectorIdentifier) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean addBridgeConnectorConfig(Node node, String bridgeConnectorIdentifier, Map<String, String> config) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean removeBridgeConnectorConfig(Node node, String bridgeConnectorIdentifier, Map<String, String> config) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Map<String, String> getBridgeConnectorConfigs(Node node, String bridgeConnectorIdentifier) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Object genericConfigurationEvent(Node node, Map<String, String> config) {
        // TODO Auto-generated method stub
        return null;
    }

  }