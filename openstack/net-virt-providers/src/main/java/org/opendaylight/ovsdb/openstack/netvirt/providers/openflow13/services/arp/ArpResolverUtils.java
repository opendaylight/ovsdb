/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.services.arp;

import org.opendaylight.controller.liblldp.EtherTypes;
import org.opendaylight.controller.liblldp.Ethernet;
import org.opendaylight.controller.liblldp.NetUtils;
import org.opendaylight.controller.liblldp.PacketException;
import org.opendaylight.yang.gen.v1.urn.opendaylight.packet.service.rev130709.PacketReceived;

public class ArpResolverUtils {

    static {
        Ethernet.etherTypeClassMap.put(EtherTypes.ARP.shortValue(), Arp.class);
    }

    /**
     * Tries to deserialize received packet as ARP packet with IPv4 protocol address and MAC
     * hardware address.
     *
     * @param potentialArp the packet for deserialization
     * @return ARP packet if received packet is ARP and deserialization was successful
     * @throws PacketException if packet is not ARP or deserialization was not successful
     */
    public static Arp getArpFrom(PacketReceived potentialArp) throws PacketException {
        byte[] payload = potentialArp.getPayload();
        Ethernet ethPkt = new Ethernet();
        ethPkt.deserialize(payload, 0, payload.length * NetUtils.NumBitsInAByte);
        if (ethPkt.getPayload() instanceof Arp) {
            return (Arp) ethPkt.getPayload();
        }
        throw new PacketException("Packet is not ARP: " + potentialArp);
    }
}
