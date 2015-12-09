/*
 * Copyright (c) 2015 Red Hat, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.openstack.netvirt.api;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;

/**
 * This interface allows for notifications from GatewayMacResolver to a generic listener.
 */
public interface GatewayMacResolverListener {

    /**
     * Method will trigger when the mac for gateway IP is resolved or updated.
     *
     * @param externalNetworkBridgeDpid Bridge used for sending ARP request
     * @param gatewayIpAddress Ip address that Mac Resolver ARPed for
     * @param macAddress Mac Address associated with the gatewayIpAddress
     * @return
     */
    void gatewayMacResolved(final Long externalNetworkBridgeDpid, final IpAddress gatewayIpAddress,
                            final MacAddress macAddress);
}
