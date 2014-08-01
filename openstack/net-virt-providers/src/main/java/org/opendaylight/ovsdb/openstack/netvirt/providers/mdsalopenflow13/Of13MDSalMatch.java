/*
 * Copyright (C) 2013 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.ovsdb.openstack.netvirt.providers.mdsalopenflow13;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Dscp;
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
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.EthernetMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.Icmpv4MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.IpMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.TcpFlagMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.TunnelBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.VlanMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._3.match.Ipv4MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._4.match.TcpMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.match.layer._4.match.UdpMatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.model.match.types.rev131026.vlan.match.fields.VlanIdBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;

public class Of13MDSalMatch {
    private static final Logger logger = LoggerFactory.getLogger(Of13MDSalMatch.class);
    private static final long IPV4 = 0x0800L;
    private static final short TCP = (short) 6;

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
    public static MatchBuilder createEthSrcMatch(MatchBuilder matchBuilder, MacAddress sMacAddr, MacAddress mask) {

        EthernetMatchBuilder ethernetMatch = new EthernetMatchBuilder();
        EthernetSourceBuilder ethSourceBuilder = new EthernetSourceBuilder();
        ethSourceBuilder.setAddress(new MacAddress(sMacAddr));
        if (mask != null) {
            ethSourceBuilder.setMask(mask);
        }
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
        ethTypeBuilder.setType(new EtherType(IPV4));
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
        ethTypeBuilder.setType(new EtherType(IPV4));
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
        ethTypeBuilder.setType(new EtherType(IPV4));
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
        ethTypeBuilder.setType(new EtherType(IPV4));
        ethType.setEthernetType(ethTypeBuilder.build());
        matchBuilder.setEthernetMatch(ethType.build());

        IpMatchBuilder ipmatch = new IpMatchBuilder();
        ipmatch.setIpProtocol(TCP);
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
        ethTypeBuilder.setType(new EtherType(IPV4));
        ethType.setEthernetType(ethTypeBuilder.build());
        matchBuilder.setEthernetMatch(ethType.build());

        IpMatchBuilder ipmatch = new IpMatchBuilder();
        ipmatch.setIpProtocol(TCP);
        matchBuilder.setIpMatch(ipmatch.build());

        TcpMatchBuilder tcpmatch = new TcpMatchBuilder();
        tcpmatch.setTcpDestinationPort(tcpDstPort);
        matchBuilder.setLayer4Match(tcpmatch.build());

        return matchBuilder;
    }

    /**
     * Create Source UDP Port Match
     *
     * @param matchBuilder @param matchbuilder MatchBuilder Object for OF match encapsulation
     * @param udpport      PortNumber representing a source UDP port
     * @return             matchBuilder Map MatchBuilder Object with a match
     */
    public static MatchBuilder createSrcUdpPortMatch(MatchBuilder matchBuilder, PortNumber udpport) {

        EthernetMatchBuilder ethType = new EthernetMatchBuilder();
        EthernetTypeBuilder ethTypeBuilder = new EthernetTypeBuilder();
        ethTypeBuilder.setType(new EtherType(IPV4));
        ethType.setEthernetType(ethTypeBuilder.build());
        matchBuilder.setEthernetMatch(ethType.build());
        // UDP Protocol Pre-Requisite Match
        IpMatchBuilder ipmatch = new IpMatchBuilder();
        ipmatch.setIpProtocol(TCP);
        matchBuilder.setIpMatch(ipmatch.build());

        UdpMatchBuilder udpmatch = new UdpMatchBuilder();
        udpmatch.setUdpSourcePort(udpport);
        matchBuilder.setLayer4Match(udpmatch.build());

        return matchBuilder;

    }

    /**
     * Create Destination UDP Port Match
     *
     * @param matchBuilder MatchBuilder Object for OF match encapsulation
     * @param udpport      Integer representing a destination UDP port
     * @return             matchBuilder Map MatchBuilder Object with a match
     */
    public static MatchBuilder createDstUdpPortMatch(MatchBuilder matchBuilder, PortNumber udpport) {

        EthernetMatchBuilder ethType = new EthernetMatchBuilder();
        EthernetTypeBuilder ethTypeBuilder = new EthernetTypeBuilder();
        ethTypeBuilder.setType(new EtherType(IPV4));
        ethType.setEthernetType(ethTypeBuilder.build());
        matchBuilder.setEthernetMatch(ethType.build());
        // UDP Protocol Pre-Requisite Match
        IpMatchBuilder ipmatch = new IpMatchBuilder();
        ipmatch.setIpProtocol(TCP);
        matchBuilder.setIpMatch(ipmatch.build());

        UdpMatchBuilder udpmatch = new UdpMatchBuilder();
        udpmatch.setUdpDestinationPort(udpport);
        matchBuilder.setLayer4Match(udpmatch.build());

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
        ethTypeBuilder.setType(new EtherType(IPV4));
        ethernetType.setEthernetType(ethTypeBuilder.build());
        matchBuilder.setEthernetMatch(ethernetType.build());

        // TCP Protocol Match
        IpMatchBuilder ipMatch = new IpMatchBuilder(); // ipv4 version
        ipMatch.setIpProtocol(TCP);
        matchBuilder.setIpMatch(ipMatch.build());

        TcpFlagMatchBuilder tcpFlagMatch = new TcpFlagMatchBuilder();
        tcpFlagMatch.setTcpFlag(tcpFlag);
        matchBuilder.setTcpFlagMatch(tcpFlagMatch.build());
        return matchBuilder;
    }

    /**
     * Create IP Protocol Match
     *
     * @param matchBuilder MatchBuilder Object for OF match encapsulation
     * @param udpport      short representing an IP protocol value
     * @return             matchBuilder Map MatchBuilder Object with a match
     */
    public static MatchBuilder createIpProtocolMatch(MatchBuilder matchBuilder, short ipProto) {

        EthernetMatchBuilder ethmatch = new EthernetMatchBuilder();
        EthernetTypeBuilder ethtype = new EthernetTypeBuilder();
        EtherType type = new EtherType(IPV4);
        ethmatch.setEthernetType(ethtype.setType(type).build());
        matchBuilder.setEthernetMatch(ethmatch.build());

        IpMatchBuilder ipmatch = new IpMatchBuilder(); // ipv4 version
        ipmatch.setIpProtocol(ipProto);
        matchBuilder.setIpMatch(ipmatch.build());
        return matchBuilder;
    }


    /**
     * Create ToS Match
     *
     * @param matchBuilder MatchBuilder Object for OF match encapsulation
     * @param udpport      Integer representing a destination UDP port
     * @return             matchBuilder Map MatchBuilder Object with a match
     */
    public static MatchBuilder createIpv4ToSMatch(MatchBuilder matchBuilder, short dscp) {
        MatchBuilder match = new MatchBuilder();
        EthernetMatchBuilder ethmatch = new EthernetMatchBuilder();
        EthernetTypeBuilder ethtype = new EthernetTypeBuilder();
        EtherType type = new EtherType(IPV4);
        ethmatch.setEthernetType(ethtype.setType(type).build());
        match.setEthernetMatch(ethmatch.build());

        IpMatchBuilder ipmatch = new IpMatchBuilder(); // ipv4 version
        ipmatch.setIpProtocol(TCP);
        Dscp dscpVal = new Dscp(dscp);
        ipmatch.setIpDscp(dscpVal);
        match.setIpMatch(ipmatch.build());
        return match;
    }

}