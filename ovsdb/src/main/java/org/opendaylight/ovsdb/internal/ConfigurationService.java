package org.opendaylight.ovsdb.internal;


import java.util.*;

import io.netty.channel.Channel;

import org.eclipse.osgi.framework.console.CommandProvider;

import org.opendaylight.ovsdb.database.OVSInstance;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.networkconfig.bridgedomain.ConfigConstants;
import org.opendaylight.controller.sal.networkconfig.bridgedomain.IPluginInBridgeDomainConfigService;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.controller.sal.utils.StatusCode;
import org.opendaylight.ovsdb.table.EchoRequestPojo;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigurationService implements IPluginInBridgeDomainConfigService, CommandProvider
{
    private static final Logger logger = LoggerFactory
            .getLogger(ConfigurationService.class);

    IConnectionServiceInternal connectionService;
    boolean forceConnect = false;

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
        registerWithOSGIConsole();
        registerRequestMappings();
    }

    private void registerWithOSGIConsole() {
        BundleContext bundleContext = FrameworkUtil.getBundle(this.getClass())
                .getBundleContext();
        bundleContext.registerService(CommandProvider.class.getName(), this,
                null);
    }
    private void registerRequestMappings() {

        MessageMapper.getMapper().map("echo", EchoRequestPojo.class);

//MessageIDMapper.getMapper().map("update", UpdatePojo.class);
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

    private Connection getConnection (Node node) {

        Connection connection = connectionService.getConnection(node);
        return connection;
    }
    /**
     * Add a new bridge
     * @param node Node serving this configuration service
     * @param bridgeIdentifier String representation of a Bridge Connector
     * @return Bridge Connector configurations
     */
    @Override
    public Status createBridgeDomain(Node node, String bridgeIdentifier,
            Map<ConfigConstants, Object> configs) throws Throwable {
        try{
            if (connectionService == null) {
                logger.error("Couldn't refer to the ConnectionService");
                return new Status(StatusCode.NOSERVICE);
            }

            Connection connection = this.getConnection(node);

            Channel channel = connection.getChannel();
            OVSInstance instance = OVSInstance.monitorOVS(connection);
            if(instance != null){
                String request = ("{\"method\":\"transact\",\"params\":[\"Open_vSwitch\",{\"op\":\"insert\",\"uuid-name\":\"new_interface\",\"table\":\"Interface\",\"row\":{\"name\":\""+bridgeIdentifier+"\",\"type\":\"internal\"}}," +
                        "{\"op\":\"insert\",\"uuid-name\":\"new_port\",\"table\":\"Port\",\"row\":{\"name\":\"" + bridgeIdentifier + "\",\"interfaces\":[\"named-uuid\",\"new_interface\"]}}," +
                        "{\"op\":\"insert\",\"uuid-name\":\"new_bridge\",\"table\":\"Bridge\",\"row\":{\"name\":\"" + bridgeIdentifier + "\",\"ports\":[\"named-uuid\",\"new_port\"]}}," +
                        "{\"op\":\"mutate\",\"table\":\"Open_vSwitch\",\"where\":[[\"_uuid\",\"==\",[\"uuid\",\"" + instance.getUuid() +"\"]]],\"mutations\":[[\"bridges\",\"insert\",[\"named-uuid\",\"new_bridge\"]]]}]," +
                        "\"id\":" + connection.getIdCounter() + "}");
                MessageMapper.getMapper().map(connection.getIdCounter(), ArrayList.class);
                connection.sendMessage(request);

            }
            else{
                String request = ("{\"method\":\"transact\",\"params\":[\"Open_vSwitch\",{\"row\":{\"bridges\":[\"named-uuid\",\"new_bridge\"]}," +
                        "\"table\":\"Open_vSwitch\",\"uuid-name\":\"new_switch\",\"op\":\"insert\"},{\"row\":{\"name\":\""+bridgeIdentifier+"\",\"type\":\"internal\"}," +
                        "\"table\":\"Interface\",\"uuid-name\":\"new_interface\",\"op\":\"insert\"},{\"row\":{\"name\":\""+bridgeIdentifier+"\",\"interfaces\":[\"named-uuid\",\"new_interface\"]}," +
                        "\"table\":\"Port\",\"uuid-name\":\"new_port\",\"op\":\"insert\"},{\"row\":{\"name\":\""+bridgeIdentifier+"\",\"ports\":[\"named-uuid\",\"new_port\"]}," +
                        "\"table\":\"Bridge\",\"uuid-name\":\"new_bridge\",\"op\":\"insert\"}],\"id\":\"" + connection.getIdCounter() + "\"}\")");
                MessageMapper.getMapper().map(connection.getIdCounter(), ArrayList.class);
                connection.sendMessage(request);
            }
            channel.closeFuture().sync();
        }catch(Exception e){
            e.printStackTrace();
        }
        return new Status(StatusCode.SUCCESS);
    }

    /**
     * Create a Port Attached to a Bridge
     * Ex. ovs-vsctl add-port br0 vif0
     * @param node Node serving this configuration service
     * @param bridgeIdentifier String representation of a Bridge Domain
     * @param portIdentifier String representation of a user defined Port Name
     */
    @Override
    public Status addPort(Node node, String bridgeIdentifier, String portIdentifier, Map<ConfigConstants, Object> configs) {
        return null;
    }
    /**
     * Implements the OVS Connection for Managers
     *
     * @param node Node serving this configuration service
     * @param managerip with IP and connection types
     */
    @SuppressWarnings("unchecked")
    public boolean setManager(Node node, String managerip) throws Throwable{
       return false;
    }

    @Override
    public Status addBridgeDomainConfig(Node node, String bridgeIdentfier,
            Map<ConfigConstants, Object> configs) {
            return null;
    }

    @Override
    public Status addPortConfig(Node node, String bridgeIdentifier, String portIdentifier,
            Map<ConfigConstants, Object> configs) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Status deletePort(Node node, String bridgeIdentifier, String portIdentifier) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Node getBridgeDomainNode(Node node, String bridgeIdentifier) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<ConfigConstants, Object> getPortConfigs(Node node, String bridgeIdentifier,
            String portIdentifier) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Status removeBridgeDomainConfig(Node node, String bridgeIdentifier,
            Map<ConfigConstants, Object> configs) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Status removePortConfig(Node node, String bridgeIdentifier, String portIdentifier,
            Map<ConfigConstants, Object> configs) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Status deleteBridgeDomain(Node node, String bridgeIdentifier) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Map<ConfigConstants, Object> getBridgeDomainConfigs(Node node, String bridgeIdentifier) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<String> getBridgeDomains(Node node) {
        return null;
    }

    @Override
    public NodeConnector getNodeConnector(Node arg0, String arg1, String arg2) {
        return null;
    }

    @Override
    public String getHelp() {
        StringBuffer help = new StringBuffer();
        help.append("---OVSDB CLI---\n");
        help.append("\t ovsconnect <ConnectionName> <ip-address>                        - Connect to OVSDB\n");
        help.append("\t addBridge <Node> <BridgeName>                                   - Add Bridge\n");
        help.append("\t addPort <Node> <BridgeName> <PortName>                          - Add Port\n");
        help.append("\t addPortVlan <Node> <BridgeName> <PortName> <vlan>               - Add Port, Vlan\n");
        help.append("\t addTunnel <Node> <Bridge> <Port> <tunnel-type> <remote-ip>      - Add Tunnel\n");
        help.append("\t forceConnect <yes|no>   - Force a new OVSDB Connection for every command (Workaround)");
        return help.toString();
    }
}
