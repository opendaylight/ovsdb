/*
 * Copyright (C) 2014 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Madhu Venugopal
 */
package org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.services;

import java.math.BigInteger;
import java.util.List;

import org.opendaylight.ovsdb.openstack.netvirt.api.ClassifierProvider;
import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.AbstractServiceInstance;
import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.Service;
import org.opendaylight.ovsdb.utils.mdsal.openflow.InstructionUtils;
import org.opendaylight.ovsdb.utils.mdsal.openflow.MatchUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.InstructionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.EtherType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.VlanId;

import com.google.common.collect.Lists;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.VlanMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.vlan.match.fields.VlanIdBuilder;

public class ClassifierService extends AbstractServiceInstance implements ClassifierProvider {
    public ClassifierService() {
        super(Service.CLASSIFIER);
    }

    public ClassifierService(Service service) {
        super(service);
    }

    @Override
    public boolean isBridgeInPipeline (String nodeId) {
        return true;
    }

    /*
     * (Table:Classifier) Egress VM Traffic Towards TEP
     * Match: Destination Ethernet Addr and OpenFlow InPort
     * Instruction: Set TunnelID and GOTO Table Tunnel Table (n)
     * table=0,in_port=2,dl_src=00:00:00:00:00:01 \
     * actions=set_field:5->tun_id,goto_table=<next-table>"
     */

