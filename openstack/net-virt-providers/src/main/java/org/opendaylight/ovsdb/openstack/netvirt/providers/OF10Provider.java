/*
 * Copyright (C) 2013 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Madhu Venugopal, Brent Salisbury, Sam Hague, Dave Tucker
 */
package org.opendaylight.ovsdb.openstack.netvirt.providers;

import org.opendaylight.controller.forwardingrulesmanager.FlowConfig;
import org.opendaylight.controller.forwardingrulesmanager.IForwardingRulesManager;
import org.opendaylight.controller.networkconfig.neutron.NeutronNetwork;
import org.opendaylight.controller.sal.action.ActionType;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.utils.EtherTypes;
import org.opendaylight.controller.sal.utils.HexEncode;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.controller.sal.utils.StatusCode;
import org.opendaylight.controller.switchmanager.ISwitchManager;
import org.opendaylight.controller.switchmanager.Switch;
import org.opendaylight.ovsdb.lib.notation.Row;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.openstack.netvirt.api.Constants;
import org.opendaylight.ovsdb.openstack.netvirt.NetworkHandler;
import org.opendaylight.ovsdb.openstack.netvirt.api.BridgeConfigurationManager;
import org.opendaylight.ovsdb.openstack.netvirt.api.NetworkingProvider;
import org.opendaylight.ovsdb.openstack.netvirt.api.TenantNetworkManager;
import org.opendaylight.ovsdb.plugin.api.OvsdbConfigurationService;
import org.opendaylight.ovsdb.plugin.api.OvsdbConnectionService;
import org.opendaylight.ovsdb.plugin.api.StatusWithUuid;
import org.opendaylight.ovsdb.schema.openvswitch.Bridge;
import org.opendaylight.ovsdb.schema.openvswitch.Interface;
import org.opendaylight.ovsdb.schema.openvswitch.Port;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Open vSwitch OpenFlow 1.0 Networking Provider for OpenStack Neutron
 */
public class OF10Provider implements NetworkingProvider {
    private static final Logger logger = LoggerFactory.getLogger(OF10Provider.class);
    private static final int INGRESS_TUNNEL_FLOW_PRIORITY = 100;
    private static final int EGRESS_TUNNEL_FLOW_PRIORITY = 100;
    private static final int DROP_FLOW_PRIORITY = 10;
    private static final int FLOOD_TUNNEL_FLOW_PRIORITY = 50;
    public static final int LLDP_PRIORITY = 1000;
    public static final int NORMAL_PRIORITY = 0;

    private volatile org.opendaylight.ovsdb.openstack.netvirt.api.ConfigurationService configurationService;
    private volatile BridgeConfigurationManager bridgeConfigurationManager;
    private volatile TenantNetworkManager tenantNetworkManager;
    private volatile OvsdbConfigurationService ovsdbConfigurationService;
    private volatile OvsdbConnectionService connectionService;
    private volatile IForwardingRulesManager forwardingRulesManager;
    private volatile ISwitchManager switchManager;

    public OF10Provider(){
    }

    @Override
    public boolean hasPerTenantTunneling() {
        return true;
    }

    private Status getTunnelReadinessStatus (Node node, String tunnelKey) {
        InetAddress srcTunnelEndPoint = configurationService.getTunnelEndPoint(node);
        if (srcTunnelEndPoint == null) {
            logger.error("Tunnel Endpoint not configured for Node {}", node);
            return new Status(StatusCode.NOTFOUND, "Tunnel Endpoint not configured for "+ node);
        }

        if (!bridgeConfigurationManager.isNodeOverlayReady(node)) {
            logger.warn("{} is not Overlay ready. It might be an OpenStack Controller Node", node);
            return new Status(StatusCode.NOTACCEPTABLE, node+" is not Overlay ready");
        }

        if (!tenantNetworkManager.isTenantNetworkPresentInNode(node, tunnelKey)) {
            logger.debug(node+" has no network corresponding to segment "+ tunnelKey);
            return new Status(StatusCode.NOTACCEPTABLE, node+" has no network corresponding to segment "+ tunnelKey);
        }
        return new Status(StatusCode.SUCCESS);
    }

    private Status getVlanReadinessStatus (Node node, String segmentationId) {
        if (!bridgeConfigurationManager.isNodeOverlayReady(node)) {
            logger.warn("{} is not Overlay ready. It might be an OpenStack Controller Node", node);
            return new Status(StatusCode.NOTACCEPTABLE, node+" is not Overlay ready");
        }

        if (!tenantNetworkManager.isTenantNetworkPresentInNode(node, segmentationId)) {
            logger.debug(node+" has no network corresponding to segment "+ segmentationId);
            return new Status(StatusCode.NOTACCEPTABLE, node+" has no network corresponding to segment "+ segmentationId);
        }
        return new Status(StatusCode.SUCCESS);
    }

    /**
     * Program OF1.0 Flow rules on br-tun on the ingress direction from the network towards the br-int.
     * The logic is to simply match on the incoming tunnel OF-Port (which carries the TenantNetwork GRE-Key)
     * and rewrite the Corresponding internal Vlan and pass it on to br-int via the patch port.
     */
    private void programLocalIngressTunnelBridgeRules(Node node, long tunnelOFPort, int internalVlan, long patchPort) {
        String brNetId = bridgeConfigurationManager.getBridgeUuid(node, configurationService.getNetworkBridgeName());
        if (brNetId == null) {
            logger.error("Failed to initialize Flow Rules for {}", node);
            return;
        }
        try {
            Row row = ovsdbConfigurationService
                    .getRow(node, ovsdbConfigurationService.getTableName(node, Bridge.class), brNetId);
            Bridge bridge = ovsdbConfigurationService.getTypedRow(node, Bridge.class, row);
            Set<String> dpids = bridge.getDatapathIdColumn().getData();
            if (dpids == null || dpids.size() ==  0) return;
            Long dpidLong = HexEncode.stringToLong((String) dpids.toArray()[0]);
            Node ofNode = new Node(Node.NodeIDType.OPENFLOW, dpidLong);
            String flowName = "TepMatch"+tunnelOFPort+""+internalVlan;
            FlowConfig flow = new FlowConfig();
            flow.setName(flowName);
            flow.setNode(ofNode);
            flow.setInstallInHw(true);
            flow.setPriority(INGRESS_TUNNEL_FLOW_PRIORITY+"");
            flow.setIngressPort(tunnelOFPort+"");
            List<String> actions = Lists.newArrayList();
            actions.add(ActionType.SET_VLAN_ID+"="+internalVlan);
            actions.add(ActionType.OUTPUT.toString()+"="+patchPort);
            flow.setActions(actions);
            Status status = this.addStaticFlow(ofNode, flow);
            logger.debug("Local Ingress Flow Programming Status {} for Flow {} on {} / {}", status, flow, ofNode, node);
        } catch (Exception e) {
            logger.error("Failed to initialize Flow Rules for {}", node, e);
        }
    }

