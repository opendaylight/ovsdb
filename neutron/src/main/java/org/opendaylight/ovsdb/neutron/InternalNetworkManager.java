package org.opendaylight.ovsdb.neutron;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.opendaylight.controller.forwardingrulesmanager.FlowConfig;
import org.opendaylight.controller.forwardingrulesmanager.IForwardingRulesManager;
import org.opendaylight.controller.networkconfig.neutron.INeutronNetworkCRUD;
import org.opendaylight.controller.networkconfig.neutron.NeutronNetwork;
import org.opendaylight.controller.sal.action.ActionType;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.utils.EtherTypes;
import org.opendaylight.controller.sal.utils.HexEncode;
import org.opendaylight.controller.sal.utils.ServiceHelper;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.controller.sal.utils.StatusCode;
import org.opendaylight.ovsdb.lib.notation.OvsDBMap;
import org.opendaylight.ovsdb.lib.notation.OvsDBSet;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.table.Bridge;
import org.opendaylight.ovsdb.lib.table.Interface;
import org.opendaylight.ovsdb.lib.table.Port;
import org.opendaylight.ovsdb.lib.table.internal.Table;
import org.opendaylight.ovsdb.plugin.IConnectionServiceInternal;
import org.opendaylight.ovsdb.plugin.OVSDBConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * OpenStack Neutron with the OpenVswitch data plan relies on a typical OVS bridge configurations that
 * consists of br-int (Integration Bridge), br-tun (Tunnel bridge), br-ex (External bridge).
 *
 * In DevStack like setups, the br-tun is not automatically created on the controller nodes.
 * Hence this class attempts to bring all the nodes to be elibible for OpenStack operations.
 *
 */
public class InternalNetworkManager {
    static final Logger logger = LoggerFactory.getLogger(InternalNetworkManager.class);

    private static InternalNetworkManager internalNetwork = new InternalNetworkManager();
    private InternalNetworkManager() {
    }

    public static InternalNetworkManager getManager() {
        return internalNetwork;
    }

    public String getInternalBridgeUUID (Node node, String bridgeName) {
        try {
            OVSDBConfigService ovsdbTable = (OVSDBConfigService)ServiceHelper.getGlobalInstance(OVSDBConfigService.class, this);
            Map<String, Table<?>> bridgeTable = ovsdbTable.getRows(node, Bridge.NAME.getName());
            if (bridgeTable == null) return null;
            for (String key : bridgeTable.keySet()) {
                Bridge bridge = (Bridge)bridgeTable.get(key);
                if (bridge.getName().equals(bridgeName)) return key;
            }
        } catch (Exception e) {
            logger.error("Error getting Bridge Identifier for {} / {}", node, bridgeName, e);
        }
        return null;
    }

    public boolean isInternalNetworkNeutronReady(Node node) throws Exception {
        if (this.getInternalBridgeUUID(node, AdminConfigManager.getManager().getIntegrationBridgeName()) != null) {
            return true;
        } else {
            return false;
        }
    }

    public boolean isInternalNetworkOverlayReady(Node node) throws Exception {
        if (this.getInternalBridgeUUID(node, AdminConfigManager.getManager().getTunnelBridgeName()) != null) {
            return true;
        } else {
            return false;
        }
    }

    public Status createInternalNetworkForOverlay(Node node) throws Exception {
        if (!isInternalNetworkNeutronReady(node)) {
            logger.error("Integration Bridge is not available in Node {}", node);
            return new Status(StatusCode.NOTACCEPTABLE, "Integration Bridge is not avaialble in Node " + node);
        }
        if (isInternalNetworkOverlayReady(node)) {
            logger.error("Network Overlay Bridge is already present in Node {}", node);
            return new Status(StatusCode.NOTACCEPTABLE, "Network Overlay Bridge is already present in Node " + node);
        }

        /*
         * Lets create this :
         *
         * Bridge br-tun
                Port patch-int
                    Interface patch-int
                        type: patch
                        options: {peer=patch-tun}
                Port br-tun
                    Interface br-tun
                        type: internal
         */

        OVSDBConfigService ovsdbTable = (OVSDBConfigService)ServiceHelper.getGlobalInstance(OVSDBConfigService.class, this);
        Bridge brTun = new Bridge();
        brTun.setName(AdminConfigManager.getManager().getTunnelBridgeName());
        // Create br-tun bridge
        Status status = ovsdbTable.insertRow(node, Bridge.NAME.getName(), null, brTun);
        if (!status.isSuccess()) return status;
        String bridgeUUID = status.getDescription();
        // Set OF Controller
        IConnectionServiceInternal connectionService = (IConnectionServiceInternal)ServiceHelper.getGlobalInstance(IConnectionServiceInternal.class, this);
        connectionService.setOFController(node, bridgeUUID);

        Port port = new Port();
        port.setName(brTun.getName());
        status = ovsdbTable.insertRow(node, Port.NAME.getName(), bridgeUUID, port);

        String patchInt = AdminConfigManager.getManager().getPatchToIntegration();
        String patchTun = AdminConfigManager.getManager().getPatchToTunnel();

        status = addPatchPort(node, bridgeUUID, patchInt, patchTun);
        if (!status.isSuccess()) return status;

        // Create the corresponding patch-tun port in br-int
        Map<String, Table<?>> bridges = ovsdbTable.getRows(node, Bridge.NAME.getName());
        for (String brIntUUID : bridges.keySet()) {
            Bridge brInt = (Bridge) bridges.get(brIntUUID);
            if (brInt.getName().equalsIgnoreCase(AdminConfigManager.getManager().getIntegrationBridgeName())) {
                return addPatchPort(node, brIntUUID, patchTun, patchInt);
            }
        }

        return status;
    }

