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
import org.junit.Test;
import org.opendaylight.controller.sal.utils.HexEncode;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.InstructionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.EtherType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.opendaylight.ovsdb.neutron.provider.OF13ProviderManager.createDestEthMatch;
import static org.opendaylight.ovsdb.neutron.provider.OF13ProviderManager.createDropInstructions;
import static org.opendaylight.ovsdb.neutron.provider.OF13ProviderManager.createEthSrcMatch;
import static org.opendaylight.ovsdb.neutron.provider.OF13ProviderManager.createEtherTypeMatch;
import static org.opendaylight.ovsdb.neutron.provider.OF13ProviderManager.createGotoTableInstructions;
import static org.opendaylight.ovsdb.neutron.provider.OF13ProviderManager.createInPortMatch;
import static org.opendaylight.ovsdb.neutron.provider.OF13ProviderManager.createOutputPortInstructions;
import static org.opendaylight.ovsdb.neutron.provider.OF13ProviderManager.createSendToControllerInstructions;
import static org.opendaylight.ovsdb.neutron.provider.OF13ProviderManager.createSetTunnelIdInstructions;
import static org.opendaylight.ovsdb.neutron.provider.OF13ProviderManager.createTunnelIDMatch;

public class OF13ProviderManagerTestIT {
    private static final Logger logger = LoggerFactory.getLogger(OF13ProviderManagerTestIT.class);

    /**
     * Create an LLDP Flow Rule to encapsulate into
     * a packet_in that is sent to the controller
     * for topology handling.
     * Match: Ethertype 0x88CCL
     * Action: Punt to Controller in a Packet_In msg
     */
    @Test
    public void testLLDPRules() throws Exception {

        String dpidLong = "00:00:00:00:00:01";
        String nodeName = "openflow:" + dpidLong;
        EtherType etherType = new EtherType(0x88CCL);

        MatchBuilder matchBuilder = new MatchBuilder();
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

        FlowKey key = new FlowKey(new FlowId(String.valueOf((long) 100)));
        flowBuilder.setBarrier(true);
        flowBuilder.setTableId((short) 0);
        flowBuilder.setKey(key);
        flowBuilder.setFlowName("LLDP_" + nodeName);
    }

    /**
     * Test Ingress Tunnel Traffic Flow Rule Creation
     * Match: OpenFlow InPort and Tunnel ID
     * Action: GOTO Local Table (n)
     */
    @Test
    public void testTunnelIn() {

        Long ofPort = (long) 1;
        Long dpidLong = HexEncode.stringToLong("00:00:00:00:00:01");
        BigInteger tunnelId = new BigInteger(String.valueOf(100));
        Short writeTable = 0;
        Short goToTableId = 1;
        String nodeName = "openflow:" + dpidLong;

        MatchBuilder matchBuilder = new MatchBuilder();
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

        long flowKey = 110;
        FlowKey key = new FlowKey(new FlowId(Long.toString(flowKey)));
        flowBuilder.setBarrier(false);
        flowBuilder.setTableId(writeTable);
        flowBuilder.setKey(key);
        flowBuilder.setFlowName("TUNIN_" + nodeName);

    }

    /**
     * Test Egress VM Traffic Towards TEP
     * Match: Destination Ethernet Addr and OpenFlow InPort
     * Instruction: Set TunnelID and GOTO Table Tunnel Table (n)
     */
    @Test
    public void testLocalInPort() {

        Long ofPort = (long) 1;
        Long dpidLong = HexEncode.stringToLong("00:00:00:00:00:01");
        BigInteger tunnelId = new BigInteger(String.valueOf(100));
        MacAddress sMacAddr = new MacAddress("00:00:00:00:00:02");
        Short writeTable = 0;
        Short goToTableId = 1;
        String nodeName = "openflow:" + dpidLong;

        MatchBuilder matchBuilder = new MatchBuilder();
        FlowBuilder flowBuilder = new FlowBuilder();

        // Create the OF Match using MatchBuilder
        flowBuilder.setMatch(createEthSrcMatch(matchBuilder, sMacAddr).build());
        // Set the Output Port/Iface
        flowBuilder.setMatch(createInPortMatch(matchBuilder, dpidLong, ofPort).build());

        // Instantiate the Builders for the OF Actions and Instructions
        InstructionBuilder ib = new InstructionBuilder();
        InstructionsBuilder isb = new InstructionsBuilder();

        // Instructions List Stores Individual Instructions
        List<Instruction> instructions = new ArrayList<Instruction>();

        createGotoTableInstructions(ib, goToTableId);
        instructions.add(ib.build());
        // Set Tunnel ID Instruction
        createSetTunnelIdInstructions(ib, tunnelId);
        instructions.add(ib.build());

        // Add InstructionBuilder to the Instruction(s)Builder List
        isb.setInstruction(instructions);

        // Add InstructionsBuilder to FlowBuilder
        flowBuilder.setInstructions(isb.build());

        // Add Flow Attributes
        long flowKey = 120;
        FlowKey key = new FlowKey(new FlowId(Long.toString(flowKey)));
        flowBuilder.setBarrier(false);
        flowBuilder.setTableId(writeTable);
        flowBuilder.setKey(key);
        flowBuilder.setFlowName("LOCALSMAC_" + nodeName);

    }

