/*
 * Copyright (C) 2014 SDN Hub, LLC.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Madhu Venugopal, Srini Seetharaman
 */
package org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.services;

import java.math.BigInteger;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.controller.sal.utils.StatusCode;
import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.ovsdb.openstack.netvirt.api.LoadBalancerConfiguration;
import org.opendaylight.ovsdb.openstack.netvirt.api.LoadBalancerConfiguration.LoadBalancerPoolMember;
import org.opendaylight.ovsdb.openstack.netvirt.api.LoadBalancerProvider;
import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.AbstractServiceInstance;
import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.Service;
import org.opendaylight.ovsdb.utils.mdsal.openflow.ActionUtils;
import org.opendaylight.ovsdb.utils.mdsal.openflow.MatchUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.address.address.Ipv4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.FlowId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.InstructionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.ApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.apply.actions._case.ApplyActionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstNxRegCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.ovs.nx.action.rev140421.OfjNxHashFields;
import org.opendaylight.yang.gen.v1.urn.opendaylight.ovs.nx.action.rev140421.OfjNxMpAlgorithm;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg0;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg1;

import com.google.common.collect.Lists;

public class LoadBalancerService extends AbstractServiceInstance implements LoadBalancerProvider {

    private static final int DEFAULT_FLOW_PRIORITY = 32768;
    private static final Long SECOND_PASS_REG0_MATCH_VALUE = 1L;

    public LoadBalancerService() {
        super(Service.LOAD_BALANCER);
    }

    public LoadBalancerService(Service service) {
        super(service);
    }

    @Override
    public boolean isBridgeInPipeline (String nodeId) {
        return true;
    }

    /**
     * When this method is called, we do the following for minimizing flow updates:
     * 1. Overwrite the solo multipath rule that applies to all members
     * 2. Append second pass rule for the header rewriting specific to this member
     * 3. Append reverse rules specific to this member
     */
    @Override
    public Status programLoadBalancerMemberRules(Node node,
            LoadBalancerConfiguration lbConfig, LoadBalancerPoolMember member, org.opendaylight.ovsdb.openstack.netvirt.api.Action action) {
        NodeBuilder nodeBuilder = new NodeBuilder();
        nodeBuilder.setId((NodeId) node.getID());
        nodeBuilder.setKey(new NodeKey(nodeBuilder.getId()));

        //Update the multipath rule
        insertLoadBalancerVIPRulesFirstPass(nodeBuilder, lbConfig);

        if (action.equals(org.opendaylight.ovsdb.openstack.netvirt.api.Action.ADD)) {
            insertLoadBalancerMemberVIPRulesSecondPass(nodeBuilder, lbConfig.getVip(), member);
            insertLoadBalancerMemberReverseRules(nodeBuilder, lbConfig.getVip(), member);
            return new Status(StatusCode.SUCCESS);
        }
        /* TODO: Delete single member.
         * For now, removing a member requires deleting the full LB instance and re-adding
         */
        return new Status(StatusCode.NOTIMPLEMENTED);
    }

    /**
     * When this method is called, we perform the following:
     * 1. Write the solo multipath rule that applies to all members
     * 2. Append second pass rules for the header rewriting for all members
     * 3. Append reverse rules for all the members, specific to the protocol/port
     */
    @Override
    public Status programLoadBalancerRules(Node node, LoadBalancerConfiguration lbConfig, org.opendaylight.ovsdb.openstack.netvirt.api.Action action) {
        //for (org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node node:InventoryDataServiceUtil.readAllNodes()) {
        NodeBuilder nodeBuilder = new NodeBuilder();
        nodeBuilder.setId((NodeId) node.getID());
        nodeBuilder.setKey(new NodeKey(nodeBuilder.getId()));

        if (action.equals(org.opendaylight.ovsdb.openstack.netvirt.api.Action.ADD)) {
            insertLoadBalancerVIPRulesFirstPass(nodeBuilder, lbConfig);
            insertLoadBalancerVIPRulesSecondPass(nodeBuilder, lbConfig);
            insertLoadBalancerReverseRules(nodeBuilder, lbConfig);
            return new Status(StatusCode.SUCCESS);
        }
        else if (action.equals(org.opendaylight.ovsdb.openstack.netvirt.api.Action.DELETE)) {
            removeLoadBalancerVIPRules(nodeBuilder, lbConfig);
            removeLoadBalancerReverseRules(nodeBuilder, lbConfig);
            return new Status(StatusCode.SUCCESS);
        }

        return new Status(StatusCode.NOTIMPLEMENTED);
    }

