package org.opendaylight.ovsdb.neutron;

import java.util.ArrayList;
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
import org.opendaylight.ovsdb.lib.notation.OvsDBMap;
import org.opendaylight.ovsdb.lib.notation.OvsDBSet;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.table.Bridge;
import org.opendaylight.ovsdb.lib.table.Interface;
import org.opendaylight.ovsdb.lib.table.Port;
import org.opendaylight.ovsdb.lib.table.internal.Table;
import org.opendaylight.ovsdb.plugin.IConnectionServiceInternal;
import org.opendaylight.ovsdb.plugin.OVSDBConfigService;
import org.opendaylight.ovsdb.plugin.StatusWithUuid;
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
    private static final int LLDP_PRIORITY = 1000;
    private static final int NORMAL_PRIORITY = 0;

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

    public boolean isInternalNetworkNeutronReady(Node node) {
        if (this.getInternalBridgeUUID(node, AdminConfigManager.getManager().getIntegrationBridgeName()) != null) {
            return true;
        } else {
            return false;
        }
    }

    public boolean isInternalNetworkOverlayReady(Node node) {
        if (!this.isInternalNetworkNeutronReady(node)) {
            return false;
        }
        if (this.getInternalBridgeUUID(node, AdminConfigManager.getManager().getTunnelBridgeName()) != null) {
            return true;
        } else {
            return false;
        }
    }

    /*
     * Lets create these if not already present :
     *
       Bridge br-int
            Port patch-tun
                Interface patch-tun
                    type: patch
                    options: {peer=patch-int}
            Port br-int
                Interface br-int
                    type: internal
      Bridge br-tun
            Port patch-int
                Interface patch-int
                    type: patch
                    options: {peer=patch-tun}
            Port br-tun
                Interface br-tun
                    type: internal
     */
    public void createInternalNetworkForOverlay(Node node) throws Exception {
        String brTun = AdminConfigManager.getManager().getTunnelBridgeName();
        String brInt = AdminConfigManager.getManager().getIntegrationBridgeName();
        String patchInt = AdminConfigManager.getManager().getPatchToIntegration();
        String patchTun = AdminConfigManager.getManager().getPatchToTunnel();

        Status status = this.addInternalBridge(node, brInt, patchTun, patchInt);
        if (!status.isSuccess()) logger.debug("Integration Bridge Creation Status : "+status.toString());
        status = this.addInternalBridge(node, brTun, patchInt, patchTun);
        if (!status.isSuccess()) logger.debug("Tunnel Bridge Creation Status : "+status.toString());
    }

    /*
     * Lets create these if not already present :
     *
       Bridge br-int
            Port br-int
                Interface br-int
                    type: internal
     */
    public void createInternalNetworkForNeutron(Node node) throws Exception {
        String brInt = AdminConfigManager.getManager().getIntegrationBridgeName();

        Status status = this.addInternalBridge(node, brInt, null, null);
        if (!status.isSuccess()) logger.debug("Integration Bridge Creation Status : "+status.toString());
    }

    private Status addInternalBridge (Node node, String bridgeName, String localPathName, String remotePatchName) throws Exception {
        OVSDBConfigService ovsdbTable = (OVSDBConfigService)ServiceHelper.getGlobalInstance(OVSDBConfigService.class, this);

        String bridgeUUID = this.getInternalBridgeUUID(node, bridgeName);
        if (bridgeUUID == null) {
            Bridge bridge = new Bridge();
            bridge.setName(bridgeName);

            StatusWithUuid statusWithUuid = ovsdbTable.insertRow(node, Bridge.NAME.getName(), null, bridge);
            if (!statusWithUuid.isSuccess()) return statusWithUuid;
            bridgeUUID = statusWithUuid.getUuid().toString();
            Port port = new Port();
            port.setName(bridgeName);
            ovsdbTable.insertRow(node, Port.NAME.getName(), bridgeUUID, port);
        }

        IConnectionServiceInternal connectionService = (IConnectionServiceInternal)ServiceHelper.getGlobalInstance(IConnectionServiceInternal.class, this);
        connectionService.setOFController(node, bridgeUUID);

        if (localPathName != null && remotePatchName != null) {
            return addPatchPort(node, bridgeUUID, localPathName, remotePatchName);
        }
        return new Status(StatusCode.SUCCESS);
    }

    private Status addPatchPort (Node node, String bridgeUUID, String portName, String patchName) throws Exception {
        OVSDBConfigService ovsdbTable = (OVSDBConfigService)ServiceHelper.getGlobalInstance(OVSDBConfigService.class, this);

        Port patchPort = new Port();
        patchPort.setName(portName);
        // Create patch port and interface
        StatusWithUuid statusWithUuid = ovsdbTable.insertRow(node, Port.NAME.getName(), bridgeUUID, patchPort);
        if (!statusWithUuid.isSuccess()) return statusWithUuid;

        String patchPortUUID = statusWithUuid.getUuid().toString();

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
        return ovsdbTable.updateRow(node, Interface.NAME.getName(), patchPortUUID, interfaceUUID, tunInterface);
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
            FlowConfig flow = new FlowConfig();
            flow.setName("NORMAL");
            flow.setNode(ofNode);
            flow.setPriority(NORMAL_PRIORITY+"");
            flow.setInstallInHw(true);
            List<String> normalAction = new ArrayList<String>();
            normalAction.add(flowName);
            flow.setActions(normalAction);
            Status status = null;
            if (frm.getStaticFlow(flowName, ofNode) == null) {
                status = frm.addStaticFlow(flow);
                logger.debug("Flow Programming Add Status {} for Flow {} on {} / {}", status, flow, ofNode, node);
            } else {
                status = frm.modifyStaticFlow(flow);
                logger.debug("Flow Programming Modify Status {} for Flow {} on {} / {}", status, flow, ofNode, node);
            }
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
            List<String> puntAction = new ArrayList<String>();
            puntAction.add(ActionType.CONTROLLER.toString());

            FlowConfig allowLLDP = new FlowConfig();
            allowLLDP.setInstallInHw(true);
            allowLLDP.setName(flowName);
            allowLLDP.setPriority(LLDP_PRIORITY+"");
            allowLLDP.setNode(ofNode);
            allowLLDP.setInstallInHw(true);
            allowLLDP.setEtherType("0x" + Integer.toHexString(EtherTypes.LLDP.intValue())
                    .toUpperCase());
            allowLLDP.setActions(puntAction);
            Status status = null;
            if (frm.getStaticFlow(flowName, ofNode) == null) {
                status = frm.addStaticFlow(allowLLDP);
                logger.debug("LLDP Flow Add Status {} for Flow {} on {} / {}", status, allowLLDP, ofNode, node);
            } else {
                status = frm.modifyStaticFlow(allowLLDP);
                logger.debug("LLDP Flow Modify Status {} for Flow {} on {} / {}", status, allowLLDP, ofNode, node);
            }
        } catch (Exception e) {
            logger.error("Failed to initialize Flow Rules for {}", node, e);
        }
    }

    public void prepareInternalNetwork(Node node) {
        try {
            this.createInternalNetworkForOverlay(node);
        } catch (Exception e) {
            logger.error("Error creating internal network "+node.toString(), e);
        }
        //Install NORMAL flows on all the bridges to make sure that we dont end up punting traffic to the OF Controller
        this.initializeOFNormalFlowRules(node, AdminConfigManager.getManager().getIntegrationBridgeName());
        this.initializeOFNormalFlowRules(node, AdminConfigManager.getManager().getExternalBridgeName());
        this.initializeOFNormalFlowRules(node, AdminConfigManager.getManager().getTunnelBridgeName());
        this.initializeLLDPFlowRules(node, AdminConfigManager.getManager().getTunnelBridgeName());
        this.initializeLLDPFlowRules(node, AdminConfigManager.getManager().getIntegrationBridgeName());
    }
}
