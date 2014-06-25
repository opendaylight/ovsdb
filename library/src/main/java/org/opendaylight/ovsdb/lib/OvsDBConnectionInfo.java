/*
 * Copyright (C) 2014 Red Hat, Inc.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Madhu Venugopal
 */

package org.opendaylight.ovsdb.lib;

import java.net.InetAddress;
public class OvsDBConnectionInfo {
    public enum ConnectionType {
        ACTIVE, PASSIVE
    }

    InetAddress address;
    int port;
    ConnectionType type;

    public OvsDBConnectionInfo(InetAddress address, int port, ConnectionType type) {
        this.address = address;
        this.port = port;
        this.type = type;
    }

    public InetAddress getAddress() {
        return address;
    }
    public int getPort() {
        return port;
    }
    public ConnectionType getType() {
        return type;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((address == null) ? 0 : address.hashCode());
        result = prime * result + port;
        result = prime * result + ((type == null) ? 0 : type.hashCode());
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
        OvsDBConnectionInfo other = (OvsDBConnectionInfo) obj;
        if (address == null) {
            if (other.address != null)
                return false;
        } else if (!address.equals(other.address))
            return false;
        if (port != other.port)
            return false;
        if (type != other.type)
            return false;
        return true;
    }
    @Override
    public String toString() {
        return "ConnectionInfo [address=" + address + ", port=" + port
                + ", type=" + type + "]";
    }
}
