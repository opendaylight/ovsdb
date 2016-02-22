/*
 * Copyright (c) 2014 - 2016 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.utils.mdsal.openflow;

import com.google.common.net.InetAddresses;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.DecNwTtlCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.DropActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.GroupActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.OutputActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.PopVlanActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetDlDstActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetDlSrcActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetFieldCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetNwDstActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetNwSrcActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.dec.nw.ttl._case.DecNwTtlBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.drop.action._case.DropActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.group.action._case.GroupActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.output.action._case.OutputActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.pop.vlan.action._case.PopVlanActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.dl.dst.action._case.SetDlDstActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.dl.src.action._case.SetDlSrcActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.field._case.SetFieldBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.nw.dst.action._case.SetNwDstActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.nw.src.action._case.SetNwSrcActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.address.Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.Icmpv4MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.TunnelBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.action.rev140421.OfjNxHashFields;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.action.rev140421.OfjNxMpAlgorithm;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.DstChoice;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstNxArpShaCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstNxArpThaCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstNxNshc1CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstNxNshc2CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstNxRegCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstNxTunIdCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstNxTunIpv4DstCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstOfArpOpCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstOfArpSpaCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstOfArpTpaCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstOfEthDstCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstOfIpDstCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.group.buckets.bucket.action.action.NxActionRegLoadNodesNodeGroupBucketsBucketActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.group.buckets.bucket.action.action.NxActionRegMoveNodesNodeGroupBucketsBucketActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.apply.actions._case.apply.actions.action.action.NxActionConntrackNodesNodeTableFlowApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.apply.actions._case.apply.actions.action.action.NxActionMultipathNodesNodeTableFlowApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.apply.actions._case.apply.actions.action.action.NxActionOutputRegNodesNodeTableFlowApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.apply.actions._case.apply.actions.action.action.NxActionRegLoadNodesNodeTableFlowApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.apply.actions._case.apply.actions.action.action.NxActionRegMoveNodesNodeTableFlowApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.apply.actions._case.apply.actions.action.action.NxActionResubmitNodesNodeTableFlowApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.apply.actions._case.apply.actions.action.action.NxActionSetNshc1NodesNodeTableFlowApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.apply.actions._case.apply.actions.action.action.NxActionSetNshc2NodesNodeTableFlowApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.apply.actions._case.apply.actions.action.action.NxActionSetNshc3NodesNodeTableFlowApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.apply.actions._case.apply.actions.action.action.NxActionSetNshc4NodesNodeTableFlowApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.apply.actions._case.apply.actions.action.action.NxActionSetNsiNodesNodeTableFlowApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.apply.actions._case.apply.actions.action.action.NxActionSetNspNodesNodeTableFlowApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.conntrack.grouping.NxConntrack;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.conntrack.grouping.NxConntrackBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.multipath.grouping.NxMultipath;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.multipath.grouping.NxMultipathBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.output.reg.grouping.NxOutputReg;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.output.reg.grouping.NxOutputRegBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.reg.load.grouping.NxRegLoad;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.reg.load.grouping.NxRegLoadBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.reg.load.grouping.nx.reg.load.DstBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.reg.move.grouping.NxRegMove;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.reg.move.grouping.NxRegMoveBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.reg.move.grouping.nx.reg.move.SrcBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.resubmit.grouping.NxResubmit;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.resubmit.grouping.NxResubmitBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.set.nshc._1.grouping.NxSetNshc1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.set.nshc._1.grouping.NxSetNshc1Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.set.nshc._2.grouping.NxSetNshc2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.set.nshc._2.grouping.NxSetNshc2Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.set.nshc._3.grouping.NxSetNshc3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.set.nshc._3.grouping.NxSetNshc3Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.set.nshc._4.grouping.NxSetNshc4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.set.nshc._4.grouping.NxSetNshc4Builder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.set.nsi.grouping.NxSetNsi;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.set.nsi.grouping.NxSetNsiBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.set.nsp.grouping.NxSetNsp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.set.nsp.grouping.NxSetNspBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.src.choice.grouping.SrcChoice;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.src.choice.grouping.src.choice.SrcNxArpShaCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.src.choice.grouping.src.choice.SrcNxNshc1CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.src.choice.grouping.src.choice.SrcNxNshc2CaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.src.choice.grouping.src.choice.SrcNxRegCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.src.choice.grouping.src.choice.SrcNxTunIdCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.src.choice.grouping.src.choice.SrcNxTunIpv4DstCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.src.choice.grouping.src.choice.SrcOfArpSpaCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.src.choice.grouping.src.choice.SrcOfEthSrcCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.src.choice.grouping.src.choice.SrcOfIpSrcCaseBuilder;

import java.math.BigInteger;

public final class ActionUtils {
    public static Action dropAction() {
        return new DropActionCaseBuilder()
            .setDropAction(new DropActionBuilder()
                .build())
            .build();
    }

    public static Action outputAction(NodeConnectorId id) {
        return new OutputActionCaseBuilder()
            .setOutputAction(new OutputActionBuilder()
                .setOutputNodeConnector(new Uri(id.getValue()))
                .build())
            .build();
    }

    public static Action groupAction(Long id) {
        return new GroupActionCaseBuilder()
            .setGroupAction(new GroupActionBuilder()
                .setGroupId(id)
                .build())
            .build();
    }

    public static Action popVlanAction() {
        return new PopVlanActionCaseBuilder()
            .setPopVlanAction(new PopVlanActionBuilder()
                    .build())
            .build();
    }

    public static Action setDlSrcAction(MacAddress mac) {
        return new SetDlSrcActionCaseBuilder()
            .setSetDlSrcAction(new SetDlSrcActionBuilder()
                .setAddress(mac)
                .build())
            .build();
    }

    public static Action setDlDstAction(MacAddress mac) {
        return new SetDlDstActionCaseBuilder()
            .setSetDlDstAction(new SetDlDstActionBuilder()
                .setAddress(mac)
                .build())
            .build();
    }

    public static Action setNwSrcAction(Address ip) {
        return new SetNwSrcActionCaseBuilder()
            .setSetNwSrcAction(new SetNwSrcActionBuilder()
                .setAddress(ip)
                .build())
            .build();
    }

    public static Action setNwDstAction(Address ip) {
        return new SetNwDstActionCaseBuilder()
            .setSetNwDstAction(new SetNwDstActionBuilder()
                .setAddress(ip)
                .build())
            .build();
    }

    public static Action decNwTtlAction() {
        return new DecNwTtlCaseBuilder()
            .setDecNwTtl(new DecNwTtlBuilder()
                .build())
            .build();
    }

    public static Action setTunnelIdAction(BigInteger tunnelId) {

        SetFieldBuilder setFieldBuilder = new SetFieldBuilder();

        // Build the Set Tunnel Field Action
        TunnelBuilder tunnel = new TunnelBuilder();
        tunnel.setTunnelId(tunnelId);
        setFieldBuilder.setTunnel(tunnel.build());

        return new SetFieldCaseBuilder()
                .setSetField(setFieldBuilder.build())
                .build();
    }

    public static Action nxLoadRegAction(DstChoice dstChoice,
                                         BigInteger value,
                                         int endOffset,
                                         boolean groupBucket) {
        NxRegLoad r = new NxRegLoadBuilder()
            .setDst(new DstBuilder()
                .setDstChoice(dstChoice)
                .setStart(0)
                .setEnd(endOffset)
                .build())
            .setValue(value)
            .build();
        if (groupBucket) {
            return new NxActionRegLoadNodesNodeGroupBucketsBucketActionsCaseBuilder()
                .setNxRegLoad(r).build();
        } else {
            return new NxActionRegLoadNodesNodeTableFlowApplyActionsCaseBuilder()
                .setNxRegLoad(r).build();
        }
    }

    public static Action nxLoadRegAction(DstChoice dstChoice,
                                         BigInteger value) {
        return nxLoadRegAction(dstChoice, value, 31, false);
    }

    public static Action nxLoadRegAction(Class<? extends NxmNxReg> reg,
                                         BigInteger value) {
        return nxLoadRegAction(new DstNxRegCaseBuilder().setNxReg(reg).build(),
                               value);
    }

    public static Action nxLoadTunIPv4Action(String ipAddress,
                                             boolean groupBucket) {
        int ip = InetAddresses.coerceToInteger(InetAddresses.forString(ipAddress));
        long ipl = ip & 0xffffffffL;
        return nxLoadRegAction(new DstNxTunIpv4DstCaseBuilder()
                                    .setNxTunIpv4Dst(Boolean.TRUE).build(),
                               BigInteger.valueOf(ipl),
                               31,
                               groupBucket);
    }

    public static Action nxLoadArpOpAction(BigInteger value) {
        return nxLoadRegAction(new DstOfArpOpCaseBuilder()
            .setOfArpOp(Boolean.TRUE).build(), value, 15, false);
    }

    public static Action nxLoadArpShaAction(MacAddress macAddress) {
        return nxLoadArpShaAction(BigInteger.valueOf(toLong(macAddress)));
    }
    public static Action nxLoadArpShaAction(BigInteger value) {
        return nxLoadRegAction(new DstNxArpShaCaseBuilder()
            .setNxArpSha(Boolean.TRUE).build(), value, 47, false);
    }

    public static Action nxLoadArpSpaAction(BigInteger value) {
        return nxLoadRegAction(new DstOfArpSpaCaseBuilder()
            .setOfArpSpa(Boolean.TRUE).build(), value);
    }

    public static Action nxLoadArpSpaAction(String ipAddress) {
        int ip = InetAddresses.coerceToInteger(InetAddresses.forString(ipAddress));
        long ipl = ip & 0xffffffffL;
        return nxLoadArpSpaAction(BigInteger.valueOf(ipl));
    }

    public static Action setIcmpTypeAction(byte value) {
        SetFieldBuilder setFieldBuilder = new SetFieldBuilder();
        setFieldBuilder.setIcmpv4Match(new Icmpv4MatchBuilder().setIcmpv4Type((short)value).build());
        return new SetFieldCaseBuilder()
                .setSetField(setFieldBuilder.build())
                .build();
    }

    public static Action nxMoveRegAction(SrcChoice srcChoice,
                                         DstChoice dstChoice,
                                         int endOffset,
                                         boolean groupBucket) {
        return nxMoveRegAction(srcChoice, dstChoice, 0, 0, endOffset, groupBucket);
    }

    public static Action nxMoveRegAction(SrcChoice srcChoice,
                                         DstChoice dstChoice,
                                         int srcStartOffset,
                                         int dstStartOffset,
                                         int dstEndOffset,
                                         boolean groupBucket) {
        NxRegMove r = new NxRegMoveBuilder()
            .setSrc(new SrcBuilder()
                .setSrcChoice(srcChoice)
                .setStart(srcStartOffset)
                .build())
            .setDst(new org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.reg.move.grouping.nx.reg.move.DstBuilder()
                .setDstChoice(dstChoice)
                .setStart(dstStartOffset)
                .setEnd(dstEndOffset)
                .build())
            .build();
        if (groupBucket) {
            return new NxActionRegMoveNodesNodeGroupBucketsBucketActionsCaseBuilder()
                .setNxRegMove(r).build();
        } else {
            return new NxActionRegMoveNodesNodeTableFlowApplyActionsCaseBuilder()
                .setNxRegMove(r).build();
        }
    }

    public static Action nxMoveRegAction(SrcChoice srcChoice,
                                         DstChoice dstChoice) {
        return nxMoveRegAction(srcChoice, dstChoice, 31, false);
    }

    public static Action nxMoveRegTunIdAction(Class<? extends NxmNxReg> src,
                                              boolean groupBucket) {
        return nxMoveRegAction(new SrcNxRegCaseBuilder().setNxReg(src).build(),
                               new DstNxTunIdCaseBuilder().setNxTunId(Boolean.TRUE).build(),
                               31,
                               groupBucket);
    }

    public static Action nxMoveArpShaToArpThaAction() {
        return nxMoveRegAction(new SrcNxArpShaCaseBuilder().setNxArpSha(Boolean.TRUE).build(),
                               new DstNxArpThaCaseBuilder().setNxArpTha(Boolean.TRUE).build(),
                               47, false);
    }

    public static Action nxMoveEthSrcToEthDstAction() {
        return nxMoveRegAction(new SrcOfEthSrcCaseBuilder().setOfEthSrc(Boolean.TRUE).build(),
                               new DstOfEthDstCaseBuilder().setOfEthDst(Boolean.TRUE).build(),
                               47, false);
    }

    public static Action nxMoveArpSpaToArpTpaAction() {
        return nxMoveRegAction(new SrcOfArpSpaCaseBuilder().setOfArpSpa(Boolean.TRUE).build(),
                               new DstOfArpTpaCaseBuilder().setOfArpTpa(Boolean.TRUE).build());
    }

    public static Action nxMoveIpSrcToIpDstAction() {
        return nxMoveRegAction(new SrcOfIpSrcCaseBuilder().setOfIpSrc(Boolean.TRUE).build(),
                               new DstOfIpDstCaseBuilder().setOfIpDst(Boolean.TRUE).build());
    }


    public static Action nxOutputRegAction(SrcChoice srcChoice) {
        NxOutputReg r = new NxOutputRegBuilder()
            .setSrc(new org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.output.reg.grouping.nx.output.reg.SrcBuilder()
                .setSrcChoice(srcChoice)
                .setOfsNbits(31)
                .build())
            .setMaxLen(0xffff)
            .build();
        return new NxActionOutputRegNodesNodeTableFlowApplyActionsCaseBuilder()
            .setNxOutputReg(r).build();
    }

    public static Action nxOutputRegAction(Class<? extends NxmNxReg> reg) {
        return nxOutputRegAction(new SrcNxRegCaseBuilder().setNxReg(reg).build());
    }

    public static Action nxResubmitAction(Integer inPort, Short table) {
        NxResubmitBuilder builder = new NxResubmitBuilder();
        if (inPort != null) {
            builder.setInPort(inPort);
        }
        if (table != null) {
            builder.setTable(table);
        }
        NxResubmit r = builder.build();
        return new NxActionResubmitNodesNodeTableFlowApplyActionsCaseBuilder().setNxResubmit(r).build();
    }

    public static Action nxSetNspAction(Long nsp) {
        NxSetNspBuilder builder = new NxSetNspBuilder();
        if (nsp != null) {
            builder.setNsp(nsp);
        }
        NxSetNsp r = builder.build();
        return new NxActionSetNspNodesNodeTableFlowApplyActionsCaseBuilder().setNxSetNsp(r).build();
    }

    public static Action nxSetNsiAction(Short nsi) {
        NxSetNsiBuilder builder = new NxSetNsiBuilder();
        if (nsi != null) {
            builder.setNsi(nsi);
        }
        NxSetNsi r = builder.build();
        return new NxActionSetNsiNodesNodeTableFlowApplyActionsCaseBuilder().setNxSetNsi(r).build();
    }

    public static Action nxLoadNshc1RegAction(Long value) {
        NxSetNshc1 newNshc1 = new NxSetNshc1Builder().setNshc(value).build();
        return new NxActionSetNshc1NodesNodeTableFlowApplyActionsCaseBuilder().setNxSetNshc1(newNshc1).build();
    }

    public static Action nxLoadNshc2RegAction(Long value) {
        NxSetNshc2 newNshc2 = new NxSetNshc2Builder().setNshc(value).build();
        return new NxActionSetNshc2NodesNodeTableFlowApplyActionsCaseBuilder().setNxSetNshc2(newNshc2).build();
    }

    public static Action nxLoadNshc3RegAction(Long value) {
        NxSetNshc3 newNshc3 = new NxSetNshc3Builder().setNshc(value).build();
        return new NxActionSetNshc3NodesNodeTableFlowApplyActionsCaseBuilder().setNxSetNshc3(newNshc3).build();
    }

    public static Action nxLoadNshc4RegAction(Long value) {
        NxSetNshc4 newNshc4 = new NxSetNshc4Builder().setNshc(value).build();
        return new NxActionSetNshc4NodesNodeTableFlowApplyActionsCaseBuilder().setNxSetNshc4(newNshc4).build();
    }

    public static Action nxMoveRegTunDstToNshc1() {
        return nxMoveRegAction(
                new SrcNxTunIpv4DstCaseBuilder().setNxTunIpv4Dst(Boolean.TRUE).build(),
                new DstNxNshc1CaseBuilder().setNxNshc1Dst(Boolean.TRUE).build(),
                31, false);
    }

    public static Action nxMoveTunIdtoNshc2() {
        return nxMoveRegAction(
                new SrcNxTunIdCaseBuilder().setNxTunId(Boolean.TRUE).build(),
                new DstNxNshc2CaseBuilder().setNxNshc2Dst(Boolean.TRUE).build(),
                31, false);
    }

    public static Action nxMoveNshc1ToTunIpv4Dst() {
        return nxMoveRegAction(
                new SrcNxNshc1CaseBuilder().setNxNshc1Dst(Boolean.TRUE).build(),
                new DstNxTunIpv4DstCaseBuilder().setNxTunIpv4Dst(Boolean.TRUE).build(),
                31, false);
    }

    public static Action nxMoveNshc2ToTunId() {
        return nxMoveRegAction(
                new SrcNxNshc2CaseBuilder().setNxNshc2Dst(Boolean.TRUE).build(),
                new DstNxTunIdCaseBuilder().setNxTunId(Boolean.TRUE).build(),
                31, false);
    }

    public static Action nxLoadTunIdAction(BigInteger tunnelId, boolean groupBucket) {
        return nxLoadRegAction(new DstNxTunIdCaseBuilder().setNxTunId(Boolean.TRUE).build(), tunnelId, 31, groupBucket);
    }

    public static Action nxMultipathAction(OfjNxHashFields fields, Integer basis,
            OfjNxMpAlgorithm algorithm, Integer maxLink, Long arg, DstChoice dstChoice,
            Integer start, Integer end) {
        NxMultipath r = new NxMultipathBuilder()
            .setFields(fields)
            .setBasis(basis)
            .setAlgorithm(algorithm)
            .setMaxLink(maxLink)
            .setArg(arg)
            .setDst(new org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.multipath.grouping.nx.multipath.DstBuilder()
                    .setDstChoice(dstChoice)
                .setStart(start)
                .setEnd(end)
                .build())
            .build();
        return new NxActionMultipathNodesNodeTableFlowApplyActionsCaseBuilder().setNxMultipath(r).build();
    }

    public static Action nxConntrackAction(Integer flags, Long zoneSrc,
                                           Integer conntrackZone, Short recircTable) {
        NxConntrack r = new NxConntrackBuilder()
            .setFlags(flags)
            .setZoneSrc(zoneSrc)
            .setConntrackZone(conntrackZone)
            .setRecircTable(recircTable)
            .build();
        return new NxActionConntrackNodesNodeTableFlowApplyActionsCaseBuilder().setNxConntrack(r).build();
    }

    /**
     * Accepts a MAC address and returns the corresponding long, where the
     * MAC bytes are set on the lower order bytes of the long.
     * @param macAddress
     * @return a long containing the mac address bytes
     */
    public static long toLong(byte[] macAddress) {
        long mac = 0;
        for (int i = 0; i < 6; i++) {
            long t = (macAddress[i] & 0xffL) << ((5-i)*8);
            mac |= t;
        }
        return mac;
    }

    /**
     * Accepts a MAC address of the form 00:aa:11:bb:22:cc, case does not
     * matter, and returns the corresponding long, where the MAC bytes are set
     * on the lower order bytes of the long.
     *
     * @param macAddress
     *            in String format
     * @return a long containing the mac address bytes
     */
    public static long toLong(MacAddress macAddress) {
        return toLong(toMACAddress(macAddress.getValue()));
    }

    /**
     * Accepts a MAC address of the form 00:aa:11:bb:22:cc, case does not
     * matter, and returns a corresponding byte[].
     * @param macAddress
     * @return
     */
    public static byte[] toMACAddress(String macAddress) {
        final String HEXES = "0123456789ABCDEF";
        byte[] address = new byte[6];
        String[] macBytes = macAddress.split(":");
        if (macBytes.length != 6) {
            throw new IllegalArgumentException(
                    "Specified MAC Address must contain 12 hex digits" +
                    " separated pairwise by :'s.");
        }
        for (int i = 0; i < 6; ++i) {
            address[i] = (byte) ((HEXES.indexOf(macBytes[i].toUpperCase()
                                                        .charAt(0)) << 4) | HEXES.indexOf(macBytes[i].toUpperCase()
                                                                                                  .charAt(1)));
        }
        return address;
    }
}
