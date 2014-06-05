/**
 * Copyright (C) 2013 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Madhu Venugopal, Brent Salisbury, Dave Tucker
 */
package org.opendaylight.ovsdb.neutron.provider;

import java.math.BigInteger;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.md.sal.common.api.data.DataModification;
import org.opendaylight.controller.networkconfig.neutron.NeutronNetwork;
import org.opendaylight.controller.sal.binding.api.data.DataBrokerService;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.utils.HexEncode;
import org.opendaylight.controller.sal.utils.ServiceHelper;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.controller.sal.utils.StatusCode;
import org.opendaylight.controller.switchmanager.ISwitchManager;
import org.opendaylight.ovsdb.lib.notation.OvsDBMap;
import org.opendaylight.ovsdb.lib.notation.OvsDBSet;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.table.Bridge;
import org.opendaylight.ovsdb.lib.table.Interface;
import org.opendaylight.ovsdb.lib.table.Port;
import org.opendaylight.ovsdb.neutron.IAdminConfigManager;
import org.opendaylight.ovsdb.neutron.IInternalNetworkManager;
import org.opendaylight.ovsdb.neutron.IMDSALConsumer;
import org.opendaylight.ovsdb.neutron.NetworkHandler;
import org.opendaylight.ovsdb.neutron.ITenantNetworkManager;
import org.opendaylight.ovsdb.plugin.IConnectionServiceInternal;
import org.opendaylight.ovsdb.plugin.OVSDBConfigService;
import org.opendaylight.ovsdb.plugin.StatusWithUuid;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.DecNwTtlCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.DropActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.GroupActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.GroupActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.OutputActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.OutputActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.PopVlanActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.PopVlanActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetFieldCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetNwDstActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetNwSrcActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetVlanIdActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.PushVlanActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.dec.nw.ttl._case.DecNwTtl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.dec.nw.ttl._case.DecNwTtlBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.drop.action._case.DropAction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.drop.action._case.DropActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.group.action._case.GroupActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.output.action._case.OutputActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.push.vlan.action._case.PushVlanActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.pop.vlan.action._case.PopVlanActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.field._case.SetFieldBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.nw.dst.action._case.SetNwDstActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.nw.src.action._case.SetNwSrcActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.vlan.id.action._case.SetVlanIdActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.address.address.Ipv4Builder;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.GoToTableCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.apply.actions._case.ApplyActionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.go.to.table._case.GoToTableBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.BucketId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.GroupId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.GroupTypes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.BucketsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.buckets.Bucket;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.buckets.BucketBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.buckets.BucketKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.Group;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.GroupBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.groups.GroupKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.group.types.rev131018.group.Buckets;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.EtherType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.VlanId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.arp.match.fields.ArpSourceHardwareAddressBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.arp.match.fields.ArpTargetHardwareAddressBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.ethernet.match.fields.EthernetDestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.ethernet.match.fields.EthernetSourceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.ethernet.match.fields.EthernetTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.EthernetMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.Icmpv4MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.IpMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.TunnelBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.VlanMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.ArpMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.Ipv4MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._4.match.TcpMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._4.match.UdpMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.vlan.match.fields.VlanIdBuilder;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 *
 */
public class OF13Provider implements NetworkProvider {
    private static final Logger logger = LoggerFactory.getLogger(OF13Provider.class);
    private DataBrokerService dataBrokerService;
    private static final short TABLE_0_DEFAULT_INGRESS = 0;
    private static final short TABLE_1_ISOLATE_TENANT = 10;
    private static final short TABLE_2_LOCAL_FORWARD = 20;
    private static Long groupId = 1L;

    private IAdminConfigManager adminConfigManager;
    private IInternalNetworkManager internalNetworkManager;
    private ITenantNetworkManager tenantNetworkManager;

    public OF13Provider(IAdminConfigManager adminConfigManager,
                        IInternalNetworkManager internalNetworkManager,
                        ITenantNetworkManager tenantNetworkManager) {
        this.adminConfigManager = adminConfigManager;
        this.internalNetworkManager = internalNetworkManager;
        this.tenantNetworkManager = tenantNetworkManager;
    }

    @Override
    public boolean hasPerTenantTunneling() {
        return false;
    }

