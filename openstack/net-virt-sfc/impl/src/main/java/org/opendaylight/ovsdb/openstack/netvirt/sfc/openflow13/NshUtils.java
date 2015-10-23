/*
 * Copyright (c) 2015 Dell, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt.sfc.openflow13;

import java.math.BigInteger;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg;

import com.google.common.net.InetAddresses;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.action.types.rev131112.action.Action;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.DstChoice;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstNxRegCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstNxTunIdCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.dst.choice.grouping.dst.choice.DstNxTunIpv4DstCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.group.buckets.bucket.action.action.NxActionRegLoadNodesNodeGroupBucketsBucketActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.apply.actions._case.apply.actions.action.action.NxActionOutputRegNodesNodeTableFlowApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.apply.actions._case.apply.actions.action.action.NxActionRegLoadNodesNodeTableFlowApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.apply.actions._case.apply.actions.action.action.NxActionSetNshc1NodesNodeTableFlowApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.apply.actions._case.apply.actions.action.action.NxActionSetNshc2NodesNodeTableFlowApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.apply.actions._case.apply.actions.action.action.NxActionSetNshc3NodesNodeTableFlowApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.apply.actions._case.apply.actions.action.action.NxActionSetNshc4NodesNodeTableFlowApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.apply.actions._case.apply.actions.action.action.NxActionSetNsiNodesNodeTableFlowApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nodes.node.table.flow.instructions.instruction.instruction.apply.actions._case.apply.actions.action.action.NxActionSetNspNodesNodeTableFlowApplyActionsCaseBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.output.reg.grouping.NxOutputReg;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.output.reg.grouping.NxOutputRegBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.reg.load.grouping.NxRegLoad;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.reg.load.grouping.NxRegLoadBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.nx.action.reg.load.grouping.nx.reg.load.DstBuilder;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.action.rev140714.src.choice.grouping.src.choice.SrcNxRegCaseBuilder;

/**
 * Open Vswitch DB OpenFlow 1.3 Networking Provider for Netvirt SFC Utilities.
 * @author Arun Yerra
 */
public class NshUtils {
    private Ipv4Address nshTunIpDst;
    private PortNumber nshTunUdpPort;
    private long nshNsp;
    private short nshNsi;
    private long nshMetaC1;
    private long nshMetaC2;

    public NshUtils() {
        super();
    }

    /**
     * {@link NshUtils} constructor.
     * @param nshTunIpDst Tunnel Destination IP
     * @param nshTunUdpPort Tunnel Transport Port
     * @param nshNsp Service Path Id
     * @param nshNsi Service Path Index
     * @param nshMetaC1 End point ID
     * @param nshMetaC2 Tunnel Id.
     */ 
    public NshUtils(Ipv4Address nshTunIpDst, PortNumber nshTunUdpPort,
            long nshNsp, short nshNsi, long nshMetaC1,
            long nshMetaC2) {
        super();
        this.nshTunIpDst = nshTunIpDst;
        this.nshTunUdpPort = nshTunUdpPort;
        this.nshNsp = nshNsp;
        this.nshNsi = nshNsi;
        this.nshMetaC1 = nshMetaC1;
        this.nshMetaC2 = nshMetaC2;
    }

    /*
     * @return the nshTunIpDst
     */
    public Ipv4Address getNshTunIpDst() {
        return nshTunIpDst;
    }

    /*
     * @param nshTunIpDst the nshTunIpDst to set
     */
    public void setNshTunIpDst(Ipv4Address nshTunIpDst) {
        this.nshTunIpDst = nshTunIpDst;
    }

    /*
     * @return the nshTunUdpPort
     */
    public PortNumber getNshTunUdpPort() {
        return nshTunUdpPort;
    }

    /*
     * @param nshTunUdpPort the nshTunUdpPort to set
     */
    public void setNshTunUdpPort(PortNumber nshTunUdpPort) {
        this.nshTunUdpPort = nshTunUdpPort;
    }

    /*
     * @return the nshNsp
     */
    public long getNshNsp() {
        return nshNsp;
    }

    /*
     * @param nshNsp the nshNsp to set
     */
    public void setNshNsp(long nshNsp) {
        this.nshNsp = nshNsp;
    }

    /*
     * @return the nshNsi
     */
    public short getNshNsi() {
        return nshNsi;
    }

    /*
     * @param nshNsi the nshNsi to set
     */
    public void setNshNsi(short nshNsi) {
        this.nshNsi = nshNsi;
    }

