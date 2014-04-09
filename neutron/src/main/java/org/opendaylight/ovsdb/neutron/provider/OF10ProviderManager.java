/*
 * Copyright (C) 2013 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Madhu Venugopal, Brent Salisbury, Sam Hague
 */
package org.opendaylight.ovsdb.neutron.provider;

import java.math.BigInteger;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.opendaylight.controller.forwardingrulesmanager.FlowConfig;
import org.opendaylight.controller.forwardingrulesmanager.IForwardingRulesManager;
import org.opendaylight.controller.networkconfig.neutron.NeutronNetwork;
import org.opendaylight.controller.sal.action.ActionType;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.utils.EtherTypes;
import org.opendaylight.controller.sal.utils.HexEncode;
import org.opendaylight.controller.sal.utils.ServiceHelper;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.controller.sal.utils.StatusCode;
import org.opendaylight.controller.switchmanager.ISwitchManager;
import org.opendaylight.controller.switchmanager.Switch;
import org.opendaylight.ovsdb.lib.notation.OvsDBMap;
import org.opendaylight.ovsdb.lib.notation.OvsDBSet;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.table.Bridge;
import org.opendaylight.ovsdb.lib.table.Interface;
import org.opendaylight.ovsdb.lib.table.Port;
import org.opendaylight.ovsdb.lib.table.internal.Table;
import org.opendaylight.ovsdb.neutron.AdminConfigManager;
import org.opendaylight.ovsdb.neutron.InternalNetworkManager;
import org.opendaylight.ovsdb.neutron.TenantNetworkManager;
import org.opendaylight.ovsdb.plugin.IConnectionServiceInternal;
import org.opendaylight.ovsdb.plugin.OVSDBConfigService;
import org.opendaylight.ovsdb.plugin.StatusWithUuid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


class OF10ProviderManager extends ProviderNetworkManager {
    private static final Logger logger = LoggerFactory.getLogger(OF10ProviderManager.class);
    private static final int INGRESS_TUNNEL_FLOW_PRIORITY = 100;
    private static final int EGRESS_TUNNEL_FLOW_PRIORITY = 100;
    private static final int DROP_FLOW_PRIORITY = 50;
    private static final int FLOOD_TUNNEL_FLOW_PRIORITY = 1;

    @Override
    public boolean hasPerTenantTunneling() {
        return true;
    }

    private Status getTunnelReadinessStatus (Node node, String tunnelKey) {
        InetAddress srcTunnelEndPoint = AdminConfigManager.getManager().getTunnelEndPoint(node);
        if (srcTunnelEndPoint == null) {
            logger.error("Tunnel Endpoint not configured for Node {}", node);
            return new Status(StatusCode.NOTFOUND, "Tunnel Endpoint not configured for "+ node);
        }

        if (!InternalNetworkManager.getManager().isInternalNetworkOverlayReady(node)) {
            logger.warn("{} is not Overlay ready. It might be an OpenStack Controller Node", node);
            return new Status(StatusCode.NOTACCEPTABLE, node+" is not Overlay ready");
        }

        if (!TenantNetworkManager.getManager().isTenantNetworkPresentInNode(node, tunnelKey)) {
            logger.debug(node+" has no network corresponding to segment "+ tunnelKey);
            return new Status(StatusCode.NOTACCEPTABLE, node+" has no network corresponding to segment "+ tunnelKey);
        }
        return new Status(StatusCode.SUCCESS);
    }