    private Status getTunnelReadinessStatus (Node node, String tunnelKey) {
        InetAddress srcTunnelEndPoint = adminConfigManager.getTunnelEndPoint(node);
        if (srcTunnelEndPoint == null) {
            logger.error("Tunnel Endpoint not configured for Node {}", node);
            return new Status(StatusCode.NOTFOUND, "Tunnel Endpoint not configured for "+ node);
        }

        if (!internalNetworkManager.isInternalNetworkNeutronReady(node)) {
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

    private String getPortUuid(Node node, String name, String bridgeUUID) throws Exception {
        OVSDBConfigService ovsdbTable = (OVSDBConfigService)ServiceHelper.getGlobalInstance(OVSDBConfigService.class, this);
        Bridge bridge = (Bridge)ovsdbTable.getRow(node, Bridge.NAME.getName(), bridgeUUID);
        if (bridge != null) {
            Set<UUID> ports = bridge.getPorts();
            for (UUID portUUID : ports) {
                Port port = (Port)ovsdbTable.getRow(node, Port.NAME.getName(), portUUID.toString());
                if (port != null && port.getName().equalsIgnoreCase(name)) return portUUID.toString();
            }
        }
        return null;
    }

    private Status addTunnelPort (Node node, String tunnelType, InetAddress src, InetAddress dst) {
        try {
            String bridgeUUID = null;
            String tunnelBridgeName = adminConfigManager.getIntegrationBridgeName();
            OVSDBConfigService ovsdbTable = (OVSDBConfigService)ServiceHelper.getGlobalInstance(OVSDBConfigService.class, this);
            Map<String, org.opendaylight.ovsdb.lib.table.internal.Table<?>> bridgeTable = ovsdbTable.getRows(node, Bridge.NAME.getName());
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
            String portName = getTunnelName(tunnelType, dst);

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
            options.put("key", "flow");
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

    /* delete port from ovsdb port table */
    private Status deletePort(Node node, String bridgeName, String portName) {
        try {
            String bridgeUUID = null;
            OVSDBConfigService ovsdbTable = (OVSDBConfigService)ServiceHelper.getGlobalInstance(OVSDBConfigService.class, this);
            Map<String, org.opendaylight.ovsdb.lib.table.internal.Table<?>> bridgeTable = ovsdbTable.getRows(node, Bridge.NAME.getName());
            if (bridgeTable != null) {
                for (String uuid : bridgeTable.keySet()) {
                    Bridge bridge = (Bridge)bridgeTable.get(uuid);
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
               status = ovsdbTable.deleteRow(node, Port.NAME.getName(), portUUID);
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
        String tunnelBridgeName = adminConfigManager.getIntegrationBridgeName();
        String portName = getTunnelName(tunnelType, dst);
        Status status = deletePort(node, tunnelBridgeName, portName);
        return status;
    }

    private Status deletePhysicalPort(Node node, String phyIntfName) {
        String intBridgeName = adminConfigManager.getIntegrationBridgeName();
        Status status = deletePort(node, intBridgeName, phyIntfName);
        return status;
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
    private Long getDpidFromNodeAndBridgeUUID (Node node, String bridgeUuid) {
        try {
            OVSDBConfigService ovsdbTable = (OVSDBConfigService) ServiceHelper.getGlobalInstance(OVSDBConfigService.class, this);
            Bridge bridge = (Bridge) ovsdbTable.getRow(node, Bridge.NAME.getName(), bridgeUuid);
            Set<String> dpids = bridge.getDatapath_id();
            if (dpids == null || dpids.size() == 0) return 0L;
            return Long.valueOf(HexEncode.stringToLong((String) dpids.toArray()[0]));
        } catch (Exception e) {
            logger.error("Error finding Bridge's OF DPID", e);
            return 0L;
        }
    }

    private Long getIntegrationBridgeOFDPID (Node node) {
        try {
            String bridgeName = adminConfigManager.getIntegrationBridgeName();
            String brUuid = this.getInternalBridgeUUID(node, bridgeName);
            if (brUuid == null) {
                logger.error("Unable to spot Bridge Identifier for {} in {}", bridgeName, node);
                return 0L;
            }

            return getDpidFromNodeAndBridgeUUID(node, brUuid);
        } catch (Exception e) {
            logger.error("Error finding Integration Bridge's OF DPID", e);
            return 0L;
        }
    }

    private Long getExternalBridgeOFDPID (Node node) {
        try {
            String bridgeName = adminConfigManager.getExternalBridgeName();
            String brUuid = this.getInternalBridgeUUID(node, bridgeName);
            if (brUuid == null) {
                logger.error("Unable to spot Bridge Identifier for {} in {}", bridgeName, node);
                return 0L;
            }

            return getDpidFromNodeAndBridgeUUID(node, brUuid);
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

            Set<BigInteger> of_ports = intf.getOfport();
            if (of_ports == null || of_ports.size() <= 0) {
                logger.error("Could NOT Identify OF value for port {} on {}", intf.getName(), node);
                return;
            }
            long localPort = ((BigInteger)of_ports.toArray()[0]).longValue();

            Map<String, String> externalIds = intf.getExternal_ids();
            if (externalIds == null) {
                logger.error("No external_ids seen in {}", intf);
                return;
            }

            String attachedMac = externalIds.get(ITenantNetworkManager.EXTERNAL_ID_VM_MAC);
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

            Set<BigInteger> of_ports = intf.getOfport();
            if (of_ports == null || of_ports.size() <= 0) {
                logger.error("Could NOT Identify OF value for port {} on {}", intf.getName(), node);
                return;
            }
            long localPort = ((BigInteger)of_ports.toArray()[0]).longValue();

            Map<String, String> externalIds = intf.getExternal_ids();
            if (externalIds == null) {
                logger.error("No external_ids seen in {}", intf);
                return;
            }

            String attachedMac = externalIds.get(ITenantNetworkManager.EXTERNAL_ID_VM_MAC);
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
        try {

            Long dpid = this.getIntegrationBridgeOFDPID(node);
            if (dpid == 0L) {
                logger.debug("Openflow Datapath-ID not set for the integration bridge in {}", node);
                return;
            }
            OVSDBConfigService ovsdbTable = (OVSDBConfigService) ServiceHelper.getGlobalInstance(OVSDBConfigService.class, this);

            Set<BigInteger> of_ports = intf.getOfport();
            if (of_ports == null || of_ports.size() <= 0) {
                logger.error("Could NOT Identify OF value for port {} on {}", intf.getName(), node);
                return;
            }
            long localPort = ((BigInteger)of_ports.toArray()[0]).longValue();

            Map<String, String> externalIds = intf.getExternal_ids();
            if (externalIds == null) {
                logger.error("No external_ids seen in {}", intf);
                return;
            }

            String attachedMac = externalIds.get(ITenantNetworkManager.EXTERNAL_ID_VM_MAC);
            if (attachedMac == null) {
                logger.error("No AttachedMac seen in {}", intf);
                return;
            }

            Map<String, org.opendaylight.ovsdb.lib.table.internal.Table<?>> intfs = ovsdbTable.getRows(node, Interface.NAME.getName());
            if (intfs != null) {
                for (org.opendaylight.ovsdb.lib.table.internal.Table<?> row : intfs.values()) {
                    Interface tunIntf = (Interface)row;
                    if (tunIntf.getName().equals(this.getTunnelName(tunnelType, dst))) {
                        of_ports = tunIntf.getOfport();
                        if (of_ports == null || of_ports.size() <= 0) {
                            logger.error("Could NOT Identify Tunnel port {} on {}", tunIntf.getName(), node);
                            continue;
                        }
                        long tunnelOFPort = ((BigInteger)of_ports.toArray()[0]).longValue();

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
        try {

            Long dpid = this.getIntegrationBridgeOFDPID(node);
            if (dpid == 0L) {
                logger.debug("Openflow Datapath-ID not set for the integration bridge in {}", node);
                return;
            }
            OVSDBConfigService ovsdbTable = (OVSDBConfigService) ServiceHelper.getGlobalInstance(OVSDBConfigService.class, this);

            Set<BigInteger> of_ports = intf.getOfport();
            if (of_ports == null || of_ports.size() <= 0) {
                logger.error("Could NOT Identify OF value for port {} on {}", intf.getName(), node);
                return;
            }
            long localPort = ((BigInteger)of_ports.toArray()[0]).longValue();

            Map<String, String> externalIds = intf.getExternal_ids();
            if (externalIds == null) {
                logger.error("No external_ids seen in {}", intf);
                return;
            }

            String attachedMac = externalIds.get(ITenantNetworkManager.EXTERNAL_ID_VM_MAC);
            if (attachedMac == null) {
                logger.error("No AttachedMac seen in {}", intf);
                return;
            }

            Map<String, org.opendaylight.ovsdb.lib.table.internal.Table<?>> intfs = ovsdbTable.getRows(node, Interface.NAME.getName());
            if (intfs != null) {
                for (org.opendaylight.ovsdb.lib.table.internal.Table<?> row : intfs.values()) {
                    Interface tunIntf = (Interface)row;
                    if (tunIntf.getName().equals(this.getTunnelName(tunnelType, dst))) {
                        of_ports = tunIntf.getOfport();
                        if (of_ports == null || of_ports.size() <= 0) {
                            logger.error("Could NOT Identify Tunnel port {} on {}", tunIntf.getName(), node);
                            continue;
                        }
                        long tunnelOFPort = ((BigInteger)of_ports.toArray()[0]).longValue();

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
        logger.debug("Program vlan rules for interface {}", intf.getName());

        try {

            Long dpid = this.getIntegrationBridgeOFDPID(node);
            if (dpid == 0L) {
                logger.debug("Openflow Datapath-ID not set for the integration bridge in {}", node);
                return;
            }
            OVSDBConfigService ovsdbTable = (OVSDBConfigService) ServiceHelper.getGlobalInstance(OVSDBConfigService.class, this);

            Set<BigInteger> of_ports = intf.getOfport();
            int timeout = 6;
            while ((of_ports == null) && (timeout > 0)) {
                of_ports = intf.getOfport();
                if (of_ports == null || of_ports.size() <= 0) {
                    // Wait for the OVSDB update to sync up the Local cache.
                    Thread.sleep(500);
                    timeout--;
                    continue;
                }
            }
            if (of_ports == null || of_ports.size() <= 0) {
                logger.error("Could NOT Identify OF value for port {} on {}", intf.getName(), node);
                return;
            }

            Map<String, String> externalIds = intf.getExternal_ids();
            if (externalIds == null) {
                logger.error("No external_ids seen in {}", intf);
                return;
            }

            String attachedMac = externalIds.get(tenantNetworkManager.EXTERNAL_ID_VM_MAC);
            if (attachedMac == null) {
                logger.error("No AttachedMac seen in {}", intf);
                return;
            }

            Map<String, org.opendaylight.ovsdb.lib.table.internal.Table<?>> intfs = ovsdbTable.getRows(node, Interface.NAME.getName());
            if (intfs != null) {
                for (org.opendaylight.ovsdb.lib.table.internal.Table<?> row : intfs.values()) {
                    Interface ethIntf = (Interface)row;
                    if (ethIntf.getName().equalsIgnoreCase(adminConfigManager.getPhysicalInterfaceName(node, network.getProviderPhysicalNetwork()))) {
                        of_ports = ethIntf.getOfport();
                        timeout = 6;
                        while ((of_ports == null) && (timeout > 0)) {
                            of_ports = ethIntf.getOfport();
                            if (of_ports == null || of_ports.size() <= 0) {
                                // Wait for the OVSDB update to sync up the Local cache.
                                Thread.sleep(500);
                                timeout--;
                                continue;
                            }
                        }

                        if (of_ports == null || of_ports.size() <= 0) {
                            logger.error("Could NOT Identify eth port {} on {}", ethIntf.getName(), node);
                            continue;
                        }
                        long ethOFPort = ((BigInteger)of_ports.toArray()[0]).longValue();

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
        logger.debug("Remove vlan rules for interface {}", intf.getName());

        try {

            Long dpid = this.getIntegrationBridgeOFDPID(node);
            if (dpid == 0L) {
                logger.debug("Openflow Datapath-ID not set for the integration bridge in {}", node);
                return;
            }
            OVSDBConfigService ovsdbTable = (OVSDBConfigService) ServiceHelper.getGlobalInstance(OVSDBConfigService.class, this);

            Set<BigInteger> of_ports = intf.getOfport();
            if (of_ports == null || of_ports.size() <= 0) {
                logger.error("Could NOT Identify OF value for port {} on {}", intf.getName(), node);
                return;
            }

            Map<String, String> externalIds = intf.getExternal_ids();
            if (externalIds == null) {
                logger.error("No external_ids seen in {}", intf);
                return;
            }

            String attachedMac = externalIds.get(tenantNetworkManager.EXTERNAL_ID_VM_MAC);
            if (attachedMac == null) {
                logger.error("No AttachedMac seen in {}", intf);
                return;
            }

            Map<String, org.opendaylight.ovsdb.lib.table.internal.Table<?>> intfs = ovsdbTable.getRows(node, Interface.NAME.getName());
            if (intfs != null) {
                for (org.opendaylight.ovsdb.lib.table.internal.Table<?> row : intfs.values()) {
                    Interface ethIntf = (Interface)row;
                    if (ethIntf.getName().equalsIgnoreCase(adminConfigManager.getPhysicalInterfaceName(node,
                                                                   network.getProviderPhysicalNetwork()))) {
                        of_ports = ethIntf.getOfport();
                        if (of_ports == null || of_ports.size() <= 0) {
                            logger.error("Could NOT Identify eth port {} on {}", ethIntf.getName(), node);
                            continue;
                        }
                        long ethOFPort = ((BigInteger)of_ports.toArray()[0]).longValue();

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
        ISwitchManager switchManager = (ISwitchManager) ServiceHelper.getInstance(ISwitchManager.class, "default", this);
        if (switchManager == null) {
            logger.error("Unable to identify SwitchManager");
        } else {
            Long dpid = this.getIntegrationBridgeOFDPID(srcNode);
            if (dpid == 0L) {
                logger.debug("Openflow Datapath-ID not set for the integration bridge in {}", srcNode);
                return new Status(StatusCode.NOTFOUND);
            }
            Set<Node> ofNodes = switchManager.getNodes();
            boolean ofNodeFound = false;
            if (ofNodes != null) {
                for (Node ofNode : ofNodes) {
                    if (ofNode.toString().contains(dpid+"")) {
                        logger.debug("Identified the Openflow node via toString {}", ofNode);
                        ofNodeFound = true;
                        break;
                    }
                }
            } else {
                logger.error("Unable to find any Node from SwitchManager");
            }
            if (!ofNodeFound) {
                logger.error("Unable to find OF Node for {} with update {} on node {}", dpid, intf, srcNode);
                return new Status(StatusCode.NOTFOUND);
            }
        }

        IConnectionServiceInternal connectionService = (IConnectionServiceInternal)ServiceHelper.getGlobalInstance(IConnectionServiceInternal.class, this);
        List<Node> nodes = connectionService.getNodes();
        nodes.remove(srcNode);
        this.programLocalRules(network.getProviderNetworkType(), network.getProviderSegmentationID(), srcNode, intf);

        if (network.getProviderNetworkType().equalsIgnoreCase(NetworkHandler.NETWORK_TYPE_VLAN)) {
            this.programVlanRules(network, srcNode, intf);
        } else if (network.getProviderNetworkType().equalsIgnoreCase(NetworkHandler.NETWORK_TYPE_GRE)
                   || network.getProviderNetworkType().equalsIgnoreCase(NetworkHandler.NETWORK_TYPE_VXLAN)){
            for (Node dstNode : nodes) {
                InetAddress src = adminConfigManager.getTunnelEndPoint(srcNode);
                InetAddress dst = adminConfigManager.getTunnelEndPoint(dstNode);
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
        try {
            OVSDBConfigService ovsdbTable = (OVSDBConfigService)ServiceHelper.getGlobalInstance(OVSDBConfigService.class, this);
            Map<String, org.opendaylight.ovsdb.lib.table.internal.Table<?>> intfs = ovsdbTable.getRows(node, Interface.NAME.getName());
            if (intfs != null) {
                for (org.opendaylight.ovsdb.lib.table.internal.Table<?> row : intfs.values()) {
                    Interface intf = (Interface)row;
                    NeutronNetwork network = tenantNetworkManager.getTenantNetworkForInterface(intf);
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
        Status status = new Status(StatusCode.SUCCESS);
        IConnectionServiceInternal connectionService = (IConnectionServiceInternal)ServiceHelper.getGlobalInstance(IConnectionServiceInternal.class, this);
        List<Node> nodes = connectionService.getNodes();
        nodes.remove(srcNode);

        logger.info("Delete intf " + intf.getName() + " isLastInstanceOnNode " + isLastInstanceOnNode);
        List<String> phyIfName = adminConfigManager.getAllPhysicalInterfaceNames(srcNode);
        if (intf.getType().equalsIgnoreCase(NetworkHandler.NETWORK_TYPE_VXLAN)
            || intf.getType().equalsIgnoreCase(NetworkHandler.NETWORK_TYPE_GRE)) {
            /* Delete tunnel port */
            try {
                OvsDBMap<String, String> options = intf.getOptions();
                InetAddress src = InetAddress.getByName(options.get("local_ip"));
                InetAddress dst = InetAddress.getByName(options.get("remote_ip"));
                status = deleteTunnelPort(srcNode, intf.getType(), src, dst);
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
                    InetAddress src = adminConfigManager.getTunnelEndPoint(srcNode);
                    InetAddress dst = adminConfigManager.getTunnelEndPoint(dstNode);
                    logger.info("Remove tunnel rules for interface " + intf.getName() + " on srcNode" + srcNode.getNodeIDString());
                    this.removeTunnelRules(tunnelType, network.getProviderSegmentationID(),
                                           dst, srcNode, intf, true, isLastInstanceOnNode);
                    logger.info("Remove tunnel rules for interface " + intf.getName() + " on dstNode" + dstNode.getNodeIDString());
                    this.removeTunnelRules(tunnelType, network.getProviderSegmentationID(),
                                           src, dstNode, intf, false, isLastInstanceOnNode);
                }
            }
        }
        return status;
    }

    @Override
    public void initializeFlowRules(Node node) {
        this.initializeFlowRules(node, adminConfigManager.getIntegrationBridgeName());
        this.initializeFlowRules(node, adminConfigManager.getExternalBridgeName());
        this.triggerInterfaceUpdates(node);
    }

    /**
     * @param node
     * @param bridgeName
     */
    private void initializeFlowRules(Node node, String bridgeName) {
        String brUuid = this.getInternalBridgeUUID(node, bridgeName);
        if (brUuid == null) {
            if (bridgeName == adminConfigManager.getExternalBridgeName()){
                logger.debug("Failed to initialize Flow Rules for bridge {} on node {}. Is the Neutron L3 agent running on this node?");
            }
            else {
                logger.debug("Failed to initialize Flow Rules for bridge {} on node {}", bridgeName, node);
            }
            return;
        }

        Long dpid = getDpidFromNodeAndBridgeUUID(node, brUuid);

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
         if (bridgeName == adminConfigManager.getExternalBridgeName()) {
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

        String nodeName = "openflow:" + dpidLong;
        EtherType etherType = new EtherType(0x88CCL);

        MatchBuilder matchBuilder = new MatchBuilder();
        NodeBuilder nodeBuilder = createNodeBuilder(nodeName);
        FlowBuilder flowBuilder = new FlowBuilder();

        // Create Match(es) and Set them in the FlowBuilder Object
        flowBuilder.setMatch(createEtherTypeMatch(matchBuilder, etherType).build());

        // Create the OF Actions and Instructions
        InstructionBuilder ib = new InstructionBuilder();
        InstructionsBuilder isb = new InstructionsBuilder();

        // Instructions List Stores Individual Instructions
        List<Instruction> instructions = new ArrayList<Instruction>();

        // Call the InstructionBuilder Methods Containing Actions
        createSendToControllerInstructions(ib);
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

        String nodeName = "openflow:" + dpidLong;

        MatchBuilder matchBuilder = new MatchBuilder();
        NodeBuilder nodeBuilder = createNodeBuilder(nodeName);
        FlowBuilder flowBuilder = new FlowBuilder();

        // Create the OF Actions and Instructions
        InstructionBuilder ib = new InstructionBuilder();
        InstructionsBuilder isb = new InstructionsBuilder();

        // Instructions List Stores Individual Instructions
        List<Instruction> instructions = new ArrayList<Instruction>();

        // Call the InstructionBuilder Methods Containing Actions
        createNormalInstructions(ib);
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

        String nodeName = "openflow:" + dpidLong;

        BigInteger tunnelId = new BigInteger(segmentationId);
        MatchBuilder matchBuilder = new MatchBuilder();
        NodeBuilder nodeBuilder = createNodeBuilder(nodeName);
        FlowBuilder flowBuilder = new FlowBuilder();

        // Create Match(es) and Set them in the FlowBuilder Object
        flowBuilder.setMatch(createTunnelIDMatch(matchBuilder, tunnelId).build());
        flowBuilder.setMatch(createInPortMatch(matchBuilder, dpidLong, ofPort).build());

        if (write) {
            // Create the OF Actions and Instructions
            InstructionBuilder ib = new InstructionBuilder();
            InstructionsBuilder isb = new InstructionsBuilder();

            // Instructions List Stores Individual Instructions
            List<Instruction> instructions = new ArrayList<Instruction>();

            // Call the InstructionBuilder Methods Containing Actions
            createGotoTableInstructions(ib, goToTableId);
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

        String nodeName = "openflow:" + dpidLong;

        MatchBuilder matchBuilder = new MatchBuilder();
        NodeBuilder nodeBuilder = createNodeBuilder(nodeName);
        FlowBuilder flowBuilder = new FlowBuilder();

        // Create Match(es) and Set them in the FlowBuilder Object
        flowBuilder.setMatch(createVlanIdMatch(matchBuilder, new VlanId(Integer.valueOf(segmentationId))).build());
        flowBuilder.setMatch(createInPortMatch(matchBuilder, dpidLong, ethPort).build());

        if (write) {
            // Create the OF Actions and Instructions
            InstructionBuilder ib = new InstructionBuilder();
            InstructionsBuilder isb = new InstructionsBuilder();

            // Instructions List Stores Individual Instructions
            List<Instruction> instructions = new ArrayList<Instruction>();

            // Call the InstructionBuilder Methods Containing Actions
            createGotoTableInstructions(ib, goToTableId);
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

        String nodeName = "openflow:" + dpidLong;

        MatchBuilder matchBuilder = new MatchBuilder();
        NodeBuilder nodeBuilder = createNodeBuilder(nodeName);
        FlowBuilder flowBuilder = new FlowBuilder();

        // Create the OF Match using MatchBuilder
        flowBuilder.setMatch(createEthSrcMatch(matchBuilder, new MacAddress(attachedMac)).build());
        // TODO Broken In_Port Match
        flowBuilder.setMatch(createInPortMatch(matchBuilder, dpidLong, inPort).build());

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
            List<Instruction> instructions = new ArrayList<Instruction>();

            // GOTO Instuctions Need to be added first to the List
            createGotoTableInstructions(ib, goToTableId);
            ib.setOrder(0);
            ib.setKey(new InstructionKey(0));
            instructions.add(ib.build());
            // TODO Broken SetTunID
            createSetTunnelIdInstructions(ib, new BigInteger(segmentationId));
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

         String nodeName = "openflow:" + dpidLong;

         MatchBuilder matchBuilder = new MatchBuilder();
         NodeBuilder nodeBuilder = createNodeBuilder(nodeName);
         FlowBuilder flowBuilder = new FlowBuilder();

         // Create the OF Match using MatchBuilder
         flowBuilder.setMatch(createEthSrcMatch(matchBuilder, new MacAddress(attachedMac)).build());
         // TODO Broken In_Port Match
         flowBuilder.setMatch(createInPortMatch(matchBuilder, dpidLong, inPort).build());

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
             List<Instruction> instructions = new ArrayList<Instruction>();

             // GOTO Instructions Need to be added first to the List
             createGotoTableInstructions(ib, goToTableId);
             ib.setOrder(0);
             ib.setKey(new InstructionKey(0));
             instructions.add(ib.build());
             // Set VLAN ID Instruction
             createSetVlanInstructions(ib, new VlanId(Integer.valueOf(segmentationId)));
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

        String nodeName = "openflow:" + dpidLong;

        MatchBuilder matchBuilder = new MatchBuilder();
        NodeBuilder nodeBuilder = createNodeBuilder(nodeName);
        FlowBuilder flowBuilder = new FlowBuilder();

        // Create the OF Match using MatchBuilder
        flowBuilder.setMatch(createInPortMatch(matchBuilder, dpidLong, inPort).build());

        if (write) {
            // Instantiate the Builders for the OF Actions and Instructions
            InstructionBuilder ib = new InstructionBuilder();
            InstructionsBuilder isb = new InstructionsBuilder();

            // Instructions List Stores Individual Instructions
            List<Instruction> instructions = new ArrayList<Instruction>();

            // Call the InstructionBuilder Methods Containing Actions
            createDropInstructions(ib);
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
        flowBuilder.setTableId((short) 0);
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

        String nodeName = "openflow:" + dpidLong;

        MatchBuilder matchBuilder = new MatchBuilder();
        NodeBuilder nodeBuilder = createNodeBuilder(nodeName);
        FlowBuilder flowBuilder = new FlowBuilder();

        // Create the OF Match using MatchBuilder
        flowBuilder.setMatch(createTunnelIDMatch(matchBuilder, new BigInteger(segmentationId)).build());
        flowBuilder.setMatch(createDestEthMatch(matchBuilder, new MacAddress(attachedMac), null).build());

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
            List<Instruction> instructions = new ArrayList<Instruction>();

            // GOTO Instuctions
            createGotoTableInstructions(ib, goToTableId);
            ib.setOrder(0);
            ib.setKey(new InstructionKey(0));
            instructions.add(ib.build());
            // Set the Output Port/Iface
            createOutputPortInstructions(ib, dpidLong, OFPortOut);
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

         String nodeName = "openflow:" + dpidLong;

         MatchBuilder matchBuilder = new MatchBuilder();
         NodeBuilder nodeBuilder = createNodeBuilder(nodeName);
         FlowBuilder flowBuilder = new FlowBuilder();

         // Create the OF Match using MatchBuilder
         flowBuilder.setMatch(createVlanIdMatch(matchBuilder, new VlanId(Integer.valueOf(segmentationId))).build());
         flowBuilder.setMatch(createDestEthMatch(matchBuilder, new MacAddress(attachedMac), null).build());

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
             List<Instruction> instructions = new ArrayList<Instruction>();

             // GOTO Instuctions
             createGotoTableInstructions(ib, goToTableId);
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

        String nodeName = "openflow:" + dpidLong;

        MatchBuilder matchBuilder = new MatchBuilder();
        NodeBuilder nodeBuilder = createNodeBuilder(nodeName);
        FlowBuilder flowBuilder = new FlowBuilder();

        // Create the OF Match using MatchBuilder
        // Match TunnelID
        flowBuilder.setMatch(createTunnelIDMatch(matchBuilder, new BigInteger(segmentationId)).build());
        // Match DMAC

        flowBuilder.setMatch(createDestEthMatch(matchBuilder, new MacAddress("01:00:00:00:00:00"), new MacAddress("01:00:00:00:00:00")).build());

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
        List<Instruction> instructions = new ArrayList<Instruction>();
        List<Instruction> existingInstructions = null;
        if (flow != null) {
            Instructions ins = flow.getInstructions();
            if (ins != null) {
                existingInstructions = ins.getInstruction();
            }
        }

        if (write) {
            // GOTO Instruction
            createGotoTableInstructions(ib, localTable);
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
            boolean flowRemove = removeOutputPortFromInstructions(ib, dpidLong,
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

         String nodeName = "openflow:" + dpidLong;

         MatchBuilder matchBuilder = new MatchBuilder();
         NodeBuilder nodeBuilder = createNodeBuilder(nodeName);
         FlowBuilder flowBuilder = new FlowBuilder();

         // Create the OF Match using MatchBuilder
         // Match Vlan ID
         flowBuilder.setMatch(createVlanIdMatch(matchBuilder, new VlanId(Integer.valueOf(segmentationId))).build());
         // Match DMAC
         flowBuilder.setMatch(createDestEthMatch(matchBuilder, new MacAddress("01:00:00:00:00:00"), new MacAddress("01:00:00:00:00:00")).build());

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

         Flow flow = this.getFlow(flowBuilder, nodeBuilder);
         // Instantiate the Builders for the OF Actions and Instructions
         InstructionBuilder ib = new InstructionBuilder();
         InstructionsBuilder isb = new InstructionsBuilder();
         List<Instruction> instructions = new ArrayList<Instruction>();

         if (write) {
             // GOTO Instuction
             createGotoTableInstructions(ib, localTable);
             ib.setOrder(0);
             ib.setKey(new InstructionKey(0));
             instructions.add(ib.build());
             // Set the Output Port/Iface
             createOutputPortInstructions(ib, dpidLong, ethPort);
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

        String nodeName = "openflow:" + dpidLong;

        MatchBuilder matchBuilder = new MatchBuilder();
        NodeBuilder nodeBuilder = createNodeBuilder(nodeName);
        FlowBuilder flowBuilder = new FlowBuilder();

        // Create Match(es) and Set them in the FlowBuilder Object
        flowBuilder.setMatch(createTunnelIDMatch(matchBuilder, new BigInteger(segmentationId)).build());

        if (write) {
            // Create the OF Actions and Instructions
            InstructionBuilder ib = new InstructionBuilder();
            InstructionsBuilder isb = new InstructionsBuilder();

            // Instructions List Stores Individual Instructions
            List<Instruction> instructions = new ArrayList<Instruction>();

            // Call the InstructionBuilder Methods Containing Actions
            createGotoTableInstructions(ib, goToTableId);
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

         String nodeName = "openflow:" + dpidLong;

         MatchBuilder matchBuilder = new MatchBuilder();
         NodeBuilder nodeBuilder = createNodeBuilder(nodeName);
         FlowBuilder flowBuilder = new FlowBuilder();

         // Create Match(es) and Set them in the FlowBuilder Object
         flowBuilder.setMatch(createVlanIdMatch(matchBuilder, new VlanId(Integer.valueOf(segmentationId))).build());

         if (write) {
             // Create the OF Actions and Instructions
             InstructionBuilder ib = new InstructionBuilder();
             InstructionsBuilder isb = new InstructionsBuilder();

             // Instructions List Stores Individual Instructions
             List<Instruction> instructions = new ArrayList<Instruction>();

             // Call the InstructionBuilder Methods Containing Actions
             //createGotoTableInstructions(ib, goToTableId);
             //ib.setOrder(0);
             //ib.setKey(new InstructionKey(0));
             //instructions.add(ib.build());
             // Set the Output Port/Iface
             createOutputPortInstructions(ib, dpidLong, ethPort);
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

        String nodeName = "openflow:" + dpidLong;

        MatchBuilder matchBuilder = new MatchBuilder();
        NodeBuilder nodeBuilder = createNodeBuilder(nodeName);
        FlowBuilder flowBuilder = new FlowBuilder();

        // Create the OF Match using MatchBuilder
        flowBuilder.setMatch(createTunnelIDMatch(matchBuilder, new BigInteger(segmentationId)).build());
        flowBuilder.setMatch(createDestEthMatch(matchBuilder, new MacAddress(attachedMac), null).build());

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
            List<Instruction> instructions = new ArrayList<Instruction>();

            // Set the Output Port/Iface
            createOutputPortInstructions(ib, dpidLong, localPort);
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

        String nodeName = "openflow:" + dpidLong;

        MatchBuilder matchBuilder = new MatchBuilder();
        NodeBuilder nodeBuilder = createNodeBuilder(nodeName);
        FlowBuilder flowBuilder = new FlowBuilder();

        // Create the OF Match using MatchBuilder
        flowBuilder.setMatch(createVlanIdMatch(matchBuilder, new VlanId(Integer.valueOf(segmentationId))).build());
        flowBuilder.setMatch(createDestEthMatch(matchBuilder, new MacAddress(attachedMac), null).build());

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
            List<Instruction> instructions = new ArrayList<Instruction>();
            List<Instruction> instructions_tmp = new ArrayList<Instruction>();

            /* Strip vlan and store to tmp instruction space*/
            createPopVlanInstructions(ib);
            ib.setOrder(0);
            ib.setKey(new InstructionKey(0));
            instructions_tmp.add(ib.build());

            // Set the Output Port/Iface
            ib = new InstructionBuilder();
            addOutputPortInstructions(ib, dpidLong, localPort, instructions_tmp);
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

        String nodeName = "openflow:" + dpidLong;

        MatchBuilder matchBuilder = new MatchBuilder();
        NodeBuilder nodeBuilder = createNodeBuilder(nodeName);
        FlowBuilder flowBuilder = new FlowBuilder();

        // Create the OF Match using MatchBuilder
        flowBuilder.setMatch(createTunnelIDMatch(matchBuilder, new BigInteger(segmentationId)).build());
        flowBuilder.setMatch(createDestEthMatch(matchBuilder, new MacAddress("01:00:00:00:00:00"), new MacAddress("01:00:00:00:00:00")).build());

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
        List<Instruction> instructions = new ArrayList<Instruction>();
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
            boolean flowRemove = removeOutputPortFromInstructions(ib, dpidLong, localPort,
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

        String nodeName = "openflow:" + dpidLong;

        MatchBuilder matchBuilder = new MatchBuilder();
        NodeBuilder nodeBuilder = createNodeBuilder(nodeName);
        FlowBuilder flowBuilder = new FlowBuilder();

        // Create the OF Match using MatchBuilder
        flowBuilder.setMatch(createVlanIdMatch(matchBuilder, new VlanId(Integer.valueOf(segmentationId))).build());
        flowBuilder.setMatch(createDestEthMatch(matchBuilder, new MacAddress("01:00:00:00:00:00"), new MacAddress("01:00:00:00:00:00")).build());

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
        List<Instruction> instructions = new ArrayList<Instruction>();
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
                List<Action> existingActions = null;
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
                existingInstructions = new ArrayList<Instruction>();
            }

            if (add_pop_vlan) {
                /* pop vlan */
                createPopVlanInstructions(ib);
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
            boolean flowRemove = removeOutputPortFromInstructions(ib, dpidLong,
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

        String nodeName = "openflow:" + dpidLong;

        MatchBuilder matchBuilder = new MatchBuilder();
        NodeBuilder nodeBuilder = createNodeBuilder(nodeName);
        FlowBuilder flowBuilder = new FlowBuilder();

        // Create Match(es) and Set them in the FlowBuilder Object
        flowBuilder.setMatch(createTunnelIDMatch(matchBuilder, new BigInteger(segmentationId)).build());

        if (write) {
            // Create the OF Actions and Instructions
            InstructionBuilder ib = new InstructionBuilder();
            InstructionsBuilder isb = new InstructionsBuilder();

            // Instructions List Stores Individual Instructions
            List<Instruction> instructions = new ArrayList<Instruction>();

            // Call the InstructionBuilder Methods Containing Actions
            createDropInstructions(ib);
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

        String nodeName = "openflow:" + dpidLong;

        MatchBuilder matchBuilder = new MatchBuilder();
        NodeBuilder nodeBuilder = createNodeBuilder(nodeName);
        FlowBuilder flowBuilder = new FlowBuilder();

        // Create Match(es) and Set them in the FlowBuilder Object
        flowBuilder.setMatch(createVlanIdMatch(matchBuilder, new VlanId(Integer.valueOf(segmentationId))).build());

        if (write) {
            // Create the OF Actions and Instructions
            InstructionBuilder ib = new InstructionBuilder();
            InstructionsBuilder isb = new InstructionsBuilder();

            // Instructions List Stores Individual Instructions
            List<Instruction> instructions = new ArrayList<Instruction>();

            // Call the InstructionBuilder Methods Containing Actions
            createDropInstructions(ib);
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
        IMDSALConsumer mdsalConsumer = (IMDSALConsumer) ServiceHelper.getInstance(IMDSALConsumer.class, "default", this);
        if (mdsalConsumer == null) {
            logger.error("ERROR finding MDSAL Service. Its possible that writeFlow is called too soon ?");
            return null;
        }

        dataBrokerService = mdsalConsumer.getDataBrokerService();

        if (dataBrokerService == null) {
            logger.error("ERROR finding reference for DataBrokerService. Please check out the MD-SAL support on the Controller.");
            return null;
        }

        InstanceIdentifier<Group> path1 = InstanceIdentifier.builder(Nodes.class).child(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory
                .rev130819.nodes.Node.class, nodeBuilder.getKey()).augmentation(FlowCapableNode.class).child(Group.class,
                new GroupKey(groupBuilder.getGroupId())).build();
        return (Group)dataBrokerService.readConfigurationData(path1);
    }

    private Group writeGroup(GroupBuilder groupBuilder, NodeBuilder nodeBuilder) {
        IMDSALConsumer mdsalConsumer = (IMDSALConsumer) ServiceHelper.getInstance(IMDSALConsumer.class, "default", this);
        if (mdsalConsumer == null) {
            logger.error("ERROR finding MDSAL Service. Its possible that writeFlow is called too soon ?");
            return null;
        }

        dataBrokerService = mdsalConsumer.getDataBrokerService();

        if (dataBrokerService == null) {
            logger.error("ERROR finding reference for DataBrokerService. Please check out the MD-SAL support on the Controller.");
            return null;
        }
        DataModification<InstanceIdentifier<?>, DataObject> modification = dataBrokerService.beginTransaction();
        InstanceIdentifier<Group> path1 = InstanceIdentifier.builder(Nodes.class).child(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory
                .rev130819.nodes.Node.class, nodeBuilder.getKey()).augmentation(FlowCapableNode.class).child(Group.class,
                new GroupKey(groupBuilder.getGroupId())).build();
        modification.putConfigurationData(nodeBuilderToInstanceId(nodeBuilder), nodeBuilder.build());
        modification.putConfigurationData(path1, groupBuilder.build());
        Future<RpcResult<TransactionStatus>> commitFuture = modification.commit();

        try {
            RpcResult<TransactionStatus> result = commitFuture.get();
            TransactionStatus status = result.getResult();
            logger.debug("Transaction Status "+status.toString()+" for Group "+groupBuilder.getGroupName());
        } catch (InterruptedException e) {
            logger.error(e.getMessage(), e);
        } catch (ExecutionException e) {
            logger.error(e.getMessage(), e);
        }
        return (Group)dataBrokerService.readConfigurationData(path1);
    }

    private Group removeGroup(GroupBuilder groupBuilder, NodeBuilder nodeBuilder) {
        IMDSALConsumer mdsalConsumer = (IMDSALConsumer) ServiceHelper.getInstance(IMDSALConsumer.class, "default", this);
        if (mdsalConsumer == null) {
            logger.error("ERROR finding MDSAL Service. Its possible that writeFlow is called too soon ?");
            return null;
        }

        dataBrokerService = mdsalConsumer.getDataBrokerService();

        if (dataBrokerService == null) {
            logger.error("ERROR finding reference for DataBrokerService. Please check out the MD-SAL support on the Controller.");
            return null;
        }
        DataModification<InstanceIdentifier<?>, DataObject> modification = dataBrokerService.beginTransaction();
        InstanceIdentifier<Group> path1 = InstanceIdentifier.builder(Nodes.class).child(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory
                .rev130819.nodes.Node.class, nodeBuilder.getKey()).augmentation(FlowCapableNode.class).child(Group.class,
                new GroupKey(groupBuilder.getGroupId())).build();
        modification.removeConfigurationData(path1);
        Future<RpcResult<TransactionStatus>> commitFuture = modification.commit();

        try {
            RpcResult<TransactionStatus> result = commitFuture.get();
            TransactionStatus status = result.getResult();
            logger.debug("Transaction Status "+status.toString()+" for Group "+groupBuilder.getGroupName());
        } catch (InterruptedException e) {
            logger.error(e.getMessage(), e);
        } catch (ExecutionException e) {
            logger.error(e.getMessage(), e);
        }
        return (Group)dataBrokerService.readConfigurationData(path1);
    }
    private Flow getFlow(FlowBuilder flowBuilder, NodeBuilder nodeBuilder) {
        IMDSALConsumer mdsalConsumer = (IMDSALConsumer) ServiceHelper.getInstance(IMDSALConsumer.class, "default", this);
        if (mdsalConsumer == null) {
            logger.error("ERROR finding MDSAL Service. Its possible that writeFlow is called too soon ?");
            return null;
        }

        dataBrokerService = mdsalConsumer.getDataBrokerService();

        if (dataBrokerService == null) {
            logger.error("ERROR finding reference for DataBrokerService. Please check out the MD-SAL support on the Controller.");
            return null;
        }

        InstanceIdentifier<Flow> path1 = InstanceIdentifier.builder(Nodes.class).child(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory
                .rev130819.nodes.Node.class, nodeBuilder.getKey()).augmentation(FlowCapableNode.class).child(Table.class,
                new TableKey(flowBuilder.getTableId())).child(Flow.class, flowBuilder.getKey()).build();
        return (Flow)dataBrokerService.readConfigurationData(path1);
    }

    private void writeFlow(FlowBuilder flowBuilder, NodeBuilder nodeBuilder) {
        IMDSALConsumer mdsalConsumer = (IMDSALConsumer) ServiceHelper.getInstance(IMDSALConsumer.class, "default", this);
        if (mdsalConsumer == null) {
            logger.error("ERROR finding MDSAL Service. Its possible that writeFlow is called too soon ?");
            return;
        }

        dataBrokerService = mdsalConsumer.getDataBrokerService();

        if (dataBrokerService == null) {
            logger.error("ERROR finding reference for DataBrokerService. Please check out the MD-SAL support on the Controller.");
            return;
        }
        DataModification<InstanceIdentifier<?>, DataObject> modification = dataBrokerService.beginTransaction();
        InstanceIdentifier<Flow> path1 = InstanceIdentifier.builder(Nodes.class).child(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory
                .rev130819.nodes.Node.class, nodeBuilder.getKey()).augmentation(FlowCapableNode.class).child(Table.class,
                new TableKey(flowBuilder.getTableId())).child(Flow.class, flowBuilder.getKey()).build();
        //modification.putOperationalData(nodeBuilderToInstanceId(nodeBuilder), nodeBuilder.build());
        //modification.putOperationalData(path1, flowBuilder.build());
        modification.putConfigurationData(nodeBuilderToInstanceId(nodeBuilder), nodeBuilder.build());
        modification.putConfigurationData(path1, flowBuilder.build());
        Future<RpcResult<TransactionStatus>> commitFuture = modification.commit();
        try {
            RpcResult<TransactionStatus> result = commitFuture.get();
            TransactionStatus status = result.getResult();
            logger.debug("Transaction Status "+status.toString()+" for Flow "+flowBuilder.getFlowName());
        } catch (InterruptedException e) {
            logger.error(e.getMessage(), e);
        } catch (ExecutionException e) {
            logger.error(e.getMessage(), e);
        }
    }

    private void removeFlow(FlowBuilder flowBuilder, NodeBuilder nodeBuilder) {
        IMDSALConsumer mdsalConsumer = (IMDSALConsumer) ServiceHelper.getInstance(IMDSALConsumer.class, "default", this);
        if (mdsalConsumer == null) {
            logger.error("ERROR finding MDSAL Service.");
            return;
        }

        dataBrokerService = mdsalConsumer.getDataBrokerService();

        if (dataBrokerService == null) {
            logger.error("ERROR finding reference for DataBrokerService. Please check out the MD-SAL support on the Controller.");
            return;
        }
        DataModification<InstanceIdentifier<?>, DataObject> modification = dataBrokerService.beginTransaction();
        InstanceIdentifier<Flow> path1 = InstanceIdentifier.builder(Nodes.class)
                .child(org.opendaylight.yang.gen.v1.urn.opendaylight.inventory
                .rev130819.nodes.Node.class, nodeBuilder.getKey())
                .augmentation(FlowCapableNode.class).child(Table.class,
                new TableKey(flowBuilder.getTableId())).child(Flow.class, flowBuilder.getKey()).build();
        //modification.removeOperationalData(nodeBuilderToInstanceId(nodeBuilder));
        //modification.removeOperationalData(path1);
        //modification.removeConfigurationData(nodeBuilderToInstanceId(nodeBuilder));
        modification.removeConfigurationData(path1);
        Future<RpcResult<TransactionStatus>> commitFuture = modification.commit();
        try {
            RpcResult<TransactionStatus> result = commitFuture.get();
            TransactionStatus status = result.getResult();
            logger.debug("Transaction Status "+status.toString()+" for Flow "+flowBuilder.getFlowName());
        } catch (InterruptedException e) {
            logger.error(e.getMessage(), e);
        } catch (ExecutionException e) {
            logger.error(e.getMessage(), e);
        }
    }

    /**
     * Create Ingress Port Match dpidLong, inPort
     *
     * @param matchBuilder  Map matchBuilder MatchBuilder Object without a match
     * @param dpidLong      Long the datapath ID of a switch/node
     * @param inPort        Long ingress port on a switch
     * @return matchBuilder Map MatchBuilder Object with a match
     */
    protected static MatchBuilder createInPortMatch(MatchBuilder matchBuilder, Long dpidLong, Long inPort) {

        NodeConnectorId ncid = new NodeConnectorId("openflow:" + dpidLong + ":" + inPort);
        logger.debug("createInPortMatch() Node Connector ID is - Type=openflow: DPID={} inPort={} ", dpidLong, inPort);
        matchBuilder.setInPort(NodeConnectorId.getDefaultInstance(ncid.getValue()));
        matchBuilder.setInPort(ncid);

        return matchBuilder;
    }

    /**
     * Create EtherType Match
     *
     * @param matchBuilder  Map matchBuilder MatchBuilder Object without a match
     * @param etherType     Long EtherType
     * @return matchBuilder Map MatchBuilder Object with a match
     */
    protected static MatchBuilder createEtherTypeMatch(MatchBuilder matchBuilder, EtherType etherType) {

        EthernetMatchBuilder ethernetMatch = new EthernetMatchBuilder();
        EthernetTypeBuilder ethTypeBuilder = new EthernetTypeBuilder();
        ethTypeBuilder.setType(new EtherType(etherType));
        ethernetMatch.setEthernetType(ethTypeBuilder.build());
        matchBuilder.setEthernetMatch(ethernetMatch.build());

        return matchBuilder;
    }

    /**
     * Create Ethernet Source Match
     *
     * @param matchBuilder  MatchBuilder Object without a match yet
     * @param sMacAddr      String representing a source MAC
     * @return matchBuilder Map MatchBuilder Object with a match
     */
    protected static MatchBuilder createEthSrcMatch(MatchBuilder matchBuilder, MacAddress sMacAddr) {

        EthernetMatchBuilder ethernetMatch = new EthernetMatchBuilder();
        EthernetSourceBuilder ethSourceBuilder = new EthernetSourceBuilder();
        ethSourceBuilder.setAddress(new MacAddress(sMacAddr));
        ethernetMatch.setEthernetSource(ethSourceBuilder.build());
        matchBuilder.setEthernetMatch(ethernetMatch.build());

        return matchBuilder;
    }

    /**
     * Create Ethernet Destination Match
     *
     * @param matchBuilder MatchBuilder Object without a match yet
     * @param vlanId       Integer representing a VLAN ID Integer representing a VLAN ID
     * @return matchBuilder Map MatchBuilder Object with a match
     */

    protected static MatchBuilder createVlanIdMatch(MatchBuilder matchBuilder, VlanId vlanId) {
        VlanMatchBuilder vlanMatchBuilder = new VlanMatchBuilder();
        VlanIdBuilder vlanIdBuilder = new VlanIdBuilder();
        vlanIdBuilder.setVlanId(new VlanId(vlanId));
        vlanIdBuilder.setVlanIdPresent(true);
        vlanMatchBuilder.setVlanId(vlanIdBuilder.build());
        matchBuilder.setVlanMatch(vlanMatchBuilder.build());

        return matchBuilder;
    }

    /**
     * Create Ethernet Destination Match
     *
     * @param matchBuilder  MatchBuilder Object without a match yet
     * @param dMacAddr      String representing a destination MAC
     * @return matchBuilder Map MatchBuilder Object with a match
     */

    protected static MatchBuilder createDestEthMatch(MatchBuilder matchBuilder, MacAddress dMacAddr, MacAddress mask) {

        EthernetMatchBuilder ethernetMatch = new EthernetMatchBuilder();
        EthernetDestinationBuilder ethDestinationBuilder = new EthernetDestinationBuilder();
        ethDestinationBuilder.setAddress(new MacAddress(dMacAddr));
        if (mask != null) {
            ethDestinationBuilder.setMask(mask);
        }
        ethernetMatch.setEthernetDestination(ethDestinationBuilder.build());
        matchBuilder.setEthernetMatch(ethernetMatch.build());

        return matchBuilder;
    }

    /**
     * Tunnel ID Match Builder
     *
     * @param matchBuilder  MatchBuilder Object without a match yet
     * @param tunnelId      BigInteger representing a tunnel ID
     * @return matchBuilder Map MatchBuilder Object with a match
     */
    protected static MatchBuilder createTunnelIDMatch(MatchBuilder matchBuilder, BigInteger tunnelId) {

        TunnelBuilder tunnelBuilder = new TunnelBuilder();
        tunnelBuilder.setTunnelId(tunnelId);
        matchBuilder.setTunnel(tunnelBuilder.build());

        return matchBuilder;
    }

    /**
     * Match ICMP code and type
     *
     * @param matchBuilder  MatchBuilder Object without a match yet
     * @param type          short representing an ICMP type
     * @param code          short representing an ICMP code
     * @return matchBuilder Map MatchBuilder Object with a match
     */
    protected static MatchBuilder createICMPv4Match(MatchBuilder matchBuilder, short type, short code) {

        EthernetMatchBuilder eth = new EthernetMatchBuilder();
        EthernetTypeBuilder ethTypeBuilder = new EthernetTypeBuilder();
        ethTypeBuilder.setType(new EtherType(0x0800L));
        eth.setEthernetType(ethTypeBuilder.build());
        matchBuilder.setEthernetMatch(eth.build());

        // Build the IPv4 Match requied per OVS Syntax
        IpMatchBuilder ipmatch = new IpMatchBuilder();
        ipmatch.setIpProtocol((short) 1);
        matchBuilder.setIpMatch(ipmatch.build());

        // Build the ICMPv4 Match
        Icmpv4MatchBuilder icmpv4match = new Icmpv4MatchBuilder();
        icmpv4match.setIcmpv4Type(type);
        icmpv4match.setIcmpv4Code(code);
        matchBuilder.setIcmpv4Match(icmpv4match.build());

        return matchBuilder;
    }

    /**
     * @param matchBuilder MatchBuilder Object without a match yet
     * @param dstip        String containing an IPv4 prefix
     * @return matchBuilder Map Object with a match
     */
    private static MatchBuilder createDstL3IPv4Match(MatchBuilder matchBuilder, Ipv4Prefix dstip) {

        EthernetMatchBuilder eth = new EthernetMatchBuilder();
        EthernetTypeBuilder ethTypeBuilder = new EthernetTypeBuilder();
        ethTypeBuilder.setType(new EtherType(0x0800L));
        eth.setEthernetType(ethTypeBuilder.build());
        matchBuilder.setEthernetMatch(eth.build());

        Ipv4MatchBuilder ipv4match = new Ipv4MatchBuilder();
        ipv4match.setIpv4Destination(dstip);

        matchBuilder.setLayer3Match(ipv4match.build());

        return matchBuilder;

    }

    /**
     * @param matchBuilder MatchBuilder Object without a match yet
     * @param srcip        String containing an IPv4 prefix
     * @return             matchBuilder Map Object with a match
     */
    private static MatchBuilder createSrcL3IPv4Match(MatchBuilder matchBuilder, Ipv4Prefix srcip) {

        EthernetMatchBuilder eth = new EthernetMatchBuilder();
        EthernetTypeBuilder ethTypeBuilder = new EthernetTypeBuilder();
        ethTypeBuilder.setType(new EtherType(0x0800L));
        eth.setEthernetType(ethTypeBuilder.build());
        matchBuilder.setEthernetMatch(eth.build());

        Ipv4MatchBuilder ipv4Match = new Ipv4MatchBuilder();
        Ipv4MatchBuilder ipv4match = new Ipv4MatchBuilder();
        ipv4match.setIpv4Source(srcip);
        matchBuilder.setLayer3Match(ipv4match.build());

        return matchBuilder;

    }

    /**
     * Create Source TCP Port Match
     *
     * @param matchBuilder @param matchbuilder MatchBuilder Object without a match yet
     * @param tcpport      Integer representing a source TCP port
     * @return             matchBuilder Map MatchBuilder Object with a match
     */
    protected static MatchBuilder createSetSrcTcpMatch(MatchBuilder matchBuilder, PortNumber tcpport) {

        EthernetMatchBuilder ethType = new EthernetMatchBuilder();
        EthernetTypeBuilder ethTypeBuilder = new EthernetTypeBuilder();
        ethTypeBuilder.setType(new EtherType(0x0800L));
        ethType.setEthernetType(ethTypeBuilder.build());
        matchBuilder.setEthernetMatch(ethType.build());

        IpMatchBuilder ipmatch = new IpMatchBuilder();
        ipmatch.setIpProtocol((short) 6);
        matchBuilder.setIpMatch(ipmatch.build());

        TcpMatchBuilder tcpmatch = new TcpMatchBuilder();
        tcpmatch.setTcpSourcePort(tcpport);
        matchBuilder.setLayer4Match(tcpmatch.build());

        return matchBuilder;

    }

    /**
     * Create Destination TCP Port Match
     *
     * @param matchBuilder MatchBuilder Object without a match yet
     * @param tcpport      Integer representing a destination TCP port
     * @return             matchBuilder Map MatchBuilder Object with a match
     */
    protected static MatchBuilder createSetDstTcpMatch(MatchBuilder matchBuilder, PortNumber tcpport) {

        EthernetMatchBuilder ethType = new EthernetMatchBuilder();
        EthernetTypeBuilder ethTypeBuilder = new EthernetTypeBuilder();
        ethTypeBuilder.setType(new EtherType(0x0800L));
        ethType.setEthernetType(ethTypeBuilder.build());
        matchBuilder.setEthernetMatch(ethType.build());

        IpMatchBuilder ipmatch = new IpMatchBuilder();
        ipmatch.setIpProtocol((short) 6);
        matchBuilder.setIpMatch(ipmatch.build());

        PortNumber dstport = new PortNumber(tcpport);
        TcpMatchBuilder tcpmatch = new TcpMatchBuilder();

        tcpmatch.setTcpDestinationPort(tcpport);
        matchBuilder.setLayer4Match(tcpmatch.build());

        return matchBuilder;
    }

    /**
     * Create Send to Controller Reserved Port Instruction (packet_in)
     *
     * @param ib Map InstructionBuilder without any instructions
     * @return ib Map InstructionBuilder with instructions
     */

    protected static InstructionBuilder createSendToControllerInstructions(InstructionBuilder ib) {

        List<Action> actionList = new ArrayList<Action>();
        ActionBuilder ab = new ActionBuilder();

        OutputActionBuilder output = new OutputActionBuilder();
        output.setMaxLength(0xffff);
        Uri value = new Uri("CONTROLLER");
        output.setOutputNodeConnector(value);
        ab.setAction(new OutputActionCaseBuilder().setOutputAction(output.build()).build());
        ab.setOrder(0);
        ab.setKey(new ActionKey(0));
        actionList.add(ab.build());

        // Create an Apply Action
        ApplyActionsBuilder aab = new ApplyActionsBuilder();
        aab.setAction(actionList);

        // Wrap our Apply Action in an Instruction
        ib.setInstruction(new ApplyActionsCaseBuilder().setApplyActions(aab.build()).build());

        return ib;
    }

    /**
     * Create NORMAL Reserved Port Instruction (packet_in)
     *
     * @param ib Map InstructionBuilder without any instructions
     * @return ib Map InstructionBuilder with instructions
     */

    protected static InstructionBuilder createNormalInstructions(InstructionBuilder ib) {

        List<Action> actionList = new ArrayList<Action>();
        ActionBuilder ab = new ActionBuilder();

        OutputActionBuilder output = new OutputActionBuilder();
        Uri value = new Uri("NORMAL");
        output.setOutputNodeConnector(value);
        ab.setAction(new OutputActionCaseBuilder().setOutputAction(output.build()).build());
        ab.setOrder(0);
        ab.setKey(new ActionKey(0));
        actionList.add(ab.build());

        // Create an Apply Action
        ApplyActionsBuilder aab = new ApplyActionsBuilder();
        aab.setAction(actionList);

        // Wrap our Apply Action in an Instruction
        ib.setInstruction(new ApplyActionsCaseBuilder().setApplyActions(aab.build()).build());

        return ib;
    }

    /**
     * Create Output Port Instruction
     *
     * @param ib       Map InstructionBuilder without any instructions
     * @param dpidLong Long the datapath ID of a switch/node
     * @param port     Long representing a port on a switch/node
     * @return ib InstructionBuilder Map with instructions
     */
    protected static InstructionBuilder createOutputPortInstructions(InstructionBuilder ib, Long dpidLong, Long port) {

        NodeConnectorId ncid = new NodeConnectorId("openflow:" + dpidLong + ":" + port);
        logger.debug("createOutputPortInstructions() Node Connector ID is - Type=openflow: DPID={} inPort={} ", dpidLong, port);

        List<Action> actionList = new ArrayList<Action>();
        ActionBuilder ab = new ActionBuilder();
        OutputActionBuilder oab = new OutputActionBuilder();
        oab.setOutputNodeConnector(ncid);

        ab.setAction(new OutputActionCaseBuilder().setOutputAction(oab.build()).build());
        ab.setOrder(0);
        ab.setKey(new ActionKey(0));
        actionList.add(ab.build());

        // Create an Apply Action
        ApplyActionsBuilder aab = new ApplyActionsBuilder();
        aab.setAction(actionList);
        ib.setInstruction(new ApplyActionsCaseBuilder().setApplyActions(aab.build()).build());

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
    protected InstructionBuilder createOutputPortInstructions(InstructionBuilder ib,
                                                              Long dpidLong, Long port ,
                                                              List<Instruction> instructions) {
        NodeConnectorId ncid = new NodeConnectorId("openflow:" + dpidLong + ":" + port);
        logger.debug("createOutputPortInstructions() Node Connector ID is - Type=openflow: DPID={} port={} existingInstructions={}", dpidLong, port, instructions);

        List<Action> actionList = new ArrayList<Action>();
        ActionBuilder ab = new ActionBuilder();

        List<Action> existingActions = null;
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
        NodeConnectorId ncid = new NodeConnectorId("openflow:" + dpidLong + ":" + port);
        logger.debug("createOutputGroupInstructions() Node Connector ID is - Type=openflow: DPID={} port={} existingInstructions={}", dpidLong, port, instructions);

        List<Action> actionList = new ArrayList<Action>();
        ActionBuilder ab = new ActionBuilder();

        List<Action> existingActions = null;
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
                    List<Action> bucketActionList = new ArrayList<Action>();
                    bucketActionList.addAll(bucket.getAction());
                    /* set order for new action and add to action list */
                    ab.setOrder(bucketActionList.size());
                    ab.setKey(new ActionKey(bucketActionList.size()));
                    bucketActionList.add(ab.build());

                    /* set bucket and buckets list. Reset groupBuilder with new buckets.*/
                    BucketsBuilder bucketsBuilder = new BucketsBuilder();
                    List<Bucket> bucketList = new ArrayList<Bucket>();
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
            groupBuilder.setGroupName("Output port group" + groupId);
            groupBuilder.setBarrier(false);

            BucketsBuilder bucketBuilder = new BucketsBuilder();
            List<Bucket> bucketList = new ArrayList<Bucket>();
            BucketBuilder bucket = new BucketBuilder();
            bucket.setBucketId(new BucketId((long) 1));
            bucket.setKey(new BucketKey(new BucketId((long) 1)));

            /* put output action to the bucket */
            List<Action> bucketActionList = new ArrayList<Action>();
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
            groupActionB.setGroup("Output port group" + groupId);
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
     * add Output Port action to Instruction action list.
     * This is use for flow with single output port actions.
     * Flow with mutiple output port actions should use createOutputPortInstructions() method.
     *
     * @param ib       Map InstructionBuilder without any instructions
     * @param dpidLong Long the datapath ID of a switch/node
     * @param port     Long representing a port on a switch/node
     * @return ib InstructionBuilder Map with instructions
     */
    protected static InstructionBuilder addOutputPortInstructions(InstructionBuilder ib,
                                                                     Long dpidLong, Long port ,
                                                                     List<Instruction> instructions) {
        NodeConnectorId ncid = new NodeConnectorId("openflow:" + dpidLong + ":" + port);
        logger.debug("addOutputPortInstructions() Node Connector ID is - Type=openflow: DPID={} port={} existingInstructions={}", dpidLong, port, instructions);

        List<Action> actionList = new ArrayList<Action>();
        ActionBuilder ab = new ActionBuilder();

        List<Action> existingActions = null;
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
        ab.setOrder(actionList.size());
        ab.setKey(new ActionKey(actionList.size()));
        actionList.add(ab.build());

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

        NodeConnectorId ncid = new NodeConnectorId("openflow:" + dpidLong + ":" + port);
        logger.debug("removeOutputPortFromGroup() Node Connector ID is - Type=openflow: DPID={} port={} existingInstructions={}", dpidLong, port, instructions);

        List<Action> actionList = new ArrayList<Action>();
        ActionBuilder ab;

        List<Action> existingActions = null;
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
            List<Action> bucketActions = new ArrayList<Action>();
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
                List<Bucket> bucketList = new ArrayList<Bucket>();
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

    /**
     * Remove Output Port from Instruction
     *
     * @param ib       Map InstructionBuilder without any instructions
     * @param dpidLong Long the datapath ID of a switch/node
     * @param port     Long representing a port on a switch/node
     * @return ib InstructionBuilder Map with instructions
     */
    protected static boolean removeOutputPortFromInstructions(InstructionBuilder ib,
                                Long dpidLong, Long port , List<Instruction> instructions) {

        NodeConnectorId ncid = new NodeConnectorId("openflow:" + dpidLong + ":" + port);
        logger.debug("createOutputPortInstructions() Node Connector ID is - Type=openflow: DPID={} port={} existingInstructions={}", dpidLong, port, instructions);

        List<Action> actionList = new ArrayList<Action>();
        ActionBuilder ab;

        List<Action> existingActions = null;
        if (instructions != null) {
            for (Instruction in : instructions) {
                if (in.getInstruction() instanceof ApplyActionsCase) {
                    existingActions = (((ApplyActionsCase) in.getInstruction()).getApplyActions().getAction());
                    actionList.addAll(existingActions);
                    break;
                }
            }
        }

        int numOutputPort = 0;
        int index = 0;
        boolean isPortDeleted = false;
        for (Action action : actionList) {
            if (action.getAction() instanceof OutputActionCase) {
                numOutputPort++;
                OutputActionCase opAction = (OutputActionCase)action.getAction();
                if (opAction.getOutputAction().getOutputNodeConnector().equals(new Uri(ncid))) {
                    /* Find the output port in action list and remove */
                    index = actionList.indexOf(action);
                    actionList.remove(action);
                    isPortDeleted = true;
                    numOutputPort--;
                    break;
                }
            }
        }

        if (isPortDeleted) {
            for (int i = index; i< actionList.size(); i++) {
                Action action = actionList.get(i);
                if (action.getOrder() != i) {
                    /* Shift the action order */
                    ab = new ActionBuilder();
                    ab.setAction(action.getAction());
                    ab.setOrder(i);
                    ab.setKey(new ActionKey(i));
                    Action actionNewOrder = ab.build();
                    actionList.remove(action);
                    actionList.add(i, actionNewOrder);
                }
            }
        }

        /* Put new action list in Apply Action instruction */
        if (numOutputPort > 0) {
            ApplyActionsBuilder aab = new ApplyActionsBuilder();
            aab.setAction(actionList);
            ib.setInstruction(new ApplyActionsCaseBuilder().setApplyActions(aab.build()).build());
            logger.debug("createOutputPortInstructions() : applyAction {}", aab.build());
            return false;
        } else {
            /* if all output port are removed. Return true to indicate flow remove */
            return true;
        }
    }

    /**
     * Create Set Vlan ID Instruction - This includes push vlan action, and set field -> vlan vid action
     *
     * @param ib     Map InstructionBuilder without any instructions
     * @param vlanId Integer representing a VLAN ID Integer representing a VLAN ID
     * @return ib Map InstructionBuilder with instructions
     */
    protected static InstructionBuilder createSetVlanInstructions(InstructionBuilder ib, VlanId vlanId) {

        List<Action> actionList = new ArrayList<Action>();
        ActionBuilder ab = new ActionBuilder();

        /* First we push vlan header */
        PushVlanActionBuilder vlan = new PushVlanActionBuilder();
        vlan.setEthernetType(new Integer(0x8100));
        ab.setAction(new PushVlanActionCaseBuilder().setPushVlanAction(vlan.build()).build());
        ab.setOrder(0);
        actionList.add(ab.build());

        /* Then we set vlan id value as vlanId */
        SetVlanIdActionBuilder vl = new SetVlanIdActionBuilder();
        vl.setVlanId(vlanId);
        ab = new ActionBuilder();
        ab.setAction(new SetVlanIdActionCaseBuilder().setSetVlanIdAction(vl.build()).build());
        ab.setOrder(1);
        actionList.add(ab.build());
        // Create an Apply Action
        ApplyActionsBuilder aab = new ApplyActionsBuilder();
        aab.setAction(actionList);

        // Wrap our Apply Action in an Instruction
        ib.setInstruction(new ApplyActionsCaseBuilder().setApplyActions(aab.build()).build());

        return ib;
    }

    /**
     * Create Pop Vlan Instruction - this remove vlan header
     *
     * @param ib Map InstructionBuilder without any instructions
     * @return ib Map InstructionBuilder with instructions
     */
    protected static InstructionBuilder createPopVlanInstructions(InstructionBuilder ib) {

        List<Action> actionList = new ArrayList<Action>();
        ActionBuilder ab = new ActionBuilder();

        PopVlanActionBuilder popVlanActionBuilder = new PopVlanActionBuilder();
        ab.setAction(new PopVlanActionCaseBuilder().setPopVlanAction(popVlanActionBuilder.build()).build());
        ab.setOrder(0);
        actionList.add(ab.build());

        // Create an Apply Action
        ApplyActionsBuilder aab = new ApplyActionsBuilder();
        aab.setAction(actionList);

        // Wrap our Apply Action in an Instruction
        ib.setInstruction(new ApplyActionsCaseBuilder().setApplyActions(aab.build()).build());

        return ib;
    }

    /**
     * Create Set IPv4 Source Instruction
     *
     * @param ib        Map InstructionBuilder without any instructions
     * @param prefixsrc String containing an IPv4 prefix
     * @return ib Map InstructionBuilder with instructions
     */
    protected static InstructionBuilder createNwSrcInstructions(InstructionBuilder ib, Ipv4Prefix prefixsrc) {

        List<Action> actionList = new ArrayList<Action>();
        ActionBuilder ab = new ActionBuilder();

        SetNwSrcActionBuilder setNwsrcActionBuilder = new SetNwSrcActionBuilder();
        Ipv4Builder ipsrc = new Ipv4Builder();
        ipsrc.setIpv4Address(prefixsrc);
        setNwsrcActionBuilder.setAddress(ipsrc.build());
        ab.setAction(new SetNwSrcActionCaseBuilder().setSetNwSrcAction(setNwsrcActionBuilder.build()).build());
        actionList.add(ab.build());

        // Create an Apply Action
        ApplyActionsBuilder aab = new ApplyActionsBuilder();
        aab.setAction(actionList);

        // Wrap our Apply Action in an Instruction
        ib.setInstruction(new ApplyActionsCaseBuilder().setApplyActions(aab.build()).build());

        return ib;
    }

    /**
     * Create Set IPv4 Destination Instruction
     *
     * @param ib           Map InstructionBuilder without any instructions
     * @param prefixdst    String containing an IPv4 prefix
     * @return ib Map InstructionBuilder with instructions
     */
    protected static InstructionBuilder createNwDstInstructions(InstructionBuilder ib, Ipv4Prefix prefixdst) {

        List<Action> actionList = new ArrayList<Action>();
        ActionBuilder ab = new ActionBuilder();

        SetNwDstActionBuilder setNwDstActionBuilder = new SetNwDstActionBuilder();
        Ipv4Builder ipdst = new Ipv4Builder();
        ipdst.setIpv4Address(prefixdst);
        setNwDstActionBuilder.setAddress(ipdst.build());
        ab.setAction(new SetNwDstActionCaseBuilder().setSetNwDstAction(setNwDstActionBuilder.build()).build());
        actionList.add(ab.build());

        // Create an Apply Action
        ApplyActionsBuilder aab = new ApplyActionsBuilder();
        aab.setAction(actionList);

        // Wrap our Apply Action in an Instruction
        ib.setInstruction(new ApplyActionsCaseBuilder().setApplyActions(aab.build()).build());

        return ib;
    }

    /**
     * Create Drop Instruction
     *
     * @param ib Map InstructionBuilder without any instructions
     * @return ib Map InstructionBuilder with instructions
     */
    protected static InstructionBuilder createDropInstructions(InstructionBuilder ib) {

        DropActionBuilder dab = new DropActionBuilder();
        DropAction dropAction = dab.build();
        ActionBuilder ab = new ActionBuilder();
        ab.setAction(new DropActionCaseBuilder().setDropAction(dropAction).build());
        ab.setOrder(0);

        // Add our drop action to a list
        List<Action> actionList = new ArrayList<Action>();
        actionList.add(ab.build());

        // Create an Apply Action
        ApplyActionsBuilder aab = new ApplyActionsBuilder();
        aab.setAction(actionList);

        // Wrap our Apply Action in an Instruction
        ib.setInstruction(new ApplyActionsCaseBuilder().setApplyActions(aab.build()).build());

        return ib;
    }

    /**
     * Create GOTO Table Instruction Builder
     *
     * @param ib      Map InstructionBuilder without any instructions
     * @param tableId short representing a flow table ID short representing a flow table ID
     * @return ib Map InstructionBuilder with instructions
     */
    protected static InstructionBuilder createGotoTableInstructions(InstructionBuilder ib, short tableId) {

        GoToTableBuilder gttb = new GoToTableBuilder();
        gttb.setTableId(tableId);

        // Wrap our Apply Action in an InstructionBuilder
        ib.setInstruction(new GoToTableCaseBuilder().setGoToTable(gttb.build()).build());

        return ib;
    }

    /**
     * Create Set Tunnel ID Instruction Builder
     *
     * @param ib       Map InstructionBuilder without any instructions
     * @param tunnelId BigInteger representing a tunnel ID
     * @return ib Map InstructionBuilder with instructions
     */
    protected static InstructionBuilder createSetTunnelIdInstructions(InstructionBuilder ib, BigInteger tunnelId) {

        List<Action> actionList = new ArrayList<Action>();
        ActionBuilder ab = new ActionBuilder();
        SetFieldBuilder setFieldBuilder = new SetFieldBuilder();

        // Build the Set Tunnel Field Action
        TunnelBuilder tunnel = new TunnelBuilder();
        tunnel.setTunnelId(tunnelId);
        setFieldBuilder.setTunnel(tunnel.build());
        ab.setAction(new SetFieldCaseBuilder().setSetField(setFieldBuilder.build()).build());
        ab.setOrder(0);
        actionList.add(ab.build());

        ApplyActionsBuilder aab = new ApplyActionsBuilder();
        aab.setAction(actionList);

        // Wrap the Apply Action in an InstructionBuilder and return
        ib.setInstruction(new ApplyActionsCaseBuilder().setApplyActions(aab.build()).build());

        return ib;
    }

    /**
     * Create Set Source TCP Port Instruction
     *
     * @param ib      Map InstructionBuilder without any instructions
     * @param tcpport Integer representing a source TCP port
     * @return ib Map InstructionBuilder with instructions
     */
    protected static InstructionBuilder createSetSrcTCPPort(InstructionBuilder ib, PortNumber tcpport) {

        List<Action> actionList = new ArrayList<Action>();
        ActionBuilder ab = new ActionBuilder();
        SetFieldBuilder setFieldBuilder = new SetFieldBuilder();

        // Build the Destination TCP Port
        PortNumber tcpsrcport = new PortNumber(tcpport);
        TcpMatchBuilder tcpmatch = new TcpMatchBuilder();
        tcpmatch.setTcpSourcePort(tcpsrcport);

        setFieldBuilder.setLayer4Match(tcpmatch.build());
        ab.setAction(new SetFieldCaseBuilder().setSetField(setFieldBuilder.build()).build());
        ab.setKey(new ActionKey(1));
        actionList.add(ab.build());

        ApplyActionsBuilder aab = new ApplyActionsBuilder();
        aab.setAction(actionList);
        ib.setInstruction(new ApplyActionsCaseBuilder().setApplyActions(aab.build()).build());

        return ib;
    }

    /**
     * Create Set Destination TCP Port Instruction
     *
     * @param ib      Map InstructionBuilder without any instructions
     * @param tcpport Integer representing a source TCP port
     * @return ib Map InstructionBuilder with instructions
     */
    protected static InstructionBuilder createSetDstTCPPort(InstructionBuilder ib, PortNumber tcpport) {

        List<Action> actionList = new ArrayList<Action>();
        ActionBuilder ab = new ActionBuilder();
        SetFieldBuilder setFieldBuilder = new SetFieldBuilder();

        // Build the Destination TCP Port
        PortNumber tcpdstport = new PortNumber(tcpport);
        TcpMatchBuilder tcpmatch = new TcpMatchBuilder();
        tcpmatch.setTcpDestinationPort(tcpdstport);

        setFieldBuilder.setLayer4Match(tcpmatch.build());
        ab.setAction(new SetFieldCaseBuilder().setSetField(setFieldBuilder.build()).build());
        ab.setKey(new ActionKey(1));
        actionList.add(ab.build());

        ApplyActionsBuilder aab = new ApplyActionsBuilder();
        aab.setAction(actionList);
        ib.setInstruction(new ApplyActionsCaseBuilder().setApplyActions(aab.build()).build());

        return ib;
    }

    /**
     * Create Set Source UDP Port Instruction
     *
     * @param ib      Map InstructionBuilder without any instructions
     * @param udpport Integer representing a source UDP port
     * @return ib Map InstructionBuilder with instructions
     */
    protected static InstructionBuilder createSetSrcUDPPort(InstructionBuilder ib, PortNumber udpport) {

        List<Action> actionList = new ArrayList<Action>();
        ActionBuilder ab = new ActionBuilder();
        SetFieldBuilder setFieldBuilder = new SetFieldBuilder();

        // Build the Destination TCP Port
        PortNumber udpsrcport = new PortNumber(udpport);
        UdpMatchBuilder udpmatch = new UdpMatchBuilder();
        udpmatch.setUdpSourcePort(udpsrcport);

        setFieldBuilder.setLayer4Match(udpmatch.build());
        ab.setAction(new SetFieldCaseBuilder().setSetField(setFieldBuilder.build()).build());
        ab.setKey(new ActionKey(1));
        actionList.add(ab.build());

        ApplyActionsBuilder aab = new ApplyActionsBuilder();
        aab.setAction(actionList);
        ib.setInstruction(new ApplyActionsCaseBuilder().setApplyActions(aab.build()).build());

        return ib;
    }

    /**
     * Create Set Destination UDP Port Instruction
     *
     * @param ib      Map InstructionBuilder without any instructions
     * @param udpport Integer representing a destination UDP port
     * @return ib Map InstructionBuilder with instructions
     */
    protected static InstructionBuilder createSetDstUDPPort(InstructionBuilder ib, PortNumber udpport) {

        List<Action> actionList = new ArrayList<Action>();
        ActionBuilder ab = new ActionBuilder();
        SetFieldBuilder setFieldBuilder = new SetFieldBuilder();

        // Build the Destination TCP Port
        PortNumber udpdstport = new PortNumber(udpport);
        UdpMatchBuilder udpmatch = new UdpMatchBuilder();
        udpmatch.setUdpDestinationPort(udpdstport);

        setFieldBuilder.setLayer4Match(udpmatch.build());
        ab.setAction(new SetFieldCaseBuilder().setSetField(setFieldBuilder.build()).build());
        ab.setKey(new ActionKey(1));
        actionList.add(ab.build());

        ApplyActionsBuilder aab = new ApplyActionsBuilder();
        aab.setAction(actionList);
        ib.setInstruction(new ApplyActionsCaseBuilder().setApplyActions(aab.build()).build());

        return ib;
    }

    /**
     * Create Set ICMP Code Instruction
     *
     * @param ib   Map InstructionBuilder without any instructions
     * @param code short repesenting an ICMP code
     * @return ib Map InstructionBuilder with instructions
     */

    private static InstructionBuilder createSetIcmpCodeInstruction(InstructionBuilder ib, short code) {

        List<Action> actionList = new ArrayList<Action>();
        ActionBuilder ab = new ActionBuilder();
        SetFieldBuilder setFieldBuilder = new SetFieldBuilder();
        Icmpv4MatchBuilder icmpv4match = new Icmpv4MatchBuilder();

        // Build the ICMPv4 Code Match
        icmpv4match.setIcmpv4Code(code);
        setFieldBuilder.setIcmpv4Match(icmpv4match.build());

        ab.setAction(new SetFieldCaseBuilder().setSetField(setFieldBuilder.build()).build());
        ab.setKey(new ActionKey(0));
        actionList.add(ab.build());
        ApplyActionsBuilder aab = new ApplyActionsBuilder();
        aab.setAction(actionList);

        // Wrap our Apply Action in an Instruction
        ib.setInstruction(new ApplyActionsCaseBuilder().setApplyActions(aab.build()).build());

        return ib;
    }

    /**
     * Create Set ICMP Code Instruction
     *
     * @param ib Map InstructionBuilder without any instructions
     * @return ib Map InstructionBuilder with instructions
     */
    private static InstructionBuilder createSetIcmpTypeInstruction(InstructionBuilder ib, short type) {

        List<Action> actionList = new ArrayList<Action>();
        ActionBuilder ab = new ActionBuilder();
        SetFieldBuilder setFieldBuilder = new SetFieldBuilder();
        Icmpv4MatchBuilder icmpv4match = new Icmpv4MatchBuilder();

        // Build the ICMPv4 Code Match
        icmpv4match.setIcmpv4Code(type);
        setFieldBuilder.setIcmpv4Match(icmpv4match.build());

        ab.setAction(new SetFieldCaseBuilder().setSetField(setFieldBuilder.build()).build());
        ab.setKey(new ActionKey(1));
        actionList.add(ab.build());
        ApplyActionsBuilder aab = new ApplyActionsBuilder();
        aab.setAction(actionList);

        // Wrap our Apply Action in an Instruction
        ib.setInstruction(new ApplyActionsCaseBuilder().setApplyActions(aab.build()).build());

        return ib;
    }

    /**
     * Create Decrement TTL Instruction
     *
     * @param ib Map InstructionBuilder without any instructions
     * @return ib Map InstructionBuilder with instructions
     */
    private static InstructionBuilder createDecNwTtlInstructions(InstructionBuilder ib) {
        DecNwTtlBuilder decNwTtlBuilder = new DecNwTtlBuilder();
        DecNwTtl decNwTtl = decNwTtlBuilder.build();
        ActionBuilder ab = new ActionBuilder();
        ab.setAction(new DecNwTtlCaseBuilder().setDecNwTtl(decNwTtl).build());

        // Add our drop action to a list
        List<Action> actionList = new ArrayList<Action>();
        actionList.add(ab.build());

        // Create an Apply Action
        ApplyActionsBuilder aab = new ApplyActionsBuilder();
        aab.setAction(actionList);

        // Wrap our Apply Action in an Instruction
        ib.setInstruction(new ApplyActionsCaseBuilder().setApplyActions(aab.build()).build());

        return ib;
    }

    /**
     * Set Src Arp MAC
     */
    private static InstructionBuilder createSrcArpMacInstructions(InstructionBuilder ib, MacAddress macsrc) {

        List<Action> actionList = new ArrayList<Action>();
        ActionBuilder ab = new ActionBuilder();

        SetFieldBuilder setFieldBuilder = new SetFieldBuilder();
        ArpMatchBuilder arpmatch = new ArpMatchBuilder();
        ArpSourceHardwareAddressBuilder arpsrc = new ArpSourceHardwareAddressBuilder();
        arpsrc.setAddress(macsrc);
        arpmatch.setArpSourceHardwareAddress(arpsrc.build());
        setFieldBuilder.setLayer3Match(arpmatch.build());
        ab.setAction(new SetFieldCaseBuilder().setSetField(setFieldBuilder.build()).build());
        ab.setKey(new ActionKey(0));
        actionList.add(ab.build());

        ApplyActionsBuilder aab = new ApplyActionsBuilder();
        aab.setAction(actionList);

        return ib;
    }

    /**
     * Set Dst Arp MAC
     */
    private static InstructionBuilder createDstArpMacInstructions(InstructionBuilder ib, MacAddress macdst) {

        List<Action> actionList = new ArrayList<Action>();
        ActionBuilder ab = new ActionBuilder();
        SetFieldBuilder setFieldBuilder = new SetFieldBuilder();

        ArpMatchBuilder arpmatch = new ArpMatchBuilder();
        ArpTargetHardwareAddressBuilder arpdst = new ArpTargetHardwareAddressBuilder();
        arpdst.setAddress(macdst);
        setFieldBuilder.setLayer3Match(arpmatch.build());
        ab.setAction(new SetFieldCaseBuilder().setSetField(setFieldBuilder.build()).build());
        ab.setKey(new ActionKey(0));
        actionList.add(ab.build());

        ApplyActionsBuilder aab = new ApplyActionsBuilder();
        aab.setAction(actionList);

        return ib;
    }

    /**
     * Set Dst Arp IP
     */
    private static InstructionBuilder createDstArpIpInstructions(InstructionBuilder ib, Ipv4Prefix dstiparp) {

        List<Action> actionList = new ArrayList<Action>();
        ActionBuilder ab = new ActionBuilder();
        SetFieldBuilder setFieldBuilder = new SetFieldBuilder();

        ArpMatchBuilder arpmatch = new ArpMatchBuilder();
        arpmatch.setArpTargetTransportAddress(dstiparp);
        setFieldBuilder.setLayer3Match(arpmatch.build());
        ab.setAction(new SetFieldCaseBuilder().setSetField(setFieldBuilder.build()).build());
        ab.setKey(new ActionKey(0));
        actionList.add(ab.build());

        ApplyActionsBuilder aab = new ApplyActionsBuilder();
        aab.setAction(actionList);

        return ib;
    }

    /**
     * Set Src Arp IP
     */
    private static InstructionBuilder createSrcArpIpInstructions(InstructionBuilder ib, Ipv4Prefix srciparp) {

        List<Action> actionList = new ArrayList<Action>();
        ActionBuilder ab = new ActionBuilder();
        SetFieldBuilder setFieldBuilder = new SetFieldBuilder();

        ArpMatchBuilder arpmatch = new ArpMatchBuilder();
        arpmatch.setArpSourceTransportAddress(srciparp);
        setFieldBuilder.setLayer3Match(arpmatch.build());
        ab.setAction(new SetFieldCaseBuilder().setSetField(setFieldBuilder.build()).build());
        ab.setKey(new ActionKey(0));
        actionList.add(ab.build());

        ApplyActionsBuilder aab = new ApplyActionsBuilder();
        aab.setAction(actionList);

        return ib;
    }

    @Override
    public void initializeOFFlowRules(Node openflowNode) {
        IConnectionServiceInternal connectionService = (IConnectionServiceInternal)ServiceHelper.getGlobalInstance(IConnectionServiceInternal.class, this);
        List<Node> ovsNodes = connectionService.getNodes();
        if (ovsNodes == null) return;
        for (Node ovsNode : ovsNodes) {
            Long brIntDpid = this.getIntegrationBridgeOFDPID(ovsNode);
            Long brExDpid = this.getExternalBridgeOFDPID(ovsNode);
            logger.debug("Compare openflowNode to OVS node {} vs {} and {}", openflowNode.getID(), brIntDpid, brExDpid);
            String openflowID = ""+openflowNode.getID();
            if (openflowID.contains(""+brExDpid)) {
                this.initializeFlowRules(ovsNode, adminConfigManager.getExternalBridgeName());
                this.triggerInterfaceUpdates(ovsNode);
            }
            if (openflowID.contains(""+brIntDpid)) {
                this.initializeFlowRules(ovsNode, adminConfigManager.getIntegrationBridgeName());
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
        try {
            OVSDBConfigService ovsdbTable = (OVSDBConfigService)ServiceHelper.getGlobalInstance(OVSDBConfigService.class, this);
            Map<String, org.opendaylight.ovsdb.lib.table.internal.Table<?>> bridgeTable = ovsdbTable.getRows(node, Bridge.NAME.getName());
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
}
