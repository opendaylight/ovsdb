/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.services.arp;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.concurrent.Future;

import org.opendaylight.controller.liblldp.EtherTypes;
import org.opendaylight.controller.liblldp.Ethernet;
import org.opendaylight.controller.liblldp.NetUtils;
import org.opendaylight.controller.liblldp.PacketException;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeConnectorRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeId;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.NodeRef;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnector;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.node.NodeConnectorKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.Node;
import org.opendaylight.yang.gen.v1.urn.opendaylight.inventory.rev130819.nodes.NodeKey;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketProcessingService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.TransmitPacketInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.TransmitPacketInputBuilder;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.JdkFutureAdapters;
import com.google.common.util.concurrent.ListenableFuture;

/**
 * Uses packet-out for sending ARP Requests.
 */
public class ArpSender {

    private static final Logger LOG = LoggerFactory.getLogger(ArpSender.class);

    private static final String OFPP_ALL = "0xfffffffc";
    private final PacketProcessingService packetProcessingService;

    public ArpSender(PacketProcessingService packetProcessingService) {
        this.packetProcessingService = checkNotNull(packetProcessingService);
    }

    /**
     * Sends ARP Request as packet-out from all openflow physical ports on the given node.
     *
     * @param senderAddress the addresses used in sender part of ARP packet
     * @param tpa the target protocol address, in this case IPv4 address for which MAC should be
     *        discovered
     * @param nodeIid the path to node from where the ARP packet will be flooded
     * @return future result about success of packet-out
     */
    public ListenableFuture<RpcResult<Void>> floodArp(ArpMessageAddress senderAddress, Ipv4Address tpa,
            InstanceIdentifier<Node> nodeIid) {
        checkNotNull(senderAddress);
        checkNotNull(tpa);
        checkNotNull(nodeIid);
        // node connector representing all physical ports on node
        NodeConnectorKey nodeConnectorKey = new NodeConnectorKey(createNodeConnectorId(OFPP_ALL,
                nodeIid.firstKeyOf(Node.class, NodeKey.class).getId()));
        InstanceIdentifier<NodeConnector> egressNc = nodeIid.child(NodeConnector.class, nodeConnectorKey);
        return sendArp(senderAddress, tpa, egressNc);
    }

    private NodeConnectorId createNodeConnectorId(String connectorId, NodeId nodeId) {
        return new NodeConnectorId(nodeId.getValue() + ":" + connectorId);
    }

    /**
     * Sends ARP Request as packet-out from the given port (node connector).
     *
     * @param senderAddress the addresses used in sender part of ARP packet
     * @param tpa the target protocol address, in this case IPv4 address for which MAC should be
     *        discovered
     * @param egressNc the path to node connector from where the ARP packet will be sent
     * @return future result about success of packet-out
     */
    public ListenableFuture<RpcResult<Void>> sendArp(ArpMessageAddress senderAddress, Ipv4Address tpa,
            InstanceIdentifier<NodeConnector> egressNc) {
        checkNotNull(senderAddress);
        checkNotNull(tpa);
        checkNotNull(egressNc);
        final Ethernet arpFrame = createArpFrame(senderAddress, tpa);
        byte[] arpFrameAsBytes;
        try {
            arpFrameAsBytes = arpFrame.serialize();
        } catch (PacketException e) {
            LOG.warn("Serializition of ARP packet is not successful.", e);
            if (LOG.isDebugEnabled()) {
                LOG.debug("ARP packet: {}", ArpUtils.getArpFrameToStringFormat(arpFrame));
            }
            return Futures.immediateFailedFuture(e);
        }
        // Generate packet with destination switch and port
        TransmitPacketInput packet = new TransmitPacketInputBuilder().setEgress(new NodeConnectorRef(egressNc))
            .setNode(new NodeRef(egressNc.firstIdentifierOf(Node.class)))
            .setPayload(arpFrameAsBytes)
            .build();
        if (LOG.isTraceEnabled()) {
            LOG.trace("Sending ARP REQUEST \n{}", ArpUtils.getArpFrameToStringFormat(arpFrame));
        }
        Future<RpcResult<Void>> futureTransmitPacketResult = packetProcessingService.transmitPacket(packet);
        return JdkFutureAdapters.listenInPoolThread(futureTransmitPacketResult);
    }

    private Ethernet createArpFrame(ArpMessageAddress senderAddress, Ipv4Address tpa) {
        byte[] senderMac = ArpUtils.macToBytes(senderAddress.getHardwareAddress());
        byte[] senderIp = ArpUtils.ipToBytes(senderAddress.getProtocolAddress());
        byte[] targetMac = NetUtils.getBroadcastMACAddr();
        byte[] targetIp = ArpUtils.ipToBytes(tpa);
        Ethernet arpFrame = new Ethernet().setSourceMACAddress(senderMac)
            .setDestinationMACAddress(targetMac)
            .setEtherType(EtherTypes.ARP.shortValue());
        Arp arp = new Arp().setOperation(ArpOperation.REQUEST.intValue())
            .setSenderHardwareAddress(senderMac)
            .setSenderProtocolAddress(senderIp)
            .setTargetHardwareAddress(targetMac)
            .setTargetProtocolAddress(targetIp);
        arpFrame.setPayload(arp);
        return arpFrame;
    }

}
