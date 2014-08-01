/*
 * Copyright (C) 2013 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 */

package org.opendaylight.ovsdb.openstack.netvirt.providers.mdsalopenflow13;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Prefix;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.inventory.rev130819.tables.table.FlowBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.types.rev131026.flow.MatchBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.EtherType;
import org.opendaylight.yang.gen.v1.urn.opendaylight.l2.types.rev130827.VlanId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;

public class OvsFlowMatchClient {
    static final Logger logger = LoggerFactory.getLogger(OvsFlowMatchClient.class);

    private Long etherType; private String tunnelID;
    private String attachedMac; private String uri;
    private VlanId vlanId; private Integer tos;
    private Short nwTtl; private Long inPortNumber;
    private Short ipProtocol; private String srcMacAddr;
    private String dstMacAddr; private String macAddressMask;
    private String srcIpPrefix; private String dstIpPrefix;
    private Integer srcTcpPort; private Integer dstTcpPort;
    private Integer srcUdpPort; private Integer dstUdpPort;private Long dpid;
    private FlowBuilder flowBuilder; private OvsFlowMatchClient ovsFlowMatch;
    private Integer tcpFlag; private Boolean matchAll;
    public Short ipProtocol() { return this.ipProtocol; }
    public Boolean matchAll() {return this.matchAll;}
    public Integer tcpFlag() { return this.tcpFlag;}
    public Long etherType() { return this.etherType;}
    public String tunnelID() { return this.tunnelID; }
    public String attachedMac() {return this.attachedMac;}
    public String uri() { return this.uri;}
    public VlanId vlanId() {return this.vlanId;}
    public Integer tos() { return this.tos; }
    public Short nwTtl() { return this.nwTtl; }
    public Long inPortNumber() { return this.inPortNumber; }
    public String srcMacAddr() { return this.srcMacAddr; }
    public String dstMacAddr() { return this.dstMacAddr; }
    public String macAddressMask() { return this.macAddressMask; }
    public String srcIpPrefix() { return this.srcIpPrefix; }
    public String dstIpPrefix() { return this.dstIpPrefix; }
    public Integer srcTcpPort() { return this.srcTcpPort; }
    public Integer dstTcpPort() { return this.dstTcpPort; }
    public Integer srcUdpPort() { return this.srcUdpPort; }
    public Integer dstUdpPort() { return this.dstUdpPort; }
    public Long dpid() { return this.dpid; }

    public OvsFlowMatchClient ipProtocol(final Short ipProtocol) {
        this.ipProtocol = ipProtocol;
        return this;
    }
    public OvsFlowMatchClient matchAll(final Boolean matchAll) {
        this.matchAll = matchAll;
        return this;
    }
    public OvsFlowMatchClient tcpFlag(final Integer tcpFlag) {
        this.tcpFlag = tcpFlag;
        return this;
    }
    public OvsFlowMatchClient etherType(final Long etherType) {
        this.etherType = etherType;
        return this;
    }
    public OvsFlowMatchClient tunnelID(final String tunnelID) {
        this.tunnelID = tunnelID;
        return this;
    }
    public OvsFlowMatchClient attachedMac(final String attachedMac) {
        this.attachedMac = attachedMac;
        return this;
    }
    public OvsFlowMatchClient uri(final String uri) {
        this.uri = uri;
        return this;
    }
    public OvsFlowMatchClient vlanId(final VlanId vlanId) {
        this.vlanId = vlanId;
        return this;
    }
    public OvsFlowMatchClient tos(final Integer tos) {
        this.tos = tos;
        return this;
    }
    public OvsFlowMatchClient nwTtl(final Short nwTtl) {
        this.nwTtl = nwTtl;
        return this;
    }
    public OvsFlowMatchClient inPortNumber(final Long inPortNumber) {
        this.inPortNumber = inPortNumber;
        return this;
    }
    public OvsFlowMatchClient srcMacAddr(final String srcMacAddr) {
        this.srcMacAddr = srcMacAddr;
        return this;
    }
    public OvsFlowMatchClient dstMacAddr(final String dstMacAddr) {
        this.dstMacAddr = dstMacAddr;
        return this;
    }
    public OvsFlowMatchClient macAddressMask(final String macAddressMask) {
        this.macAddressMask = macAddressMask;
        return this;
    }
    public OvsFlowMatchClient srcIpPrefix(final String srcIpPrefix) {
        this.srcIpPrefix = srcIpPrefix;
        return this;
    }
    public OvsFlowMatchClient dstIpPrefix(final String dstIpPrefix) {
        this.dstIpPrefix = dstIpPrefix;
        return this;
    }
    public OvsFlowMatchClient srcTcpPort(final Integer srcTcpPort) {
        this.srcTcpPort = srcTcpPort;
        return this;
    }
    public OvsFlowMatchClient dstTcpPort(final Integer dstTcpPort) {
        this.dstTcpPort = dstTcpPort;
        return this;
    }
    public OvsFlowMatchClient srcUdpPort(final Integer srcUdpPort) {
        this.srcUdpPort = srcUdpPort;
        return this;
    }
    public OvsFlowMatchClient dstUdpPort(final Integer dstUdpPort) {
        this.dstUdpPort = dstUdpPort;
        return this;
    }
    public OvsFlowMatchClient dpid(final Long dpid) {
        this.dpid = dpid;
        return this;
    }