    /*
     * @return the nshMetaC1
     */
    public long getNshMetaC1() {
        return nshMetaC1;
    }

    /*
     * @param nshMetaC1 the nshMetaC1 to set
     */
    public void setNshMetaC1(long nshMetaC1) {
        this.nshMetaC1 = nshMetaC1;
    }

    /*
     * @return the nshMetaC2
     */
    public long getNshMetaC2() {
        return nshMetaC2;
    }

    /*
     * @param nshMetaC2 the nshMetaC2 to set
     */
    public void setNshMetaC2(long nshMetaC2) {
        this.nshMetaC2 = nshMetaC2;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "NshUtils [nshTunIpDst=" + nshTunIpDst + ", nshTunUdpPort=" + nshTunUdpPort + ", nshNsp=" + nshNsp
                + ", nshNsi=" + nshNsi + ", nshMetaC1=" + nshMetaC1 + ", nshMetaC2=" + nshMetaC2 + "]";
    }

    /**
     * This method loads the action into NX register. 
     *{@link NshUtils} Loading Register 
     * @param dstChoice destination
     * @param value value
     * @param endOffset Offset
     * @param groupBucket Identifies the group
     */
    public static Action nxLoadRegAction(DstChoice dstChoice, BigInteger value, int endOffset, boolean groupBucket) {
        NxRegLoad reg = new NxRegLoadBuilder().setDst(
                new DstBuilder().setDstChoice(dstChoice)
                    .setStart(Integer.valueOf(0))
                    .setEnd(Integer.valueOf(endOffset))
                    .build())
            .setValue(value)
            .build();
        if (groupBucket) {
            return new NxActionRegLoadNodesNodeGroupBucketsBucketActionsCaseBuilder().setNxRegLoad(reg).build();
        } else {
            return new NxActionRegLoadNodesNodeTableFlowApplyActionsCaseBuilder().setNxRegLoad(reg).build();
        }
    }

    public static Action nxLoadRegAction(DstChoice dstChoice, BigInteger value) {
        return nxLoadRegAction(dstChoice, value, 31, false);
    }

    public static Action nxLoadRegAction(Class<? extends NxmNxReg> reg, BigInteger value) {
        return nxLoadRegAction(new DstNxRegCaseBuilder().setNxReg(reg).build(), value);
    }

    public static Action nxSetNsiAction(Short nsi) {
        NxSetNsi newNsi = new NxSetNsiBuilder().setNsi(nsi).build();
        return new NxActionSetNsiNodesNodeTableFlowApplyActionsCaseBuilder().setNxSetNsi(newNsi).build();
    }

    public static Action nxSetNspAction(Long nsp) {
        NxSetNsp newNsp = new NxSetNspBuilder().setNsp(nsp).build();
        return new NxActionSetNspNodesNodeTableFlowApplyActionsCaseBuilder().setNxSetNsp(newNsp).build();
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

    /**
     * This method loads Destination IPv4 address of Tunnel.
     */ 
    public static Action nxLoadTunIPv4Action(String ipAddress, boolean groupBucket) {
        int ip = InetAddresses.coerceToInteger(InetAddresses.forString(ipAddress));
        long ipl = ip & 0xffffffffL;
        return nxLoadRegAction(new DstNxTunIpv4DstCaseBuilder().setNxTunIpv4Dst(Boolean.TRUE).build(),
                BigInteger.valueOf(ipl), 31, groupBucket);
    }

    public static Action nxLoadTunIdAction(BigInteger tunnelId, boolean groupBucket) {
        return nxLoadRegAction(new DstNxTunIdCaseBuilder().setNxTunId(Boolean.TRUE).build(), tunnelId, 31, groupBucket);
    }

    /**
     * This method loads output port.
     */ 
    public static Action nxOutputRegAction(SrcChoice srcChoice) {
        NxOutputReg reg = new NxOutputRegBuilder().setSrc(
                new org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension
                    .nicira.action.rev140714.nx.action.output.reg.grouping.nx.output.reg
                    .SrcBuilder().setSrcChoice(srcChoice)
                    .setOfsNbits(Integer.valueOf(31))
                    .build())
            .setMaxLen(Integer.valueOf(0xffff))
            .build();
        return new NxActionOutputRegNodesNodeTableFlowApplyActionsCaseBuilder().setNxOutputReg(reg).build();
    }

    public static Action nxOutputRegAction(Class<? extends NxmNxReg> reg) {
        return nxOutputRegAction(new SrcNxRegCaseBuilder().setNxReg(reg).build());
    }
}
