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
import org.opendaylight.ovsdb.utils.mdsal.openflow.MatchUtils;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg5;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstOfEthDstCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.src.choice.grouping.src.choice.SrcNxRegCaseBuilder;
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
    public static final Class<? extends NxmNxReg> SRC_MAC_4_HIGH_BYTES_FIELD = NxmNxReg4.class;
    public static final Class<? extends NxmNxReg> SRC_MAC_2_LOW_BYTES_FIELD = NxmNxReg5.class;

    public IcmpEchoResponderService() {
        super(Service.ICMP_ECHO);
    }

    public IcmpEchoResponderService(Service service) {
        super(service);
    }

    @Override
    public Status programIcmpEchoEntry(Long dpid, String segmentationId, String macAddressStr, InetAddress ipAddress, Action action) {

        if (segmentationId == null) return new Status(StatusCode.BADREQUEST);

        if (ipAddress instanceof Inet6Address) {
            // WORKAROUND: For now ipv6 is not supported
            // TODO: implement ipv6 case
            LOG.debug("ipv6 address case is not implemented yet. dpid {} segmentationId {} macAddressStr, ipAddress {} action {}",
                    dpid, segmentationId, macAddressStr, ipAddress, action);
            return new Status(StatusCode.NOTIMPLEMENTED);
        }

        String nodeName = Constants.OPENFLOW_NODE_PREFIX + dpid;
        MacAddress macAddress = new MacAddress(macAddressStr);

        programEntry(nodeName, segmentationId, macAddress, ipAddress, true, action);
        programEntry(nodeName, segmentationId, macAddress, ipAddress, false, action);

        // ToDo: WriteFlow/RemoveFlow should return something we can use to check success
        return new Status(StatusCode.SUCCESS);
    }

    private Status programEntry(String nodeName, String segmentationId, MacAddress macAddress, InetAddress ipAddress, boolean isRouted, Action action) {
        FlowBuilder flowBuilder = new FlowBuilder();
        String flowName = (isRouted ? "RoutedIcmpEchoResponder_" : "LanIcmpEchoResponder_")
                + segmentationId + "_" + ipAddress.getHostAddress();

        //The non-routed flow has an extra match condition and it must therefor be of a higher
        //prio to make sure we get a "best match" kind of logic between the two
        flowBuilder.setId(new FlowId(flowName));
        FlowKey key = new FlowKey(new FlowId(flowName));
        flowBuilder.setBarrier(true);
        flowBuilder.setTableId(this.getTable());
        flowBuilder.setKey(key);
        flowBuilder.setPriority(isRouted ? 2048 : 2049);
        flowBuilder.setFlowName(flowName);
        flowBuilder.setHardTimeout(0);
        flowBuilder.setIdleTimeout(0);

        NodeBuilder nodeBuilder = OF13Provider.createNodeBuilder(nodeName);

        MatchBuilder matchBuilder = new MatchBuilder();

        MatchUtils.createTunnelIDMatch(matchBuilder, new BigInteger(segmentationId));

        // Match ICMP echo requests, type=8, code=0
        MatchUtils.createICMPv4Match(matchBuilder, (short) 8, (short) 0);
        MatchUtils.createDstL3IPv4Match(matchBuilder, MatchUtils.iPv4PrefixFromIPv4Address(ipAddress.getHostAddress()));

        if (!isRouted) {
            //packets that have been "routed" in table 60 (DVR) will have their src MAC in nxm_nx_reg4 and nxm_nx_reg5
            //here we check that nxm_nx_reg4 is empty to check whether the packet has *not* been router since its
            //destination is on the same LAN
            MatchUtils.addNxRegMatch(matchBuilder, new MatchUtils.RegMatch(SRC_MAC_4_HIGH_BYTES_FIELD, 0x0L));
        }

        flowBuilder.setMatch(matchBuilder.build());

        if (action.equals(Action.ADD)) {
            // Instructions List Stores Individual Instructions
            InstructionsBuilder isb = new InstructionsBuilder();
            List<Instruction> instructions = Lists.newArrayList();
            InstructionBuilder ib = new InstructionBuilder();
            ApplyActionsBuilder aab = new ApplyActionsBuilder();
            ActionBuilder ab = new ActionBuilder();
            List<org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.list.Action> actionList = Lists.newArrayList();

            int order = 0;
            if (isRouted) {
                ab.setAction(
                            ActionUtils.nxMoveRegAction(
                            new SrcNxRegCaseBuilder().setNxReg(SRC_MAC_4_HIGH_BYTES_FIELD).build(),
                            new DstOfEthDstCaseBuilder().setOfEthDst(true).build(),
                            0, 0, 31, false));
                ab.setOrder(order);
                ab.setKey(new ActionKey(order));
                actionList.add(ab.build());
                ++order;

                ab.setAction(
                            ActionUtils.nxMoveRegAction(
                            new SrcNxRegCaseBuilder().setNxReg(SRC_MAC_2_LOW_BYTES_FIELD).build(),
                            new DstOfEthDstCaseBuilder().setOfEthDst(true).build(),
                            0, 32, 47, false));
                ab.setOrder(order);
                ab.setKey(new ActionKey(order));
                actionList.add(ab.build());
                ++order;
            } else {
                ab.setAction(ActionUtils.nxMoveEthSrcToEthDstAction());
                ab.setOrder(order);
                ab.setKey(new ActionKey(order));
                actionList.add(ab.build());
                ++order;
            }

            // Set Eth Src
            ab.setAction(ActionUtils.setDlSrcAction(new MacAddress(macAddress)));
            ab.setOrder(order);
            ab.setKey(new ActionKey(order));
            actionList.add(ab.build());
            ++order;

            // Move Ip Src to Ip Dst
            ab.setAction(ActionUtils.nxMoveIpSrcToIpDstAction());
            ab.setOrder(order);
            ab.setKey(new ActionKey(order));
            actionList.add(ab.build());
            ++order;

            // Set Ip Src
            ab.setAction(ActionUtils.setNwSrcAction(new Ipv4Builder().setIpv4Address(
                    MatchUtils.iPv4PrefixFromIPv4Address(ipAddress.getHostAddress())).build()));
            ab.setOrder(order);
            ab.setKey(new ActionKey(order));
            actionList.add(ab.build());
            ++order;

            // Set the ICMP type to 0 (echo reply)
            ab.setAction(ActionUtils.setIcmpTypeAction((byte)0));
            ab.setOrder(order);
            ab.setKey(new ActionKey(order));
            actionList.add(ab.build());
            ++order;

            // Output of InPort
            ab.setAction(ActionUtils.outputAction(new NodeConnectorId(nodeName + ":INPORT")));
            ab.setOrder(order);
            ab.setKey(new ActionKey(order));
            actionList.add(ab.build());

            // Create Apply Actions Instruction
            aab.setAction(actionList);
            ib.setInstruction(new ApplyActionsCaseBuilder().setApplyActions(aab.build()).build());
            ib.setOrder(0);
            ib.setKey(new InstructionKey(0));
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
        super.setDependencies(bundleContext.getServiceReference(IcmpEchoProvider.class.getName()), this);
    }

    @Override
    public void setDependencies(Object impl) {}
}
