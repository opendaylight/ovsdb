/*
 * Copyright (c) 2014, 2015 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.services;

import java.math.BigInteger;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.List;

import org.apache.commons.net.util.SubnetUtils;
import org.opendaylight.ovsdb.openstack.netvirt.api.Action;
import org.opendaylight.ovsdb.openstack.netvirt.api.Constants;
import org.opendaylight.ovsdb.openstack.netvirt.api.RoutingProvider;
import org.opendaylight.ovsdb.openstack.netvirt.providers.ConfigInterface;
import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.AbstractServiceInstance;
import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.Service;
import org.opendaylight.ovsdb.openstack.netvirt.api.Status;
import org.opendaylight.ovsdb.openstack.netvirt.api.StatusCode;
import org.opendaylight.ovsdb.utils.mdsal.openflow.ActionUtils;
import org.opendaylight.ovsdb.utils.mdsal.openflow.FlowUtils;
import org.opendaylight.ovsdb.utils.mdsal.openflow.MatchUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.InstructionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.ApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.apply.actions._case.ApplyActionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder;

import com.google.common.collect.Lists;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RoutingService extends AbstractServiceInstance implements RoutingProvider, ConfigInterface {
    private static final Logger LOG = LoggerFactory.getLogger(RoutingService.class);
    public RoutingService() {
        super(Service.ROUTING);
    }

    public RoutingService(Service service) {
        super(service);
    }

    @Override
    public Status programRouterInterface(Long dpid, String sourceSegId, String destSegId, String macAddress,
                                         InetAddress address, int mask, Action action) {

        SubnetUtils addressSubnetInfo = new SubnetUtils(address.getHostAddress() + "/" + mask);
        final String prefixString = addressSubnetInfo.getInfo().getNetworkAddress() + "/" + mask;

        NodeBuilder nodeBuilder = FlowUtils.createNodeBuilder(dpid);
        FlowBuilder flowBuilder = new FlowBuilder();
        String flowName = "Routing_" + sourceSegId + "_" + destSegId + "_" + prefixString;
        FlowUtils.initFlowBuilder(flowBuilder, flowName, getTable()).setPriority(2048);

        MatchBuilder matchBuilder = new MatchBuilder();
        if (sourceSegId.equals(Constants.EXTERNAL_NETWORK)) {
            // If matching on external network, use register reserved for InboundNatService to ensure that
            // ip rewrite is meant to be consumed by this destination tunnel id.
            MatchUtils.addNxRegMatch(matchBuilder,
                    new MatchUtils.RegMatch(InboundNatService.REG_FIELD, Long.valueOf(destSegId)));
        } else {
            MatchUtils.createTunnelIDMatch(matchBuilder, new BigInteger(sourceSegId));
        }

        if (address instanceof Inet6Address) {
            // WORKAROUND: For now ipv6 is not supported
            // TODO: implement ipv6 case
            LOG.debug("ipv6 address is not implemented yet. address {}", address);
            return new Status(StatusCode.NOTIMPLEMENTED);
        }
        MatchUtils.createDstL3IPv4Match(matchBuilder, new Ipv4Prefix(prefixString));
        flowBuilder.setMatch(matchBuilder.build());

        if (action.equals(Action.ADD)) {
            // Instructions List Stores Individual Instructions
            InstructionsBuilder isb = new InstructionsBuilder();
            List<Instruction> instructions = Lists.newArrayList();
            InstructionBuilder ib = new InstructionBuilder();
            ApplyActionsBuilder aab = new ApplyActionsBuilder();
            ActionBuilder ab = new ActionBuilder();
            List<org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action> actionList
                    = Lists.newArrayList();

            // Set source Mac address
            ab.setAction(ActionUtils.setDlSrcAction(new MacAddress(macAddress)));
            ab.setOrder(0);
            ab.setKey(new ActionKey(0));
            actionList.add(ab.build());

            // DecTTL
            ab.setAction(ActionUtils.decNwTtlAction());
            ab.setOrder(1);
            ab.setKey(new ActionKey(1));
            actionList.add(ab.build());

            // Set Destination Tunnel ID
            ab.setAction(ActionUtils.setTunnelIdAction(new BigInteger(destSegId)));
            ab.setOrder(2);
            ab.setKey(new ActionKey(2));
            actionList.add(ab.build());

            // Create Apply Actions Instruction
            aab.setAction(actionList);
            ib.setInstruction(new ApplyActionsCaseBuilder().setApplyActions(aab.build()).build());
            ib.setOrder(0);
            ib.setKey(new InstructionKey(0));
            instructions.add(ib.build());

            // Goto Next Table
            ib = getMutablePipelineInstructionBuilder();
            ib.setOrder(2);
            ib.setKey(new InstructionKey(2));
            instructions.add(ib.build());

            flowBuilder.setInstructions(isb.setInstruction(instructions).build());
            writeFlow(flowBuilder, nodeBuilder);
        } else {
            removeFlow(flowBuilder, nodeBuilder);
        }

        // ToDo: WriteFlow/RemoveFlow should return something we can use to check success
        return new Status(StatusCode.SUCCESS);
    }

    @Override
    public Status programDefaultRouteEntry(Long dpid, String segmentationId, String macAddress,
                                           InetAddress nextHop, Action action) {

        NodeBuilder nodeBuilder = FlowUtils.createNodeBuilder(dpid);
        FlowBuilder flowBuilder = new FlowBuilder();
        String flowName = "DefaultRoute_" + nextHop.getHostAddress();
        FlowUtils.initFlowBuilder(flowBuilder, flowName, getTable()).setPriority(1024);

        MatchBuilder matchBuilder = new MatchBuilder();
        MatchUtils.createTunnelIDMatch(matchBuilder, new BigInteger(segmentationId));
        flowBuilder.setMatch(matchBuilder.build());

        if (action.equals(Action.ADD)) {
            // Instructions List Stores Individual Instructions
            InstructionsBuilder isb = new InstructionsBuilder();
            List<Instruction> instructions = Lists.newArrayList();
            InstructionBuilder ib = new InstructionBuilder();
            ApplyActionsBuilder aab = new ApplyActionsBuilder();
            ActionBuilder ab = new ActionBuilder();
            List<org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action> actionList =
                    Lists.newArrayList();

            // Set source Mac address
            ab.setAction(ActionUtils.setDlSrcAction(new MacAddress(macAddress)));
            ab.setOrder(0);
            ab.setKey(new ActionKey(0));
            actionList.add(ab.build());

            // DecTTL
            ab.setAction(ActionUtils.decNwTtlAction());
            ab.setOrder(1);
            ab.setKey(new ActionKey(1));
            actionList.add(ab.build());

            // Create Apply Actions Instruction
            aab.setAction(actionList);
            ib.setInstruction(new ApplyActionsCaseBuilder().setApplyActions(aab.build()).build());
            ib.setOrder(0);
            ib.setKey(new InstructionKey(0));
            instructions.add(ib.build());

            // Goto Next Table
            ib = getMutablePipelineInstructionBuilder();
            ib.setOrder(1);
            ib.setKey(new InstructionKey(1));
            instructions.add(ib.build());

            flowBuilder.setInstructions(isb.setInstruction(instructions).build());

            writeFlow(flowBuilder, nodeBuilder);
        } else {
            removeFlow(flowBuilder, nodeBuilder);
        }

        // ToDo: WriteFlow/RemoveFlow should return something we can use to check success
        return new Status(StatusCode.SUCCESS);
    }

    @Override
    public void setDependencies(BundleContext bundleContext, ServiceReference serviceReference) {
        super.setDependencies(bundleContext.getServiceReference(RoutingProvider.class.getName()), this);
    }

    @Override
    public void setDependencies(Object impl) {

    }
}
