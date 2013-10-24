package org.opendaylight.ovsdb.plugin;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.felix.dm.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.opendaylight.controller.sal.core.Actions;
import org.opendaylight.controller.sal.core.Bandwidth;
import org.opendaylight.controller.sal.core.Buffers;
import org.opendaylight.controller.sal.core.Capabilities;
import org.opendaylight.controller.sal.core.Capabilities.CapabilitiesType;
import org.opendaylight.controller.sal.core.ConstructionException;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.core.Property;
import org.opendaylight.controller.sal.core.State;
import org.opendaylight.controller.sal.core.Tables;
import org.opendaylight.controller.sal.core.TimeStamp;
import org.opendaylight.controller.sal.inventory.IPluginInInventoryService;
import org.opendaylight.controller.sal.utils.NodeCreator;
import org.opendaylight.controller.sal.utils.NodeConnectorCreator;

/**
 * Stub Implementation for IPluginInReadService used by SAL
 *
 *
 */
public class InventoryService implements IPluginInInventoryService, InventoryServiceInternal {
    private static final Logger logger = LoggerFactory
            .getLogger(InventoryService.class);

    private ConcurrentMap<Node, Map<String, Property>> nodeProps;
    private ConcurrentMap<NodeConnector, Map<String, Property>> nodeConnectorProps;

    /**
     * Function called by the dependency manager when all the required
     * dependencies are satisfied
     *
     */
    void init() {
        nodeProps = new ConcurrentHashMap<Node, Map<String, Property>>();
        nodeConnectorProps = new ConcurrentHashMap<NodeConnector, Map<String, Property>>();
        Node.NodeIDType.registerIDType("OVS", String.class);
        NodeConnector.NodeConnectorIDType.registerIDType("OVS", String.class, "OVS");
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

    /**
     * Retrieve nodes from openflow
     */
    @Override
    public ConcurrentMap<Node, Map<String, Property>> getNodeProps() {
        return nodeProps;
    }

    /**
     * Retrieve nodeConnectors from openflow
     */
    @Override
    public ConcurrentMap<NodeConnector, Map<String, Property>> getNodeConnectorProps(
            Boolean refresh) {
        return nodeConnectorProps;
    }

}
