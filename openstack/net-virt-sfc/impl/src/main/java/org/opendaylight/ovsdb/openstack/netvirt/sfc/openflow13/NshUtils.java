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

    public static Long convertIpAddressToLong(Ipv4Address ipv4Address) {
        return (InetAddresses.coerceToInteger(InetAddresses.forString(ipv4Address.getValue()))) & 0xFFFFFFFFL;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "NshUtils [nshTunIpDst=" + nshTunIpDst + ", nshTunUdpPort=" + nshTunUdpPort + ", nshNsp=" + nshNsp
                + ", nshNsi=" + nshNsi + ", nshMetaC1=" + nshMetaC1 + ", nshMetaC2=" + nshMetaC2 + "]";
    }
}