    /**
     * Test Drop frames sourced from a VM that do not
     * match the associated MAC address of the local VM.
     * Match: Low priority anything not matching the VM SMAC
     * Instruction: Drop
     */
    @Test
    public void testDropSrcIface() {

        Long ofPort = (long) 1;
        String dpid = "00:00:00:00:00:01";
        Long dpidLong = HexEncode.stringToLong(dpid);

        String nodeName = "openflow:" + dpidLong;
        MatchBuilder matchBuilder = new MatchBuilder();
        FlowBuilder flowBuilder = new FlowBuilder();

        // Create the OF Match using MatchBuilder
        flowBuilder.setMatch(createInPortMatch(matchBuilder, dpidLong, ofPort).build());

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

        long flowKey = 130;
        FlowKey key = new FlowKey(new FlowId(Long.toString(flowKey)));
        flowBuilder.setBarrier(false);
        flowBuilder.setTableId((short) 0);
        flowBuilder.setKey(key);
        flowBuilder.setFlowName("LOCALDROP_" + nodeName);
        flowBuilder.setPriority(8192);

    }

    /**
     * Test Egress Tunnel Traffic
     * Match: Destination Ethernet Addr and Local InPort
     * Instruction: Set TunnelID and GOTO Table Tunnel Table (n)
     */
    @Test
    public void testTunnelOut() {

        Long ofPort = (long) 1;
        String dpid = "00:00:00:00:00:01";
        Long dpidLong = HexEncode.stringToLong(dpid);
        BigInteger tunnelId = new BigInteger(String.valueOf(100));
        MacAddress dMacAddr = new MacAddress("00:00:00:00:00:03");
        Short writeTable = 0;
        Short goToTableId = 1;

        String nodeName = "openflow:" + dpidLong;
        MatchBuilder matchBuilder = new MatchBuilder();
        FlowBuilder flowBuilder = new FlowBuilder();

        // Create the OF Match using MatchBuilder
        flowBuilder.setMatch(createTunnelIDMatch(matchBuilder, tunnelId).build());
        flowBuilder.setMatch(createDestEthMatch(matchBuilder, dMacAddr).build());

        // Instantiate the Builders for the OF Actions and Instructions
        InstructionBuilder ib = new InstructionBuilder();
        InstructionsBuilder isb = new InstructionsBuilder();

        // Instructions List Stores Individual Instructions
        List<Instruction> instructions = new ArrayList<Instruction>();

        // GOTO Table Instuctions
        createGotoTableInstructions(ib, goToTableId);
        instructions.add(ib.build());
        // Set the Output Port/Iface
        createOutputPortInstructions(ib, dpidLong, ofPort);
        instructions.add(ib.build());

        // Add InstructionBuilder to the Instruction(s)Builder List
        isb.setInstruction(instructions);

        // Add InstructionsBuilder to FlowBuilder
        flowBuilder.setInstructions(isb.build());

        long flowKey = 140;
        FlowKey key = new FlowKey(new FlowId(Long.toString(flowKey)));
        flowBuilder.setBarrier(false);
        flowBuilder.setTableId(writeTable);
        flowBuilder.setKey(key);
        flowBuilder.setFlowName("TUNOUT_" + nodeName);
    }

    /**
     * Test Egress Tunnel Traffic
     * Match: Destination Ethernet Addr and Local InPort
     * Instruction: Set TunnelID and GOTO Table Tunnel Table (n)
     */
    @Test
    public void testTunnelFloodOut() {

        Long ofPort = (long) 1;
        String dpid = "00:00:00:00:00:01";
        Long dpidLong = HexEncode.stringToLong(dpid);
        BigInteger tunnelId = new BigInteger(String.valueOf(100));
        MacAddress dMacAddr = new MacAddress("00:00:00:00:00:03");
        Short writeTable = 0;
        Short goToTableId = 1;

        String nodeName = "openflow:" + dpidLong;
        MatchBuilder matchBuilder = new MatchBuilder();
        FlowBuilder flowBuilder = new FlowBuilder();

        // Create Match(es) and Set them in the FlowBuilder Object
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
        createGotoTableInstructions(ib, goToTableId);
        instructions.add(ib.build());
        // Set the Output Port/Iface
        createOutputPortInstructions(ib, dpidLong, ofPort);
        instructions.add(ib.build());

        // Add InstructionBuilder to the Instruction(s)Builder List
        isb.setInstruction(instructions);

        // Add InstructionsBuilder to FlowBuilder
        flowBuilder.setInstructions(isb.build());

        long flowKey = 150;
        FlowKey key = new FlowKey(new FlowId(Long.toString(flowKey)));
        flowBuilder.setBarrier(false);
        flowBuilder.setTableId(writeTable);
        flowBuilder.setKey(key);
        flowBuilder.setPriority(16384);
        flowBuilder.setFlowName("TUNFLOODOUT_" + nodeName);
    }