    private void removeLocalIngressTunnelBridgeRules(Node node, long tunnelOFPort, int internalVlan, long patchPort) {
        String brNetId = bridgeConfigurationManager.getBridgeUuid(node, configurationService.getNetworkBridgeName());
        if (brNetId == null) {
            logger.error("Failed to remove Flow Rules for {}", node);
            return;
        }
        try {
            Row row = ovsdbConfigurationService
                    .getRow(node, ovsdbConfigurationService.getTableName(node, Bridge.class), brNetId);
            Bridge bridge = ovsdbConfigurationService.getTypedRow(node, Bridge.class, row);
            Set<String> dpids = bridge.getDatapathIdColumn().getData();
            if (dpids == null || dpids.size() ==  0) return;
            Long dpidLong = HexEncode.stringToLong((String) dpids.toArray()[0]);
            Node ofNode = new Node(Node.NodeIDType.OPENFLOW, dpidLong);
            String flowName = "TepMatch"+tunnelOFPort+""+internalVlan;

            Status status = this.deleteStaticFlow(ofNode, flowName);
            logger.debug("Local Ingress Flow Removal Status {} for Flow {} on {} / {}", status, flowName, ofNode, node);
        } catch (Exception e) {
            logger.error("Failed to Remove Flow Rules for {}", node, e);
        }
    }

    /**
     * Program OF1.0 Flow rules on br-tun on the remote Node on its egress direction towards the overlay network
     * for a VM (with the attachedMac).
     * The logic is to simply match on the incoming vlan, mac from the patch-port connected to br-int (patch-int)
     * and output the traffic to the appropriate GRE Tunnel (which carries the GRE-Key for that Tenant Network).
     * Also perform the Strip-Vlan action.
     */
    private void programRemoteEgressTunnelBridgeRules(Node node, long patchPort, String attachedMac,
            int internalVlan, long tunnelOFPort) {
        String brNetId = bridgeConfigurationManager.getBridgeUuid(node, configurationService.getNetworkBridgeName());
        if (brNetId == null) {
            logger.error("Failed to initialize Flow Rules for {}", node);
            return;
        }
        try {
            Row row = ovsdbConfigurationService
                    .getRow(node, ovsdbConfigurationService.getTableName(node, Bridge.class), brNetId);
            Bridge bridge = ovsdbConfigurationService.getTypedRow(node, Bridge.class, row);
            Set<String> dpids = bridge.getDatapathIdColumn().getData();
            if (dpids == null || dpids.size() ==  0) return;
            Long dpidLong = HexEncode.stringToLong((String) dpids.toArray()[0]);
            Node ofNode = new Node(Node.NodeIDType.OPENFLOW, dpidLong);
            String flowName = "TepMatch"+tunnelOFPort+""+internalVlan+""+HexEncode.stringToLong(attachedMac);
            FlowConfig flow = new FlowConfig();
            flow.setName(flowName);
            flow.setNode(ofNode);
            flow.setInstallInHw(true);
            flow.setPriority(EGRESS_TUNNEL_FLOW_PRIORITY+"");
            flow.setDstMac(attachedMac);
            flow.setIngressPort(patchPort+"");
            flow.setVlanId(internalVlan+"");
            List<String> actions = Lists.newArrayList();
            actions.add(ActionType.POP_VLAN.toString());
            actions.add(ActionType.OUTPUT.toString()+"="+tunnelOFPort);
            flow.setActions(actions);
            Status status = this.addStaticFlow(ofNode, flow);
            logger.debug("Remote Egress Flow Programming Status {} for Flow {} on {} / {}", status, flow, ofNode, node);
        } catch (Exception e) {
            logger.error("Failed to initialize Flow Rules for {}", node, e);
        }
    }

    private void removeRemoteEgressTunnelBridgeRules(Node node, long patchPort, String attachedMac,
            int internalVlan, long tunnelOFPort) {
        String brNetId = bridgeConfigurationManager.getBridgeUuid(node, configurationService.getNetworkBridgeName());
        if (brNetId == null) {
            logger.error("Failed to initialize Flow Rules for {}", node);
            return;
        }
        try {
            Row row = ovsdbConfigurationService
                    .getRow(node, ovsdbConfigurationService.getTableName(node, Bridge.class), brNetId);
            Bridge bridge = ovsdbConfigurationService.getTypedRow(node, Bridge.class, row);
            Set<String> dpids = bridge.getDatapathIdColumn().getData();
            if (dpids == null || dpids.size() ==  0) return;
            Long dpidLong = HexEncode.stringToLong((String) dpids.toArray()[0]);
            Node ofNode = new Node(Node.NodeIDType.OPENFLOW, dpidLong);
            String flowName = "TepMatch"+tunnelOFPort+""+internalVlan+""+HexEncode.stringToLong(attachedMac);
            Status status = this.deleteStaticFlow(ofNode, flowName);
            logger.debug("Remote Egress Flow Removal Status {} for Flow {} on {} / {}", status, flowName, ofNode, node);
        } catch (Exception e) {
            logger.error("Failed to Remove Flow Rules for {}", node, e);
        }
    }

    /**
     * Program OF1.0 Flow rules to flood the broadcast & unknown-unicast traffic over br-tun on the egress direction
     * towards the network on all the overlay tunnels that corresponds to the tenant network.
     * The logic is to simply match on the incoming vlan, mac from the patch-port connected to br-int (patch-int)
     * and output the traffic to all the GRE-Tunnels for this Tenant Network (which carries the GRE-Key).
     * Also perform the Strip-Vlan action.
     */
    private void programFloodEgressTunnelBridgeRules(Node node, long patchPort, int internalVlan, long tunnelOFPort) {
        String brNetId = bridgeConfigurationManager.getBridgeUuid(node, configurationService.getNetworkBridgeName());
        if (brNetId == null) {
            logger.error("Failed to initialize Flow Rules for {}", node);
            return;
        }
        try {
            Row row = ovsdbConfigurationService
                    .getRow(node, ovsdbConfigurationService.getTableName(node, Bridge.class), brNetId);
            Bridge bridge = ovsdbConfigurationService.getTypedRow(node, Bridge.class, row);
            Set<String> dpids = bridge.getDatapathIdColumn().getData();
            if (dpids == null || dpids.size() ==  0) return;
            Long dpidLong = HexEncode.stringToLong((String) dpids.toArray()[0]);
            Node ofNode = new Node(Node.NodeIDType.OPENFLOW, dpidLong);
            String flowName = "TepFlood"+internalVlan;
            FlowConfig existingFlowConfig = forwardingRulesManager.getStaticFlow(flowName, ofNode);
            FlowConfig flow = existingFlowConfig;
            Status status;
            if (flow == null) {
                flow = new FlowConfig();
                flow.setName(flowName);
                flow.setNode(ofNode);
                flow.setPriority(FLOOD_TUNNEL_FLOW_PRIORITY+"");
                flow.setIngressPort(patchPort+"");
                flow.setVlanId(internalVlan+"");
                List<String> actions = Lists.newArrayList();
                actions.add(ActionType.POP_VLAN.toString());
                actions.add(ActionType.OUTPUT.toString()+"="+tunnelOFPort);
                flow.setActions(actions);
                status = forwardingRulesManager.addStaticFlow(flow);
                logger.debug("Add Flood Egress Flow Programming Status {} for Flow {} on {} / {}",
                              status, flow, ofNode, node);
            } else {
                flow = new FlowConfig(existingFlowConfig);
                List<String> actions = flow.getActions();
                String outputPort = ActionType.OUTPUT.toString()+"="+tunnelOFPort;
                if (actions != null && !actions.contains(outputPort)) {
                    actions.add(outputPort);
                    flow.setActions(actions);
                } else {
                    logger.debug("Flood Egress Flow already exists. Skipping modify for Flow {} on {} / {}",
                                 flow, ofNode, node);
                    return;
                }
                status = forwardingRulesManager.modifyStaticFlow(flow);
                logger.debug("Modify Flood Egress Flow Programming Status {} for Flow {} on {} / {}",
                              status, flow, ofNode, node);
            }
        } catch (Exception e) {
            logger.error("Failed to initialize Flow Rules for {}", node, e);
        }
    }

