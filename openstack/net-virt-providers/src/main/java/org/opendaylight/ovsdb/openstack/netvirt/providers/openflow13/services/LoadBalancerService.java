/*
 * Copyright (c) 2014 SDN Hub, LLC. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.services;

import java.math.BigInteger;
import java.util.List;
import java.util.Map;

import org.opendaylight.ovsdb.openstack.netvirt.NetworkHandler;
import org.opendaylight.ovsdb.openstack.netvirt.api.Constants;
import org.opendaylight.ovsdb.openstack.netvirt.api.LoadBalancerConfiguration;
import org.opendaylight.ovsdb.openstack.netvirt.api.LoadBalancerConfiguration.LoadBalancerPoolMember;
import org.opendaylight.ovsdb.openstack.netvirt.api.LoadBalancerProvider;
import org.opendaylight.ovsdb.openstack.netvirt.api.Status;
import org.opendaylight.ovsdb.openstack.netvirt.api.StatusCode;
import org.opendaylight.ovsdb.openstack.netvirt.providers.ConfigInterface;
import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.AbstractServiceInstance;
import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.Service;
import org.opendaylight.ovsdb.utils.mdsal.openflow.ActionUtils;
import org.opendaylight.ovsdb.utils.mdsal.openflow.MatchUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.address.address.Ipv4Builder;
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
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.VlanId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstNxRegCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.action.rev140421.OfjNxHashFields;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.action.rev140421.OfjNxMpAlgorithm;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;

public class LoadBalancerService extends AbstractServiceInstance implements LoadBalancerProvider, ConfigInterface {

    private static final Logger LOG = LoggerFactory.getLogger(LoadBalancerProvider.class);
    private static final int DEFAULT_FLOW_PRIORITY = 32768;
    private static final Long FIRST_PASS_REGA_MATCH_VALUE = 0L;
    private static final Long SECOND_PASS_REGA_MATCH_VALUE = 1L;

    private static final Class<? extends NxmNxReg> REG_FIELD_A = NxmNxReg1.class;
    private static final Class<? extends NxmNxReg> REG_FIELD_B = NxmNxReg2.class;

    public LoadBalancerService() {
        super(Service.LOAD_BALANCER);
    }

    public LoadBalancerService(Service service) {
        super(service);
    }

    /**
     * When this method is called, we do the following for minimizing flow updates:
     * 1. Overwrite the solo multipath rule that applies to all members
     * 2. Append second pass rule for the header rewriting specific to this member
     * 3. Append reverse rules specific to this member
     */
    @Override
    public Status programLoadBalancerPoolMemberRules(Node node,
                                                     LoadBalancerConfiguration lbConfig, LoadBalancerPoolMember member,
                                                     org.opendaylight.ovsdb.openstack.netvirt.api.Action action) {
        if (lbConfig == null || member == null) {
            LOG.error("Null value for LB config {} or Member {}", lbConfig, member);
            return new Status(StatusCode.BADREQUEST);
        }
        if (!lbConfig.isValid()) {
            LOG.error("LB config is invalid: {}", lbConfig);
            return new Status(StatusCode.BADREQUEST);
        }
        LOG.debug("Performing {} rules for member {} with index {} on LB with VIP {} and total members {}",
                action, member.getIP(), member.getIndex(), lbConfig.getVip(), lbConfig.getMembers().size());

        NodeBuilder nodeBuilder = new NodeBuilder();
        nodeBuilder.setId(new NodeId(Constants.OPENFLOW_NODE_PREFIX + node.getNodeId().getValue()));
        nodeBuilder.setKey(new NodeKey(nodeBuilder.getId()));

        //Update the multipath rule
        manageLoadBalancerVIPRulesFirstPass(nodeBuilder, lbConfig, true);

        if (action.equals(org.opendaylight.ovsdb.openstack.netvirt.api.Action.ADD)) {
            manageLoadBalancerMemberVIPRulesSecondPass(nodeBuilder, lbConfig, member, true);
            manageLoadBalancerMemberReverseRules(nodeBuilder, lbConfig, member, true);
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
    public Status programLoadBalancerRules(Node node, LoadBalancerConfiguration lbConfig,
                                           org.opendaylight.ovsdb.openstack.netvirt.api.Action action) {
        if (lbConfig == null || !lbConfig.isValid()) {
            LOG.error("LB config is invalid: {}", lbConfig);
            return new Status(StatusCode.BADREQUEST);
        }
        LOG.debug("Performing {} rules for VIP {} and {} members", action, lbConfig.getVip(), lbConfig.getMembers().size());

        NodeBuilder nodeBuilder = new NodeBuilder();
        nodeBuilder.setId(new NodeId(Constants.OPENFLOW_NODE_PREFIX + node.getNodeId().getValue()));
        nodeBuilder.setKey(new NodeKey(nodeBuilder.getId()));

        if (action.equals(org.opendaylight.ovsdb.openstack.netvirt.api.Action.ADD)) {
            manageLoadBalancerVIPRulesFirstPass(nodeBuilder, lbConfig, true);
            manageLoadBalancerVIPRulesSecondPass(nodeBuilder, lbConfig, true);
            manageLoadBalancerReverseRules(nodeBuilder, lbConfig, true);
            return new Status(StatusCode.SUCCESS);
        }
        else if (action.equals(org.opendaylight.ovsdb.openstack.netvirt.api.Action.DELETE)) {
            manageLoadBalancerVIPRulesFirstPass(nodeBuilder, lbConfig, false);
            manageLoadBalancerVIPRulesSecondPass(nodeBuilder, lbConfig, false);
            manageLoadBalancerReverseRules(nodeBuilder, lbConfig, false);
            return new Status(StatusCode.SUCCESS);
        }

        return new Status(StatusCode.NOTIMPLEMENTED);
    }

    /**
     * Method to insert/remove default rule for traffic destined to the VIP and no
     * server selection performed yet
     * @param nodeBuilder NodeBuilder
     * @param lbConfig LoadBalancerConfiguration
     * @param write Boolean to indicate of the flow is to be inserted or removed
     */
    private void manageLoadBalancerVIPRulesFirstPass(NodeBuilder nodeBuilder, LoadBalancerConfiguration lbConfig, boolean write) {
        MatchBuilder matchBuilder = new MatchBuilder();
        FlowBuilder flowBuilder = new FlowBuilder();

        // Match Tunnel-ID, VIP, and Reg0==0
        if (lbConfig.getProviderNetworkType().equalsIgnoreCase(NetworkHandler.NETWORK_TYPE_VXLAN) ||
            lbConfig.getProviderNetworkType().equalsIgnoreCase(NetworkHandler.NETWORK_TYPE_GRE)) {
            MatchUtils.createTunnelIDMatch(matchBuilder, new BigInteger(lbConfig.getProviderSegmentationId()));
        } else if (lbConfig.getProviderNetworkType().equalsIgnoreCase(NetworkHandler.NETWORK_TYPE_VLAN)) {
            MatchUtils.createVlanIdMatch(matchBuilder, new VlanId(Integer.valueOf(lbConfig.getProviderSegmentationId())), true);
        } else {
            return; //Should not get here. TODO: Other types
        }

        MatchUtils.createDstL3IPv4Match(matchBuilder, MatchUtils.iPv4PrefixFromIPv4Address(lbConfig.getVip()));
        MatchUtils.addNxRegMatch(matchBuilder, new MatchUtils.RegMatch(REG_FIELD_A, FIRST_PASS_REGA_MATCH_VALUE));

        String flowId = "LOADBALANCER_FORWARD_FLOW1_" + lbConfig.getProviderSegmentationId() + "_" + lbConfig.getVip();
        flowBuilder.setId(new FlowId(flowId));
        FlowKey key = new FlowKey(new FlowId(flowId));
        flowBuilder.setMatch(matchBuilder.build());
        flowBuilder.setPriority(DEFAULT_FLOW_PRIORITY);
        flowBuilder.setBarrier(true);
        flowBuilder.setTableId(this.getTable());
        flowBuilder.setKey(key);
        flowBuilder.setFlowName(flowId);
        flowBuilder.setHardTimeout(0);
        flowBuilder.setIdleTimeout(0);

        if (write) {
            // Create the OF Actions and Instructions
            InstructionsBuilder isb = new InstructionsBuilder();

            // Instructions List Stores Individual Instructions
            List<Instruction> instructions = Lists.newArrayList();

            List<Action> actionList = Lists.newArrayList();

            ActionBuilder ab = new ActionBuilder();
            ab.setAction(ActionUtils.nxLoadRegAction(new DstNxRegCaseBuilder().setNxReg(REG_FIELD_A).build(),
                    BigInteger.valueOf(SECOND_PASS_REGA_MATCH_VALUE)));
            ab.setOrder(0);
            ab.setKey(new ActionKey(0));
            actionList.add(ab.build());

            ab = new ActionBuilder();
            ab.setAction(ActionUtils.nxMultipathAction(OfjNxHashFields.NXHASHFIELDSSYMMETRICL4,
                    0, OfjNxMpAlgorithm.NXMPALGMODULON,
                    lbConfig.getMembers().size()-1, //By Nicira-Ext spec, this field is max_link minus 1
                    0L, new DstNxRegCaseBuilder().setNxReg(REG_FIELD_B).build(),
                    0, 31));
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

            writeFlow(flowBuilder, nodeBuilder);

        } else {
            removeFlow(flowBuilder, nodeBuilder);
        }
    }

    /*
     * Method to program each rule that matches on Reg0 and Reg1 to insert appropriate header rewriting
     * rules for all members. This function calls manageLoadBalancerMemberVIPRulesSecondPass in turn.
     * @param nodeBuilder Node to insert rule to
     * @param lbConfig Configuration for this LoadBalancer instance
     * @param write Boolean to indicate of the flow is to be inserted or removed
     */
    private void manageLoadBalancerVIPRulesSecondPass(NodeBuilder nodeBuilder, LoadBalancerConfiguration lbConfig, boolean write) {
        for(Map.Entry<String, LoadBalancerPoolMember> entry : lbConfig.getMembers().entrySet()){
            manageLoadBalancerMemberVIPRulesSecondPass(nodeBuilder, lbConfig, entry.getValue(), write);
        }
    }

    private void manageLoadBalancerMemberVIPRulesSecondPass(NodeBuilder nodeBuilder, LoadBalancerConfiguration lbConfig, LoadBalancerPoolMember member, boolean write) {
        String vip = lbConfig.getVip();

        MatchBuilder matchBuilder = new MatchBuilder();
        FlowBuilder flowBuilder = new FlowBuilder();

        // Match Tunnel-ID, VIP, Reg0==1 and Reg1==Index of member
        if (lbConfig.getProviderNetworkType().equalsIgnoreCase(NetworkHandler.NETWORK_TYPE_VXLAN) ||
            lbConfig.getProviderNetworkType().equalsIgnoreCase(NetworkHandler.NETWORK_TYPE_GRE)) {
            MatchUtils.createTunnelIDMatch(matchBuilder, new BigInteger(lbConfig.getProviderSegmentationId()));
        } else if (lbConfig.getProviderNetworkType().equalsIgnoreCase(NetworkHandler.NETWORK_TYPE_VLAN)) {
            MatchUtils.createVlanIdMatch(matchBuilder, new VlanId(Integer.valueOf(lbConfig.getProviderSegmentationId())), true);
        } else {
            return; //Should not get here. TODO: Other types
        }

        MatchUtils.createDstL3IPv4Match(matchBuilder, MatchUtils.iPv4PrefixFromIPv4Address(vip));
        MatchUtils.addNxRegMatch(matchBuilder, new MatchUtils.RegMatch(REG_FIELD_A, SECOND_PASS_REGA_MATCH_VALUE),
                                               new MatchUtils.RegMatch(REG_FIELD_B, (long)member.getIndex()));

        String flowId = "LOADBALANCER_FORWARD_FLOW2_" + lbConfig.getProviderSegmentationId() + "_" +
                        vip + "_" + member.getIP();
        flowBuilder.setId(new FlowId(flowId));
        FlowKey key = new FlowKey(new FlowId(flowId));
        flowBuilder.setMatch(matchBuilder.build());
        flowBuilder.setPriority(DEFAULT_FLOW_PRIORITY+1);
        flowBuilder.setBarrier(true);
        flowBuilder.setTableId(this.getTable());
        flowBuilder.setKey(key);
        flowBuilder.setFlowName(flowId);
        flowBuilder.setHardTimeout(0);
        flowBuilder.setIdleTimeout(0);

        if (write) {
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
            Ipv4Builder ipb = new Ipv4Builder().setIpv4Address(MatchUtils.iPv4PrefixFromIPv4Address(member.getIP()));
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

            writeFlow(flowBuilder, nodeBuilder);

        } else {
            removeFlow(flowBuilder, nodeBuilder);
        }
    }

    /**
     * Method to program all reverse rules that matches member {IP, Protocol, Port} for all members.
     * This function calls manageLoadBalancerMemberReverseRules in turn.
     * @param nodeBuilder Node to insert rule to
     * @param lbConfig Configuration for this LoadBalancer instance
     */
    private void manageLoadBalancerReverseRules(NodeBuilder nodeBuilder, LoadBalancerConfiguration lbConfig, boolean write) {
        for(Map.Entry<String, LoadBalancerPoolMember> entry : lbConfig.getMembers().entrySet()){
            manageLoadBalancerMemberReverseRules(nodeBuilder, lbConfig, entry.getValue(), write);
        }
    }

    private void manageLoadBalancerMemberReverseRules(NodeBuilder nodeBuilder, LoadBalancerConfiguration lbConfig,
            LoadBalancerPoolMember member, boolean write) {

        String vip = lbConfig.getVip();
        String vmac = lbConfig.getVmac();

        MatchBuilder matchBuilder = new MatchBuilder();
        FlowBuilder flowBuilder = new FlowBuilder();

        // Match Tunnel-ID, MemberIP, and Protocol/Port
        if (lbConfig.getProviderNetworkType().equalsIgnoreCase(NetworkHandler.NETWORK_TYPE_VXLAN) ||
                   lbConfig.getProviderNetworkType().equalsIgnoreCase(NetworkHandler.NETWORK_TYPE_GRE)) {
            MatchUtils.createTunnelIDMatch(matchBuilder, new BigInteger(lbConfig.getProviderSegmentationId()));
        } else if (lbConfig.getProviderNetworkType().equalsIgnoreCase(NetworkHandler.NETWORK_TYPE_VLAN)) {
            MatchUtils.createVlanIdMatch(matchBuilder, new VlanId(Integer.valueOf(lbConfig.getProviderSegmentationId())), true);
        } else {
            return; //Should not get here. TODO: Other types
        }

        MatchUtils.createSrcL3IPv4Match(matchBuilder, MatchUtils.iPv4PrefixFromIPv4Address(member.getIP()));
        MatchUtils.createSetSrcTcpMatch(matchBuilder, new PortNumber(member.getPort()));

        String flowId = "LOADBALANCER_REVERSE_FLOW_" + lbConfig.getProviderSegmentationId() +
                        vip + "_" + member.getIP();
        flowBuilder.setId(new FlowId(flowId));
        FlowKey key = new FlowKey(new FlowId(flowId));
        flowBuilder.setMatch(matchBuilder.build());
        flowBuilder.setPriority(DEFAULT_FLOW_PRIORITY);
        flowBuilder.setBarrier(true);
        flowBuilder.setTableId(this.getTable());
        flowBuilder.setKey(key);
        flowBuilder.setFlowName(flowId);
        flowBuilder.setHardTimeout(0);
        flowBuilder.setIdleTimeout(0);

        if (write) {
            // Create the OF Actions and Instructions
            InstructionsBuilder isb = new InstructionsBuilder();

            // Instructions List Stores Individual Instructions
            List<Instruction> instructions = Lists.newArrayList();

            List<Action> actionList = Lists.newArrayList();
            ActionBuilder ab = new ActionBuilder();
            Ipv4Builder ipb = new Ipv4Builder().setIpv4Address(MatchUtils.iPv4PrefixFromIPv4Address(vip));
            ab.setAction(ActionUtils.setNwSrcAction(ipb.build()));
            ab.setOrder(0);
            ab.setKey(new ActionKey(0));
            actionList.add(ab.build());

            /* If a dummy MAC is assigned to the VIP, we use that as the
             * source MAC for the reverse traffic.
             */
            if (vmac != null) {
                ab = new ActionBuilder();
                ab.setAction(ActionUtils.setDlDstAction(new MacAddress(vmac)));
                ab.setOrder(1);
                ab.setKey(new ActionKey(1));
                actionList.add(ab.build());
            }

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

            writeFlow(flowBuilder, nodeBuilder);

        } else {
            removeFlow(flowBuilder, nodeBuilder);
        }
    }

    @Override
    public void setDependencies(BundleContext bundleContext, ServiceReference serviceReference) {
        super.setDependencies(bundleContext.getServiceReference(LoadBalancerProvider.class.getName()), this);
    }

    @Override
    public void setDependencies(Object impl) {

    }
}