    public void buildOtherProviderOpenFlowImplementationsHere() {
        // TODO Other Provider Matches and Versions are simply new builders in this class
    }

    public FlowBuilder buildMDSalMatch(FlowBuilder flowBuilder, OvsFlowMatchClient ovsFlowMatch) {
        this.flowBuilder = flowBuilder;
        this.ovsFlowMatch = ovsFlowMatch;
        MatchBuilder matchBuilder = new MatchBuilder();
        // Match: IP protocol
        if (ovsFlowMatch.ipProtocol() != null) {
            //            logger.info("NEWMATCH ipProtocool() ->  {}", ipProtocol());
            Of13MDSalMatch.createIpProtocolMatch(matchBuilder, ipProtocol);
        }
        // Match: Ethertype
        if (ovsFlowMatch.etherType() != null) {
            EtherType etherType = new EtherType(ovsFlowMatch.etherType());
            //            logger.info("ClientMatch EtherType: {}", ovsFlowMatch.etherType());
            Of13MDSalMatch.createEtherTypeMatch(matchBuilder, etherType);
            //            logger.info("MatchBuilfer EtherType: {}", matchBuilder.getEthernetMatch());
        }
        // Match: Tunnel ID
        if (ovsFlowMatch.tunnelID() != null) {
            BigInteger tunID = new BigInteger(ovsFlowMatch.tunnelID());
            Of13MDSalMatch.createTunnelIDMatch(matchBuilder, tunID);
        }
        // Match: Source Mac Address and Mask (typically broadcast/multicast)
        if (this.srcMacAddr() != null && this.macAddressMask() != null) {
            MacAddress macAddress = new MacAddress(this.srcMacAddr());
            MacAddress macAddressMask = new MacAddress(this.macAddressMask());
            Of13MDSalMatch.createEthSrcMatch(matchBuilder, macAddress, macAddressMask);
        }
        // Match: Source Mac Address w/o Mask
        if (this.srcMacAddr() != null && this.macAddressMask() == null) {
            MacAddress macAddress = new MacAddress(this.srcMacAddr());
            Of13MDSalMatch.createEthSrcMatch(matchBuilder, macAddress, null);
        }
        // Match: Destination Mac Address and Mask (typically broadcast/multicast)
        if (this.dstMacAddr() != null && this.dstMacAddr() != null) {
            MacAddress macAddress = new MacAddress(this.dstMacAddr());
            MacAddress macAddressMask = new MacAddress(this.macAddressMask());
            Of13MDSalMatch.createDestEthMatch(matchBuilder, macAddress, macAddressMask);
        }
        // Match: Destination Mac Address w/o Mask
        if (this.dstMacAddr() != null && this.macAddressMask() == null) {
            MacAddress macAddress = new MacAddress(this.dstMacAddr());
            Of13MDSalMatch.createDestEthMatch(matchBuilder, macAddress, null);
        }
        // Match: L3 Source IPv4
        if (this.srcIpPrefix() != null) {
            Ipv4Prefix ipv4Address = new Ipv4Prefix(this.srcIpPrefix);
            Of13MDSalMatch.createSrcL3IPv4Match(matchBuilder, ipv4Address);
        }
        // Match: L3 Destination IPv4
        if (this.dstIpPrefix() != null) {
            Ipv4Prefix ipv4Address = new Ipv4Prefix(this.dstIpPrefix);
            Of13MDSalMatch.createDstL3IPv4Match(matchBuilder, ipv4Address);
        }
        // Match: L4 Source TCP Port
        if (this.srcTcpPort() != null) {
            PortNumber srcTcpPort = new PortNumber(this.srcTcpPort());
            Of13MDSalMatch.createSetSrcTcpMatch(matchBuilder, srcTcpPort);
        }
        // Match: L4 Destination TCP Port
        if (this.dstTcpPort() != null) {
            PortNumber dstTcpPort = new PortNumber(this.dstTcpPort());
            Of13MDSalMatch.createSetSrcTcpMatch(matchBuilder, dstTcpPort);
        }
        // Match: L4 Source UDP Port
        if (this.srcUdpPort() != null) {
            PortNumber srcUdpPort = new PortNumber(this.srcUdpPort());
            Of13MDSalMatch.createDstUdpPortMatch(matchBuilder, srcUdpPort);
        }
        // Match: L4 Destination UDP Port
        if (this.dstUdpPort() != null) {
            PortNumber dstUdpPort = new PortNumber(ovsFlowMatch.dstUdpPort());
            Of13MDSalMatch.createSrcUdpPortMatch(matchBuilder, dstUdpPort);
        }
        // Match: Ingress OpenFlow Port with Node Connector ID via DPID
        if (ovsFlowMatch.inPortNumber() != null && ovsFlowMatch.dpid() != null) {
            Of13MDSalMatch.createInPortMatch(matchBuilder, ovsFlowMatch.dpid(),
                    ovsFlowMatch.inPortNumber());
        }
        // Match: TCP Flag Match NXM Extension
        if (this.tcpFlag() != null) {
            Of13MDSalMatch.createTcpFlagMatch(matchBuilder, this.tcpFlag);
        }
        // Match: VLAN ID
        if (this.vlanId() != null) {
            Of13MDSalMatch.createVlanIdMatch(matchBuilder, this.vlanId);
        }
        flowBuilder.setMatch(matchBuilder.build());
        return flowBuilder;
    }