    @Override
    public void programLocalInPort(Long dpidLong, String segmentationId, Long inPort, String attachedMac, boolean write) {
        String nodeName = OPENFLOW + dpidLong;

        MatchBuilder matchBuilder = new MatchBuilder();
        NodeBuilder nodeBuilder = createNodeBuilder(nodeName);
        FlowBuilder flowBuilder = new FlowBuilder();

        // Create the OF Match using MatchBuilder
        flowBuilder.setMatch(MatchUtils.createEthSrcMatch(matchBuilder, new MacAddress(attachedMac)).build());
        // TODO Broken In_Port Match
        flowBuilder.setMatch(MatchUtils.createInPortMatch(matchBuilder, dpidLong, inPort).build());

        String flowId = "LocalMac_"+segmentationId+"_"+inPort+"_"+attachedMac;
        // Add Flow Attributes
        flowBuilder.setId(new FlowId(flowId));
        FlowKey key = new FlowKey(new FlowId(flowId));
        flowBuilder.setStrict(true);
        flowBuilder.setBarrier(false);
        flowBuilder.setTableId(getTable());
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
            ib = this.getMutablePipelineInstructionBuilder();
            ib.setOrder(0);
            ib.setKey(new InstructionKey(0));
            instructions.add(ib.build());
            // TODO Broken SetTunID
            InstructionUtils.createSetTunnelIdInstructions(ib, new BigInteger(segmentationId));
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

    public static MatchBuilder createVlanIdPresentMatch(MatchBuilder matchBuilder, boolean present) {
        VlanMatchBuilder vlanMatchBuilder = new VlanMatchBuilder();
        VlanIdBuilder vlanIdBuilder = new VlanIdBuilder();
        vlanIdBuilder.setVlanIdPresent(present);
        vlanIdBuilder.setVlanId(new VlanId(0));
        vlanMatchBuilder.setVlanId(vlanIdBuilder.build());
        matchBuilder.setVlanMatch(vlanMatchBuilder.build());

        return matchBuilder;
    }
    /*
     * (Table:Classifier) Egress VM Traffic Towards TEP
     * Match: Source Ethernet Addr and OpenFlow InPort
     * Instruction: Set VLANID and GOTO Table Egress (n)
     * table=0,in_port=2,dl_src=00:00:00:00:00:01 \
     * actions=push_vlan, set_field:5->vlan_id,goto_table=<Next-Table>"
     */

    @Override
    public void programLocalInPortSetVlan(Long dpidLong, String segmentationId, Long inPort, String attachedMac, boolean write) {

        String nodeName = OPENFLOW + dpidLong;

        MatchBuilder matchBuilder = new MatchBuilder();
        NodeBuilder nodeBuilder = createNodeBuilder(nodeName);
        FlowBuilder flowBuilder = new FlowBuilder();

        // Create the OF Match using MatchBuilder
        flowBuilder.setMatch(MatchUtils.createEthSrcMatch(matchBuilder, new MacAddress(attachedMac)).build());
        flowBuilder.setMatch(MatchUtils.createInPortMatch(matchBuilder, dpidLong, inPort).build());
        flowBuilder.setMatch(createVlanIdPresentMatch(matchBuilder, false).build());

        String flowId = "LocalMac_"+segmentationId+"_"+inPort+"_"+attachedMac;
        // Add Flow Attributes
        flowBuilder.setId(new FlowId(flowId));
        FlowKey key = new FlowKey(new FlowId(flowId));
        flowBuilder.setStrict(true);
        flowBuilder.setBarrier(false);
        flowBuilder.setTableId(getTable());
        flowBuilder.setKey(key);
        flowBuilder.setFlowName(flowId);
        flowBuilder.setPriority(18000);
        flowBuilder.setHardTimeout(0);
        flowBuilder.setIdleTimeout(0);

        if (write) {
            // Instantiate the Builders for the OF Actions and Instructions
            InstructionBuilder ib = new InstructionBuilder();
            InstructionsBuilder isb = new InstructionsBuilder();

            // Instructions List Stores Individual Instructions
            List<Instruction> instructions = Lists.newArrayList();

            // Set VLAN ID Instruction
            InstructionUtils.createSetVlanInstructions(ib, new VlanId(Integer.valueOf(segmentationId)));
            ib.setOrder(0);
            ib.setKey(new InstructionKey(0));
            instructions.add(ib.build());

            // GOTO Instructions Need to be added first to the List
            ib = this.getMutablePipelineInstructionBuilder();
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
     * (Table:Classifier) Drop frames source from a VM that do not
     * match the associated MAC address of the local VM.
     * Match: Low priority anything not matching the VM SMAC
     * Instruction: Drop
     * table=0,priority=16384,in_port=1 actions=drop"
     */

    @Override
    public void programDropSrcIface(Long dpidLong, Long inPort, boolean write) {

        String nodeName = OPENFLOW + dpidLong;

        MatchBuilder matchBuilder = new MatchBuilder();
        NodeBuilder nodeBuilder = createNodeBuilder(nodeName);
        FlowBuilder flowBuilder = new FlowBuilder();

        // Create the OF Match using MatchBuilder
        flowBuilder.setMatch(MatchUtils.createInPortMatch(matchBuilder, dpidLong, inPort).build());

        if (write) {
            // Instantiate the Builders for the OF Actions and Instructions
            InstructionBuilder ib = new InstructionBuilder();
            InstructionsBuilder isb = new InstructionsBuilder();

            // Instructions List Stores Individual Instructions
            List<Instruction> instructions = Lists.newArrayList();

            // Call the InstructionBuilder Methods Containing Actions
            InstructionUtils.createDropInstructions(ib);
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
        flowBuilder.setTableId(getTable());
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
     * (Table:0) Ingress Tunnel Traffic
     * Match: OpenFlow InPort and Tunnel ID
     * Action: GOTO Local Table (10)
     * table=0,tun_id=0x5,in_port=10, actions=goto_table:2
     */

    @Override
    public void programTunnelIn(Long dpidLong, String segmentationId,
            Long ofPort, boolean write) {

        String nodeName = OPENFLOW + dpidLong;

        BigInteger tunnelId = new BigInteger(segmentationId);
        MatchBuilder matchBuilder = new MatchBuilder();
        NodeBuilder nodeBuilder = createNodeBuilder(nodeName);
        FlowBuilder flowBuilder = new FlowBuilder();

        // Create Match(es) and Set them in the FlowBuilder Object
        flowBuilder.setMatch(MatchUtils.createTunnelIDMatch(matchBuilder, tunnelId).build());
        flowBuilder.setMatch(MatchUtils.createInPortMatch(matchBuilder, dpidLong, ofPort).build());

        if (write) {
            // Create the OF Actions and Instructions
            InstructionBuilder ib = new InstructionBuilder();
            InstructionsBuilder isb = new InstructionsBuilder();

            // Instructions List Stores Individual Instructions
            List<Instruction> instructions = Lists.newArrayList();

            // Call the InstructionBuilder Methods Containing Actions
            ib = this.getMutablePipelineInstructionBuilder();
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
        flowBuilder.setTableId(getTable());
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

    @Override
    public void programVlanIn(Long dpidLong, String segmentationId,  Long ethPort, boolean write) {

        String nodeName = OPENFLOW + dpidLong;

        MatchBuilder matchBuilder = new MatchBuilder();
        NodeBuilder nodeBuilder = createNodeBuilder(nodeName);
        FlowBuilder flowBuilder = new FlowBuilder();

        // Create Match(es) and Set them in the FlowBuilder Object
        flowBuilder.setMatch(
                MatchUtils.createVlanIdMatch(matchBuilder, new VlanId(Integer.valueOf(segmentationId)))
                .build())
                .setMatch(MatchUtils.createInPortMatch(matchBuilder, dpidLong, ethPort)
                        .build());

        if (write) {
            // Create the OF Actions and Instructions
            InstructionBuilder ib = new InstructionBuilder();
            InstructionsBuilder isb = new InstructionsBuilder();

            // Instructions List Stores Individual Instructions
            List<Instruction> instructions = Lists.newArrayList();

            // Call the InstructionBuilder Methods Containing Actions
            ib = this.getMutablePipelineInstructionBuilder();
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
        flowBuilder.setTableId(getTable());
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
     * Create an LLDP Flow Rule to encapsulate into
     * a packet_in that is sent to the controller
     * for topology handling.
     * Match: Ethertype 0x88CCL
     * Action: Punt to Controller in a Packet_In msg
     */

    @Override
    public void programLLDPPuntRule(Long dpidLong) {

        String nodeName = OPENFLOW + dpidLong;
        EtherType etherType = new EtherType(0x88CCL);

        MatchBuilder matchBuilder = new MatchBuilder();
        NodeBuilder nodeBuilder = createNodeBuilder(nodeName);
        FlowBuilder flowBuilder = new FlowBuilder();

        // Create Match(es) and Set them in the FlowBuilder Object
        flowBuilder.setMatch(MatchUtils.createEtherTypeMatch(matchBuilder, etherType).build());

        // Create the OF Actions and Instructions
        InstructionBuilder ib = new InstructionBuilder();
        InstructionsBuilder isb = new InstructionsBuilder();

        // Instructions List Stores Individual Instructions
        List<Instruction> instructions = Lists.newArrayList();

        // Call the InstructionBuilder Methods Containing Actions
        InstructionUtils.createSendToControllerInstructions(ib);
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
        flowBuilder.setTableId(getTable());
        flowBuilder.setKey(key);
        flowBuilder.setFlowName(flowId);
        flowBuilder.setHardTimeout(0);
        flowBuilder.setIdleTimeout(0);
        writeFlow(flowBuilder, nodeBuilder);
    }
}