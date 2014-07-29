/**
 * Copyright (C) 2013 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Madhu Venugopal, Brent Salisbury, Dave Tucker
 */
package org.opendaylight.ovsdb.openstack.netvirt.providers;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.networkconfig.neutron.NeutronNetwork;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.utils.HexEncode;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.controller.sal.utils.StatusCode;
import org.opendaylight.ovsdb.lib.notation.Row;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.openstack.netvirt.api.Constants;
import org.opendaylight.ovsdb.openstack.netvirt.NetworkHandler;
import org.opendaylight.ovsdb.openstack.netvirt.api.BridgeConfigurationManager;
import org.opendaylight.ovsdb.openstack.netvirt.api.NetworkingProvider;
import org.opendaylight.ovsdb.openstack.netvirt.api.TenantNetworkManager;
import org.opendaylight.ovsdb.plugin.api.OvsdbConfigurationService;
import org.opendaylight.ovsdb.plugin.api.OvsdbConnectionService;
import org.opendaylight.ovsdb.openstack.netvirt.providers.mdsalopenflow13.OF13MdSalInstruction;
import org.opendaylight.ovsdb.openstack.netvirt.providers.mdsalopenflow13.OF13MdSalMatch;
import org.opendaylight.ovsdb.plugin.api.StatusWithUuid;
import org.opendaylight.ovsdb.schema.openvswitch.Bridge;
import org.opendaylight.ovsdb.schema.openvswitch.Interface;
import org.opendaylight.ovsdb.schema.openvswitch.Port;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.GroupActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.GroupActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.OutputActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.OutputActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.PopVlanActionCase;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.Instructions;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.EtherType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.VlanId;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.net.InetAddress;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;

/**
 * Open vSwitch OpenFlow 1.3 Networking Provider for OpenStack Neutron
 */
public class OF13Provider implements NetworkingProvider {
    private static final Logger logger = LoggerFactory.getLogger(OF13Provider.class);
    private DataBroker dataBroker;
    private static final short TABLE_0_DEFAULT_INGRESS = 0;
    private static final short TABLE_1_ISOLATE_TENANT = 10;
    private static final short TABLE_2_LOCAL_FORWARD = 20;
    private static final String OPENFLOW = "openflow:";
    private static Long groupId = 1L;

    private volatile org.opendaylight.ovsdb.openstack.netvirt.api.ConfigurationService configurationService;
    private volatile BridgeConfigurationManager bridgeConfigurationManager;
    private volatile TenantNetworkManager tenantNetworkManager;
    private volatile OvsdbConfigurationService ovsdbConfigurationService;
    private volatile OvsdbConnectionService connectionService;
    private volatile MdsalConsumer mdsalConsumer;

    public OF13Provider(){

    }

    @Override
    public boolean hasPerTenantTunneling() {
        return false;
    }

    private Status getTunnelReadinessStatus (Node node, String tunnelKey) {
        InetAddress srcTunnelEndPoint = configurationService.getTunnelEndPoint(node);
        if (srcTunnelEndPoint == null) {
            logger.error("Tunnel Endpoint not configured for Node {}", node);
            return new Status(StatusCode.NOTFOUND, "Tunnel Endpoint not configured for "+ node);
        }

        if (!bridgeConfigurationManager.isNodeNeutronReady(node)) {
            logger.error(node+" is not Overlay ready");
            return new Status(StatusCode.NOTACCEPTABLE, node+" is not Overlay ready");
        }

        if (!tenantNetworkManager.isTenantNetworkPresentInNode(node, tunnelKey)) {
            logger.debug(node+" has no VM corresponding to segment "+ tunnelKey);
            return new Status(StatusCode.NOTACCEPTABLE, node+" has no VM corresponding to segment "+ tunnelKey);
        }
        return new Status(StatusCode.SUCCESS);
    }

    private String getTunnelName(String tunnelType, InetAddress dst) {
        return tunnelType+"-"+dst.getHostAddress();
    }

    private boolean isTunnelPresent(Node node, String tunnelName, String bridgeUUID) throws Exception {
        Preconditions.checkNotNull(ovsdbConfigurationService);
        Row bridgeRow = ovsdbConfigurationService
                .getRow(node, ovsdbConfigurationService.getTableName(node, Bridge.class), bridgeUUID);
        Bridge bridge = ovsdbConfigurationService.getTypedRow(node, Bridge.class, bridgeRow);
        if (bridge != null) {
            Set<UUID> ports = bridge.getPortsColumn().getData();
            for (UUID portUUID : ports) {
                Row portRow = ovsdbConfigurationService
                        .getRow(node, ovsdbConfigurationService.getTableName(node, Port.class), portUUID.toString());
                Port port = ovsdbConfigurationService.getTypedRow(node, Port.class, portRow);
                if (port != null && tunnelName.equalsIgnoreCase(port.getName())) return true;
            }
        }
        return false;
    }

    private String getPortUuid(Node node, String name, String bridgeUUID) throws Exception {
        Preconditions.checkNotNull(ovsdbConfigurationService);
        Row bridgeRow = ovsdbConfigurationService
                .getRow(node, ovsdbConfigurationService.getTableName(node, Bridge.class), bridgeUUID);
        Bridge bridge = ovsdbConfigurationService.getTypedRow(node, Bridge.class, bridgeRow);
        if (bridge != null) {
            Set<UUID> ports = bridge.getPortsColumn().getData();
            for (UUID portUUID : ports) {
                Row portRow = ovsdbConfigurationService
                        .getRow(node, ovsdbConfigurationService.getTableName(node, Port.class), portUUID.toString());
                Port port = ovsdbConfigurationService.getTypedRow(node, Port.class, portRow);
                if (port != null && name.equalsIgnoreCase(port.getName())) return portUUID.toString();
            }
        }
        return null;
    }