    private void insertLoadBalancerVIPRulesFirstPass(NodeBuilder nodeBuilder, LoadBalancerConfiguration lbConfig) {
        MatchBuilder matchBuilder = new MatchBuilder();
        FlowBuilder flowBuilder = new FlowBuilder();

        // Match Only VIP
        MatchUtils.createDstL3IPv4Match(matchBuilder, new Ipv4Prefix(lbConfig.getVip()));

        // Create the OF Actions and Instructions
        InstructionsBuilder isb = new InstructionsBuilder();

        // Instructions List Stores Individual Instructions
        List<Instruction> instructions = Lists.newArrayList();

        List<Action> actionList = Lists.newArrayList();

        ActionBuilder ab = new ActionBuilder();
        ab.setAction(ActionUtils.nxLoadRegAction(new DstNxRegCaseBuilder().setNxReg(NxmNxReg0.class).build(),
                BigInteger.valueOf(SECOND_PASS_REG0_MATCH_VALUE)));
        ab.setOrder(0);
        ab.setKey(new ActionKey(0));
        actionList.add(ab.build());

        ab = new ActionBuilder();
        ab.setAction(ActionUtils.nxMultipathAction(OfjNxHashFields.NXHASHFIELDSSYMMETRICL4,
                (Integer)0, OfjNxMpAlgorithm.NXMPALGMODULON, (Integer)lbConfig.getMembers().size(),
                (Long)0L, (Integer)0, new DstNxRegCaseBuilder().setNxReg(NxmNxReg1.class).build()));
        ab.setOrder(1);
        ab.setKey(new ActionKey(1));
        actionList.add(ab.build());

        ab = new ActionBuilder();
        ab.setAction(ActionUtils.nxResubmitAction(null, this.getTable()));
        ab.setOrder(2);
        ab.setKey(new ActionKey(2));
        actionList.add(ab.build());

        // Create an Apply Action
        ApplyActionsBuilder aab = new ApplyActionsBuilder();
        aab.setAction(actionList);
        InstructionBuilder ib = new InstructionBuilder();
        ib.setInstruction(new ApplyActionsCaseBuilder().setApplyActions(aab.build()).build());

        // Call the InstructionBuilder Methods Containing Actions
        ib.setOrder(0);
        ib.setKey(new InstructionKey(0));
        instructions.add(ib.build());

        // Add InstructionBuilder to the Instruction(s)Builder List
        isb.setInstruction(instructions);

        // Add InstructionsBuilder to FlowBuilder
        flowBuilder.setInstructions(isb.build());

        String flowId = "LOADBALANCER_PIPELINE_FLOW_FORWARD1_" + lbConfig.getVip();
        flowBuilder.setId(new FlowId(flowId));
        FlowKey key = new FlowKey(new FlowId(flowId));
        flowBuilder.setMatch(matchBuilder.build());
        flowBuilder.setPriority(DEFAULT_FLOW_PRIORITY);
        flowBuilder.setBarrier(true);
        flowBuilder.setTableId((short) this.getTable());
        flowBuilder.setKey(key);
        flowBuilder.setFlowName(flowId);
        flowBuilder.setHardTimeout(0);
        flowBuilder.setIdleTimeout(0);
        writeFlow(flowBuilder, nodeBuilder);
    }

    /*
     * Method to program each rule that matches on Reg0 and Reg1 to insert appropriate header rewriting
     * rules for all members. This function calls insertLoadBalancerMemberVIPRulesSecondPass in turn.
     * @param nodeBuilder Node to insert rule to
     * @param lbConfig Configuration for this LoadBalancer instance
     */
    private void insertLoadBalancerVIPRulesSecondPass(NodeBuilder nodeBuilder, LoadBalancerConfiguration lbConfig) {
        Iterator<Map.Entry<String, LoadBalancerPoolMember>> it = lbConfig.getMembers().entrySet().iterator();
        Map.Entry<String, LoadBalancerPoolMember> entry;
        while (it.hasNext()) {
            entry = (Map.Entry<String, LoadBalancerPoolMember>)it.next();
            insertLoadBalancerMemberVIPRulesSecondPass(nodeBuilder, lbConfig.getVip(), (LoadBalancerPoolMember)entry.getValue());
            it.remove();
        }
    }

