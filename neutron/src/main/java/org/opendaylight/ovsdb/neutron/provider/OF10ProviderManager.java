package org.opendaylight.ovsdb.neutron.provider;

import java.net.InetAddress;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import org.opendaylight.ovsdb.neutron.AdminConfigManager;
import org.opendaylight.ovsdb.neutron.InternalNetworkManager;
import org.opendaylight.ovsdb.neutron.TenantNetworkManager;
import org.opendaylight.ovsdb.plugin.IConnectionServiceInternal;
import org.opendaylight.ovsdb.plugin.OVSDBConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


class OF10ProviderManager extends ProviderNetworkManager {
    private static final Logger logger = LoggerFactory.getLogger(OF10ProviderManager.class);

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

        try {
            if (!InternalNetworkManager.getManager().isInternalNetworkOverlayReady(node)) {
                logger.error(node+" is not Overlay ready");
                return new Status(StatusCode.NOTACCEPTABLE, node+" is not Overlay ready");
            }
        } catch (Exception e) {
            logger.error(node+" is not Overlay ready");
            return new Status(StatusCode.NOTACCEPTABLE, node+" is not Overlay ready");
        }

        if (!TenantNetworkManager.getManager().isTenantNetworkPresentInNode(node, tunnelKey)) {
            logger.debug(node+" has no VM corresponding to segment "+ tunnelKey);
            return new Status(StatusCode.NOTACCEPTABLE, node+" has no VM corresponding to segment "+ tunnelKey);
        }
        return new Status(StatusCode.SUCCESS);
    }

    @Override
    public Status createTunnels(String tunnelType, String tunnelKey, Node srcNode) {
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
            addTunnelPort(srcNode, tunnelType, src, dst, tunnelKey);
            addTunnelPort(dstNode, tunnelType, dst, src, tunnelKey);
        }
        return new Status(StatusCode.SUCCESS);
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
            String portName = tunnelType+"-"+key+"-"+dst.getHostAddress();

            if (this.isTunnelPresent(node, portName, bridgeUUID)) {
                logger.trace("Tunnel {} is present in {} of {}", portName, tunnelBridgeName, node);
                return new Status(StatusCode.SUCCESS);
            }

            Port tunnelPort = new Port();
            tunnelPort.setName(portName);
            Status status = ovsdbTable.insertRow(node, Port.NAME.getName(), bridgeUUID, tunnelPort);
            if (!status.isSuccess()) {
                logger.error("Failed to insert Tunnel port {} in {}", portName, bridgeUUID);
                return status;
            }

            String tunnelPortUUID = status.getDescription();

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
            status = ovsdbTable.updateRow(node, Interface.NAME.getName(), tunnelPortUUID, interfaceUUID, tunInterface);
            logger.debug("Tunnel {} add status : {}", tunInterface, status);
            return status;
        } catch (Exception e) {
            e.printStackTrace();
            return new Status(StatusCode.INTERNALERROR);
        }
    }

    @Override
    public Status createTunnels(String tunnelType, String tunnelKey) {
        IConnectionServiceInternal connectionService = (IConnectionServiceInternal)ServiceHelper.getGlobalInstance(IConnectionServiceInternal.class, this);
        List<Node> nodes = connectionService.getNodes();
        for (Node srcNode : nodes) {
            this.createTunnels(tunnelType, tunnelKey, srcNode);
        }
        return null;
    }
}
