package org.opendaylight.ovsdb.utils.mdsal.openflow;

import java.math.BigInteger;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Uri;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.DecNwTtlCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.DropActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.GroupActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.OutputActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetDlDstActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.SetDlSrcActionCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.dec.nw.ttl._case.DecNwTtlBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.drop.action._case.DropActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.group.action._case.GroupActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.output.action._case.OutputActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.dl.dst.action._case.SetDlDstActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.action.set.dl.src.action._case.SetDlSrcActionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.DstChoice;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstNxArpShaCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstNxArpThaCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstNxRegCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstNxTunIdCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstNxTunIpv4DstCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstOfArpOpCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstOfArpSpaCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstOfArpTpaCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstOfEthDstCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.group.buckets.bucket.action.action.NxActionRegLoadNodesNodeGroupBucketsBucketActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.group.buckets.bucket.action.action.NxActionRegMoveNodesNodeGroupBucketsBucketActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.apply.actions._case.apply.actions.action.action.NxActionOutputRegNodesNodeTableFlowApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.apply.actions._case.apply.actions.action.action.NxActionRegLoadNodesNodeTableFlowApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.apply.actions._case.apply.actions.action.action.NxActionRegMoveNodesNodeTableFlowApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.output.reg.grouping.NxOutputReg;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.output.reg.grouping.NxOutputRegBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.reg.load.grouping.NxRegLoad;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.reg.load.grouping.NxRegLoadBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.reg.load.grouping.nx.reg.load.DstBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.reg.move.grouping.NxRegMove;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.reg.move.grouping.NxRegMoveBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.reg.move.grouping.nx.reg.move.SrcBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.src.choice.grouping.SrcChoice;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.src.choice.grouping.src.choice.SrcNxArpShaCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.src.choice.grouping.src.choice.SrcNxRegCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.src.choice.grouping.src.choice.SrcOfArpSpaCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.src.choice.grouping.src.choice.SrcOfEthSrcCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.ovs.nx.sal.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.apply.actions._case.apply.actions.action.action.NxActionResubmitNodesNodeTableFlowApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.ovs.nx.sal.action.rev140714.nx.action.resubmit.grouping.NxResubmit;
import org.opendaylight.yang.gen.v1.urn.opendaylight.ovs.nx.sal.action.rev140714.nx.action.resubmit.grouping.NxResubmitBuilder;

import org.opendaylight.yang.gen.v1.urn.opendaylight.ovs.nx.sal.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.apply.actions._case.apply.actions.action.action.NxActionSetNspNodesNodeTableFlowApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.ovs.nx.sal.action.rev140714.nx.action.set.nsp.grouping.NxSetNsp;
import org.opendaylight.yang.gen.v1.urn.opendaylight.ovs.nx.sal.action.rev140714.nx.action.set.nsp.grouping.NxSetNspBuilder;

import org.opendaylight.yang.gen.v1.urn.opendaylight.ovs.nx.sal.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.apply.actions._case.apply.actions.action.action.NxActionSetNsiNodesNodeTableFlowApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.ovs.nx.sal.action.rev140714.nx.action.set.nsi.grouping.NxSetNsi;
import org.opendaylight.yang.gen.v1.urn.opendaylight.ovs.nx.sal.action.rev140714.nx.action.set.nsi.grouping.NxSetNsiBuilder;

import com.google.common.net.InetAddresses;

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

    public static Action decNwTtlAction() {
        return new DecNwTtlCaseBuilder()
            .setDecNwTtl(new DecNwTtlBuilder()
                .build())
            .build();
    }

    public static Action nxLoadRegAction(DstChoice dstChoice,
                                         BigInteger value,
                                         int endOffset,
                                         boolean groupBucket) {
        NxRegLoad r = new NxRegLoadBuilder()
            .setDst(new DstBuilder()
                .setDstChoice(dstChoice)
                .setStart(Integer.valueOf(0))
                .setEnd(Integer.valueOf(endOffset))
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

    public static Action nxMoveRegAction(SrcChoice srcChoice,
                                         DstChoice dstChoice,
                                         int endOffset,
                                         boolean groupBucket) {
        NxRegMove r = new NxRegMoveBuilder()
            .setSrc(new SrcBuilder()
                .setSrcChoice(srcChoice)
                .setStart(Integer.valueOf(0))
                .setEnd(Integer.valueOf(endOffset))
                .build())
            .setDst(new org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.reg.move.grouping.nx.reg.move.DstBuilder()
                .setDstChoice(dstChoice)
                .setStart(Integer.valueOf(0))
                .setEnd(Integer.valueOf(endOffset))
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
        return nxMoveRegAction(new SrcNxRegCaseBuilder()
                                    .setNxReg(src).build(),
                               new DstNxTunIdCaseBuilder()
                                   .setNxTunId(Boolean.TRUE).build(),
                               31,
                               groupBucket);
    }

    public static Action nxMoveArpShaToArpThaAction() {
        return nxMoveRegAction(new SrcNxArpShaCaseBuilder()
                                   .setNxArpSha(Boolean.TRUE).build(),
                               new DstNxArpThaCaseBuilder()
                                   .setNxArpTha(Boolean.TRUE).build(),
                               47, false);
    }

    public static Action nxMoveEthSrcToEthDstAction() {
        return nxMoveRegAction(new SrcOfEthSrcCaseBuilder()
                                   .setOfEthSrc(Boolean.TRUE).build(),
                               new DstOfEthDstCaseBuilder()
                                   .setOfEthDst(Boolean.TRUE).build(),
                               47, false);
    }

    public static Action nxMoveArpSpaToArpTpaAction() {
        return nxMoveRegAction(new SrcOfArpSpaCaseBuilder()
                                   .setOfArpSpa(Boolean.TRUE).build(),
                               new DstOfArpTpaCaseBuilder()
                                   .setOfArpTpa(Boolean.TRUE).build());
    }

    public static Action nxOutputRegAction(SrcChoice srcChoice) {
        NxOutputReg r = new NxOutputRegBuilder()
            .setSrc(new org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.output.reg.grouping.nx.output.reg.SrcBuilder()
                .setSrcChoice(srcChoice)
                .setOfsNbits(Integer.valueOf(31))
                .build())
            .setMaxLen(Integer.valueOf(0xffff))
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

    public static Action nxSetNsiAction(Short nsp) {
        NxSetNsiBuilder builder = new NxSetNsiBuilder();
        if (nsp != null) {
            builder.setNsi(nsp);
        }
        NxSetNsi r = builder.build();
        return new NxActionSetNsiNodesNodeTableFlowApplyActionsCaseBuilder().setNxSetNsi(r).build();
    }

}