    @Override
    public String toString() {
        return "OvsFlowMatch{" +
                "etherType=" + etherType +
                ", tunnelID='" + tunnelID + '\'' +
                ", attachedMac='" + attachedMac + '\'' +
                ", uri='" + uri + '\'' +
                ", vlanId=" + vlanId +
                ", tos=" + tos +
                ", nwTtl=" + nwTtl +
                ", inPortNumber=" + inPortNumber +
                ", ipProtocol=" + ipProtocol +
                ", srcMacAddr='" + srcMacAddr + '\'' +
                ", dstMacAddr='" + dstMacAddr + '\'' +
                ", macAddressMask='" + macAddressMask + '\'' +
                ", srcIpPrefix='" + srcIpPrefix + '\'' +
                ", dstIpPrefix='" + dstIpPrefix + '\'' +
                ", srcTcpPort=" + srcTcpPort +
                ", dstTcpPort=" + dstTcpPort +
                ", srcUdpPort=" + srcUdpPort +
                ", dstUdpPort=" + dstUdpPort +
                ", dpid=" + dpid +
                ", flowBuilder=" + flowBuilder +
                ", ovsFlowMatch=" + ovsFlowMatch +
                ", tcpFlag=" + tcpFlag +
                ", matchAll=" + matchAll +
                '}';
    }
}