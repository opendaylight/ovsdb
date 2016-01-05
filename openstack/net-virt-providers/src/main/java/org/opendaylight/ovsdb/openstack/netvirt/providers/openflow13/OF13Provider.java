/*
 * Copyright (c) 2013, 2015 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13;

import java.net.InetAddress;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.ovsdb.openstack.netvirt.translator.NeutronNetwork;
import org.opendaylight.ovsdb.openstack.netvirt.translator.NeutronPort;
import org.opendaylight.ovsdb.openstack.netvirt.translator.NeutronSecurityGroup;
import org.opendaylight.ovsdb.openstack.netvirt.translator.Neutron_IPs;
import org.opendaylight.ovsdb.openstack.netvirt.MdsalHelper;
import org.opendaylight.ovsdb.openstack.netvirt.NetworkHandler;
import org.opendaylight.ovsdb.openstack.netvirt.api.BridgeConfigurationManager;
import org.opendaylight.ovsdb.openstack.netvirt.api.ClassifierProvider;
import org.opendaylight.ovsdb.openstack.netvirt.api.ConfigurationService;
import org.opendaylight.ovsdb.openstack.netvirt.api.Constants;
import org.opendaylight.ovsdb.openstack.netvirt.api.EgressAclProvider;
import org.opendaylight.ovsdb.openstack.netvirt.api.IngressAclProvider;
import org.opendaylight.ovsdb.openstack.netvirt.api.L2ForwardingProvider;
import org.opendaylight.ovsdb.openstack.netvirt.api.NetworkingProvider;
import org.opendaylight.ovsdb.openstack.netvirt.api.NetworkingProviderManager;
import org.opendaylight.ovsdb.openstack.netvirt.api.NodeCacheManager;
import org.opendaylight.ovsdb.openstack.netvirt.api.SecurityServicesManager;
import org.opendaylight.ovsdb.openstack.netvirt.api.Southbound;
import org.opendaylight.ovsdb.openstack.netvirt.api.Status;
import org.opendaylight.ovsdb.openstack.netvirt.api.StatusCode;
import org.opendaylight.ovsdb.openstack.netvirt.api.TenantNetworkManager;
import org.opendaylight.ovsdb.openstack.netvirt.providers.ConfigInterface;
import org.opendaylight.ovsdb.openstack.netvirt.providers.NetvirtProvidersProvider;
import org.opendaylight.ovsdb.utils.mdsal.openflow.FlowUtils;
import org.opendaylight.ovsdb.utils.mdsal.openflow.InstructionUtils;
import org.opendaylight.ovsdb.utils.servicehelper.ServiceHelper;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.GroupActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.GroupActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.OutputActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.OutputActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.group.action._case.GroupActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.output.action._case.OutputActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.InstructionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.ApplyActionsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.ApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.apply.actions._case.ApplyActionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.BucketId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.GroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.GroupTypes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.Buckets;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.BucketsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.buckets.Bucket;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.buckets.BucketBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.buckets.BucketKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.Group;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.GroupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.GroupKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.ovsdb.rev150105.OvsdbTerminationPointAugmentation;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.CheckedFuture;


/**
 * Open vSwitch OpenFlow 1.3 Networking Provider for OpenStack Neutron
 *
 * @author Madhu Venugopal
 * @author Brent Salisbury
 * @author Dave Tucker
 * @author Sam Hague
 */
// Methods' parameters in this class follow the same pattern to avoid confusion between same-typed parameters
// The patterns need to be preserved even though not all parameters are used in all methods
@SuppressWarnings("UnusedParameters")
public class OF13Provider implements ConfigInterface, NetworkingProvider {
    private static final Logger LOG = LoggerFactory.getLogger(OF13Provider.class);
    private static final short TABLE_0_DEFAULT_INGRESS = 0;
    private static final short TABLE_1_ISOLATE_TENANT = 10;
    private static final short TABLE_2_LOCAL_FORWARD = 20;
    private static Long groupId = 1L;
    private DataBroker dataBroker = null;

    private volatile ConfigurationService configurationService;
    private volatile BridgeConfigurationManager bridgeConfigurationManager;
    private volatile TenantNetworkManager tenantNetworkManager;
    private volatile SecurityServicesManager securityServicesManager;
    private volatile ClassifierProvider classifierProvider;
    private volatile IngressAclProvider ingressAclProvider;
    private volatile EgressAclProvider egressAclProvider;
    private volatile NodeCacheManager nodeCacheManager;
    private volatile L2ForwardingProvider l2ForwardingProvider;

    public static final String NAME = "OF13Provider";
    private volatile BundleContext bundleContext;
    private volatile Southbound southbound;