    private Status getVlanReadinessStatus (Node node, String segmentationId) {
        if (!InternalNetworkManager.getManager().isInternalNetworkOverlayReady(node)) {
            logger.warn("{} is not Overlay ready. It might be an OpenStack Controller Node", node);
            return new Status(StatusCode.NOTACCEPTABLE, node+" is not Overlay ready");
        }

        if (!TenantNetworkManager.getManager().isTenantNetworkPresentInNode(node, segmentationId)) {
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
    private void programLocalIngressTunnelBridgeRules(Node node, int tunnelOFPort, int internalVlan, int patchPort) {
        String brNetId = InternalNetworkManager.getManager().getInternalBridgeUUID(node, AdminConfigManager.getManager().getNetworkBridgeName());
        if (brNetId == null) {
            logger.error("Failed to initialize Flow Rules for {}", node);
            return;
        }
        try {
            OVSDBConfigService ovsdbTable = (OVSDBConfigService)ServiceHelper.getGlobalInstance(OVSDBConfigService.class, this);
            Bridge bridge = (Bridge) ovsdbTable.getRow(node, Bridge.NAME.getName(), brNetId);
            Set<String> dpids = bridge.getDatapath_id();
            if (dpids == null || dpids.size() ==  0) return;
            Long dpidLong = Long.valueOf(HexEncode.stringToLong((String)dpids.toArray()[0]));
            Node ofNode = new Node(Node.NodeIDType.OPENFLOW, dpidLong);
            String flowName = "TepMatch"+tunnelOFPort+""+internalVlan;
            FlowConfig flow = new FlowConfig();
            flow.setName(flowName);
            flow.setNode(ofNode);
            flow.setInstallInHw(true);
            flow.setPriority(INGRESS_TUNNEL_FLOW_PRIORITY+"");
            flow.setIngressPort(tunnelOFPort+"");
            List<String> actions = new ArrayList<String>();
            actions.add(ActionType.SET_VLAN_ID+"="+internalVlan);
            actions.add(ActionType.OUTPUT.toString()+"="+patchPort);
            flow.setActions(actions);
            Status status = this.addStaticFlow(ofNode, flow);
            logger.debug("Local Ingress Flow Programming Status {} for Flow {} on {} / {}", status, flow, ofNode, node);
        } catch (Exception e) {
            logger.error("Failed to initialize Flow Rules for {}", node, e);
        }
    }

    private void removeLocalIngressTunnelBridgeRules(Node node, int tunnelOFPort, int internalVlan, int patchPort) {
        String brNetId = InternalNetworkManager.getManager().getInternalBridgeUUID(node, AdminConfigManager.getManager().getNetworkBridgeName());
        if (brNetId == null) {
            logger.error("Failed to remove Flow Rules for {}", node);
            return;
        }
        try {
            OVSDBConfigService ovsdbTable = (OVSDBConfigService)ServiceHelper.getGlobalInstance(OVSDBConfigService.class, this);
            Bridge bridge = (Bridge) ovsdbTable.getRow(node, Bridge.NAME.getName(), brNetId);
            Set<String> dpids = bridge.getDatapath_id();
            if (dpids == null || dpids.size() ==  0) return;
            Long dpidLong = Long.valueOf(HexEncode.stringToLong((String)dpids.toArray()[0]));
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
    private void programRemoteEgressTunnelBridgeRules(Node node, int patchPort, String attachedMac,
            int internalVlan, int tunnelOFPort) {
        String brNetId = InternalNetworkManager.getManager().getInternalBridgeUUID(node, AdminConfigManager.getManager().getNetworkBridgeName());
        if (brNetId == null) {
            logger.error("Failed to initialize Flow Rules for {}", node);
            return;
        }
        try {
            OVSDBConfigService ovsdbTable = (OVSDBConfigService)ServiceHelper.getGlobalInstance(OVSDBConfigService.class, this);
            Bridge bridge = (Bridge) ovsdbTable.getRow(node, Bridge.NAME.getName(), brNetId);
            Set<String> dpids = bridge.getDatapath_id();
            if (dpids == null || dpids.size() ==  0) return;
            Long dpidLong = Long.valueOf(HexEncode.stringToLong((String)dpids.toArray()[0]));
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
            List<String> actions = new ArrayList<String>();
            actions.add(ActionType.POP_VLAN.toString());
            actions.add(ActionType.OUTPUT.toString()+"="+tunnelOFPort);
            flow.setActions(actions);
            Status status = this.addStaticFlow(ofNode, flow);
            logger.debug("Remote Egress Flow Programming Status {} for Flow {} on {} / {}", status, flow, ofNode, node);
        } catch (Exception e) {
            logger.error("Failed to initialize Flow Rules for {}", node, e);
        }
    }

    private void removeRemoteEgressTunnelBridgeRules(Node node, int patchPort, String attachedMac,
            int internalVlan, int tunnelOFPort) {
        String brNetId = InternalNetworkManager.getManager().getInternalBridgeUUID(node, AdminConfigManager.getManager().getNetworkBridgeName());
        if (brNetId == null) {
            logger.error("Failed to initialize Flow Rules for {}", node);
            return;
        }
        try {
            OVSDBConfigService ovsdbTable = (OVSDBConfigService)ServiceHelper.getGlobalInstance(OVSDBConfigService.class, this);
            Bridge bridge = (Bridge) ovsdbTable.getRow(node, Bridge.NAME.getName(), brNetId);
            Set<String> dpids = bridge.getDatapath_id();
            if (dpids == null || dpids.size() ==  0) return;
            Long dpidLong = Long.valueOf(HexEncode.stringToLong((String)dpids.toArray()[0]));
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
    private void programFloodEgressTunnelBridgeRules(Node node, int patchPort, int internalVlan, int tunnelOFPort) {
        String brNetId = InternalNetworkManager.getManager().getInternalBridgeUUID(node, AdminConfigManager.getManager().getNetworkBridgeName());
        if (brNetId == null) {
            logger.error("Failed to initialize Flow Rules for {}", node);
            return;
        }
        try {
            OVSDBConfigService ovsdbTable = (OVSDBConfigService)ServiceHelper.getGlobalInstance(OVSDBConfigService.class, this);
            Bridge bridge = (Bridge) ovsdbTable.getRow(node, Bridge.NAME.getName(), brNetId);
            Set<String> dpids = bridge.getDatapath_id();
            if (dpids == null || dpids.size() ==  0) return;
            Long dpidLong = Long.valueOf(HexEncode.stringToLong((String)dpids.toArray()[0]));
            Node ofNode = new Node(Node.NodeIDType.OPENFLOW, dpidLong);
            String flowName = "TepFlood"+internalVlan;
            IForwardingRulesManager frm = (IForwardingRulesManager) ServiceHelper.getInstance(
                    IForwardingRulesManager.class, "default", this);
            FlowConfig existingFlowConfig = frm.getStaticFlow(flowName, ofNode);
            FlowConfig flow = existingFlowConfig;
            Status status = null;
            if (flow == null) {
                flow = new FlowConfig();
                flow.setName(flowName);
                flow.setNode(ofNode);
                flow.setPriority(FLOOD_TUNNEL_FLOW_PRIORITY+"");
                flow.setIngressPort(patchPort+"");
                flow.setVlanId(internalVlan+"");
                List<String> actions = new ArrayList<String>();
                actions.add(ActionType.POP_VLAN.toString());
                actions.add(ActionType.OUTPUT.toString()+"="+tunnelOFPort);
                flow.setActions(actions);
                status = frm.addStaticFlow(flow);
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
                status = frm.modifyStaticFlow(flow);
                logger.debug("Modify Flood Egress Flow Programming Status {} for Flow {} on {} / {}",
                              status, flow, ofNode, node);
            }
        } catch (Exception e) {
            logger.error("Failed to initialize Flow Rules for {}", node, e);
        }
    }

    private void removeFloodEgressTunnelBridgeRules(Node node, int patchPort, int internalVlan, int tunnelOFPort) {
        String brNetId = InternalNetworkManager.getManager().getInternalBridgeUUID(node, AdminConfigManager.getManager().getNetworkBridgeName());
        if (brNetId == null) {
            logger.error("Failed to remove Flow Rules for {}", node);
            return;
        }
        try {
            OVSDBConfigService ovsdbTable = (OVSDBConfigService)ServiceHelper.getGlobalInstance(OVSDBConfigService.class, this);
            Bridge bridge = (Bridge) ovsdbTable.getRow(node, Bridge.NAME.getName(), brNetId);
            Set<String> dpids = bridge.getDatapath_id();
            if (dpids == null || dpids.size() ==  0) return;
            Long dpidLong = Long.valueOf(HexEncode.stringToLong((String)dpids.toArray()[0]));
            Node ofNode = new Node(Node.NodeIDType.OPENFLOW, dpidLong);
            String flowName = "TepFlood"+internalVlan;
            IForwardingRulesManager frm = (IForwardingRulesManager) ServiceHelper.getInstance(
                    IForwardingRulesManager.class, "default", this);
            FlowConfig flow = frm.getStaticFlow(flowName, ofNode);
            Status status = null;
            if (flow != null) {
                status = frm.removeStaticFlow(flowName, ofNode);
                logger.debug("Remove Flood Egress Flow Programming Status {} for Flow {} on {} / {}",
                              status, flow, ofNode, node);

            } else {
                logger.debug("Flood Egress Flow already removed. Skipping removal for Flow {} on {} / {}",
                             flow, ofNode, node);
                return;
            }
        } catch (Exception e) {
            logger.error("Failed to remove Flow Rules for {}", node, e);
        }
    }

    private void programTunnelRules (String tunnelType, String segmentationId, InetAddress dst, Node node,
                                     Interface intf, boolean local) {
        String networkId = TenantNetworkManager.getManager().getNetworkIdForSegmentationId(segmentationId);
        if (networkId == null) {
            logger.debug("Tenant Network not found with Segmentation-id {}", segmentationId);
            return;
        }
        int internalVlan = TenantNetworkManager.getManager().getInternalVlan(node, networkId);
        if (internalVlan == 0) {
            logger.debug("No InternalVlan provisioned for Tenant Network {}",networkId);
            return;
        }
        Map<String, String> externalIds = intf.getExternal_ids();
        if (externalIds == null) {
            logger.error("No external_ids seen in {}", intf);
            return;
        }

        String attachedMac = externalIds.get(TenantNetworkManager.EXTERNAL_ID_VM_MAC);
        if (attachedMac == null) {
            logger.error("No AttachedMac seen in {}", intf);
            return;
        }
        String patchInt = AdminConfigManager.getManager().getPatchToIntegration();

        int patchOFPort = -1;
        try {
            OVSDBConfigService ovsdbTable = (OVSDBConfigService)ServiceHelper.getGlobalInstance(OVSDBConfigService.class, this);
            Map<String, Table<?>> intfs = ovsdbTable.getRows(node, Interface.NAME.getName());
            if (intfs != null) {
                for (Table<?> row : intfs.values()) {
                    Interface patchIntf = (Interface)row;
                    if (patchIntf.getName().equalsIgnoreCase(patchInt)) {
                        Set<BigInteger> of_ports = patchIntf.getOfport();
                        if (of_ports == null || of_ports.size() <= 0) {
                            logger.error("Could NOT Identified Patch port {} on {}", patchInt, node);
                            continue;
                        }
                        patchOFPort = Long.valueOf(((BigInteger)of_ports.toArray()[0]).longValue()).intValue();
                        logger.debug("Identified Patch port {} -> OF ({}) on {}", patchInt, patchOFPort, node);
                        break;
                    }
                }
                if (patchOFPort == -1) {
                    logger.error("Cannot identify {} interface on {}", patchInt, node);
                }
                for (Table<?> row : intfs.values()) {
                    Interface tunIntf = (Interface)row;
                    if (tunIntf.getName().equals(this.getTunnelName(tunnelType, segmentationId, dst))) {
                        Set<BigInteger> of_ports = tunIntf.getOfport();
                        if (of_ports == null || of_ports.size() <= 0) {
                            logger.warn("Could not Identify Tunnel port {} on {}. Don't panic. It might get converged soon...", tunIntf.getName(), node);
                            continue;
                        }
                        int tunnelOFPort = Long.valueOf(((BigInteger)of_ports.toArray()[0]).longValue()).intValue();

                        if (tunnelOFPort == -1) {
                            logger.warn("Tunnel Port {} on node {}: OFPort = -1 . Don't panic. It might get converged soon...", tunIntf.getName(), node);
                            return;
                        }
                        logger.debug("Identified Tunnel port {} -> OF ({}) on {}", tunIntf.getName(), tunnelOFPort, node);

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
            Interface intf, boolean local) {
        String networkId = TenantNetworkManager.getManager().getNetworkIdForSegmentationId(segmentationId);
        if (networkId == null) {
            logger.debug("Tenant Network not found with Segmentation-id {}",segmentationId);
            return;
        }
        int internalVlan = TenantNetworkManager.getManager().getInternalVlan(node,networkId);
        if (internalVlan == 0) {
            logger.debug("No InternalVlan provisioned for Tenant Network {}",networkId);
            return;
        }
        Map<String, String> externalIds = intf.getExternal_ids();
        if (externalIds == null) {
            logger.error("No external_ids seen in {}", intf);
            return;
        }

        String attachedMac = externalIds.get(TenantNetworkManager.EXTERNAL_ID_VM_MAC);
        if (attachedMac == null) {
            logger.error("No AttachedMac seen in {}", intf);
            return;
        }
        String patchInt = AdminConfigManager.getManager().getPatchToIntegration();

        int patchOFPort = -1;
        try {
            OVSDBConfigService ovsdbTable = (OVSDBConfigService)ServiceHelper.getGlobalInstance(OVSDBConfigService.class, this);
            Map<String, Table<?>> intfs = ovsdbTable.getRows(node, Interface.NAME.getName());
            if (intfs != null) {
                for (Table<?> row : intfs.values()) {
                    Interface patchIntf = (Interface)row;
                    if (patchIntf.getName().equalsIgnoreCase(patchInt)) {
                        Set<BigInteger> of_ports = patchIntf.getOfport();
                        if (of_ports == null || of_ports.size() <= 0) {
                            logger.error("Could NOT Identified Patch port {} on {}", patchInt, node);
                            continue;
                        }
                        patchOFPort = Long.valueOf(((BigInteger)of_ports.toArray()[0]).longValue()).intValue();
                        logger.debug("Identified Patch port {} -> OF ({}) on {}", patchInt, patchOFPort, node);
                        break;
                    }
                }
                if (patchOFPort == -1) {
                    logger.error("Cannot identify {} interface on {}", patchInt, node);
                }
                for (Table<?> row : intfs.values()) {
                    Interface tunIntf = (Interface)row;
                    if (tunIntf.getName().equals(this.getTunnelName(tunnelType, segmentationId, dst))) {
                        Set<BigInteger> of_ports = tunIntf.getOfport();
                        if (of_ports == null || of_ports.size() <= 0) {
                            logger.error("Could NOT Identify Tunnel port {} on {}", tunIntf.getName(), node);
                            continue;
                        }
                        int tunnelOFPort = Long.valueOf(((BigInteger)of_ports.toArray()[0]).longValue()).intValue();

                        if (tunnelOFPort == -1) {
                            logger.error("Could NOT Identify Tunnel port {} -> OF ({}) on {}", tunIntf.getName(), tunnelOFPort, node);
                            return;
                        }
                        logger.debug("Identified Tunnel port {} -> OF ({}) on {}", tunIntf.getName(), tunnelOFPort, node);

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

    private String getIntModVlanFlowName (int patchOFPort, String fromVlan, String toVlan) {
        return "int_mod_"+patchOFPort+"_"+fromVlan+"_"+toVlan;
    }

    private String getIntDropFlowName (int patchOFPort) {
        return "int_drop_"+patchOFPort;
    }

    private String getNetModVlanFlowName (int patchOFPort, String fromVlan, String toVlan) {
        return "net_mod_"+patchOFPort+"_"+fromVlan+"_"+toVlan;
    }

    private String getNetDropFlowName (int patchOFPort) {
        return "net_drop_"+patchOFPort;
    }

    private String getNetFwdFlowName (int inOFPort, int outOFPort, String vlan) {
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
    private void programDropRule (Node node, Node ofNode, int inOFPort, String flowName) {
        logger.debug("programDropRule: node: {} / {}, inOfPort: {}, flowName: {}",
                node, ofNode, inOFPort, flowName);

        try {
            FlowConfig flow = new FlowConfig();
            flow.setName(flowName);
            flow.setNode(ofNode);
            flow.setInstallInHw(true);
            flow.setPriority(DROP_FLOW_PRIORITY+"");
            flow.setIngressPort(inOFPort+"");
            List<String> actions = new ArrayList<String>();
            actions.add(ActionType.DROP+"");
            flow.setActions(actions);
            Status status = this.addStaticFlow(ofNode, flow);
            logger.debug("programDropRule: Flow Programming Status {} for Flow {} on {} / {}",
                    status, flow, node, ofNode);
        } catch (Exception e) {
            logger.error("programDropRule: Failed to initialize Flow Rules for {} / {}", node, ofNode, e);
        }
    }

    /* in_port=p2,dl_vlan=v actions=mod_vlan_vid,[NORMAL|output:p2] */
    private void programModVlanRule (Node node, Node ofNode, int inOFPort, int outOFPort, String fromVlan,
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
            List<String> actions = new ArrayList<String>();
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
    private void programForwardRule (Node node, Node ofNode, int inOFPort, int outOFPort, String vlan, String flowName) {
        logger.debug("programModVlanRule: node: {} / {}, inOfPort: {}, outOFPort: {}, flowName: {}",
                node, ofNode, inOFPort, outOFPort, flowName);

        try {
            FlowConfig flow = new FlowConfig();
            flow.setName(flowName);
            flow.setNode(ofNode);
            flow.setInstallInHw(true);
            flow.setPriority(EGRESS_TUNNEL_FLOW_PRIORITY + "");
            flow.setIngressPort(inOFPort + "");
            flow.setVlanId(vlan);
            List<String> actions = new ArrayList<String>();
            actions.add(ActionType.OUTPUT.toString()+"="+outOFPort);
            flow.setActions(actions);
            Status status = this.addStaticFlow(ofNode, flow);
            logger.debug("programForwardRule: Flow Programming Status {} for Flow {} on {} / {}",
                    status, flow, node, ofNode);
        } catch (Exception e) {
            logger.error("programForwardRule: Failed to initialize Flow Rules for {} / {}", node, ofNode, e);
        }
    }

    /*
     * Transient class to return all the vlan network data needed for flow programming.
     */
    public class vlanNet {
        public int patchIntOfPort;
        public int patchNetOfPort;
        public int phyOfPort;
        public int internalVlan;

        public vlanNet (NeutronNetwork network, Node node, Interface intf) {
            patchIntOfPort = -1;
            patchNetOfPort = -1;
            phyOfPort = -1;
            internalVlan = 0;

            initializeVlanNet(network, node, intf);
        }

        public boolean isValid () {
            if ((patchIntOfPort != -1) && (patchNetOfPort != -1) && (phyOfPort != -1) && (internalVlan != -1)) {
                return true;
            } else {
                return false;
            }
        }

        public int getPatchIntOfPort () {
            return patchIntOfPort;
        }

        public int getPatchNetOfPort () {
            return patchNetOfPort;
        }

        public int getPhyOfPort () {
            return phyOfPort;
        }

        public int getInternalVlan () {
            return internalVlan;
        }

        public void initializeVlanNet (NeutronNetwork network, Node node, Interface intf) {
            internalVlan = TenantNetworkManager.getManager().getInternalVlan(node, network.getNetworkUUID());
            if (internalVlan == 0) {
                logger.debug("No InternalVlan provisioned for Tenant Network {}", network.getNetworkUUID());
                return;
            }

            /* Get ofports for patch ports and physical interface. */
            String patchToNetworkName = AdminConfigManager.getManager().getPatchToNetwork();
            String patchToIntegrationName = AdminConfigManager.getManager().getPatchToIntegration();
            String physNetName = AdminConfigManager.getManager().getPhyIfName(node, network.getProviderPhysicalNetwork());

            try {
                OVSDBConfigService ovsdbTable = (OVSDBConfigService)ServiceHelper.getGlobalInstance(OVSDBConfigService.class, this);
                Map<String, Table<?>> intfs = ovsdbTable.getRows(node, Interface.NAME.getName());
                if (intfs != null) {
                    for (Table<?> row : intfs.values()) {
                        Interface patchIntIntf = (Interface)row;
                        if (patchIntIntf.getName().equalsIgnoreCase(patchToNetworkName)) {
                            Set<BigInteger> of_ports = patchIntIntf.getOfport();
                            if (of_ports == null || of_ports.size() <= 0) {
                                logger.error("Could not identify patch port {} on {}", patchToNetworkName, node);
                                continue;
                            }
                            patchIntOfPort = Long.valueOf(((BigInteger)of_ports.toArray()[0]).longValue()).intValue();
                            logger.debug("Identified patch port {} -> OF ({}) on {}", patchToNetworkName, patchIntOfPort, node);
                            break;
                        }
                    }
                    if (patchIntOfPort == -1) {
                        logger.error("Cannot identify {} interface on {}", patchToNetworkName, node);
                        return;
                    }

                    for (Table<?> row : intfs.values()) {
                        Interface patchPhyIntf = (Interface)row;
                        if (patchPhyIntf.getName().equalsIgnoreCase(patchToIntegrationName)) {
                            Set<BigInteger> of_ports = patchPhyIntf.getOfport();
                            if (of_ports == null || of_ports.size() <= 0) {
                                logger.error("Could not identify patch port {} on {}", patchToIntegrationName, node);
                                continue;
                            }
                            patchNetOfPort = Long.valueOf(((BigInteger)of_ports.toArray()[0]).longValue()).intValue();
                            logger.debug("Identified patch port {} -> OF ({}) on {}", patchToIntegrationName, patchNetOfPort, node);
                            break;
                        }
                    }
                    if (patchNetOfPort == -1) {
                        logger.error("Cannot identify {} interface on {}", patchToNetworkName, node);
                        return;
                    }

                    for (Table<?> row : intfs.values()) {
                        Interface phyIntf = (Interface)row;
                        if (phyIntf.getName().equalsIgnoreCase(physNetName)) {
                            Set<BigInteger> of_ports = phyIntf.getOfport();
                            if (of_ports == null || of_ports.size() <= 0) {
                                logger.error("Could not identify port {} on {}", physNetName, node);
                                continue;
                            }
                            phyOfPort = Long.valueOf(((BigInteger)of_ports.toArray()[0]).longValue()).intValue();
                            logger.debug("Identified port {} -> OF ({}) on {}", physNetName, phyOfPort, node);
                            break;
                        }
                    }
                    if (phyOfPort == -1) {
                        logger.error("Cannot identify {} interface on {}", physNetName, node);
                        return;
                    }
                }
            } catch (Exception e) {
                logger.error("", e);
            }

            return;
        }
    }

    private Node getOFNode (Node node, String bridgeName) {
        String brUUID = InternalNetworkManager.getManager().getInternalBridgeUUID(node, bridgeName);
        if (brUUID == null) {
            logger.error("getOFNode: Unable to find {} UUID on node {}", bridgeName, node);
            return null;
        }

        try {
            OVSDBConfigService ovsdbTable = (OVSDBConfigService)ServiceHelper.getGlobalInstance(OVSDBConfigService.class, this);
            Bridge bridge = (Bridge) ovsdbTable.getRow(node, Bridge.NAME.getName(), brUUID);
            Set<String> dpids = bridge.getDatapath_id();
            if (dpids == null || dpids.size() ==  0) {
                return null;
            }
            Long dpidLong = Long.valueOf(HexEncode.stringToLong((String)dpids.toArray()[0]));
            Node ofNode = new Node(Node.NodeIDType.OPENFLOW, dpidLong);
            return ofNode;
        } catch (Exception e) {
            logger.error("deleteRule: Failed to delete Flow Rule for {}", node, e);
        }

        return null;
    }

    /*
     * Vlan isolation uses a patch port between br-int and br-net. Anything received on one end of
     * the patch is piped to the other end of the patch so the incoming packets from the network would
     * arrive untouched at the patch port on br-int.

     * Program OF1.0 Flow rules on br-int in the ingress direction from the network towards the br-int.
     * The logic is to simply match on the incoming patch OF-Port and external vlan
     * and rewrite the external vlan to the internal vlan. All other traffic coming
     * in the patch port is dropped.
     * priority=100,in_port=2,dl_vlan=2001 actions=mod_vlan_vid:1,NORMAL
     * priority=50,in_port=2 actions=drop
     *
     * Program OF1.0 Flow rules on br-net in the egress direction towards the network.
     * The logic is to simply match on the incoming patch OF-Port and internal vlan,
     * rewrite the internal vlan to the external vlan and forward out the physical port. All
     * other traffic coming in the patch port is dropped.
     * There is also a flow to match the externally tagged packets from the physical port
     * and forward to the patch port.
     *
     * priority=100,in_port=1,dl_vlan=1 actions=mod_vlan_vid:2001,output:2
     * priority=50,in_port=1 actions=drop
     * priority=100,in_port=2,dl_vlan=2001 actions=output:1
     */

    private void programVlanRules (NeutronNetwork network, Node node, Interface intf) {
        vlanNet vlanNet = new vlanNet(network, node, intf);
        if (vlanNet.isValid()) {
            String intBrName = AdminConfigManager.getManager().getIntegrationBridgeName();
            String netBrName = AdminConfigManager.getManager().getNetworkBridgeName();
            String intDropFlowName = getIntDropFlowName(vlanNet.getPatchIntOfPort());
            String intModVlanFlowName = getIntModVlanFlowName(vlanNet.getPatchIntOfPort(), network.getProviderSegmentationID(), vlanNet.getInternalVlan()+"");
            String netDropFlowName = getNetDropFlowName(vlanNet.getPatchNetOfPort());
            String netModVlanFlowName = getNetModVlanFlowName(vlanNet.getPatchNetOfPort(), vlanNet.getInternalVlan()+"", network.getProviderSegmentationID());
            String netFwdFlowName = getNetFwdFlowName(vlanNet.getPhyOfPort(), vlanNet.getPatchNetOfPort(), network.getProviderSegmentationID());

            Node intOFNode = getOFNode(node, intBrName);
            if (intOFNode == null) {
                logger.error("Unable to find {} ofNode, Failed to initialize Flow Rules for {}", intBrName, node);
                return;
            }

            Node netOFNode = getOFNode(node, netBrName);
            if (netOFNode == null) {
                logger.error("Unable to find {} ofNode, Failed to initialize Flow Rules for {}", netBrName, node);
                return;
            }

            programDropRule(node, intOFNode, vlanNet.getPatchIntOfPort(), intDropFlowName);
            programModVlanRule(node, intOFNode, vlanNet.getPatchIntOfPort(), -1, network.getProviderSegmentationID(),
                    vlanNet.getInternalVlan()+"", intModVlanFlowName);

            deleteRule(node, netOFNode, "NORMAL");
            programDropRule(node, netOFNode, vlanNet.getPatchNetOfPort(), netDropFlowName);
            programModVlanRule(node, netOFNode, vlanNet.getPatchNetOfPort(), vlanNet.getPhyOfPort(),
                    vlanNet.getInternalVlan()+"", network.getProviderSegmentationID(), netModVlanFlowName);
            programForwardRule(node, netOFNode, vlanNet.getPhyOfPort(), vlanNet.getPatchNetOfPort(),
                    network.getProviderSegmentationID(), netFwdFlowName);
        }
    }

    private void removeVlanRules (NeutronNetwork network, Node node, Interface intf) {
        vlanNet vlanNet = new vlanNet(network, node, intf);
        if (vlanNet.isValid()) {
            String intBrName = AdminConfigManager.getManager().getIntegrationBridgeName();
            String netBrName = AdminConfigManager.getManager().getNetworkBridgeName();
            String intModVlanFlowName = getIntModVlanFlowName(vlanNet.getPatchIntOfPort(), vlanNet.getInternalVlan()+"", network.getProviderSegmentationID());
            String intDropFlowName = getIntDropFlowName(vlanNet.getPatchIntOfPort());
            String netModVlanFlowName = getNetModVlanFlowName(vlanNet.getPatchIntOfPort(), vlanNet.getInternalVlan()+"", network.getProviderSegmentationID());
            String netDropFlowName = getNetDropFlowName(vlanNet.getPatchIntOfPort());
            String netFwdFlowName = getNetFwdFlowName(vlanNet.getPhyOfPort(), vlanNet.getPatchIntOfPort(), network.getProviderSegmentationID());

            Node intOFNode = getOFNode(node, intBrName);
            if (intOFNode == null) {
                logger.error("Unable to find {} ofNode, Failed to initialize Flow Rules for {}", intBrName, node);
                return;
            }

            Node netOFNode = getOFNode(node, netBrName);
            if (netOFNode == null) {
                logger.error("Unable to find {} ofNode, Failed to initialize Flow Rules for {}", netBrName, node);
                return;
            }

            deleteRule(node, intOFNode, intModVlanFlowName);
            deleteRule(node, intOFNode, intDropFlowName);
            deleteRule(node, netOFNode, netFwdFlowName);
            deleteRule(node, netOFNode, netModVlanFlowName);
            deleteRule(node, netOFNode, netDropFlowName);
        }
    }

    @Override
    public Status handleInterfaceUpdate(NeutronNetwork network, Node srcNode, Interface intf) {
        logger.debug("handleInterfaceUpdate: networkType: {}, segmentationId: {}, srcNode: {}, intf: {}",
                     network.getProviderNetworkType(), network.getProviderSegmentationID(), srcNode, intf.getName());

        if (network.getProviderNetworkType().equalsIgnoreCase("vlan")) {
            Status status = getVlanReadinessStatus(srcNode, network.getProviderSegmentationID());
            if (!status.isSuccess()) {
                return status;
            } else {
                this.programVlanRules(network, srcNode, intf);
                return new Status(StatusCode.SUCCESS);
            }
        } else if (network.getProviderNetworkType().equalsIgnoreCase("vxlan") ||
                   network.getProviderNetworkType().equalsIgnoreCase("gre")) {
            Status status = getTunnelReadinessStatus(srcNode, network.getProviderSegmentationID());
            if (!status.isSuccess()) return status;

            IConnectionServiceInternal connectionService = (IConnectionServiceInternal)ServiceHelper.getGlobalInstance(IConnectionServiceInternal.class, this);
            List<Node> nodes = connectionService.getNodes();
            nodes.remove(srcNode);
            for (Node dstNode : nodes) {
                status = getTunnelReadinessStatus(dstNode, network.getProviderSegmentationID());
                if (!status.isSuccess()) continue;
                InetAddress src = AdminConfigManager.getManager().getTunnelEndPoint(srcNode);
                InetAddress dst = AdminConfigManager.getManager().getTunnelEndPoint(dstNode);
                status = addTunnelPort(srcNode, network.getProviderNetworkType(), src, dst, network.getProviderSegmentationID());
                if (status.isSuccess()) {
                    this.programTunnelRules(network.getProviderNetworkType(), network.getProviderSegmentationID(), dst, srcNode, intf, true);
                }
                addTunnelPort(dstNode, network.getProviderNetworkType(), dst, src, network.getProviderSegmentationID());
                if (status.isSuccess()) {
                    this.programTunnelRules(network.getProviderNetworkType(), network.getProviderSegmentationID(), src, dstNode, intf, false);
                }
            }
            return new Status(StatusCode.SUCCESS);
        } else {
            return new Status(StatusCode.BADREQUEST);
        }
    }

    @Override
    public Status handleInterfaceDelete(NeutronNetwork network, Node srcNode, Interface intf, boolean isLastInstanceOnNode) {
        Status status = new Status(StatusCode.SUCCESS);

        logger.debug("handleInterfaceDelete: networkType: {}, segmentationId: {}, srcNode: {}, intf: {}",
                network.getProviderNetworkType(), network.getProviderSegmentationID(), srcNode, intf.getName(), isLastInstanceOnNode);

        if (network.getProviderNetworkType().equalsIgnoreCase("vlan")) {
            this.removeVlanRules(network, srcNode, intf);
        } else if (network.getProviderNetworkType().equalsIgnoreCase("vxlan") ||
                   network.getProviderNetworkType().equalsIgnoreCase("gre")) {
            IConnectionServiceInternal connectionService = (IConnectionServiceInternal)ServiceHelper.getGlobalInstance(IConnectionServiceInternal.class, this);
            List<Node> nodes = connectionService.getNodes();
            nodes.remove(srcNode);
            for (Node dstNode : nodes) {
                InetAddress src = AdminConfigManager.getManager().getTunnelEndPoint(srcNode);
                InetAddress dst = AdminConfigManager.getManager().getTunnelEndPoint(dstNode);
                this.removeTunnelRules(network.getProviderNetworkType(), network.getProviderSegmentationID(), dst, srcNode, intf, true);
                if (isLastInstanceOnNode) {
                    status = deleteTunnelPort(srcNode, network.getProviderNetworkType(), src, dst, network.getProviderSegmentationID());
                }
                this.removeTunnelRules(network.getProviderNetworkType(), network.getProviderSegmentationID(), src, dstNode, intf, false);
                if (status.isSuccess() && isLastInstanceOnNode) {
                    deleteTunnelPort(dstNode, network.getProviderNetworkType(), dst, src, network.getProviderSegmentationID());
                }
            }
        } else {
            return new Status(StatusCode.BADREQUEST);
        }

        return status;
    }

    private String getTunnelName(String tunnelType, String key, InetAddress dst) {
        return tunnelType+"-"+key+"-"+dst.getHostAddress();
    }

    private Interface getTunnelInterface (Node node, String tunnelType, InetAddress dst, String key) {
        try {
            OVSDBConfigService ovsdbTable = (OVSDBConfigService)ServiceHelper.getGlobalInstance(OVSDBConfigService.class, this);
            String portName = getTunnelName(tunnelType, key, dst);

            Map<String, Table<?>> tunIntfs = ovsdbTable.getRows(node, Interface.NAME.getName());
            if (tunIntfs != null) {
                for (Table<?> row : tunIntfs.values()) {
                    Interface tunIntf = (Interface)row;
                    if (tunIntf.getName().equals(portName)) return tunIntf;
                }

            }
        } catch (Exception e) {
            logger.error("", e);
        }
        return null;
    }

    private boolean isTunnelPresent(Node node, String tunnelName, String bridgeUUID) throws Exception {
        OVSDBConfigService ovsdbTable = (OVSDBConfigService)ServiceHelper.getGlobalInstance(OVSDBConfigService.class, this);
        Bridge bridge = (Bridge)ovsdbTable.getRow(node, Bridge.NAME.getName(), bridgeUUID);
        if (bridge != null) {
            Set<UUID> ports = bridge.getPorts();
            for (UUID portUUID : ports) {
                Port port = (Port)ovsdbTable.getRow(node, Port.NAME.getName(), portUUID.toString());
                if (port != null && port.getName().equalsIgnoreCase(tunnelName)) return true;
            }
        }
        return false;
    }

    private String getTunnelPortUuid(Node node, String tunnelName, String bridgeUUID) throws Exception {
        OVSDBConfigService ovsdbTable = (OVSDBConfigService)ServiceHelper.getGlobalInstance(OVSDBConfigService.class, this);
        Bridge bridge = (Bridge)ovsdbTable.getRow(node, Bridge.NAME.getName(), bridgeUUID);
        if (bridge != null) {
            Set<UUID> ports = bridge.getPorts();
            for (UUID portUUID : ports) {
                Port port = (Port)ovsdbTable.getRow(node, Port.NAME.getName(), portUUID.toString());
                if (port != null && port.getName().equalsIgnoreCase(tunnelName)) return portUUID.toString();
            }
        }
        return null;
    }

    private Status addTunnelPort (Node node, String tunnelType, InetAddress src, InetAddress dst, String key) {
        try {
            String bridgeUUID = null;
            String tunnelBridgeName = AdminConfigManager.getManager().getNetworkBridgeName();
            OVSDBConfigService ovsdbTable = (OVSDBConfigService)ServiceHelper.getGlobalInstance(OVSDBConfigService.class, this);
            Map<String, Table<?>> bridgeTable = ovsdbTable.getRows(node, Bridge.NAME.getName());
            if (bridgeTable != null) {
                for (String uuid : bridgeTable.keySet()) {
                    Bridge bridge = (Bridge)bridgeTable.get(uuid);
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

            Port tunnelPort = new Port();
            tunnelPort.setName(portName);
            StatusWithUuid statusWithUuid = ovsdbTable.insertRow(node, Port.NAME.getName(), bridgeUUID, tunnelPort);
            if (!statusWithUuid.isSuccess()) {
                logger.error("Failed to insert Tunnel port {} in {}", portName, bridgeUUID);
                return statusWithUuid;
            }

            String tunnelPortUUID = statusWithUuid.getUuid().toString();
            String interfaceUUID = null;
            int timeout = 6;
            while ((interfaceUUID == null) && (timeout > 0)) {
                tunnelPort = (Port)ovsdbTable.getRow(node, Port.NAME.getName(), tunnelPortUUID);
                OvsDBSet<UUID> interfaces = tunnelPort.getInterfaces();
                if (interfaces == null || interfaces.size() == 0) {
                    // Wait for the OVSDB update to sync up the Local cache.
                    Thread.sleep(500);
                    timeout--;
                    continue;
                }
                interfaceUUID = interfaces.toArray()[0].toString();
                Interface intf = (Interface)ovsdbTable.getRow(node, Interface.NAME.getName(), interfaceUUID);
                if (intf == null) interfaceUUID = null;
            }

            if (interfaceUUID == null) {
                logger.error("Cannot identify Tunnel Interface for port {}/{}", portName, tunnelPortUUID);
                return new Status(StatusCode.INTERNALERROR);
            }

            Interface tunInterface = new Interface();
            tunInterface.setType(tunnelType);
            OvsDBMap<String, String> options = new OvsDBMap<String, String>();
            options.put("key", key);
            options.put("local_ip", src.getHostAddress());
            options.put("remote_ip", dst.getHostAddress());
            tunInterface.setOptions(options);
            Status status = ovsdbTable.updateRow(node, Interface.NAME.getName(), tunnelPortUUID, interfaceUUID, tunInterface);
            logger.debug("Tunnel {} add status : {}", tunInterface, status);
            return status;
        } catch (Exception e) {
            logger.error("Exception in addTunnelPort", e);
            return new Status(StatusCode.INTERNALERROR);
        }
    }

    private Status deleteTunnelPort (Node node, String tunnelType, InetAddress src, InetAddress dst, String key) {
        try {
            String bridgeUUID = null;
            String tunnelBridgeName = AdminConfigManager.getManager().getNetworkBridgeName();
            OVSDBConfigService ovsdbTable = (OVSDBConfigService)ServiceHelper.getGlobalInstance(OVSDBConfigService.class, this);
            Map<String, Table<?>> bridgeTable = ovsdbTable.getRows(node, Bridge.NAME.getName());
            if (bridgeTable != null) {
                for (String uuid : bridgeTable.keySet()) {
                    Bridge bridge = (Bridge)bridgeTable.get(uuid);
                    if (bridge.getName().equals(tunnelBridgeName)) {
                        bridgeUUID = uuid;
                        break;
                    }
                }
            }
            if (bridgeUUID == null) {
                logger.debug("Could not find Bridge {} in {}", tunnelBridgeName, node);
                return new Status(StatusCode.SUCCESS);
            }
            String portName = getTunnelName(tunnelType, key, dst);
            String tunnelPortUUID = this.getTunnelPortUuid(node, portName, bridgeUUID);
            Status status = ovsdbTable.deleteRow(node, Port.NAME.getName(), tunnelPortUUID);
            if (!status.isSuccess()) {
                logger.error("Failed to delete Tunnel port {} in {} status : {}", portName, bridgeUUID, status);
                return status;
            }

            logger.debug("Tunnel {} delete status : {}", portName, status);
            return status;
        } catch (Exception e) {
            logger.error("Exception in deleteTunnelPort", e);
            return new Status(StatusCode.INTERNALERROR);
        }
    }

    @Override
    public Status handleInterfaceUpdate(String tunnelType, String tunnelKey) {
        IConnectionServiceInternal connectionService = (IConnectionServiceInternal)ServiceHelper.getGlobalInstance(IConnectionServiceInternal.class, this);
        List<Node> nodes = connectionService.getNodes();
        for (Node srcNode : nodes) {
            this.handleInterfaceUpdate(null, srcNode, null);
        }
        return new Status(StatusCode.SUCCESS);
    }

    @Override
    public void initializeFlowRules(Node node) {
        this.initializeFlowRules(node, AdminConfigManager.getManager().getIntegrationBridgeName());
        this.initializeFlowRules(node, AdminConfigManager.getManager().getExternalBridgeName());
    }

    private void initializeFlowRules(Node node, String bridgeName) {
        String brIntId = this.getInternalBridgeUUID(node, bridgeName);
        if (brIntId == null) {
            if (bridgeName == AdminConfigManager.getManager().getExternalBridgeName()){
                logger.debug("Failed to initialize Flow Rules for bridge {} on node {}. Is the Neutron L3 agent running on this node?");
            }
            else {
                logger.debug("Failed to initialize Flow Rules for bridge {} on node {}", bridgeName, node);
            }
            return;
        }

        try {
            OVSDBConfigService ovsdbTable = (OVSDBConfigService)ServiceHelper.getGlobalInstance(OVSDBConfigService.class, this);
            Bridge bridge = (Bridge) ovsdbTable.getRow(node, Bridge.NAME.getName(), brIntId);
            Set<String> dpids = bridge.getDatapath_id();
            if (dpids == null || dpids.size() ==  0) return;
            Long dpidLong = Long.valueOf(HexEncode.stringToLong((String)dpids.toArray()[0]));
            Node ofNode = new Node(Node.NodeIDType.OPENFLOW, dpidLong);
            ISwitchManager switchManager = (ISwitchManager) ServiceHelper.getInstance(ISwitchManager.class, "default", this);
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
        List<String> normalAction = new ArrayList<String>();
        normalAction.add(flowName);
        flow.setActions(normalAction);
        Status status = this.addStaticFlow(ofNode, flow);
        logger.debug("Flow Programming Add Status {} for Flow {} on {}", status, flow, ofNode);
    }

    private void initializeLLDPFlowRules(Node ofNode) {
        String flowName = "PuntLLDP";
        List<String> puntAction = new ArrayList<String>();
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
        IForwardingRulesManager frm = (IForwardingRulesManager) ServiceHelper.getInstance(
                IForwardingRulesManager.class, "default", this);
        String flowName = flowConfig.getName();
        if (frm.getStaticFlow(flowName, ofNode) != null) {
            logger.debug("Flow already exists {} on {}. Skipping installation.", flowName, ofNode);
            return new Status(StatusCode.CONFLICT, "Flow with name "+flowName+" exists in node "+ofNode.toString());
        }
        return frm.addStaticFlow(flowConfig);
    }

    private Status deleteStaticFlow (Node ofNode, String flowName) {
        IForwardingRulesManager frm = (IForwardingRulesManager) ServiceHelper.getInstance(
                IForwardingRulesManager.class, "default", this);
        if (frm.getStaticFlow(flowName, ofNode) == null) {
            logger.debug("Flow does not exist {} on {}. Skipping deletion.", flowName, ofNode);
            return new Status(StatusCode.SUCCESS);
        }
        return frm.removeStaticFlow(flowName,ofNode);
    }}
