/*
 * Copyright (C) 2013 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Madhu Venugopal, Brent Salisbury
 */
package org.opendaylight.ovsdb.neutron.provider;

import java.math.BigInteger;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.opendaylight.controller.forwardingrulesmanager.FlowConfig;
import org.opendaylight.controller.forwardingrulesmanager.IForwardingRulesManager;
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
    Map<NodeVlan, FlowConfig> floodEntries = new HashMap<NodeVlan, FlowConfig>();
    private static final int INGRESS_TUNNEL_FLOW_PRIORITY = 100;
    private static final int EGRESS_TUNNEL_FLOW_PRIORITY = 100;
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
            logger.error(node+" is not Overlay ready");
            return new Status(StatusCode.NOTACCEPTABLE, node+" is not Overlay ready");
        }

        if (!TenantNetworkManager.getManager().isTenantNetworkPresentInNode(node, tunnelKey)) {
            logger.debug(node+" has no VM corresponding to segment "+ tunnelKey);
            return new Status(StatusCode.NOTACCEPTABLE, node+" has no VM corresponding to segment "+ tunnelKey);
        }
        return new Status(StatusCode.SUCCESS);
    }

    /**
     * Program OF1.0 Flow rules on br-tun on the ingress direction from the network towards the br-int.
     * The logic is to simply match on the incoming tunnel OF-Port (which carries the TenantNetwork GRE-Key)
     * and rewrite the Corresponding internal Vlan and pass it on to br-int via the patch port.
     */
    private void programLocalIngressTunnelBridgeRules(Node node, int tunnelOFPort, int internalVlan, int patchPort) {
        String brIntId = InternalNetworkManager.getManager().getInternalBridgeUUID(node, AdminConfigManager.getManager().getTunnelBridgeName());
        if (brIntId == null) {
            logger.error("Failed to initialize Flow Rules for {}", node);
            return;
        }
        try {
            OVSDBConfigService ovsdbTable = (OVSDBConfigService)ServiceHelper.getGlobalInstance(OVSDBConfigService.class, this);
            Bridge bridge = (Bridge) ovsdbTable.getRow(node, Bridge.NAME.getName(), brIntId);
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

    /**
     * Program OF1.0 Flow rules on br-tun on the remote Node on its egress direction towards the overlay network
     * for a VM (with the attachedMac).
     * The logic is to simply match on the incoming vlan, mac from the patch-port connected to br-int (patch-int)
     * and output the traffic to the appropriate GRE Tunnel (which carries the GRE-Key for that Tenant Network).
     * Also perform the Strip-Vlan action.
     */
    private void programRemoteEgressTunnelBridgeRules(Node node, int patchPort, String attachedMac,
            int internalVlan, int tunnelOFPort) {
        String brIntId = InternalNetworkManager.getManager().getInternalBridgeUUID(node, AdminConfigManager.getManager().getTunnelBridgeName());
        if (brIntId == null) {
            logger.error("Failed to initialize Flow Rules for {}", node);
            return;
        }
        try {
            OVSDBConfigService ovsdbTable = (OVSDBConfigService)ServiceHelper.getGlobalInstance(OVSDBConfigService.class, this);
            Bridge bridge = (Bridge) ovsdbTable.getRow(node, Bridge.NAME.getName(), brIntId);
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

    /**
     * Program OF1.0 Flow rules to flood the broadcast & unknown-unicast traffic over br-tun on the egress direction
     * towards the network on all the overlay tunnels that corresponds to the tenant network.
     * The logic is to simply match on the incoming vlan, mac from the patch-port connected to br-int (patch-int)
     * and output the traffic to all the GRE-Tunnels for this Tenant Network (which carries the GRE-Key).
     * Also perform the Strip-Vlan action.
     */
    private void programFloodEgressTunnelBridgeRules(Node node, int patchPort, int internalVlan, int tunnelOFPort) {
        String brIntId = InternalNetworkManager.getManager().getInternalBridgeUUID(node, AdminConfigManager.getManager().getTunnelBridgeName());
        if (brIntId == null) {
            logger.error("Failed to initialize Flow Rules for {}", node);
            return;
        }
        try {
            OVSDBConfigService ovsdbTable = (OVSDBConfigService)ServiceHelper.getGlobalInstance(OVSDBConfigService.class, this);
            Bridge bridge = (Bridge) ovsdbTable.getRow(node, Bridge.NAME.getName(), brIntId);
            Set<String> dpids = bridge.getDatapath_id();
            if (dpids == null || dpids.size() ==  0) return;
            Long dpidLong = Long.valueOf(HexEncode.stringToLong((String)dpids.toArray()[0]));
            Node ofNode = new Node(Node.NodeIDType.OPENFLOW, dpidLong);
            NodeVlan nv = new NodeVlan(ofNode, internalVlan);
            FlowConfig existingFlowConfig = floodEntries.get(nv);
            IForwardingRulesManager frm = (IForwardingRulesManager) ServiceHelper.getInstance(
                    IForwardingRulesManager.class, "default", this);
            FlowConfig flow = existingFlowConfig;
            Status status = null;
            if (flow == null) {
                flow = new FlowConfig();
                flow.setName("TepFlood"+internalVlan);
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
                    return;
                }
                status = frm.modifyStaticFlow(flow);
                logger.debug("Modify Flood Egress Flow Programming Status {} for Flow {} on {} / {}",
                              status, flow, ofNode, node);
            }
            if (status.isSuccess()) {
                floodEntries.put(nv, flow);
            }

        } catch (Exception e) {
            logger.error("Failed to initialize Flow Rules for {}", node, e);
        }
    }

    private void programTunnelRules (String tunnelType, String segmentationId, InetAddress dst, Node node,
                                     Interface intf, boolean local) {
        String networkId = TenantNetworkManager.getManager().getNetworkIdForSegmentationId(segmentationId);
        if (networkId == null) {
            logger.debug("Tenant Network not found with Segmenation-id {}",segmentationId);
            return;
        }
        int internalVlan = TenantNetworkManager.getManager().getInternalVlan(networkId);
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

    @Override
    public Status createTunnels(String tunnelType, String tunnelKey, Node srcNode, Interface intf) {
        Status status = getTunnelReadinessStatus(srcNode, tunnelKey);
        if (!status.isSuccess()) return status;

        IConnectionServiceInternal connectionService = (IConnectionServiceInternal)ServiceHelper.getGlobalInstance(IConnectionServiceInternal.class, this);
        List<Node> nodes = connectionService.getNodes();
        nodes.remove(srcNode);
        for (Node dstNode : nodes) {
            status = getTunnelReadinessStatus(dstNode, tunnelKey);
            if (!status.isSuccess()) continue;
            InetAddress src = AdminConfigManager.getManager().getTunnelEndPoint(srcNode);
            InetAddress dst = AdminConfigManager.getManager().getTunnelEndPoint(dstNode);
            status = addTunnelPort(srcNode, tunnelType, src, dst, tunnelKey);
            if (status.isSuccess()) {
                this.programTunnelRules(tunnelType, tunnelKey, dst, srcNode, intf, true);
            }
            addTunnelPort(dstNode, tunnelType, dst, src, tunnelKey);
            if (status.isSuccess()) {
                this.programTunnelRules(tunnelType, tunnelKey, src, dstNode, intf, false);
            }
        }
        return new Status(StatusCode.SUCCESS);
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

    private Status addTunnelPort (Node node, String tunnelType, InetAddress src, InetAddress dst, String key) {
        try {
            String bridgeUUID = null;
            String tunnelBridgeName = AdminConfigManager.getManager().getTunnelBridgeName();
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

    @Override
    public Status createTunnels(String tunnelType, String tunnelKey) {
        IConnectionServiceInternal connectionService = (IConnectionServiceInternal)ServiceHelper.getGlobalInstance(IConnectionServiceInternal.class, this);
        List<Node> nodes = connectionService.getNodes();
        for (Node srcNode : nodes) {
            this.createTunnels(tunnelType, tunnelKey, srcNode, null);
        }
        return new Status(StatusCode.SUCCESS);
    }

    @Override
    public void initializeFlowRules(Node node) {
        this.initializeFlowRules(node, AdminConfigManager.getManager().getIntegrationBridgeName());
        this.initializeFlowRules(node, AdminConfigManager.getManager().getTunnelBridgeName());
        this.initializeFlowRules(node, AdminConfigManager.getManager().getExternalBridgeName());
    }

    private void initializeFlowRules(Node node, String bridgeName) {
        String brIntId = this.getInternalBridgeUUID(node, bridgeName);
        if (brIntId == null) {
            logger.error("Failed to initialize Flow Rules for {}", node);
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

    private class NodeVlan {
        Node node;
        int vlan;
        public NodeVlan(Node node, int vlan) {
            super();
            this.node = node;
            this.vlan = vlan;
        }
        public Node getNode() {
            return node;
        }
        public int getVlan() {
            return vlan;
        }
        @Override
        public String toString() {
            return "NodeVlan [node=" + node + ", vlan=" + vlan + "]";
        }
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((node == null) ? 0 : node.hashCode());
            result = prime * result + vlan;
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
            NodeVlan other = (NodeVlan) obj;
            if (node == null) {
                if (other.node != null)
                    return false;
            } else if (!node.equals(other.node))
                return false;
            if (vlan != other.vlan)
                return false;
            return true;
        }
    }
}