    /**
     * Test Table Drain w/ Catch All
     * Match: Tunnel ID
     * Action: GOTO Local Table (10)
     */
    @Test
    public void testTunnelMiss() {

        String dpid = "00:00:00:00:00:01";
        Long dpidLong = HexEncode.stringToLong(dpid);
        BigInteger tunnelId = new BigInteger(String.valueOf(100));
        Short writeTable = 0;
        Short goToTableId = 1;
        String nodeName = "openflow:" + dpidLong;

        MatchBuilder matchBuilder = new MatchBuilder();
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

        long flowKey = 160;
        FlowKey key = new FlowKey(new FlowId(Long.toString(flowKey)));
        flowBuilder.setBarrier(false);
        flowBuilder.setTableId(writeTable);
        flowBuilder.setKey(key);
        flowBuilder.setPriority(8192);
        flowBuilder.setFlowName("TUNMISS_" + nodeName);
    }

    /**
     * Test Local Broadcast Flood
     * Match: Tunnel ID and dMAC
     * Action: Output Port
     */
    @Test
    public void testUcastOut() {

        Long localport = (long) 2;
        String dpid = "00:00:00:00:00:01";
        Long dpidLong = HexEncode.stringToLong(dpid);
        BigInteger tunnelId = new BigInteger(String.valueOf(100));
        MacAddress dMacAddr = new MacAddress("00:00:00:00:00:03");
        Short writeTable = 0;
        String nodeName = "openflow:" + dpidLong;

        MatchBuilder matchBuilder = new MatchBuilder();
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
        createOutputPortInstructions(ib, dpidLong, localport);
        instructions.add(ib.build());

        // Add InstructionBuilder to the Instruction(s)Builder List
        isb.setInstruction(instructions);

        // Add InstructionsBuilder to FlowBuilder
        flowBuilder.setInstructions(isb.build());

        long flowKey = 170;
        FlowKey key = new FlowKey(new FlowId(Long.toString(flowKey)));
        flowBuilder.setBarrier(false);
        flowBuilder.setTableId(writeTable);
        flowBuilder.setKey(key);
        flowBuilder.setFlowName("LOCALHOSTUCAST_" + nodeName);
    }

    /**
     * Test Local Broadcast Flood
     * Match: Tunnel ID and dMAC (::::FF:FF)
     * Action: Forward to appropriate TEPs
     */
    @Test
    public void testBcastOut() {

        Long ofPort = (long) 1;
        String dpid = "00:00:00:00:00:01";
        Long dpidLong = HexEncode.stringToLong(dpid);
        BigInteger tunnelId = new BigInteger(String.valueOf(100));
        MacAddress dMacAddr = new MacAddress("00:00:00:00:00:03");
        Short writeTable = 0;
        String nodeName = "openflow:" + dpidLong;

        MatchBuilder matchBuilder = new MatchBuilder();
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
        createOutputPortInstructions(ib, dpidLong, ofPort);
        instructions.add(ib.build());

        // Add InstructionBuilder to the Instruction(s)Builder List
        isb.setInstruction(instructions);

        // Add InstructionsBuilder to FlowBuilder
        flowBuilder.setInstructions(isb.build());

        long flowKey = 180;
        FlowKey key = new FlowKey(new FlowId(Long.toString(flowKey)));
        flowBuilder.setBarrier(false);
        flowBuilder.setTableId(writeTable);
        flowBuilder.setKey(key);
        flowBuilder.setPriority(16384);
        flowBuilder.setFlowName("LOCALHOSTBCAST_" + nodeName);
    }

    /**
     * Test Local Table Miss
     * Match: Any Remaining Flows w/a TunID
     * Action: Drop w/ a low priority
     */
    @Test
    public void testLocalTableMiss() {

        String dpid = "00:00:00:00:00:01";
        Long dpidLong = HexEncode.stringToLong(dpid);
        BigInteger tunnelId = new BigInteger(String.valueOf(100));
        Short writeTable = 0;
        String nodeName = "openflow:" + dpidLong;

        MatchBuilder matchBuilder = new MatchBuilder();
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

        long flowKey = 190;
        FlowKey key = new FlowKey(new FlowId(Long.toString(flowKey)));
        flowBuilder.setBarrier(false);
        flowBuilder.setTableId(writeTable);
        flowBuilder.setKey(key);
        flowBuilder.setPriority(8192);
        flowBuilder.setFlowName("TUNMISS_" + nodeName);
    }
}