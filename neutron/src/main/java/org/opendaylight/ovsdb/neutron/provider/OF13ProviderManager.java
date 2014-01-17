/**
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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.opendaylight.controller.md.sal.common.api.TransactionStatus;
import org.opendaylight.controller.md.sal.common.api.data.DataModification;
import org.opendaylight.controller.sal.binding.api.data.DataBrokerService;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.utils.HexEncode;
import org.opendaylight.controller.sal.utils.ServiceHelper;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.ovsdb.lib.table.Bridge;
import org.opendaylight.ovsdb.lib.table.Interface;
import org.opendaylight.ovsdb.neutron.AdminConfigManager;
import org.opendaylight.ovsdb.neutron.IMDSALConsumer;
import org.opendaylight.ovsdb.plugin.OVSDBConfigService;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.DecNwTtlCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.DropActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.OutputActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetFieldCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetNwDstActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetNwSrcActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetVlanIdActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.StripVlanActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.dec.nw.ttl._case.DecNwTtl;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.dec.nw.ttl._case.DecNwTtlBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.drop.action._case.DropAction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.drop.action._case.DropActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.field._case.SetFieldBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.nw.dst.action._case.SetNwDstActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.nw.src.action._case.SetNwSrcActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.vlan.id.action._case.SetVlanIdActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.strip.vlan.action._case.StripVlanAction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.strip.vlan.action._case.StripVlanActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.address.address.Ipv4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.GoToTableCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.go.to.table._case.GoToTableBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.VlanId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.arp.match.fields.ArpSourceHardwareAddressBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.arp.match.fields.ArpTargetHardwareAddressBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.ethernet.match.fields.EthernetDestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.ethernet.match.fields.EthernetSourceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.Icmpv4MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.IpMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.TunnelBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.VlanMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.ArpMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.Ipv4MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._4.match.TcpMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._4.match.UdpMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.vlan.match.fields.VlanIdBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.Nodes;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.EtherType;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowCapableNode;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.InstructionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.EthernetMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.ethernet.match.fields.EthernetTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.output.action._case.OutputActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.apply.actions._case.ApplyActionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.ApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.Table;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.TableKey;


/**
 *
 */
class OF13ProviderManager extends ProviderNetworkManager {
    private static final Logger logger = LoggerFactory.getLogger(OF13ProviderManager.class);
    private DataBrokerService dataBrokerService;

    @Override
    public boolean hasPerTenantTunneling() {
        return false;
    }

    @Override
    public Status createTunnels(String tunnelType, String tunnelKey, Node source, Interface intf) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Status createTunnels(String tunnelType, String tunnelKey) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void initializeFlowRules(Node node) {
        this.initializeFlowRules(node, AdminConfigManager.getManager().getIntegrationBridgeName());
    }