    private Status addPatchPort (Node node, String bridgeUUID, String portName, String patchName) throws Exception {
        Status status = null;
        OVSDBConfigService ovsdbTable = (OVSDBConfigService)ServiceHelper.getGlobalInstance(OVSDBConfigService.class, this);

        Port patchPort = new Port();
        patchPort.setName(portName);
        // Create patch-int port and interface
        status = ovsdbTable.insertRow(node, Port.NAME.getName(), bridgeUUID, patchPort);
        if (!status.isSuccess()) return status;

        String patchPortUUID = status.getDescription();

        String interfaceUUID = null;
        int timeout = 6;
        while ((interfaceUUID == null) && (timeout > 0)) {
            patchPort = (Port)ovsdbTable.getRow(node, Port.NAME.getName(), patchPortUUID);
            OvsDBSet<UUID> interfaces = patchPort.getInterfaces();
            if (interfaces == null || interfaces.size() == 0) {
                // Wait for the OVSDB update to sync up the Local cache.
                Thread.sleep(500);
                timeout--;
                continue;
            }
            interfaceUUID = interfaces.toArray()[0].toString();
        }

        if (interfaceUUID == null) {
            return new Status(StatusCode.INTERNALERROR);
        }

        Interface tunInterface = new Interface();
        tunInterface.setType("patch");
        OvsDBMap<String, String> options = new OvsDBMap<String, String>();
        options.put("peer", patchName);
        tunInterface.setOptions(options);
        status = ovsdbTable.updateRow(node, Interface.NAME.getName(), patchPortUUID, interfaceUUID, tunInterface);

        return status;
    }

    private void prepareInternalNetwork (NeutronNetwork network, Node node) {
        // vlan, vxlan, and gre
        if (network.getProviderNetworkType().equalsIgnoreCase("gre") ||
                network.getProviderNetworkType().equalsIgnoreCase("vxlan") ||
                network.getProviderNetworkType().equalsIgnoreCase("vlan")) {

            try {
                if (!this.isInternalNetworkOverlayReady(node)) {
                    this.createInternalNetworkForOverlay(node);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            try {
                if (!this.isInternalNetworkNeutronReady(node)) {
                    // TODO : FILL IN
                    // this.createInternalNetworkForNeutron(node);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        this.initializeOFNormalFlowRules(node, AdminConfigManager.getManager().getIntegrationBridgeName());
        this.initializeLLDPFlowRules(node, AdminConfigManager.getManager().getTunnelBridgeName());
        this.initializeLLDPFlowRules(node, AdminConfigManager.getManager().getIntegrationBridgeName());
    }

    private void initializeOFNormalFlowRules(Node node, String bridgeName) {
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
            IForwardingRulesManager frm = (IForwardingRulesManager) ServiceHelper.getInstance(
                    IForwardingRulesManager.class, "default", this);
            String flowName = ActionType.HW_PATH.toString();
            if (frm.getStaticFlow(flowName, ofNode) != null) {
                logger.debug("Static Flow {} already programmed in the node {}", flowName, ofNode);
                return;
            }
            FlowConfig flow = new FlowConfig();
            flow.setName("IntegrationBridgeNormal");
            flow.setNode(ofNode);
            flow.setPriority("1");
            List<String> normalAction = new ArrayList<String>();
            normalAction.add(flowName);
            flow.setActions(normalAction);
            Status status = frm.addStaticFlow(flow);
            logger.debug("Flow Programming Status {} for Flow {} on {} / {}", status, flow, ofNode, node);
        } catch (Exception e) {
            logger.error("Failed to initialize Flow Rules for {}", node, e);
        }
    }

    private void initializeLLDPFlowRules(Node node, String bridgeName) {
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
            IForwardingRulesManager frm = (IForwardingRulesManager) ServiceHelper.getInstance(
                    IForwardingRulesManager.class, "default", this);
            String flowName = "PuntLLDP";
            if (frm.getStaticFlow(flowName, ofNode) != null) {
                logger.debug("Static Flow {} already programmed in the node {}", flowName, ofNode);
                return;
            }

            List<String> puntAction = new ArrayList<String>();
            puntAction.add(ActionType.CONTROLLER.toString());

            FlowConfig allowLLDP = new FlowConfig();
            allowLLDP.setInstallInHw(true);
            allowLLDP.setName(flowName);
            allowLLDP.setPriority("10");
            allowLLDP.setNode(ofNode);
            allowLLDP.setEtherType("0x" + Integer.toHexString(EtherTypes.LLDP.intValue())
                    .toUpperCase());
            allowLLDP.setActions(puntAction);
            Status status = frm.addStaticFlow(allowLLDP);
            logger.debug("Flow Programming Status {} for Flow {} on {} / {}", status, allowLLDP, ofNode, node);
        } catch (Exception e) {
            logger.error("Failed to initialize Flow Rules for {}", node, e);
        }
    }

    public void prepareInternalNetwork(NeutronNetwork network) {
        IConnectionServiceInternal connectionService = (IConnectionServiceInternal)ServiceHelper.getGlobalInstance(IConnectionServiceInternal.class, this);
        List<Node> nodes = connectionService.getNodes();
        for (Node node : nodes) {
            prepareInternalNetwork(network, node);
        }
    }

    public void prepareInternalNetwork(Node node) {
        INeutronNetworkCRUD neutronNetworkService = (INeutronNetworkCRUD)ServiceHelper.getGlobalInstance(INeutronNetworkCRUD.class, this);
        List <NeutronNetwork> networks = neutronNetworkService.getAllNetworks();
        for (NeutronNetwork network : networks) {
            prepareInternalNetwork(network, node);
        }
    }

    public static List safe( List other ) {
        return other == null ? Collections.EMPTY_LIST : other;
    }
}
