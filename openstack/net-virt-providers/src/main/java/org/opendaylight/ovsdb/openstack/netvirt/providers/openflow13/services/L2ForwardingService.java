/*
 * Copyright (c) 2014, 2015 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.services;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.opendaylight.ovsdb.openstack.netvirt.api.L2ForwardingProvider;
import org.opendaylight.ovsdb.openstack.netvirt.providers.ConfigInterface;
import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.AbstractServiceInstance;
import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.Service;
import org.opendaylight.ovsdb.utils.mdsal.openflow.ActionUtils;
import org.opendaylight.ovsdb.utils.mdsal.openflow.FlowUtils;
import org.opendaylight.ovsdb.utils.mdsal.openflow.InstructionUtils;
import org.opendaylight.ovsdb.utils.mdsal.openflow.MatchUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.OutputActionCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.OutputActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.output.action._case.OutputActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.Flow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.InstructionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.ApplyActionsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.ApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.apply.actions._case.ApplyActions;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.apply.actions._case.ApplyActionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.VlanId;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class L2ForwardingService extends AbstractServiceInstance implements ConfigInterface, L2ForwardingProvider {
    private static final Logger LOG = LoggerFactory.getLogger(L2ForwardingService.class);
    public L2ForwardingService() {
        super(Service.L2_FORWARDING);
    }

    public L2ForwardingService(Service service) {
        super(service);
    }

    /*
     * (Table:L2Forwarding) Local Unicast
     * Match: Tunnel ID and dMAC
     * Action: Output Port
     * table=2,tun_id=0x5,dl_dst=00:00:00:00:00:01 actions=output:2 goto:<next-table>
     */
    @Override
    public void programLocalUcastOut(Long dpidLong, String segmentationId,
            Long localPort, String attachedMac, boolean write) {

        String nodeName = OPENFLOW + dpidLong;

        NodeBuilder nodeBuilder = createNodeBuilder(nodeName);
        FlowBuilder flowBuilder = new FlowBuilder();

        // Create the OF Match using MatchBuilder
        MatchBuilder matchBuilder = new MatchBuilder();
        MatchUtils.createTunnelIDMatch(matchBuilder, new BigInteger(segmentationId));
        MatchUtils.createDestEthMatch(matchBuilder, new MacAddress(attachedMac), null);
        flowBuilder.setMatch(matchBuilder.build());

        // Add Flow Attributes
        String flowName = "UcastOut_" + segmentationId + "_" + localPort + "_" + attachedMac;
        FlowUtils.initFlowBuilder(flowBuilder, flowName, getTable());

        if (write) {
            // Set the Output Port/Iface
            Instruction setOutputPortInstruction =
                    InstructionUtils.createOutputPortInstructions(new InstructionBuilder(), dpidLong, localPort)
                            .setOrder(0)
                            .setKey(new InstructionKey(0))
                            .build();

            // Add InstructionsBuilder to FlowBuilder
            InstructionUtils.setFlowBuilderInstruction(flowBuilder, setOutputPortInstruction);
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
     * table=110,dl_vlan=2001,dl_dst=fa:16:3e:a3:3b:cc actions=pop_vlan,output:1
     */

    @Override
    public void programLocalVlanUcastOut (Long dpidLong, String segmentationId, Long localPort, String attachedMac, boolean write) {

        String nodeName = OPENFLOW + dpidLong;

        NodeBuilder nodeBuilder = createNodeBuilder(nodeName);
        FlowBuilder flowBuilder = new FlowBuilder();

        // Create the OF Match using MatchBuilder
        MatchBuilder matchBuilder = new MatchBuilder();
        MatchUtils.createVlanIdMatch(matchBuilder, new VlanId(Integer.valueOf(segmentationId)), true);
        MatchUtils.createDestEthMatch(matchBuilder, new MacAddress(attachedMac), null);
        flowBuilder.setMatch(matchBuilder.build());

        // Add Flow Attributes
        String flowName = "VlanUcastOut_" + segmentationId + "_" + localPort + "_" + attachedMac;
        FlowUtils.initFlowBuilder(flowBuilder, flowName, getTable());

        if (write) {
            /* Strip vlan and store to tmp instruction space*/
            Instruction stripVlanInstruction = InstructionUtils.createPopVlanInstructions(new InstructionBuilder())
                    .setOrder(0)
                    .setKey(new InstructionKey(0))
                    .build();

            // Set the Output Port/Iface
            Instruction setOutputPortInstruction =
                    InstructionUtils.addOutputPortInstructions(new InstructionBuilder(), dpidLong, localPort,
                            Collections.singletonList(stripVlanInstruction))
                            .setOrder(1)
                            .setKey(new InstructionKey(0))
                            .build();

            // Add InstructionsBuilder to FlowBuilder
            InstructionUtils.setFlowBuilderInstruction(flowBuilder, setOutputPortInstruction);
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

        NodeBuilder nodeBuilder = createNodeBuilder(nodeName);
        FlowBuilder flowBuilder = new FlowBuilder();

        // Create the OF Match using MatchBuilder
        MatchBuilder matchBuilder = new MatchBuilder();
        MatchUtils.addNxRegMatch(matchBuilder,
                new MatchUtils.RegMatch(ClassifierService.REG_FIELD, ClassifierService.REG_VALUE_FROM_REMOTE));
        MatchUtils.createTunnelIDMatch(matchBuilder, new BigInteger(segmentationId));
        MatchUtils.createDestEthMatch(matchBuilder, new MacAddress("01:00:00:00:00:00"),
                new MacAddress("01:00:00:00:00:00"));
        flowBuilder.setMatch(matchBuilder.build());

        // Add Flow Attributes
        String flowName = "BcastOut_" + segmentationId;
        FlowUtils.initFlowBuilder(flowBuilder, flowName, getTable())
                .setPriority(16384);

        Flow flow = this.getFlow(flowBuilder, nodeBuilder);

        // Retrieve the existing instructions
        List<Instruction> existingInstructions = InstructionUtils.extractExistingInstructions(flow);

        if (write) {
            // Create output port list
            Instruction outputPortInstruction =
                    createOutputPortInstructions(new InstructionBuilder(), dpidLong, localPort, existingInstructions)
                            .setOrder(0)
                            .setKey(new InstructionKey(0))
                            .build();

            /* Alternative method to address Bug 2004 is to use appendResubmitLocalFlood(ib) so that we send the
             * flow back to the local flood rule. (See git history.) */

            // Add InstructionsBuilder to FlowBuilder
            InstructionUtils.setFlowBuilderInstruction(flowBuilder, outputPortInstruction);

            writeFlow(flowBuilder, nodeBuilder);
        } else {
            InstructionBuilder ib = new InstructionBuilder();
            boolean flowRemove = InstructionUtils.removeOutputPortFromInstructions(ib, dpidLong, localPort,
                    existingInstructions);
            if (flowRemove) {
                /* if all ports are removed, remove flow */
                removeFlow(flowBuilder, nodeBuilder);
            } else {
                /* Install instruction with new output port list*/
                Instruction outputPortInstruction = ib
                        .setOrder(0)
                        .setKey(new InstructionKey(0))
                        .build();

                // Add InstructionsBuilder to FlowBuilder
                InstructionUtils.setFlowBuilderInstruction(flowBuilder, outputPortInstruction);

                writeFlow(flowBuilder, nodeBuilder);
            }
        }
    }

    /*
     * (Table:2) Local VLAN Broadcast Flood
     * Match: vlan ID and dMAC (::::FF:FF)
     * table=2,priority=16384,vlan_id=0x5,dl_dst=ff:ff:ff:ff:ff:ff \
     * actions=strip_vlan, output:2,3,4,5
     * table=110,dl_vlan=2001,dl_dst=01:00:00:00:00:00/01:00:00:00:00:00 actions=output:2,pop_vlan,output:1,output:3,output:4
     */

    @Override
    public void programLocalVlanBcastOut(Long dpidLong, String segmentationId,
                                         Long localPort, Long ethPort, boolean write) {

        String nodeName = OPENFLOW + dpidLong;

        NodeBuilder nodeBuilder = createNodeBuilder(nodeName);
        FlowBuilder flowBuilder = new FlowBuilder();

        // Create the OF Match using MatchBuilder
        MatchBuilder matchBuilder = new MatchBuilder();
        MatchUtils.createVlanIdMatch(matchBuilder, new VlanId(Integer.valueOf(segmentationId)), true);
        MatchUtils.createDestEthMatch(matchBuilder, new MacAddress("01:00:00:00:00:00"),
                new MacAddress("01:00:00:00:00:00"));
        flowBuilder.setMatch(matchBuilder.build());

        // Add Flow Attributes
        String flowName = "VlanBcastOut_" + segmentationId + "_" + ethPort;
        FlowUtils.initFlowBuilder(flowBuilder, flowName, getTable())
                .setPriority(16384);
        Flow flow = this.getFlow(flowBuilder, nodeBuilder);

        // Retrieve the existing instructions
        List<Instruction> existingInstructions = InstructionUtils.extractExistingInstructions(flow);

        if (write) {
            List<Action> actionList;
            if (existingInstructions == null || existingInstructions.isEmpty()) {
                /* First time called there should be no instructions.
                 * We can simply add the output:ethPort first, followed by
                 * popVlan and then the local port. The next calls will append
                 * the rest of the local ports.
                 */
                actionList = new ArrayList<>();

                actionList.add(
                        new ActionBuilder()
                                .setAction(ActionUtils.outputAction(new NodeConnectorId(nodeName + ":" + ethPort)))
                                .setOrder(0)
                                .setKey(new ActionKey(0))
                                .build());

                actionList.add(
                        new ActionBuilder()
                                .setAction(ActionUtils.popVlanAction())
                                .setOrder(1)
                                .setKey(new ActionKey(1))
                                .build());

                actionList.add(
                        new ActionBuilder()
                                .setAction(ActionUtils.outputAction(new NodeConnectorId(nodeName + ":" + localPort)))
                                .setOrder(2)
                                .setKey(new ActionKey(2))
                                .build());
            } else {
                /* Subsequent calls require appending any new local ports for this tenant. */
                Instruction in = existingInstructions.get(0);
                actionList = (((ApplyActionsCase) in.getInstruction()).getApplyActions().getAction());

                NodeConnectorId ncid = new NodeConnectorId(nodeName + ":" + localPort);
                final Uri nodeConnectorUri = new Uri(ncid);
                boolean addNew = true;

                /* Check if the port is already in the output list */
                for (Action action : actionList) {
                    if (action.getAction() instanceof OutputActionCase) {
                        OutputActionCase opAction = (OutputActionCase) action.getAction();
                        if (opAction.getOutputAction().getOutputNodeConnector().equals(nodeConnectorUri)) {
                            addNew = false;
                            break;
                        }
                    }
                }

                if (addNew) {
                    actionList.add(
                            new ActionBuilder()
                                    .setAction(
                                            ActionUtils.outputAction(new NodeConnectorId(nodeName + ":" + localPort)))
                                    .setOrder(actionList.size())
                                    .setKey(new ActionKey(actionList.size()))
                                    .build());
                }
            }

            ApplyActions applyActions = new ApplyActionsBuilder().setAction(actionList).build();
            Instruction applyActionsInstruction =
                    new InstructionBuilder()
                            .setInstruction(new ApplyActionsCaseBuilder().setApplyActions(applyActions).build())
                            .setOrder(0)
                            .setKey(new InstructionKey(0))
                            .build();

            // Add InstructionsBuilder to FlowBuilder
            InstructionUtils.setFlowBuilderInstruction(flowBuilder, applyActionsInstruction);
            writeFlow(flowBuilder, nodeBuilder);
        } else {
            InstructionBuilder ib = new InstructionBuilder();
            boolean flowRemove = removeOutputPortFromInstructions(ib, dpidLong, localPort, ethPort,
                    existingInstructions);
            if (flowRemove) {
                /* if all ports are removed, remove flow */
                removeFlow(flowBuilder, nodeBuilder);
            } else {
                /* Install instruction with new output port list*/
                Instruction outputPortInstruction = ib
                        .setOrder(0)
                        .setKey(new InstructionKey(0))
                        .build();

                // Add InstructionsBuilder to FlowBuilder
                InstructionUtils.setFlowBuilderInstruction(flowBuilder, outputPortInstruction);
                writeFlow(flowBuilder, nodeBuilder);
            }
        }
    }

    private boolean removeOutputPortFromInstructions(InstructionBuilder ib, Long dpidLong, Long localPort,
                                                     Long ethPort, List<Instruction> instructions) {
        List<Action> actionList = new ArrayList<>();
        boolean removeFlow = true;

        if (instructions != null && !instructions.isEmpty()) {
            Instruction in = instructions.get(0);
            List<Action> oldActionList = (((ApplyActionsCase) in.getInstruction()).getApplyActions().getAction());
            NodeConnectorId ncid = new NodeConnectorId(OPENFLOW + dpidLong + ":" + localPort);
            final Uri localNodeConnectorUri = new Uri(ncid);
            NodeConnectorId ncidEth = new NodeConnectorId(OPENFLOW + dpidLong + ":" + ethPort);
            final Uri ethNodeConnectorUri = new Uri(ncidEth);

            // Remove the port from the output list
            int index = 2;
            for (Action action : oldActionList) {
                if (action.getAction() instanceof OutputActionCase) {
                    OutputActionCase opAction = (OutputActionCase) action.getAction();
                    if (opAction.getOutputAction().getOutputNodeConnector().equals(ethNodeConnectorUri)) {
                        actionList.add(action);
                    } else if (!opAction.getOutputAction().getOutputNodeConnector().equals(localNodeConnectorUri)) {
                        actionList.add(
                                new ActionBuilder()
                                        .setAction(action.getAction())
                                        .setOrder(index)
                                        .setKey(new ActionKey(index))
                                        .build());
                        index++;
                    }
                } else {
                    actionList.add(action);
                }
            }
            ApplyActions applyActions = new ApplyActionsBuilder().setAction(actionList).build();
            ib.setInstruction(new ApplyActionsCaseBuilder().setApplyActions(applyActions).build());
        }

        if (actionList.size() > 2) {
            // Add InstructionBuilder to the Instruction(s)Builder List
            // TODO This doesn't actually do anything
            InstructionsBuilder isb = new InstructionsBuilder();
            isb.setInstruction(instructions);
            removeFlow = false;
        }

        return removeFlow;
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

        NodeBuilder nodeBuilder = createNodeBuilder(nodeName);
        FlowBuilder flowBuilder = new FlowBuilder();

        // Create Match(es) and Set them in the FlowBuilder Object
        flowBuilder.setMatch(
                MatchUtils.createTunnelIDMatch(new MatchBuilder(), new BigInteger(segmentationId)).build());

        if (write) {
            // Call the InstructionBuilder Methods Containing Actions
            Instruction dropInstruction = InstructionUtils.createDropInstructions(new InstructionBuilder())
                    .setOrder(0)
                    .setKey(new InstructionKey(0))
                    .build();

            // Add InstructionsBuilder to FlowBuilder
            InstructionUtils.setFlowBuilderInstruction(flowBuilder, dropInstruction);
        }

        // Add Flow Attributes
        String flowName = "LocalTableMiss_" + segmentationId;
        FlowUtils.initFlowBuilder(flowBuilder, flowName, getTable())
                .setPriority(8192);
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

        NodeBuilder nodeBuilder = createNodeBuilder(nodeName);
        FlowBuilder flowBuilder = new FlowBuilder();

        // Create Match(es) and Set them in the FlowBuilder Object
        flowBuilder.setMatch(
                MatchUtils.createVlanIdMatch(new MatchBuilder(), new VlanId(Integer.valueOf(segmentationId)),
                        true).build());

        if (write) {
            // Call the InstructionBuilder Methods Containing Actions
            Instruction dropInstruction = InstructionUtils.createDropInstructions(new InstructionBuilder())
                    .setOrder(0)
                    .setKey(new InstructionKey(0))
                    .build();

            // Add InstructionsBuilder to FlowBuilder
            InstructionUtils.setFlowBuilderInstruction(flowBuilder, dropInstruction);
        }

        // Add Flow Attributes
        String flowName = "LocalTableMiss_" + segmentationId;
        FlowUtils.initFlowBuilder(flowBuilder, flowName, getTable())
                .setPriority(8192);
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

        NodeBuilder nodeBuilder = createNodeBuilder(nodeName);
        FlowBuilder flowBuilder = new FlowBuilder();

        // Create the OF Match using MatchBuilder
        MatchBuilder matchBuilder = new MatchBuilder();
        MatchUtils.createTunnelIDMatch(matchBuilder, new BigInteger(segmentationId));
        MatchUtils.createDestEthMatch(matchBuilder, new MacAddress(attachedMac), null);
        flowBuilder.setMatch(matchBuilder.build());

        // Add Flow Attributes
        String flowName = "TunnelOut_" + segmentationId + "_" + OFPortOut + "_" + attachedMac;
        FlowUtils.initFlowBuilder(flowBuilder, flowName, getTable());

        if (write) {
            // Set the Output Port/Iface
            Instruction outputPortInstruction =
                    InstructionUtils.createOutputPortInstructions(new InstructionBuilder(), dpidLong, OFPortOut)
                            .setOrder(0)
                            .setKey(new InstructionKey(1))
                            .build();

            // Add InstructionsBuilder to FlowBuilder
            InstructionUtils.setFlowBuilderInstruction(flowBuilder, outputPortInstruction);

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

        NodeBuilder nodeBuilder = createNodeBuilder(nodeName);
        FlowBuilder flowBuilder = new FlowBuilder();

        // Create the OF Match using MatchBuilder
        MatchBuilder matchBuilder = new MatchBuilder();
        MatchUtils.createVlanIdMatch(matchBuilder, new VlanId(Integer.valueOf(segmentationId)), true);
        MatchUtils.createDestEthMatch(matchBuilder, new MacAddress(attachedMac), null);
        flowBuilder.setMatch(matchBuilder.build());

        // Add Flow Attributes
        String flowName = "VlanOut_" + segmentationId + "_" + ethPort + "_" + attachedMac;
        FlowUtils.initFlowBuilder(flowBuilder, flowName, getTable());

        if (write) {
            // Instructions List Stores Individual Instructions
            Instruction outputPortInstruction =
                    InstructionUtils.createOutputPortInstructions(new InstructionBuilder(), dpidLong, ethPort)
                            .setOrder(0)
                            .setKey(new InstructionKey(1))
                            .build();

            // Add InstructionsBuilder to FlowBuilder
            InstructionUtils.setFlowBuilderInstruction(flowBuilder, outputPortInstruction);

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

        NodeBuilder nodeBuilder = createNodeBuilder(nodeName);
        FlowBuilder flowBuilder = new FlowBuilder();

        // Create the OF Match using MatchBuilder
        MatchBuilder matchBuilder = new MatchBuilder();
        // Match TunnelID
        MatchUtils.addNxRegMatch(matchBuilder,
                new MatchUtils.RegMatch(ClassifierService.REG_FIELD, ClassifierService.REG_VALUE_FROM_LOCAL));
        MatchUtils.createTunnelIDMatch(matchBuilder, new BigInteger(segmentationId));
        // Match DMAC
        MatchUtils.createDestEthMatch(matchBuilder, new MacAddress("01:00:00:00:00:00"),
                new MacAddress("01:00:00:00:00:00"));
        flowBuilder.setMatch(matchBuilder.build());

        // Add Flow Attributes
        String flowName = "TunnelFloodOut_" + segmentationId;
        final FlowId flowId = new FlowId(flowName);
        flowBuilder
                .setId(flowId)
                .setBarrier(true)
                .setTableId(getTable())
                .setKey(new FlowKey(flowId))
                .setPriority(16383)  // FIXME: change it back to 16384 once bug 3005 is fixed.
                .setFlowName(flowName)
                .setHardTimeout(0)
                .setIdleTimeout(0);

        Flow flow = this.getFlow(flowBuilder, nodeBuilder);
        // Instantiate the Builders for the OF Actions and Instructions
        List<Instruction> existingInstructions = InstructionUtils.extractExistingInstructions(flow);

        if (write) {
            // Set the Output Port/Iface
            Instruction outputPortInstruction =
                    createOutputPortInstructions(new InstructionBuilder(), dpidLong, OFPortOut, existingInstructions)
                            .setOrder(0)
                            .setKey(new InstructionKey(0))
                            .build();

            // Add InstructionsBuilder to FlowBuilder
            InstructionUtils.setFlowBuilderInstruction(flowBuilder, outputPortInstruction);

            writeFlow(flowBuilder, nodeBuilder);
        } else {
            InstructionBuilder ib = new InstructionBuilder();
            /* remove port from action list */
            boolean flowRemove = InstructionUtils.removeOutputPortFromInstructions(ib, dpidLong,
                    OFPortOut, existingInstructions);
            if (flowRemove) {
                /* if all port are removed, remove the flow too. */
                removeFlow(flowBuilder, nodeBuilder);
            } else {
                /* Install instruction with new output port list*/
                Instruction instruction = ib
                        .setOrder(0)
                        .setKey(new InstructionKey(0))
                        .build();

                // Add InstructionsBuilder to FlowBuilder
                InstructionUtils.setFlowBuilderInstruction(flowBuilder, instruction);
                writeFlow(flowBuilder, nodeBuilder);
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

        NodeBuilder nodeBuilder = createNodeBuilder(nodeName);
        FlowBuilder flowBuilder = new FlowBuilder();

        // Create the OF Match using MatchBuilder
        MatchBuilder matchBuilder = new MatchBuilder();
        // Match Vlan ID
        MatchUtils.createVlanIdMatch(matchBuilder, new VlanId(Integer.valueOf(segmentationId)), true);
        // Match DMAC
        MatchUtils.createDestEthMatch(matchBuilder, new MacAddress("01:00:00:00:00:00"),
                new MacAddress("01:00:00:00:00:00"));
        flowBuilder.setMatch(matchBuilder.build());

        // Add Flow Attributes
        String flowName = "VlanFloodOut_" + segmentationId + "_" + ethPort;
        final FlowId flowId = new FlowId(flowName);
        flowBuilder
                .setId(flowId)
                .setBarrier(true)
                .setTableId(getTable())
                .setKey(new FlowKey(flowId))
                .setPriority(16384)
                .setFlowName(flowName)
                .setHardTimeout(0)
                .setIdleTimeout(0);

        //ToDo: Is there something to be done with result of the call to getFlow?
        Flow flow = this.getFlow(flowBuilder, nodeBuilder);

        if (write) {
            // Set the Output Port/Iface
            Instruction outputPortInstruction =
                    InstructionUtils.createOutputPortInstructions(new InstructionBuilder(), dpidLong, ethPort)
                            .setOrder(0)
                            .setKey(new InstructionKey(0))
                            .build();

            // Add InstructionsBuilder to FlowBuilder
            InstructionUtils.setFlowBuilderInstruction(flowBuilder, outputPortInstruction);

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

        NodeBuilder nodeBuilder = createNodeBuilder(nodeName);
        FlowBuilder flowBuilder = new FlowBuilder();

        // Create Match(es) and Set them in the FlowBuilder Object
        flowBuilder.setMatch(
                MatchUtils.createTunnelIDMatch(new MatchBuilder(), new BigInteger(segmentationId)).build());

        if (write) {
            // Call the InstructionBuilder Methods Containing Actions
            Instruction mutablePipelineInstruction = getMutablePipelineInstructionBuilder()
                    .setOrder(0)
                    .setKey(new InstructionKey(0))
                    .build();

            // Add InstructionsBuilder to FlowBuilder
            InstructionUtils.setFlowBuilderInstruction(flowBuilder, mutablePipelineInstruction);
        }

        // Add Flow Attributes
        String flowName = "TunnelMiss_" + segmentationId;
        FlowUtils.initFlowBuilder(flowBuilder, flowName, getTable())
                .setPriority(8192);
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
     * table=110,priority=8192,dl_vlan=2001 actions=output:2
     */

    @Override
    public void programVlanMiss(Long dpidLong, String segmentationId, Long ethPort, boolean write) {

        String nodeName = OPENFLOW + dpidLong;

        NodeBuilder nodeBuilder = createNodeBuilder(nodeName);
        FlowBuilder flowBuilder = new FlowBuilder();

        // Create Match(es) and Set them in the FlowBuilder Object
        flowBuilder.setMatch(
                MatchUtils.createVlanIdMatch(new MatchBuilder(), new VlanId(Integer.valueOf(segmentationId)),
                        true).build());

        if (write) {
            // Set the Output Port/Iface
            Instruction outputPortInstruction =
                    InstructionUtils.createOutputPortInstructions(new InstructionBuilder(), dpidLong, ethPort)
                            .setOrder(0)
                            .setKey(new InstructionKey(1))
                            .build();

            // Add InstructionsBuilder to FlowBuilder
            InstructionUtils.setFlowBuilderInstruction(flowBuilder, outputPortInstruction);
        }

        // Add Flow Attributes
        String flowName = "VlanMiss_" + segmentationId;
        FlowUtils.initFlowBuilder(flowBuilder, flowName, getTable())
                .setPriority(8192);
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
        LOG.debug("createOutputPortInstructions() Node Connector ID is - Type=openflow: DPID={} port={} existingInstructions={}", dpidLong, port, instructions);

        List<Action> actionList = new ArrayList<>();
        ActionBuilder ab = new ActionBuilder();

        List<Action> existingActions;
        if (instructions != null && instructions.size() > 0) {
            /**
             * First instruction is the one containing the output ports.
             * So, only extract the actions from that.
             */
            Instruction in = instructions.get(0);
            if (in.getInstruction() instanceof ApplyActionsCase) {
                existingActions = (((ApplyActionsCase) in.getInstruction()).getApplyActions().getAction());
                // Only include output actions
                for (Action action : existingActions) {
                    if (action.getAction() instanceof OutputActionCase) {
                        actionList.add(action);
                    }
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
            OutputActionCase opAction = (OutputActionCase)action.getAction();
            /* If output port action already in the action list of one of the buckets, skip */
            if (opAction.getOutputAction().getOutputNodeConnector().equals(new Uri(ncid))) {
                addNew = false;
                break;
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
        LOG.debug("createOutputPortInstructions() : applyAction {}", aab.build());
        return ib;
    }

    @Override
    public void setDependencies(BundleContext bundleContext, ServiceReference serviceReference) {
        super.setDependencies(bundleContext.getServiceReference(L2ForwardingProvider.class.getName()), this);
    }

    @Override
    public void setDependencies(Object impl) {

    }
}
