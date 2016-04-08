/*
 * Copyright (c) 2015 Dell Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.routemgr.net;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.ovsdb.routemgr.utils.BitBufferHelper;
import org.opendaylight.ovsdb.routemgr.utils.BufferException;
import org.opendaylight.ovsdb.routemgr.utils.RoutemgrUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IetfInetUtil;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.ovsdb.routemgr.nd.packet.rev160302.EthernetHeader;
import org.opendaylight.yang.gen.v1.urn.opendaylight.ovsdb.routemgr.nd.packet.rev160302.Ipv6Header;
import org.opendaylight.yang.gen.v1.urn.opendaylight.ovsdb.routemgr.nd.packet.rev160302.NeighborAdvertisePacket;
import org.opendaylight.yang.gen.v1.urn.opendaylight.ovsdb.routemgr.nd.packet.rev160302.NeighborAdvertisePacketBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.ovsdb.routemgr.nd.packet.rev160302.NeighborSolicitationPacket;
import org.opendaylight.yang.gen.v1.urn.opendaylight.ovsdb.routemgr.nd.packet.rev160302.NeighborSolicitationPacketBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingListener;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketReceived;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.TransmitPacketInputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.TransmitPacketInput;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;




public class PktHandler implements PacketProcessingListener {
    private static final Logger LOG = LoggerFactory.getLogger(PktHandler.class);
    public static final int IPv6_ETHTYPE = 34525;
    public static final int ICMP_v6 = 1;

    public static final int ETHTYPE_START = 96;
    public static final int ONE_BYTE  = 8;
    public static final int TWO_BYTES = 16;
    public static final int IPv6_HDR_START = 112;
    public static final int IPv6_NEXT_HDR = 48;
    public static final int ICMPV6_HDR_START = 432;
    public static final int ICMPV6_OFFSET = 54;
    private final ExecutorService packetProcessor = Executors.newCachedThreadPool();

    private DataBroker dataService;
    private PacketProcessingService pktProcessService;


    public void setDataBrokerService(DataBroker dataService) {
        this.dataService = dataService;
    }

    public void setPacketProcessingService(PacketProcessingService packetProcessingService) {
        this.pktProcessService = packetProcessingService;
    }

    @Override
    public void onPacketReceived(PacketReceived packetReceived) {
        boolean result = false;
        int     ethType;
        int     v6NxtHdr;

        if (packetReceived == null) {
            LOG.debug("receiving null packet. returning without any processing");
            return;
        }
        byte[] data = packetReceived.getPayload();
        if (data.length <= 0) {
            LOG.debug("received packet with invalid length {}", data.length);
            return;
        }
        LOG.debug("in routemgr packet in processing");
        try {
            ethType = BitBufferHelper.getInt(BitBufferHelper.getBits(data, ETHTYPE_START, TWO_BYTES));
            if (ethType == IPv6_ETHTYPE) {
                v6NxtHdr = BitBufferHelper.getByte(BitBufferHelper.getBits(data,
                            (IPv6_HDR_START + IPv6_NEXT_HDR), ONE_BYTE));
                if (v6NxtHdr == RoutemgrUtil.ICMPv6_TYPE) {
                    LOG.debug("ICMPv6 Pdu received on port {}, pdu {} ", packetReceived.getIngress(), data);
                    int icmpv6Type = BitBufferHelper.getInt(BitBufferHelper.getBits(data,
                                        ICMPV6_HDR_START, ONE_BYTE));
                    if ((icmpv6Type == RoutemgrUtil.ICMPv6_RS_CODE) || (icmpv6Type == RoutemgrUtil.ICMPv6_NS_CODE)) {
                        packetProcessor.submit(new PacketHandler(icmpv6Type, packetReceived));
                    }
                } else {
                    LOG.debug("IPv6 Pdu received on port {} with next-header {} ",
                        packetReceived.getIngress(), v6NxtHdr);
                    LOG.debug("PDU received {}", data);
                }
            } else {
                LOG.debug("Pdu received on port {} ", packetReceived.getIngress());
                return;
            }
        } catch (Exception e) {
            LOG.warn("Failed to decode packet: {}", e.getMessage());
            return;
        }
    }

    public void close() {
        packetProcessor.shutdown();
    }

    private class PacketHandler implements Runnable {
        int type;
        PacketReceived packet;

        public PacketHandler(int icmpv6Type, PacketReceived packet) {
            this.type = icmpv6Type;
            this.packet = packet;
        }

        @Override
        public void run() {
            if (type == RoutemgrUtil.ICMPv6_NS_CODE) {
                byte[] data = packet.getPayload();
                //deserialize the packet
                NeighborSolicitationPacket nsPdu = deserializeNSPacket(data);
                LOG.debug ("deserialized the received NS packet {}", nsPdu);
                //validate the checksum
                Ipv6Header ipv6Header = (Ipv6Header)nsPdu;
                if (validateChecksum(data, ipv6Header, nsPdu.getIcmp6Chksum()) == false) {
                    LOG.warn("Received NS packet with invalid checksum  on {}. Ignoring the packet",
                        packet.getIngress());
                    return;
                }

                // obtain the interface
                IfMgr ifMgr = IfMgr.getIfMgrInstance();
                VirtualPort port = ifMgr.getInterfaceForAddress(nsPdu.getTargetIpAddress());
                if (port == null) {
                    LOG.warn("No learnt interface is available for the given target IP {}",
                        nsPdu.getTargetIpAddress());
                    return;
                }
                LOG.debug("formulating the response msg");
                //formulate the NA response
                NeighborAdvertisePacketBuilder naPacket = new NeighborAdvertisePacketBuilder();
                updateNAResponse(nsPdu, port, naPacket);
                LOG.debug("NA msg {}", naPacket.build());
                // serialize the response packet
                byte[] txPayload = convertIcmp6PacketToByte(naPacket.build());
                InstanceIdentifier<Node> outNode = packet.getIngress().getValue().firstIdentifierOf(Node.class);
                TransmitPacketInput input = new TransmitPacketInputBuilder().setPayload(txPayload)
                                              .setNode(new NodeRef(outNode))
                                              .setEgress(packet.getIngress()).build();
                // Tx the packet out of the controller.
                if(pktProcessService != null) {
                    LOG.debug("transmitting the packet out on {}", packet.getIngress());
                    pktProcessService.transmitPacket(input);
                }
            } else if (type == RoutemgrUtil.ICMPv6_RS_CODE) {
                // TODO
            }

        }

        private NeighborSolicitationPacket deserializeNSPacket(byte[] data) {
            NeighborSolicitationPacketBuilder nsPdu = new NeighborSolicitationPacketBuilder();
            int bitOffset = 0;

            try {
                nsPdu.setDestinationMac(new MacAddress(
                    RoutemgrUtil.bytesToHexString(BitBufferHelper.getBits(data, bitOffset, 48))));
                bitOffset = bitOffset + 48;
                nsPdu.setSourceMac(new MacAddress(
                    RoutemgrUtil.bytesToHexString(BitBufferHelper.getBits(data, bitOffset, 48))));
                bitOffset = bitOffset + 48;
                nsPdu.setEthertype(BitBufferHelper.getInt(BitBufferHelper.getBits(data, bitOffset, 16)));

                bitOffset = IPv6_HDR_START;
                nsPdu.setVersion(BitBufferHelper.getShort(BitBufferHelper.getBits(data, bitOffset, 4)));
                bitOffset = bitOffset + 4;
                nsPdu.setFlowLabel(BitBufferHelper.getLong(BitBufferHelper.getBits(data, bitOffset, 28)));
                bitOffset = bitOffset + 28;
                nsPdu.setIpv6Length(BitBufferHelper.getInt(BitBufferHelper.getBits(data, bitOffset, 16)));
                bitOffset = bitOffset + 16;
                nsPdu.setNextHeader(BitBufferHelper.getShort(BitBufferHelper.getBits(data, bitOffset, 8)));
                bitOffset = bitOffset + 8;
                nsPdu.setHopLimit(BitBufferHelper.getShort(BitBufferHelper.getBits(data, bitOffset, 8)));
                bitOffset = bitOffset + 8;
                nsPdu.setSourceIpv6(Ipv6Address.getDefaultInstance(
                    InetAddress.getByAddress(BitBufferHelper.getBits(data, bitOffset, 128)).getHostAddress()));
                bitOffset = bitOffset + 128;
                nsPdu.setDestinationIpv6(Ipv6Address.getDefaultInstance(
                    InetAddress.getByAddress(BitBufferHelper.getBits(data, bitOffset, 128)).getHostAddress()));
                bitOffset = bitOffset + 128;

                nsPdu.setIcmp6Type(BitBufferHelper.getShort(BitBufferHelper.getBits(data, bitOffset, 8)));
                bitOffset = bitOffset + 8;
                nsPdu.setIcmp6Code(BitBufferHelper.getShort(BitBufferHelper.getBits(data, bitOffset, 8)));
                bitOffset = bitOffset + 8;
                nsPdu.setIcmp6Chksum(BitBufferHelper.getInt(BitBufferHelper.getBits(data, bitOffset, 16)));
                bitOffset = bitOffset + 16;
                nsPdu.setReserved(Long.valueOf(0));
                bitOffset = bitOffset + 32;
                nsPdu.setTargetIpAddress(Ipv6Address.getDefaultInstance(
                    InetAddress.getByAddress(BitBufferHelper.getBits(data, bitOffset, 128)).getHostAddress()));
            } catch (BufferException | UnknownHostException  e) {
                LOG.warn("Exception obtained when deserializing NS packet", e.toString());
            }
            return nsPdu.build();
        }

        private long normalizeChecksum(long value) {
            if((value & 0xffff0000) > 0) {
                value = (value & 0xffff);
                value += 1;
            }
            return value;
        }

        private int calcIcmpv6Checksum(byte[] packet, Ipv6Header ip6Hdr) {
            long checksum = getSummation(ip6Hdr.getSourceIpv6());
            checksum += getSummation(ip6Hdr.getDestinationIpv6());
            checksum = normalizeChecksum(checksum);

            checksum += ip6Hdr.getIpv6Length();
            checksum += ip6Hdr.getNextHeader();

            int icmp6Offset = ICMPV6_OFFSET;
            long value = (((packet[icmp6Offset] & 0xff) << 8) | (packet[icmp6Offset + 1] & 0xff));
            checksum += value;
            checksum = normalizeChecksum(checksum);
            icmp6Offset += 2;

            //move to icmp6 payload skipping the checksum field
            icmp6Offset += 2;
            int length = packet.length - icmp6Offset;
            while (length > 1) {
                value = (((packet[icmp6Offset] & 0xff) << 8) | (packet[icmp6Offset + 1] & 0xff));
                checksum += value;
                checksum = normalizeChecksum(checksum);
                icmp6Offset += 2;
                length -= 2;
            }

            if (length > 0) {
                checksum += packet[icmp6Offset];
                checksum = normalizeChecksum(checksum);
            }

            int cSum = (int)(~checksum & 0xffff);
            LOG.debug("in calcIcmpv6Checksum total sum {} b4 one's complement {}", checksum, cSum);
            return cSum;
        }

        private boolean validateChecksum(byte[] packet, Ipv6Header ip6Hdr, int recvChecksum) {
            int checksum = calcIcmpv6Checksum(packet, ip6Hdr);

            if (checksum == recvChecksum) {
                return true;
            }
            return false;
        }

        private long getSummation(Ipv6Address addr) {
            byte[] bAddr = null;
            try {
                bAddr = InetAddress.getByName(addr.getValue()).getAddress();
            } catch (UnknownHostException e) {
                LOG.warn("Exception obtained when extracting ip address", e.toString());
                return 0;
            }

            long sum = 0;
            int i = 0;
            long value = 0;
            while (i < bAddr.length) {
                value = (((bAddr[i] & 0xff) << 8) | (bAddr[i+1] & 0xff));
                sum += value;
                sum = normalizeChecksum(sum);
                i+=2;
            }
            LOG.debug("summation for ip {}, {}", addr, sum);
            return sum;
        }

        private void updateNAResponse(NeighborSolicitationPacket pdu, VirtualPort port, NeighborAdvertisePacketBuilder naPacket) {
            long flag = 0;
            LOG.debug("source v6 addr {}", pdu.getSourceIpv6());
            try {
                Ipv6Address UNSPECIFIED_ADDR = Ipv6Address.getDefaultInstance(InetAddress.getByName("0:0:0:0:0:0:0:0").getHostAddress());
                if (!pdu.getSourceIpv6().equals(UNSPECIFIED_ADDR)) {
                    naPacket.setDestinationIpv6(pdu.getSourceIpv6());
                    flag = 0x60;
                } else {
                    Ipv6Address ALL_NODES_MCAST_ADDR = Ipv6Address.getDefaultInstance(InetAddress.getByName("FF02::1").getHostAddress());
                    naPacket.setDestinationIpv6(ALL_NODES_MCAST_ADDR);
                    flag = 0x20;
                }
            } catch (UnknownHostException e) {
                LOG.warn("Exception obtained when for mac", e.toString());
            }
            LOG.debug("dest v6 address {}, flags {}", naPacket.getDestinationIpv6(), flag);
            naPacket.setDestinationMac(pdu.getSourceMac());
            naPacket.setEthertype(pdu.getEthertype());
            naPacket.setSourceIpv6(port.getIpAddr().getIpv6Address());
            naPacket.setSourceMac(new MacAddress(port.getMacAddress())); 
            naPacket.setHopLimit(RoutemgrUtil.ICMPv6_MAX_HOP_LIMIT);
            naPacket.setIcmp6Type(RoutemgrUtil.ICMPv6_NA_CODE);
            naPacket.setIcmp6Code(pdu.getIcmp6Code());
            flag = flag << 24;
            naPacket.setFlags(flag);
            naPacket.setFlowLabel(pdu.getFlowLabel());
            naPacket.setIpv6Length(32);
            naPacket.setNextHeader(pdu.getNextHeader());
            naPacket.setOptionType((short)2);
            naPacket.setTargetAddrLength((short)1);
            naPacket.setTargetAddress(pdu.getTargetIpAddress());
            naPacket.setTargetLlAddress(new MacAddress(port.getMacAddress()));
            naPacket.setVersion(pdu.getVersion());
            naPacket.setIcmp6Chksum(0);
            return;
        }

        private byte[] convertIcmp6PayloadtoByte(NeighborAdvertisePacket pdu) {
            LOG.debug("in convertIcmp6PayloadtoByte");
            byte[] data = new byte[36];
            Arrays.fill(data, (byte)0);

            ByteBuffer buf = ByteBuffer.wrap(data);
            buf.put((byte)pdu.getIcmp6Type().shortValue());
            buf.put((byte)pdu.getIcmp6Code().shortValue());
            buf.putShort((short)pdu.getIcmp6Chksum().intValue());
            buf.putInt((int)pdu.getFlags().longValue());
            buf.put(IetfInetUtil.INSTANCE.ipv6AddressBytes(pdu.getTargetAddress()));
            buf.put((byte)pdu.getOptionType().shortValue());
            buf.put((byte)pdu.getTargetAddrLength().shortValue());
            buf.put(RoutemgrUtil.bytesFromHexString(pdu.getTargetLlAddress().getValue().toString()));
            LOG.debug("icmp6 payload {}", data);
            return data;
        }

        private byte[] convertIcmp6PacketToByte(NeighborAdvertisePacket pdu) {
            ByteBuffer buf = ByteBuffer.allocate(ICMPV6_OFFSET+pdu.getIpv6Length());

            buf.put(convertEthernetHeaderToByte((EthernetHeader)pdu), 0, 14);
            buf.put(convertIpv6HeaderToByte((Ipv6Header)pdu), 0, 40);
            buf.put(convertIcmp6PayloadtoByte(pdu), 0, pdu.getIpv6Length());
            int checksum = calcIcmpv6Checksum(buf.array(), (Ipv6Header) pdu);
            buf.putShort((ICMPV6_OFFSET + 2), (short)checksum);
            LOG.debug("updated payload {}", buf.array());
            return (buf.array());
        }

        private byte[] convertEthernetHeaderToByte(EthernetHeader ethPdu) {
            LOG.debug("in convertEthernetHeaderToByte ");
            byte[] data = new byte[16];
            Arrays.fill(data, (byte)0);

            ByteBuffer buf = ByteBuffer.wrap(data);
            buf.put(RoutemgrUtil.bytesFromHexString(ethPdu.getDestinationMac().getValue().toString()));
            buf.put(RoutemgrUtil.bytesFromHexString(ethPdu.getSourceMac().getValue().toString()));
            buf.putShort((short)ethPdu.getEthertype().intValue());
            LOG.debug("ether data {}", data);
            return data;
        }

        private byte[] convertIpv6HeaderToByte(Ipv6Header ip6Pdu) {
            LOG.debug("in convertIpv6HeaderToByte");
            byte[] data = new byte[128];
            Arrays.fill(data, (byte)0);

            ByteBuffer buf = ByteBuffer.wrap(data);
            long fLabel = (((long)(ip6Pdu.getVersion().shortValue() & 0x0f) << 28)
                | (ip6Pdu.getFlowLabel().longValue() & 0x0fffffff));
            buf.putInt((int)fLabel);
            buf.putShort((short)ip6Pdu.getIpv6Length().intValue());
            buf.put((byte)ip6Pdu.getNextHeader().shortValue());
            buf.put((byte)ip6Pdu.getHopLimit().shortValue());
            buf.put(IetfInetUtil.INSTANCE.ipv6AddressBytes(ip6Pdu.getSourceIpv6()));
            buf.put(IetfInetUtil.INSTANCE.ipv6AddressBytes(ip6Pdu.getDestinationIpv6()));
            LOG.debug ("ipv6 header {}", data);
            return data;
        }
    }
}
