/*
 * Copyright (C) 2013 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.ovsdb.utils.mdsal.openflow;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.EtherType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.VlanId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.ethernet.match.fields.EthernetDestinationBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.ethernet.match.fields.EthernetSourceBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.ethernet.match.fields.EthernetTypeBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.EthernetMatch;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.EthernetMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.Icmpv4MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.IpMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.TcpFlagMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.TunnelBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.VlanMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.Ipv4MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._4.match.TcpMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.vlan.match.fields.VlanIdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg0;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg5;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg6;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.general.rev140714.ExtensionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.general.rev140714.GeneralAugMatchNodesNodeTableFlow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.general.rev140714.GeneralAugMatchNodesNodeTableFlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.general.rev140714.general.extension.grouping.ExtensionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.general.rev140714.general.extension.list.grouping.ExtensionList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.general.rev140714.general.extension.list.grouping.ExtensionListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.NxAugMatchNodesNodeTableFlow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.NxAugMatchNodesNodeTableFlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.NxmNxReg0Key;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.NxmNxReg1Key;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.NxmNxReg2Key;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.NxmNxReg3Key;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.NxmNxReg4Key;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.NxmNxReg5Key;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.NxmNxReg6Key;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.NxmNxReg7Key;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.NxmNxTunIdKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.nxm.nx.reg.grouping.NxmNxRegBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.nxm.nx.tun.id.grouping.NxmNxTunIdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.ovs.nx.sal.match.rev140714.NxmNxNspKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.ovs.nx.sal.match.rev140714.nxm.nx.nsp.grouping.NxmNxNspBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.ovs.nx.sal.match.rev140714.NxmNxNsiKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.ovs.nx.sal.match.rev140714.nxm.nx.nsi.grouping.NxmNxNsiBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

public class MatchUtils {
    private static final Logger logger = LoggerFactory.getLogger(MatchUtils.class);

    /**
     * Create Ingress Port Match dpidLong, inPort
     *
     * @param matchBuilder Map matchBuilder MatchBuilder Object without a match
     * @param dpidLong     Long the datapath ID of a switch/node
     * @param inPort       Long ingress port on a switch
     * @return matchBuilder Map MatchBuilder Object with a match
     */
    public static MatchBuilder createInPortMatch(MatchBuilder matchBuilder, Long dpidLong, Long inPort) {

        NodeConnectorId ncid = new NodeConnectorId("openflow:" + dpidLong + ":" + inPort);
        logger.debug("createInPortMatch() Node Connector ID is - Type=openflow: DPID={} inPort={} ", dpidLong, inPort);
        matchBuilder.setInPort(NodeConnectorId.getDefaultInstance(ncid.getValue()));
        matchBuilder.setInPort(ncid);

        return matchBuilder;
    }

    /**
     * Create EtherType Match
     *
     * @param matchBuilder Map matchBuilder MatchBuilder Object without a match
     * @param etherType    Long EtherType
     * @return matchBuilder Map MatchBuilder Object with a match
     */
    public static MatchBuilder createEtherTypeMatch(MatchBuilder matchBuilder, EtherType etherType) {

        EthernetMatchBuilder ethernetMatch = new EthernetMatchBuilder();
        EthernetTypeBuilder ethTypeBuilder = new EthernetTypeBuilder();
        ethTypeBuilder.setType(new EtherType(etherType));
        ethernetMatch.setEthernetType(ethTypeBuilder.build());
        matchBuilder.setEthernetMatch(ethernetMatch.build());

        return matchBuilder;
    }

    /**
     * Create Ethernet Source Match
     *
     * @param matchBuilder MatchBuilder Object without a match yet
     * @param sMacAddr     String representing a source MAC
     * @return matchBuilder Map MatchBuilder Object with a match
     */
    public static MatchBuilder createEthSrcMatch(MatchBuilder matchBuilder, MacAddress sMacAddr) {

        EthernetMatchBuilder ethernetMatch = new EthernetMatchBuilder();
        EthernetSourceBuilder ethSourceBuilder = new EthernetSourceBuilder();
        ethSourceBuilder.setAddress(new MacAddress(sMacAddr));
        ethernetMatch.setEthernetSource(ethSourceBuilder.build());
        matchBuilder.setEthernetMatch(ethernetMatch.build());

        return matchBuilder;
    }

    /**
     * Create Ethernet Destination Match
     *
     * @param matchBuilder MatchBuilder Object without a match yet
     * @param vlanId       Integer representing a VLAN ID Integer representing a VLAN ID
     * @return matchBuilder Map MatchBuilder Object with a match
     */
    public static MatchBuilder createVlanIdMatch(MatchBuilder matchBuilder, VlanId vlanId) {
        VlanMatchBuilder vlanMatchBuilder = new VlanMatchBuilder();
        VlanIdBuilder vlanIdBuilder = new VlanIdBuilder();
        vlanIdBuilder.setVlanId(new VlanId(vlanId));
        vlanIdBuilder.setVlanIdPresent(true);
        vlanMatchBuilder.setVlanId(vlanIdBuilder.build());
        matchBuilder.setVlanMatch(vlanMatchBuilder.build());

        return matchBuilder;
    }

    /**
     * Create Ethernet Destination Match
     *
     * @param matchBuilder MatchBuilder Object without a match yet
     * @param dMacAddr     String representing a destination MAC
     * @return matchBuilder Map MatchBuilder Object with a match
     */
    public static MatchBuilder createDestEthMatch(MatchBuilder matchBuilder, MacAddress dMacAddr, MacAddress mask) {

        EthernetMatchBuilder ethernetMatch = new EthernetMatchBuilder();
        EthernetDestinationBuilder ethDestinationBuilder = new EthernetDestinationBuilder();
        ethDestinationBuilder.setAddress(new MacAddress(dMacAddr));
        if (mask != null) {
            ethDestinationBuilder.setMask(mask);
        }
        ethernetMatch.setEthernetDestination(ethDestinationBuilder.build());
        matchBuilder.setEthernetMatch(ethernetMatch.build());

        return matchBuilder;
    }

    /**
     * Tunnel ID Match Builder
     *
     * @param matchBuilder MatchBuilder Object without a match yet
     * @param tunnelId     BigInteger representing a tunnel ID
     * @return matchBuilder Map MatchBuilder Object with a match
     */
    public static MatchBuilder createTunnelIDMatch(MatchBuilder matchBuilder, BigInteger tunnelId) {

        TunnelBuilder tunnelBuilder = new TunnelBuilder();
        tunnelBuilder.setTunnelId(tunnelId);
        matchBuilder.setTunnel(tunnelBuilder.build());

        return matchBuilder;
    }

    /**
     * Match ICMP code and type
     *
     * @param matchBuilder MatchBuilder Object without a match yet
     * @param type         short representing an ICMP type
     * @param code         short representing an ICMP code
     * @return matchBuilder Map MatchBuilder Object with a match
     */
    public static MatchBuilder createICMPv4Match(MatchBuilder matchBuilder, short type, short code) {

        EthernetMatchBuilder eth = new EthernetMatchBuilder();
        EthernetTypeBuilder ethTypeBuilder = new EthernetTypeBuilder();
        ethTypeBuilder.setType(new EtherType(0x0800L));
        eth.setEthernetType(ethTypeBuilder.build());
        matchBuilder.setEthernetMatch(eth.build());

        // Build the IPv4 Match requied per OVS Syntax
        IpMatchBuilder ipmatch = new IpMatchBuilder();
        ipmatch.setIpProtocol((short) 1);
        matchBuilder.setIpMatch(ipmatch.build());

        // Build the ICMPv4 Match
        Icmpv4MatchBuilder icmpv4match = new Icmpv4MatchBuilder();
        icmpv4match.setIcmpv4Type(type);
        icmpv4match.setIcmpv4Code(code);
        matchBuilder.setIcmpv4Match(icmpv4match.build());

        return matchBuilder;
    }

    /**
     * @param matchBuilder MatchBuilder Object without a match yet
     * @param dstip        String containing an IPv4 prefix
     * @return matchBuilder Map Object with a match
     */
    public static MatchBuilder createDstL3IPv4Match(MatchBuilder matchBuilder, Ipv4Prefix dstip) {

        EthernetMatchBuilder eth = new EthernetMatchBuilder();
        EthernetTypeBuilder ethTypeBuilder = new EthernetTypeBuilder();
        ethTypeBuilder.setType(new EtherType(0x0800L));
        eth.setEthernetType(ethTypeBuilder.build());
        matchBuilder.setEthernetMatch(eth.build());

        Ipv4MatchBuilder ipv4match = new Ipv4MatchBuilder();
        ipv4match.setIpv4Destination(dstip);

        matchBuilder.setLayer3Match(ipv4match.build());

        return matchBuilder;

    }

    /**
     * @param matchBuilder MatchBuilder Object without a match yet
     * @param srcip        String containing an IPv4 prefix
     * @return matchBuilder Map Object with a match
     */
    public static MatchBuilder createSrcL3IPv4Match(MatchBuilder matchBuilder, Ipv4Prefix srcip) {

        EthernetMatchBuilder eth = new EthernetMatchBuilder();
        EthernetTypeBuilder ethTypeBuilder = new EthernetTypeBuilder();
        ethTypeBuilder.setType(new EtherType(0x0800L));
        eth.setEthernetType(ethTypeBuilder.build());
        matchBuilder.setEthernetMatch(eth.build());

        Ipv4MatchBuilder ipv4match = new Ipv4MatchBuilder();
        ipv4match.setIpv4Source(srcip);
        matchBuilder.setLayer3Match(ipv4match.build());

        return matchBuilder;

    }

    /**
     * Create Source TCP Port Match
     *
     * @param matchBuilder @param matchbuilder MatchBuilder Object without a match yet
     * @param tcpport      Integer representing a source TCP port
     * @return matchBuilder Map MatchBuilder Object with a match
     */
    public static MatchBuilder createSetSrcTcpMatch(MatchBuilder matchBuilder, PortNumber tcpport) {

        EthernetMatchBuilder ethType = new EthernetMatchBuilder();
        EthernetTypeBuilder ethTypeBuilder = new EthernetTypeBuilder();
        ethTypeBuilder.setType(new EtherType(0x0800L));
        ethType.setEthernetType(ethTypeBuilder.build());
        matchBuilder.setEthernetMatch(ethType.build());

        IpMatchBuilder ipmatch = new IpMatchBuilder();
        ipmatch.setIpProtocol((short) 6);
        matchBuilder.setIpMatch(ipmatch.build());

        TcpMatchBuilder tcpmatch = new TcpMatchBuilder();
        tcpmatch.setTcpSourcePort(tcpport);
        matchBuilder.setLayer4Match(tcpmatch.build());

        return matchBuilder;

    }

    /**
     * Create Destination TCP Port Match
     *
     * @param matchBuilder MatchBuilder Object without a match yet
     * @param tcpDstPort   Integer representing a destination TCP port
     * @return matchBuilder Map MatchBuilder Object with a match
     */
    public static MatchBuilder createSetDstTcpMatch(MatchBuilder matchBuilder, PortNumber tcpDstPort) {

        EthernetMatchBuilder ethType = new EthernetMatchBuilder();
        EthernetTypeBuilder ethTypeBuilder = new EthernetTypeBuilder();
        ethTypeBuilder.setType(new EtherType(0x0800L));
        ethType.setEthernetType(ethTypeBuilder.build());
        matchBuilder.setEthernetMatch(ethType.build());

        IpMatchBuilder ipmatch = new IpMatchBuilder();
        ipmatch.setIpProtocol((short) 6);
        matchBuilder.setIpMatch(ipmatch.build());

        TcpMatchBuilder tcpmatch = new TcpMatchBuilder();
        tcpmatch.setTcpDestinationPort(tcpDstPort);
        matchBuilder.setLayer4Match(tcpmatch.build());

        return matchBuilder;
    }

    /**
     * Test match for TCP_Flags
     *
     * @return match containing TCP_Flag (), IP Protocol (TCP), TCP_Flag (SYN)
     * <p/>
     * Defined TCP Flag values in OVS v2.1+
     * TCP_FIN 0x001 / TCP_SYN 0x002 / TCP_RST 0x004
     * TCP_PSH 0x008 / TCP_ACK 0x010 / TCP_URG 0x020
     * TCP_ECE 0x040 / TCP_CWR 0x080 / TCP_NS  0x100
     */
    public static MatchBuilder createTcpFlagMatch(MatchBuilder matchBuilder, int tcpFlag) {

        // Ethertype match
        EthernetMatchBuilder ethernetType = new EthernetMatchBuilder();
        EthernetTypeBuilder ethTypeBuilder = new EthernetTypeBuilder();
        ethTypeBuilder.setType(new EtherType(0x0800L));
        ethernetType.setEthernetType(ethTypeBuilder.build());
        matchBuilder.setEthernetMatch(ethernetType.build());

        // TCP Protocol Match
        IpMatchBuilder ipMatch = new IpMatchBuilder(); // ipv4 version
        ipMatch.setIpProtocol((short) 6);
        matchBuilder.setIpMatch(ipMatch.build());

        // TCP Port Match
        PortNumber dstPort = new PortNumber(80);
        TcpMatchBuilder tcpMatch = new TcpMatchBuilder();
        tcpMatch.setTcpDestinationPort(dstPort);
        matchBuilder.setLayer4Match(tcpMatch.build());

        TcpFlagMatchBuilder tcpFlagMatch = new TcpFlagMatchBuilder();
        tcpFlagMatch.setTcpFlag(0x002);
        matchBuilder.setTcpFlagMatch(tcpFlagMatch.build());
        return matchBuilder;
    }


    public static class RegMatch {
        final Class<? extends NxmNxReg> reg;
        final Long value;
        public RegMatch(Class<? extends NxmNxReg> reg, Long value) {
            super();
            this.reg = reg;
            this.value = value;
        }
        public static RegMatch of(Class<? extends NxmNxReg> reg, Long value) {
            return new RegMatch(reg, value);
        }
    }

    public static void addNxRegMatch(MatchBuilder match,
                                     RegMatch... matches) {
        ArrayList<ExtensionList> extensions = new ArrayList<>();
        for (RegMatch rm : matches) {
            Class<? extends ExtensionKey> key;
            if (NxmNxReg0.class.equals(rm.reg)) {
                key = NxmNxReg0Key.class;
            } else if (NxmNxReg1.class.equals(rm.reg)) {
                key = NxmNxReg1Key.class;
            } else if (NxmNxReg2.class.equals(rm.reg)) {
                key = NxmNxReg2Key.class;
            } else if (NxmNxReg3.class.equals(rm.reg)) {
                key = NxmNxReg3Key.class;
            } else if (NxmNxReg4.class.equals(rm.reg)) {
                key = NxmNxReg4Key.class;
            } else if (NxmNxReg5.class.equals(rm.reg)) {
                key = NxmNxReg5Key.class;
            } else if (NxmNxReg6.class.equals(rm.reg)) {
                key = NxmNxReg6Key.class;
            } else {
                key = NxmNxReg7Key.class;
            }
            NxAugMatchNodesNodeTableFlow am =
                    new NxAugMatchNodesNodeTableFlowBuilder()
                .setNxmNxReg(new NxmNxRegBuilder()
                    .setReg(rm.reg)
                    .setValue(rm.value)
                    .build())
                .build();
            extensions.add(new ExtensionListBuilder()
                .setExtensionKey(key)
                .setExtension(new ExtensionBuilder()
                     .addAugmentation(NxAugMatchNodesNodeTableFlow.class, am)
                     .build())
                .build());
        }
        GeneralAugMatchNodesNodeTableFlow m =
                new GeneralAugMatchNodesNodeTableFlowBuilder()
            .setExtensionList(extensions)
            .build();
        match.addAugmentation(GeneralAugMatchNodesNodeTableFlow.class, m);
    }

    public static void addNxTunIdMatch(MatchBuilder match,
                                       int tunId) {
        NxAugMatchNodesNodeTableFlow am =
               new NxAugMatchNodesNodeTableFlowBuilder()
                   .setNxmNxTunId(new NxmNxTunIdBuilder()
                       .setValue(BigInteger.valueOf(tunId))
                       .build())
                   .build();
        GeneralAugMatchNodesNodeTableFlow m =
                new GeneralAugMatchNodesNodeTableFlowBuilder()
            .setExtensionList(ImmutableList.of(new ExtensionListBuilder()
                .setExtensionKey(NxmNxTunIdKey.class)
                .setExtension(new ExtensionBuilder()
                    .addAugmentation(NxAugMatchNodesNodeTableFlow.class, am)
                    .build())
                .build()))
            .build();
        match.addAugmentation(GeneralAugMatchNodesNodeTableFlow.class, m);
    }

    public static void addNxNsp(MatchBuilder match, long nsp) {
        org.opendaylight.yang.gen.v1.urn.opendaylight.ovs.nx.sal.match.rev140714.NxAugMatchNodesNodeTableFlow am =
                new org.opendaylight.yang.gen.v1.urn.opendaylight.ovs.nx.sal.match.rev140714.NxAugMatchNodesNodeTableFlowBuilder()
                .setNxmNxNsp(new NxmNxNspBuilder()
                .setValue(nsp)
                .build())
                .build();
        addExtension(match, NxmNxNspKey.class, am);
    }

    public static void addNxNsi(MatchBuilder match, short nsi) {
        org.opendaylight.yang.gen.v1.urn.opendaylight.ovs.nx.sal.match.rev140714.NxAugMatchNodesNodeTableFlow am =
                new org.opendaylight.yang.gen.v1.urn.opendaylight.ovs.nx.sal.match.rev140714.NxAugMatchNodesNodeTableFlowBuilder()
                .setNxmNxNsi(new NxmNxNsiBuilder()
                .setNsi(nsi)
                .build())
                .build();
        addExtension(match, NxmNxNsiKey.class, am);
    }

    private static void addExtension (MatchBuilder match, Class<? extends ExtensionKey> extensionKey, org.opendaylight.yang.gen.v1.urn.opendaylight.ovs.nx.sal.match.rev140714.NxAugMatchNodesNodeTableFlow am) {
        GeneralAugMatchNodesNodeTableFlow existingAugmentations = match.getAugmentation(GeneralAugMatchNodesNodeTableFlow.class);
        List<ExtensionList> extensions = null;
        if (existingAugmentations != null ) {
            extensions = existingAugmentations.getExtensionList();
        }
        if (extensions == null) {
            extensions = Lists.newArrayList();
        }

        extensions.add(new ExtensionListBuilder()
                           .setExtensionKey(extensionKey)
                           .setExtension(new ExtensionBuilder()
                           .addAugmentation(org.opendaylight.yang.gen.v1.urn.opendaylight.ovs.nx.sal.match.rev140714.NxAugMatchNodesNodeTableFlow.class, am)
                           .build())
                           .build());

        GeneralAugMatchNodesNodeTableFlow m = new GeneralAugMatchNodesNodeTableFlowBuilder()
        .setExtensionList(extensions)
        .build();
        match.addAugmentation(GeneralAugMatchNodesNodeTableFlow.class, m);
    }

    public static EthernetMatch ethernetMatch(MacAddress srcMac,
                                              MacAddress dstMac,
                                              Long etherType) {
        EthernetMatchBuilder emb = new  EthernetMatchBuilder();
        if (srcMac != null)
            emb.setEthernetSource(new EthernetSourceBuilder()
                .setAddress(srcMac)
                .build());
        if (dstMac != null)
            emb.setEthernetDestination(new EthernetDestinationBuilder()
                .setAddress(dstMac)
                .build());
        if (etherType != null)
            emb.setEthernetType(new EthernetTypeBuilder()
                .setType(new EtherType(etherType))
                .build());
        return emb.build();
    }
}
