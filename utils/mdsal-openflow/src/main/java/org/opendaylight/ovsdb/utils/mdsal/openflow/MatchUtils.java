/*
 * Copyright (c) 2013, 2015 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.utils.mdsal.openflow;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.MetadataBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.TcpFlagMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.TunnelBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.VlanMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.ArpMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.Ipv4MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._4.match.TcpMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._4.match.UdpMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.vlan.match.fields.VlanIdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg0;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg1;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg2;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg3;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg4;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg5;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmNxReg6;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowjava.nx.match.rev140421.NxmOfEthDst;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.general.rev140714.ExtensionKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.general.rev140714.GeneralAugMatchNodesNodeTableFlow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.general.rev140714.GeneralAugMatchNodesNodeTableFlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.general.rev140714.general.extension.grouping.ExtensionBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.general.rev140714.general.extension.list.grouping.ExtensionList;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.general.rev140714.general.extension.list.grouping.ExtensionListBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.NxAugMatchNodesNodeTableFlow;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.NxAugMatchNodesNodeTableFlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.NxmNxCtStateKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.NxmNxCtZoneKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.NxmNxReg0Key;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.NxmNxReg1Key;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.NxmNxReg2Key;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.NxmNxReg3Key;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.NxmNxReg4Key;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.NxmNxReg5Key;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.NxmNxReg6Key;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.NxmNxReg7Key;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.NxmNxTunIdKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.NxmOfTcpDstKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.NxmOfTcpSrcKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.NxmOfUdpDstKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.NxmOfUdpSrcKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.nxm.nx.ct.state.grouping.NxmNxCtStateBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.nxm.nx.ct.zone.grouping.NxmNxCtZoneBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.nxm.nx.reg.grouping.NxmNxRegBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.nxm.nx.tun.id.grouping.NxmNxTunIdBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.NxmNxNspKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.nxm.nx.nsp.grouping.NxmNxNspBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.NxmNxNsiKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.nxm.nx.nsi.grouping.NxmNxNsiBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.nxm.of.eth.dst.grouping.NxmOfEthDstBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.nxm.of.tcp.src.grouping.NxmOfTcpSrcBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.nxm.of.tcp.dst.grouping.NxmOfTcpDstBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.nxm.of.udp.dst.grouping.NxmOfUdpDstBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.openflowplugin.extension.nicira.match.rev140714.nxm.of.udp.src.grouping.NxmOfUdpSrcBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

public class MatchUtils {
    private static final Logger LOG = LoggerFactory.getLogger(MatchUtils.class);
    public static final short ICMP_SHORT = 1;
    public static final short TCP_SHORT = 6;
    public static final short UDP_SHORT = 17;
    public static final String TCP = "tcp";
    public static final String UDP = "udp";
    private static final int TCP_SYN = 0x0002;
    public static final String ICMP = "icmp";
    public static final short ALL_ICMP = -1;

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
        LOG.debug("createInPortMatch() Node Connector ID is - Type=openflow: DPID={} inPort={} ", dpidLong, inPort);
        matchBuilder.setInPort(NodeConnectorId.getDefaultInstance(ncid.getValue()));
        matchBuilder.setInPort(ncid);

        return matchBuilder;
    }

    public static MatchBuilder createInPortReservedMatch(MatchBuilder matchBuilder, Long dpidLong, String inPort) {

        NodeConnectorId ncid = new NodeConnectorId("openflow:" + dpidLong + ":" + inPort);
        LOG.debug("createInPortResrevedMatch() Node Connector ID is - Type=openflow: DPID={} inPort={} ",
                dpidLong, inPort);
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
    public static MatchBuilder createVlanIdMatch(MatchBuilder matchBuilder, VlanId vlanId, boolean present) {
        VlanMatchBuilder vlanMatchBuilder = new VlanMatchBuilder();
        VlanIdBuilder vlanIdBuilder = new VlanIdBuilder();
        vlanIdBuilder.setVlanId(new VlanId(vlanId));
        vlanIdBuilder.setVlanIdPresent(present);
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
     * @param matchBuilder MatchBuilder Object
     * @param type         short representing an ICMP type
     * @param code         short representing an ICMP code
     * @return matchBuilder Map MatchBuilder Object with a match
     */
    public static MatchBuilder createICMPv4Match(MatchBuilder matchBuilder, short type, short code) {

        // Build the IPv4 Match requied per OVS Syntax
        IpMatchBuilder ipmatch = new IpMatchBuilder();
        ipmatch.setIpProtocol((short) 1);
        matchBuilder.setIpMatch(ipmatch.build());

        // Build the ICMPv4 Match
        Icmpv4MatchBuilder icmpv4match = new Icmpv4MatchBuilder();
        if (type != ALL_ICMP || code != ALL_ICMP) {
            icmpv4match.setIcmpv4Type(type);
            icmpv4match.setIcmpv4Code(code);
        }
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
     * @param dstip        String containing an IPv4 prefix
     * @return matchBuilder Map Object with a match
     */
    public static MatchBuilder createArpDstIpv4Match(MatchBuilder matchBuilder, Ipv4Prefix dstip) {
        ArpMatchBuilder arpDstMatch = new ArpMatchBuilder();
        arpDstMatch.setArpTargetTransportAddress(dstip)
                .setArpOp(FlowUtils.ARP_OP_REQUEST);
        matchBuilder.setLayer3Match(arpDstMatch.build());

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
     * @param matchBuilder MatchBuilder Object without a match yet
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
     * @param matchBuilder MatchBuilder Object without a match yet
     * @param tcpPort  PortNumber representing a destination TCP port
     * @param tcpFlag  int representing a tcp_flag
     * @return match containing TCP_Flag (), IP Protocol (TCP), TCP_Flag (SYN)
     * <p>
     * Defined TCP Flag values in OVS v2.1+
     * TCP_FIN 0x001 / TCP_SYN 0x002 / TCP_RST 0x004
     * TCP_PSH 0x008 / TCP_ACK 0x010 / TCP_URG 0x020
     * TCP_ECE 0x040 / TCP_CWR 0x080 / TCP_NS  0x100
     */
    public static MatchBuilder createTcpFlagMatch(MatchBuilder matchBuilder, PortNumber tcpPort, int tcpFlag) {

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
        PortNumber dstPort = new PortNumber(tcpPort);
        TcpMatchBuilder tcpMatch = new TcpMatchBuilder();
        tcpMatch.setTcpDestinationPort(dstPort);
        matchBuilder.setLayer4Match(tcpMatch.build());

        TcpFlagMatchBuilder tcpFlagMatch = new TcpFlagMatchBuilder();
        tcpFlagMatch.setTcpFlag(tcpFlag);
        matchBuilder.setTcpFlagMatch(tcpFlagMatch.build());
        return matchBuilder;
    }

    /**
     * @return MatchBuilder containing the metadata match values
     */
    public static MatchBuilder createMetadataMatch(MatchBuilder matchBuilder, BigInteger metaData,  BigInteger metaDataMask) {

        // metadata matchbuilder
        MetadataBuilder metadata = new MetadataBuilder();
        metadata.setMetadata(metaData);
        // Optional metadata mask
        if (metaDataMask != null) {
            metadata.setMetadataMask(metaDataMask);
        }
        matchBuilder.setMetadata(metadata.build());

        return matchBuilder;
    }

    /**
     * Create  TCP Port Match
     *
     * @param matchBuilder MatchBuilder Object without a match yet
     * @param ipProtocol   Integer representing the IP protocol
     * @return matchBuilder Map MatchBuilder Object with a match
     */
    public static MatchBuilder createIpProtocolMatch(MatchBuilder matchBuilder, short ipProtocol) {

        EthernetMatchBuilder ethType = new EthernetMatchBuilder();
        EthernetTypeBuilder ethTypeBuilder = new EthernetTypeBuilder();
        ethTypeBuilder.setType(new EtherType(0x0800L));
        ethType.setEthernetType(ethTypeBuilder.build());
        matchBuilder.setEthernetMatch(ethType.build());

        IpMatchBuilder ipMmatch = new IpMatchBuilder();
        if (ipProtocol == TCP_SHORT) {
            ipMmatch.setIpProtocol(TCP_SHORT);
        }
        else if (ipProtocol == UDP_SHORT) {
            ipMmatch.setIpProtocol(UDP_SHORT);
        }
        else if (ipProtocol == ICMP_SHORT) {
            ipMmatch.setIpProtocol(ICMP_SHORT);
        }
        matchBuilder.setIpMatch(ipMmatch.build());
        return matchBuilder;
    }

    /**
     * Create tcp syn with proto match.
     *
     * @param matchBuilder the match builder
     * @return matchBuilder match builder
     */
    public static MatchBuilder createTcpSynWithProtoMatch(MatchBuilder matchBuilder) {

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

        TcpFlagMatchBuilder tcpFlagMatch = new TcpFlagMatchBuilder();
        tcpFlagMatch.setTcpFlag(TCP_SYN);
        matchBuilder.setTcpFlagMatch(tcpFlagMatch.build());
        return matchBuilder;
    }

    /**
     * Create tcp proto syn match.
     *
     * @param matchBuilder the match builder
     * @return matchBuilder match builder
     */
    public static MatchBuilder createTcpProtoSynMatch(MatchBuilder matchBuilder) {

        // TCP Protocol Match
        IpMatchBuilder ipMatch = new IpMatchBuilder(); // ipv4 version
        ipMatch.setIpProtocol((short) 6);
        matchBuilder.setIpMatch(ipMatch.build());

        TcpFlagMatchBuilder tcpFlagMatch = new TcpFlagMatchBuilder();
        tcpFlagMatch.setTcpFlag(TCP_SYN);
        matchBuilder.setTcpFlagMatch(tcpFlagMatch.build());
        return matchBuilder;
    }

    /**
     * Create dmac tcp port with flag match.
     *
     * @param matchBuilder the match builder
     * @param attachedMac the attached mac
     * @param tcpFlag the tcp flag
     * @param tunnelID the tunnel iD
     * @return match containing TCP_Flag (), IP Protocol (TCP), TCP_Flag (SYN)
     */
    public static MatchBuilder createDmacTcpPortWithFlagMatch(MatchBuilder matchBuilder,
            String attachedMac, Integer tcpFlag, String tunnelID) {
        return createDmacTcpPortIpSaWithFlagMatch(matchBuilder, attachedMac, tcpFlag, null, tunnelID);
    }

    /**
     * Create dmac ipSa match.
     *
     * @param matchBuilder the match builder
     * @param attachedMac the attached mac
     * @param ipPrefix the src ipPrefix
     * @param tunnelID the tunnel iD
     * @return match containing TCP_Flag (), IP Protocol (TCP), TCP_Flag (SYN), Ip Source Address (IPsa)
     */
    public static MatchBuilder createDmacIpSaMatch(
            MatchBuilder matchBuilder, String attachedMac, Ipv4Prefix ipPrefix, String tunnelID) {
        return createDmacTcpPortIpSaWithFlagMatch(matchBuilder, attachedMac, null, ipPrefix, tunnelID);
    }

    /**
     * Create dmac tcp port ipSa with flag match.
     *
     * @param matchBuilder the match builder
     * @param attachedMac the attached mac
     * @param tcpFlag the tcp flag
     * @param ipPrefix the src ipPrefix
     * @param tunnelID the tunnel iD
     * @return match containing TCP_Flag (), IP Protocol (TCP), TCP_Flag (SYN), Ip Source Address (IPsa)
     */
    public static MatchBuilder createDmacTcpPortIpSaWithFlagMatch(
            MatchBuilder matchBuilder, String attachedMac, Integer tcpFlag, Ipv4Prefix ipPrefix, String tunnelID) {

        EthernetMatchBuilder ethernetMatch = new EthernetMatchBuilder();
        EthernetTypeBuilder ethTypeBuilder = new EthernetTypeBuilder();
        ethTypeBuilder.setType(new EtherType(0x0800L));
        ethernetMatch.setEthernetType(ethTypeBuilder.build());

        if (attachedMac != null) {
            EthernetDestinationBuilder ethDestinationBuilder = new EthernetDestinationBuilder();
            ethDestinationBuilder.setAddress(new MacAddress(attachedMac));
            ethernetMatch.setEthernetDestination(ethDestinationBuilder.build());
            matchBuilder.setEthernetMatch(ethernetMatch.build());
        }

        if (tcpFlag != null) {
            // TCP Protocol Match
            IpMatchBuilder ipMatch = new IpMatchBuilder(); // ipv4 version
            ipMatch.setIpProtocol(TCP_SHORT);
            matchBuilder.setIpMatch(ipMatch.build());

            TcpFlagMatchBuilder tcpFlagMatch = new TcpFlagMatchBuilder();
            tcpFlagMatch.setTcpFlag(tcpFlag);
            matchBuilder.setTcpFlagMatch(tcpFlagMatch.build());
        }

        if (tunnelID != null) {
            TunnelBuilder tunnelBuilder = new TunnelBuilder();
            tunnelBuilder.setTunnelId(new BigInteger(tunnelID));
            matchBuilder.setTunnel(tunnelBuilder.build());
        }

        if (ipPrefix != null) {
            Ipv4MatchBuilder ipv4match = new Ipv4MatchBuilder();
            ipv4match.setIpv4Source(ipPrefix);
            matchBuilder.setLayer3Match(ipv4match.build());
        }

        return matchBuilder;
    }

    /**
     * Create dmac tcp syn match.
     *
     * @param matchBuilder the match builder
     * @param attachedMac the attached mac
     * @param tcpPort the tcp port
     * @param tcpFlag the tcp flag
     * @param tunnelID the tunnel iD
     * @return the match builder
     */
    public static MatchBuilder createDmacTcpSynMatch(MatchBuilder matchBuilder,
            String attachedMac, PortNumber tcpPort, Integer tcpFlag, String tunnelID) {

        EthernetMatchBuilder ethernetMatch = new EthernetMatchBuilder();
        EthernetTypeBuilder ethTypeBuilder = new EthernetTypeBuilder();
        ethTypeBuilder.setType(new EtherType(0x0800L));
        ethernetMatch.setEthernetType(ethTypeBuilder.build());

        EthernetDestinationBuilder ethDestinationBuilder = new EthernetDestinationBuilder();
        ethDestinationBuilder.setAddress(new MacAddress(attachedMac));
        ethernetMatch.setEthernetDestination(ethDestinationBuilder.build());
        matchBuilder.setEthernetMatch(ethernetMatch.build());

        // TCP Protocol Match
        IpMatchBuilder ipMatch = new IpMatchBuilder(); // ipv4 version
        ipMatch.setIpProtocol((short) 6);
        matchBuilder.setIpMatch(ipMatch.build());

        // TCP Port Match
        PortNumber dstPort = new PortNumber(tcpPort);
        TcpMatchBuilder tcpMatch = new TcpMatchBuilder();
        tcpMatch.setTcpDestinationPort(dstPort);
        matchBuilder.setLayer4Match(tcpMatch.build());

        TcpFlagMatchBuilder tcpFlagMatch = new TcpFlagMatchBuilder();
        tcpFlagMatch.setTcpFlag(tcpFlag);
        matchBuilder.setTcpFlagMatch(tcpFlagMatch.build());

        TunnelBuilder tunnelBuilder = new TunnelBuilder();
        tunnelBuilder.setTunnelId(new BigInteger(tunnelID));
        matchBuilder.setTunnel(tunnelBuilder.build());

        return matchBuilder;
    }

    /**
     * Create dmac tcp syn dst ip prefix tcp port.
     *
     * @param matchBuilder the match builder
     * @param attachedMac the attached mac
     * @param tcpPort the tcp port
     * @param tcpFlag the tcp flag
     * @param segmentationId the segmentation id
     * @param dstIp the dst ip
     * @return the match builder
     */
    public static MatchBuilder createDmacTcpSynDstIpPrefixTcpPort(MatchBuilder matchBuilder,
            MacAddress attachedMac, PortNumber tcpPort,  Integer tcpFlag, String segmentationId,
            Ipv4Prefix dstIp) {

        EthernetMatchBuilder ethernetMatch = new EthernetMatchBuilder();
        EthernetTypeBuilder ethTypeBuilder = new EthernetTypeBuilder();
        ethTypeBuilder.setType(new EtherType(0x0800L));
        ethernetMatch.setEthernetType(ethTypeBuilder.build());

        EthernetDestinationBuilder ethDestinationBuilder = new EthernetDestinationBuilder();
        ethDestinationBuilder.setAddress(new MacAddress(attachedMac));
        ethernetMatch.setEthernetDestination(ethDestinationBuilder.build());

        matchBuilder.setEthernetMatch(ethernetMatch.build());

        Ipv4MatchBuilder ipv4match = new Ipv4MatchBuilder();
        ipv4match.setIpv4Destination(dstIp);
        matchBuilder.setLayer3Match(ipv4match.build());

        // TCP Protocol Match
        IpMatchBuilder ipMatch = new IpMatchBuilder(); // ipv4 version
        ipMatch.setIpProtocol(TCP_SHORT);
        matchBuilder.setIpMatch(ipMatch.build());

        // TCP Port Match
        PortNumber dstPort = new PortNumber(tcpPort);
        TcpMatchBuilder tcpMatch = new TcpMatchBuilder();
        tcpMatch.setTcpDestinationPort(dstPort);
        matchBuilder.setLayer4Match(tcpMatch.build());

        TcpFlagMatchBuilder tcpFlagMatch = new TcpFlagMatchBuilder();
        tcpFlagMatch.setTcpFlag(tcpFlag);
        matchBuilder.setTcpFlagMatch(tcpFlagMatch.build());

        TunnelBuilder tunnelBuilder = new TunnelBuilder();
        tunnelBuilder.setTunnelId(new BigInteger(segmentationId));
        matchBuilder.setTunnel(tunnelBuilder.build());

        return matchBuilder;
    }

    /**
     * Create dmac ip tcp syn match.
     *
     * @param matchBuilder the match builder
     * @param dMacAddr the d mac addr
     * @param mask the mask
     * @param ipPrefix the ip prefix
     * @return MatchBuilder containing the metadata match values
     */
    public static MatchBuilder createDmacIpTcpSynMatch(MatchBuilder matchBuilder,
            MacAddress dMacAddr, MacAddress mask, Ipv4Prefix ipPrefix) {

        EthernetMatchBuilder ethernetMatch = new EthernetMatchBuilder();
        EthernetDestinationBuilder ethDestBuilder = new EthernetDestinationBuilder();
        ethDestBuilder.setAddress(new MacAddress(dMacAddr));
        if (mask != null) {
            ethDestBuilder.setMask(mask);
        }
        ethernetMatch.setEthernetDestination(ethDestBuilder.build());
        matchBuilder.setEthernetMatch(ethernetMatch.build());
        // Ethertype match
        EthernetMatchBuilder ethernetType = new EthernetMatchBuilder();
        EthernetTypeBuilder ethTypeBuilder = new EthernetTypeBuilder();
        ethTypeBuilder.setType(new EtherType(0x0800L));
        ethernetType.setEthernetType(ethTypeBuilder.build());
        matchBuilder.setEthernetMatch(ethernetType.build());
        if (ipPrefix != null) {
            Ipv4MatchBuilder ipv4match = new Ipv4MatchBuilder();
            ipv4match.setIpv4Destination(ipPrefix);
            matchBuilder.setLayer3Match(ipv4match.build());
        }
        // TCP Protocol Match
        IpMatchBuilder ipMatch = new IpMatchBuilder(); // ipv4 version
        ipMatch.setIpProtocol(TCP_SHORT);
        matchBuilder.setIpMatch(ipMatch.build());
        // TCP Flag Match
        TcpFlagMatchBuilder tcpFlagMatch = new TcpFlagMatchBuilder();
        tcpFlagMatch.setTcpFlag(TCP_SYN);
        matchBuilder.setTcpFlagMatch(tcpFlagMatch.build());

        return matchBuilder;
    }

    /**
     * Create smac tcp syn dst ip prefix tcp port.
     *
     * @param matchBuilder the match builder
     * @param attachedMac the attached mac
     * @param tcpPort the tcp port
     * @param tcpFlag the tcp flag
     * @param segmentationId the segmentation id
     * @param dstIp the dst ip
     * @return the match builder
     */
    public static MatchBuilder createSmacTcpSynDstIpPrefixTcpPort(MatchBuilder matchBuilder, MacAddress attachedMac,
            PortNumber tcpPort, Integer tcpFlag, String segmentationId, Ipv4Prefix dstIp) {

        EthernetMatchBuilder ethernetMatch = new EthernetMatchBuilder();
        EthernetTypeBuilder ethTypeBuilder = new EthernetTypeBuilder();
        ethTypeBuilder.setType(new EtherType(0x0800L));
        ethernetMatch.setEthernetType(ethTypeBuilder.build());

        EthernetSourceBuilder ethSourceBuilder = new EthernetSourceBuilder();
        ethSourceBuilder.setAddress(new MacAddress(attachedMac));
        ethernetMatch.setEthernetSource(ethSourceBuilder.build());

        matchBuilder.setEthernetMatch(ethernetMatch.build());

        Ipv4MatchBuilder ipv4match = new Ipv4MatchBuilder();
        ipv4match.setIpv4Destination(dstIp);
        matchBuilder.setLayer3Match(ipv4match.build());

        // TCP Protocol Match
        IpMatchBuilder ipMatch = new IpMatchBuilder(); // ipv4 version
        ipMatch.setIpProtocol(TCP_SHORT);
        matchBuilder.setIpMatch(ipMatch.build());

        // TCP Port Match
        PortNumber dstPort = new PortNumber(tcpPort);
        TcpMatchBuilder tcpMatch = new TcpMatchBuilder();
        tcpMatch.setTcpDestinationPort(dstPort);
        matchBuilder.setLayer4Match(tcpMatch.build());

        TcpFlagMatchBuilder tcpFlagMatch = new TcpFlagMatchBuilder();
        tcpFlagMatch.setTcpFlag(tcpFlag);
        matchBuilder.setTcpFlagMatch(tcpFlagMatch.build());

        TunnelBuilder tunnelBuilder = new TunnelBuilder();
        tunnelBuilder.setTunnelId(new BigInteger(segmentationId));
        matchBuilder.setTunnel(tunnelBuilder.build());

        return matchBuilder;
    }

    /**
     * Create smac tcp port with flag match.
     *
     * @param matchBuilder the match builder
     * @param attachedMac the attached mac
     * @param tcpFlag the tcp flag
     * @param tunnelID the tunnel iD
     * @return matchBuilder
     */
    public static MatchBuilder createSmacTcpPortWithFlagMatch(MatchBuilder matchBuilder, String attachedMac,
            Integer tcpFlag, String tunnelID) {

        EthernetMatchBuilder ethernetMatch = new EthernetMatchBuilder();
        EthernetTypeBuilder ethTypeBuilder = new EthernetTypeBuilder();
        ethTypeBuilder.setType(new EtherType(0x0800L));
        ethernetMatch.setEthernetType(ethTypeBuilder.build());

        EthernetSourceBuilder ethSrcBuilder = new EthernetSourceBuilder();
        ethSrcBuilder.setAddress(new MacAddress(attachedMac));
        ethernetMatch.setEthernetSource(ethSrcBuilder.build());
        matchBuilder.setEthernetMatch(ethernetMatch.build());

        // TCP Protocol Match
        IpMatchBuilder ipMatch = new IpMatchBuilder(); // ipv4 version
        ipMatch.setIpProtocol(TCP_SHORT);
        matchBuilder.setIpMatch(ipMatch.build());

        TcpFlagMatchBuilder tcpFlagMatch = new TcpFlagMatchBuilder();
        tcpFlagMatch.setTcpFlag(tcpFlag);
        matchBuilder.setTcpFlagMatch(tcpFlagMatch.build());

        TunnelBuilder tunnelBuilder = new TunnelBuilder();
        tunnelBuilder.setTunnelId(new BigInteger(tunnelID));
        matchBuilder.setTunnel(tunnelBuilder.build());

        return matchBuilder;
    }

    /**
     * Create smac ip tcp syn match.
     *
     * @param matchBuilder the match builder
     * @param dMacAddr the d mac addr
     * @param mask the mask
     * @param ipPrefix the ip prefix
     * @return MatchBuilder containing the metadata match values
     */
    public static MatchBuilder createSmacIpTcpSynMatch(MatchBuilder matchBuilder, MacAddress dMacAddr,
            MacAddress mask, Ipv4Prefix ipPrefix) {

        EthernetMatchBuilder ethernetMatch = new EthernetMatchBuilder();
        EthernetSourceBuilder ethSrcBuilder = new EthernetSourceBuilder();
        ethSrcBuilder.setAddress(new MacAddress(dMacAddr));
        if (mask != null) {
            ethSrcBuilder.setMask(mask);
        }
        ethernetMatch.setEthernetSource(ethSrcBuilder.build());
        matchBuilder.setEthernetMatch(ethernetMatch.build());
        // Ethertype match
        EthernetMatchBuilder ethernetType = new EthernetMatchBuilder();
        EthernetTypeBuilder ethTypeBuilder = new EthernetTypeBuilder();
        ethTypeBuilder.setType(new EtherType(0x0800L));
        ethernetType.setEthernetType(ethTypeBuilder.build());
        matchBuilder.setEthernetMatch(ethernetType.build());
        if (ipPrefix != null) {
            Ipv4MatchBuilder ipv4match = new Ipv4MatchBuilder();
            ipv4match.setIpv4Destination(ipPrefix);
            matchBuilder.setLayer3Match(ipv4match.build());
        }
        // TCP Protocol Match
        IpMatchBuilder ipMatch = new IpMatchBuilder(); // ipv4 version
        ipMatch.setIpProtocol(TCP_SHORT);
        matchBuilder.setIpMatch(ipMatch.build());
        // TCP Flag Match
        TcpFlagMatchBuilder tcpFlagMatch = new TcpFlagMatchBuilder();
        tcpFlagMatch.setTcpFlag(TCP_SYN);
        matchBuilder.setTcpFlagMatch(tcpFlagMatch.build());

        return matchBuilder;
    }

    /**
     * Create smac tcp syn.
     *
     * @param matchBuilder the match builder
     * @param attachedMac the attached mac
     * @param tcpPort the tcp port
     * @param tcpFlag the tcp flag
     * @param tunnelID the tunnel iD
     * @return the match builder
     */
    public static MatchBuilder createSmacTcpSyn(MatchBuilder matchBuilder,
            String attachedMac, PortNumber tcpPort, Integer tcpFlag, String tunnelID) {

        EthernetMatchBuilder ethernetMatch = new EthernetMatchBuilder();
        EthernetTypeBuilder ethTypeBuilder = new EthernetTypeBuilder();
        ethTypeBuilder.setType(new EtherType(0x0800L));
        ethernetMatch.setEthernetType(ethTypeBuilder.build());

        EthernetSourceBuilder ethSrcBuilder = new EthernetSourceBuilder();
        ethSrcBuilder.setAddress(new MacAddress(attachedMac));
        ethernetMatch.setEthernetSource(ethSrcBuilder.build());
        matchBuilder.setEthernetMatch(ethernetMatch.build());

        // TCP Protocol Match
        IpMatchBuilder ipMatch = new IpMatchBuilder(); // ipv4 version
        ipMatch.setIpProtocol((short) 6);
        matchBuilder.setIpMatch(ipMatch.build());

        // TCP Port Match
        PortNumber dstPort = new PortNumber(tcpPort);
        TcpMatchBuilder tcpMatch = new TcpMatchBuilder();
        tcpMatch.setTcpDestinationPort(dstPort);
        matchBuilder.setLayer4Match(tcpMatch.build());


        TcpFlagMatchBuilder tcpFlagMatch = new TcpFlagMatchBuilder();
        tcpFlagMatch.setTcpFlag(tcpFlag);
        matchBuilder.setTcpFlagMatch(tcpFlagMatch.build());

        TunnelBuilder tunnelBuilder = new TunnelBuilder();
        tunnelBuilder.setTunnelId(new BigInteger(tunnelID));
        matchBuilder.setTunnel(tunnelBuilder.build());

        return matchBuilder;
    }

    /**
     * @return MatchBuilder containing the metadata match values
     */
    public static MatchBuilder createMacSrcIpTcpSynMatch(MatchBuilder matchBuilder,
            MacAddress dMacAddr,  MacAddress mask, Ipv4Prefix ipPrefix) {

        EthernetMatchBuilder ethernetMatch = new EthernetMatchBuilder();
        EthernetDestinationBuilder ethDestinationBuilder = new EthernetDestinationBuilder();
        ethDestinationBuilder.setAddress(new MacAddress(dMacAddr));
        if (mask != null) {
            ethDestinationBuilder.setMask(mask);
        }
        ethernetMatch.setEthernetDestination(ethDestinationBuilder.build());
        matchBuilder.setEthernetMatch(ethernetMatch.build());
        // Ethertype match
        EthernetMatchBuilder ethernetType = new EthernetMatchBuilder();
        EthernetTypeBuilder ethTypeBuilder = new EthernetTypeBuilder();
        ethTypeBuilder.setType(new EtherType(0x0800L));
        ethernetType.setEthernetType(ethTypeBuilder.build());
        matchBuilder.setEthernetMatch(ethernetType.build());
        if (ipPrefix != null) {
            Ipv4MatchBuilder ipv4match = new Ipv4MatchBuilder();
            ipv4match.setIpv4Source(ipPrefix);
            matchBuilder.setLayer3Match(ipv4match.build());
        }
        // TCP Protocol Match
        IpMatchBuilder ipMatch = new IpMatchBuilder(); // ipv4 version
        ipMatch.setIpProtocol(TCP_SHORT);
        matchBuilder.setIpMatch(ipMatch.build());
        // TCP Flag Match
        TcpFlagMatchBuilder tcpFlagMatch = new TcpFlagMatchBuilder();
        tcpFlagMatch.setTcpFlag(TCP_SYN);
        matchBuilder.setTcpFlagMatch(tcpFlagMatch.build());

        return matchBuilder;
    }

    /**
     * Create a DHCP match with pot provided.
     *
     * @param matchBuilder the match builder
     * @param srcPort the source port
     * @param dstPort the destination port
     * @return the DHCP match
     */
    public static MatchBuilder createDhcpMatch(MatchBuilder matchBuilder,
                                               int srcPort, int dstPort) {

        EthernetMatchBuilder ethernetMatch = new EthernetMatchBuilder();
        EthernetTypeBuilder ethTypeBuilder = new EthernetTypeBuilder();
        ethTypeBuilder.setType(new EtherType(0x0800L));
        ethernetMatch.setEthernetType(ethTypeBuilder.build());
        matchBuilder.setEthernetMatch(ethernetMatch.build());

        IpMatchBuilder ipmatch = new IpMatchBuilder();
        ipmatch.setIpProtocol(UDP_SHORT);
        matchBuilder.setIpMatch(ipmatch.build());

        UdpMatchBuilder udpmatch = new UdpMatchBuilder();
        udpmatch.setUdpSourcePort(new PortNumber(srcPort));
        udpmatch.setUdpDestinationPort(new PortNumber(dstPort));
        matchBuilder.setLayer4Match(udpmatch.build());

        return matchBuilder;

    }

    /**
     * Creates DHCP server packet match with DHCP mac address and port.
     *
     * @param matchBuilder the matchbuilder
     * @param dhcpServerMac MAc address of the DHCP server of the subnet
     * @param srcPort the source port
     * @param dstPort the destination port
     * @return the DHCP server match
     */
    public static MatchBuilder createDhcpServerMatch(MatchBuilder matchBuilder, String dhcpServerMac, int srcPort,
            int dstPort) {

        EthernetMatchBuilder ethernetMatch = new EthernetMatchBuilder();
        EthernetTypeBuilder ethTypeBuilder = new EthernetTypeBuilder();
        ethTypeBuilder.setType(new EtherType(0x0800L));
        ethernetMatch.setEthernetType(ethTypeBuilder.build());
        matchBuilder.setEthernetMatch(ethernetMatch.build());

        EthernetSourceBuilder ethSourceBuilder = new EthernetSourceBuilder();
        ethSourceBuilder.setAddress(new MacAddress(dhcpServerMac));
        ethernetMatch.setEthernetSource(ethSourceBuilder.build());
        matchBuilder.setEthernetMatch(ethernetMatch.build());

        IpMatchBuilder ipmatch = new IpMatchBuilder();
        ipmatch.setIpProtocol(UDP_SHORT);
        matchBuilder.setIpMatch(ipmatch.build());

        UdpMatchBuilder udpmatch = new UdpMatchBuilder();
        udpmatch.setUdpSourcePort(new PortNumber(srcPort));
        udpmatch.setUdpDestinationPort(new PortNumber(dstPort));
        matchBuilder.setLayer4Match(udpmatch.build());

        return matchBuilder;

    }

    /**
     * Creates a Match with src ip address mac address set.
     * @param matchBuilder MatchBuilder Object
     * @param srcip String containing an IPv4 prefix
     * @param srcMac The source macAddress
     * @return matchBuilder Map Object with a match
     */
    public static MatchBuilder createSrcL3Ipv4MatchWithMac(MatchBuilder matchBuilder, Ipv4Prefix srcip, MacAddress srcMac) {

        Ipv4MatchBuilder ipv4MatchBuilder = new Ipv4MatchBuilder();
        ipv4MatchBuilder.setIpv4Source(new Ipv4Prefix(srcip));
        EthernetTypeBuilder ethTypeBuilder = new EthernetTypeBuilder();
        ethTypeBuilder.setType(new EtherType(0x0800L));
        EthernetMatchBuilder eth = new EthernetMatchBuilder();
        eth.setEthernetType(ethTypeBuilder.build());
        eth.setEthernetSource(new EthernetSourceBuilder()
                .setAddress(srcMac)
                .build());

        matchBuilder.setLayer3Match(ipv4MatchBuilder.build());
        matchBuilder.setEthernetMatch(eth.build());
        return matchBuilder;

    }

    /**
     * Creates a ether net match with ether type set to 0x0800L.
     * @param matchBuilder MatchBuilder Object
     * @param srcMac The source macAddress
     * @param dstMac The destination mac address
     * @return matchBuilder Map Object with a match
     */
    public static MatchBuilder createEtherMatchWithType(MatchBuilder matchBuilder,String srcMac, String dstMac)
    {
        EthernetTypeBuilder ethTypeBuilder = new EthernetTypeBuilder();
        ethTypeBuilder.setType(new EtherType(0x0800L));
        EthernetMatchBuilder eth = new EthernetMatchBuilder();
        eth.setEthernetType(ethTypeBuilder.build());
        if (null != srcMac) {
            eth.setEthernetSource(new EthernetSourceBuilder()
            .setAddress(new MacAddress(srcMac)).build());
        }
        if (null != dstMac) {
            eth.setEthernetDestination(new EthernetDestinationBuilder()
                           .setAddress(new MacAddress(dstMac)).build());
        }
        matchBuilder.setEthernetMatch(eth.build());
        return matchBuilder;
    }
    /**
     * Adds remote Ip prefix to existing match.
     * @param matchBuilder The match builder
     * @param sourceIpPrefix The source IP prefix
     * @param destIpPrefix The destination IP prefix
     * @return matchBuilder Map Object with a match
     */
    public static MatchBuilder addRemoteIpPrefix(MatchBuilder matchBuilder,
                                          Ipv4Prefix sourceIpPrefix,Ipv4Prefix destIpPrefix) {
        Ipv4MatchBuilder ipv4match = new Ipv4MatchBuilder();
        if (null != sourceIpPrefix) {
            ipv4match.setIpv4Source(sourceIpPrefix);
        }
        if (null != destIpPrefix) {
            ipv4match.setIpv4Destination(destIpPrefix);
        }
        matchBuilder.setLayer3Match(ipv4match.build());

        return matchBuilder;
    }
    /**
     * Add a layer4 match to an existing match
     *
     * @param matchBuilder Map matchBuilder MatchBuilder Object with a match
     * @param protocol The layer4 protocol
     * @param srcPort The src port
     * @param destPort The destination port
     * @return matchBuilder Map Object with a match
     */
    public static MatchBuilder addLayer4Match(MatchBuilder matchBuilder,
                                              int protocol, int srcPort, int destPort) {
        IpMatchBuilder ipmatch = new IpMatchBuilder();
        if (TCP_SHORT == protocol) {
            ipmatch.setIpProtocol(TCP_SHORT);
            TcpMatchBuilder tcpmatch = new TcpMatchBuilder();
            if (0 != srcPort) {
                tcpmatch.setTcpSourcePort(new PortNumber(srcPort));
            }
            if (0 != destPort) {
                tcpmatch.setTcpDestinationPort(new PortNumber(destPort));
            }
            matchBuilder.setLayer4Match(tcpmatch.build());
        } else if (UDP_SHORT == protocol) {
            ipmatch.setIpProtocol(UDP_SHORT);
            UdpMatchBuilder udpMatch = new UdpMatchBuilder();
            if (0 != srcPort) {
                udpMatch.setUdpSourcePort(new PortNumber(srcPort));
            }
            if (0 != destPort) {
                udpMatch.setUdpDestinationPort(new PortNumber(destPort));
            }
            matchBuilder.setLayer4Match(udpMatch.build());
        }
        matchBuilder.setIpMatch(ipmatch.build());

        return matchBuilder;
    }

    /**
     * Add a layer4 match to an existing match with mask
     *
     * @param matchBuilder Map matchBuilder MatchBuilder Object with a match.
     * @param protocol The layer4 protocol
     * @param srcPort The src port
     * @param destPort The destination port
     * @param mask the mask for the port
     * @return matchBuilder Map Object with a match
     */
    public static MatchBuilder addLayer4MatchWithMask(MatchBuilder matchBuilder,
                                                      int protocol, int srcPort, int destPort,int mask) {

        IpMatchBuilder ipmatch = new IpMatchBuilder();

        NxAugMatchNodesNodeTableFlow nxAugMatch = null;
        GeneralAugMatchNodesNodeTableFlow genAugMatch = null;
        if (protocol == TCP_SHORT) {
            ipmatch.setIpProtocol(TCP_SHORT);
            if (0 != srcPort) {
                NxmOfTcpSrcBuilder tcpSrc = new NxmOfTcpSrcBuilder();
                tcpSrc.setPort(new PortNumber(srcPort));
                tcpSrc.setMask(mask);
                nxAugMatch = new NxAugMatchNodesNodeTableFlowBuilder().setNxmOfTcpSrc(tcpSrc.build()).build();
                genAugMatch = new GeneralAugMatchNodesNodeTableFlowBuilder()
                .setExtensionList(ImmutableList.of(new ExtensionListBuilder().setExtensionKey(NxmOfTcpSrcKey.class)
                                                   .setExtension(new ExtensionBuilder()
                                                   .addAugmentation(NxAugMatchNodesNodeTableFlow.class, nxAugMatch)
                                                                 .build()).build())).build();
            } else if (0 != destPort) {
                NxmOfTcpDstBuilder tcpDst = new NxmOfTcpDstBuilder();
                tcpDst.setPort(new PortNumber(destPort));
                tcpDst.setMask(mask);
                nxAugMatch = new NxAugMatchNodesNodeTableFlowBuilder()
                .setNxmOfTcpDst(tcpDst.build())
                .build();
                genAugMatch = new GeneralAugMatchNodesNodeTableFlowBuilder()
                .setExtensionList(ImmutableList.of(new ExtensionListBuilder().setExtensionKey(NxmOfTcpDstKey.class)
                                                   .setExtension(new ExtensionBuilder()
                                                   .addAugmentation(NxAugMatchNodesNodeTableFlow.class, nxAugMatch)
                                                                 .build()).build())).build();
            }

        } else if (UDP_SHORT == protocol) {
            ipmatch.setIpProtocol(UDP_SHORT);
            UdpMatchBuilder udpMatch = new UdpMatchBuilder();
            if (0 != srcPort) {
                NxmOfUdpSrcBuilder udpSrc = new NxmOfUdpSrcBuilder();
                udpSrc.setPort(new PortNumber(srcPort));
                udpSrc.setMask(mask);
                nxAugMatch = new NxAugMatchNodesNodeTableFlowBuilder().setNxmOfUdpSrc(udpSrc.build()).build();
                genAugMatch = new GeneralAugMatchNodesNodeTableFlowBuilder()
                .setExtensionList(ImmutableList.of(new ExtensionListBuilder().setExtensionKey(NxmOfUdpSrcKey.class)
                                                   .setExtension(new ExtensionBuilder()
                                                   .addAugmentation(NxAugMatchNodesNodeTableFlow.class, nxAugMatch)
                                                                 .build()).build())).build();
            } else if (0 != destPort) {
                NxmOfUdpDstBuilder udpDst = new NxmOfUdpDstBuilder();
                udpDst.setPort(new PortNumber(destPort));
                udpDst.setMask(mask);
                nxAugMatch = new NxAugMatchNodesNodeTableFlowBuilder()
                .setNxmOfUdpDst(udpDst.build())
                .build();
                genAugMatch = new GeneralAugMatchNodesNodeTableFlowBuilder()
                .setExtensionList(ImmutableList.of(new ExtensionListBuilder().setExtensionKey(NxmOfUdpDstKey.class)
                                                   .setExtension(new ExtensionBuilder()
                                                   .addAugmentation(NxAugMatchNodesNodeTableFlow.class, nxAugMatch)
                                                                 .build()).build())).build();
            }
        }
        matchBuilder.setIpMatch(ipmatch.build());
        matchBuilder.addAugmentation(GeneralAugMatchNodesNodeTableFlow.class, genAugMatch);
        return matchBuilder;
    }

    public static MatchBuilder addCtState(MatchBuilder matchBuilder,int ct_state, int mask) {
        NxmNxCtStateBuilder ctStateBuilder = new NxmNxCtStateBuilder();
        ctStateBuilder.setCtState((long)ct_state);
        ctStateBuilder.setMask((long)mask);
        NxAugMatchNodesNodeTableFlow nxAugMatch = new NxAugMatchNodesNodeTableFlowBuilder()
        .setNxmNxCtState(ctStateBuilder.build())
        .build();
        GeneralAugMatchNodesNodeTableFlow genAugMatch = new GeneralAugMatchNodesNodeTableFlowBuilder()
        .setExtensionList(ImmutableList.of(new ExtensionListBuilder().setExtensionKey(NxmNxCtStateKey.class)
                                           .setExtension(new ExtensionBuilder()
                                           .addAugmentation(NxAugMatchNodesNodeTableFlow.class, nxAugMatch)
                                                         .build()).build())).build();
        matchBuilder.addAugmentation(GeneralAugMatchNodesNodeTableFlow.class, genAugMatch);
        return matchBuilder;
    }

    public static MatchBuilder addCtZone(MatchBuilder matchBuilder,int ct_zone) {
        NxmNxCtZoneBuilder ctZoneBuilder = new NxmNxCtZoneBuilder();
        ctZoneBuilder.setCtZone(ct_zone);
        NxAugMatchNodesNodeTableFlow nxAugMatch = new NxAugMatchNodesNodeTableFlowBuilder()
        .setNxmNxCtZone(ctZoneBuilder.build())
        .build();
        GeneralAugMatchNodesNodeTableFlow genAugMatch = new GeneralAugMatchNodesNodeTableFlowBuilder()
        .setExtensionList(ImmutableList.of(new ExtensionListBuilder().setExtensionKey(NxmNxCtZoneKey.class)
                                           .setExtension(new ExtensionBuilder()
                                           .addAugmentation(NxAugMatchNodesNodeTableFlow.class, nxAugMatch)
                                                         .build()).build())).build();
        matchBuilder.addAugmentation(GeneralAugMatchNodesNodeTableFlow.class, genAugMatch);
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

    public static MatchBuilder addNxRegMatch(MatchBuilder matchBuilder, RegMatch... matches) {
        List<ExtensionList> extensions = new ArrayList<>();
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
        GeneralAugMatchNodesNodeTableFlow m = new GeneralAugMatchNodesNodeTableFlowBuilder()
                .setExtensionList(extensions)
                .build();
        matchBuilder.addAugmentation(GeneralAugMatchNodesNodeTableFlow.class, m);
        return matchBuilder;
    }

    public static MatchBuilder addNxTunIdMatch(MatchBuilder matchBuilder, int tunId) {
        NxAugMatchNodesNodeTableFlow am = new NxAugMatchNodesNodeTableFlowBuilder()
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
        matchBuilder.addAugmentation(GeneralAugMatchNodesNodeTableFlow.class, m);
        return matchBuilder;
    }

    public static MatchBuilder addNxNspMatch(MatchBuilder matchBuilder, long nsp) {
        NxAugMatchNodesNodeTableFlow am = new NxAugMatchNodesNodeTableFlowBuilder()
                .setNxmNxNsp(new NxmNxNspBuilder()
                        .setValue(nsp)
                        .build())
                .build();
        addExtension(matchBuilder, NxmNxNspKey.class, am);
        return matchBuilder;
    }

    public static MatchBuilder addNxNsiMatch(MatchBuilder matchBuilder, short nsi) {
        NxAugMatchNodesNodeTableFlow am = new NxAugMatchNodesNodeTableFlowBuilder()
                .setNxmNxNsi(new NxmNxNsiBuilder()
                        .setNsi(nsi)
                        .build())
                .build();
        addExtension(matchBuilder, NxmNxNsiKey.class, am);
        return matchBuilder;
    }

    private static void addExtension(MatchBuilder matchBuilder, Class<? extends ExtensionKey> extensionKey,
                                     NxAugMatchNodesNodeTableFlow am) {
        GeneralAugMatchNodesNodeTableFlow existingAugmentations =
                matchBuilder.getAugmentation(GeneralAugMatchNodesNodeTableFlow.class);
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
                        .addAugmentation(NxAugMatchNodesNodeTableFlow.class, am)
                        .build())
                .build());

        GeneralAugMatchNodesNodeTableFlow m = new GeneralAugMatchNodesNodeTableFlowBuilder()
                .setExtensionList(extensions)
                .build();
        matchBuilder.addAugmentation(GeneralAugMatchNodesNodeTableFlow.class, m);
    }

    public static EthernetMatch ethernetMatch(MacAddress srcMac,
                                              MacAddress dstMac,
                                              Long etherType) {
        EthernetMatchBuilder emb = new  EthernetMatchBuilder();
        if (srcMac != null) {
            emb.setEthernetSource(new EthernetSourceBuilder()
                .setAddress(srcMac)
                .build());
        }
        if (dstMac != null) {
            emb.setEthernetDestination(new EthernetDestinationBuilder()
                .setAddress(dstMac)
                .build());
        }
        if (etherType != null) {
            emb.setEthernetType(new EthernetTypeBuilder()
                .setType(new EtherType(etherType))
                .build());
        }
        return emb.build();
    }

    /**
     * Create ipv4 prefix from ipv4 address, by appending /32 mask
     *
     * @param ipv4AddressString the ip address, in string format
     * @return Ipv4Prefix with ipv4Address and /32 mask
     */
    public static Ipv4Prefix iPv4PrefixFromIPv4Address(String ipv4AddressString) {
        return new Ipv4Prefix(ipv4AddressString + "/32");
    }

    /**
     * Converts port range into a set of masked port ranges.
     *
     * @param portMin the strating port of the range.
     * @param portMax the ending port of the range.
     * @return the map contianing the port no and their mask.
     *
     */
    public static Map<Integer,Integer>  getLayer4MaskForRange(int portMin, int portMax) {
        int [] offset = {32768,16384,8192,4096,2048,1024,512,256,128,64,32,16,8,4,2,1};
        int[] mask = {0x8000,0xC000,0xE000,0xF000,0xF800,0xFC00,0xFE00,0xFF00,
            0xFF80,0xFFC0,0xFFE0,0xFFF0,0xFFF8,0xFFFC,0xFFFE,0xFFFF};
        int noOfPorts = portMax - portMin + 1;
        String binaryNoOfPorts = Integer.toBinaryString(noOfPorts);
        int medianOffset = 16 - binaryNoOfPorts.length();
        int medianLength = offset[medianOffset];
        int median = 0;
        for (int tempMedian = 0;tempMedian < portMax;) {
            tempMedian = medianLength + tempMedian;
            if (portMin < tempMedian) {
                median = tempMedian;
                break;
            }
        }
        Map<Integer,Integer> portMap = new HashMap<Integer,Integer>();
        int tempMedian = 0;
        int currentMedain = median;
        for (int tempMedianOffset = medianOffset;16 > tempMedianOffset;tempMedianOffset++) {
            tempMedian = currentMedain - offset[tempMedianOffset];
            if (portMin <= tempMedian) {
                for (;portMin <= tempMedian;) {
                    portMap.put(tempMedian, mask[tempMedianOffset]);
                    currentMedain = tempMedian;
                    tempMedian = tempMedian - offset[tempMedianOffset];
                }
            }
        }
        currentMedain = median;
        for (int tempMedianOffset = medianOffset;16 > tempMedianOffset;tempMedianOffset++) {
            tempMedian = currentMedain + offset[tempMedianOffset];
            if (portMax >= tempMedian - 1) {
                for (;portMax >= tempMedian - 1;) {
                    portMap.put(currentMedain, mask[tempMedianOffset]);
                    currentMedain = tempMedian;
                    tempMedian = tempMedian  + offset[tempMedianOffset];
                }
            }
        }
        return portMap;
    }

    /**
     * Return Long that represents OF port for strings where OF is explicitly provided
     *
     * @param ofPortIdentifier the string with encoded OF port (example format "OFPort|999")
     * @return the OFport or null
     */
    public static Long parseExplicitOFPort(String ofPortIdentifier) {
        if (ofPortIdentifier != null) {
            String[] pair = ofPortIdentifier.split("\\|");
            if ((pair.length > 1) && (pair[0].equalsIgnoreCase("OFPort"))) {
                return new Long(pair[1]);
            }
        }
        return null;
    }
}
