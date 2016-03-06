/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.services.arp;

import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.annotation.Nullable;

import org.opendaylight.controller.liblldp.EtherTypes;
import org.opendaylight.controller.liblldp.Ethernet;
import org.opendaylight.controller.liblldp.HexEncode;
import org.opendaylight.controller.liblldp.Packet;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;

import com.google.common.net.InetAddresses;

public class ArpUtils {

    private ArpUtils() {
        throw new UnsupportedOperationException("Cannot create an instance.");
    }

    /**
     * Returns Ethernet and ARP in readable string format
     */
    public static String getArpFrameToStringFormat(Ethernet eth) {
        String ethernetString = "Ethernet [getEtherType()="
                + EtherTypes.loadFromString(String.valueOf(eth.getEtherType())) + ", getSourceMACAddress()="
                + HexEncode.bytesToHexStringFormat(eth.getSourceMACAddress()) + ", getDestinationMACAddress()="
                + HexEncode.bytesToHexStringFormat(eth.getDestinationMACAddress()) + "]\n";
        Packet potentialArp = eth.getPayload();
        String arpString = null;
        if (potentialArp instanceof Arp) {
            Arp arp = (Arp) potentialArp;
            arpString = ArpUtils.getArpToStringFormat(arp);
        } else {
            arpString = "ARP was not found in Ethernet frame.";
        }
        return ethernetString.concat(arpString);
    }

    /**
     * Returns ARP in readable string format
     */
    public static String getArpToStringFormat(Arp arp) {
        try {
            return "Arp [getHardwareType()=" + arp.getHardwareType() + ", getProtocolType()=" + arp.getProtocolType()
                    + ", getHardwareLength()=" + arp.getHardwareLength() + ", getProtocolLength()="
                    + arp.getProtocolLength() + ", getOperation()=" + ArpOperation.loadFromInt(arp.getOperation())
                    + ", getSenderHardwareAddress()="
                    + HexEncode.bytesToHexStringFormat(arp.getSenderHardwareAddress())
                    + ", getSenderProtocolAddress()="
                    + InetAddress.getByAddress(arp.getSenderProtocolAddress()).getHostAddress()
                    + ", getTargetHardwareAddress()="
                    + HexEncode.bytesToHexStringFormat(arp.getTargetHardwareAddress())
                    + ", getTargetProtocolAddress()="
                    + InetAddress.getByAddress(arp.getTargetProtocolAddress()).getHostAddress() + "]\n";
        } catch (UnknownHostException e1) {
            return "Error during parsing Arp " + arp;
        }
    }

    public static byte[] macToBytes(MacAddress mac) {
        return HexEncode.bytesFromHexString(mac.getValue());
    }

    public static @Nullable MacAddress bytesToMac(byte[] macBytes) {
        String mac = HexEncode.bytesToHexStringFormat(macBytes);
        if (!"null".equals(mac)) {
            return new MacAddress(mac);
        }
        return null;
    }

    public static byte[] ipToBytes(Ipv4Address ip) {
        return InetAddresses.forString(ip.getValue()).getAddress();
    }

    public static @Nullable Ipv4Address bytesToIp(byte[] ipv4AsBytes) {
        try {
            return new Ipv4Address(InetAddress.getByAddress(ipv4AsBytes).getHostAddress());
        } catch (UnknownHostException e) {
            return null;
        }
    }

}
