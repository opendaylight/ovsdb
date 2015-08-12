/*
 * Copyright (c) 2013, 2015 Red Hat, Inc. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.plugin.md;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv6Address;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.Inet4Address;
import java.net.InetAddress;

/**
 * Utilities to convert Java types to the types specified in the Yang models
 */
public final class Utils {

    static final Logger logger = LoggerFactory.getLogger(Utils.class);

    /**
     * Returns a {@link org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress}
     * from a @{link java.net.InetAddress}
     */
    public static IpAddress convertIpAddress(InetAddress inetAddress){

        if (inetAddress instanceof Inet4Address){
            Ipv4Address ipv4Address = new Ipv4Address(inetAddress.getHostAddress());
            return new IpAddress(ipv4Address);
        }
        else {
            Ipv6Address ipv6Address = new Ipv6Address(inetAddress.getHostAddress());
            return new IpAddress(ipv6Address);
        }
    }
}
