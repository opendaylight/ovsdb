/*
 * Copyright (c) 2016 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.services;

import com.google.common.collect.Lists;
import org.opendaylight.ovsdb.openstack.netvirt.api.*;
import org.opendaylight.ovsdb.openstack.netvirt.providers.ConfigInterface;
import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.AbstractServiceInstance;
import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.OF13Provider;
import org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.Service;
import org.opendaylight.ovsdb.utils.mdsal.openflow.ActionUtils;
import org.opendaylight.ovsdb.utils.mdsal.openflow.FlowUtils;
import org.opendaylight.ovsdb.utils.mdsal.openflow.MatchUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.ActionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.address.address.Ipv4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.InstructionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.ApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.instruction.apply.actions._case.ApplyActionsBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.Instruction;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.instruction.list.InstructionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.List;

/**
 * @author Josh Hershberg (jhershbe@redhat.com)
 */
public class IcmpEchoResponderService extends AbstractServiceInstance implements IcmpEchoProvider, ConfigInterface {
    private static final Logger LOG = LoggerFactory.getLogger(IcmpEchoResponderService.class);

    public IcmpEchoResponderService() {
        super(Service.ICMP_ECHO);
    }

    public IcmpEchoResponderService(Service service) {
        super(service);
    }

    @Override
    public Status programIcmpEchoEntry(Long dpid, String segmentationId, String macAddressStr, InetAddress ipAddress, Action action) {
        String nodeName = Constants.OPENFLOW_NODE_PREFIX + dpid;
        MacAddress macAddress = new MacAddress(macAddressStr);

        MatchBuilder matchBuilder = new MatchBuilder();
        NodeBuilder nodeBuilder = OF13Provider.createNodeBuilder(nodeName);

        // Instructions List Stores Individual Instructions
        InstructionsBuilder isb = new InstructionsBuilder();
        List<Instruction> instructions = Lists.newArrayList();
        InstructionBuilder ib = new InstructionBuilder();
        ApplyActionsBuilder aab = new ApplyActionsBuilder();
        ActionBuilder ab = new ActionBuilder();
        List<org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action> actionList = Lists.newArrayList();

        if (segmentationId != null) {
            final Long inPort = MatchUtils.parseExplicitOFPort(segmentationId);
            if (inPort != null) {
                MatchUtils.createInPortMatch(matchBuilder, dpid, inPort);
            } else {
                MatchUtils.createTunnelIDMatch(matchBuilder, new BigInteger(segmentationId));
            }
        }

        if (ipAddress instanceof Inet6Address) {
            // WORKAROUND: For now ipv6 is not supported
            // TODO: implement ipv6 case
            LOG.debug("ipv6 address case is not implemented yet. dpid {} segmentationId {} macAddressStr, ipAddress {} action {}",
                    dpid, segmentationId, macAddressStr, ipAddress, action);
            return new Status(StatusCode.NOTIMPLEMENTED);
        }

        // Match ICMP echo requests, type=8, code=0
        MatchUtils.createICMPv4Match(matchBuilder, (short)8, (short)0);
        MatchUtils.createDstL3IPv4Match(matchBuilder, MatchUtils.iPv4PrefixFromIPv4Address(ipAddress.getHostAddress()));

        // Move Eth Src to Eth Dst
        ab.setAction(ActionUtils.nxMoveEthSrcToEthDstAction());
        ab.setOrder(0);
        ab.setKey(new ActionKey(0));
        actionList.add(ab.build());

        // Set Eth Src
        ab.setAction(ActionUtils.setDlSrcAction(new MacAddress(macAddress)));
        ab.setOrder(1);
        ab.setKey(new ActionKey(1));
        actionList.add(ab.build());

        // Move Ip Src to Ip Dst
        ab.setAction(ActionUtils.nxMoveIpSrcToIpDstAction());
        ab.setOrder(2);
        ab.setKey(new ActionKey(2));
        actionList.add(ab.build());

        // Set Ip Src
        ab.setAction(ActionUtils.setNwSrcAction(new Ipv4Builder().setIpv4Address(
                                    MatchUtils.iPv4PrefixFromIPv4Address(ipAddress.getHostAddress())).build()));
        ab.setOrder(3);
        ab.setKey(new ActionKey(3));
        actionList.add(ab.build());

        // Set the ICMP type to 0 (echo reply)
        ab.setAction(ActionUtils.setIcmpTypeAction((byte)0));
        ab.setOrder(4);
        ab.setKey(new ActionKey(4));
        actionList.add(ab.build());

        // Output of InPort
        ab.setAction(ActionUtils.outputAction(new NodeConnectorId(nodeName + ":INPORT")));
        ab.setOrder(5);
        ab.setKey(new ActionKey(5));
        actionList.add(ab.build());

        // Create Apply Actions Instruction
        aab.setAction(actionList);
        ib.setInstruction(new ApplyActionsCaseBuilder().setApplyActions(aab.build()).build());
        ib.setOrder(0);
        ib.setKey(new InstructionKey(0));
        instructions.add(ib.build());

        FlowBuilder flowBuilder = new FlowBuilder();
        String flowName = "IcmpEchoResponder_" + segmentationId + "_" + ipAddress.getHostAddress();
        FlowUtils.initFlowBuilder(flowBuilder, flowName, getTable()).setPriority(1024);
        flowBuilder.setMatch(matchBuilder.build());
        flowBuilder.setInstructions(isb.setInstruction(instructions).build());

        if (action.equals(Action.ADD)) {
            writeFlow(flowBuilder, nodeBuilder);
        } else {
            removeFlow(flowBuilder, nodeBuilder);
        }

        // ToDo: WriteFlow/RemoveFlow should return something we can use to check success
        return new Status(StatusCode.SUCCESS);
    }

    @Override
    public void setDependencies(BundleContext bundleContext, ServiceReference serviceReference) {
        super.setDependencies(bundleContext.getServiceReference(IcmpEchoProvider.class.getName()), this);
    }

    @Override
    public void setDependencies(Object impl) {}
}
