/*
 * Copyright (C) 2013 Red Hat, Inc. and others...
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Madhu Venugopal, Brent Salisbury, Dave Tucker
 */
package org.opendaylight.ovsdb.neutron;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

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
import org.opendaylight.ovsdb.lib.table.Open_vSwitch;
import org.opendaylight.ovsdb.lib.table.Port;
import org.opendaylight.ovsdb.lib.table.internal.Table;
import org.opendaylight.ovsdb.neutron.provider.ProviderNetworkManager;
import org.opendaylight.ovsdb.plugin.IConnectionServiceInternal;
import org.opendaylight.ovsdb.plugin.OVSDBConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TenantNetworkManager {
    static final Logger logger = LoggerFactory.getLogger(TenantNetworkManager.class);

    public static final String EXTERNAL_ID_VM_ID = "vm-id";
    public static final String EXTERNAL_ID_INTERFACE_ID = "iface-id";
    public static final String EXTERNAL_ID_VM_MAC = "attached-mac";
    private static TenantNetworkManager tenantHelper = new TenantNetworkManager();
    private ConcurrentMap<String, NodeConfiguration> nodeConfigurationCache = new ConcurrentHashMap<>();

    private boolean enableContainer = false;
    private TenantNetworkManager() {
        String isTenantContainer = System.getProperty("TenantIsContainer");
        if (isTenantContainer != null && isTenantContainer.equalsIgnoreCase("true")) {
            enableContainer =  true;
        }
    }

    public static TenantNetworkManager getManager() {
        return tenantHelper;
    }

    public int getInternalVlan(Node node, String networkId) {
        String nodeUuid = getNodeUUID(node);
        if (nodeUuid == null) {
            logger.error("Unable to get UUID for Node {}", node);
            return 0;
        }

        NodeConfiguration nodeConfiguration = nodeConfigurationCache.get(nodeUuid);

        if (nodeConfiguration == null) {
            nodeConfiguration = addNodeConfigurationToCache(node);
        }
        Integer vlan = nodeConfiguration.getInternalVlan(networkId);
        if (vlan == null) return 0;
        return vlan.intValue();
    }

    private NodeConfiguration addNodeConfigurationToCache(Node node) {
        NodeConfiguration nodeConfiguration = new NodeConfiguration(node);
        String nodeUuid = getNodeUUID(node);
        if (nodeUuid == null) {
            logger.error("Cannot get Node UUID for Node {}", node);
            return null;
        }
        this.nodeConfigurationCache.put(nodeUuid, nodeConfiguration);
        return nodeConfigurationCache.get(nodeUuid);
    }

    public void networkCreated (String networkId) {
        IConnectionServiceInternal connectionService = (IConnectionServiceInternal)ServiceHelper.getGlobalInstance(IConnectionServiceInternal.class, this);
        List<Node> nodes = connectionService.getNodes();

        for (Node node : nodes) {
            this.networkCreated(node, networkId);
        }

    }

    private String getNodeUUID(Node node) {
        String nodeUuid = new String();
        OVSDBConfigService ovsdbConfigService = (OVSDBConfigService)ServiceHelper.getGlobalInstance(OVSDBConfigService.class, this);
        try {
            Map<String, Table<?>> ovsTable = ovsdbConfigService.getRows(node, Open_vSwitch.NAME.getName());
            nodeUuid = (String)ovsTable.keySet().toArray()[0];
        }
        catch (Exception e) {
            logger.error("Unable to get the Open_vSwitch table for Node {}: {}", node, e);
        }

        return nodeUuid;
    }

    public int networkCreated (Node node, String networkId) {
        String nodeUuid = getNodeUUID(node);
        if (nodeUuid == null) {
            logger.error("Unable to get UUID for Node {}", node);
            return 0;
        }

        NodeConfiguration nodeConfiguration = nodeConfigurationCache.get(nodeUuid);

        // Cache miss
        if (nodeConfiguration == null)
        {
            nodeConfiguration = addNodeConfigurationToCache(node);
        }

        int internalVlan = nodeConfiguration.assignInternalVlan(networkId);
        if (enableContainer && internalVlan != 0) {
            IContainerManager containerManager = (IContainerManager)ServiceHelper.getGlobalInstance(IContainerManager.class, this);
            if (containerManager == null) {
                logger.error("ContainerManager is null. Failed to create Container for {}", networkId);
                return 0;
            }

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
        if (ProviderNetworkManager.getManager().hasPerTenantTunneling()) {
            String nodeUuid = getNodeUUID(node);
            if (nodeUuid == null) {
                logger.debug("Unable to get UUID for Node {}", node);
                return false;
            }

            NodeConfiguration nodeConfiguration = nodeConfigurationCache.get(nodeUuid);

            // Cache miss
            if (nodeConfiguration == null)
            {
                logger.error("Configuration data unavailable for Node {} ", node);
                return false;
            }

            int internalVlan = nodeConfiguration.getInternalVlan(networkId);
            if (internalVlan == 0) {
                logger.debug("No InternalVlan provisioned for Tenant Network {}",networkId);
                return false;
            }
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
                if (externalIds != null && externalIds.get(EXTERNAL_ID_INTERFACE_ID) != null) {
                    if (this.isInterfacePresentInTenantNetwork(externalIds.get(EXTERNAL_ID_INTERFACE_ID), networkId)) {
                        logger.debug("Tenant Network {} with Segmentation-id {} is present in Node {} / Interface {}",
                                      networkId, segmentationId, node, intf);
                        return true;
                    }
                }
            }

        } catch (Exception e) {
            logger.error("Error while trying to determine if network is present on node", e);
            return false;
        }

        logger.debug("Tenant Network {} with Segmenation-id {} is NOT present in Node {}",
                networkId, segmentationId, node);

        return false;
    }

    public String getNetworkIdForSegmentationId (String segmentationId) {
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
        if (neutronPort != null && neutronPort.getNetworkUUID().equalsIgnoreCase(networkId)) return true;
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

        String nodeUuid = getNodeUUID(node);
        if (nodeUuid == null) {
            logger.error("Unable to get UUID for Node {}", node);
            return;
        }

        NodeConfiguration nodeConfiguration = nodeConfigurationCache.get(nodeUuid);

        // Cache miss
        if (nodeConfiguration == null)
        {
            logger.error("Configuration data unavailable for Node {} ", node);
            return;
        }

        int vlan = nodeConfiguration.getInternalVlan(network.getID());
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
        if (enableContainer) this.addPortToTenantNetworkContainer(node, portUUID, network);
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
            logger.error("Exception in addPortToTenantNetworkContainer", e);
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

    public void networkDeleted(String id) {
        if (!enableContainer) return;

        IContainerManager containerManager = (IContainerManager)ServiceHelper.getGlobalInstance(IContainerManager.class, this);
        if (containerManager == null) {
            logger.error("ContainerManager is not accessible");
            return;
        }

        String networkID = BaseHandler.convertNeutronIDToKey(id);
        ContainerConfig config = new ContainerConfig();
        config.setContainer(networkID);
        containerManager.removeContainer(config);
    }

}
