/*
 * Copyright (C) 2014 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Madhu Venugopal, Dave Tucker
 */

package org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.services;

import java.math.BigInteger;
import java.net.InetAddress;
import java.util.List;

import org.opendaylight.controller.sal.core.Node;
import org.opendaylight.controller.sal.utils.Status;
import org.opendaylight.controller.sal.utils.StatusCode;
import org.opendaylight.ovsdb.openstack.netvirt.api.Action;
import org.opendaylight.ovsdb.openstack.netvirt.api.Constants;
import org.opendaylight.ovsdb.openstack.netvirt.api.RoutingProvider;
import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.AbstractServiceInstance;
import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.OF13Provider;
import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.Service;
import org.opendaylight.ovsdb.utils.mdsal.openflow.InstructionUtils;
import org.opendaylight.ovsdb.utils.mdsal.openflow.MatchUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
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

import com.google.common.collect.Lists;

public class RoutingService extends AbstractServiceInstance implements RoutingProvider {
    public RoutingService() {
        super(Service.ROUTING);
    }

    public RoutingService(Service service) {
        super(service);
    }

    @Override
    public Status programRouterInterface(Node node, Long dpid, String segmentationId, String macAddress,
                                         InetAddress address, int mask, Action action) {

        String nodeName = Constants.OPENFLOW_NODE_PREFIX + dpid;

        MatchBuilder matchBuilder = new MatchBuilder();
        NodeBuilder nodeBuilder = OF13Provider.createNodeBuilder(nodeName);

        // Instructions List Stores Individual Instructions
        InstructionsBuilder isb = new InstructionsBuilder();
        List<Instruction> instructions = Lists.newArrayList();
        InstructionBuilder ib = new InstructionBuilder();

        String prefixString = address.getHostAddress() + "/" + mask;
        MatchUtils.createTunnelIDMatch(matchBuilder, new BigInteger(segmentationId));
        MatchUtils.createDstL3IPv4Match(matchBuilder, new Ipv4Prefix(prefixString));

        // Set source Mac address
        InstructionUtils.createDlSrcInstructions(ib, new MacAddress(macAddress));
        ib.setOrder(0);
        ib.setKey(new InstructionKey(0));
        instructions.add(ib.build());

        // DecTTL
        InstructionUtils.createDecNwTtlInstructions(ib);
        ib.setOrder(1);
        ib.setKey(new InstructionKey(1));
        instructions.add(ib.build());

        // Goto Next Table
        ib = getMutablePipelineInstructionBuilder();
        ib.setOrder(2);
        ib.setKey(new InstructionKey(2));
        instructions.add(ib.build());

        FlowBuilder flowBuilder = new FlowBuilder();
        flowBuilder.setMatch(matchBuilder.build());
        flowBuilder.setInstructions(isb.setInstruction(instructions).build());

        String flowId = "Routing_" + prefixString;
        flowBuilder.setId(new FlowId(flowId));
        FlowKey key = new FlowKey(new FlowId(flowId));
        flowBuilder.setBarrier(true);
        flowBuilder.setTableId(this.getTable());
        flowBuilder.setKey(key);
        flowBuilder.setPriority(2048);
        flowBuilder.setFlowName(flowId);
        flowBuilder.setHardTimeout(0);
        flowBuilder.setIdleTimeout(0);
        writeFlow(flowBuilder, nodeBuilder);

        if (action.equals(Action.ADD)) {
            writeFlow(flowBuilder, nodeBuilder);
        } else {
            removeFlow(flowBuilder, nodeBuilder);
        }

        // ToDo: WriteFlow/RemoveFlow should return something we can use to check success
        return new Status(StatusCode.SUCCESS);
    }

    @Override
    public Status programDefaultRouteEntry(Node node, Long dpid, String segmentationId, String macAddress,
                                           InetAddress nextHop, Action action) {

        String nodeName = Constants.OPENFLOW_NODE_PREFIX + dpid;

        MatchBuilder matchBuilder = new MatchBuilder();
        NodeBuilder nodeBuilder = OF13Provider.createNodeBuilder(nodeName);

        MatchUtils.createTunnelIDMatch(matchBuilder, new BigInteger(segmentationId));

        // Instructions List Stores Individual Instructions
        InstructionsBuilder isb = new InstructionsBuilder();
        List<Instruction> instructions = Lists.newArrayList();
        InstructionBuilder ib = new InstructionBuilder();

        // Set source Mac address
        InstructionUtils.createDlSrcInstructions(ib, new MacAddress(macAddress));
        ib.setOrder(0);
        ib.setKey(new InstructionKey(0));
        instructions.add(ib.build());

        // DecTTL
        InstructionUtils.createDecNwTtlInstructions(ib);
        ib.setOrder(1);
        ib.setKey(new InstructionKey(1));
        instructions.add(ib.build());

        // Goto Next Table
        ib = getMutablePipelineInstructionBuilder();
        ib.setOrder(2);
        ib.setKey(new InstructionKey(2));
        instructions.add(ib.build());

        FlowBuilder flowBuilder = new FlowBuilder();
        flowBuilder.setMatch(matchBuilder.build());
        flowBuilder.setInstructions(isb.setInstruction(instructions).build());

        String flowId = "DefaultRoute_" + nextHop.getHostAddress();
        flowBuilder.setId(new FlowId(flowId));
        FlowKey key = new FlowKey(new FlowId(flowId));
        flowBuilder.setBarrier(true);
        flowBuilder.setTableId(this.getTable());
        flowBuilder.setKey(key);
        flowBuilder.setPriority(1024);
        flowBuilder.setFlowName(flowId);
        flowBuilder.setHardTimeout(0);
        flowBuilder.setIdleTimeout(0);

        if (action.equals(Action.ADD)) {
            writeFlow(flowBuilder, nodeBuilder);
        } else {
            removeFlow(flowBuilder, nodeBuilder);
        }

        // ToDo: WriteFlow/RemoveFlow should return something we can use to check success
        return new Status(StatusCode.SUCCESS);
    }
}