    private Status addTunnelPort (Node node, String tunnelType, InetAddress src, InetAddress dst) {
        Preconditions.checkNotNull(ovsdbConfigurationService);
        try {
            String bridgeUUID = null;
            String tunnelBridgeName = configurationService.getIntegrationBridgeName();
            Map<String, Row> bridgeTable = ovsdbConfigurationService.getRows(node, ovsdbConfigurationService.getTableName(node, Bridge.class));
            if (bridgeTable != null) {
                for (String uuid : bridgeTable.keySet()) {
                    Bridge bridge = ovsdbConfigurationService.getTypedRow(node,Bridge.class, bridgeTable.get(uuid));
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
            String portName = getTunnelName(tunnelType, dst);

            if (this.isTunnelPresent(node, portName, bridgeUUID)) {
                logger.trace("Tunnel {} is present in {} of {}", portName, tunnelBridgeName, node);
                return new Status(StatusCode.SUCCESS);
            }

            Port tunnelPort = ovsdbConfigurationService.createTypedRow(node, Port.class);
            tunnelPort.setName(portName);
            StatusWithUuid statusWithUuid = ovsdbConfigurationService
                    .insertRow(node, ovsdbConfigurationService.getTableName(node, Port.class), bridgeUUID, tunnelPort.getRow());
            if (!statusWithUuid.isSuccess()) {
                logger.error("Failed to insert Tunnel port {} in {}", portName, bridgeUUID);
                return statusWithUuid;
            }

            String tunnelPortUUID = statusWithUuid.getUuid().toString();
            String interfaceUUID = null;
            int timeout = 6;
            while ((interfaceUUID == null) && (timeout > 0)) {
                Row portRow = ovsdbConfigurationService
                        .getRow(node, ovsdbConfigurationService.getTableName(node, Port.class), tunnelPortUUID);
                tunnelPort = ovsdbConfigurationService.getTypedRow(node, Port.class, portRow);
                Set<UUID> interfaces = tunnelPort.getInterfacesColumn().getData();
                if (interfaces == null || interfaces.size() == 0) {
                    // Wait for the OVSDB update to sync up the Local cache.
                    Thread.sleep(500);
                    timeout--;
                    continue;
                }
                interfaceUUID = interfaces.toArray()[0].toString();
                Row intfRow = ovsdbConfigurationService
                        .getRow(node, ovsdbConfigurationService.getTableName(node, Interface.class), interfaceUUID);
                Interface intf = ovsdbConfigurationService.getTypedRow(node, Interface.class, intfRow);
                if (intf == null) interfaceUUID = null;
            }

            if (interfaceUUID == null) {
                logger.error("Cannot identify Tunnel Interface for port {}/{}", portName, tunnelPortUUID);
                return new Status(StatusCode.INTERNALERROR);
            }

            Interface tunInterface = ovsdbConfigurationService.createTypedRow(node, Interface.class);
            tunInterface.setType(tunnelType);
            Map<String, String> options = Maps.newHashMap();
            options.put("key", "flow");
            options.put("local_ip", src.getHostAddress());
            options.put("remote_ip", dst.getHostAddress());
            tunInterface.setOptions(options);
            Status status = ovsdbConfigurationService
                    .updateRow(node, ovsdbConfigurationService.getTableName(node, Interface.class), tunnelPortUUID, interfaceUUID, tunInterface.getRow());
            logger.debug("Tunnel {} add status : {}", tunInterface, status);
            return status;
        } catch (Exception e) {
            logger.error("Exception in addTunnelPort", e);
            return new Status(StatusCode.INTERNALERROR);
        }
    }

    /* delete port from ovsdb port table */
    private Status deletePort(Node node, String bridgeName, String portName) {
        Preconditions.checkNotNull(ovsdbConfigurationService);
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
                   logger.error("Failed to delete port {} in {} status : {}", portName, bridgeUUID,
                                status);
                   return status;
               }
               logger.debug("Port {} delete status : {}", portName, status);
            }
            return status;
        } catch (Exception e) {
            logger.error("Exception in deletePort", e);
            return new Status(StatusCode.INTERNALERROR);
        }
    }

    private Status deleteTunnelPort(Node node, String tunnelType, InetAddress src, InetAddress dst) {
        String tunnelBridgeName = configurationService.getIntegrationBridgeName();
        String portName = getTunnelName(tunnelType, dst);
        return deletePort(node, tunnelBridgeName, portName);
    }

    private Status deletePhysicalPort(Node node, String phyIntfName) {
        String intBridgeName = configurationService.getIntegrationBridgeName();
        return deletePort(node, intBridgeName, phyIntfName);
    }

    private void programLocalBridgeRules(Node node, Long dpid, String segmentationId, String attachedMac, long localPort) {
         /*
         * Table(0) Rule #3
         * ----------------
         * Match: VM sMac and Local Ingress Port
         * Action:Action: Set Tunnel ID and GOTO Local Table (5)
         */

         handleLocalInPort(dpid, TABLE_0_DEFAULT_INGRESS, TABLE_1_ISOLATE_TENANT, segmentationId, localPort, attachedMac, true);

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
         * Match: VM sMac and Local Ingress Port
         * Action: Set VLAN ID and GOTO Local Table 1
         */

        handleLocalInPortSetVlan(dpid, TABLE_0_DEFAULT_INGRESS,
                TABLE_1_ISOLATE_TENANT, segmentationId, localPort,
                attachedMac, true);

        /*
         * Table(0) Rule #3
         * ----------------
         * Match: Drop any remaining Ingress Local VM Packets
         * Action: Drop w/ a low priority
         */

        handleDropSrcIface(dpid, localPort, true);

        /*
         * Table(2) Rule #1
         * ----------------
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

        handleLocalVlanBcastOut(dpid, TABLE_2_LOCAL_FORWARD, segmentationId,
                localPort, true);

         /*
          * Table(2) Rule #3
          * ----------------
          * Match: Any Remaining Flows w/a VLAN ID
          * Action: Drop w/ a low priority
          * Example: table=2,priority=8192,vlan_id=0x5 actions=drop
          */

          handleLocalVlanTableMiss(dpid, TABLE_2_LOCAL_FORWARD, segmentationId,
                                   true);
   }

    private void removeLocalVlanRules(Node node, Long dpid,
                                      String segmentationId, String attachedMac,
                                      long localPort) {
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

        handleLocalVlanBcastOut(dpid, TABLE_2_LOCAL_FORWARD, segmentationId,
                localPort, false);
   }

   private void programLocalIngressVlanRules(Node node, Long dpid, String segmentationId, String attachedMac, long ethPort) {
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
         */

        handleVlanFloodOut(dpid, TABLE_1_ISOLATE_TENANT, TABLE_2_LOCAL_FORWARD,
                           segmentationId, ethPort, true);
   }

   private void programRemoteEgressVlanRules(Node node, Long dpid, String segmentationId, String attachedMac, long ethPort) {
       /*
        * Table(1) Rule #1
        * ----------------
        * Match: Destination MAC is local VM MAC and vlan id
        * Action: go to table 2
        * -------------------------------------------
        * Example: table=1,vlan_id=0x5,dl_dst=00:00:00:00:00:08 \
        * actions=goto_table:2
        */

       handleVlanOut(dpid, TABLE_1_ISOLATE_TENANT, TABLE_2_LOCAL_FORWARD,
                     segmentationId, ethPort, attachedMac, true);

       /*
        * Table(1) Rule #3
        * ----------------
        * Match:  VLAN ID
        * Action: Go to table 2
        * -------------------------------------------
        * Example: table=1,priority=8192,vlan_id=0x5 actions=output:1,goto_table:2
        */

       handleVlanMiss(dpid, TABLE_1_ISOLATE_TENANT, TABLE_2_LOCAL_FORWARD,
                      segmentationId, ethPort, true);
   }

   private void removeRemoteEgressVlanRules(Node node, Long dpid, String segmentationId, String attachedMac, long ethPort) {
       /*
        * Table(1) Rule #1
        * ----------------
        * Match: Destination MAC is local VM MAC and vlan id
        * Action: go to table 2
        * -------------------------------------------
        * Example: table=1,vlan_id=0x5,dl_dst=00:00:00:00:00:08 \
        * actions=goto_table:2
        */

       handleVlanOut(dpid, TABLE_1_ISOLATE_TENANT, TABLE_2_LOCAL_FORWARD,
                     segmentationId, ethPort, attachedMac, false);
   }

   private void removePerVlanRules(Node node, Long dpid, String segmentationId, long ethPort) {
       /*
        * Table(2) Rule #3
        * ----------------
        * Match: Any Remaining Flows w/a VLAN ID
        * Action: Drop w/ a low priority
        * Example: table=2,priority=8192,vlan_id=0x5 actions=drop
        */

        handleLocalVlanTableMiss(dpid, TABLE_2_LOCAL_FORWARD, segmentationId,
                                 false);

        /*
         * Table(0) Rule #2
         * ----------------
         * Match: Ingress port = physical interface, Vlan ID
         * Action: GOTO Local Table 2
         */

        handleVlanIn(dpid, TABLE_0_DEFAULT_INGRESS, TABLE_2_LOCAL_FORWARD,
                     segmentationId, ethPort, false);

         /*
          * Table(1) Rule #2
          * ----------------
          * Match: Match VLAN ID and L2 ::::FF:FF Flooding
          * Action: Flood to local and remote VLAN members
          * -------------------------------------------
          * Example: table=1,priority=16384,vlan_id=0x5,dl_dst=ff:ff:ff:ff:ff:ff \
          * actions=output:10 (eth port),goto_table:2
          */

         handleVlanFloodOut(dpid, TABLE_1_ISOLATE_TENANT, TABLE_2_LOCAL_FORWARD,
                            segmentationId, ethPort, false);

         /*
          * Table(1) Rule #3
          * ----------------
          * Match:  VLAN ID
          * Action: Go to table 2
          * -------------------------------------------
          * Example: table=1,priority=8192,vlan_id=0x5 actions=output:1,goto_table:2
          */

         handleVlanMiss(dpid, TABLE_1_ISOLATE_TENANT, TABLE_2_LOCAL_FORWARD,
                        segmentationId, ethPort, false);
   }
    private Long getDpid (Node node, String bridgeUuid) {
        Preconditions.checkNotNull(ovsdbConfigurationService);
        try {
            Row bridgeRow =  ovsdbConfigurationService
                    .getRow(node, ovsdbConfigurationService.getTableName(node, Bridge.class), bridgeUuid);
            Bridge bridge = ovsdbConfigurationService.getTypedRow(node, Bridge.class, bridgeRow);
            Set<String> dpids = bridge.getDatapathIdColumn().getData();
            if (dpids == null || dpids.size() == 0) return 0L;
            return HexEncode.stringToLong((String) dpids.toArray()[0]);
        } catch (Exception e) {
            logger.error("Error finding Bridge's OF DPID", e);
            return 0L;
        }
    }

    private Long getIntegrationBridgeOFDPID (Node node) {
        try {
            String bridgeName = configurationService.getIntegrationBridgeName();
            String brIntId = this.getInternalBridgeUUID(node, bridgeName);
            if (brIntId == null) {
                logger.error("Unable to spot Bridge Identifier for {} in {}", bridgeName, node);
                return 0L;
            }

            return getDpid(node, brIntId);
        } catch (Exception e) {
            logger.error("Error finding Integration Bridge's OF DPID", e);
            return 0L;
        }
    }

    private Long getExternalBridgeDpid (Node node) {
        try {
            String bridgeName = configurationService.getExternalBridgeName();
            String brUuid = this.getInternalBridgeUUID(node, bridgeName);
            if (brUuid == null) {
                logger.error("Unable to spot Bridge Identifier for {} in {}", bridgeName, node);
                return 0L;
            }

            return getDpid(node, brUuid);
        } catch (Exception e) {
            logger.error("Error finding External Bridge's OF DPID", e);
            return 0L;
        }
    }

    private void programLocalRules (String networkType, String segmentationId, Node node, Interface intf) {
        try {
            Long dpid = this.getIntegrationBridgeOFDPID(node);
            if (dpid == 0L) {
                logger.debug("Openflow Datapath-ID not set for the integration bridge in {}", node);
                return;
            }

            Set<Long> of_ports = intf.getOpenFlowPortColumn().getData();
            if (of_ports == null || of_ports.size() <= 0) {
                logger.debug("Could NOT Identify OF value for port {} on {}", intf.getName(), node);
                return;
            }
            long localPort = (Long)of_ports.toArray()[0];

            Map<String, String> externalIds = intf.getExternalIdsColumn().getData();
            if (externalIds == null) {
                logger.error("No external_ids seen in {}", intf);
                return;
            }

            String attachedMac = externalIds.get(Constants.EXTERNAL_ID_VM_MAC);
            if (attachedMac == null) {
                logger.error("No AttachedMac seen in {}", intf);
                return;
            }

            /* Program local rules based on network type */
            if (networkType.equalsIgnoreCase(NetworkHandler.NETWORK_TYPE_VLAN)) {
                logger.debug("Program local vlan rules for interface {}", intf.getName());
                programLocalVlanRules(node, dpid, segmentationId, attachedMac, localPort);
            } else if (networkType.equalsIgnoreCase(NetworkHandler.NETWORK_TYPE_GRE) ||
                       networkType.equalsIgnoreCase(NetworkHandler.NETWORK_TYPE_VXLAN)) {
                logger.debug("Program local bridge rules for interface {}", intf.getName());
                programLocalBridgeRules(node, dpid, segmentationId, attachedMac, localPort);
            }
        } catch (Exception e) {
            logger.error("Exception in programming Local Rules for "+intf+" on "+node, e);
        }
    }

    private void removeLocalRules (String networkType, String segmentationId, Node node, Interface intf) {
        try {
            Long dpid = this.getIntegrationBridgeOFDPID(node);
            if (dpid == 0L) {
                logger.debug("Openflow Datapath-ID not set for the integration bridge in {}", node);
                return;
            }

            Set<Long> of_ports = intf.getOpenFlowPortColumn().getData();
            if (of_ports == null || of_ports.size() <= 0) {
                logger.debug("Could NOT Identify OF value for port {} on {}", intf.getName(), node);
                return;
            }
            long localPort = (Long)of_ports.toArray()[0];

            Map<String, String> externalIds = intf.getExternalIdsColumn().getData();
            if (externalIds == null) {
                logger.error("No external_ids seen in {}", intf);
                return;
            }

            String attachedMac = externalIds.get(Constants.EXTERNAL_ID_VM_MAC);
            if (attachedMac == null) {
                logger.error("No AttachedMac seen in {}", intf);
                return;
            }

            /* Program local rules based on network type */
            if (networkType.equalsIgnoreCase(NetworkHandler.NETWORK_TYPE_VLAN)) {
                logger.debug("Remove local vlan rules for interface {}", intf.getName());
                removeLocalVlanRules(node, dpid, segmentationId, attachedMac, localPort);
            } else if (networkType.equalsIgnoreCase(NetworkHandler.NETWORK_TYPE_GRE) ||
                       networkType.equalsIgnoreCase(NetworkHandler.NETWORK_TYPE_VXLAN)) {
                logger.debug("Remove local bridge rules for interface {}", intf.getName());
                removeLocalBridgeRules(node, dpid, segmentationId, attachedMac, localPort);
            }
        } catch (Exception e) {
            logger.error("Exception in removing Local Rules for "+intf+" on "+node, e);
        }
    }

    private void programTunnelRules (String tunnelType, String segmentationId, InetAddress dst, Node node,
            Interface intf, boolean local) {

        Preconditions.checkNotNull(ovsdbConfigurationService);

        try {

            Long dpid = this.getIntegrationBridgeOFDPID(node);
            if (dpid == 0L) {
                logger.debug("Openflow Datapath-ID not set for the integration bridge in {}", node);
                return;
            }

            Set<Long> of_ports = intf.getOpenFlowPortColumn().getData();
            if (of_ports == null || of_ports.size() <= 0) {
                logger.debug("Could NOT Identify OF value for port {} on {}", intf.getName(), node);
                return;
            }
            long localPort = (Long)of_ports.toArray()[0];

            Map<String, String> externalIds = intf.getExternalIdsColumn().getData();
            if (externalIds == null) {
                logger.error("No external_ids seen in {}", intf);
                return;
            }

            String attachedMac = externalIds.get(Constants.EXTERNAL_ID_VM_MAC);
            if (attachedMac == null) {
                logger.error("No AttachedMac seen in {}", intf);
                return;
            }

            Map<String, Row> intfs = ovsdbConfigurationService.getRows(node, ovsdbConfigurationService.getTableName(node, Interface.class));
            if (intfs != null) {
                for (Row row : intfs.values()) {
                    Interface tunIntf = ovsdbConfigurationService.getTypedRow(node, Interface.class, row);
                    if (tunIntf.getName().equals(this.getTunnelName(tunnelType, dst))) {
                        of_ports = tunIntf.getOpenFlowPortColumn().getData();
                        if (of_ports == null || of_ports.size() <= 0) {
                            logger.debug("Could NOT Identify Tunnel port {} on {}", tunIntf.getName(), node);
                            continue;
                        }
                        long tunnelOFPort = (Long)of_ports.toArray()[0];

                        if (tunnelOFPort == -1) {
                            logger.error("Could NOT Identify Tunnel port {} -> OF ({}) on {}", tunIntf.getName(), tunnelOFPort, node);
                            return;
                        }
                        logger.debug("Identified Tunnel port {} -> OF ({}) on {}", tunIntf.getName(), tunnelOFPort, node);

                        if (!local) {
                            programRemoteEgressTunnelBridgeRules(node, dpid, segmentationId, attachedMac, tunnelOFPort, localPort);
                        }
                        logger.trace("program local ingress tunnel rules: node" + node.getNodeIDString() + " intf " + intf.getName());
                        if (local) {
                            programLocalIngressTunnelBridgeRules(node, dpid, segmentationId, attachedMac, tunnelOFPort, localPort);
                        }
                        return;
                    }
                }
            }
        } catch (Exception e) {
            logger.error("", e);
        }
    }

    private void removeTunnelRules (String tunnelType, String segmentationId, InetAddress dst, Node node,
            Interface intf, boolean local, boolean isLastInstanceOnNode) {

        Preconditions.checkNotNull(ovsdbConfigurationService);
        try {

            Long dpid = this.getIntegrationBridgeOFDPID(node);
            if (dpid == 0L) {
                logger.debug("Openflow Datapath-ID not set for the integration bridge in {}", node);
                return;
            }

            Set<Long> of_ports = intf.getOpenFlowPortColumn().getData();
            if (of_ports == null || of_ports.size() <= 0) {
                logger.error("Could NOT Identify OF value for port {} on {}", intf.getName(), node);
                return;
            }
            long localPort = (Long)of_ports.toArray()[0];

            Map<String, String> externalIds = intf.getExternalIdsColumn().getData();
            if (externalIds == null) {
                logger.error("No external_ids seen in {}", intf);
                return;
            }

            String attachedMac = externalIds.get(Constants.EXTERNAL_ID_VM_MAC);
            if (attachedMac == null) {
                logger.error("No AttachedMac seen in {}", intf);
                return;
            }

            Map<String, Row> intfs = ovsdbConfigurationService.getRows(node, ovsdbConfigurationService.getTableName(node, Interface.class));
            if (intfs != null) {
                for (Row row : intfs.values()) {
                    Interface tunIntf = ovsdbConfigurationService.getTypedRow(node, Interface.class, row);
                    if (tunIntf.getName().equals(this.getTunnelName(tunnelType, dst))) {
                        of_ports = tunIntf.getOpenFlowPortColumn().getData();
                        if (of_ports == null || of_ports.size() <= 0) {
                            logger.error("Could NOT Identify Tunnel port {} on {}", tunIntf.getName(), node);
                            continue;
                        }
                        long tunnelOFPort = (Long)of_ports.toArray()[0];

                        if (tunnelOFPort == -1) {
                            logger.error("Could NOT Identify Tunnel port {} -> OF ({}) on {}", tunIntf.getName(), tunnelOFPort, node);
                            return;
                        }
                        logger.debug("Identified Tunnel port {} -> OF ({}) on {}", tunIntf.getName(), tunnelOFPort, node);

                        if (!local) {
                            removeRemoteEgressTunnelBridgeRules(node, dpid, segmentationId, attachedMac, tunnelOFPort, localPort);
                        }
                        if (local && isLastInstanceOnNode) {
                            removePerTunnelRules(node, dpid, segmentationId, tunnelOFPort);
                        }
                        return;
                    }
                }
            }
        } catch (Exception e) {
            logger.error("", e);
        }
    }

    private void programVlanRules (NeutronNetwork network, Node node, Interface intf) {
        Preconditions.checkNotNull(ovsdbConfigurationService);
        logger.debug("Program vlan rules for interface {}", intf.getName());
        try {

            Long dpid = this.getIntegrationBridgeOFDPID(node);
            if (dpid == 0L) {
                logger.debug("Openflow Datapath-ID not set for the integration bridge in {}", node);
                return;
            }

            Set<Long> of_ports = intf.getOpenFlowPortColumn().getData();
            int timeout = 6;
            while ((of_ports == null) && (timeout > 0)) {
                of_ports = intf.getOpenFlowPortColumn().getData();
                if (of_ports == null || of_ports.size() <= 0) {
                    // Wait for the OVSDB update to sync up the Local cache.
                    Thread.sleep(500);
                    timeout--;
                }
            }
            if (of_ports == null || of_ports.size() <= 0) {
                logger.error("Could NOT Identify OF value for port {} on {}", intf.getName(), node);
                return;
            }

            Map<String, String> externalIds = intf.getExternalIdsColumn().getData();
            if (externalIds == null) {
                logger.error("No external_ids seen in {}", intf);
                return;
            }

            String attachedMac = externalIds.get(Constants.EXTERNAL_ID_VM_MAC);
            if (attachedMac == null) {
                logger.error("No AttachedMac seen in {}", intf);
                return;
            }

            Map<String, Row> intfs = ovsdbConfigurationService.getRows(node, ovsdbConfigurationService.getTableName(node, Interface.class));
            if (intfs != null) {
                for (Row row : intfs.values()) {
                    Interface ethIntf = ovsdbConfigurationService.getTypedRow(node, Interface.class, row);
                    if (ethIntf.getName().equalsIgnoreCase(bridgeConfigurationManager.getPhysicalInterfaceName(node, network.getProviderPhysicalNetwork()))) {
                        of_ports = ethIntf.getOpenFlowPortColumn().getData();
                        timeout = 6;
                        while ((of_ports == null) && (timeout > 0)) {
                            of_ports = ethIntf.getOpenFlowPortColumn().getData();
                            if (of_ports == null || of_ports.size() <= 0) {
                                // Wait for the OVSDB update to sync up the Local cache.
                                Thread.sleep(500);
                                timeout--;
                            }
                        }

                        if (of_ports == null || of_ports.size() <= 0) {
                            logger.error("Could NOT Identify eth port {} on {}", ethIntf.getName(), node);
                            continue;
                        }
                        long ethOFPort = (Long)of_ports.toArray()[0];

                        if (ethOFPort == -1) {
                            logger.error("Could NOT Identify eth port {} -> OF ({}) on {}", ethIntf.getName(), ethOFPort, node);
                            throw new Exception("port number < 0");
                        }
                        logger.debug("Identified eth port {} -> OF ({}) on {}", ethIntf.getName(), ethOFPort, node);

                        programRemoteEgressVlanRules(node, dpid, network.getProviderSegmentationID(), attachedMac, ethOFPort);
                        programLocalIngressVlanRules(node, dpid, network.getProviderSegmentationID(), attachedMac, ethOFPort);
                        return;
                    }
                }
            }
        } catch (Exception e) {
            logger.error("", e);
        }
    }

    private void removeVlanRules (NeutronNetwork network, Node node,
                      Interface intf, boolean isLastInstanceOnNode) {
        Preconditions.checkNotNull(ovsdbConfigurationService);
        logger.debug("Remove vlan rules for interface {}", intf.getName());

        try {

            Long dpid = this.getIntegrationBridgeOFDPID(node);
            if (dpid == 0L) {
                logger.debug("Openflow Datapath-ID not set for the integration bridge in {}", node);
                return;
            }

            Set<Long> of_ports = intf.getOpenFlowPortColumn().getData();
            if (of_ports == null || of_ports.size() <= 0) {
                logger.error("Could NOT Identify OF value for port {} on {}", intf.getName(), node);
                return;
            }

            Map<String, String> externalIds = intf.getExternalIdsColumn().getData();
            if (externalIds == null) {
                logger.error("No external_ids seen in {}", intf);
                return;
            }

            String attachedMac = externalIds.get(Constants.EXTERNAL_ID_VM_MAC);
            if (attachedMac == null) {
                logger.error("No AttachedMac seen in {}", intf);
                return;
            }

            Map<String, Row> intfs = ovsdbConfigurationService.getRows(node, ovsdbConfigurationService.getTableName(node, Interface.class));
            if (intfs != null) {
                for (Row row : intfs.values()) {
                    Interface ethIntf = ovsdbConfigurationService.getTypedRow(node, Interface.class, row);
                    if (ethIntf.getName().equalsIgnoreCase(bridgeConfigurationManager.getPhysicalInterfaceName(node,
                                                                   network.getProviderPhysicalNetwork()))) {
                        of_ports = ethIntf.getOpenFlowPortColumn().getData();
                        if (of_ports == null || of_ports.size() <= 0) {
                            logger.error("Could NOT Identify eth port {} on {}", ethIntf.getName(), node);
                            continue;
                        }
                        long ethOFPort = (Long)of_ports.toArray()[0];

                        if (ethOFPort == -1) {
                            logger.error("Could NOT Identify eth port {} -> OF ({}) on {}", ethIntf.getName(), ethOFPort, node);
                            throw new Exception("port number < 0");
                        }
                        logger.debug("Identified eth port {} -> OF ({}) on {}", ethIntf.getName(), ethOFPort, node);

                        removeRemoteEgressVlanRules(node, dpid, network.getProviderSegmentationID(), attachedMac, ethOFPort);
                        if (isLastInstanceOnNode) {
                            removePerVlanRules(node, dpid, network.getProviderSegmentationID(), ethOFPort);
                        }
                        return;
                    }
                }
            }
        } catch (Exception e) {
            logger.error("", e);
        }
    }

    @Override
    public Status handleInterfaceUpdate(NeutronNetwork network, Node srcNode, Interface intf) {
        Preconditions.checkNotNull(connectionService);
        List<Node> nodes = connectionService.getNodes();
        nodes.remove(srcNode);
        this.programLocalRules(network.getProviderNetworkType(), network.getProviderSegmentationID(), srcNode, intf);

        if (network.getProviderNetworkType().equalsIgnoreCase(NetworkHandler.NETWORK_TYPE_VLAN)) {
            this.programVlanRules(network, srcNode, intf);
        } else if (network.getProviderNetworkType().equalsIgnoreCase(NetworkHandler.NETWORK_TYPE_GRE)
                   || network.getProviderNetworkType().equalsIgnoreCase(NetworkHandler.NETWORK_TYPE_VXLAN)){
            for (Node dstNode : nodes) {
                InetAddress src = configurationService.getTunnelEndPoint(srcNode);
                InetAddress dst = configurationService.getTunnelEndPoint(dstNode);
                Status status = addTunnelPort(srcNode, network.getProviderNetworkType(), src, dst);
                if (status.isSuccess()) {
                    this.programTunnelRules(network.getProviderNetworkType(), network.getProviderSegmentationID(), dst, srcNode, intf, true);
                }
                addTunnelPort(dstNode, network.getProviderNetworkType(), dst, src);
                if (status.isSuccess()) {
                    this.programTunnelRules(network.getProviderNetworkType(), network.getProviderSegmentationID(), src, dstNode, intf, false);
                }
            }
        }

        return new Status(StatusCode.SUCCESS);
    }

    private Status triggerInterfaceUpdates(Node node) {
        Preconditions.checkNotNull(ovsdbConfigurationService);
        try {
            Map<String, Row> intfs = ovsdbConfigurationService.getRows(node, ovsdbConfigurationService.getTableName(node, Interface.class));
            if (intfs != null) {
                for (Row row : intfs.values()) {
                    Interface intf = ovsdbConfigurationService.getTypedRow(node, Interface.class, row);
                    NeutronNetwork network = tenantNetworkManager.getTenantNetwork(intf);
                    logger.debug("Trigger Interface update for {}", intf);
                    if (network != null) {
                        this.handleInterfaceUpdate(network, node, intf);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error Triggering the lost interface updates for "+ node, e);
            return new Status(StatusCode.INTERNALERROR, e.getLocalizedMessage());
        }
        return new Status(StatusCode.SUCCESS);
    }
    @Override
    public Status handleInterfaceUpdate(String tunnelType, String tunnelKey) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Status handleInterfaceDelete(String tunnelType, NeutronNetwork network, Node srcNode, Interface intf,
            boolean isLastInstanceOnNode) {
        Preconditions.checkNotNull(connectionService);
        Status status = new Status(StatusCode.SUCCESS);
        List<Node> nodes = connectionService.getNodes();
        nodes.remove(srcNode);

        logger.info("Delete intf " + intf.getName() + " isLastInstanceOnNode " + isLastInstanceOnNode);
        List<String> phyIfName = bridgeConfigurationManager.getAllPhysicalInterfaceNames(srcNode);
        if (intf.getTypeColumn().getData().equalsIgnoreCase(NetworkHandler.NETWORK_TYPE_VXLAN)
            || intf.getTypeColumn().getData().equalsIgnoreCase(NetworkHandler.NETWORK_TYPE_GRE)) {
            /* Delete tunnel port */
            try {
                Map<String, String> options = intf.getOptionsColumn().getData();
                InetAddress src = InetAddress.getByName(options.get("local_ip"));
                InetAddress dst = InetAddress.getByName(options.get("remote_ip"));
                status = deleteTunnelPort(srcNode, intf.getTypeColumn().getData(), src, dst);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        } else if (phyIfName.contains(intf.getName())) {
            deletePhysicalPort(srcNode, intf.getName());
        } else {
            /* delete all other interfaces */
            this.removeLocalRules(network.getProviderNetworkType(), network.getProviderSegmentationID(),
                    srcNode, intf);

            if (network.getProviderNetworkType().equalsIgnoreCase(NetworkHandler.NETWORK_TYPE_VLAN)) {
                this.removeVlanRules(network, srcNode,
                                 intf, isLastInstanceOnNode);
            } else if (network.getProviderNetworkType().equalsIgnoreCase(NetworkHandler.NETWORK_TYPE_GRE)
                   || network.getProviderNetworkType().equalsIgnoreCase(NetworkHandler.NETWORK_TYPE_VXLAN)) {

                for (Node dstNode : nodes) {
                    InetAddress src = configurationService.getTunnelEndPoint(srcNode);
                    InetAddress dst = configurationService.getTunnelEndPoint(dstNode);
                    logger.info("Remove tunnel rules for interface " + intf.getName() + " on srcNode " + srcNode.getNodeIDString());
                    this.removeTunnelRules(tunnelType, network.getProviderSegmentationID(),
                                           dst, srcNode, intf, true, isLastInstanceOnNode);
                    logger.info("Remove tunnel rules for interface " + intf.getName() + " on dstNode " + dstNode.getNodeIDString());
                    this.removeTunnelRules(tunnelType, network.getProviderSegmentationID(),
                                           src, dstNode, intf, false, isLastInstanceOnNode);
                }
            }
        }
        return status;
    }

    @Override
    public void initializeFlowRules(Node node) {
        this.initializeFlowRules(node, configurationService.getIntegrationBridgeName());
        this.initializeFlowRules(node, configurationService.getExternalBridgeName());
        this.triggerInterfaceUpdates(node);
    }

    private void initializeFlowRules(Node node, String bridgeName) {
        String bridgeUuid = this.getInternalBridgeUUID(node, bridgeName);
        if (bridgeUuid == null) {
            return;
        }

        Long dpid = getDpid(node, bridgeUuid);

        if (dpid == 0L) {
            logger.debug("Openflow Datapath-ID not set for the integration bridge in {}", node);
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

        String nodeName = OPENFLOW + dpidLong;
        EtherType etherType = new EtherType(0x88CCL);

        MatchBuilder matchBuilder = new MatchBuilder();
        NodeBuilder nodeBuilder = createNodeBuilder(nodeName);
        FlowBuilder flowBuilder = new FlowBuilder();

        // Create Match(es) and Set them in the FlowBuilder Object
        flowBuilder.setMatch(OF13MdSalMatch.createEtherTypeMatch(matchBuilder, etherType).build());

        // Create the OF Actions and Instructions
        InstructionBuilder ib = new InstructionBuilder();
        InstructionsBuilder isb = new InstructionsBuilder();

        // Instructions List Stores Individual Instructions
        List<Instruction> instructions = Lists.newArrayList();

        // Call the InstructionBuilder Methods Containing Actions
        OF13MdSalInstruction.createSendToControllerInstructions(ib);
        ib.setOrder(0);
        ib.setKey(new InstructionKey(0));
        instructions.add(ib.build());

        // Add InstructionBuilder to the Instruction(s)Builder List
        isb.setInstruction(instructions);

        // Add InstructionsBuilder to FlowBuilder
        flowBuilder.setInstructions(isb.build());

        String flowId = "LLDP";
        flowBuilder.setId(new FlowId(flowId));
        FlowKey key = new FlowKey(new FlowId(flowId));
        flowBuilder.setBarrier(true);
        flowBuilder.setTableId((short) 0);
        flowBuilder.setKey(key);
        flowBuilder.setFlowName(flowId);
        flowBuilder.setHardTimeout(0);
        flowBuilder.setIdleTimeout(0);
        writeFlow(flowBuilder, nodeBuilder);
    }

    /*
    * Create a NORMAL Table Miss Flow Rule
    * Match: any
    * Action: forward to NORMAL pipeline
    */

    private void writeNormalRule(Long dpidLong) {

        String nodeName = OPENFLOW + dpidLong;

        MatchBuilder matchBuilder = new MatchBuilder();
        NodeBuilder nodeBuilder = createNodeBuilder(nodeName);
        FlowBuilder flowBuilder = new FlowBuilder();

        // Create the OF Actions and Instructions
        InstructionBuilder ib = new InstructionBuilder();
        InstructionsBuilder isb = new InstructionsBuilder();

        // Instructions List Stores Individual Instructions
        List<Instruction> instructions = Lists.newArrayList();

        // Call the InstructionBuilder Methods Containing Actions
        OF13MdSalInstruction.createNormalInstructions(ib);
        ib.setOrder(0);
        ib.setKey(new InstructionKey(0));
        instructions.add(ib.build());

        // Add InstructionBuilder to the Instruction(s)Builder List
        isb.setInstruction(instructions);

        // Add InstructionsBuilder to FlowBuilder
        flowBuilder.setInstructions(isb.build());

        String flowId = "NORMAL";
        flowBuilder.setId(new FlowId(flowId));
        FlowKey key = new FlowKey(new FlowId(flowId));
        flowBuilder.setMatch(matchBuilder.build());
        flowBuilder.setPriority(0);
        flowBuilder.setBarrier(true);
        flowBuilder.setTableId((short) 0);
        flowBuilder.setKey(key);
        flowBuilder.setFlowName(flowId);
        flowBuilder.setHardTimeout(0);
        flowBuilder.setIdleTimeout(0);
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

        String nodeName = OPENFLOW + dpidLong;

        BigInteger tunnelId = new BigInteger(segmentationId);
        MatchBuilder matchBuilder = new MatchBuilder();
        NodeBuilder nodeBuilder = createNodeBuilder(nodeName);
        FlowBuilder flowBuilder = new FlowBuilder();

        // Create Match(es) and Set them in the FlowBuilder Object
        flowBuilder.setMatch(OF13MdSalMatch.createTunnelIDMatch(matchBuilder, tunnelId).build());
        flowBuilder.setMatch(OF13MdSalMatch.createInPortMatch(matchBuilder, dpidLong, ofPort).build());

        if (write) {
            // Create the OF Actions and Instructions
            InstructionBuilder ib = new InstructionBuilder();
            InstructionsBuilder isb = new InstructionsBuilder();

            // Instructions List Stores Individual Instructions
            List<Instruction> instructions = Lists.newArrayList();

            // Call the InstructionBuilder Methods Containing Actions
            OF13MdSalInstruction.createGotoTableInstructions(ib, goToTableId);
            ib.setOrder(0);
            ib.setKey(new InstructionKey(0));
            instructions.add(ib.build());

            // Add InstructionBuilder to the Instruction(s)Builder List
            isb.setInstruction(instructions);

            // Add InstructionsBuilder to FlowBuilder
            flowBuilder.setInstructions(isb.build());
        }

        String flowId = "TunnelIn_"+segmentationId+"_"+ofPort;
        // Add Flow Attributes
        flowBuilder.setId(new FlowId(flowId));
        FlowKey key = new FlowKey(new FlowId(flowId));
        flowBuilder.setStrict(true);
        flowBuilder.setBarrier(false);
        flowBuilder.setTableId(writeTable);
        flowBuilder.setKey(key);
        flowBuilder.setFlowName(flowId);
        flowBuilder.setHardTimeout(0);
        flowBuilder.setIdleTimeout(0);

        if (write) {
            writeFlow(flowBuilder, nodeBuilder);
        } else {
            removeFlow(flowBuilder, nodeBuilder);
        }
    }

    /*
     * (Table:0) Ingress VLAN Traffic
     * Match: OpenFlow InPort and vlan ID
     * Action: GOTO Local Table (20)
     * table=0,vlan_id=0x5,in_port=10, actions=goto_table:2
     */

    private void handleVlanIn(Long dpidLong, Short writeTable, Short goToTableId,
                      String segmentationId,  Long ethPort, boolean write) {

        String nodeName = OPENFLOW + dpidLong;

        MatchBuilder matchBuilder = new MatchBuilder();
        NodeBuilder nodeBuilder = createNodeBuilder(nodeName);
        FlowBuilder flowBuilder = new FlowBuilder();

        // Create Match(es) and Set them in the FlowBuilder Object
        flowBuilder.setMatch(
                OF13MdSalMatch.createVlanIdMatch(matchBuilder, new VlanId(Integer.valueOf(segmentationId)))
                        .build())
                .setMatch(OF13MdSalMatch.createInPortMatch(matchBuilder, dpidLong, ethPort)
                        .build());

        if (write) {
            // Create the OF Actions and Instructions
            InstructionBuilder ib = new InstructionBuilder();
            InstructionsBuilder isb = new InstructionsBuilder();

            // Instructions List Stores Individual Instructions
            List<Instruction> instructions = Lists.newArrayList();

            // Call the InstructionBuilder Methods Containing Actions
            OF13MdSalInstruction.createGotoTableInstructions(ib, goToTableId);
            ib.setOrder(0);
            ib.setKey(new InstructionKey(0));
            instructions.add(ib.build());

            // Add InstructionBuilder to the Instruction(s)Builder List
            isb.setInstruction(instructions);

            // Add InstructionsBuilder to FlowBuilder
            flowBuilder.setInstructions(isb.build());
        }

        String flowId = "VlanIn_"+segmentationId+"_"+ethPort;
        // Add Flow Attributes
        flowBuilder.setId(new FlowId(flowId));
        FlowKey key = new FlowKey(new FlowId(flowId));
        flowBuilder.setStrict(true);
        flowBuilder.setBarrier(false);
        flowBuilder.setTableId(writeTable);
        flowBuilder.setKey(key);
        flowBuilder.setFlowName(flowId);
        flowBuilder.setHardTimeout(0);
        flowBuilder.setIdleTimeout(0);
        if (write) {
            writeFlow(flowBuilder, nodeBuilder);
        } else {
            removeFlow(flowBuilder, nodeBuilder);
        }
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

        String nodeName = OPENFLOW + dpidLong;

        MatchBuilder matchBuilder = new MatchBuilder();
        NodeBuilder nodeBuilder = createNodeBuilder(nodeName);
        FlowBuilder flowBuilder = new FlowBuilder();

        // Create the OF Match using MatchBuilder
        flowBuilder.setMatch(OF13MdSalMatch.createEthSrcMatch(matchBuilder, new MacAddress(attachedMac)).build());
        // TODO Broken In_Port Match
        flowBuilder.setMatch(OF13MdSalMatch.createInPortMatch(matchBuilder, dpidLong, inPort).build());

        String flowId = "LocalMac_"+segmentationId+"_"+inPort+"_"+attachedMac;
        // Add Flow Attributes
        flowBuilder.setId(new FlowId(flowId));
        FlowKey key = new FlowKey(new FlowId(flowId));
        flowBuilder.setStrict(true);
        flowBuilder.setBarrier(false);
        flowBuilder.setTableId(writeTable);
        flowBuilder.setKey(key);
        flowBuilder.setFlowName(flowId);
        flowBuilder.setHardTimeout(0);
        flowBuilder.setIdleTimeout(0);

        if (write) {
            // Instantiate the Builders for the OF Actions and Instructions
            InstructionBuilder ib = new InstructionBuilder();
            InstructionsBuilder isb = new InstructionsBuilder();

            // Instructions List Stores Individual Instructions
            List<Instruction> instructions = Lists.newArrayList();

            // GOTO Instructions Need to be added first to the List
            OF13MdSalInstruction.createGotoTableInstructions(ib, goToTableId);
            ib.setOrder(0);
            ib.setKey(new InstructionKey(0));
            instructions.add(ib.build());
            // TODO Broken SetTunID
            OF13MdSalInstruction.createSetTunnelIdInstructions(ib, new BigInteger(segmentationId));
            ib.setOrder(1);
            ib.setKey(new InstructionKey(1));
            instructions.add(ib.build());

            // Add InstructionBuilder to the Instruction(s)Builder List
            isb.setInstruction(instructions);

            // Add InstructionsBuilder to FlowBuilder
            flowBuilder.setInstructions(isb.build());

            writeFlow(flowBuilder, nodeBuilder);
        } else {
            removeFlow(flowBuilder, nodeBuilder);
        }
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

         String nodeName = OPENFLOW + dpidLong;

         MatchBuilder matchBuilder = new MatchBuilder();
         NodeBuilder nodeBuilder = createNodeBuilder(nodeName);
         FlowBuilder flowBuilder = new FlowBuilder();

         // Create the OF Match using MatchBuilder
         flowBuilder.setMatch(OF13MdSalMatch.createEthSrcMatch(matchBuilder, new MacAddress(attachedMac)).build());
         flowBuilder.setMatch(OF13MdSalMatch.createInPortMatch(matchBuilder, dpidLong, inPort).build());

         String flowId = "LocalMac_"+segmentationId+"_"+inPort+"_"+attachedMac;
         // Add Flow Attributes
         flowBuilder.setId(new FlowId(flowId));
         FlowKey key = new FlowKey(new FlowId(flowId));
         flowBuilder.setStrict(true);
         flowBuilder.setBarrier(false);
         flowBuilder.setTableId(writeTable);
         flowBuilder.setKey(key);
         flowBuilder.setFlowName(flowId);
         flowBuilder.setHardTimeout(0);
         flowBuilder.setIdleTimeout(0);

         if (write) {
             // Instantiate the Builders for the OF Actions and Instructions
             InstructionBuilder ib = new InstructionBuilder();
             InstructionsBuilder isb = new InstructionsBuilder();

             // Instructions List Stores Individual Instructions
             List<Instruction> instructions = Lists.newArrayList();

             // GOTO Instructions Need to be added first to the List
             OF13MdSalInstruction.createGotoTableInstructions(ib, goToTableId);
             ib.setOrder(0);
             ib.setKey(new InstructionKey(0));
             instructions.add(ib.build());
             // Set VLAN ID Instruction
             OF13MdSalInstruction.createSetVlanInstructions(ib, new VlanId(Integer.valueOf(segmentationId)));
             ib.setOrder(1);
             ib.setKey(new InstructionKey(1));
             instructions.add(ib.build());

             // Add InstructionBuilder to the Instruction(s)Builder List
             isb.setInstruction(instructions);

             // Add InstructionsBuilder to FlowBuilder
             flowBuilder.setInstructions(isb.build());

             writeFlow(flowBuilder, nodeBuilder);
         } else {
             removeFlow(flowBuilder, nodeBuilder);
         }
     }

    /*
     * (Table:0) Drop frames source from a VM that do not
     * match the associated MAC address of the local VM.
     * Match: Low priority anything not matching the VM SMAC
     * Instruction: Drop
     * table=0,priority=16384,in_port=1 actions=drop"
     */

    private void handleDropSrcIface(Long dpidLong, Long inPort, boolean write) {

        String nodeName = OPENFLOW + dpidLong;

        MatchBuilder matchBuilder = new MatchBuilder();
        NodeBuilder nodeBuilder = createNodeBuilder(nodeName);
        FlowBuilder flowBuilder = new FlowBuilder();

        // Create the OF Match using MatchBuilder
        flowBuilder.setMatch(OF13MdSalMatch.createInPortMatch(matchBuilder, dpidLong, inPort).build());

        if (write) {
            // Instantiate the Builders for the OF Actions and Instructions
            InstructionBuilder ib = new InstructionBuilder();
            InstructionsBuilder isb = new InstructionsBuilder();

            // Instructions List Stores Individual Instructions
            List<Instruction> instructions = Lists.newArrayList();

            // Call the InstructionBuilder Methods Containing Actions
            OF13MdSalInstruction.createDropInstructions(ib);
            ib.setOrder(0);
            ib.setKey(new InstructionKey(0));
            instructions.add(ib.build());

            // Add InstructionBuilder to the Instruction(s)Builder List
            isb.setInstruction(instructions);

            // Add InstructionsBuilder to FlowBuilder
            flowBuilder.setInstructions(isb.build());
        }

        String flowId = "DropFilter_"+inPort;
        // Add Flow Attributes
        flowBuilder.setId(new FlowId(flowId));
        FlowKey key = new FlowKey(new FlowId(flowId));
        flowBuilder.setStrict(true);
        flowBuilder.setBarrier(false);
        flowBuilder.setTableId(TABLE_0_DEFAULT_INGRESS);
        flowBuilder.setKey(key);
        flowBuilder.setFlowName(flowId);
        flowBuilder.setPriority(8192);
        flowBuilder.setHardTimeout(0);
        flowBuilder.setIdleTimeout(0);
        if (write) {
            writeFlow(flowBuilder, nodeBuilder);
        } else {
            removeFlow(flowBuilder, nodeBuilder);
        }
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

        String nodeName = OPENFLOW + dpidLong;

        MatchBuilder matchBuilder = new MatchBuilder();
        NodeBuilder nodeBuilder = createNodeBuilder(nodeName);
        FlowBuilder flowBuilder = new FlowBuilder();

        // Create the OF Match using MatchBuilder
        flowBuilder.setMatch(OF13MdSalMatch.createTunnelIDMatch(matchBuilder, new BigInteger(segmentationId)).build());
        flowBuilder.setMatch(OF13MdSalMatch.createDestEthMatch(matchBuilder, new MacAddress(attachedMac), null).build());

        String flowId = "TunnelOut_"+segmentationId+"_"+OFPortOut+"_"+attachedMac;
        // Add Flow Attributes
        flowBuilder.setId(new FlowId(flowId));
        FlowKey key = new FlowKey(new FlowId(flowId));
        flowBuilder.setStrict(true);
        flowBuilder.setBarrier(false);
        flowBuilder.setTableId(writeTable);
        flowBuilder.setKey(key);
        flowBuilder.setFlowName(flowId);
        flowBuilder.setHardTimeout(0);
        flowBuilder.setIdleTimeout(0);

        if (write) {
            // Instantiate the Builders for the OF Actions and Instructions
            InstructionBuilder ib = new InstructionBuilder();
            InstructionsBuilder isb = new InstructionsBuilder();

            // Instructions List Stores Individual Instructions
            List<Instruction> instructions = Lists.newArrayList();

            // GOTO Instructions
            OF13MdSalInstruction.createGotoTableInstructions(ib, goToTableId);
            ib.setOrder(0);
            ib.setKey(new InstructionKey(0));
            instructions.add(ib.build());
            // Set the Output Port/Iface
            OF13MdSalInstruction.createOutputPortInstructions(ib, dpidLong, OFPortOut);
            ib.setOrder(1);
            ib.setKey(new InstructionKey(1));
            instructions.add(ib.build());

            // Add InstructionBuilder to the Instruction(s)Builder List
            isb.setInstruction(instructions);

            // Add InstructionsBuilder to FlowBuilder
            flowBuilder.setInstructions(isb.build());

            writeFlow(flowBuilder, nodeBuilder);
        } else {
            removeFlow(flowBuilder, nodeBuilder);
        }
    }

    /*
     * (Table:1) Egress VLAN Traffic
     * Match: Destination Ethernet Addr and VLAN id
     * Instruction: GOTO Table Table 2
     * table=1,vlan_id=0x5,dl_dst=00:00:00:00:00:08 \
     * actions= goto_table:2"
     */

     private void handleVlanOut(Long dpidLong, Short writeTable,
                        Short goToTableId, String segmentationId,
                        Long ethPort, String attachedMac, boolean write) {

         String nodeName = OPENFLOW + dpidLong;

         MatchBuilder matchBuilder = new MatchBuilder();
         NodeBuilder nodeBuilder = createNodeBuilder(nodeName);
         FlowBuilder flowBuilder = new FlowBuilder();

         // Create the OF Match using MatchBuilder
         flowBuilder.setMatch(
                 OF13MdSalMatch.createVlanIdMatch(matchBuilder, new VlanId(Integer.valueOf(segmentationId))).build());
         flowBuilder.setMatch(OF13MdSalMatch.createDestEthMatch(matchBuilder, new MacAddress(attachedMac), null).build());

         String flowId = "VlanOut_"+segmentationId+"_"+ethPort+"_"+attachedMac;
         // Add Flow Attributes
         flowBuilder.setId(new FlowId(flowId));
         FlowKey key = new FlowKey(new FlowId(flowId));
         flowBuilder.setStrict(true);
         flowBuilder.setBarrier(false);
         flowBuilder.setTableId(writeTable);
         flowBuilder.setKey(key);
         flowBuilder.setFlowName(flowId);
         flowBuilder.setHardTimeout(0);
         flowBuilder.setIdleTimeout(0);

         if (write) {
             // Instantiate the Builders for the OF Actions and Instructions
             InstructionBuilder ib = new InstructionBuilder();
             InstructionsBuilder isb = new InstructionsBuilder();

             // Instructions List Stores Individual Instructions
             List<Instruction> instructions = Lists.newArrayList();

             // GOTO Instructions
             OF13MdSalInstruction.createGotoTableInstructions(ib, goToTableId);
             ib.setOrder(0);
             ib.setKey(new InstructionKey(0));
             instructions.add(ib.build());

             // Add InstructionBuilder to the Instruction(s)Builder List
             isb.setInstruction(instructions);

             // Add InstructionsBuilder to FlowBuilder
             flowBuilder.setInstructions(isb.build());

             writeFlow(flowBuilder, nodeBuilder);
         } else {
             removeFlow(flowBuilder, nodeBuilder);
         }
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

        String nodeName = OPENFLOW + dpidLong;

        MatchBuilder matchBuilder = new MatchBuilder();
        NodeBuilder nodeBuilder = createNodeBuilder(nodeName);
        FlowBuilder flowBuilder = new FlowBuilder();

        // Create the OF Match using MatchBuilder
        // Match TunnelID
        flowBuilder.setMatch(OF13MdSalMatch.createTunnelIDMatch(matchBuilder, new BigInteger(segmentationId)).build());
        // Match DMAC

        flowBuilder.setMatch(OF13MdSalMatch.createDestEthMatch(matchBuilder, new MacAddress("01:00:00:00:00:00"),
                new MacAddress("01:00:00:00:00:00")).build());

        String flowId = "TunnelFloodOut_"+segmentationId;
        // Add Flow Attributes
        flowBuilder.setId(new FlowId(flowId));
        FlowKey key = new FlowKey(new FlowId(flowId));
        flowBuilder.setBarrier(true);
        flowBuilder.setTableId(writeTable);
        flowBuilder.setKey(key);
        flowBuilder.setPriority(16384);
        flowBuilder.setFlowName(flowId);
        flowBuilder.setHardTimeout(0);
        flowBuilder.setIdleTimeout(0);

        Flow flow = this.getFlow(flowBuilder, nodeBuilder);
        // Instantiate the Builders for the OF Actions and Instructions
        InstructionBuilder ib = new InstructionBuilder();
        InstructionsBuilder isb = new InstructionsBuilder();
        List<Instruction> instructions = Lists.newArrayList();
        List<Instruction> existingInstructions = null;
        if (flow != null) {
            Instructions ins = flow.getInstructions();
            if (ins != null) {
                existingInstructions = ins.getInstruction();
            }
        }

        if (write) {
            // GOTO Instruction
            OF13MdSalInstruction.createGotoTableInstructions(ib, localTable);
            ib.setOrder(0);
            ib.setKey(new InstructionKey(0));
            instructions.add(ib.build());
            // Set the Output Port/Iface
            //createOutputGroupInstructions(nodeBuilder, ib, dpidLong, OFPortOut, existingInstructions);
            createOutputPortInstructions(ib, dpidLong, OFPortOut, existingInstructions);
            ib.setOrder(1);
            ib.setKey(new InstructionKey(1));
            instructions.add(ib.build());

            // Add InstructionBuilder to the Instruction(s)Builder List
            isb.setInstruction(instructions);

            // Add InstructionsBuilder to FlowBuilder
            flowBuilder.setInstructions(isb.build());

            writeFlow(flowBuilder, nodeBuilder);
        } else {
            /* remove port from action list */
            boolean flowRemove = OF13MdSalInstruction.removeOutputPortFromInstructions(ib, dpidLong,
                    OFPortOut, existingInstructions);
            if (flowRemove) {
                /* if all port are removed, remove the flow too. */
                removeFlow(flowBuilder, nodeBuilder);
            } else {
                /* Install instruction with new output port list*/
                ib.setOrder(0);
                ib.setKey(new InstructionKey(0));
                instructions.add(ib.build());

                // Add InstructionBuilder to the Instruction(s)Builder List
                isb.setInstruction(instructions);

                // Add InstructionsBuilder to FlowBuilder
                flowBuilder.setInstructions(isb.build());
            }
        }
    }

    /*
     * (Table:1) Egress VLAN Traffic
     * Match: Destination Ethernet Addr and VLAN id
     * Instruction: GOTO table 2 and Output port eth interface
     * Example: table=1,priority=16384,vlan_id=0x5,dl_dst=ff:ff:ff:ff:ff:ff \
     * actions=output:eth1,goto_table:2
     */

     private void handleVlanFloodOut(Long dpidLong, Short writeTable,
                           Short localTable, String segmentationId,
                           Long ethPort, boolean write) {

         String nodeName = OPENFLOW + dpidLong;

         MatchBuilder matchBuilder = new MatchBuilder();
         NodeBuilder nodeBuilder = createNodeBuilder(nodeName);
         FlowBuilder flowBuilder = new FlowBuilder();

         // Create the OF Match using MatchBuilder
         // Match Vlan ID
         flowBuilder.setMatch(
                 OF13MdSalMatch.createVlanIdMatch(matchBuilder, new VlanId(Integer.valueOf(segmentationId))).build());
         // Match DMAC
         flowBuilder.setMatch(OF13MdSalMatch.createDestEthMatch(matchBuilder, new MacAddress("01:00:00:00:00:00"),
                 new MacAddress("01:00:00:00:00:00")).build());

         String flowId = "VlanFloodOut_"+segmentationId;
         // Add Flow Attributes
         flowBuilder.setId(new FlowId(flowId));
         FlowKey key = new FlowKey(new FlowId(flowId));
         flowBuilder.setBarrier(true);
         flowBuilder.setTableId(writeTable);
         flowBuilder.setKey(key);
         flowBuilder.setPriority(16384);
         flowBuilder.setFlowName(flowId);
         flowBuilder.setHardTimeout(0);
         flowBuilder.setIdleTimeout(0);

         //ToDo: Is there something to be done with result of the call to getFlow?

         Flow flow = this.getFlow(flowBuilder, nodeBuilder);
         // Instantiate the Builders for the OF Actions and Instructions
         InstructionBuilder ib = new InstructionBuilder();
         InstructionsBuilder isb = new InstructionsBuilder();
         List<Instruction> instructions = Lists.newArrayList();

         if (write) {
             // GOTO Instruction
             OF13MdSalInstruction.createGotoTableInstructions(ib, localTable);
             ib.setOrder(0);
             ib.setKey(new InstructionKey(0));
             instructions.add(ib.build());
             // Set the Output Port/Iface
             OF13MdSalInstruction.createOutputPortInstructions(ib, dpidLong, ethPort);
             ib.setOrder(1);
             ib.setKey(new InstructionKey(1));
             instructions.add(ib.build());

             // Add InstructionBuilder to the Instruction(s)Builder List
             isb.setInstruction(instructions);

             // Add InstructionsBuilder to FlowBuilder
             flowBuilder.setInstructions(isb.build());

             writeFlow(flowBuilder, nodeBuilder);
         } else {
             removeFlow(flowBuilder, nodeBuilder);
         }
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

        String nodeName = OPENFLOW + dpidLong;

        MatchBuilder matchBuilder = new MatchBuilder();
        NodeBuilder nodeBuilder = createNodeBuilder(nodeName);
        FlowBuilder flowBuilder = new FlowBuilder();

        // Create Match(es) and Set them in the FlowBuilder Object
        flowBuilder.setMatch(OF13MdSalMatch.createTunnelIDMatch(matchBuilder, new BigInteger(segmentationId)).build());

        if (write) {
            // Create the OF Actions and Instructions
            InstructionBuilder ib = new InstructionBuilder();
            InstructionsBuilder isb = new InstructionsBuilder();

            // Instructions List Stores Individual Instructions
            List<Instruction> instructions = Lists.newArrayList();

            // Call the InstructionBuilder Methods Containing Actions
            OF13MdSalInstruction.createGotoTableInstructions(ib, goToTableId);
            ib.setOrder(0);
            ib.setKey(new InstructionKey(0));
            instructions.add(ib.build());

            // Add InstructionBuilder to the Instruction(s)Builder List
            isb.setInstruction(instructions);

            // Add InstructionsBuilder to FlowBuilder
            flowBuilder.setInstructions(isb.build());
        }

        String flowId = "TunnelMiss_"+segmentationId;
        // Add Flow Attributes
        flowBuilder.setId(new FlowId(flowId));
        FlowKey key = new FlowKey(new FlowId(flowId));
        flowBuilder.setStrict(true);
        flowBuilder.setBarrier(false);
        flowBuilder.setTableId(writeTable);
        flowBuilder.setKey(key);
        flowBuilder.setPriority(8192);
        flowBuilder.setFlowName(flowId);
        flowBuilder.setHardTimeout(0);
        flowBuilder.setIdleTimeout(0);
        if (write) {
            writeFlow(flowBuilder, nodeBuilder);
        } else {
            removeFlow(flowBuilder, nodeBuilder);
        }
    }


    /*
     * (Table:1) Table Drain w/ Catch All
     * Match: Vlan ID
     * Action: Output port eth interface
     * table=1,priority=8192,vlan_id=0x5 actions= output port:eth1
     */

     private void handleVlanMiss(Long dpidLong, Short writeTable,
                         Short goToTableId, String segmentationId,
                         Long ethPort, boolean write) {

         String nodeName = OPENFLOW + dpidLong;

         MatchBuilder matchBuilder = new MatchBuilder();
         NodeBuilder nodeBuilder = createNodeBuilder(nodeName);
         FlowBuilder flowBuilder = new FlowBuilder();

         // Create Match(es) and Set them in the FlowBuilder Object
         flowBuilder.setMatch(
                 OF13MdSalMatch.createVlanIdMatch(matchBuilder, new VlanId(Integer.valueOf(segmentationId))).build());

         if (write) {
             // Create the OF Actions and Instructions
             InstructionBuilder ib = new InstructionBuilder();
             InstructionsBuilder isb = new InstructionsBuilder();

             // Instructions List Stores Individual Instructions
             List<Instruction> instructions = Lists.newArrayList();

             // Call the InstructionBuilder Methods Containing Actions
             //createGotoTableInstructions(ib, goToTableId);
             //ib.setOrder(0);
             //ib.setKey(new InstructionKey(0));
             //instructions.add(ib.build());
             // Set the Output Port/Iface
             OF13MdSalInstruction.createOutputPortInstructions(ib, dpidLong, ethPort);
             ib.setOrder(0);
             ib.setKey(new InstructionKey(1));
             instructions.add(ib.build());

             // Add InstructionBuilder to the Instruction(s)Builder List
             isb.setInstruction(instructions);

             // Add InstructionsBuilder to FlowBuilder
             flowBuilder.setInstructions(isb.build());
         }

         String flowId = "VlanMiss_"+segmentationId;
         // Add Flow Attributes
         flowBuilder.setId(new FlowId(flowId));
         FlowKey key = new FlowKey(new FlowId(flowId));
         flowBuilder.setStrict(true);
         flowBuilder.setBarrier(false);
         flowBuilder.setTableId(writeTable);
         flowBuilder.setKey(key);
         flowBuilder.setPriority(8192);
         flowBuilder.setFlowName(flowId);
         flowBuilder.setHardTimeout(0);
         flowBuilder.setIdleTimeout(0);
         if (write) {
             writeFlow(flowBuilder, nodeBuilder);
         } else {
             removeFlow(flowBuilder, nodeBuilder);
         }
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

        String nodeName = OPENFLOW + dpidLong;

        MatchBuilder matchBuilder = new MatchBuilder();
        NodeBuilder nodeBuilder = createNodeBuilder(nodeName);
        FlowBuilder flowBuilder = new FlowBuilder();

        // Create the OF Match using MatchBuilder
        flowBuilder.setMatch(OF13MdSalMatch.createTunnelIDMatch(matchBuilder, new BigInteger(segmentationId)).build());
        flowBuilder.setMatch(OF13MdSalMatch.createDestEthMatch(matchBuilder, new MacAddress(attachedMac), null).build());

        String flowId = "UcastOut_"+segmentationId+"_"+localPort+"_"+attachedMac;
        // Add Flow Attributes
        flowBuilder.setId(new FlowId(flowId));
        FlowKey key = new FlowKey(new FlowId(flowId));
        flowBuilder.setStrict(true);
        flowBuilder.setBarrier(false);
        flowBuilder.setTableId(writeTable);
        flowBuilder.setKey(key);
        flowBuilder.setFlowName(flowId);
        flowBuilder.setHardTimeout(0);
        flowBuilder.setIdleTimeout(0);

        if (write) {
            // Instantiate the Builders for the OF Actions and Instructions
            InstructionBuilder ib = new InstructionBuilder();
            InstructionsBuilder isb = new InstructionsBuilder();

            // Instructions List Stores Individual Instructions
            List<Instruction> instructions = Lists.newArrayList();

            // Set the Output Port/Iface
            OF13MdSalInstruction.createOutputPortInstructions(ib, dpidLong, localPort);
            ib.setOrder(0);
            ib.setKey(new InstructionKey(0));
            instructions.add(ib.build());

            // Add InstructionBuilder to the Instruction(s)Builder List
            isb.setInstruction(instructions);

            // Add InstructionsBuilder to FlowBuilder
            flowBuilder.setInstructions(isb.build());
            writeFlow(flowBuilder, nodeBuilder);
        } else {
            removeFlow(flowBuilder, nodeBuilder);
        }
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

        String nodeName = OPENFLOW + dpidLong;

        MatchBuilder matchBuilder = new MatchBuilder();
        NodeBuilder nodeBuilder = createNodeBuilder(nodeName);
        FlowBuilder flowBuilder = new FlowBuilder();

        // Create the OF Match using MatchBuilder
        flowBuilder.setMatch(
                OF13MdSalMatch.createVlanIdMatch(matchBuilder, new VlanId(Integer.valueOf(segmentationId))).build());
        flowBuilder.setMatch(OF13MdSalMatch.createDestEthMatch(matchBuilder, new MacAddress(attachedMac), null).build());

        String flowId = "VlanUcastOut_"+segmentationId+"_"+localPort+"_"+attachedMac;
        // Add Flow Attributes
        flowBuilder.setId(new FlowId(flowId));
        FlowKey key = new FlowKey(new FlowId(flowId));
        flowBuilder.setStrict(true);
        flowBuilder.setBarrier(false);
        flowBuilder.setTableId(writeTable);
        flowBuilder.setKey(key);
        flowBuilder.setFlowName(flowId);
        flowBuilder.setHardTimeout(0);
        flowBuilder.setIdleTimeout(0);

        if (write) {
            // Instantiate the Builders for the OF Actions and Instructions
            InstructionBuilder ib = new InstructionBuilder();
            InstructionsBuilder isb = new InstructionsBuilder();

            // Instructions List Stores Individual Instructions
            List<Instruction> instructions = Lists.newArrayList();
            List<Instruction> instructions_tmp = Lists.newArrayList();

            /* Strip vlan and store to tmp instruction space*/
            OF13MdSalInstruction.createPopVlanInstructions(ib);
            ib.setOrder(0);
            ib.setKey(new InstructionKey(0));
            instructions_tmp.add(ib.build());

            // Set the Output Port/Iface
            ib = new InstructionBuilder();
            OF13MdSalInstruction.addOutputPortInstructions(ib, dpidLong, localPort, instructions_tmp);
            ib.setOrder(0);
            ib.setKey(new InstructionKey(0));
            instructions.add(ib.build());

            // Add InstructionBuilder to the Instruction(s)Builder List
            isb.setInstruction(instructions);

            // Add InstructionsBuilder to FlowBuilder
            flowBuilder.setInstructions(isb.build());
            writeFlow(flowBuilder, nodeBuilder);
        } else {
            removeFlow(flowBuilder, nodeBuilder);
        }
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

        String nodeName = OPENFLOW + dpidLong;

        MatchBuilder matchBuilder = new MatchBuilder();
        NodeBuilder nodeBuilder = createNodeBuilder(nodeName);
        FlowBuilder flowBuilder = new FlowBuilder();

        // Create the OF Match using MatchBuilder
        flowBuilder.setMatch(OF13MdSalMatch.createTunnelIDMatch(matchBuilder, new BigInteger(segmentationId)).build());
        flowBuilder.setMatch(OF13MdSalMatch.createDestEthMatch(matchBuilder, new MacAddress("01:00:00:00:00:00"),
                new MacAddress("01:00:00:00:00:00")).build());

        String flowId = "BcastOut_"+segmentationId;
        // Add Flow Attributes
        flowBuilder.setId(new FlowId(flowId));
        FlowKey key = new FlowKey(new FlowId(flowId));
        flowBuilder.setStrict(true);
        flowBuilder.setBarrier(false);
        flowBuilder.setTableId(writeTable);
        flowBuilder.setKey(key);
        flowBuilder.setPriority(16384);
        flowBuilder.setFlowName(flowId);
        flowBuilder.setHardTimeout(0);
        flowBuilder.setIdleTimeout(0);
        Flow flow = this.getFlow(flowBuilder, nodeBuilder);
        // Instantiate the Builders for the OF Actions and Instructions
        InstructionBuilder ib = new InstructionBuilder();
        InstructionsBuilder isb = new InstructionsBuilder();
        List<Instruction> instructions = Lists.newArrayList();
        List<Instruction> existingInstructions = null;
        if (flow != null) {
            Instructions ins = flow.getInstructions();
            if (ins != null) {
                existingInstructions = ins.getInstruction();
            }
        }

        if (write) {
            // Create output port list
            createOutputPortInstructions(ib, dpidLong, localPort, existingInstructions);
            ib.setOrder(0);
            ib.setKey(new InstructionKey(0));
            instructions.add(ib.build());

            // Add InstructionBuilder to the Instruction(s)Builder List
            isb.setInstruction(instructions);

            // Add InstructionsBuilder to FlowBuilder
            flowBuilder.setInstructions(isb.build());

            writeFlow(flowBuilder, nodeBuilder);
        } else {
            boolean flowRemove = OF13MdSalInstruction.removeOutputPortFromInstructions(ib, dpidLong, localPort,
                    existingInstructions);
            if (flowRemove) {
                /* if all ports are removed, remove flow */
                removeFlow(flowBuilder, nodeBuilder);
            } else {
                /* Install instruction with new output port list*/
                ib.setOrder(0);
                ib.setKey(new InstructionKey(0));
                instructions.add(ib.build());

                // Add InstructionBuilder to the Instruction(s)Builder List
                isb.setInstruction(instructions);

                // Add InstructionsBuilder to FlowBuilder
                flowBuilder.setInstructions(isb.build());

                writeFlow(flowBuilder, nodeBuilder);
            }
        }
    }

    /*
     * (Table:2) Local VLAN Broadcast Flood
     * Match: vlan ID and dMAC (::::FF:FF)
     * table=2,priority=16384,vlan_id=0x5,dl_dst=ff:ff:ff:ff:ff:ff \
     * actions=strip_vlan, output:2,3,4,5
     */

    private void handleLocalVlanBcastOut(Long dpidLong, Short writeTable,
                                 String segmentationId, Long localPort,
                                 boolean write) {

        String nodeName = OPENFLOW + dpidLong;

        MatchBuilder matchBuilder = new MatchBuilder();
        NodeBuilder nodeBuilder = createNodeBuilder(nodeName);
        FlowBuilder flowBuilder = new FlowBuilder();

        // Create the OF Match using MatchBuilder
        flowBuilder.setMatch(
                OF13MdSalMatch.createVlanIdMatch(matchBuilder, new VlanId(Integer.valueOf(segmentationId))).build());
        flowBuilder.setMatch(OF13MdSalMatch.createDestEthMatch(matchBuilder, new MacAddress("01:00:00:00:00:00"),
                new MacAddress("01:00:00:00:00:00")).build());

        String flowId = "VlanBcastOut_"+segmentationId;
        // Add Flow Attributes
        flowBuilder.setId(new FlowId(flowId));
        FlowKey key = new FlowKey(new FlowId(flowId));
        flowBuilder.setStrict(true);
        flowBuilder.setBarrier(false);
        flowBuilder.setTableId(writeTable);
        flowBuilder.setKey(key);
        flowBuilder.setPriority(16384);
        flowBuilder.setFlowName(flowId);
        flowBuilder.setHardTimeout(0);
        flowBuilder.setIdleTimeout(0);
        Flow flow = this.getFlow(flowBuilder, nodeBuilder);
        // Instantiate the Builders for the OF Actions and Instructions
        InstructionBuilder ib = new InstructionBuilder();
        InstructionsBuilder isb = new InstructionsBuilder();
        List<Instruction> instructions = Lists.newArrayList();
        List<Instruction> existingInstructions = null;
        boolean add_pop_vlan = true;
        if (flow != null) {
            Instructions ins = flow.getInstructions();
            if (ins != null) {
                existingInstructions = ins.getInstruction();
            }
        }

        if (write) {
            if (existingInstructions != null) {
                /* Check if pop vlan is already the first action in action list */
                List<Action> existingActions;
                for (Instruction in : existingInstructions) {
                    if (in.getInstruction() instanceof ApplyActionsCase) {
                        existingActions = (((ApplyActionsCase)
                                in.getInstruction()).getApplyActions().getAction());
                        if (existingActions.get(0).getAction() instanceof PopVlanActionCase) {
                            add_pop_vlan = false;
                            break;
                        }
                    }
                }
            } else {
                existingInstructions = Lists.newArrayList();
            }

            if (add_pop_vlan) {
                /* pop vlan */
                OF13MdSalInstruction.createPopVlanInstructions(ib);
                ib.setOrder(0);
                ib.setKey(new InstructionKey(0));
                existingInstructions.add(ib.build());
                ib = new InstructionBuilder();
            }

            // Create port list
            //createOutputGroupInstructions(nodeBuilder, ib, dpidLong, localPort, existingInstructions);
            createOutputPortInstructions(ib, dpidLong, localPort, existingInstructions);
            ib.setOrder(0);
            ib.setKey(new InstructionKey(0));
            instructions.add(ib.build());

            // Add InstructionBuilder to the Instruction(s)Builder List
            isb.setInstruction(instructions);

            // Add InstructionsBuilder to FlowBuilder
            flowBuilder.setInstructions(isb.build());

            writeFlow(flowBuilder, nodeBuilder);
        } else {
            //boolean flowRemove = removeOutputPortFromGroup(nodeBuilder, ib, dpidLong,
            //                     localPort, existingInstructions);
            boolean flowRemove = OF13MdSalInstruction.removeOutputPortFromInstructions(ib, dpidLong,
                    localPort, existingInstructions);
            if (flowRemove) {
                /* if all ports are removed, remove flow */
                removeFlow(flowBuilder, nodeBuilder);
            } else {
                /* Install instruction with new output port list*/
                ib.setOrder(0);
                ib.setKey(new InstructionKey(0));
                instructions.add(ib.build());

                // Add InstructionBuilder to the Instruction(s)Builder List
                isb.setInstruction(instructions);

                // Add InstructionsBuilder to FlowBuilder
                flowBuilder.setInstructions(isb.build());
                writeFlow(flowBuilder, nodeBuilder);
            }
        }
    }

    /*
     * (Table:1) Local Table Miss
     * Match: Any Remaining Flows w/a TunID
     * Action: Drop w/ a low priority
     * table=2,priority=8192,tun_id=0x5 actions=drop
     */

    private void handleLocalTableMiss(Long dpidLong, Short writeTable,
                             String segmentationId, boolean write) {

        String nodeName = OPENFLOW + dpidLong;

        MatchBuilder matchBuilder = new MatchBuilder();
        NodeBuilder nodeBuilder = createNodeBuilder(nodeName);
        FlowBuilder flowBuilder = new FlowBuilder();

        // Create Match(es) and Set them in the FlowBuilder Object
        flowBuilder.setMatch(OF13MdSalMatch.createTunnelIDMatch(matchBuilder, new BigInteger(segmentationId)).build());

        if (write) {
            // Create the OF Actions and Instructions
            InstructionBuilder ib = new InstructionBuilder();
            InstructionsBuilder isb = new InstructionsBuilder();

            // Instructions List Stores Individual Instructions
            List<Instruction> instructions = Lists.newArrayList();

            // Call the InstructionBuilder Methods Containing Actions
            OF13MdSalInstruction.createDropInstructions(ib);
            ib.setOrder(0);
            ib.setKey(new InstructionKey(0));
            instructions.add(ib.build());

            // Add InstructionBuilder to the Instruction(s)Builder List
            isb.setInstruction(instructions);

            // Add InstructionsBuilder to FlowBuilder
            flowBuilder.setInstructions(isb.build());
        }

        String flowId = "LocalTableMiss_"+segmentationId;
        // Add Flow Attributes
        flowBuilder.setId(new FlowId(flowId));
        FlowKey key = new FlowKey(new FlowId(flowId));
        flowBuilder.setStrict(true);
        flowBuilder.setBarrier(false);
        flowBuilder.setTableId(writeTable);
        flowBuilder.setKey(key);
        flowBuilder.setPriority(8192);
        flowBuilder.setFlowName(flowId);
        flowBuilder.setHardTimeout(0);
        flowBuilder.setIdleTimeout(0);
        if (write) {
            writeFlow(flowBuilder, nodeBuilder);
        } else {
            removeFlow(flowBuilder, nodeBuilder);
        }
    }

    /*
     * (Table:1) Local Table Miss
     * Match: Any Remaining Flows w/a VLAN ID
     * Action: Drop w/ a low priority
     * table=2,priority=8192,vlan_id=0x5 actions=drop
     */

    private void handleLocalVlanTableMiss(Long dpidLong, Short writeTable,
                                  String segmentationId, boolean write) {

        String nodeName = OPENFLOW + dpidLong;

        MatchBuilder matchBuilder = new MatchBuilder();
        NodeBuilder nodeBuilder = createNodeBuilder(nodeName);
        FlowBuilder flowBuilder = new FlowBuilder();

        // Create Match(es) and Set them in the FlowBuilder Object
        flowBuilder.setMatch(
                OF13MdSalMatch.createVlanIdMatch(matchBuilder, new VlanId(Integer.valueOf(segmentationId))).build());

        if (write) {
            // Create the OF Actions and Instructions
            InstructionBuilder ib = new InstructionBuilder();
            InstructionsBuilder isb = new InstructionsBuilder();

            // Instructions List Stores Individual Instructions
            List<Instruction> instructions = Lists.newArrayList();

            // Call the InstructionBuilder Methods Containing Actions
            OF13MdSalInstruction.createDropInstructions(ib);
            ib.setOrder(0);
            ib.setKey(new InstructionKey(0));
            instructions.add(ib.build());

            // Add InstructionBuilder to the Instruction(s)Builder List
            isb.setInstruction(instructions);

            // Add InstructionsBuilder to FlowBuilder
            flowBuilder.setInstructions(isb.build());
        }

        String flowId = "LocalTableMiss_"+segmentationId;
        // Add Flow Attributes
        flowBuilder.setId(new FlowId(flowId));
        FlowKey key = new FlowKey(new FlowId(flowId));
        flowBuilder.setStrict(true);
        flowBuilder.setBarrier(false);
        flowBuilder.setTableId(writeTable);
        flowBuilder.setKey(key);
        flowBuilder.setPriority(8192);
        flowBuilder.setFlowName(flowId);
        flowBuilder.setHardTimeout(0);
        flowBuilder.setIdleTimeout(0);
        if (write) {
            writeFlow(flowBuilder, nodeBuilder);
        } else {
            removeFlow(flowBuilder, nodeBuilder);
        }
    }

    private Group getGroup(GroupBuilder groupBuilder, NodeBuilder nodeBuilder) {
        Preconditions.checkNotNull(mdsalConsumer);
        if (mdsalConsumer == null) {
            logger.error("ERROR finding MDSAL Service. Its possible that writeFlow is called too soon ?");
            return null;
        }

        dataBroker = mdsalConsumer.getDataBroker();
        if (dataBroker == null) {
            logger.error("ERROR finding reference for DataBroker. Please check MD-SAL support on the Controller.");
            return null;
        }

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
            logger.error(e.getMessage(), e);
        }

        logger.debug("Cannot find data for Group " + groupBuilder.getGroupName());
        return null;
    }

    private void writeGroup(GroupBuilder groupBuilder, NodeBuilder nodeBuilder) {
        Preconditions.checkNotNull(mdsalConsumer);
        if (mdsalConsumer == null) {
            logger.error("ERROR finding MDSAL Service. Its possible that writeFlow is called too soon ?");
            return;
        }

        dataBroker = mdsalConsumer.getDataBroker();
        if (dataBroker == null) {
            logger.error("ERROR finding reference for DataBroker. Please check MD-SAL support on the Controller.");
            return;
        }

        ReadWriteTransaction modification = dataBroker.newReadWriteTransaction();

        // Sanity check: do not create parent's tree deeper than we should
        InstanceIdentifier<?> requiredPath = InstanceIdentifier.builder(Nodes.class).child(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory
                .rev130819.nodes.Node.class, nodeBuilder.getKey()).build();
        try {
            if (!modification.read(LogicalDatastoreType.CONFIGURATION, requiredPath).get().isPresent()) {
                logger.error("Unable to get configuration resource to store group "+groupBuilder.getGroupName()
                        +" ("+requiredPath.toString()+")");
                return;
            }
        } catch (InterruptedException|ExecutionException e) {
            logger.error(e.getMessage(), e);
            return;
        }

        InstanceIdentifier<Group> path1 = InstanceIdentifier.builder(Nodes.class).child(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory
                .rev130819.nodes.Node.class, nodeBuilder.getKey()).augmentation(FlowCapableNode.class).child(Group.class,
                new GroupKey(groupBuilder.getGroupId())).build();
        modification.put(LogicalDatastoreType.CONFIGURATION, path1, groupBuilder.build(), true /*createMissingParents*/);

        CheckedFuture<Void, TransactionCommitFailedException> commitFuture = modification.submit();
        try {
            commitFuture.get();  // TODO: Make it async (See bug 1362)
            logger.debug("Transaction success for write of Group "+groupBuilder.getGroupName());
        } catch (InterruptedException|ExecutionException e) {
            logger.error(e.getMessage(), e);
        }
    }

    private void removeGroup(GroupBuilder groupBuilder, NodeBuilder nodeBuilder) {
        Preconditions.checkNotNull(mdsalConsumer);
        if (mdsalConsumer == null) {
            logger.error("ERROR finding MDSAL Service. Its possible that writeFlow is called too soon ?");
            return;
        }

        dataBroker = mdsalConsumer.getDataBroker();
        if (dataBroker == null) {
            logger.error("ERROR finding reference for DataBroker. Please check MD-SAL support on the Controller.");
            return;
        }

        WriteTransaction modification = dataBroker.newWriteOnlyTransaction();
        InstanceIdentifier<Group> path1 = InstanceIdentifier.builder(Nodes.class).child(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory
                .rev130819.nodes.Node.class, nodeBuilder.getKey()).augmentation(FlowCapableNode.class).child(Group.class,
                new GroupKey(groupBuilder.getGroupId())).build();
        modification.delete(LogicalDatastoreType.CONFIGURATION, path1);
        CheckedFuture<Void, TransactionCommitFailedException> commitFuture = modification.submit();

        try {
            commitFuture.get();  // TODO: Make it async (See bug 1362)
            logger.debug("Transaction success for deletion of Group "+groupBuilder.getGroupName());
        } catch (InterruptedException|ExecutionException e) {
            logger.error(e.getMessage(), e);
        }
    }
    private Flow getFlow(FlowBuilder flowBuilder, NodeBuilder nodeBuilder) {
        Preconditions.checkNotNull(mdsalConsumer);
        if (mdsalConsumer == null) {
            logger.error("ERROR finding MDSAL Service. Its possible that writeFlow is called too soon ?");
            return null;
        }

        dataBroker = mdsalConsumer.getDataBroker();
        if (dataBroker == null) {
            logger.error("ERROR finding reference for DataBroker. Please check MD-SAL support on the Controller.");
            return null;
        }

        InstanceIdentifier<Flow> path1 = InstanceIdentifier.builder(Nodes.class).child(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory
                .rev130819.nodes.Node.class, nodeBuilder.getKey()).augmentation(FlowCapableNode.class).child(Table.class,
                new TableKey(flowBuilder.getTableId())).child(Flow.class, flowBuilder.getKey()).build();

        ReadOnlyTransaction readTx = dataBroker.newReadOnlyTransaction();
        try {
            Optional<Flow> data = readTx.read(LogicalDatastoreType.CONFIGURATION, path1).get();
            if (data.isPresent()) {
                return data.get();
            }
        } catch (InterruptedException|ExecutionException e) {
            logger.error(e.getMessage(), e);
        }

        logger.debug("Cannot find data for Flow " + flowBuilder.getFlowName());
        return null;
    }

    private void writeFlow(FlowBuilder flowBuilder, NodeBuilder nodeBuilder) {
        Preconditions.checkNotNull(mdsalConsumer);
        if (mdsalConsumer == null) {
            logger.error("ERROR finding MDSAL Service. Its possible that writeFlow is called too soon ?");
            return;
        }

        dataBroker = mdsalConsumer.getDataBroker();
        if (dataBroker == null) {
            logger.error("ERROR finding reference for DataBroker. Please check MD-SAL support on the Controller.");
            return;
        }

        ReadWriteTransaction modification = dataBroker.newReadWriteTransaction();

        // Sanity check: do not create parent's tree deeper than we should
        InstanceIdentifier<?> requiredPath = InstanceIdentifier.builder(Nodes.class).child(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory
                .rev130819.nodes.Node.class, nodeBuilder.getKey()).build();
        try {
            if (!modification.read(LogicalDatastoreType.CONFIGURATION, requiredPath).get().isPresent()) {
                logger.error("Unable to get configuration resource to store flow "+flowBuilder.getFlowName()
                        +" ("+requiredPath.toString()+")");
                return;
            }
        } catch (InterruptedException|ExecutionException e) {
            logger.error(e.getMessage(), e);
            return;
        }

        InstanceIdentifier<Flow> path1 = InstanceIdentifier.builder(Nodes.class).child(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory
                .rev130819.nodes.Node.class, nodeBuilder.getKey()).augmentation(FlowCapableNode.class).child(Table.class,
                new TableKey(flowBuilder.getTableId())).child(Flow.class, flowBuilder.getKey()).build();

        //modification.put(LogicalDatastoreType.OPERATIONAL, path1, flowBuilder.build());
        modification.put(LogicalDatastoreType.CONFIGURATION, path1, flowBuilder.build(), true /*createMissingParents*/);


        CheckedFuture<Void, TransactionCommitFailedException> commitFuture = modification.submit();
        try {
            commitFuture.get();  // TODO: Make it async (See bug 1362)
            logger.debug("Transaction success for write of Flow "+flowBuilder.getFlowName());
        } catch (InterruptedException|ExecutionException e) {
            logger.error(e.getMessage(), e);
        }
    }

    private void removeFlow(FlowBuilder flowBuilder, NodeBuilder nodeBuilder) {
        Preconditions.checkNotNull(mdsalConsumer);
        if (mdsalConsumer == null) {
            logger.error("ERROR finding MDSAL Service.");
            return;
        }

        dataBroker = mdsalConsumer.getDataBroker();
        if (dataBroker == null) {
            logger.error("ERROR finding reference for DataBroker. Please check MD-SAL support on the Controller.");
            return;
        }

        WriteTransaction modification = dataBroker.newWriteOnlyTransaction();
        InstanceIdentifier<Flow> path1 = InstanceIdentifier.builder(Nodes.class)
                .child(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory
                .rev130819.nodes.Node.class, nodeBuilder.getKey())
                .augmentation(FlowCapableNode.class).child(Table.class,
                new TableKey(flowBuilder.getTableId())).child(Flow.class, flowBuilder.getKey()).build();
        //modification.delete(LogicalDatastoreType.OPERATIONAL, nodeBuilderToInstanceId(nodeBuilder));
        //modification.delete(LogicalDatastoreType.OPERATIONAL, path1);
        //modification.delete(LogicalDatastoreType.CONFIGURATION, nodeBuilderToInstanceId(nodeBuilder));
        modification.delete(LogicalDatastoreType.CONFIGURATION, path1);

        CheckedFuture<Void, TransactionCommitFailedException> commitFuture = modification.submit();
        try {
            commitFuture.get();  // TODO: Make it async (See bug 1362)
            logger.debug("Transaction success for deletion of Flow "+flowBuilder.getFlowName());
        } catch (InterruptedException|ExecutionException e) {
            logger.error(e.getMessage(), e);
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
    protected InstructionBuilder createOutputPortInstructions(InstructionBuilder ib,
                                                              Long dpidLong, Long port ,
                                                              List<Instruction> instructions) {
        NodeConnectorId ncid = new NodeConnectorId(OPENFLOW + dpidLong + ":" + port);
        logger.debug("createOutputPortInstructions() Node Connector ID is - Type=openflow: DPID={} port={} existingInstructions={}", dpidLong, port, instructions);

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
        /* Create output action for this port*/
        OutputActionBuilder oab = new OutputActionBuilder();
        oab.setOutputNodeConnector(ncid);
        ab.setAction(new OutputActionCaseBuilder().setOutputAction(oab.build()).build());
        boolean addNew = true;

        /* Find the group action and get the group */
        for (Action action : actionList) {
            if (action.getAction() instanceof OutputActionCase) {
                OutputActionCase opAction = (OutputActionCase)action.getAction();
                /* If output port action already in the action list of one of the buckets, skip */
                if (opAction.getOutputAction().getOutputNodeConnector().equals(new Uri(ncid))) {
                    addNew = false;
                    break;
                }
            }
        }
        if (addNew) {
            ab.setOrder(actionList.size());
            ab.setKey(new ActionKey(actionList.size()));
            actionList.add(ab.build());
        }
        // Create an Apply Action
        ApplyActionsBuilder aab = new ApplyActionsBuilder();
        aab.setAction(actionList);
        ib.setInstruction(new ApplyActionsCaseBuilder().setApplyActions(aab.build()).build());
        logger.debug("createOutputPortInstructions() : applyAction {}", aab.build());
        return ib;
    }

    /**
     * Create Output Port Group Instruction
     *
     * @param ib       Map InstructionBuilder without any instructions
     * @param dpidLong Long the datapath ID of a switch/node
     * @param port     Long representing a port on a switch/node
     * @return ib InstructionBuilder Map with instructions
     */
    protected InstructionBuilder createOutputGroupInstructions(NodeBuilder nodeBuilder,
                                                               InstructionBuilder ib,
                                                               Long dpidLong, Long port ,
                                                               List<Instruction> instructions) {
        NodeConnectorId ncid = new NodeConnectorId(OPENFLOW + dpidLong + ":" + port);
        logger.debug("createOutputGroupInstructions() Node Connector ID is - Type=openflow: DPID={} port={} existingInstructions={}", dpidLong, port, instructions);

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
        logger.debug("createOutputGroupInstructions(): output action {}", ab.build());
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
                logger.debug("createOutputGroupInstructions: group {}", group);
                break;
            }
        }

        logger.debug("createOutputGroupInstructions: groupActionAdded {}", groupActionAdded);
        if (groupActionAdded) {
            /* modify the action bucket in group */
            groupBuilder = new GroupBuilder(group);
            Buckets buckets = groupBuilder.getBuckets();
            for (Bucket bucket : buckets.getBucket()) {
                List<Action> bucketActions = bucket.getAction();
                logger.debug("createOutputGroupInstructions: bucketActions {}", bucketActions);
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
            logger.debug("createOutputGroupInstructions: addNew {}", addNew);
            if (addNew) {
                /* the new output action is not in the bucket, add to bucket */
                if (!buckets.getBucket().isEmpty()) {
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
                    logger.debug("createOutputGroupInstructions: bucketList {}", bucketList);
                }
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
        logger.debug("createOutputGroupInstructions: group {}", groupBuilder.build());
        logger.debug("createOutputGroupInstructions: actionList {}", actionList);

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
    protected boolean removeOutputPortFromGroup(NodeBuilder nodeBuilder, InstructionBuilder ib,
                                Long dpidLong, Long port , List<Instruction> instructions) {

        NodeConnectorId ncid = new NodeConnectorId(OPENFLOW + dpidLong + ":" + port);
        logger.debug("removeOutputPortFromGroup() Node Connector ID is - Type=openflow: DPID={} port={} existingInstructions={}", dpidLong, port, instructions);

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
                logger.debug("removeOutputPortFromGroup: bucketList {}", bucketList);

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
        Preconditions.checkNotNull(connectionService);
        List<Node> ovsNodes = connectionService.getNodes();
        if (ovsNodes == null) return;
        for (Node ovsNode : ovsNodes) {
            Long brIntDpid = this.getIntegrationBridgeOFDPID(ovsNode);
            Long brExDpid = this.getExternalBridgeDpid(ovsNode);
            logger.debug("Compare openflowNode to OVS node {} vs {} and {}", openflowNode.getID(), brIntDpid, brExDpid);
            String openflowID = openflowNode.getID().toString();
            if (openflowID.contains(brExDpid.toString())) {
                this.initializeFlowRules(ovsNode, configurationService.getExternalBridgeName());
                this.triggerInterfaceUpdates(ovsNode);
            }
            if (openflowID.contains(brIntDpid.toString())) {
                this.initializeFlowRules(ovsNode, configurationService.getIntegrationBridgeName());
                this.triggerInterfaceUpdates(ovsNode);
            }
        }
    }

    private NodeBuilder createNodeBuilder(String nodeId) {
        NodeBuilder builder = new NodeBuilder();
        builder.setId(new NodeId(nodeId));
        builder.setKey(new NodeKey(builder.getId()));
        return builder;
    }

    private InstanceIdentifier<org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node> nodeBuilderToInstanceId(NodeBuilder
                                                                                                                                             node) {
        return InstanceIdentifier.builder(Nodes.class).child(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node.class,
                node.getKey()).toInstance();
    }

    private String getInternalBridgeUUID (Node node, String bridgeName) {
        Preconditions.checkNotNull(ovsdbConfigurationService);
        try {
            Map<String, Row> bridgeTable = ovsdbConfigurationService.getRows(node, ovsdbConfigurationService.getTableName(node, Bridge.class));
            if (bridgeTable == null) return null;
            for (String key : bridgeTable.keySet()) {
                Bridge bridge = ovsdbConfigurationService.getTypedRow(node, Bridge.class, bridgeTable.get(key));
                if (bridge.getName().equals(bridgeName)) return key;
            }
        } catch (Exception e) {
            logger.error("Error getting Bridge Identifier for {} / {}", node, bridgeName, e);
        }
        return null;
    }
}