    public OF13Provider() {
        this.dataBroker = NetvirtProvidersProvider.getDataBroker();
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public boolean supportsServices() {
        return true;
    }

    @Override
    public boolean hasPerTenantTunneling() {
        return false;
    }

    // The method is tested for in OF13ProviderTest
    @SuppressWarnings("unused")
    private Status getTunnelReadinessStatus (Node node, String tunnelKey) {
        InetAddress srcTunnelEndPoint = configurationService.getTunnelEndPoint(node);
        if (srcTunnelEndPoint == null) {
            LOG.error("Tunnel Endpoint not configured for Node {}", node);
            return new Status(StatusCode.NOTFOUND, "Tunnel Endpoint not configured for "+ node);
        }

        if (!bridgeConfigurationManager.isNodeNeutronReady(node)) {
            LOG.error("{} is not Overlay ready", node);
            return new Status(StatusCode.NOTACCEPTABLE, node+" is not Overlay ready");
        }

        if (!tenantNetworkManager.isTenantNetworkPresentInNode(node, tunnelKey)) {
            LOG.debug("{} has no VM corresponding to segment {}", node, tunnelKey);
            return new Status(StatusCode.NOTACCEPTABLE, node+" has no VM corresponding to segment "+ tunnelKey);
        }
        return new Status(StatusCode.SUCCESS);
    }

    private String getTunnelName(String tunnelType, InetAddress dst) {
        return tunnelType+"-"+dst.getHostAddress();
    }

    private boolean addTunnelPort (Node node, String tunnelType, InetAddress src, InetAddress dst) {
        String tunnelBridgeName = configurationService.getIntegrationBridgeName();
        String portName = getTunnelName(tunnelType, dst);
        LOG.info("addTunnelPort enter: portName: {}", portName);
        if (southbound.extractTerminationPointAugmentation(node, portName) != null
                || southbound.isTunnelTerminationPointExist(node, tunnelBridgeName, portName)) {
            LOG.info("Tunnel {} is present in {} of {}", portName, tunnelBridgeName, node.getNodeId().getValue());
            return true;
        }

        Map<String, String> options = Maps.newHashMap();
        options.put("key", "flow");
        options.put("local_ip", src.getHostAddress());
        options.put("remote_ip", dst.getHostAddress());

        if (!southbound.addTunnelTerminationPoint(node, tunnelBridgeName, portName, tunnelType, options)) {
            LOG.error("Failed to insert Tunnel port {} in {}", portName, tunnelBridgeName);
            return false;
        }

            LOG.info("addTunnelPort exit: portName: {}", portName);
        return true;
    }

    /* delete port from ovsdb port table */
    private boolean deletePort(Node node, String bridgeName, String portName) {
        // TODO SB_MIGRATION
        // might need to convert from ovsdb node to bridge node
        return southbound.deleteTerminationPoint(node, portName);
    }

    private boolean deleteTunnelPort(Node node, String tunnelType, InetAddress src, InetAddress dst) {
        String tunnelBridgeName = configurationService.getIntegrationBridgeName();
        String portName = getTunnelName(tunnelType, dst);
        return deletePort(node, tunnelBridgeName, portName);
    }

    private boolean deletePhysicalPort(Node node, String phyIntfName) {
        String intBridgeName = configurationService.getIntegrationBridgeName();
        return deletePort(node, intBridgeName, phyIntfName);
    }

    private void programLocalBridgeRules(Node node, Long dpid, String segmentationId,
                                         String attachedMac, long localPort) {
        /*
         * Table(0) Rule #3
         * ----------------
         * Match: VM sMac and Local Ingress Port
         * Action:Action: Set Tunnel ID and GOTO Local Table (5)
         */

        handleLocalInPort(dpid, TABLE_0_DEFAULT_INGRESS, TABLE_1_ISOLATE_TENANT,
                segmentationId, localPort, attachedMac, true);

        /*
         * Table(0) Rule #4
         * ----------------
         * Match: Drop any remaining Ingress Local VM Packets
         * Action: Drop w/ a low priority
         */

        handleDropSrcIface(dpid, localPort, true);

        /*
         * Table(2) Rule #1
         * ----------------
         * Match: Match TunID and Destination DL/dMAC Addr
         * Action: Output Port
         * table=2,tun_id=0x5,dl_dst=00:00:00:00:00:01 actions=output:2
         */

        handleLocalUcastOut(dpid, TABLE_2_LOCAL_FORWARD, segmentationId, localPort, attachedMac, true);

        /*
         * Table(2) Rule #2
         * ----------------
         * Match: Tunnel ID and dMAC (::::FF:FF)
         * table=2,priority=16384,tun_id=0x5,dl_dst=ff:ff:ff:ff:ff:ff \
         * actions=output:2,3,4,5
         */

        handleLocalBcastOut(dpid, TABLE_2_LOCAL_FORWARD, segmentationId, localPort, true);
        handleTunnelFloodOut(dpid, TABLE_1_ISOLATE_TENANT, TABLE_2_LOCAL_FORWARD, segmentationId, localPort, true);

        /*
         * TODO : Optimize the following 2 writes to be restricted only for the very first port known in a segment.
         */
        /*
         * Table(1) Rule #3
         * ----------------
         * Match:  Any remaining Ingress Local VM Packets
         * Action: Drop w/ a low priority
         * -------------------------------------------
         * table=1,priority=8192,tun_id=0x5 actions=goto_table:2
         */

        handleTunnelMiss(dpid, TABLE_1_ISOLATE_TENANT, TABLE_2_LOCAL_FORWARD, segmentationId, true);

        /*
         * Table(2) Rule #3
         * ----------------
         * Match: Any Remaining Flows w/a TunID
         * Action: Drop w/ a low priority
         * table=2,priority=8192,tun_id=0x5 actions=drop
         */

        handleLocalTableMiss(dpid, TABLE_2_LOCAL_FORWARD, segmentationId, true);
    }

    private void removeLocalBridgeRules(Node node, Long dpid, String segmentationId, String attachedMac, long localPort) {
        /*
         * Table(0) Rule #3
         * ----------------
         * Match: VM sMac and Local Ingress Port
         * Action:Action: Set Tunnel ID and GOTO Local Table (5)
         */

        handleLocalInPort(dpid, TABLE_0_DEFAULT_INGRESS, TABLE_1_ISOLATE_TENANT, segmentationId, localPort, attachedMac, false);

        /*
         * Table(0) Rule #4
         * ----------------
         * Match: Drop any remaining Ingress Local VM Packets
         * Action: Drop w/ a low priority
         */

        handleDropSrcIface(dpid, localPort, false);

        /*
         * Table(2) Rule #1
         * ----------------
         * Match: Match TunID and Destination DL/dMAC Addr
         * Action: Output Port
         * table=2,tun_id=0x5,dl_dst=00:00:00:00:00:01 actions=output:2
         */

        handleLocalUcastOut(dpid, TABLE_2_LOCAL_FORWARD, segmentationId, localPort, attachedMac, false);

        /*
         * Table(2) Rule #2
         * ----------------
         * Match: Tunnel ID and dMAC (::::FF:FF)
         * table=2,priority=16384,tun_id=0x5,dl_dst=ff:ff:ff:ff:ff:ff \
         * actions=output:2,3,4,5
         */

        handleLocalBcastOut(dpid, TABLE_2_LOCAL_FORWARD, segmentationId, localPort, false);
        handleTunnelFloodOut(dpid, TABLE_1_ISOLATE_TENANT, TABLE_2_LOCAL_FORWARD, segmentationId, localPort, false);
    }

    private void programLocalIngressTunnelBridgeRules(Node node, Long dpid, String segmentationId, String attachedMac, long tunnelOFPort, long localPort) {
        /*
         * Table(0) Rule #2
         * ----------------
         * Match: Ingress Port, Tunnel ID
         * Action: GOTO Local Table (20)
         */

        handleTunnelIn(dpid, TABLE_0_DEFAULT_INGRESS, TABLE_2_LOCAL_FORWARD, segmentationId, tunnelOFPort, true);

        /*
         * Table(1) Rule #2
         * ----------------
         * Match: Match Tunnel ID and L2 ::::FF:FF Flooding
         * Action: Flood to selected destination TEPs
         * -------------------------------------------
         * table=1,priority=16384,tun_id=0x5,dl_dst=ff:ff:ff:ff:ff:ff \
         * actions=output:10,output:11,goto_table:2
         */

        handleTunnelFloodOut(dpid, TABLE_1_ISOLATE_TENANT, TABLE_2_LOCAL_FORWARD, segmentationId, tunnelOFPort, true);

    }

    private void programRemoteEgressTunnelBridgeRules(Node node, Long dpid, String segmentationId, String attachedMac, long tunnelOFPort, long localPort) {
        /*
         * Table(1) Rule #1
         * ----------------
         * Match: Drop any remaining Ingress Local VM Packets
         * Action: Drop w/ a low priority
         * -------------------------------------------
         * table=1,tun_id=0x5,dl_dst=00:00:00:00:00:08 \
         * actions=output:11,goto_table:2
         */

        handleTunnelOut(dpid, TABLE_1_ISOLATE_TENANT, TABLE_2_LOCAL_FORWARD, segmentationId, tunnelOFPort, attachedMac, true);
    }

    private void removeRemoteEgressTunnelBridgeRules(Node node, Long dpid, String segmentationId, String attachedMac, long tunnelOFPort, long localPort) {
        /*
         * Table(1) Rule #1
         * ----------------
         * Match: Drop any remaining Ingress Local VM Packets
         * Action: Drop w/ a low priority
         * -------------------------------------------
         * table=1,tun_id=0x5,dl_dst=00:00:00:00:00:08 \
         * actions=output:11,goto_table:2
         */

        handleTunnelOut(dpid, TABLE_1_ISOLATE_TENANT, TABLE_2_LOCAL_FORWARD, segmentationId, tunnelOFPort, attachedMac, false);
    }

    /* Remove tunnel rules if last node in this tenant network */
    private void removePerTunnelRules(Node node, Long dpid, String segmentationId, long tunnelOFPort) {
        /*
         * TODO : Optimize the following 2 writes to be restricted only for the very first port known in a segment.
         */
        /*
         * Table(1) Rule #3
         * ----------------
         * Match:  Any remaining Ingress Local VM Packets
         * Action: Drop w/ a low priority
         * -------------------------------------------
         * table=1,priority=8192,tun_id=0x5 actions=goto_table:2
         */

        handleTunnelMiss(dpid, TABLE_1_ISOLATE_TENANT, TABLE_2_LOCAL_FORWARD, segmentationId, false);

        /*
         * Table(2) Rule #3
         * ----------------
         * Match: Any Remaining Flows w/a TunID
         * Action: Drop w/ a low priority
         * table=2,priority=8192,tun_id=0x5 actions=drop
         */

        handleLocalTableMiss(dpid, TABLE_2_LOCAL_FORWARD, segmentationId, false);

        /*
         * Table(0) Rule #2
         * ----------------
         * Match: Ingress Port, Tunnel ID
         * Action: GOTO Local Table (10)
         */

        handleTunnelIn(dpid, TABLE_0_DEFAULT_INGRESS, TABLE_2_LOCAL_FORWARD, segmentationId, tunnelOFPort, false);

        /*
         * Table(1) Rule #2
         * ----------------
         * Match: Match Tunnel ID and L2 ::::FF:FF Flooding
         * Action: Flood to selected destination TEPs
         * -------------------------------------------
         * table=1,priority=16384,tun_id=0x5,dl_dst=ff:ff:ff:ff:ff:ff \
         * actions=output:10,output:11,goto_table:2
         */

        handleTunnelFloodOut(dpid, TABLE_1_ISOLATE_TENANT, TABLE_2_LOCAL_FORWARD, segmentationId, tunnelOFPort, false);
    }

    private void programLocalVlanRules(Node node, Long dpid, String segmentationId, String attachedMac, long localPort) {
        /*
         * Table(0) Rule #1
         * ----------------
         * Tag traffic coming from the local port and vm srcmac
         * Match: VM sMac and Local Ingress Port
         * Action: Set VLAN ID and GOTO Local Table 1
         */

        handleLocalInPortSetVlan(dpid, TABLE_0_DEFAULT_INGRESS,
                TABLE_1_ISOLATE_TENANT, segmentationId, localPort,
                attachedMac, true);

        /*
         * Table(0) Rule #3
         * ----------------
         * Drop all other traffic coming from the local port
         * Match: Drop any remaining Ingress Local VM Packets
         * Action: Drop w/ a low priority
         */

        handleDropSrcIface(dpid, localPort, true);

        /*
         * Table(2) Rule #1
         * ----------------
         * Forward unicast traffic destined to the local port after stripping tag
         * Match: Match VLAN ID and Destination DL/dMAC Addr
         * Action: strip vlan, output to local port
         * Example: table=2,vlan_id=0x5,dl_dst=00:00:00:00:00:01 actions= strip vlan, output:2
         */

        handleLocalVlanUcastOut(dpid, TABLE_2_LOCAL_FORWARD, segmentationId,
                localPort, attachedMac, true);

        /*
         * Table(2) Rule #2
         * ----------------
         * Match: VLAN ID and dMAC (::::FF:FF)
         * Action: strip vlan, output to all local ports in this vlan
         * Example: table=2,priority=16384,vlan_id=0x5,dl_dst=ff:ff:ff:ff:ff:ff \
         * actions= strip_vlan, output:2,3,4,5
         */

        //handleLocalVlanBcastOut(dpid, TABLE_2_LOCAL_FORWARD, segmentationId,
        //        localPort, ethPort, true);
        //handleVlanFloodOut(dpid, TABLE_1_ISOLATE_TENANT, TABLE_2_LOCAL_FORWARD,
        //        segmentationId, localPort, ethport, true);

        /*
         * Table(2) Rule #3
         * ----------------
         * Match: Any Remaining Flows w/a VLAN ID
         * Action: Drop w/ a low priority
         * Example: table=2,priority=8192,vlan_id=0x5 actions=drop
         */

        //handleLocalVlanTableMiss(dpid, TABLE_2_LOCAL_FORWARD, segmentationId,
        //        true);
    }

    private void removeLocalVlanRules(Node node, Long dpid,
                                      String segmentationId, String attachedMac, long localPort) {
        /*
         * Table(0) Rule #1
         * ----------------
         * Match: VM sMac and Local Ingress Port
         * Action: Set VLAN ID and GOTO Local Table 1
         */

        handleLocalInPortSetVlan(dpid, TABLE_0_DEFAULT_INGRESS,
                TABLE_1_ISOLATE_TENANT, segmentationId, localPort,
                attachedMac, false);

        /*
         * Table(0) Rule #3
         * ----------------
         * Match: Drop any remaining Ingress Local VM Packets
         * Action: Drop w/ a low priority
         */

        handleDropSrcIface(dpid, localPort, false);

        /*
         * Table(2) Rule #1
         * ----------------
         * Match: Match VLAN ID and Destination DL/dMAC Addr
         * Action: strip vlan, output to local port
         * Example: table=2,vlan_id=0x5,dl_dst=00:00:00:00:00:01 actions= strip vlan, output:2
         */

        handleLocalVlanUcastOut(dpid, TABLE_2_LOCAL_FORWARD, segmentationId,
                localPort, attachedMac, false);

        /*
         * Table(2) Rule #2
         * ----------------
         * Match: VLAN ID and dMAC (::::FF:FF)
         * Action: strip vlan, output to all local ports in this vlan
         * Example: table=2,priority=16384,vlan_id=0x5,dl_dst=ff:ff:ff:ff:ff:ff \
         * actions= strip_vlan, output:2,3,4,5
         */

        //handleLocalVlanBcastOut(dpid, TABLE_2_LOCAL_FORWARD, segmentationId,
        //        localPort, ethPort, false);
        //handleVlanFloodOut(dpid, TABLE_1_ISOLATE_TENANT, TABLE_2_LOCAL_FORWARD,
        //        segmentationId, localPort, false);

    }

    private void programLocalIngressVlanRules(Node node, Long dpid, String segmentationId, String attachedMac,
                                              long localPort, long ethPort) {
        /*
         * Table(0) Rule #2
         * ----------------
         * Match: Ingress port = physical interface, Vlan ID
         * Action: GOTO Local Table 2
         */

        handleVlanIn(dpid, TABLE_0_DEFAULT_INGRESS, TABLE_2_LOCAL_FORWARD,
                segmentationId, ethPort, true);

        /*
         * Table(1) Rule #2
         * ----------------
         * Match: Match VLAN ID and L2 ::::FF:FF Flooding
         * Action: Flood to local and remote VLAN members
         * -------------------------------------------
         * Example: table=1,priority=16384,vlan_id=0x5,dl_dst=ff:ff:ff:ff:ff:ff \
         * actions=output:10 (eth port),goto_table:2
         * table=110, priority=16384,dl_vlan=2001,dl_dst=01:00:00:00:00:00/01:00:00:00:00:00 actions=output:2,pop_vlan,output:1,output:3,output:4
         */

        handleLocalVlanBcastOut(dpid, TABLE_2_LOCAL_FORWARD, segmentationId, localPort, ethPort, true);

        /*
         * Table(1) Rule #2
         * ----------------
         * Match: Match VLAN ID and L2 ::::FF:FF Flooding
         * Action: Flood to local and remote VLAN members
         * -------------------------------------------
         * Example: table=1,priority=16384,vlan_id=0x5,dl_dst=ff:ff:ff:ff:ff:ff \
         * actions=output:10 (eth port),goto_table:2
         */

        //handleVlanFloodOut(dpid, TABLE_1_ISOLATE_TENANT, TABLE_2_LOCAL_FORWARD,
        //        segmentationId, ethPort, true);
    }

    private void programRemoteEgressVlanRules(Node node, Long dpid, String segmentationId,
                                              String attachedMac, long ethPort) {
        /*
         * Table(1) Rule #1
         * ----------------
         * Match: Destination MAC is local VM MAC and vlan id
         * Action: go to table 2
         * -------------------------------------------
         * Example: table=1,vlan_id=0x5,dl_dst=00:00:00:00:00:08 \
         * actions=goto_table:2
         */

        //handleVlanOut(dpid, TABLE_1_ISOLATE_TENANT, TABLE_2_LOCAL_FORWARD,
        //        segmentationId, ethPort, attachedMac, true);

        /*
         * Table(1) Rule #3
         * ----------------
         * Match:  VLAN ID
         * Action: Go to table 2
         * -------------------------------------------
         * Example: table=1,priority=8192,vlan_id=0x5 actions=output:1,goto_table:2
         * table=110,priority=8192,dl_vlan=2001 actions=output:2
         */

        handleVlanMiss(dpid, TABLE_1_ISOLATE_TENANT, TABLE_2_LOCAL_FORWARD, segmentationId, ethPort, true);
    }

    private void removeRemoteEgressVlanRules(Node node, Long dpid, String segmentationId,
                                             String attachedMac, long localPort, long ethPort) {
        /*
         * Table(1) Rule #1
         * ----------------
         * Match: Destination MAC is local VM MAC and vlan id
         * Action: go to table 2
         * -------------------------------------------
         * Example: table=1,vlan_id=0x5,dl_dst=00:00:00:00:00:08 \
         * actions=goto_table:2
         */

        //handleVlanOut(dpid, TABLE_1_ISOLATE_TENANT, TABLE_2_LOCAL_FORWARD,
        //        segmentationId, ethPort, attachedMac, false);

        /*
         * Table(1) Rule #2
         * ----------------
         * Match: Match VLAN ID and L2 ::::FF:FF Flooding
         * Action: Flood to local and remote VLAN members
         * -------------------------------------------
         * Example: table=1,priority=16384,vlan_id=0x5,dl_dst=ff:ff:ff:ff:ff:ff \
         * actions=output:10 (eth port),goto_table:2
         * table=110, priority=16384,dl_vlan=2001,dl_dst=01:00:00:00:00:00/01:00:00:00:00:00 actions=output:2,pop_vlan,output:1,output:3,output:4
         */

        handleLocalVlanBcastOut(dpid, TABLE_2_LOCAL_FORWARD, segmentationId, localPort, ethPort, false);
    }

    private void removePerVlanRules(Node node, Long dpid, String segmentationId, long localPort, long ethPort) {
        /*
         * Table(2) Rule #3
         * ----------------
         * Match: Any Remaining Flows w/a VLAN ID
         * Action: Drop w/ a low priority
         * Example: table=2,priority=8192,vlan_id=0x5 actions=drop
         */

        //handleLocalVlanTableMiss(dpid, TABLE_2_LOCAL_FORWARD, segmentationId, false);

        /*
         * Table(0) Rule #2
         * ----------------
         * Match: Ingress port = physical interface, Vlan ID
         * Action: GOTO Local Table 2
         */

        handleVlanIn(dpid, TABLE_0_DEFAULT_INGRESS, TABLE_2_LOCAL_FORWARD, segmentationId, ethPort, false);

        /*
         * Table(1) Rule #2
         * ----------------
         * Match: Match VLAN ID and L2 ::::FF:FF Flooding
         * Action: Flood to local and remote VLAN members
         * -------------------------------------------
         * Example: table=1,priority=16384,vlan_id=0x5,dl_dst=ff:ff:ff:ff:ff:ff \
         * actions=output:10 (eth port),goto_table:2
         * table=110, priority=16384,dl_vlan=2001,dl_dst=01:00:00:00:00:00/01:00:00:00:00:00 actions=output:2,pop_vlan,output:1,output:3,output:4
         */

        //handleLocalVlanBcastOut(dpid, TABLE_2_LOCAL_FORWARD, segmentationId, localPort, ethPort, false);

        /*
         * Table(1) Rule #2
         * ----------------
         * Match: Match VLAN ID and L2 ::::FF:FF Flooding
         * Action: Flood to local and remote VLAN members
         * -------------------------------------------
         * Example: table=1,priority=16384,vlan_id=0x5,dl_dst=ff:ff:ff:ff:ff:ff \
         * actions=output:10 (eth port),goto_table:2
         */

        //handleVlanFloodOut(dpid, TABLE_1_ISOLATE_TENANT, TABLE_2_LOCAL_FORWARD,
        //        segmentationId, ethPort, false);

        /*
         * Table(1) Rule #3
         * ----------------
         * Match:  VLAN ID
         * Action: Go to table 2
         * -------------------------------------------
         * Example: table=1,priority=8192,vlan_id=0x5 actions=output:1,goto_table:2
         * table=110,priority=8192,dl_vlan=2001 actions=output:2
         */

        handleVlanMiss(dpid, TABLE_1_ISOLATE_TENANT, TABLE_2_LOCAL_FORWARD, segmentationId, ethPort, false);
    }

    private long getDpid(Node node) {
        long dpid = southbound.getDataPathId(node);
        if (dpid == 0) {
            LOG.warn("getDpid: dpid not found: {}", node);
        }
        return dpid;
    }

    private long getIntegrationBridgeOFDPID(Node node) {
        long dpid = 0L;
        if (southbound.getBridgeName(node).equals(configurationService.getIntegrationBridgeName())) {
            dpid = getDpid(node);
        }
        return dpid;
    }

    /**
     * Returns true is the network if of type GRE or VXLAN
     *
     * @param networkType The type of the network
     * @return returns true if the network is a tunnel
     */
    private boolean isTunnel(String networkType)
    {
        return (networkType.equalsIgnoreCase(NetworkHandler.NETWORK_TYPE_GRE) || networkType.equalsIgnoreCase
                (NetworkHandler.NETWORK_TYPE_VXLAN));
    }

    /**
     * Returns true if the network is of type vlan.
     *
     * @param networkType The type of the network
     * @return returns true if the network is a vlan
     */
    private boolean isVlan(String networkType)
    {
        return networkType.equalsIgnoreCase(NetworkHandler.NETWORK_TYPE_VLAN);
    }

    private void programLocalRules(String networkType, String segmentationId, Node node,
                                    OvsdbTerminationPointAugmentation intf) {
        LOG.debug("programLocalRules: node: {}, intf: {}, networkType: {}, segmentationId: {}",
                node.getNodeId(), intf.getName(), networkType, segmentationId);
        try {
            long dpid = getIntegrationBridgeOFDPID(node);
            if (dpid == 0L) {
                LOG.debug("programLocalRules: Openflow Datapath-ID not set for the integration bridge in {}",
                        node);
                return;
            }

            long localPort = southbound.getOFPort(intf);
            if (localPort == 0) {
                LOG.info("programLocalRules: could not find ofPort for Port {} on Node {}",
                         intf.getName(), node.getNodeId());
                return;
            }

            String attachedMac = southbound.getInterfaceExternalIdsValue(intf, Constants.EXTERNAL_ID_VM_MAC);
            if (attachedMac == null) {
                LOG.warn("No AttachedMac seen in {}", intf);
                return;
            }

            /* Program local rules based on network type */
            if (isVlan(networkType)) {
                LOG.debug("Program local vlan rules for interface {}", intf.getName());
                programLocalVlanRules(node, dpid, segmentationId, attachedMac, localPort);
            }
            if ((isTunnel(networkType) || isVlan(networkType))) {
                programLocalSecurityGroupRules(attachedMac, node, intf, dpid, localPort, segmentationId, true);
            }
            if (isTunnel(networkType)) {
                LOG.debug("Program local bridge rules for interface {}, "
                                + "dpid: {}, segmentationId: {}, attachedMac: {}, localPort: {}",
                        intf.getName(), dpid, segmentationId, attachedMac, localPort);
                programLocalBridgeRules(node, dpid, segmentationId, attachedMac, localPort);
            }
        } catch (Exception e) {
            LOG.error("Exception in programming Local Rules for " + intf + " on " + node, e);
        }
    }

    private void removeLocalRules(String networkType, String segmentationId, Node node,
                                   OvsdbTerminationPointAugmentation intf) {
        LOG.debug("removeLocalRules: node: {}, intf: {}, networkType: {}, segmentationId: {}",
                node.getNodeId(), intf.getName(), networkType, segmentationId);
        try {
            long dpid = getIntegrationBridgeOFDPID(node);
            if (dpid == 0L) {
                LOG.debug("removeLocalRules: Openflow Datapath-ID not set for the integration bridge in {}", node);
                return;
            }

            long localPort = southbound.getOFPort(intf);
            if (localPort == 0) {
                LOG.info("removeLocalRules: could not find ofPort");
                return;
            }

            String attachedMac = southbound.getInterfaceExternalIdsValue(intf, Constants.EXTERNAL_ID_VM_MAC);
            if (attachedMac == null) {
                LOG.warn("No AttachedMac seen in {}", intf);
                return;
            }

            /* Program local rules based on network type */
            if (isVlan(networkType)) {
                LOG.debug("Remove local vlan rules for interface {}", intf.getName());
                removeLocalVlanRules(node, dpid, segmentationId, attachedMac, localPort);
            } else if (isTunnel(networkType)) {
                LOG.debug("Remove local bridge rules for interface {}", intf.getName());
                removeLocalBridgeRules(node, dpid, segmentationId, attachedMac, localPort);
            }
            if (isTunnel(networkType) || isVlan(networkType)) {
                programLocalSecurityGroupRules(attachedMac, node, intf, dpid, localPort, segmentationId, false);
            }
        } catch (Exception e) {
            LOG.error("Exception in removing Local Rules for " + intf + " on " + node, e);
        }
    }

    // TODO SB_MIGRATION
    // Need to handle case where a node comes online after a network and tunnels have
    // already been created. The interface update is what triggers creating the l2 forwarding flows
    // so we don't see those updates in this case - we only see the new nodes interface updates.
    private void programTunnelRules (String tunnelType, String segmentationId, InetAddress dst, Node node,
                                     OvsdbTerminationPointAugmentation intf, boolean local) {
        LOG.debug("programTunnelRules: node: {}, intf: {}, local: {}, tunnelType: {}, "
                        + "segmentationId: {}, dstAddr: {}",
                node.getNodeId(), intf.getName(), local, tunnelType, segmentationId, dst.getHostAddress());
        try {
            long dpid = getIntegrationBridgeOFDPID(node);
            if (dpid == 0L) {
                LOG.debug("programTunnelRules: Openflow Datapath-ID not set for the integration bridge in {}", node);
                return;
            }

            long localPort = southbound.getOFPort(intf);
            if (localPort == 0) {
                LOG.info("programTunnelRules: could not find ofPort for Port {} on Node{}", intf.getName(), node.getNodeId());
                return;
            }

            String attachedMac = southbound.getInterfaceExternalIdsValue(intf, Constants.EXTERNAL_ID_VM_MAC);
            if (attachedMac == null) {
                LOG.warn("programTunnelRules: No AttachedMac seen in {}", intf);
                return;
            }

            OvsdbTerminationPointAugmentation tunnelPort= southbound.getTerminationPointOfBridge(node, getTunnelName(tunnelType, dst));
            if(tunnelPort != null){
                long tunnelOFPort = southbound.getOFPort(tunnelPort);
                if (tunnelOFPort == 0) {
                    LOG.error("programTunnelRules: Could not Identify Tunnel port {} -> OF ({}) on {}",
                            tunnelPort.getName(), tunnelOFPort, node);
                    return;
                }
                LOG.debug("programTunnelRules: Identified Tunnel port {} -> OF ({}) on {}",
                        tunnelPort.getName(), tunnelOFPort, node);

                if (!local) {
                    LOG.trace("programTunnelRules: program remote egress tunnel rules: node {}, intf {}",
                            node.getNodeId().getValue(), intf.getName());
                    programRemoteEgressTunnelBridgeRules(node, dpid, segmentationId, attachedMac,
                            tunnelOFPort, localPort);
                } else {
                    LOG.trace("programTunnelRules: program local ingress tunnel rules: node {}, intf {}",
                            node.getNodeId().getValue(), intf.getName());
                    programLocalIngressTunnelBridgeRules(node, dpid, segmentationId, attachedMac,
                            tunnelOFPort, localPort);
                }
            }
        } catch (Exception e) {
            LOG.trace("", e);
        }
    }

    private void removeTunnelRules (String tunnelType, String segmentationId, InetAddress dst, Node node,
                                    OvsdbTerminationPointAugmentation intf,
                                    boolean local, boolean isLastInstanceOnNode) {
        LOG.debug("removeTunnelRules: node: {}, intf: {}, local: {}, tunnelType: {}, "
                        + "segmentationId: {}, dstAddr: {}, isLastinstanceOnNode: {}",
                node.getNodeId(), intf.getName(), local, tunnelType, segmentationId, dst, isLastInstanceOnNode);
        try {
            long dpid = getIntegrationBridgeOFDPID(node);
            if (dpid == 0L) {
                LOG.debug("removeTunnelRules: Openflow Datapath-ID not set for the integration bridge in {}", node);
                return;
            }

            long localPort = southbound.getOFPort(intf);
            if (localPort == 0) {
                LOG.info("removeTunnelRules: could not find ofPort");
                return;
            }

            String attachedMac = southbound.getInterfaceExternalIdsValue(intf, Constants.EXTERNAL_ID_VM_MAC);
            if (attachedMac == null) {
                LOG.error("removeTunnelRules: No AttachedMac seen in {}", intf);
                return;
            }

            List<OvsdbTerminationPointAugmentation> intfs = southbound.getTerminationPointsOfBridge(node);
            for (OvsdbTerminationPointAugmentation tunIntf : intfs) {
                if (tunIntf.getName().equals(getTunnelName(tunnelType, dst))) {
                    long tunnelOFPort = southbound.getOFPort(tunIntf);
                    if (tunnelOFPort == 0) {
                        LOG.error("Could not Identify Tunnel port {} -> OF ({}) on {}",
                                tunIntf.getName(), tunnelOFPort, node);
                        return;
                    }
                    LOG.debug("Identified Tunnel port {} -> OF ({}) on {}",
                            tunIntf.getName(), tunnelOFPort, node);

                    if (!local) {
                        removeRemoteEgressTunnelBridgeRules(node, dpid, segmentationId, attachedMac,
                                tunnelOFPort, localPort);
                    }
                    if (local && isLastInstanceOnNode) {
                        removePerTunnelRules(node, dpid, segmentationId, tunnelOFPort);
                    }
                    return;
                }
            }
        } catch (Exception e) {
            LOG.error("", e);
        }
    }

    private void programVlanRules (NeutronNetwork network, Node node, OvsdbTerminationPointAugmentation intf) {
        LOG.debug("programVlanRules: node: {}, network: {}, intf: {}",
                node.getNodeId(), network.getNetworkUUID(), intf.getName());
        long dpid = getIntegrationBridgeOFDPID(node);
        if (dpid == 0L) {
            LOG.debug("programVlanRules: Openflow Datapath-ID not set for the integration bridge in {}", node);
            return;
        }

        long localPort = southbound.getOFPort(intf);
        if (localPort == 0) {
            LOG.debug("programVlanRules: could not find ofPort for {}", intf.getName());
            return;
        }

        String attachedMac = southbound.getInterfaceExternalIdsValue(intf, Constants.EXTERNAL_ID_VM_MAC);
        if (attachedMac == null) {
            LOG.debug("programVlanRules: No AttachedMac seen in {}", intf);
            return;
        }

        String phyIfName =
                bridgeConfigurationManager.getPhysicalInterfaceName(node, network.getProviderPhysicalNetwork());
        long ethOFPort = southbound.getOFPort(node, phyIfName);
        if (ethOFPort == 0) {
            LOG.warn("programVlanRules: could not find ofPort for physical port {}", phyIfName);
            return;
        }
        LOG.debug("programVlanRules: Identified eth port {} -> ofPort ({}) on {}",
                phyIfName, ethOFPort, node);
        // TODO: add logic to only add rule on remote nodes
        programRemoteEgressVlanRules(node, dpid, network.getProviderSegmentationID(),
                attachedMac, ethOFPort);
        programLocalIngressVlanRules(node, dpid, network.getProviderSegmentationID(),
                attachedMac, localPort, ethOFPort);
    }

    private void removeVlanRules (NeutronNetwork network, Node node, OvsdbTerminationPointAugmentation intf,
                                  boolean isLastInstanceOnNode) {
        LOG.debug("removeVlanRules: node: {}, network: {}, intf: {}, isLastInstanceOnNode",
                node.getNodeId(), network.getNetworkUUID(), intf.getName(), isLastInstanceOnNode);
        long dpid = getIntegrationBridgeOFDPID(node);
        if (dpid == 0L) {
            LOG.debug("removeVlanRules: Openflow Datapath-ID not set for the integration bridge in {}", node);
            return;
        }

        long localPort = southbound.getOFPort(intf);
        if (localPort == 0) {
            LOG.debug("removeVlanRules: programVlanRules: could not find ofPort for {}", intf.getName());
            return;
        }

        String attachedMac = southbound.getInterfaceExternalIdsValue(intf, Constants.EXTERNAL_ID_VM_MAC);
        if (attachedMac == null) {
            LOG.debug("removeVlanRules: No AttachedMac seen in {}", intf);
            return;
        }

        String phyIfName =
                bridgeConfigurationManager.getPhysicalInterfaceName(node, network.getProviderPhysicalNetwork());
        long ethOFPort = southbound.getOFPort(node, phyIfName);
        if (ethOFPort == 0) {
            LOG.warn("removeVlanRules: could not find ofPort for physical port {}", phyIfName);
            return;
        }
        LOG.debug("removeVlanRules: Identified eth port {} -> ofPort ({}) on {}",
                phyIfName, ethOFPort, node);

        removeRemoteEgressVlanRules(node, dpid, network.getProviderSegmentationID(),
                attachedMac, localPort, ethOFPort);
        if (isLastInstanceOnNode) {
            removePerVlanRules(node, dpid, network.getProviderSegmentationID(), localPort, ethOFPort);
        }
    }

    private void programLocalSecurityGroupRules(String attachedMac, Node node, OvsdbTerminationPointAugmentation intf,
                                 Long dpid,long localPort, String segmentationId,
                                 boolean write) {

        LOG.debug("programLocalRules: Program fixed security group rules for interface {}", intf.getName());
        NeutronPort dhcpPort = securityServicesManager.getDhcpServerPort(intf);
        boolean isComputePort = false;
        boolean isLastPortinBridge = false;
        boolean isLastPortinSubnet = false;
        List<Neutron_IPs> srcAddressList = null;
        if (null != dhcpPort) {
            isComputePort = securityServicesManager.isComputePort(intf);
            isLastPortinBridge = securityServicesManager.isLastPortinBridge(node, intf);
            isLastPortinSubnet = false;
            if (isComputePort) {
                isLastPortinSubnet = securityServicesManager.isLastPortinSubnet(node, intf);
                srcAddressList = securityServicesManager.getIpAddressList(intf);
                if (null == srcAddressList) {
                    LOG.warn("programLocalRules: No Ip address assigned {}", intf);
                    return;
                }
            }
            ingressAclProvider.programFixedSecurityGroup(dpid, segmentationId, dhcpPort.getMacAddress(), localPort,
                                                       isLastPortinSubnet, isComputePort, write);
            egressAclProvider.programFixedSecurityGroup(dpid, segmentationId, attachedMac, localPort,
                                                      srcAddressList, isLastPortinBridge, isComputePort,write);
            /* If the network type is tunnel based (VXLAN/GRRE/etc) with Neutron Port Security ACLs */
            /* TODO SB_MIGRATION */

            LOG.debug("Neutron port has a Port Security Group");
            // Retrieve the security group from the Neutron Port and apply the rules
            if (securityServicesManager.isPortSecurityReady(intf)) {
                //Associate the security group flows.
                List<NeutronSecurityGroup> securityGroupListInPort = securityServicesManager
                        .getSecurityGroupInPortList(intf);
                String neutronPortId = southbound.getInterfaceExternalIdsValue(intf,
                                                                               Constants.EXTERNAL_ID_INTERFACE_ID);
                for (NeutronSecurityGroup securityGroupInPort:securityGroupListInPort) {
                    ingressAclProvider.programPortSecurityGroup(dpid, segmentationId, attachedMac, localPort,
                                                              securityGroupInPort, neutronPortId, write);
                    egressAclProvider.programPortSecurityGroup(dpid, segmentationId, attachedMac, localPort,
                                                             securityGroupInPort, neutronPortId, write);
                }
            }
        } else {
            LOG.warn("programLocalRules: No DCHP port seen in  network of {}", intf);
        }
    }

    /*
     * The function is for the new compute node joining the existing network.
     * When a new VM is instantiated in the new compute node, neutron port add
     * event is generated. This event is processed only for that node. So,
     * loop through all the ports of the same network and install unicast mac
     * flow for the VM's created on the TEP of the destination node in src node.
     * This function will be executed even for any new VM creation in an existing
     * network. If a cache is maintained to optimize the below flow addition, it will
     * work only for one unstack and restack. For the next unstack and restack,
     * it will not work since the cache would have been already deleted.
     */
    private void programTunnelRulesInNewNode(NeutronNetwork network,
                                             String networkType, String segmentationId,
                                             InetAddress src, InetAddress dst,
                                             Node srcBridgeNode, Node dstBridgeNode,
                                             OvsdbTerminationPointAugmentation intf){
        try {
            long localPort = southbound.getOFPort(intf);
            if(localPort != 0)
            {
                LOG.debug("Interface update details {}", intf);

                /*
                 * When a network is added and the TEP destination is not present in a
                 * node C1, tunnelin and broadcast rules will not be programmed, since
                 * OF port is not created. So, when a new node C2 joins and create a new
                 * VM, the tunnelin and broadcast rule will not be present in C1.
                 * So, handling it in the case below to make ping work.
                 */
                if(securityServicesManager.getNeutronPortFromDhcpIntf(intf) == null){
                    programTunnelRules(networkType, segmentationId, src, dstBridgeNode, intf, true);
                }

                /*
                 * FIX for 4208 - loop through all the ports and add the VM's
                 * unicast mac rule of the destination node in the source node.
                 * When a new node is added, it needs to configure the VM unicast mac
                 * flow rules which were created before it was joined to an existing
                 * network.
                 */
                List<OvsdbTerminationPointAugmentation> ports = southbound.getTerminationPointsOfBridge(dstBridgeNode);
                for (OvsdbTerminationPointAugmentation port : ports) {
                    if(network == tenantNetworkManager.getTenantNetwork(port)){
                        programTunnelRules(networkType, segmentationId, dst, srcBridgeNode, port, false);
                    }
                    else{
                        LOG.trace("Port {} is not part of network {}", port, network);
                    }
                }
            }
        } catch (Exception e) {
            LOG.error("Exception during handlingNeutron network add", e);
        }
    }

    @Override
    public boolean handleInterfaceUpdate(NeutronNetwork network, Node srcNode,
                                         OvsdbTerminationPointAugmentation intf) {
        Preconditions.checkNotNull(nodeCacheManager);
        Map<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId,Node> nodes =
                nodeCacheManager.getOvsdbNodes();
        nodes.remove(southbound.extractBridgeOvsdbNodeId(srcNode));
        String networkType = network.getProviderNetworkType();
        String segmentationId = network.getProviderSegmentationID();
        Node srcBridgeNode = southbound.getBridgeNode(srcNode, configurationService.getIntegrationBridgeName());
        programLocalRules(networkType, network.getProviderSegmentationID(), srcBridgeNode, intf);

        if (isVlan(networkType)) {
            programVlanRules(network, srcNode, intf);
        } else if (isTunnel(networkType)){

            boolean sourceTunnelStatus = false;
            boolean destTunnelStatus = false;
            for (Node dstNode : nodes.values()) {
                InetAddress src = configurationService.getTunnelEndPoint(srcNode);
                InetAddress dst = configurationService.getTunnelEndPoint(dstNode);
                if ((src != null) && (dst != null)) {
                    sourceTunnelStatus = addTunnelPort(srcBridgeNode, networkType, src, dst);

                    Node dstBridgeNode = southbound.getBridgeNode(dstNode,
                            configurationService.getIntegrationBridgeName());

                    if(dstBridgeNode != null){
                        destTunnelStatus = addTunnelPort(dstBridgeNode, networkType, dst, src);
                    }

                    if (sourceTunnelStatus) {
                        programTunnelRules(networkType, segmentationId, dst, srcBridgeNode, intf, true);
                    }
                    if (destTunnelStatus) {
                        programTunnelRules(networkType, segmentationId, src, dstBridgeNode, intf, false);
                        programTunnelRulesInNewNode(network, networkType, segmentationId, src, dst,
                                                    srcBridgeNode, dstBridgeNode, intf);
                    }
                } else {
                    LOG.warn("Tunnel end-point configuration missing. Please configure it in OpenVSwitch Table. "
                                    + "Check source {} or destination {}",
                            src != null ? src.getHostAddress() : "null",
                            dst != null ? dst.getHostAddress() : "null");
                }
            }
        }

        return true;
    }

    private void triggerInterfaceUpdates(Node node) {
        LOG.debug("enter triggerInterfaceUpdates for {}", node.getNodeId());
        List<OvsdbTerminationPointAugmentation> ports = southbound.extractTerminationPointAugmentations(node);
        if (ports != null && !ports.isEmpty()) {
            for (OvsdbTerminationPointAugmentation port : ports) {
                NeutronNetwork neutronNetwork = tenantNetworkManager.getTenantNetwork(port);
                if (neutronNetwork != null) {
                    LOG.warn("Trigger Interface update for {}", port);
                    handleInterfaceUpdate(neutronNetwork, node, port);
                }
            }
        } else {
            LOG.warn("triggerInterfaceUpdates: tps are null");
        }
        LOG.debug("exit triggerInterfaceUpdates for {}", node.getNodeId());
    }

    @Override
    public boolean handleInterfaceDelete(String tunnelType, NeutronNetwork network, Node srcNode,
                                         OvsdbTerminationPointAugmentation intf, boolean isLastInstanceOnNode) {
        Map<org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.NodeId,Node> nodes =
                nodeCacheManager.getOvsdbNodes();
        nodes.remove(southbound.extractBridgeOvsdbNodeId(srcNode));

        LOG.info("Delete intf " + intf.getName() + " isLastInstanceOnNode " + isLastInstanceOnNode);
        List<String> phyIfName = bridgeConfigurationManager.getAllPhysicalInterfaceNames(srcNode);
        if (southbound.isTunnel(intf)) {
            // Delete tunnel port
            try {
                InetAddress src = InetAddress.getByName(
                        southbound.getOptionsValue(intf.getOptions(), "local_ip"));
                InetAddress dst = InetAddress.getByName(
                        southbound.getOptionsValue(intf.getOptions(), "remote_ip"));
                deleteTunnelPort(srcNode,
                        MdsalHelper.createOvsdbInterfaceType(intf.getInterfaceType()),
                        src, dst);
            } catch (Exception e) {
                LOG.error(e.getMessage(), e);
            }
        } else if (phyIfName.contains(intf.getName())) {
            deletePhysicalPort(srcNode, intf.getName());
        } else {
            // delete all other interfaces
            removeLocalRules(network.getProviderNetworkType(), network.getProviderSegmentationID(),
                    srcNode, intf);

            if (isVlan(network.getProviderNetworkType())) {
                removeVlanRules(network, srcNode, intf, isLastInstanceOnNode);
            } else if (isTunnel(network.getProviderNetworkType())) {

                for (Node dstNode : nodes.values()) {
                    InetAddress src = configurationService.getTunnelEndPoint(srcNode);
                    InetAddress dst = configurationService.getTunnelEndPoint(dstNode);
                    if ((src != null) && (dst != null)) {
                        LOG.info("Remove tunnel rules for interface "
                                + intf.getName() + " on srcNode " + srcNode.getNodeId().getValue());
                        removeTunnelRules(tunnelType, network.getProviderSegmentationID(),
                                dst, srcNode, intf, true, isLastInstanceOnNode);
                        Node dstBridgeNode = southbound.getBridgeNode(dstNode, Constants.INTEGRATION_BRIDGE);
                        if(dstBridgeNode != null){
                            LOG.info("Remove tunnel rules for interface "
                                    + intf.getName() + " on dstNode " + dstNode.getNodeId().getValue());
                            removeTunnelRules(tunnelType, network.getProviderSegmentationID(),
                                    src, dstBridgeNode, intf, false, isLastInstanceOnNode);
                        }
                    } else {
                        LOG.warn("Tunnel end-point configuration missing. Please configure it in "
                                        + "OpenVSwitch Table. "
                                        + "Check source {} or destination {}",
                                src != null ? src.getHostAddress() : "null",
                                dst != null ? dst.getHostAddress() : "null");
                    }
                }
            }
        }
        return true;
    }

    @Override
    public void initializeFlowRules(Node node) {
        initializeFlowRules(node, configurationService.getIntegrationBridgeName());
        initializeFlowRules(node, configurationService.getExternalBridgeName());
        triggerInterfaceUpdates(node);
    }

    private void initializeFlowRules(Node node, String bridgeName) {
        Long dpid = southbound.getDataPathId(node);
        String datapathId = southbound.getDatapathId(node);
        LOG.info("initializeFlowRules: bridgeName: {}, dpid: {} - {}",
                bridgeName, dpid, datapathId);

        if (dpid == 0L) {
            LOG.debug("Openflow Datapath-ID not set for the integration bridge in {}", node);
            return;
        }

        /*
         * Table(0) Rule #1
         * ----------------
         * Match: LLDP (0x88CCL)
         * Action: Packet_In to Controller Reserved Port
         */

        writeLLDPRule(dpid);

        if (bridgeName.equals(configurationService.getExternalBridgeName())) {
            writeNormalRule(dpid);
        }
    }

    /*
     * Create an LLDP Flow Rule to encapsulate into
     * a packet_in that is sent to the controller
     * for topology handling.
     * Match: Ethertype 0x88CCL
     * Action: Punt to Controller in a Packet_In msg
     */

    private void writeLLDPRule(Long dpidLong) {
        classifierProvider.programLLDPPuntRule(dpidLong);
    }

    /*
     * Create a NORMAL Table Miss Flow Rule
     * Match: any
     * Action: forward to NORMAL pipeline
     */

    private void writeNormalRule(Long dpidLong) {
        NodeBuilder nodeBuilder = FlowUtils.createNodeBuilder(dpidLong);
        FlowBuilder flowBuilder = new FlowBuilder();
        String flowName = "NORMAL";
        FlowUtils.initFlowBuilder(flowBuilder, flowName, (short)0).setPriority(0);
        MatchBuilder matchBuilder = new MatchBuilder();
        flowBuilder.setMatch(matchBuilder.build());

        // Create the OF Actions and Instructions
        InstructionBuilder ib = new InstructionBuilder();
        InstructionsBuilder isb = new InstructionsBuilder();

        // Instructions List Stores Individual Instructions
        List<Instruction> instructions = Lists.newArrayList();

        // Call the InstructionBuilder Methods Containing Actions
        InstructionUtils.createNormalInstructions(FlowUtils.getNodeName(dpidLong), ib);
        ib.setOrder(0);
        ib.setKey(new InstructionKey(0));
        instructions.add(ib.build());

        // Add InstructionBuilder to the Instruction(s)Builder List
        isb.setInstruction(instructions);

        // Add InstructionsBuilder to FlowBuilder
        flowBuilder.setInstructions(isb.build());
        writeFlow(flowBuilder, nodeBuilder);
    }

    /*
     * (Table:0) Ingress Tunnel Traffic
     * Match: OpenFlow InPort and Tunnel ID
     * Action: GOTO Local Table (10)
     * table=0,tun_id=0x5,in_port=10, actions=goto_table:2
     */

    private void handleTunnelIn(Long dpidLong, Short writeTable,
            Short goToTableId, String segmentationId,
            Long ofPort, boolean write) {
        classifierProvider.programTunnelIn(dpidLong, segmentationId, ofPort, write);
    }

    /*
     * (Table:0) Ingress VLAN Traffic
     * Match: OpenFlow InPort and vlan ID
     * Action: GOTO Local Table (20)
     * table=0,vlan_id=0x5,in_port=10, actions=goto_table:2
     */

    private void handleVlanIn(Long dpidLong, Short writeTable, Short goToTableId,
            String segmentationId,  Long ethPort, boolean write) {
        classifierProvider.programVlanIn(dpidLong, segmentationId, ethPort, write);
    }

    /*
     * (Table:0) Egress VM Traffic Towards TEP
     * Match: Destination Ethernet Addr and OpenFlow InPort
     * Instruction: Set TunnelID and GOTO Table Tunnel Table (n)
     * table=0,in_port=2,dl_src=00:00:00:00:00:01 \
     * actions=set_field:5->tun_id,goto_table=1"
     */

    private void handleLocalInPort(Long dpidLong, Short writeTable, Short goToTableId,
            String segmentationId, Long inPort, String attachedMac,
            boolean write) {
        classifierProvider.programLocalInPort(dpidLong, segmentationId, inPort, attachedMac, write);
    }

    /*
     * (Table:0) Egress VM Traffic Towards TEP
     * Match: Source Ethernet Addr and OpenFlow InPort
     * Instruction: Set VLANID and GOTO Table Egress (n)
     * table=0,in_port=2,dl_src=00:00:00:00:00:01 \
     * actions=push_vlan, set_field:5->vlan_id,goto_table=1"
     */

    private void handleLocalInPortSetVlan(Long dpidLong, Short writeTable,
            Short goToTableId, String segmentationId,
            Long inPort, String attachedMac,
            boolean write) {
        classifierProvider.programLocalInPortSetVlan(dpidLong, segmentationId, inPort, attachedMac, write);
    }

    /*
     * (Table:0) Drop frames source from a VM that do not
     * match the associated MAC address of the local VM.
     * Match: Low priority anything not matching the VM SMAC
     * Instruction: Drop
     * table=0,priority=16384,in_port=1 actions=drop"
     */

    private void handleDropSrcIface(Long dpidLong, Long inPort, boolean write) {
        classifierProvider.programDropSrcIface(dpidLong, inPort, write);
    }

    /*
     * (Table:1) Egress Tunnel Traffic
     * Match: Destination Ethernet Addr and Local InPort
     * Instruction: Set TunnelID and GOTO Table Tunnel Table (n)
     * table=1,tun_id=0x5,dl_dst=00:00:00:00:00:08 \
     * actions=output:10,goto_table:2"
     */
    private void handleTunnelOut(Long dpidLong, Short writeTable,
            Short goToTableId, String segmentationId,
            Long OFPortOut, String attachedMac,
            boolean write) {
        l2ForwardingProvider.programTunnelOut(dpidLong, segmentationId, OFPortOut, attachedMac, write);
    }

    /*
     * (Table:1) Egress VLAN Traffic
     * Match: Destination Ethernet Addr and VLAN id
     * Instruction: GOTO Table Table 2
     * table=1,vlan_id=0x5,dl_dst=00:00:00:00:00:08 \
     * actions= goto_table:2"
     */
    // TODO This method is referenced from commented code above (which needs to be checked)
    @SuppressWarnings("unused")
    private void handleVlanOut(Long dpidLong, Short writeTable,
            Short goToTableId, String segmentationId,
            Long ethPort, String attachedMac, boolean write) {
        l2ForwardingProvider.programVlanOut(dpidLong, segmentationId, ethPort, attachedMac, write);
    }

    /*
     * (Table:1) Egress Tunnel Traffic
     * Match: Destination Ethernet Addr and Local InPort
     * Instruction: Set TunnelID and GOTO Table Tunnel Table (n)
     * table=1,priority=16384,tun_id=0x5,dl_dst=ff:ff:ff:ff:ff:ff \
     * actions=output:10,output:11,goto_table:2
     */

    private void handleTunnelFloodOut(Long dpidLong, Short writeTable,
            Short localTable, String segmentationId,
            Long OFPortOut, boolean write) {
        l2ForwardingProvider.programTunnelFloodOut(dpidLong, segmentationId, OFPortOut, write);
    }

    /*
     * (Table:1) Egress VLAN Traffic
     * Match: Destination Ethernet Addr and VLAN id
     * Instruction: GOTO table 2 and Output port eth interface
     * Example: table=1,priority=16384,vlan_id=0x5,dl_dst=ff:ff:ff:ff:ff:ff \
     * actions=output:eth1,goto_table:2
     */
    // TODO This method is referenced from commented code above (which needs to be checked)
    @SuppressWarnings("unused")
    private void handleVlanFloodOut(Long dpidLong, Short writeTable,
            Short localTable, String segmentationId,
            Long localPort, Long ethPort, boolean write) {
        //l2ForwardingProvider.programVlanFloodOut(dpidLong, segmentationId, localPort, ethPort, write);
    }

    /*
     * (Table:1) Table Drain w/ Catch All
     * Match: Tunnel ID
     * Action: GOTO Local Table (10)
     * table=2,priority=8192,tun_id=0x5 actions=drop
     */

    private void handleTunnelMiss(Long dpidLong, Short writeTable,
            Short goToTableId, String segmentationId,
            boolean write) {
        l2ForwardingProvider.programTunnelMiss(dpidLong, segmentationId, write);
    }


    /*
     * (Table:1) Table Drain w/ Catch All
     * Match: Vlan ID
     * Action: Output port eth interface
     * table=1,priority=8192,vlan_id=0x5 actions= output port:eth1
     * table=110,priority=8192,dl_vlan=2001 actions=output:2
     */

    private void handleVlanMiss(Long dpidLong, Short writeTable,
            Short goToTableId, String segmentationId,
            Long ethPort, boolean write) {
        l2ForwardingProvider.programVlanMiss(dpidLong, segmentationId, ethPort, write);
    }

    /*
     * (Table:1) Local Broadcast Flood
     * Match: Tunnel ID and dMAC
     * Action: Output Port
     * table=2,tun_id=0x5,dl_dst=00:00:00:00:00:01 actions=output:2
     */

    private void handleLocalUcastOut(Long dpidLong, Short writeTable,
            String segmentationId, Long localPort,
            String attachedMac, boolean write) {
        l2ForwardingProvider.programLocalUcastOut(dpidLong, segmentationId, localPort, attachedMac, write);
    }

    /*
     * (Table:2) Local VLAN unicast
     * Match: VLAN ID and dMAC
     * Action: Output Port
     * table=2,vlan_id=0x5,dl_dst=00:00:00:00:00:01 actions=output:2
     */

    private void handleLocalVlanUcastOut(Long dpidLong, Short writeTable,
            String segmentationId, Long localPort,
            String attachedMac, boolean write) {
        l2ForwardingProvider.programLocalVlanUcastOut(dpidLong, segmentationId, localPort, attachedMac, write);
    }

    /*
     * (Table:2) Local Broadcast Flood
     * Match: Tunnel ID and dMAC (::::FF:FF)
     * table=2,priority=16384,tun_id=0x5,dl_dst=ff:ff:ff:ff:ff:ff \
     * actions=output:2,3,4,5
     */

    private void handleLocalBcastOut(Long dpidLong, Short writeTable,
            String segmentationId, Long localPort,
            boolean write) {
        l2ForwardingProvider.programLocalBcastOut(dpidLong, segmentationId, localPort, write);
    }

    /*
     * (Table:2) Local VLAN Broadcast Flood
     * Match: vlan ID and dMAC (::::FF:FF)
     * table=2,priority=16384,vlan_id=0x5,dl_dst=ff:ff:ff:ff:ff:ff \
     * actions=strip_vlan, output:2,3,4,5
     * table=110,dl_vlan=2001,dl_dst=01:00:00:00:00:00/01:00:00:00:00:00 actions=output:2,pop_vlan,output:1,output:3,output:4
     */

    private void handleLocalVlanBcastOut(Long dpidLong, Short writeTable, String segmentationId,
                                         Long localPort, Long ethPort, boolean write) {
        l2ForwardingProvider.programLocalVlanBcastOut(dpidLong, segmentationId, localPort, ethPort, write);
    }

    /*
     * (Table:1) Local Table Miss
     * Match: Any Remaining Flows w/a TunID
     * Action: Drop w/ a low priority
     * table=2,priority=8192,tun_id=0x5 actions=drop
     */

    private void handleLocalTableMiss(Long dpidLong, Short writeTable,
            String segmentationId, boolean write) {
        l2ForwardingProvider.programLocalTableMiss(dpidLong, segmentationId, write);
    }

    /*
     * (Table:1) Local Table Miss
     * Match: Any Remaining Flows w/a VLAN ID
     * Action: Drop w/ a low priority
     * table=2,priority=8192,vlan_id=0x5 actions=drop
     */
    // TODO This method is referenced from commented code above (which needs to be checked)
    @SuppressWarnings("unused")
    private void handleLocalVlanTableMiss(Long dpidLong, Short writeTable,
            String segmentationId, boolean write) {
        l2ForwardingProvider.programLocalVlanTableMiss(dpidLong, segmentationId, write);
    }

    private Group getGroup(GroupBuilder groupBuilder, NodeBuilder nodeBuilder) {
        InstanceIdentifier<Group> path1 = InstanceIdentifier.builder(Nodes.class).child(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory
                .rev130819.nodes.Node.class, nodeBuilder.getKey()).augmentation(FlowCapableNode.class).child(Group.class,
                        new GroupKey(groupBuilder.getGroupId())).build();
        ReadOnlyTransaction readTx = dataBroker.newReadOnlyTransaction();
        try {
            Optional<Group> data = readTx.read(LogicalDatastoreType.CONFIGURATION, path1).get();
            if (data.isPresent()) {
                return data.get();
            }
        } catch (InterruptedException|ExecutionException e) {
            LOG.error(e.getMessage(), e);
        }

        LOG.debug("Cannot find data for Group " + groupBuilder.getGroupName());
        return null;
    }

    private void writeGroup(GroupBuilder groupBuilder, NodeBuilder nodeBuilder) {
        if (NetvirtProvidersProvider.isMasterProviderInstance()) {
            ReadWriteTransaction modification = dataBroker.newReadWriteTransaction();
            InstanceIdentifier<Group> path1 = InstanceIdentifier.builder(Nodes.class).child(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory
                    .rev130819.nodes.Node.class, nodeBuilder.getKey()).augmentation(FlowCapableNode.class).child(Group.class,
                            new GroupKey(groupBuilder.getGroupId())).build();
            modification.put(LogicalDatastoreType.CONFIGURATION, path1, groupBuilder.build(), true /*createMissingParents*/);

            CheckedFuture<Void, TransactionCommitFailedException> commitFuture = modification.submit();
            try {
                commitFuture.get();  // TODO: Make it async (See bug 1362)
                LOG.debug("Transaction success for write of Group " + groupBuilder.getGroupName());
            } catch (InterruptedException|ExecutionException e) {
                LOG.error(e.getMessage(), e);
            }
        }
    }

    private void removeGroup(GroupBuilder groupBuilder, NodeBuilder nodeBuilder) {
        if (NetvirtProvidersProvider.isMasterProviderInstance()) {
            WriteTransaction modification = dataBroker.newWriteOnlyTransaction();
            InstanceIdentifier<Group> path1 = InstanceIdentifier.builder(Nodes.class).child(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory
                    .rev130819.nodes.Node.class, nodeBuilder.getKey()).augmentation(FlowCapableNode.class).child(Group.class,
                            new GroupKey(groupBuilder.getGroupId())).build();
            modification.delete(LogicalDatastoreType.CONFIGURATION, path1);
            CheckedFuture<Void, TransactionCommitFailedException> commitFuture = modification.submit();

            try {
                commitFuture.get();  // TODO: Make it async (See bug 1362)
                LOG.debug("Transaction success for deletion of Group " + groupBuilder.getGroupName());
            } catch (InterruptedException|ExecutionException e) {
                LOG.error(e.getMessage(), e);
            }
        }
    }

    private void writeFlow(FlowBuilder flowBuilder, NodeBuilder nodeBuilder) {
        if (NetvirtProvidersProvider.isMasterProviderInstance()){
            ReadWriteTransaction modification = dataBroker.newReadWriteTransaction();
            InstanceIdentifier<Flow> path1 =
                    InstanceIdentifier.builder(Nodes.class).child(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory
                                    .rev130819.nodes.Node.class,
                            nodeBuilder.getKey()).augmentation(FlowCapableNode.class).child(Table.class,
                            new TableKey(flowBuilder.getTableId())).child(Flow.class, flowBuilder.getKey()).build();

            //modification.put(LogicalDatastoreType.OPERATIONAL, path1, flowBuilder.build());
            modification.put(LogicalDatastoreType.CONFIGURATION, path1, flowBuilder.build(),
                    true);//createMissingParents


            CheckedFuture<Void, TransactionCommitFailedException> commitFuture = modification.submit();
            try {
                commitFuture.get();  // TODO: Make it async (See bug 1362)
                LOG.debug("Transaction success for write of Flow " + flowBuilder.getFlowName());
            } catch (InterruptedException|ExecutionException e) {
                LOG.error(e.getMessage(), e);
            }
        }
    }

    /**
     * Create Output Port Group Instruction
     *
     * @param ib       Map InstructionBuilder without any instructions
     * @param dpidLong Long the datapath ID of a switch/node
     * @param port     Long representing a port on a switch/node
     * @return ib InstructionBuilder Map with instructions
     */
    // TODO This method is referenced from commented code in L2ForwardingService (which needs to be checked)
    @SuppressWarnings("unused")
    protected InstructionBuilder createOutputGroupInstructions(NodeBuilder nodeBuilder,
            InstructionBuilder ib,
            Long dpidLong, Long port ,
            List<Instruction> instructions) {
        NodeConnectorId ncid = new NodeConnectorId(Constants.OPENFLOW_NODE_PREFIX + dpidLong + ":" + port);
        LOG.debug("createOutputGroupInstructions() Node Connector ID is - Type=openflow: DPID={} port={} existingInstructions={}", dpidLong, port, instructions);

        List<Action> actionList = Lists.newArrayList();
        ActionBuilder ab = new ActionBuilder();

        List<Action> existingActions;
        if (instructions != null) {
            for (Instruction in : instructions) {
                if (in.getInstruction() instanceof ApplyActionsCase) {
                    existingActions = (((ApplyActionsCase) in.getInstruction()).getApplyActions().getAction());
                    actionList.addAll(existingActions);
                }
            }
        }

        GroupBuilder groupBuilder = new GroupBuilder();
        Group group = null;

        /* Create output action for this port*/
        OutputActionBuilder oab = new OutputActionBuilder();
        oab.setOutputNodeConnector(ncid);
        ab.setAction(new OutputActionCaseBuilder().setOutputAction(oab.build()).build());
        LOG.debug("createOutputGroupInstructions(): output action {}", ab.build());
        boolean addNew = true;
        boolean groupActionAdded = false;

        /* Find the group action and get the group */
        for (Action action : actionList) {
            if (action.getAction() instanceof GroupActionCase) {
                groupActionAdded = true;
                GroupActionCase groupAction = (GroupActionCase) action.getAction();
                Long id = groupAction.getGroupAction().getGroupId();
                String groupName = groupAction.getGroupAction().getGroup();
                GroupKey key = new GroupKey(new GroupId(id));

                groupBuilder.setGroupId(new GroupId(id));
                groupBuilder.setGroupName(groupName);
                groupBuilder.setGroupType(GroupTypes.GroupAll);
                groupBuilder.setKey(key);
                group = getGroup(groupBuilder, nodeBuilder);
                LOG.debug("createOutputGroupInstructions: group {}", group);
                break;
            }
        }

        LOG.debug("createOutputGroupInstructions: groupActionAdded {}", groupActionAdded);
        if (groupActionAdded) {
            /* modify the action bucket in group */
            groupBuilder = new GroupBuilder(group);
            Buckets buckets = groupBuilder.getBuckets();
            for (Bucket bucket : buckets.getBucket()) {
                List<Action> bucketActions = bucket.getAction();
                LOG.debug("createOutputGroupInstructions: bucketActions {}", bucketActions);
                for (Action action : bucketActions) {
                    if (action.getAction() instanceof OutputActionCase) {
                        OutputActionCase opAction = (OutputActionCase)action.getAction();
                        /* If output port action already in the action list of one of the buckets, skip */
                        if (opAction.getOutputAction().getOutputNodeConnector().equals(new Uri(ncid))) {
                            addNew = false;
                            break;
                        }
                    }
                }
            }
            LOG.debug("createOutputGroupInstructions: addNew {}", addNew);
            if (addNew && !buckets.getBucket().isEmpty()) {
                /* the new output action is not in the bucket, add to bucket */
                Bucket bucket = buckets.getBucket().get(0);
                List<Action> bucketActionList = Lists.newArrayList();
                bucketActionList.addAll(bucket.getAction());
                /* set order for new action and add to action list */
                ab.setOrder(bucketActionList.size());
                ab.setKey(new ActionKey(bucketActionList.size()));
                bucketActionList.add(ab.build());

                /* set bucket and buckets list. Reset groupBuilder with new buckets.*/
                BucketsBuilder bucketsBuilder = new BucketsBuilder();
                List<Bucket> bucketList = Lists.newArrayList();
                BucketBuilder bucketBuilder = new BucketBuilder();
                bucketBuilder.setBucketId(new BucketId((long) 1));
                bucketBuilder.setKey(new BucketKey(new BucketId((long) 1)));
                bucketBuilder.setAction(bucketActionList);
                bucketList.add(bucketBuilder.build());
                bucketsBuilder.setBucket(bucketList);
                groupBuilder.setBuckets(bucketsBuilder.build());
                LOG.debug("createOutputGroupInstructions: bucketList {}", bucketList);
            }
        } else {
            /* create group */
            groupBuilder = new GroupBuilder();
            groupBuilder.setGroupType(GroupTypes.GroupAll);
            groupBuilder.setGroupId(new GroupId(groupId));
            groupBuilder.setKey(new GroupKey(new GroupId(groupId)));
            groupBuilder.setGroupName("Output port group " + groupId);
            groupBuilder.setBarrier(false);

            BucketsBuilder bucketBuilder = new BucketsBuilder();
            List<Bucket> bucketList = Lists.newArrayList();
            BucketBuilder bucket = new BucketBuilder();
            bucket.setBucketId(new BucketId((long) 1));
            bucket.setKey(new BucketKey(new BucketId((long) 1)));

            /* put output action to the bucket */
            List<Action> bucketActionList = Lists.newArrayList();
            /* set order for new action and add to action list */
            ab.setOrder(bucketActionList.size());
            ab.setKey(new ActionKey(bucketActionList.size()));
            bucketActionList.add(ab.build());

            bucket.setAction(bucketActionList);
            bucketList.add(bucket.build());
            bucketBuilder.setBucket(bucketList);
            groupBuilder.setBuckets(bucketBuilder.build());

            /* Add new group action */
            GroupActionBuilder groupActionB = new GroupActionBuilder();
            groupActionB.setGroupId(groupId);
            groupActionB.setGroup("Output port group " + groupId);
            ab = new ActionBuilder();
            ab.setAction(new GroupActionCaseBuilder().setGroupAction(groupActionB.build()).build());
            ab.setOrder(actionList.size());
            ab.setKey(new ActionKey(actionList.size()));
            actionList.add(ab.build());

            groupId++;
        }
        LOG.debug("createOutputGroupInstructions: group {}", groupBuilder.build());
        LOG.debug("createOutputGroupInstructions: actionList {}", actionList);

        if (addNew) {
            /* rewrite the group to group table */
            writeGroup(groupBuilder, nodeBuilder);
        }

        // Create an Apply Action
        ApplyActionsBuilder aab = new ApplyActionsBuilder();
        aab.setAction(actionList);
        ib.setInstruction(new ApplyActionsCaseBuilder().setApplyActions(aab.build()).build());

        return ib;
    }

    /**
     * Remove Output Port from action list in group bucket
     *
     * @param ib       Map InstructionBuilder without any instructions
     * @param dpidLong Long the datapath ID of a switch/node
     * @param port     Long representing a port on a switch/node
     * @return ib InstructionBuilder Map with instructions
     */
    // TODO This method is referenced from commented code in L2ForwardingService (which needs to be checked)
    @SuppressWarnings("unused")
    protected boolean removeOutputPortFromGroup(NodeBuilder nodeBuilder, InstructionBuilder ib,
            Long dpidLong, Long port , List<Instruction> instructions) {

        NodeConnectorId ncid = new NodeConnectorId(Constants.OPENFLOW_NODE_PREFIX + dpidLong + ":" + port);
        LOG.debug("removeOutputPortFromGroup() Node Connector ID is - Type=openflow: DPID={} port={} existingInstructions={}", dpidLong, port, instructions);

        List<Action> actionList = Lists.newArrayList();
        ActionBuilder ab;

        List<Action> existingActions;
        if (instructions != null) {
            for (Instruction in : instructions) {
                if (in.getInstruction() instanceof ApplyActionsCase) {
                    existingActions = (((ApplyActionsCase) in.getInstruction()).getApplyActions().getAction());
                    actionList.addAll(existingActions);
                    break;
                }
            }
        }

        GroupBuilder groupBuilder = new GroupBuilder();
        Group group = null;
        boolean groupActionAdded = false;
        /* Find the group action and get the group */
        for (Action action : actionList) {
            if (action.getAction() instanceof GroupActionCase) {
                groupActionAdded = true;
                GroupActionCase groupAction = (GroupActionCase) action.getAction();
                Long id = groupAction.getGroupAction().getGroupId();
                String groupName = groupAction.getGroupAction().getGroup();
                GroupKey key = new GroupKey(new GroupId(id));

                groupBuilder.setGroupId(new GroupId(id));
                groupBuilder.setGroupName(groupName);
                groupBuilder.setGroupType(GroupTypes.GroupAll);
                groupBuilder.setKey(key);
                group = getGroup(groupBuilder, nodeBuilder);
                break;
            }
        }

        if (groupActionAdded) {
            /* modify the action bucket in group */
            groupBuilder = new GroupBuilder(group);
            Buckets buckets = groupBuilder.getBuckets();
            List<Action> bucketActions = Lists.newArrayList();
            for (Bucket bucket : buckets.getBucket()) {
                int index = 0;
                boolean isPortDeleted = false;
                bucketActions = bucket.getAction();
                for (Action action : bucketActions) {
                    if (action.getAction() instanceof OutputActionCase) {
                        OutputActionCase opAction = (OutputActionCase)action.getAction();
                        if (opAction.getOutputAction().getOutputNodeConnector().equals(new Uri(ncid))) {
                            /* Find the output port in action list and remove */
                            index = bucketActions.indexOf(action);
                            bucketActions.remove(action);
                            isPortDeleted = true;
                            break;
                        }
                    }
                }
                if (isPortDeleted && !bucketActions.isEmpty()) {
                    for (int i = index; i< bucketActions.size(); i++) {
                        Action action = bucketActions.get(i);
                        if (action.getOrder() != i) {
                            /* Shift the action order */
                            ab = new ActionBuilder();
                            ab.setAction(action.getAction());
                            ab.setOrder(i);
                            ab.setKey(new ActionKey(i));
                            Action actionNewOrder = ab.build();
                            bucketActions.remove(action);
                            bucketActions.add(i, actionNewOrder);
                        }
                    }

                } else if (bucketActions.isEmpty()) {
                    /* remove bucket with empty action list */
                    buckets.getBucket().remove(bucket);
                    break;
                }
            }
            if (!buckets.getBucket().isEmpty()) {
                /* rewrite the group to group table */
                /* set bucket and buckets list. Reset groupBuilder with new buckets.*/
                BucketsBuilder bucketsBuilder = new BucketsBuilder();
                List<Bucket> bucketList = Lists.newArrayList();
                BucketBuilder bucketBuilder = new BucketBuilder();
                bucketBuilder.setBucketId(new BucketId((long) 1));
                bucketBuilder.setKey(new BucketKey(new BucketId((long) 1)));
                bucketBuilder.setAction(bucketActions);
                bucketList.add(bucketBuilder.build());
                bucketsBuilder.setBucket(bucketList);
                groupBuilder.setBuckets(bucketsBuilder.build());
                LOG.debug("removeOutputPortFromGroup: bucketList {}", bucketList);

                writeGroup(groupBuilder, nodeBuilder);
                ApplyActionsBuilder aab = new ApplyActionsBuilder();
                aab.setAction(actionList);
                ib.setInstruction(new ApplyActionsCaseBuilder().setApplyActions(aab.build()).build());
                return false;
            } else {
                /* remove group with empty bucket. return true to delete flow */
                removeGroup(groupBuilder, nodeBuilder);
                return true;
            }
        } else {
            /* no group for port list. flow can be removed */
            return true;
        }
    }

    @Override
    public void initializeOFFlowRules(Node openflowNode) {
        String bridgeName = southbound.getBridgeName(openflowNode);
        LOG.info("initializeOFFlowRules: bridgeName: {}", bridgeName);
        if (bridgeName.equals(configurationService.getIntegrationBridgeName())) {
            initializeFlowRules(openflowNode, configurationService.getIntegrationBridgeName());
            triggerInterfaceUpdates(openflowNode);
        } else if (bridgeName.equals(configurationService.getExternalBridgeName())) {
            initializeFlowRules(openflowNode, configurationService.getExternalBridgeName());
            LOG.info("initializeOFFlowRules after writeFlow: bridgeName: {}", bridgeName);
            triggerInterfaceUpdates(openflowNode);
            LOG.info("initializeOFFlowRules after triggerUpdates: bridgeName: {}", bridgeName);
        }
    }

    public static NodeBuilder createNodeBuilder(String nodeId) {
        NodeBuilder builder = new NodeBuilder();
        builder.setId(new NodeId(nodeId));
        builder.setKey(new NodeKey(builder.getId()));
        return builder;
    }

    @Override
    public void setDependencies(BundleContext bundleContext, ServiceReference serviceReference) {
        this.bundleContext = bundleContext;
        configurationService =
                (ConfigurationService) ServiceHelper.getGlobalInstance(ConfigurationService.class, this);
        tenantNetworkManager =
                (TenantNetworkManager) ServiceHelper.getGlobalInstance(TenantNetworkManager.class, this);
        bridgeConfigurationManager =
                (BridgeConfigurationManager) ServiceHelper.getGlobalInstance(BridgeConfigurationManager.class, this);
        nodeCacheManager =
                (NodeCacheManager) ServiceHelper.getGlobalInstance(NodeCacheManager.class, this);
        classifierProvider =
                (ClassifierProvider) ServiceHelper.getGlobalInstance(ClassifierProvider.class, this);
        ingressAclProvider =
                (IngressAclProvider) ServiceHelper.getGlobalInstance(IngressAclProvider.class, this);
        egressAclProvider =
                (EgressAclProvider) ServiceHelper.getGlobalInstance(EgressAclProvider.class, this);
        l2ForwardingProvider =
                (L2ForwardingProvider) ServiceHelper.getGlobalInstance(L2ForwardingProvider.class, this);
        securityServicesManager =
                (SecurityServicesManager) ServiceHelper.getGlobalInstance(SecurityServicesManager.class, this);
        southbound =
                (Southbound) ServiceHelper.getGlobalInstance(Southbound.class, this);
    }

    @Override
    public void setDependencies(Object impl) {
        if (impl instanceof NetworkingProviderManager) {
            NetworkingProviderManager networkingProviderManager = (NetworkingProviderManager) impl;
            networkingProviderManager.providerAdded(
                    bundleContext.getServiceReference(NetworkingProvider.class.getName()), this);
        }
    }
}