    private void removeFloodEgressTunnelBridgeRules(Node node, long patchPort, int internalVlan, long tunnelOFPort) {
        String brNetId = bridgeConfigurationManager.getBridgeUuid(node, configurationService.getNetworkBridgeName());
        if (brNetId == null) {
            logger.error("Failed to remove Flow Rules for {}", node);
            return;
        }
        try {
            Row row = ovsdbConfigurationService
                    .getRow(node, ovsdbConfigurationService.getTableName(node, Bridge.class), brNetId);
            Bridge bridge = ovsdbConfigurationService.getTypedRow(node, Bridge.class, row);
            Set<String> dpids = bridge.getDatapathIdColumn().getData();
            if (dpids == null || dpids.size() ==  0) return;
            Long dpidLong = HexEncode.stringToLong((String) dpids.toArray()[0]);
            Node ofNode = new Node(Node.NodeIDType.OPENFLOW, dpidLong);
            String flowName = "TepFlood"+internalVlan;
            FlowConfig flow = forwardingRulesManager.getStaticFlow(flowName, ofNode);
            Status status;
            if (flow != null) {
                status = forwardingRulesManager.removeStaticFlow(flowName, ofNode);
                logger.debug("Remove Flood Egress Flow Programming Status {} for Flow {} on {} / {}",
                              status, flow, ofNode, node);

            } else {
                logger.debug("Flood Egress Flow already removed. Skipping removal of flow on {} / {}",
                             ofNode, node);
            }
        } catch (Exception e) {
            logger.error("Failed to remove Flow Rules for {}", node, e);
        }
    }

    private void programTunnelRules (String tunnelType, String segmentationId, InetAddress dst, Node node,
                                     Interface iface, boolean local) {
        String networkId = tenantNetworkManager.getNetworkId(segmentationId);
        if (networkId == null) {
            logger.debug("Tenant Network not found with Segmentation-id {}", segmentationId);
            return;
        }
        int internalVlan = tenantNetworkManager.getInternalVlan(node, networkId);
        if (internalVlan == 0) {
            logger.debug("No InternalVlan provisioned for Tenant Network {}",networkId);
            return;
        }
        Map<String, String> externalIds = iface.getExternalIdsColumn().getData();
        if (externalIds == null) {
            logger.error("No external_ids seen in {}", iface);
            return;
        }

        String attachedMac = externalIds.get(Constants.EXTERNAL_ID_VM_MAC);
        if (attachedMac == null) {
            logger.error("No AttachedMac seen in {}", iface);
            return;
        }
        String patchInt = configurationService
                .getPatchPortName(new ImmutablePair<>(configurationService.getNetworkBridgeName(),
                                                      configurationService.getIntegrationBridgeName()));

        long patchOFPort = -1;
        try {
            Map<String, Row> ifaces = ovsdbConfigurationService.getRows(node, ovsdbConfigurationService.getTableName(node, Interface.class));
            if (ifaces != null) {
                for (Row row : ifaces.values()) {
                    Interface patchiface = ovsdbConfigurationService.getTypedRow(node, Interface.class, row);
                    if (patchiface.getName().equalsIgnoreCase(patchInt)) {
                        Set<Long> of_ports = patchiface.getOpenFlowPortColumn().getData();
                        if (of_ports == null || of_ports.size() <= 0) {
                            logger.error("Could NOT Identified Patch port {} on {}", patchInt, node);
                            continue;
                        }
                        patchOFPort = (long)of_ports.toArray()[0];
                        logger.debug("Identified Patch port {} -> OF ({}) on {}", patchInt, patchOFPort, node);
                        break;
                    }
                }
                if (patchOFPort == -1) {
                    logger.error("Cannot identify {} interface on {}", patchInt, node);
                }
                for (Row row : ifaces.values()) {
                    Interface tuniface = ovsdbConfigurationService.getTypedRow(node, Interface.class, row);
                    if (tuniface.getName().equals(this.getTunnelName(tunnelType, segmentationId, dst))) {
                        Set<Long> of_ports = tuniface.getOpenFlowPortColumn().getData();
                        if (of_ports == null || of_ports.size() <= 0) {
                            logger.warn("Could not Identify Tunnel port {} on {}. Don't panic. It might get converged soon...", tuniface.getName(), node);
                            continue;
                        }
                        long tunnelOFPort = (long)of_ports.toArray()[0];

                        if (tunnelOFPort == -1) {
                            logger.warn("Tunnel Port {} on node {}: OFPort = -1 . Don't panic. It might get converged soon...", tuniface.getName(), node);
                            return;
                        }
                        logger.debug("Identified Tunnel port {} -> OF ({}) on {}", tuniface.getName(), tunnelOFPort, node);

                        if (!local) {
                            programRemoteEgressTunnelBridgeRules(node, patchOFPort, attachedMac, internalVlan, tunnelOFPort);
                        }
                        programLocalIngressTunnelBridgeRules(node, tunnelOFPort, internalVlan, patchOFPort);
                        programFloodEgressTunnelBridgeRules(node, patchOFPort, internalVlan, tunnelOFPort);
                        return;
                    }
                }
            }
        } catch (Exception e) {
            logger.error("", e);
        }
    }

