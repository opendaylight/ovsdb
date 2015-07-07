/*
 * Copyright (c) 2015 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.services.arp;

import static com.google.common.base.Preconditions.checkNotNull;

import javax.annotation.concurrent.Immutable;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;

/**
 * Represents ARP fields where protocol address is IPv4 address and hardware address is MAC address.
 */
@Immutable
public class ArpMessageAddress {

    private final MacAddress hwAddress;
    private final Ipv4Address protocolAddress;

    public ArpMessageAddress(MacAddress hwAddress, Ipv4Address protocolAddress) {
        this.hwAddress = checkNotNull(hwAddress);
        this.protocolAddress = checkNotNull(protocolAddress);
    }

    public MacAddress getHardwareAddress() {
        return hwAddress;
    }

    public Ipv4Address getProtocolAddress() {
        return protocolAddress;
    }

}
