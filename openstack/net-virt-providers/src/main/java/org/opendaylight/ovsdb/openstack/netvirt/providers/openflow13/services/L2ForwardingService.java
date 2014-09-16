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

import org.opendaylight.ovsdb.openstack.netvirt.api.L2ForwardingProvider;
import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.AbstractServiceInstance;
import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.Service;
import org.opendaylight.ovsdb.utils.mdsal.openflow.InstructionUtils;
import org.opendaylight.ovsdb.utils.mdsal.openflow.MatchUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.OutputActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.OutputActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.PopVlanActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.output.action._case.OutputActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.VlanId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

public class L2ForwardingService extends AbstractServiceInstance implements L2ForwardingProvider {
    private static final Logger logger = LoggerFactory.getLogger(L2ForwardingService.class);
    public L2ForwardingService() {
        super(Service.L2_FORWARDING);
    }

    public L2ForwardingService(Service service) {
        super(service);
    }

    @Override
    public boolean isBridgeInPipeline (String nodeId) {
        return true;
    }

    /*
     * (Table:L2Forwarding) Local Broadcast Flood
     * Match: Tunnel ID and dMAC
     * Action: Output Port
     * table=2,tun_id=0x5,dl_dst=00:00:00:00:00:01 actions=output:2 goto:<next-table>
     */
    @Override
    public void programLocalUcastOut(Long dpidLong, String segmentationId,
            Long localPort, String attachedMac, boolean write) {

        String nodeName = OPENFLOW + dpidLong;

        MatchBuilder matchBuilder = new MatchBuilder();
        NodeBuilder nodeBuilder = createNodeBuilder(nodeName);
        FlowBuilder flowBuilder = new FlowBuilder();

        // Create the OF Match using MatchBuilder
        flowBuilder.setMatch(MatchUtils.createTunnelIDMatch(matchBuilder, new BigInteger(segmentationId)).build());
        flowBuilder.setMatch(MatchUtils.createDestEthMatch(matchBuilder, new MacAddress(attachedMac), null).build());

        String flowId = "UcastOut_"+segmentationId+"_"+localPort+"_"+attachedMac;
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

            // Set the Output Port/Iface
            InstructionUtils.createOutputPortInstructions(ib, dpidLong, localPort);
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

    @Override
    public void programLocalVlanUcastOut (Long dpidLong, String segmentationId, Long localPort, String attachedMac, boolean write) {

        String nodeName = OPENFLOW + dpidLong;

        MatchBuilder matchBuilder = new MatchBuilder();
        NodeBuilder nodeBuilder = createNodeBuilder(nodeName);
        FlowBuilder flowBuilder = new FlowBuilder();

        // Create the OF Match using MatchBuilder
        flowBuilder.setMatch(
                MatchUtils.createVlanIdMatch(matchBuilder, new VlanId(Integer.valueOf(segmentationId))).build());
        flowBuilder.setMatch(MatchUtils.createDestEthMatch(matchBuilder, new MacAddress(attachedMac), null).build());

        String flowId = "VlanUcastOut_"+segmentationId+"_"+localPort+"_"+attachedMac;
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
            //List<Instruction> instructions_tmp = Lists.newArrayList();

            /* Strip vlan and store to tmp instruction space*/
            InstructionUtils.createPopVlanInstructions(ib);
            ib.setOrder(0);
            ib.setKey(new InstructionKey(0));
            instructions.add(ib.build());

            // Set the Output Port/Iface
            //ib = new InstructionBuilder();
            //InstructionUtils.addOutputPortInstructions(ib, dpidLong, localPort, instructions_tmp);
            createOutputPortInstructions(ib, dpidLong, localPort, instructions);
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
     * (Table:2) Local Broadcast Flood
     * Match: Tunnel ID and dMAC (::::FF:FF)
     * table=2,priority=16384,tun_id=0x5,dl_dst=ff:ff:ff:ff:ff:ff \
     * actions=output:2,3,4,5
     */

    @Override
    public void programLocalBcastOut(Long dpidLong, String segmentationId, Long localPort, boolean write) {

        String nodeName = OPENFLOW + dpidLong;

        MatchBuilder matchBuilder = new MatchBuilder();
        NodeBuilder nodeBuilder = createNodeBuilder(nodeName);
        FlowBuilder flowBuilder = new FlowBuilder();

        // Create the OF Match using MatchBuilder
        flowBuilder.setMatch(MatchUtils.createTunnelIDMatch(matchBuilder, new BigInteger(segmentationId)).build());
        flowBuilder.setMatch(MatchUtils.createDestEthMatch(matchBuilder, new MacAddress("01:00:00:00:00:00"),
                new MacAddress("01:00:00:00:00:00")).build());

        String flowId = "BcastOut_"+segmentationId;
        // Add Flow Attributes
        flowBuilder.setId(new FlowId(flowId));
        FlowKey key = new FlowKey(new FlowId(flowId));
        flowBuilder.setStrict(true);
        flowBuilder.setBarrier(false);
        flowBuilder.setTableId(getTable());
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
            boolean flowRemove = InstructionUtils.removeOutputPortFromInstructions(ib, dpidLong, localPort,
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

    @Override
    public void programLocalVlanBcastOut(Long dpidLong,
            String segmentationId, Long localPort,
            boolean write) {

        String nodeName = OPENFLOW + dpidLong;

        MatchBuilder matchBuilder = new MatchBuilder();
        NodeBuilder nodeBuilder = createNodeBuilder(nodeName);
        FlowBuilder flowBuilder = new FlowBuilder();

        // Create the OF Match using MatchBuilder
        flowBuilder.setMatch(
                MatchUtils.createVlanIdMatch(matchBuilder, new VlanId(Integer.valueOf(segmentationId))).build());
        flowBuilder.setMatch(MatchUtils.createDestEthMatch(matchBuilder, new MacAddress("01:00:00:00:00:00"),
                new MacAddress("01:00:00:00:00:00")).build());

        String flowId = "VlanBcastOut_"+segmentationId;
        // Add Flow Attributes
        flowBuilder.setId(new FlowId(flowId));
        FlowKey key = new FlowKey(new FlowId(flowId));
        flowBuilder.setStrict(true);
        flowBuilder.setBarrier(false);
        flowBuilder.setTableId(getTable());
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
                InstructionUtils.createPopVlanInstructions(ib);
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
            boolean flowRemove = InstructionUtils.removeOutputPortFromInstructions(ib, dpidLong,
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

    @Override
    public void programLocalTableMiss(Long dpidLong, String segmentationId, boolean write) {

        String nodeName = OPENFLOW + dpidLong;

        MatchBuilder matchBuilder = new MatchBuilder();
        NodeBuilder nodeBuilder = createNodeBuilder(nodeName);
        FlowBuilder flowBuilder = new FlowBuilder();

        // Create Match(es) and Set them in the FlowBuilder Object
        flowBuilder.setMatch(MatchUtils.createTunnelIDMatch(matchBuilder, new BigInteger(segmentationId)).build());

        if (write) {
            // Create the OF Actions and Instructions
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

        String flowId = "LocalTableMiss_"+segmentationId;
        // Add Flow Attributes
        flowBuilder.setId(new FlowId(flowId));
        FlowKey key = new FlowKey(new FlowId(flowId));
        flowBuilder.setStrict(true);
        flowBuilder.setBarrier(false);
        flowBuilder.setTableId(getTable());
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

    @Override
    public void programLocalVlanTableMiss(Long dpidLong, String segmentationId, boolean write) {

        String nodeName = OPENFLOW + dpidLong;

        MatchBuilder matchBuilder = new MatchBuilder();
        NodeBuilder nodeBuilder = createNodeBuilder(nodeName);
        FlowBuilder flowBuilder = new FlowBuilder();

        // Create Match(es) and Set them in the FlowBuilder Object
        flowBuilder.setMatch(
                MatchUtils.createVlanIdMatch(matchBuilder, new VlanId(Integer.valueOf(segmentationId))).build());

        if (write) {
            // Create the OF Actions and Instructions
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

        String flowId = "LocalTableMiss_"+segmentationId;
        // Add Flow Attributes
        flowBuilder.setId(new FlowId(flowId));
        FlowKey key = new FlowKey(new FlowId(flowId));
        flowBuilder.setStrict(true);
        flowBuilder.setBarrier(false);
        flowBuilder.setTableId(getTable());
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
     * (Table:1) Egress Tunnel Traffic
     * Match: Destination Ethernet Addr and Local InPort
     * Instruction: Set TunnelID and GOTO Table Tunnel Table (n)
     * table=1,tun_id=0x5,dl_dst=00:00:00:00:00:08 \
     * actions=output:10,goto_table:2"
     */
    // TODO : Check on the reason why the original handleTunnelOut was chaining the traffic to table 2
    @Override
    public void programTunnelOut(Long dpidLong, String segmentationId, Long OFPortOut, String attachedMac, boolean write) {

        String nodeName = OPENFLOW + dpidLong;

        MatchBuilder matchBuilder = new MatchBuilder();
        NodeBuilder nodeBuilder = createNodeBuilder(nodeName);
        FlowBuilder flowBuilder = new FlowBuilder();

        // Create the OF Match using MatchBuilder
        flowBuilder.setMatch(MatchUtils.createTunnelIDMatch(matchBuilder, new BigInteger(segmentationId)).build());
        flowBuilder.setMatch(MatchUtils.createDestEthMatch(matchBuilder, new MacAddress(attachedMac), null).build());

        String flowId = "TunnelOut_"+segmentationId+"_"+OFPortOut+"_"+attachedMac;
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

            // GOTO Instructions
            ib = this.getMutablePipelineInstructionBuilder();
            ib.setOrder(0);
            ib.setKey(new InstructionKey(0));
            instructions.add(ib.build());
            // Set the Output Port/Iface
            InstructionUtils.createOutputPortInstructions(ib, dpidLong, OFPortOut);
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
    // TODO : Check on the reason why the original handleTunnelOut was chaining the traffic to table 2
    @Override
    public void programVlanOut(Long dpidLong, String segmentationId, Long ethPort, String attachedMac, boolean write) {

        String nodeName = OPENFLOW + dpidLong;

        MatchBuilder matchBuilder = new MatchBuilder();
        NodeBuilder nodeBuilder = createNodeBuilder(nodeName);
        FlowBuilder flowBuilder = new FlowBuilder();

        // Create the OF Match using MatchBuilder
        flowBuilder.setMatch(
                MatchUtils.createVlanIdMatch(matchBuilder, new VlanId(Integer.valueOf(segmentationId))).build());
        flowBuilder.setMatch(MatchUtils.createDestEthMatch(matchBuilder, new MacAddress(attachedMac), null).build());

        String flowId = "VlanOut_"+segmentationId+"_"+ethPort+"_"+attachedMac;
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

            // GOTO Instructions
            ib = this.getMutablePipelineInstructionBuilder();
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
    @Override
    public void programTunnelFloodOut(Long dpidLong, String segmentationId, Long OFPortOut, boolean write) {

        String nodeName = OPENFLOW + dpidLong;

        MatchBuilder matchBuilder = new MatchBuilder();
        NodeBuilder nodeBuilder = createNodeBuilder(nodeName);
        FlowBuilder flowBuilder = new FlowBuilder();

        // Create the OF Match using MatchBuilder
        // Match TunnelID
        flowBuilder.setMatch(MatchUtils.createTunnelIDMatch(matchBuilder, new BigInteger(segmentationId)).build());
        // Match DMAC

        flowBuilder.setMatch(MatchUtils.createDestEthMatch(matchBuilder, new MacAddress("01:00:00:00:00:00"),
                new MacAddress("01:00:00:00:00:00")).build());

        String flowId = "TunnelFloodOut_"+segmentationId;
        // Add Flow Attributes
        flowBuilder.setId(new FlowId(flowId));
        FlowKey key = new FlowKey(new FlowId(flowId));
        flowBuilder.setBarrier(true);
        flowBuilder.setTableId(getTable());
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
            ib = this.getMutablePipelineInstructionBuilder();
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
            boolean flowRemove = InstructionUtils.removeOutputPortFromInstructions(ib, dpidLong,
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

    @Override
    public void programVlanFloodOut(Long dpidLong, String segmentationId, Long ethPort, boolean write) {

        String nodeName = OPENFLOW + dpidLong;

        MatchBuilder matchBuilder = new MatchBuilder();
        NodeBuilder nodeBuilder = createNodeBuilder(nodeName);
        FlowBuilder flowBuilder = new FlowBuilder();

        // Create the OF Match using MatchBuilder
        // Match Vlan ID
        flowBuilder.setMatch(
                MatchUtils.createVlanIdMatch(matchBuilder, new VlanId(Integer.valueOf(segmentationId))).build());
        // Match DMAC
        flowBuilder.setMatch(MatchUtils.createDestEthMatch(matchBuilder, new MacAddress("01:00:00:00:00:00"),
                new MacAddress("01:00:00:00:00:00")).build());

        String flowId = "VlanFloodOut_"+segmentationId;
        // Add Flow Attributes
        flowBuilder.setId(new FlowId(flowId));
        FlowKey key = new FlowKey(new FlowId(flowId));
        flowBuilder.setBarrier(true);
        flowBuilder.setTableId(getTable());
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
            ib = this.getMutablePipelineInstructionBuilder();
            ib.setOrder(0);
            ib.setKey(new InstructionKey(0));
            instructions.add(ib.build());
            // Set the Output Port/Iface
            InstructionUtils.createOutputPortInstructions(ib, dpidLong, ethPort);
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
    @Override
    public void programTunnelMiss(Long dpidLong, String segmentationId, boolean write) {

        String nodeName = OPENFLOW + dpidLong;

        MatchBuilder matchBuilder = new MatchBuilder();
        NodeBuilder nodeBuilder = createNodeBuilder(nodeName);
        FlowBuilder flowBuilder = new FlowBuilder();

        // Create Match(es) and Set them in the FlowBuilder Object
        flowBuilder.setMatch(MatchUtils.createTunnelIDMatch(matchBuilder, new BigInteger(segmentationId)).build());

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

        String flowId = "TunnelMiss_"+segmentationId;
        // Add Flow Attributes
        flowBuilder.setId(new FlowId(flowId));
        FlowKey key = new FlowKey(new FlowId(flowId));
        flowBuilder.setStrict(true);
        flowBuilder.setBarrier(false);
        flowBuilder.setTableId(getTable());
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

    @Override
    public void programVlanMiss(Long dpidLong, String segmentationId, Long ethPort, boolean write) {

        String nodeName = OPENFLOW + dpidLong;

        MatchBuilder matchBuilder = new MatchBuilder();
        NodeBuilder nodeBuilder = createNodeBuilder(nodeName);
        FlowBuilder flowBuilder = new FlowBuilder();

        // Create Match(es) and Set them in the FlowBuilder Object
        flowBuilder.setMatch(
                MatchUtils.createVlanIdMatch(matchBuilder, new VlanId(Integer.valueOf(segmentationId))).build());

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

            // Set the Output Port/Iface
            InstructionUtils.createOutputPortInstructions(ib, dpidLong, ethPort);
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
        flowBuilder.setTableId(getTable());
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
}