    private void removeTunnelRules (String tunnelType, String segmentationId, InetAddress dst, Node node,
            Interface iface, boolean local) {
        String networkId = tenantNetworkManager.getNetworkId(segmentationId);
        if (networkId == null) {
            logger.debug("Tenant Network not found with Segmentation-id {}",segmentationId);
            return;
        }
        int internalVlan = tenantNetworkManager.getInternalVlan(node, networkId);
        if (internalVlan == 0) {
            logger.debug("No InternalVlan provisioned for Tenant Network {}",networkId);
            return;
        }
        Map<String, String> externalIds = iface.getExternalIdsColumn().getData();
        if (externalIds == null) {
            logger.error("No external_ids seen in {}", iface);
            return;
        }

        String attachedMac = externalIds.get(Constants.EXTERNAL_ID_VM_MAC);
        if (attachedMac == null) {
            logger.error("No AttachedMac seen in {}", iface);
            return;
        }
        String patchInt = configurationService
                .getPatchPortName(new ImmutablePair<>(configurationService.getNetworkBridgeName(),
                                                      configurationService.getIntegrationBridgeName()));

        long patchOFPort = -1;
        try {
            Map<String, Row> ifaces = ovsdbConfigurationService.getRows(node, ovsdbConfigurationService.getTableName(node, Interface.class));
            if (ifaces != null) {
                for (Row row : ifaces.values()) {
                    Interface patchiface = ovsdbConfigurationService.getTypedRow(node, Interface.class, row);
                    if (patchiface.getName().equalsIgnoreCase(patchInt)) {
                        Set<Long> of_ports = patchiface.getOpenFlowPortColumn().getData();
                        if (of_ports == null || of_ports.size() <= 0) {
                            logger.error("Could NOT Identified Patch port {} on {}", patchInt, node);
                            continue;
                        }
                        patchOFPort = (long)of_ports.toArray()[0];
                        logger.debug("Identified Patch port {} -> OF ({}) on {}", patchInt, patchOFPort, node);
                        break;
                    }
                }
                if (patchOFPort == -1) {
                    logger.error("Cannot identify {} interface on {}", patchInt, node);
                }
                for (Row row : ifaces.values()) {
                    Interface tuniface = ovsdbConfigurationService.getTypedRow(node, Interface.class, row);
                    if (tuniface.getName().equals(this.getTunnelName(tunnelType, segmentationId, dst))) {
                        Set<Long> of_ports = tuniface.getOpenFlowPortColumn().getData();
                        if (of_ports == null || of_ports.size() <= 0) {
                            logger.error("Could NOT Identify Tunnel port {} on {}", tuniface.getName(), node);
                            continue;
                        }
                        long tunnelOFPort = (long)of_ports.toArray()[0];

                        if (tunnelOFPort == -1) {
                            logger.error("Could NOT Identify Tunnel port {} -> OF ({}) on {}", tuniface.getName(), tunnelOFPort, node);
                            return;
                        }
                        logger.debug("Identified Tunnel port {} -> OF ({}) on {}", tuniface.getName(), tunnelOFPort, node);

                        if (!local) {
                            removeRemoteEgressTunnelBridgeRules(node, patchOFPort, attachedMac, internalVlan, tunnelOFPort);
                        }
                        removeLocalIngressTunnelBridgeRules(node, tunnelOFPort, internalVlan, patchOFPort);
                        removeFloodEgressTunnelBridgeRules(node, patchOFPort, internalVlan, tunnelOFPort);
                        return;
                    }
                }
            }
        } catch (Exception e) {
            logger.error("", e);
        }
    }

    private String getIntModVlanFlowName (long inOFPort, String fromVlan, String toVlan) {
        return "int_mod_"+inOFPort+"_"+fromVlan+"_"+toVlan;
    }

    private String getIntDropFlowName (long inOFPort) {
        return "int_drop_"+inOFPort;
    }

    private String getNetModVlanFlowName (long inOFPort, String fromVlan, String toVlan) {
        return "net_mod_"+inOFPort+"_"+fromVlan+"_"+toVlan;
    }

    private String getNetDropFlowName (long inOFPort) {
        return "net_drop_"+inOFPort;
    }

    private String getNetFwdFlowName (long inOFPort, long outOFPort, String vlan) {
        return "net_fwd_"+vlan+"_"+inOFPort+"_"+outOFPort;
    }

    private void deleteRule (Node node, Node ofNode, String flowName) {
        logger.debug("deleteRule: node: {} / {}, flowName: {}", node, ofNode, flowName);

        try {
            this.deleteStaticFlow(ofNode, flowName);
        } catch (Exception e) {
            logger.error("deleteRule: Failed to delete Flow Rule for {} / {}", node, ofNode, e);
        }
    }

    /* in_port=p actions=drop */
    private void programDropRule (Node node, Node ofNode, long inOFPort, String flowName) {
        logger.debug("programDropRule: node: {} / {}, inOfPort: {}, flowName: {}",
                node, ofNode, inOFPort, flowName);

        try {
            FlowConfig flow = new FlowConfig();
            flow.setName(flowName);
            flow.setNode(ofNode);
            flow.setInstallInHw(true);
            flow.setPriority(DROP_FLOW_PRIORITY+"");
            flow.setIngressPort(inOFPort+"");
            List<String> actions = Lists.newArrayList();
            actions.add(ActionType.DROP+"");
            flow.setActions(actions);
            Status status = this.addStaticFlow(ofNode, flow);
            logger.debug("programDropRule: Flow Programming Status {} for Flow {} on {} / {}",
                    status, flow, node, ofNode);
        } catch (Exception e) {
            logger.error("programDropRule: Failed to initialize Flow Rules for {} / {}", node, ofNode, e);
        }
    }

    /* in_port=p1,dl_vlan=v actions=mod_vlan_vid,[NORMAL|output:p2] */
    private void programModVlanRule (Node node, Node ofNode, long inOFPort, long outOFPort, String fromVlan,
                                     String toVlan, String flowName) {
        logger.debug("programModVlanRule: node: {} / {}, inOfPort: {}, fromVlan: {}, toVlan: {}, flowName: {}",
                node, ofNode, inOFPort, fromVlan, toVlan, flowName);

        try {
            FlowConfig flow = new FlowConfig();
            flow.setName(flowName);
            flow.setNode(ofNode);
            flow.setInstallInHw(true);
            flow.setPriority(INGRESS_TUNNEL_FLOW_PRIORITY+"");
            flow.setIngressPort(inOFPort+"");
            flow.setVlanId(fromVlan);
            List<String> actions = Lists.newArrayList();
            actions.add(ActionType.SET_VLAN_ID+"="+toVlan);
            if (outOFPort == -1) {
                actions.add(ActionType.HW_PATH.toString());
            } else {
                actions.add(ActionType.OUTPUT.toString()+"="+outOFPort);
            }
            flow.setActions(actions);
            Status status = this.addStaticFlow(ofNode, flow);
            logger.debug("programModVlanRule: Flow Programming Status {} for Flow {} on {} / {}",
                    status, flow, node, ofNode);
        } catch (Exception e) {
            logger.error("programModVlanRule: Failed to initialize Flow Rule for {} / {}", node, ofNode, e);
        }
    }

