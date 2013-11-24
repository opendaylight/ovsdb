package org.opendaylight.ovsdb.neutron;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.opendaylight.controller.containermanager.ContainerConfig;
import org.opendaylight.controller.containermanager.ContainerFlowConfig;
import org.opendaylight.controller.containermanager.IContainerManager;
import org.opendaylight.controller.networkconfig.neutron.INeutronNetworkCRUD;
import org.opendaylight.controller.networkconfig.neutron.INeutronPortCRUD;
import org.opendaylight.controller.networkconfig.neutron.NeutronNetwork;
import org.opendaylight.controller.networkconfig.neutron.NeutronPort;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.core.NodeConnector;
import org.opendaylight.controller.sal.utils.HexEncode;
import org.opendaylight.controller.sal.utils.ServiceHelper;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.ovsdb.lib.notation.OvsDBSet;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.table.Bridge;
import org.opendaylight.ovsdb.lib.table.Interface;
import org.opendaylight.ovsdb.lib.table.Port;
import org.opendaylight.ovsdb.lib.table.internal.Table;
import org.opendaylight.ovsdb.plugin.OVSDBConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TenantNetworkManager {
    static final Logger logger = LoggerFactory.getLogger(TenantNetworkManager.class);

    private static final int MAX_VLAN = 4096;
    private static final String EXTERNAL_ID_VM_ID = "vm-id";
    private static final String EXTERNAL_ID_INTERFACE_ID = "iface-id";
    private static final String EXTERNAL_ID_VM_MAC = "attached-mac";
    private static TenantNetworkManager tenantHelper = new TenantNetworkManager();
    private Queue<Integer> internalVlans = new LinkedList<Integer>();
    private Map<String, Integer> tenantVlanMap = new HashMap<String, Integer>();
    private TenantNetworkManager() {
        for (int i = 1; i < MAX_VLAN ; i++) {
            internalVlans.add(i);
        }
    }

    public static TenantNetworkManager getManager() {
        return tenantHelper;
    }

    private int assignInternalVlan (String networkId) {
        Integer mappedVlan = tenantVlanMap.get(networkId);
        if (mappedVlan != null) return mappedVlan;
        mappedVlan = internalVlans.poll();
        if (mappedVlan != null) tenantVlanMap.put(networkId, mappedVlan);
        return mappedVlan;
    }

    public void internalVlanInUse (int vlan) {
        internalVlans.remove(vlan);
    }

    public int getInternalVlan (String networkId) {
        Integer vlan = tenantVlanMap.get(networkId);
        if (vlan == null) return 0;
        return vlan.intValue();
    }

    public int networkCreated (String networkId) {
        IContainerManager containerManager = (IContainerManager)ServiceHelper.getGlobalInstance(IContainerManager.class, this);
        if (containerManager == null) {
            logger.error("ContainerManager is null. Failed to create Container for {}", networkId);
            return 0;
        }
        int internalVlan = this.assignInternalVlan(networkId);
        if (internalVlan != 0) {
            ContainerConfig config = new ContainerConfig();
            config.setContainer(BaseHandler.convertNeutronIDToKey(networkId));
            Status status = containerManager.addContainer(config);
            logger.debug("Container Creation Status for {} : {}", networkId, status.toString());

            ContainerFlowConfig flowConfig = new ContainerFlowConfig("InternalVlan", internalVlan+"",
                    null, null, null, null, null);
            List<ContainerFlowConfig> containerFlowConfigs = new ArrayList<ContainerFlowConfig>();
            containerFlowConfigs.add(flowConfig);
            containerManager.addContainerFlows(BaseHandler.convertNeutronIDToKey(networkId), containerFlowConfigs);
        }
        return internalVlan;
    }

    /**
     * Are there any TenantNetwork VM present on this Node ?
     * This method uses Interface Table's external-id field to locate the VM.
     */
    public boolean isTenantNetworkPresentInNode(Node node, String segmentationId) {
        String networkId = this.getNetworkIdForSegmentationId(segmentationId);
        if (networkId == null) {
            logger.debug("Tenant Network not found with Segmenation-id {}",segmentationId);
            return false;
        }
        int internalVlan = this.getInternalVlan(networkId);
        if (internalVlan == 0) {
            logger.debug("No InternalVlan provisioned for Tenant Network {}",networkId);
            return false;
        }
        OVSDBConfigService ovsdbTable = (OVSDBConfigService)ServiceHelper.getGlobalInstance(OVSDBConfigService.class, this);
        try {
            /*
            // Vlan Tag based identification
            Map<String, Table<?>> portTable = ovsdbTable.getRows(node, Port.NAME.getName());
            if (portTable == null) {
                logger.debug("Port table is null for Node {} ", node);
                return false;
            }

            for (Table<?> row : portTable.values()) {
                Port port = (Port)row;
                Set<BigInteger> tags = port.getTag();
                if (tags.contains(internalVlan)) {
                    logger.debug("Tenant Network {} with Segmenation-id {} is present in Node {} / Port {}",
                                  networkId, segmentationId, node, port);
                    return true;
                }
            }
             */
            // External-id based more accurate VM Location identification
            Map<String, Table<?>> ifTable = ovsdbTable.getRows(node, Interface.NAME.getName());
            if (ifTable == null) {
                logger.debug("Interface table is null for Node {} ", node);
                return false;
            }

            for (Table<?> row : ifTable.values()) {
                Interface intf = (Interface)row;
                Map<String, String> externalIds = intf.getExternal_ids();
                if (externalIds != null) {
                    if (this.isInterfacePresentInTenantNetwork(externalIds.get(EXTERNAL_ID_INTERFACE_ID), networkId)) {
                        logger.debug("Tenant Network {} with Segmenation-id {} is present in Node {} / Interface {}",
                                      networkId, segmentationId, node, intf);
                        return true;
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        logger.debug("Tenant Network {} with Segmenation-id {} is NOT present in Node {}",
                networkId, segmentationId, node);

        return false;
    }

    private String getNetworkIdForSegmentationId (String segmentationId) {
        INeutronNetworkCRUD neutronNetworkService = (INeutronNetworkCRUD)ServiceHelper.getGlobalInstance(INeutronNetworkCRUD.class, this);
        List <NeutronNetwork> networks = neutronNetworkService.getAllNetworks();
        for (NeutronNetwork network : networks) {
            if (network.getProviderSegmentationID().equalsIgnoreCase(segmentationId)) return network.getNetworkUUID();
        }
        return null;
    }

    private boolean isInterfacePresentInTenantNetwork (String portId, String networkId) {
        INeutronPortCRUD neutronPortService = (INeutronPortCRUD)ServiceHelper.getGlobalInstance(INeutronPortCRUD.class, this);
        NeutronPort neutronPort = neutronPortService.getPort(portId);
        if (neutronPort.getNetworkUUID().equalsIgnoreCase(networkId)) return true;
        return false;
    }

    public NeutronNetwork getTenantNetworkForInterface (Interface intf) {
        logger.trace("getTenantNetworkForInterface for {}", intf);
        if (intf == null) return null;
        Map<String, String> externalIds = intf.getExternal_ids();
        logger.trace("externalIds {}", externalIds);
        if (externalIds == null) return null;
        String neutronPortId = externalIds.get(EXTERNAL_ID_INTERFACE_ID);
        if (neutronPortId == null) return null;
        INeutronPortCRUD neutronPortService = (INeutronPortCRUD)ServiceHelper.getGlobalInstance(INeutronPortCRUD.class, this);
        NeutronPort neutronPort = neutronPortService.getPort(neutronPortId);
        logger.trace("neutronPort {}", neutronPort);
        if (neutronPort == null) return null;
        INeutronNetworkCRUD neutronNetworkService = (INeutronNetworkCRUD)ServiceHelper.getGlobalInstance(INeutronNetworkCRUD.class, this);
        NeutronNetwork neutronNetwork = neutronNetworkService.getNetwork(neutronPort.getNetworkUUID());
        logger.debug("{} mappped to {}", intf, neutronNetwork);
        return neutronNetwork;
    }

    public void programTenantNetworkInternalVlan(Node node, String portUUID, NeutronNetwork network) {
        int vlan = this.getInternalVlan(network.getID());
        logger.debug("Programming Vlan {} on {}", vlan, portUUID);
        if (vlan <= 0) {
            logger.error("Unable to get an internalVlan for Network {}", network);
            return;
        }
        OVSDBConfigService ovsdbTable = (OVSDBConfigService)ServiceHelper.getGlobalInstance(OVSDBConfigService.class, this);
        Port port = new Port();
        OvsDBSet<BigInteger> tags = new OvsDBSet<BigInteger>();
        tags.add(BigInteger.valueOf(vlan));
        port.setTag(tags);
        ovsdbTable.updateRow(node, Port.NAME.getName(), null, portUUID, port);
        this.addPortToTenantNetworkContainer(node, portUUID, network);
    }

    private void addPortToTenantNetworkContainer(Node node, String portUUID, NeutronNetwork network) {
        IContainerManager containerManager = (IContainerManager)ServiceHelper.getGlobalInstance(IContainerManager.class, this);
        if (containerManager == null) {
            logger.error("ContainerManager is not accessible");
            return;
        }
        OVSDBConfigService ovsdbTable = (OVSDBConfigService)ServiceHelper.getGlobalInstance(OVSDBConfigService.class, this);
        try {
            Port port = (Port)ovsdbTable.getRow(node, Port.NAME.getName(), portUUID);
            if (port == null) {
                logger.trace("Unable to identify Port with UUID {}", portUUID);
                return;
            }
            Set<UUID> interfaces = port.getInterfaces();
            if (interfaces == null) {
                logger.trace("No interfaces available to fetch the OF Port");
                return;
            }
            Bridge bridge = this.getBridgeIdForPort(node, portUUID);
            if (bridge == null) {
                logger.debug("Unable to spot Bridge for Port {} in node {}", port, node);
                return;
            }
            Set<String> dpids = bridge.getDatapath_id();
            if (dpids == null || dpids.size() ==  0) return;
            Long dpidLong = Long.valueOf(HexEncode.stringToLong((String)dpids.toArray()[0]));

            for (UUID intfUUID : interfaces) {
                Interface intf = (Interface)ovsdbTable.getRow(node, Interface.NAME.getName(), intfUUID.toString());
                if (intf == null) continue;
                Set<BigInteger> of_ports = intf.getOfport();
                if (of_ports == null) continue;
                for (BigInteger of_port : of_ports) {
                    ContainerConfig config = new ContainerConfig();
                    config.setContainer(BaseHandler.convertNeutronIDToKey(network.getID()));
                    logger.debug("Adding Port {} to Container : {}", port.toString(), config.getContainer());
                    List<String> ncList = new ArrayList<String>();
                    Node ofNode = new Node(Node.NodeIDType.OPENFLOW, dpidLong);
                    NodeConnector nc = NodeConnector.fromStringNoNode(Node.NodeIDType.OPENFLOW.toString(),
                                                                      Long.valueOf(of_port.longValue()).intValue()+"",
                                                                      ofNode);
                    ncList.add(nc.toString());
                    config.addNodeConnectors(ncList);

                    Status status = containerManager.addContainerEntry(BaseHandler.convertNeutronIDToKey(network.getID()), ncList);

                    if (!status.isSuccess()) {
                        logger.error(" Failed {} : to add port {} to container - {}",
                                status, nc, network.getID());
                    } else {
                        logger.error(" Successfully added port {} to container - {}",
                                       nc, network.getID());
                    }
                }
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private Bridge getBridgeIdForPort (Node node, String uuid) {
        OVSDBConfigService ovsdbTable = (OVSDBConfigService)ServiceHelper.getGlobalInstance(OVSDBConfigService.class, this);
        try {
            Map<String, Table<?>> bridges = ovsdbTable.getRows(node, Bridge.NAME.getName());
            if (bridges == null) return null;
            for (String bridgeUUID : bridges.keySet()) {
                Bridge bridge = (Bridge)bridges.get(bridgeUUID);
                Set<UUID> portUUIDs = bridge.getPorts();
                logger.trace("Scanning Bridge {} to identify Port : {} ",bridge, uuid);
                for (UUID portUUID : portUUIDs) {
                    if (portUUID.toString().equalsIgnoreCase(uuid)) {
                        logger.trace("Found Port {} -> ", uuid, bridgeUUID);
                        return bridge;
                    }
                }
            }
        } catch (Exception e) {
            logger.debug("Failed to get BridgeId port {} in Node {}", uuid, node);
        }
        return null;
    }

}