    /**
     * @param node
     * @param bridgeName
     */
    private void initializeFlowRules(Node node, String bridgeName) {

        try {
            // TODO : 3 second sleep hack is to make sure the OF connection is established.
            // Correct fix is to check the MD-SAL inventory before proceeding and listen
            // to Inventory update for processing.
            Thread.sleep(3000);
        } catch (Exception e) {
            e.printStackTrace();
        }
        String brIntId = this.getInternalBridgeUUID(node, bridgeName);
        if (brIntId == null) {
            logger.error("Failed to initialize Flow Rules for {}", node);
            return;
        }

        try {
            OVSDBConfigService ovsdbTable = (OVSDBConfigService) ServiceHelper.getGlobalInstance(OVSDBConfigService.class, this);
            Bridge bridge = (Bridge) ovsdbTable.getRow(node, Bridge.NAME.getName(), brIntId);
            Set<String> dpids = bridge.getDatapath_id();
            if (dpids == null || dpids.size() == 0) return;
            Long dpidLong = Long.valueOf(HexEncode.stringToLong((String) dpids.toArray()[0]));



            // TEMPORARY Variables :-)
            // Table 0,1,2 are passed for writing the table ID and GotoTable
            Short table0 = 0;
            Short table1 = 1;
            Short table2 = 2;

            Long OFPortOut = (long) 29;
            Long localPort = Long.valueOf(2);
            BigInteger tunnelId = new BigInteger(String.valueOf(100));
            MacAddress dMacAddr = new MacAddress("00:00:00:00:00:02");
            MacAddress sMacAddr = new MacAddress("00:00:00:00:00:01");
            MacAddress bcastAddr = new MacAddress("FF:FF:FF:FF:FF:FF");

            /*
            //Example Flow Rules
            table=0,tun_id=0x5,in_port=10, actions=goto_table:2
            table=0,tun_id=0x5,in_port=11 actions=goto_table:2
            table=0,in_port=2,dl_src=00:00:00:00:00:01 actions=set_field:5->tun_id,goto_table=1
            table=0,priority=16384,in_port=2 actions=drop

            table=1,tun_id=0x5,dl_dst=00:00:00:00:00:08 actions=output:11,goto_table:2
            table=1,tun_id=0x5,dl_dst=00:00:00:00:00:04 actions=output:10,goto_table:2
            table=1,tun_id=0x5,dl_dst=00:00:00:00:00:05 actions=output:10,goto_table:2
            table=1,priority=16384,tun_id=0x5,dl_dst=ff:ff:ff:ff:ff:ff actions=output:10,output:11,goto_table:2
            table=1,priority=8192,tun_id=0x5 actions=goto_table:2

            table=2,tun_id=0x5,dl_dst=00:00:00:00:00:01 actions=output:2
            table=2,priority=16384,tun_id=0x5,dl_dst=ff:ff:ff:ff:ff:ff actions=output:2
            table=2,priority=8192,tun_id=0x5 actions=drop
            */

           /*
            * (NOTES ONLY, DO NOT COMMIT)
            * Table #0 Flow Rules (Classifier Table):
            * #1 -------------------------------------------
            * table=0,tun_id=0x5,in_port=10, actions=goto_table:2
            * #2 -------------------------------------------
            * table=0,in_port=2,dl_src=00:00:00:00:00:01 actions=set_field:5->tun_id,goto_table=1
            * #3 -------------------------------------------
            * table=0,priority=16384,in_port=1 actions=drop"
            *
            * Parameter Order: DPID, flowmod TABLE, GotoTABLE, TunnelID, [TunnelPort||LocalPort], IPv4_src/dst, L4src/dst, L2 src/dst, VID
            */

           /*
            * Table(0) Rule #1
            * ----------------
            * Match: LLDP (0x88CCL)
            * Action: Packet_In to Controller Reserved Port
            */

            writeLLDPRule(dpidLong);

            /*
            * Table(0) Rule #2
            * ----------------
            * Match: Ingress Port, Tunnel ID
            * Action: GOTO Local Table (10)
            */

            writeTunnelIn(dpidLong, table0, table2, tunnelId, OFPortOut);

            /*
            * Table(0) Rule #3
            * ----------------
            * Match: VM sMac and Local Ingress Port
            * Action:Action: Set Tunnel ID and GOTO Local Table (5)
            */

            writeLocalInPort(dpidLong, table0, table1, tunnelId, localPort, bcastAddr);

           /*
            * Table(0) Rule #4
            * ----------------
            * Match: Drop any remaining Ingress Local VM Packets
            * Action: Drop w/ a low priority
            */

            writeDropSrcIface(dpidLong, localPort);

            /*
            * (NOTES ONLY, DO NOT COMMIT)
            * Table #1 Rules (Tunnel Related Flows):
            * #1 -------------------------------------------
            * table=1,tun_id=0x5,dl_dst=00:00:00:00:00:08 \
            * actions=output:11,goto_table:2
            * #2 -------------------------------------------
            * table=1,priority=16384,tun_id=0x5,dl_dst=ff:ff:ff:ff:ff:ff \
            * actions=output:10,output:11,goto_table:2
            * #3 -------------------------------------------
            * table=1,priority=8192,tun_id=0x5 actions=goto_table:2
            */

           /*
            * Table(1) Rule #1
            * ----------------
            * Match: Drop any remaining Ingress Local VM Packets
            * Action: Drop w/ a low priority
            * -------------------------------------------
            * table=1,tun_id=0x5,dl_dst=00:00:00:00:00:08 \
            * actions=output:11,goto_table:2
            */

            writeTunnelOut(dpidLong, table1, table2, tunnelId, OFPortOut, dMacAddr);

           /*
            * Table(1) Rule #2
            * ----------------
            * Match: Match Tunnel ID and L2 ::::FF:FF Flooding
            * Action: Flood to selected destination TEPs
            * -------------------------------------------
            * table=1,priority=16384,tun_id=0x5,dl_dst=ff:ff:ff:ff:ff:ff \
            * actions=output:10,output:11,goto_table:2
            */

            writeTunnelFloodOut(dpidLong, table1, table2, tunnelId, OFPortOut, dMacAddr);

           /*
            * Table(1) Rule #3
            * ----------------
            * Match:  Any remaining Ingress Local VM Packets
            * Action: Drop w/ a low priority
            * -------------------------------------------
            * table=1,priority=8192,tun_id=0x5 actions=goto_table:2
            */

            writeTunnelMiss(dpidLong, table1, table2, tunnelId);

           /*
            * (NOTES ONLY, DO NOT COMMIT)
            * Table 2 Rules (Local to Hypervisor)
            * -----------------------------------
            * table=2,tun_id=0x5,dl_dst=00:00:00:00:00:01 actions=output:2
            * table=2,priority=16384,tun_id=0x5,dl_dst=ff:ff:ff:ff:ff:ff \
            * actions=output:2
            * table=2,priority=8192,tun_id=0x5 actions=drop
            */

           /*
            * Table(2) Rule #1
            * ----------------
            * Match: Match TunID and Destination DL/dMAC Addr
            * Action: Output Port
            * table=2,tun_id=0x5,dl_dst=00:00:00:00:00:01 actions=output:2
            */

            writeLocalUcastOut(dpidLong, table2, tunnelId, localPort, dMacAddr);

           /*
            * Table(2) Rule #2
            * ----------------
            * Match: Drop any remaining Ingress Local VM Packets
            * Action: Forward to Local VMs
            * table=2,priority=16384,tun_id=0x5,dl_dst=ff:ff:ff:ff:ff:ff \
            * actions=output:2
            */

            writeLocalBcastOut(dpidLong, table2, tunnelId, localPort, dMacAddr);

           /*
            * Table(2) Rule #3
            * ----------------
            * Match: Any Remaining Flows w/a TunID
            * Action: Drop w/ a low priority
            * table=2,priority=8192,tun_id=0x5 actions=drop
            */

            writeLocalTableMiss(dpidLong, table2, tunnelId);

        } catch (Exception e) {
            logger.error("Failed to initialize Flow Rules for " + node.toString(), e);
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
        instructions.add(ib.build());

        // Add InstructionBuilder to the Instruction(s)Builder List
        isb.setInstruction(instructions);

        // Add InstructionsBuilder to FlowBuilder
        flowBuilder.setInstructions(isb.build());

        flowBuilder.setId(new FlowId("10"));
        FlowKey key = new FlowKey(new FlowId(String.valueOf((long) 110)));
        flowBuilder.setBarrier(true);
        flowBuilder.setTableId((short) 0);
        flowBuilder.setKey(key);
        flowBuilder.setFlowName("LLDP_" + nodeName);
        writeFlow(flowBuilder, nodeBuilder);
    }

    /*
     * (Table:0) Ingress Tunnel Traffic
     * Match: OpenFlow InPort and Tunnel ID
     * Action: GOTO Local Table (10)
     * table=0,tun_id=0x5,in_port=10, actions=goto_table:2
     */

    private void writeTunnelIn(Long dpidLong, Short writeTable, Short goToTableId, BigInteger tunnelId,  Long ofPort) {

        String nodeName = "openflow:" + dpidLong;

        MatchBuilder matchBuilder = new MatchBuilder();
        NodeBuilder nodeBuilder = createNodeBuilder(nodeName);
        FlowBuilder flowBuilder = new FlowBuilder();

        // Create Match(es) and Set them in the FlowBuilder Object
        flowBuilder.setMatch(createTunnelIDMatch(matchBuilder, tunnelId).build());
        flowBuilder.setMatch(createInPortMatch(matchBuilder, dpidLong, ofPort).build());

        // Create the OF Actions and Instructions
        InstructionBuilder ib = new InstructionBuilder();
        InstructionsBuilder isb = new InstructionsBuilder();

        // Instructions List Stores Individual Instructions
        List<Instruction> instructions = new ArrayList<Instruction>();

        // Call the InstructionBuilder Methods Containing Actions
        createGotoTableInstructions(ib, goToTableId);
        instructions.add(ib.build());

        // Add InstructionBuilder to the Instruction(s)Builder List
        isb.setInstruction(instructions);

        // Add InstructionsBuilder to FlowBuilder
        flowBuilder.setInstructions(isb.build());

        // Add Flow Attributes
        flowBuilder.setId(new FlowId("20"));
        FlowKey key = new FlowKey(new FlowId(String.valueOf((long) 120)));
        flowBuilder.setBarrier(false);
        flowBuilder.setTableId(writeTable);
        flowBuilder.setKey(key);
        flowBuilder.setFlowName("TUNIN_" + nodeName);
        writeFlow(flowBuilder, nodeBuilder);
    }

   /*
    * (Table:0) Egress VM Traffic Towards TEP
    * Match: Destination Ethernet Addr and OpenFlow InPort
    * Instruction: Set TunnelID and GOTO Table Tunnel Table (n)
    * table=0,in_port=2,dl_src=00:00:00:00:00:01 \
    * actions=set_field:5->tun_id,goto_table=1"
    */

    private void writeLocalInPort(Long dpidLong, Short writeTable, Short goToTableId, BigInteger tunnelId, Long inPort, MacAddress sMacAddr) {

        String nodeName = "openflow:" + dpidLong;

        MatchBuilder matchBuilder = new MatchBuilder();
        NodeBuilder nodeBuilder = createNodeBuilder(nodeName);
        FlowBuilder flowBuilder = new FlowBuilder();

        // Create the OF Match using MatchBuilder
        flowBuilder.setMatch(createEthSrcMatch(matchBuilder, sMacAddr).build());
        // TODO Broken In_Port Match
        flowBuilder.setMatch(createInPortMatch(matchBuilder, dpidLong, inPort).build());

        // Instantiate the Builders for the OF Actions and Instructions
        InstructionBuilder ib = new InstructionBuilder();
        InstructionsBuilder isb = new InstructionsBuilder();

        // Instructions List Stores Individual Instructions
        List<Instruction> instructions = new ArrayList<Instruction>();

        // GOTO Instuctions Need to be added first to the List
        createGotoTableInstructions(ib, goToTableId);
        instructions.add(ib.build());
        // TODO Broken SetTunID
        createSetTunnelIdInstructions(ib, tunnelId);
        instructions.add(ib.build());

        // Add InstructionBuilder to the Instruction(s)Builder List
        isb.setInstruction(instructions);

        // Add InstructionsBuilder to FlowBuilder
        flowBuilder.setInstructions(isb.build());

        // Add Flow Attributes
        flowBuilder.setId(new FlowId("30"));
        FlowKey key = new FlowKey(new FlowId(String.valueOf((long) 130)));
        flowBuilder.setBarrier(false);
        flowBuilder.setTableId(writeTable);
        flowBuilder.setKey(key);
        flowBuilder.setFlowName("LOCALSMAC_" + nodeName);
        writeFlow(flowBuilder, nodeBuilder);
    }

    /*
     * (Table:0) Drop frames sourced from a VM that do not
     * match the associated MAC address of the local VM.
     * Match: Low priority anything not matching the VM SMAC
     * Instruction: Drop
     * table=0,priority=16384,in_port=1 actions=drop"
     */

    private void writeDropSrcIface(Long dpidLong, Long inPort) {

        String nodeName = "openflow:" + dpidLong;

        MatchBuilder matchBuilder = new MatchBuilder();
        NodeBuilder nodeBuilder = createNodeBuilder(nodeName);
        FlowBuilder flowBuilder = new FlowBuilder();

        // Create the OF Match using MatchBuilder
        flowBuilder.setMatch(createInPortMatch(matchBuilder, dpidLong, inPort).build());

        // Instantiate the Builders for the OF Actions and Instructions
        InstructionBuilder ib = new InstructionBuilder();
        InstructionsBuilder isb = new InstructionsBuilder();

        // Instructions List Stores Individual Instructions
        List<Instruction> instructions = new ArrayList<Instruction>();

        // Call the InstructionBuilder Methods Containing Actions
        createDropInstructions(ib);
        instructions.add(ib.build());

        // Add InstructionBuilder to the Instruction(s)Builder List
        isb.setInstruction(instructions);

        // Add InstructionsBuilder to FlowBuilder
        flowBuilder.setInstructions(isb.build());

        // Add Flow Attributes
        flowBuilder.setId(new FlowId("40"));
        FlowKey key = new FlowKey(new FlowId(String.valueOf((long) 140)));
        flowBuilder.setBarrier(false);
        flowBuilder.setTableId((short) 0);
        flowBuilder.setKey(key);
        flowBuilder.setFlowName("LOCALDROP_" + nodeName);
        flowBuilder.setPriority(8192);
        writeFlow(flowBuilder, nodeBuilder);
    }

   /*
    * (Table:1) Egress Tunnel Traffic
    * Match: Destination Ethernet Addr and Local InPort
    * Instruction: Set TunnelID and GOTO Table Tunnel Table (n)
    * table=1,tun_id=0x5,dl_dst=00:00:00:00:00:08 \
    * actions=output:10,goto_table:2"
    */

    private void writeTunnelOut(Long dpidLong, Short writeTable, Short goToTableId, BigInteger tunnelId , Long OFPortOut, MacAddress dMacAddr) {

        String nodeName = "openflow:" + dpidLong;

        MatchBuilder matchBuilder = new MatchBuilder();
        NodeBuilder nodeBuilder = createNodeBuilder(nodeName);
        FlowBuilder flowBuilder = new FlowBuilder();

        // Create the OF Match using MatchBuilder
        flowBuilder.setMatch(createTunnelIDMatch(matchBuilder, tunnelId).build());
        flowBuilder.setMatch(createDestEthMatch(matchBuilder, dMacAddr).build());

        // Instantiate the Builders for the OF Actions and Instructions
        InstructionBuilder ib = new InstructionBuilder();
        InstructionsBuilder isb = new InstructionsBuilder();

        // Instructions List Stores Individual Instructions
        List<Instruction> instructions = new ArrayList<Instruction>();

        // GOTO Instuctions
        createGotoTableInstructions(ib, goToTableId);
        instructions.add(ib.build());
        // Set the Output Port/Iface
        createOutputPortInstructions(ib, dpidLong, OFPortOut);
        instructions.add(ib.build());

        // Add InstructionBuilder to the Instruction(s)Builder List
        isb.setInstruction(instructions);

        // Add InstructionsBuilder to FlowBuilder
        flowBuilder.setInstructions(isb.build());

        // Add Flow Attributes
        flowBuilder.setId(new FlowId("50"));
        FlowKey key = new FlowKey(new FlowId(String.valueOf((long) 150)));
        flowBuilder.setBarrier(false);
        flowBuilder.setTableId(writeTable);
        flowBuilder.setKey(key);
        flowBuilder.setFlowName("TUNOUT_" + nodeName);
        writeFlow(flowBuilder, nodeBuilder);
    }

       /*
    * (Table:1) Egress Tunnel Traffic
    * Match: Destination Ethernet Addr and Local InPort
    * Instruction: Set TunnelID and GOTO Table Tunnel Table (n)
    * table=1,priority=16384,tun_id=0x5,dl_dst=ff:ff:ff:ff:ff:ff \
    * actions=output:10,output:11,goto_table:2
    */

    private void writeTunnelFloodOut(Long dpidLong, Short writeTable, Short localTable, BigInteger tunnelId,  Long OFPortOut, MacAddress dMacAddr) {

        String nodeName = "openflow:" + dpidLong;

        MatchBuilder matchBuilder = new MatchBuilder();
        NodeBuilder nodeBuilder = createNodeBuilder(nodeName);
        FlowBuilder flowBuilder = new FlowBuilder();

        // Create the OF Match using MatchBuilder
        // Match TunnelID
        flowBuilder.setMatch(createTunnelIDMatch(matchBuilder, tunnelId).build());
        // Match DMAC
        flowBuilder.setMatch(createDestEthMatch(matchBuilder, dMacAddr).build());

        // Instantiate the Builders for the OF Actions and Instructions
        InstructionBuilder ib = new InstructionBuilder();
        InstructionsBuilder isb = new InstructionsBuilder();

        // Instructions List Stores Individual Instructions
        List<Instruction> instructions = new ArrayList<Instruction>();

        // GOTO Instuction
        createGotoTableInstructions(ib, localTable);
        instructions.add(ib.build());
        // Set the Output Port/Iface
        createOutputPortInstructions(ib, dpidLong, OFPortOut);
        instructions.add(ib.build());

        // Add InstructionBuilder to the Instruction(s)Builder List
        isb.setInstruction(instructions);

        // Add InstructionsBuilder to FlowBuilder
        flowBuilder.setInstructions(isb.build());

        // Add Flow Attributes
        flowBuilder.setId(new FlowId("60"));
        FlowKey key = new FlowKey(new FlowId(String.valueOf((long) 160)));
        flowBuilder.setBarrier(true);
        flowBuilder.setTableId(writeTable);
        flowBuilder.setKey(key);
        flowBuilder.setPriority(16384);
        flowBuilder.setFlowName("TUNFLOODOUT_" + nodeName);
        writeFlow(flowBuilder, nodeBuilder);
    }

   /*
    * (Table:1) Table Drain w/ Catch All
    * Match: Tunnel ID
    * Action: GOTO Local Table (10)
    * table=2,priority=8192,tun_id=0x5 actions=drop
    */

    private void writeTunnelMiss(Long dpidLong, Short writeTable, Short goToTableId, BigInteger tunnelId) {

        String nodeName = "openflow:" + dpidLong;

        MatchBuilder matchBuilder = new MatchBuilder();
        NodeBuilder nodeBuilder = createNodeBuilder(nodeName);
        FlowBuilder flowBuilder = new FlowBuilder();

        // Create Match(es) and Set them in the FlowBuilder Object
        flowBuilder.setMatch(createTunnelIDMatch(matchBuilder, tunnelId).build());

        // Create the OF Actions and Instructions
        InstructionBuilder ib = new InstructionBuilder();
        InstructionsBuilder isb = new InstructionsBuilder();

        // Instructions List Stores Individual Instructions
        List<Instruction> instructions = new ArrayList<Instruction>();

        // Call the InstructionBuilder Methods Containing Actions
        createGotoTableInstructions(ib, goToTableId);
        instructions.add(ib.build());

        // Add InstructionBuilder to the Instruction(s)Builder List
        isb.setInstruction(instructions);

        // Add InstructionsBuilder to FlowBuilder
        flowBuilder.setInstructions(isb.build());

        // Add Flow Attributes
        flowBuilder.setId(new FlowId("70"));
        FlowKey key = new FlowKey(new FlowId(String.valueOf((long) 170)));
        flowBuilder.setBarrier(false);
        flowBuilder.setTableId(writeTable);
        flowBuilder.setKey(key);
        flowBuilder.setPriority(8192);
        flowBuilder.setFlowName("TUNMISS_" + nodeName);
        writeFlow(flowBuilder, nodeBuilder);
    }

    /*
     * (Table:1) Local Broadcast Flood
     * Match: Tunnel ID and dMAC
     * Action: Output Port
     * table=2,tun_id=0x5,dl_dst=00:00:00:00:00:01 actions=output:2
     */

    private void writeLocalUcastOut(Long dpidLong, Short writeTable, BigInteger tunnelId, Long localPort, MacAddress dMacAddr) {

        String nodeName = "openflow:" + dpidLong;

        MatchBuilder matchBuilder = new MatchBuilder();
        NodeBuilder nodeBuilder = createNodeBuilder(nodeName);
        FlowBuilder flowBuilder = new FlowBuilder();

        // Create the OF Match using MatchBuilder
        flowBuilder.setMatch(createTunnelIDMatch(matchBuilder, tunnelId).build());
        flowBuilder.setMatch(createDestEthMatch(matchBuilder, dMacAddr).build());

        // Instantiate the Builders for the OF Actions and Instructions
        InstructionBuilder ib = new InstructionBuilder();
        InstructionsBuilder isb = new InstructionsBuilder();

        // Instructions List Stores Individual Instructions
        List<Instruction> instructions = new ArrayList<Instruction>();

        // Set the Output Port/Iface
        createOutputPortInstructions(ib, dpidLong, localPort);
        instructions.add(ib.build());

        // Add InstructionBuilder to the Instruction(s)Builder List
        isb.setInstruction(instructions);

        // Add InstructionsBuilder to FlowBuilder
        flowBuilder.setInstructions(isb.build());

        // Add Flow Attributes
        flowBuilder.setId(new FlowId("80"));
        FlowKey key = new FlowKey(new FlowId(String.valueOf((long) 180)));
        flowBuilder.setBarrier(false);
        flowBuilder.setTableId(writeTable);
        flowBuilder.setKey(key);
        flowBuilder.setFlowName("LOCALHOSTUCAST_" + nodeName);
        writeFlow(flowBuilder, nodeBuilder);
    }

    /*
     * (Table:1) Local Broadcast Flood
     * Match: Tunnel ID and dMAC (::::FF:FF)
     * table=2,priority=16384,tun_id=0x5,dl_dst=ff:ff:ff:ff:ff:ff \
     * actions=output:2,3,4,5
     */

    private void writeLocalBcastOut(Long dpidLong, Short writeTable, BigInteger tunnelId, Long localPort, MacAddress dMacAddr) {

        String nodeName = "openflow:" + dpidLong;

        MatchBuilder matchBuilder = new MatchBuilder();
        NodeBuilder nodeBuilder = createNodeBuilder(nodeName);
        FlowBuilder flowBuilder = new FlowBuilder();

        // Create the OF Match using MatchBuilder
        flowBuilder.setMatch(createTunnelIDMatch(matchBuilder, tunnelId).build());
        flowBuilder.setMatch(createDestEthMatch(matchBuilder, dMacAddr).build());

        // Instantiate the Builders for the OF Actions and Instructions
        InstructionBuilder ib = new InstructionBuilder();
        InstructionsBuilder isb = new InstructionsBuilder();

        // Instructions List Stores Individual Instructions
        List<Instruction> instructions = new ArrayList<Instruction>();

        // Broken OutPort TODO: localPort needs to be a list of Ports)
        createOutputPortInstructions(ib, dpidLong, localPort);
        instructions.add(ib.build());

        // Add InstructionBuilder to the Instruction(s)Builder List
        isb.setInstruction(instructions);

        // Add InstructionsBuilder to FlowBuilder
        flowBuilder.setInstructions(isb.build());

        // Add Flow Attributes
        flowBuilder.setId(new FlowId("90"));
        FlowKey key = new FlowKey(new FlowId(String.valueOf((long) 190)));
        flowBuilder.setBarrier(false);
        flowBuilder.setTableId(writeTable);
        flowBuilder.setKey(key);
        flowBuilder.setPriority(16384);
        flowBuilder.setFlowName("LOCALHOSTBCAST_" + nodeName);
        writeFlow(flowBuilder, nodeBuilder);
    }

    /*
     * (Table:1) Local Table Miss
     * Match: Any Remaining Flows w/a TunID
     * Action: Drop w/ a low priority
     * table=2,priority=8192,tun_id=0x5 actions=drop
     */

    private void writeLocalTableMiss(Long dpidLong, Short writeTable, BigInteger tunnelId) {

        String nodeName = "openflow:" + dpidLong;

        MatchBuilder matchBuilder = new MatchBuilder();
        NodeBuilder nodeBuilder = createNodeBuilder(nodeName);
        FlowBuilder flowBuilder = new FlowBuilder();

        // Create Match(es) and Set them in the FlowBuilder Object
        flowBuilder.setMatch(createTunnelIDMatch(matchBuilder, tunnelId).build());

        // Create the OF Actions and Instructions
        InstructionBuilder ib = new InstructionBuilder();
        InstructionsBuilder isb = new InstructionsBuilder();

        // Instructions List Stores Individual Instructions
        List<Instruction> instructions = new ArrayList<Instruction>();

        // Call the InstructionBuilder Methods Containing Actions
        createDropInstructions(ib);
        instructions.add(ib.build());

        // Add InstructionBuilder to the Instruction(s)Builder List
        isb.setInstruction(instructions);

        // Add InstructionsBuilder to FlowBuilder
        flowBuilder.setInstructions(isb.build());

        // Add Flow Attributes
        flowBuilder.setId(new FlowId("100"));
        FlowKey key = new FlowKey(new FlowId(String.valueOf((long) 200)));
        flowBuilder.setBarrier(false);
        flowBuilder.setTableId(writeTable);
        flowBuilder.setKey(key);
        flowBuilder.setPriority(8192);
        flowBuilder.setFlowName("TUNMISS_" + nodeName);
        writeFlow(flowBuilder, nodeBuilder);
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
        modification.putOperationalData(nodeBuilderToInstanceId(nodeBuilder), nodeBuilder.build());
        modification.putOperationalData(path1, flowBuilder.build());
        modification.putConfigurationData(nodeBuilderToInstanceId(nodeBuilder), nodeBuilder.build());
        modification.putConfigurationData(path1, flowBuilder.build());
        Future<RpcResult<TransactionStatus>> commitFuture = modification.commit();
        try {
            RpcResult<TransactionStatus> result = commitFuture.get();
            TransactionStatus status = result.getResult();
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

    protected static MatchBuilder createDestEthMatch(MatchBuilder matchBuilder, MacAddress dMacAddr) {

        EthernetMatchBuilder ethernetMatch = new EthernetMatchBuilder();
        EthernetDestinationBuilder ethDestinationBuilder = new EthernetDestinationBuilder();
        ethDestinationBuilder.setAddress(new MacAddress(dMacAddr));
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
        output.setMaxLength(56);
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
        ib.setOrder(0);
        ib.setKey(new InstructionKey(0));

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
        ab.setOrder(5);
        ab.setKey(new ActionKey(5));
        actionList.add(ab.build());

        // Create an Apply Action
        ApplyActionsBuilder aab = new ApplyActionsBuilder();
        aab.setAction(actionList);
        ib.setInstruction(new ApplyActionsCaseBuilder().setApplyActions(aab.build()).build());

        return ib;
    }

    /**
     * Create Set Vlan ID Instruction
     *
     * @param ib     Map InstructionBuilder without any instructions
     * @param vlanId Integer representing a VLAN ID Integer representing a VLAN ID
     * @return ib Map InstructionBuilder with instructions
     */
    protected static InstructionBuilder createSetVlanInstructions(InstructionBuilder ib, VlanId vlanId) {

        List<Action> actionList = new ArrayList<Action>();
        ActionBuilder ab = new ActionBuilder();

        SetVlanIdActionBuilder vl = new SetVlanIdActionBuilder();
        vl.setVlanId(vlanId);
        ab.setAction(new SetVlanIdActionCaseBuilder().setSetVlanIdAction(vl.build()).build());
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
     * @param ib Map InstructionBuilder without any instructions
     * @return ib Map InstructionBuilder with instructions
     */
    protected static InstructionBuilder createStripVlanInstructions(InstructionBuilder ib) {

        StripVlanActionBuilder stripVlanActionBuilder = new StripVlanActionBuilder();
        StripVlanAction vlanAction = stripVlanActionBuilder.build();
        ActionBuilder ab = new ActionBuilder();
        ab.setAction(new StripVlanActionCaseBuilder().setStripVlanAction(vlanAction).build());

        // Add our drop action to a list
        List<Action> actionList = new ArrayList<Action>();
        actionList.add(ab.build());

        // Create an Apply Action
        ApplyActionsBuilder aab = new ApplyActionsBuilder();
        aab.setAction(actionList);

        // Wrap our Apply Action in an Instruction
        ib.setInstruction(new ApplyActionsCaseBuilder().setApplyActions(aab.build()).build());
        ib.setOrder(0);
        ib.setKey(new InstructionKey(0));

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

        // Add our drop action to a list
        List<Action> actionList = new ArrayList<Action>();
        actionList.add(ab.build());

        // Create an Apply Action
        ApplyActionsBuilder aab = new ApplyActionsBuilder();
        aab.setAction(actionList);

        // Wrap our Apply Action in an Instruction
        ib.setInstruction(new ApplyActionsCaseBuilder().setApplyActions(aab.build()).build());
        ib.setOrder(0);
        ib.setKey(new InstructionKey(0));

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
        ib.setOrder(0);
        ib.setKey(new InstructionKey(0));

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
        actionList.add(ab.build());

        ApplyActionsBuilder aab = new ApplyActionsBuilder();
        aab.setAction(actionList);

        // Wrap the Apply Action in an InstructionBuilder and return
        ib.setOrder(0);
        ib.setKey(new InstructionKey(0));
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
        ib.setKey(new InstructionKey(1));
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
        ib.setKey(new InstructionKey(1));
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
        ib.setKey(new InstructionKey(1));
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
        ib.setKey(new InstructionKey(1));
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
        ib.setKey(new InstructionKey(0));
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
        ib.setKey(new InstructionKey(1));
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
        ib.setKey(new InstructionKey(0));
        ib.setOrder(0);

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
}