    /* in_port=p1,dl_vlan=v actions=output:p2 */
    private void programForwardRule (Node node, Node ofNode, long inOFPort, long outOFPort, String vlan, String flowName) {
        logger.debug("programForwardRule: node: {} / {}, inOfPort: {}, outOFPort: {}, flowName: {}",
                node, ofNode, inOFPort, outOFPort, flowName);

        try {
            FlowConfig flow = new FlowConfig();
            flow.setName(flowName);
            flow.setNode(ofNode);
            flow.setInstallInHw(true);
            flow.setPriority(EGRESS_TUNNEL_FLOW_PRIORITY + "");
            flow.setIngressPort(inOFPort + "");
            flow.setVlanId(vlan);
            List<String> actions = Lists.newArrayList();
            actions.add(ActionType.OUTPUT.toString()+"="+outOFPort);
            flow.setActions(actions);
            Status status = this.addStaticFlow(ofNode, flow);
            logger.debug("programForwardRule: Flow Programming Status {} for Flow {} on {} / {}",
                    status, flow, node, ofNode);
        } catch (Exception e) {
            logger.error("programForwardRule: Failed to initialize Flow Rules for {} / {}", node, ofNode, e);
        }
    }

    public long getOFPort (Node node, String portName) {
        long ofPort = -1;
        try {
            Map<String, Row> ifaces = ovsdbConfigurationService.getRows(node, ovsdbConfigurationService.getTableName(node, Interface.class));
            if (ifaces != null) {
                for (Row row : ifaces.values()) {
                    Interface iface = ovsdbConfigurationService.getTypedRow(node, Interface.class, row);
                    if (iface.getName().equalsIgnoreCase(portName)) {
                        Set<Long> of_ports = iface.getOpenFlowPortColumn().getData();
                        if (of_ports == null || of_ports.size() <= 0) {
                            logger.error("Could not identify patch port {} on {}", portName, node);
                            continue;
                        }
                        ofPort = (Long)of_ports.toArray()[0];
                        logger.debug("Identified port {} -> OF ({}) on {}", portName, ofPort, node);
                        break;
                    }
                }
            }
        } catch (Exception e) {
            logger.error("", e);
        }

        return ofPort;
    }

    /*
     * Transient class to return all the vlan network data needed for flow programming.
     */
    public class vlanNet {
        public long patchIntOfPort;
        public long patchNetOfPort;
        public long physicalOfPort;
        public int internalVlan;

        public vlanNet (NeutronNetwork network, Node node, Interface iface) {
            patchIntOfPort = -1;
            patchNetOfPort = -1;
            physicalOfPort = -1;
            internalVlan = 0;

            initializeVlanNet(network, node, iface);
        }

        public boolean isValid () {
            return (patchIntOfPort != -1) && (patchNetOfPort != -1) && (physicalOfPort != -1) && (internalVlan != -1);
        }

        public long getPatchIntOfPort () {
            return patchIntOfPort;
        }

        public long getPatchNetOfPort () {
            return patchNetOfPort;
        }

        public long getphysicalOfPort () {
            return physicalOfPort;
        }

        public int getInternalVlan () {
            return internalVlan;
        }

        public void initializeVlanNet (NeutronNetwork network, Node node, Interface iface) {
            internalVlan = tenantNetworkManager.getInternalVlan(node, network.getNetworkUUID());
            if (internalVlan == 0) {
                logger.debug("No InternalVlan provisioned for Tenant Network {}", network.getNetworkUUID());
                return;
            }

            /* Get ofports for patch ports and physical interface. */
            String patchToNetworkName = configurationService
                    .getPatchPortName(new ImmutablePair<>(configurationService.getIntegrationBridgeName(),
                                                          configurationService.getNetworkBridgeName()));

            String patchToIntegrationName = configurationService
                    .getPatchPortName(new ImmutablePair<>(configurationService.getNetworkBridgeName(),
                                                          configurationService.getIntegrationBridgeName()));

            String physNetName = bridgeConfigurationManager.getPhysicalInterfaceName(node, network.getProviderPhysicalNetwork());

            patchIntOfPort = getOFPort(node, patchToNetworkName);
            if (patchIntOfPort == -1) {
                logger.error("Cannot identify {} interface on {}", patchToNetworkName, node);
                return;
            }

            patchNetOfPort = getOFPort(node, patchToIntegrationName);
            if (patchNetOfPort == -1) {
                logger.error("Cannot identify {} interface on {}", patchToIntegrationName, node);
                return;
            }

            physicalOfPort = getOFPort(node, physNetName);
            if (physicalOfPort == -1) {
                logger.error("Cannot identify {} interface on {}", physNetName, node);
            }
        }
    }

    private Node getOFNode (Node node, String bridgeName) {
        String brUUID = bridgeConfigurationManager.getBridgeUuid(node, bridgeName);
        if (brUUID == null) {
            logger.error("getOFNode: Unable to find {} UUID on node {}", bridgeName, node);
            return null;
        }

        try {
            Row row = ovsdbConfigurationService
                    .getRow(node, ovsdbConfigurationService.getTableName(node, Bridge.class), brUUID);
            Bridge bridge = ovsdbConfigurationService.getTypedRow(node, Bridge.class, row);
            Set<String> dpids = bridge.getDatapathIdColumn().getData();
            if (dpids == null || dpids.size() ==  0) {
                return null;
            }
            Long dpidLong = HexEncode.stringToLong((String) dpids.toArray()[0]);
            return new Node(Node.NodeIDType.OPENFLOW, dpidLong);
        } catch (Exception e) {
            logger.error("deleteRule: Failed to delete Flow Rule for {}", node, e);
        }

        return null;
    }

    /*
     * Vlan isolation uses a patch port between br-int and br-net. Anything received on one end of
     * the patch is piped to the other end of the patch so the incoming packets from the network would
     * arrive untouched at the patch port on br-int.
     *
     * Program OF1.0 Flow rules on br-net in the ingress direction from the network
     * and egress direction towards the network.
     * The logic is to simply match on the incoming patch OF-Port and internal vlan,
     * rewrite the internal vlan to the external vlan and forward out the physical port.
     * There is also a flow to match the externally tagged packets from the physical port
     * rewrite the external tag to the internal tag and forward to the patch port.
     *
     * priority=100,in_port=1,dl_vlan=1 actions=mod_vlan_vid:2001,output:2
     * priority=100,in_port=2,dl_vlan=2001 actions=mod_vlan_vid:1actions=output:1
     */

