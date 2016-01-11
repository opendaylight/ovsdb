/*
 * Copyright (c) 2014, 2015 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.services;

import java.math.BigInteger;
import java.util.List;

import org.opendaylight.ovsdb.openstack.netvirt.api.ClassifierProvider;
import org.opendaylight.ovsdb.openstack.netvirt.providers.ConfigInterface;
import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.AbstractServiceInstance;
import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.Service;
import org.opendaylight.ovsdb.utils.mdsal.openflow.ActionUtils;
import org.opendaylight.ovsdb.utils.mdsal.openflow.FlowUtils;
import org.opendaylight.ovsdb.utils.mdsal.openflow.InstructionUtils;
import org.opendaylight.ovsdb.utils.mdsal.openflow.MatchUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.InstructionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.ApplyActionsCase;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.ApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.apply.actions._case.ApplyActionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.EtherType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.VlanId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg0;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstNxRegCaseBuilder;

import com.google.common.collect.Lists;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

public class ClassifierService extends AbstractServiceInstance implements ClassifierProvider, ConfigInterface {
    public final static long REG_VALUE_FROM_LOCAL = 0x1L;
    public final static long REG_VALUE_FROM_REMOTE = 0x2L;
    public static final Class<? extends NxmNxReg> REG_FIELD = NxmNxReg0.class;

    public ClassifierService() {
        super(Service.CLASSIFIER);
    }

    public ClassifierService(Service service) {
        super(service);
    }

    /*
     * (Table:Classifier) Egress VM Traffic Towards TEP
     * Match: Destination Ethernet Addr and OpenFlow InPort
     * Instruction: Set TunnelID and GOTO Table Tunnel Table (n)
     * table=0,in_port=2,dl_src=00:00:00:00:00:01 \
     * actions=set_field:5->tun_id,goto_table=<next-table>"
     */
    @Override
    public void programLocalInPort(Long dpidLong, String segmentationId, Long inPort, String attachedMac,
                                   boolean write) {
        NodeBuilder nodeBuilder = FlowUtils.createNodeBuilder(dpidLong);
        FlowBuilder flowBuilder = new FlowBuilder();
        String flowName = "LocalMac_" + segmentationId + "_" + inPort + "_" + attachedMac;
        FlowUtils.initFlowBuilder(flowBuilder, flowName, getTable());

        MatchBuilder matchBuilder = new MatchBuilder();
        MatchUtils.createEthSrcMatch(matchBuilder, new MacAddress(attachedMac));
        MatchUtils.createInPortMatch(matchBuilder, dpidLong, inPort);
        flowBuilder.setMatch(matchBuilder.build());

        if (write) {
            // Instantiate the Builders for the OF Actions and Instructions
            InstructionBuilder ib = new InstructionBuilder();
            InstructionsBuilder isb = new InstructionsBuilder();

            // Instructions List Stores Individual Instructions
            List<Instruction> instructions = Lists.newArrayList();

            InstructionUtils.createSetTunnelIdInstructions(ib, new BigInteger(segmentationId));
            ApplyActionsCase aac = (ApplyActionsCase) ib.getInstruction();
            List<Action> actionList = aac.getApplyActions().getAction();

            ActionBuilder ab = new ActionBuilder();
            ab.setAction(ActionUtils.nxLoadRegAction(new DstNxRegCaseBuilder().setNxReg(REG_FIELD).build(),
                    BigInteger.valueOf(REG_VALUE_FROM_LOCAL)));
            ab.setOrder(1);
            ab.setKey(new ActionKey(1));

            actionList.add(ab.build());

            ib.setOrder(0);
            ib.setKey(new InstructionKey(0));
            instructions.add(ib.build());

            // Next service GOTO Instructions Need to be appended to the List
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
     * (Table:Classifier) Egress VM Traffic Towards TEP
     * Match: Source Ethernet Addr and OpenFlow InPort
     * Instruction: Set VLANID and GOTO Table Egress (n)
     * table=0,in_port=2,dl_src=00:00:00:00:00:01 \
     * actions=push_vlan, set_field:5->vlan_id,goto_table=<Next-Table>"
     * table=0,in_port=1,vlan_tci=0x0000/0x1fff,dl_src=fa:16:3e:70:2f:c2 actions=push_vlan:0x8100,set_field:6097->vlan_vid,goto_table:20
     */
    @Override
    public void programLocalInPortSetVlan(Long dpidLong, String segmentationId, Long inPort, String attachedMac,
                                          boolean write) {
        NodeBuilder nodeBuilder = FlowUtils.createNodeBuilder(dpidLong);
        FlowBuilder flowBuilder = new FlowBuilder();
        String flowName = "LocalMac_" + segmentationId + "_" + inPort + "_" + attachedMac;
        FlowUtils.initFlowBuilder(flowBuilder, flowName, getTable());

        MatchBuilder matchBuilder = new MatchBuilder();
        MatchUtils.createEthSrcMatch(matchBuilder, new MacAddress(attachedMac));
        MatchUtils.createInPortMatch(matchBuilder, dpidLong, inPort);
        // openflowplugin requires a vlan match to add a vlan
        MatchUtils.createVlanIdMatch(matchBuilder, new VlanId(0), false);
        flowBuilder.setMatch(matchBuilder.build());

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

            // Next service GOTO Instructions Need to be appended to the List
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
        NodeBuilder nodeBuilder = FlowUtils.createNodeBuilder(dpidLong);
        FlowBuilder flowBuilder = new FlowBuilder();
        String flowName = "DropFilter_" + inPort;
        FlowUtils.initFlowBuilder(flowBuilder, flowName, getTable()).setPriority(8192);

        MatchBuilder matchBuilder = new MatchBuilder();
        MatchUtils.createInPortMatch(matchBuilder, dpidLong, inPort);
        flowBuilder.setMatch(matchBuilder.build());

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
    public void programTunnelIn(Long dpidLong, String segmentationId, Long ofPort, boolean write) {
        NodeBuilder nodeBuilder = FlowUtils.createNodeBuilder(dpidLong);
        FlowBuilder flowBuilder = new FlowBuilder();
        String flowName = "TunnelIn_" + segmentationId + "_" + ofPort;
        FlowUtils.initFlowBuilder(flowBuilder, flowName, getTable());

        MatchBuilder matchBuilder = new MatchBuilder();
        MatchUtils.createTunnelIDMatch(matchBuilder, new BigInteger(segmentationId));
        MatchUtils.createInPortMatch(matchBuilder, dpidLong, ofPort);
        flowBuilder.setMatch(matchBuilder.build());

        if (write) {
            // Create the OF Actions and Instructions
            InstructionBuilder ib = new InstructionBuilder();
            InstructionsBuilder isb = new InstructionsBuilder();

            // Instructions List Stores Individual Instructions
            List<Instruction> instructions = Lists.newArrayList();

            List<Action> actionList = Lists.newArrayList();
            ActionBuilder ab = new ActionBuilder();
            ab.setAction(ActionUtils.nxLoadRegAction(new DstNxRegCaseBuilder().setNxReg(REG_FIELD).build(),
                    BigInteger.valueOf(REG_VALUE_FROM_REMOTE)));
            ab.setOrder(0);
            ab.setKey(new ActionKey(0));
            actionList.add(ab.build());

            ApplyActionsBuilder aab = new ApplyActionsBuilder();
            aab.setAction(actionList);
            ib.setInstruction(new ApplyActionsCaseBuilder().setApplyActions(aab.build()).build());

            // Call the InstructionBuilder Methods Containing Actions
            ib.setOrder(0);
            ib.setKey(new InstructionKey(0));
            instructions.add(ib.build());

            // Append the default pipeline after the first classification
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
     * (Table:0) Ingress VLAN Traffic
     * Match: OpenFlow InPort and vlan ID
     * Action: GOTO Local Table (20)
     * table=0,vlan_id=0x5,in_port=10, actions=goto_table:2
     * table=0,in_port=2,dl_vlan=2001 actions=goto_table:20
     */
    @Override
    public void programVlanIn(Long dpidLong, String segmentationId, Long ethPort, boolean write) {
        NodeBuilder nodeBuilder = FlowUtils.createNodeBuilder(dpidLong);
        FlowBuilder flowBuilder = new FlowBuilder();
        String flowName = "VlanIn_" + segmentationId + "_" + ethPort;
        FlowUtils.initFlowBuilder(flowBuilder, flowName, getTable());

        MatchBuilder matchBuilder = new MatchBuilder();
        MatchUtils.createVlanIdMatch(matchBuilder, new VlanId(Integer.valueOf(segmentationId)), true);
        MatchUtils.createInPortMatch(matchBuilder, dpidLong, ethPort);
        flowBuilder.setMatch(matchBuilder.build());

        if (write) {
            // Create the OF Actions and Instructions
            InstructionsBuilder isb = new InstructionsBuilder();

            // Instructions List Stores Individual Instructions
            List<Instruction> instructions = Lists.newArrayList();

            // Append the default pipeline after the first classification
            InstructionBuilder ib = this.getMutablePipelineInstructionBuilder();
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
     * Create an LLDP Flow Rule to encapsulate into
     * a packet_in that is sent to the controller
     * for topology handling.
     * Match: Ethertype 0x88CCL
     * Action: Punt to Controller in a Packet_In msg
     */
    @Override
    public void programLLDPPuntRule(Long dpidLong) {
        NodeBuilder nodeBuilder = FlowUtils.createNodeBuilder(dpidLong);
        FlowBuilder flowBuilder = new FlowBuilder();
        String flowName = "LLDP";
        FlowUtils.initFlowBuilder(flowBuilder, flowName, Service.CLASSIFIER.getTable());

        MatchBuilder matchBuilder = new MatchBuilder();
        MatchUtils.createEtherTypeMatch(matchBuilder, new EtherType(0x88CCL));
        flowBuilder.setMatch(matchBuilder.build());

        // Create the OF Actions and Instructions
        InstructionBuilder ib = new InstructionBuilder();
        InstructionsBuilder isb = new InstructionsBuilder();

        // Instructions List Stores Individual Instructions
        List<Instruction> instructions = Lists.newArrayList();

        // Call the InstructionBuilder Methods Containing Actions
        InstructionUtils.createSendToControllerInstructions(FlowUtils.getNodeName(dpidLong), ib);
        ib.setOrder(0);
        ib.setKey(new InstructionKey(0));
        instructions.add(ib.build());

        // Add InstructionBuilder to the Instruction(s)Builder List
        isb.setInstruction(instructions);

        // Add InstructionsBuilder to FlowBuilder
        flowBuilder.setInstructions(isb.build());

        writeFlow(flowBuilder, nodeBuilder);
    }

    @Override
    public void programGotoTable(Long dpidLong, boolean write) {
        NodeBuilder nodeBuilder = FlowUtils.createNodeBuilder(dpidLong);
        FlowBuilder flowBuilder = new FlowBuilder();
        String flowName = "TableOffset_" + getTable();
        FlowUtils.initFlowBuilder(flowBuilder, flowName, Service.CLASSIFIER.getTable())
                .setPriority(0);

        MatchBuilder matchBuilder = new MatchBuilder();
        flowBuilder.setMatch(matchBuilder.build());

        if (write) {
            InstructionsBuilder isb = new InstructionsBuilder();
            List<Instruction> instructions = Lists.newArrayList();
            InstructionBuilder ib =
                    InstructionUtils.createGotoTableInstructions(new InstructionBuilder(), getTable());
            ib.setOrder(0);
            ib.setKey(new InstructionKey(0));
            instructions.add(ib.build());

            isb.setInstruction(instructions);
            flowBuilder.setInstructions(isb.build());
            writeFlow(flowBuilder, nodeBuilder);
        } else {
            removeFlow(flowBuilder, nodeBuilder);
        }
    }

    @Override
    public void setDependencies(BundleContext bundleContext, ServiceReference serviceReference) {
        super.setDependencies(bundleContext.getServiceReference(ClassifierProvider.class.getName()), this);
    }

    @Override
    public void setDependencies(Object impl) {}
}