    private void insertLoadBalancerMemberVIPRulesSecondPass(NodeBuilder nodeBuilder, String vip, LoadBalancerPoolMember member) {
        MatchBuilder matchBuilder = new MatchBuilder();
        FlowBuilder flowBuilder = new FlowBuilder();

        // Match VIP, Reg0==1 and Reg1==Index of member
        MatchUtils.addNxRegMatch(matchBuilder, new MatchUtils.RegMatch(NxmNxReg0.class, SECOND_PASS_REG0_MATCH_VALUE));
        MatchUtils.addNxRegMatch(matchBuilder, new MatchUtils.RegMatch(NxmNxReg1.class, (long)member.getIndex()));
        MatchUtils.createDstL3IPv4Match(matchBuilder, new Ipv4Prefix(vip));

        // Create the OF Actions and Instructions
        InstructionsBuilder isb = new InstructionsBuilder();

        // Instructions List Stores Individual Instructions
        List<Instruction> instructions = Lists.newArrayList();

        List<Action> actionList = Lists.newArrayList();
        ActionBuilder ab = new ActionBuilder();
        ab.setAction(ActionUtils.setDlDstAction(new MacAddress(member.getMAC())));
        ab.setOrder(0);
        ab.setKey(new ActionKey(0));
        actionList.add(ab.build());

        ab = new ActionBuilder();
        Ipv4Builder ipb = new Ipv4Builder().setIpv4Address(new Ipv4Prefix(member.getIP()));
        ab.setAction(ActionUtils.setNwDstAction(ipb.build()));
        ab.setOrder(1);
        ab.setKey(new ActionKey(1));
        actionList.add(ab.build());

        // Create an Apply Action
        ApplyActionsBuilder aab = new ApplyActionsBuilder();
        aab.setAction(actionList);

        // Call the InstructionBuilder Methods Containing Actions
        InstructionBuilder ib = new InstructionBuilder();
        ib.setInstruction(new ApplyActionsCaseBuilder().setApplyActions(aab.build()).build());
        ib.setOrder(0);
        ib.setKey(new InstructionKey(0));
        instructions.add(ib.build());

        // Call the InstructionBuilder Methods Containing Actions
        ib = this.getMutablePipelineInstructionBuilder();
        ib.setOrder(1);
        ib.setKey(new InstructionKey(1));
        instructions.add(ib.build());

        // Add InstructionBuilder to the Instruction(s)Builder List
        isb.setInstruction(instructions);

        // Add InstructionsBuilder to FlowBuilder
        flowBuilder.setInstructions(isb.build());

        String flowId = "LOADBALANCER_PIPELINE_FORWARD2_" + vip + "_" + member.getIP();
        flowBuilder.setId(new FlowId(flowId));
        FlowKey key = new FlowKey(new FlowId(flowId));
        flowBuilder.setMatch(matchBuilder.build());
        flowBuilder.setPriority(DEFAULT_FLOW_PRIORITY + 1);
        flowBuilder.setBarrier(true);
        flowBuilder.setTableId((short) this.getTable());
        flowBuilder.setKey(key);
        flowBuilder.setFlowName(flowId);
        flowBuilder.setHardTimeout(0);
        flowBuilder.setIdleTimeout(0);
        writeFlow(flowBuilder, nodeBuilder);
    }

    /**
     * Method to program all reverse rules that matches member {IP, Protocol, Port} for all members.
     * This function calls insertLoadBalancerMemberReverseRules in turn.
     * @param nodeBuilder Node to insert rule to
     * @param lbConfig Configuration for this LoadBalancer instance
     */
    private void insertLoadBalancerReverseRules(NodeBuilder nodeBuilder, LoadBalancerConfiguration lbConfig) {
        Iterator<Map.Entry<String, LoadBalancerPoolMember>> it = lbConfig.getMembers().entrySet().iterator();
        Map.Entry<String, LoadBalancerPoolMember> entry;
        while (it.hasNext()) {
            entry = (Map.Entry<String, LoadBalancerPoolMember>)it.next();
            insertLoadBalancerMemberReverseRules(nodeBuilder, lbConfig.getVip(), (LoadBalancerPoolMember)entry.getValue());
            it.remove();
        }
    }

