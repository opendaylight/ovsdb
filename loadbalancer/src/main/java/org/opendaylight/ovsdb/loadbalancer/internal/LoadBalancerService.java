/*
 * Copyright IBM Corporation, 2013.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.loadbalancer.internal;

import java.util.Set;

import org.opendaylight.controller.hosttracker.IfIptoHost;
import org.opendaylight.controller.sal.packet.IDataPacketService;
import org.opendaylight.controller.sal.utils.GlobalConstants;
import org.opendaylight.ovsdb.loadbalancer.ConfigManager;
import org.opendaylight.ovsdb.loadbalancer.IConfigManager;
import org.opendaylight.ovsdb.loadbalancer.entities.Pool;
import org.opendaylight.ovsdb.loadbalancer.entities.PoolMember;
import org.opendaylight.ovsdb.loadbalancer.entities.VIP;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketReceived;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is the main class that represents the load balancer service. This
 * is a reactive load balancer application that balances traffic to backend
 * servers using the multipath action in OVS.
 * The service proactivly installs OpenFlow rules to hash
 * specific source address and source port to divide traffic into
 * multiple puckets. Packets are rewritten by OVS based on the hash value with the 
 * appropriate L2 and L3 dst addresses. The servers can only use a random
 * policy with this mechanism. This service can be configured via a REST APIs which are similar to
 * the OpenStack Quantum LBaaS (Load-balancer-as-a-Service) v1.0 API proposal
 * (http://wiki.openstack.org/Quantum/LBaaS)
 *
 * To use this service, a virtual IP (or VIP) should be exposed to the clients
 * of this service and used as the destination address. A VIP is a entity that
 * comprises of a virtual IP, port and protocol (TCP or UDP). Assumptions: 1.
 * One or more VIPs may be mapped to the same server pool.
 *
 * 2. Only one server pool can be be assigned to a VIP.
 *
 *
 */
public class LoadBalancerService implements  IConfigManager, LoadbalancerHandler {

    /*
     * Logger instance
     */
    private static Logger lbsLogger = LoggerFactory.getLogger(LoadBalancerService.class);

    /*
     * Single instance of the configuration manager. Application passes this
     * reference to all the new policies implemented for load balancing.
     */
    private static ConfigManager configManager = new ConfigManager();

    
    /*
     * Reference to the data packet service
     */
    private IDataPacketService dataPacketService = null;

    /*
     * Reference to the host tracker service
     */
    private IfIptoHost hostTracker;


    /*
     * Load balancer application installs all flows with priority 2.
     */
//    private static short LB_IPSWITCH_PRIORITY = 2;

    /*
     * Name of the container where this application will register.
     */
    private String containerName = null;

    /*
     * Set/unset methods for the service instance that load balancer service
     * requires
     */
    public String getContainerName() {
        if (containerName == null)
            return GlobalConstants.DEFAULT.toString();
        return containerName;
    }

    void setDataPacketService(IDataPacketService s) {
        this.dataPacketService = s;
    }

    void unsetDataPacketService(IDataPacketService s) {
        if (this.dataPacketService == s) {
            this.dataPacketService = null;
        }
    }

    public void setHostTracker(IfIptoHost hostTracker) {
        lbsLogger.debug("Setting HostTracker");
        this.hostTracker = hostTracker;
    }

    public void unsetHostTracker(IfIptoHost hostTracker) {
        if (this.hostTracker == hostTracker) {
            this.hostTracker = null;
        }
    }



    /**
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
    public synchronized void onSwitchAppeared(InstanceIdentifier<Table> appearedTablePath) {
        lbsLogger.debug("New Switch appeared ..");
       
        /**
         * appearedTablePath is in form of /nodes/node/node-id/table/table-id
         * so we shorten it to /nodes/node/node-id to get identifier of switch.
         * 
         */
        InstanceIdentifier<Node> nodePath = appearedTablePath.firstIdentifierOf(Node.class);;
        
        /**
         * We check if we already initialized dispatcher for that node,
         * if not we create new handler for switch.
         * 
         */
       
    }
    
    /*
     * All the methods below are just proxy methods to direct the REST API
     * requests to configuration manager. We need this redirection as currently,
     * opendaylight supports only one implementation of the service.
     */
    @Override
    public Set<VIP> getAllVIPs() {
        return configManager.getAllVIPs();
    }

    @Override
    public boolean vipExists(String name, String ip, String protocol, short protocolPort, String poolName) {
        return configManager.vipExists(name, ip, protocol, protocolPort, poolName);
    }

    @Override
    public boolean vipExists(VIP vip) {
        return configManager.vipExists(vip);
    }

    @Override
    public VIP createVIP(String name, String ip, String protocol, short protocolPort, String poolName) {
        return configManager.createVIP(name, ip, protocol, protocolPort, poolName);
    }

    @Override
    public VIP updateVIP(String name, String poolName) {
        return configManager.updateVIP(name, poolName);
    }

    @Override
    public VIP deleteVIP(String name) {
        return configManager.deleteVIP(name);
    }

    @Override
    public boolean memberExists(String name, String memberIP, String poolName) {
        return configManager.memberExists(name, memberIP, poolName);
    }

    @Override
    public Set<PoolMember> getAllPoolMembers(String poolName) {

        return configManager.getAllPoolMembers(poolName);
    }

    @Override
    public PoolMember addPoolMember(String name, String memberIP, String poolName) {
        return configManager.addPoolMember(name, memberIP, poolName);
    }

    @Override
    public PoolMember removePoolMember(String name, String poolName) {

        return configManager.removePoolMember(name, poolName);
    }

    @Override
    public Set<Pool> getAllPools() {

        return configManager.getAllPools();
    }

    @Override
    public Pool getPool(String poolName) {
        return configManager.getPool(poolName);
    }

    @Override
    public boolean poolExists(String name, String lbMethod) {
        return configManager.poolExists(name, lbMethod);
    }

    @Override
    public Pool createPool(String name, String lbMethod) {
        return configManager.createPool(name, lbMethod);
    }

    @Override
    public Pool deletePool(String poolName) {
        return configManager.deletePool(poolName);
    }

    @Override
    public boolean vipExists(String name) {
        return configManager.vipExists(name);
    }

    @Override
    public boolean memberExists(String name, String poolName) {
        return configManager.memberExists(name, poolName);
    }

    @Override
    public boolean poolExists(String name) {
        return configManager.poolExists(name);
    }

    @Override
    public String getVIPAttachedPool(String name) {
        return configManager.getVIPAttachedPool(name);
    }



 
}