    private void programVlanRules (NeutronNetwork network, Node node, Interface iface) {
        vlanNet vlanNet = new vlanNet(network, node, iface);
        if (vlanNet.isValid()) {
            String netBrName = configurationService.getNetworkBridgeName();
            String intModVlanFlowName = getIntModVlanFlowName(vlanNet.getPatchNetOfPort(), vlanNet.getInternalVlan()+"", network.getProviderSegmentationID());
            String netModVlanFlowName = getNetModVlanFlowName(vlanNet.getPatchNetOfPort(), network.getProviderSegmentationID(), vlanNet.getInternalVlan()+"");

            Node netOFNode = getOFNode(node, netBrName);
            if (netOFNode == null) {
                logger.error("Unable to find {} ofNode, Failed to initialize Flow Rules for {}", netBrName, node);
                return;
            }

            /* Program flows on br-net */
            deleteRule(node, netOFNode, "NORMAL");
            programModVlanRule(node, netOFNode, vlanNet.getPatchNetOfPort(), vlanNet.getphysicalOfPort(),
                    vlanNet.getInternalVlan()+"", network.getProviderSegmentationID(), intModVlanFlowName);
            programModVlanRule(node, netOFNode, vlanNet.getphysicalOfPort(), vlanNet.getPatchNetOfPort(),
                    network.getProviderSegmentationID(), vlanNet.getInternalVlan()+"", netModVlanFlowName);
        }
    }

    private void removeVlanRules (NeutronNetwork network, Node node, Interface iface) {
        vlanNet vlanNet = new vlanNet(network, node, iface);
        if (vlanNet.isValid()) {
            String netBrName = configurationService.getNetworkBridgeName();
            String intModVlanFlowName = getIntModVlanFlowName(vlanNet.getPatchNetOfPort(), vlanNet.getInternalVlan()+"", network.getProviderSegmentationID());
            String netModVlanFlowName = getNetModVlanFlowName(vlanNet.getPatchNetOfPort(), network.getProviderSegmentationID(), vlanNet.getInternalVlan()+"");

            Node netOFNode = getOFNode(node, netBrName);
            if (netOFNode == null) {
                logger.error("Unable to find {} ofNode, Failed to initialize Flow Rules for {}", netBrName, node);
                return;
            }

            deleteRule(node, netOFNode, intModVlanFlowName);
            deleteRule(node, netOFNode, netModVlanFlowName);
        }
    }

    @Override
    public Status handleInterfaceUpdate(NeutronNetwork network, Node srcNode, Interface iface) {
        logger.debug("handleInterfaceUpdate: networkType: {}, segmentationId: {}, srcNode: {}, iface: {}",
                     network.getProviderNetworkType(), network.getProviderSegmentationID(), srcNode, iface.getName());

        if (network.getProviderNetworkType().equalsIgnoreCase(NetworkHandler.NETWORK_TYPE_VLAN)) {
            Status status = getVlanReadinessStatus(srcNode, network.getProviderSegmentationID());
            if (!status.isSuccess()) {
                return status;
            } else {
                this.programVlanRules(network, srcNode, iface);
                return new Status(StatusCode.SUCCESS);
            }
        } else if (network.getProviderNetworkType().equalsIgnoreCase(NetworkHandler.NETWORK_TYPE_VXLAN) ||
                   network.getProviderNetworkType().equalsIgnoreCase(NetworkHandler.NETWORK_TYPE_GRE)) {
            Status status = getTunnelReadinessStatus(srcNode, network.getProviderSegmentationID());
            if (!status.isSuccess()) return status;

            List<Node> nodes = connectionService.getNodes();
            nodes.remove(srcNode);
            for (Node dstNode : nodes) {
                status = getTunnelReadinessStatus(dstNode, network.getProviderSegmentationID());
                if (!status.isSuccess()) continue;
                InetAddress src = configurationService.getTunnelEndPoint(srcNode);
                InetAddress dst = configurationService.getTunnelEndPoint(dstNode);
                status = addTunnelPort(srcNode, network.getProviderNetworkType(), src, dst, network.getProviderSegmentationID());
                if (status.isSuccess()) {
                    this.programTunnelRules(network.getProviderNetworkType(), network.getProviderSegmentationID(), dst, srcNode, iface, true);
                    status = addTunnelPort(dstNode, network.getProviderNetworkType(), dst, src, network.getProviderSegmentationID());
                    if (status.isSuccess()) {
                        this.programTunnelRules(network.getProviderNetworkType(), network.getProviderSegmentationID(), src, dstNode, iface, false);
                    }
                }
            }
            return new Status(StatusCode.SUCCESS);
        } else {
            return new Status(StatusCode.BADREQUEST);
        }
    }