    private void insertLoadBalancerMemberReverseRules(NodeBuilder nodeBuilder, String vip, LoadBalancerPoolMember member) {
        MatchBuilder matchBuilder = new MatchBuilder();
        FlowBuilder flowBuilder = new FlowBuilder();

        // Match VIP, Reg0==1 and Reg1==Index of member
        MatchUtils.createSrcL3IPv4Match(matchBuilder, new Ipv4Prefix(member.getIP()));
        if (member.getProtocol().equalsIgnoreCase(LoadBalancerConfiguration.PROTOCOL_HTTP))
            MatchUtils.createSetSrcTcpMatch(matchBuilder, new PortNumber(LoadBalancerConfiguration.PROTOCOL_HTTP_PORT));
        else if (member.getProtocol().equalsIgnoreCase(LoadBalancerConfiguration.PROTOCOL_HTTPS))
            MatchUtils.createSetSrcTcpMatch(matchBuilder, new PortNumber(LoadBalancerConfiguration.PROTOCOL_HTTPS_PORT));
        else //Not possible
            return;

        // Create the OF Actions and Instructions
        InstructionsBuilder isb = new InstructionsBuilder();

        // Instructions List Stores Individual Instructions
        List<Instruction> instructions = Lists.newArrayList();

        List<Action> actionList = Lists.newArrayList();
        ActionBuilder ab = new ActionBuilder();
        Ipv4Builder ipb = new Ipv4Builder().setIpv4Address(new Ipv4Prefix(vip));
        ab.setAction(ActionUtils.setNwSrcAction(ipb.build()));
        ab.setOrder(0);
        ab.setKey(new ActionKey(0));
        actionList.add(ab.build());

        // Create an Apply Action
        ApplyActionsBuilder aab = new ApplyActionsBuilder();
        aab.setAction(actionList);

        // Call the InstructionBuilder Methods Containing Actions
        InstructionBuilder ib = new InstructionBuilder();
        ib.setInstruction(new ApplyActionsCaseBuilder().setApplyActions(aab.build()).build());
        ib.setOrder(0);
        ib.setKey(new InstructionKey(0));
        instructions.add(ib.build());

        // Call the InstructionBuilder Methods Containing Actions
        ib = this.getMutablePipelineInstructionBuilder();
        ib.setOrder(1);
        ib.setKey(new InstructionKey(1));
        instructions.add(ib.build());

        // Add InstructionBuilder to the Instruction(s)Builder List
        isb.setInstruction(instructions);

        // Add InstructionsBuilder to FlowBuilder
        flowBuilder.setInstructions(isb.build());

        String flowId = "LOADBALANCER_PIPELINE_REVERSE_" + vip + "_" + member.getIP();
        flowBuilder.setId(new FlowId(flowId));
        FlowKey key = new FlowKey(new FlowId(flowId));
        flowBuilder.setMatch(matchBuilder.build());
        flowBuilder.setPriority(DEFAULT_FLOW_PRIORITY);
        flowBuilder.setBarrier(true);
        flowBuilder.setTableId((short) this.getTable());
        flowBuilder.setKey(key);
        flowBuilder.setFlowName(flowId);
        flowBuilder.setHardTimeout(0);
        flowBuilder.setIdleTimeout(0);
        writeFlow(flowBuilder, nodeBuilder);
    }

    private void removeLoadBalancerVIPRules(NodeBuilder nodeBuilder, LoadBalancerConfiguration lbConfig) {
        //TODO: Remove all VIP flows
    }

    private void removeLoadBalancerReverseRules(NodeBuilder nodeBuilder, LoadBalancerConfiguration lbConfig) {
        Iterator<Map.Entry<String, LoadBalancerPoolMember>> it = lbConfig.getMembers().entrySet().iterator();
        Map.Entry<String, LoadBalancerPoolMember> entry;
        while (it.hasNext()) {
            entry = (Map.Entry<String, LoadBalancerPoolMember>)it.next();
            LoadBalancerPoolMember member = (LoadBalancerPoolMember) entry.getValue();
            //TODO: Remove per member reverse flow
            it.remove();
        }
    }
}
