/*
 * Copyright (c) 2014 Cisco Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.southbound;

import org.opendaylight.ovsdb.lib.OvsdbClient;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.PortNumber;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.overlay.rev150105.IpPortLocator;
import org.opendaylight.yang.gen.v1.urn.tbd.params.xml.ns.yang.network.topology.rev131021.network.topology.topology.Node;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;

public class OvsdbClientKey {
    /*
     * This class is immutable.  If you are in anyway changing its fields after
     * creation, your are doing it wrong :)
     */
    private IpAddress ipaddress;
    private PortNumber port;

    OvsdbClientKey(IpPortLocator locator) {
        ipaddress = locator.getIp();
        port = locator.getPort();
    }

    OvsdbClientKey(IpAddress ip, PortNumber port) {
        this.ipaddress = ip;
        this.port = port;
    }

    OvsdbClientKey(OvsdbClient client) {
        ipaddress = SouthboundMapper.createIpAddress(client.getConnectionInfo().getRemoteAddress());
        port = new PortNumber(client.getConnectionInfo().getRemotePort());
    }

    public IpAddress getIp() {
        return ipaddress;
    }

    public PortNumber getPort() {
        return port;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result
                + ((ipaddress == null) ? 0 : ipaddress.hashCode());
        result = prime * result + ((port == null) ? 0 : port.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        OvsdbClientKey other = (OvsdbClientKey) obj;
        if (ipaddress == null) {
            if (other.ipaddress != null)
                return false;
        } else if (!ipaddress.equals(other.ipaddress))
            return false;
        if (port == null) {
            if (other.port != null)
                return false;
        } else if (!port.equals(other.port))
            return false;
        return true;
    }

    InstanceIdentifier<Node> toInstanceIndentifier() {
        return SouthboundMapper.createInstanceIdentifier(ipaddress,port);
    }
}
