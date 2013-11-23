package org.opendaylight.ovsdb.neutron;

import java.util.List;
import java.util.Map;

import org.opendaylight.controller.networkconfig.neutron.INeutronNetworkCRUD;
import org.opendaylight.controller.networkconfig.neutron.NeutronNetwork;
import org.opendaylight.controller.sal.core.Node;
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

    public boolean isInternalNetworkNeutronReady(Node node) throws Exception {
        OVSDBConfigService ovsdbTable = (OVSDBConfigService)ServiceHelper.getGlobalInstance(OVSDBConfigService.class, this);
        Map<String, Table<?>> bridgeTable = ovsdbTable.getRows(node, Bridge.NAME.getName());
        if (bridgeTable != null) {
            for (Table<?> row : bridgeTable.values()) {
                Bridge bridge = (Bridge)row;
                if (bridge.getName().equals(AdminConfigManager.getManager().getIntegrationBridgeName())) return true;
            }
        }
        return false;
    }

    public boolean isInternalNetworkOverlayReady(Node node) throws Exception {
        OVSDBConfigService ovsdbTable = (OVSDBConfigService)ServiceHelper.getGlobalInstance(OVSDBConfigService.class, this);
        Map<String, Table<?>> bridgeTable = ovsdbTable.getRows(node, Bridge.NAME.getName());
        if (bridgeTable != null) {
            for (Table<?> row : bridgeTable.values()) {
                Bridge bridge = (Bridge)row;
                if (bridge.getName().equals(AdminConfigManager.getManager().getTunnelBridgeName())) return true;
            }
        }
        return false;
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

        Port port = new Port();
        port.setName(brTun.getName());
        status = ovsdbTable.insertRow(node, Port.NAME.getName(), bridgeUUID, port);

        status = addPatchPort(node, bridgeUUID, "patch-int", "patch-tun");
        if (!status.isSuccess()) return status;

        // Create the corresponding patch-tun port in br-int
        Map<String, Table<?>> bridges = ovsdbTable.getRows(node, Bridge.NAME.getName());
        for (String brIntUUID : bridges.keySet()) {
            Bridge brInt = (Bridge) bridges.get(brIntUUID);
            if (brInt.getName().equalsIgnoreCase(AdminConfigManager.getManager().getIntegrationBridgeName())) {
                return addPatchPort(node, brIntUUID, "patch-tun", "patch-int");
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
            patchPort = (Port)ovsdbTable.getRow(node, Port.NAME.getName(), status.getDescription());
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

}