    @Override
    public Status handleInterfaceDelete(String tunnelType, NeutronNetwork network,
                                        Node srcNode,Interface iface, boolean isLastInstanceOnNode) {
        Status status = new Status(StatusCode.SUCCESS);
        logger.debug("handleInterfaceDelete: srcNode: {}, networkType: {}, iface: {}, type: {}, isLast: {}",
                srcNode, (network != null) ? network.getProviderNetworkType() : "",
                iface.getName(), iface.getTypeColumn().getData(), isLastInstanceOnNode);

        List<String> phyIfName = bridgeConfigurationManager.getAllPhysicalInterfaceNames(srcNode);
        if ((network != null) && network.getProviderNetworkType().equalsIgnoreCase(NetworkHandler.NETWORK_TYPE_VLAN)) {
            if (isLastInstanceOnNode) {
                this.removeVlanRules(network, srcNode, iface);
            }
        } else if (iface.getTypeColumn().getData().equalsIgnoreCase(NetworkHandler.NETWORK_TYPE_VXLAN) ||
                   iface.getTypeColumn().getData().equalsIgnoreCase(NetworkHandler.NETWORK_TYPE_GRE)) {
            /* Delete tunnel port */
            try {
                Map<String, String> options = iface.getOptionsColumn().getData();
                InetAddress src = InetAddress.getByName(options.get("local_ip"));
                InetAddress dst = InetAddress.getByName(options.get("remote_ip"));
                String key = options.get("key");
                status = deleteTunnelPort(srcNode, iface.getTypeColumn().getData(), src, dst, key);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        } else if (phyIfName.contains(iface.getName())) {
            deletePhysicalPort(srcNode, iface.getName());
        } else {
            /* delete all other interfaces */
            List<Node> nodes = connectionService.getNodes();
            nodes.remove(srcNode);
            for (Node dstNode : nodes) {
                InetAddress src = configurationService.getTunnelEndPoint(srcNode);
                InetAddress dst = configurationService.getTunnelEndPoint(dstNode);
                assert network != null;
                this.removeTunnelRules(network.getProviderNetworkType(), network.getProviderSegmentationID(), dst, srcNode, iface, true);
                if (isLastInstanceOnNode) {
                    status = deleteTunnelPort(srcNode, network.getProviderNetworkType(), src, dst, network.getProviderSegmentationID());
                }
                this.removeTunnelRules(network.getProviderNetworkType(), network.getProviderSegmentationID(), src, dstNode, iface, false);
                if (status.isSuccess() && isLastInstanceOnNode) {
                    deleteTunnelPort(dstNode, network.getProviderNetworkType(), dst, src, network.getProviderSegmentationID());
                }
            }
        }
        return status;
    }

    private String getTunnelName(String tunnelType, String key, InetAddress dst) {
        return tunnelType+"-"+key+"-"+dst.getHostAddress();
    }

    private Interface getTunnelInterface (Node node, String tunnelType, InetAddress dst, String key) {
        try {
            String portName = getTunnelName(tunnelType, key, dst);

            Map<String, Row> tunifaces = ovsdbConfigurationService.getRows(node, ovsdbConfigurationService.getTableName(node, Interface.class));
            if (tunifaces != null) {
                for (Row row : tunifaces.values()) {
                    Interface tuniface = ovsdbConfigurationService.getTypedRow(node, Interface.class, row);
                    if (tuniface.getName().equals(portName)) return tuniface;
                }

            }
        } catch (Exception e) {
            logger.error("", e);
        }
        return null;
    }

    private boolean isTunnelPresent(Node node, String tunnelName, String bridgeUUID) throws Exception {
        Row row = ovsdbConfigurationService
                .getRow(node, ovsdbConfigurationService.getTableName(node, Bridge.class), bridgeUUID);
        Bridge bridge = ovsdbConfigurationService.getTypedRow(node, Bridge.class, row);
        if (bridge != null) {
            Set<UUID> ports = bridge.getPortsColumn().getData();
            for (UUID portUUID : ports) {
                Row portRow = ovsdbConfigurationService
                        .getRow(node, ovsdbConfigurationService.getTableName(node, Port.class), portUUID.toString());
                Port port = ovsdbConfigurationService.getTypedRow(node, Port.class, portRow);
                if (port != null && port.getName().equalsIgnoreCase(tunnelName)) return true;
            }
        }
        return false;
    }

    private String getPortUuid(Node node, String portName, String bridgeUUID) throws Exception {
        Row row = ovsdbConfigurationService
                .getRow(node, ovsdbConfigurationService.getTableName(node, Bridge.class), bridgeUUID);
        Bridge bridge = ovsdbConfigurationService.getTypedRow(node, Bridge.class, row);
        if (bridge != null) {
            Set<UUID> ports = bridge.getPortsColumn().getData();
            for (UUID portUUID : ports) {
                Row portRow = ovsdbConfigurationService
                        .getRow(node, ovsdbConfigurationService.getTableName(node, Port.class), portUUID.toString());
                Port port = ovsdbConfigurationService.getTypedRow(node, Port.class, portRow);
                if (port != null && port.getName().equalsIgnoreCase(portName)) return portUUID.toString();
            }
        }
        return null;
    }

    private Status addTunnelPort (Node node, String tunnelType, InetAddress src, InetAddress dst, String key) {
        try {
            String bridgeUUID = null;
            String tunnelBridgeName = configurationService.getNetworkBridgeName();
            Map<String, Row> bridgeTable = ovsdbConfigurationService.getRows(node, ovsdbConfigurationService.getTableName(node, Bridge.class));
            if (bridgeTable != null) {
                for (String uuid : bridgeTable.keySet()) {
                    Bridge bridge = ovsdbConfigurationService.getTypedRow(node, Bridge.class, bridgeTable.get(uuid));
                    if (bridge.getName().equals(tunnelBridgeName)) {
                        bridgeUUID = uuid;
                        break;
                    }
                }
            }
            if (bridgeUUID == null) {
                logger.error("Could not find Bridge {} in {}", tunnelBridgeName, node);
                return new Status(StatusCode.NOTFOUND, "Could not find "+tunnelBridgeName+" in "+node);
            }
            String portName = getTunnelName(tunnelType, key, dst);

            if (this.isTunnelPresent(node, portName, bridgeUUID)) {
                logger.trace("Tunnel {} is present in {} of {}", portName, tunnelBridgeName, node);
                return new Status(StatusCode.SUCCESS);
            }

            Port tunnelPort = ovsdbConfigurationService.createTypedRow(node, Port.class);
            tunnelPort.setName(portName);
            StatusWithUuid statusWithUuid = ovsdbConfigurationService
                    .insertRow(node, ovsdbConfigurationService.getTableName(node, Port.class), bridgeUUID,
                               tunnelPort.getRow());
            if (!statusWithUuid.isSuccess()) {
                logger.error("Failed to insert Tunnel port {} in {}", portName, bridgeUUID);
                return statusWithUuid;
            }

            String tunnelPortUUID = statusWithUuid.getUuid().toString();
            String interfaceUUID = null;
            int timeout = 6;
            while ((interfaceUUID == null) && (timeout > 0)) {
                Row row = ovsdbConfigurationService
                        .getRow(node, ovsdbConfigurationService.getTableName(node, Port.class), tunnelPortUUID);
                tunnelPort = ovsdbConfigurationService.getTypedRow(node, Port.class, row);
                Set<UUID> interfaces = tunnelPort.getInterfacesColumn().getData();
                if (interfaces == null || interfaces.size() == 0) {
                    // Wait for the OVSDB update to sync up the Local cache.
                    Thread.sleep(500);
                    timeout--;
                    continue;
                }
                interfaceUUID = interfaces.toArray()[0].toString();
                Row ifaceRow = ovsdbConfigurationService
                        .getRow(node, ovsdbConfigurationService.getTableName(node, Interface.class), interfaceUUID);
                Interface iface = ovsdbConfigurationService.getTypedRow(node, Interface.class, ifaceRow);
                if (iface == null) interfaceUUID = null;
            }

            if (interfaceUUID == null) {
                logger.error("Cannot identify Tunnel Interface for port {}/{}", portName, tunnelPortUUID);
                return new Status(StatusCode.INTERNALERROR);
            }

            Interface tunInterface = ovsdbConfigurationService.createTypedRow(node, Interface.class);
            tunInterface.setType(tunnelType);
            Map<String, String> options = Maps.newHashMap();
            options.put("key", key);
            options.put("local_ip", src.getHostAddress());
            options.put("remote_ip", dst.getHostAddress());
            tunInterface.setOptions(options);
            Status status = ovsdbConfigurationService
                    .updateRow(node, ovsdbConfigurationService.getTableName(node, Interface.class), tunnelPortUUID,
                               interfaceUUID, tunInterface.getRow());
            logger.debug("Tunnel {} add status : {}", tunInterface, status);
            return status;
        } catch (Exception e) {
            logger.error("Exception in addTunnelPort", e);
            return new Status(StatusCode.INTERNALERROR);
        }
    }

    private Status deletePort(Node node, String bridgeName, String portName) {
        try {
            String bridgeUUID = null;
            Map<String, Row> bridgeTable = ovsdbConfigurationService.getRows(node, ovsdbConfigurationService.getTableName(node, Bridge.class));
            if (bridgeTable != null) {
                for (String uuid : bridgeTable.keySet()) {
                    Bridge bridge = ovsdbConfigurationService.getTypedRow(node, Bridge.class, bridgeTable.get(uuid));
                    if (bridge.getName().equals(bridgeName)) {
                        bridgeUUID = uuid;
                        break;
                    }
                }
            }
            if (bridgeUUID == null) {
                logger.debug("Could not find Bridge {} in {}", bridgeName, node);
                return new Status(StatusCode.SUCCESS);
            }
            String portUUID = this.getPortUuid(node, portName, bridgeUUID);
            Status status = new Status(StatusCode.SUCCESS);
            if (portUUID != null) {
                status = ovsdbConfigurationService
                        .deleteRow(node, ovsdbConfigurationService.getTableName(node, Port.class), portUUID);
                if (!status.isSuccess()) {
                    logger.error("Failed to delete port {} in {} status : {}", portName, bridgeUUID, status);
                    return status;
                }
                logger.debug("Port {} delete status : {}", portName, status);
            }
            return status;
        } catch (Exception e) {
            logger.error("Exception in deleteTunnelPort", e);
            return new Status(StatusCode.INTERNALERROR);
        }
    }

    private Status deleteTunnelPort (Node node, String tunnelType, InetAddress src, InetAddress dst, String key) {
        String tunnelBridgeName = configurationService.getNetworkBridgeName();
        String portName = getTunnelName(tunnelType, key, dst);
        return deletePort(node, tunnelBridgeName, portName);
    }

    private Status deletePhysicalPort(Node node, String phyIfaceName) {
        String netBridgeName = configurationService.getNetworkBridgeName();
        return deletePort(node, netBridgeName, phyIfaceName);
    }

    @Override
    public Status handleInterfaceUpdate(String tunnelType, String tunnelKey) {
        List<Node> nodes = connectionService.getNodes();
        for (Node srcNode : nodes) {
            this.handleInterfaceUpdate(null, srcNode, null);
        }
        return new Status(StatusCode.SUCCESS);
    }

    @Override
    public void initializeFlowRules(Node node) {
        this.initializeFlowRules(node, configurationService.getIntegrationBridgeName());
        this.initializeFlowRules(node, configurationService.getExternalBridgeName());
    }

    private void initializeFlowRules(Node node, String bridgeName) {
        String bridgeUuid = bridgeConfigurationManager.getBridgeUuid(node, bridgeName);
        if (bridgeUuid == null) {
            if (bridgeName.equals(configurationService.getExternalBridgeName())){
                logger.debug("Failed to initialize Flow Rules for bridge {} on node {}. Is the Neutron L3 agent running on this node?");
            }
            else {
                logger.debug("Failed to initialize Flow Rules for bridge {} on node {}", bridgeName, node);
            }
            return;
        }

        try {
            Row row = ovsdbConfigurationService
                    .getRow(node, ovsdbConfigurationService.getTableName(node, Bridge.class), bridgeUuid);
            Bridge bridge = ovsdbConfigurationService.getTypedRow(node, Bridge.class, row);
            Set<String> dpids = bridge.getDatapathIdColumn().getData();
            if (dpids == null || dpids.size() ==  0) return;
            Long dpidLong = HexEncode.stringToLong((String) dpids.toArray()[0]);
            Node ofNode = new Node(Node.NodeIDType.OPENFLOW, dpidLong);
            List<Switch> nodes = switchManager.getNetworkDevices();
            if (nodes == null) {
                logger.debug("No OF nodes learned yet in {}", node);
                return;
            }
            for (Switch device : nodes) {
                if (device.getNode().equals(ofNode)) {
                    logger.debug("Initialize OF Flows on {}", ofNode);
                    initializeNormalFlowRules(ofNode);
                    return;
                }
            }
            logger.debug("Could not identify OF node {} for bridge {} in {}", ofNode.toString(), bridgeName, node.toString());
        } catch (Exception e) {
            logger.error("Failed to initialize Flow Rules for "+node.toString(), e);
        }
    }

    @Override
    public void initializeOFFlowRules(Node openflowNode) {
        this.initializeNormalFlowRules(openflowNode);
        this.initializeLLDPFlowRules(openflowNode);
    }

    private void initializeNormalFlowRules(Node ofNode) {
        String flowName = ActionType.HW_PATH.toString();
        FlowConfig flow = new FlowConfig();
        flow.setName("NORMAL");
        flow.setNode(ofNode);
        flow.setPriority(NORMAL_PRIORITY+"");
        flow.setInstallInHw(true);
        List<String> normalAction = Lists.newArrayList();
        normalAction.add(flowName);
        flow.setActions(normalAction);
        Status status = this.addStaticFlow(ofNode, flow);
        logger.debug("Flow Programming Add Status {} for Flow {} on {}", status, flow, ofNode);
    }

    private void initializeLLDPFlowRules(Node ofNode) {
        String flowName = "PuntLLDP";
        List<String> puntAction = Lists.newArrayList();
        puntAction.add(ActionType.CONTROLLER.toString());

        FlowConfig allowLLDP = new FlowConfig();
        allowLLDP.setName(flowName);
        allowLLDP.setPriority(LLDP_PRIORITY+"");
        allowLLDP.setNode(ofNode);
        allowLLDP.setInstallInHw(true);
        allowLLDP.setEtherType("0x" + Integer.toHexString(EtherTypes.LLDP.intValue()).toUpperCase());
        allowLLDP.setActions(puntAction);
        Status status = this.addStaticFlow(ofNode, allowLLDP);
        logger.debug("LLDP Flow Add Status {} for Flow {} on {}", status, allowLLDP, ofNode);
    }

    private Status addStaticFlow (Node ofNode, FlowConfig flowConfig) {
        String flowName = flowConfig.getName();
        if (forwardingRulesManager.getStaticFlow(flowName, ofNode) != null) {
            logger.debug("Flow already exists {} on {}. Skipping installation.", flowName, ofNode);
            return new Status(StatusCode.CONFLICT, "Flow with name "+flowName+" exists in node "+ofNode.toString());
        }
        return forwardingRulesManager.addStaticFlow(flowConfig);
    }

    private Status deleteStaticFlow (Node ofNode, String flowName) {
        if (forwardingRulesManager.getStaticFlow(flowName, ofNode) == null) {
            logger.debug("Flow does not exist {} on {}. Skipping deletion.", flowName, ofNode);
            return new Status(StatusCode.SUCCESS);
        }
        return forwardingRulesManager.removeStaticFlow(flowName,ofNode);
    }

}
