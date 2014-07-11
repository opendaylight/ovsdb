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

import io.netty.channel.Channel;

import java.net.InetAddress;
import java.net.InetSocketAddress;
public class OvsdbConnectionInfo {
    public enum ConnectionType {
        ACTIVE, PASSIVE
    }

    private Channel channel;
    private ConnectionType type;

    public OvsdbConnectionInfo(Channel channel, ConnectionType type) {
        this.channel = channel;
        this.type = type;
    }

    public InetAddress getRemoteAddress() {
        return ((InetSocketAddress)channel.remoteAddress()).getAddress();
    }
    public int getRemotePort() {
        return ((InetSocketAddress)channel.remoteAddress()).getPort();
    }

    public InetAddress getLocalAddress() {
        return ((InetSocketAddress)channel.localAddress()).getAddress();
    }
    public int getLocalPort() {
        return ((InetSocketAddress)channel.localAddress()).getPort();
    }

    public ConnectionType getType() {
        return type;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((channel == null) ? 0 : getRemoteAddress().hashCode());
        result = prime * result + ((type == null) ? 0 : getRemotePort());
        result = prime * result + ((channel == null) ? 0 : getLocalAddress().hashCode());
        result = prime * result + ((type == null) ? 0 : getLocalPort());
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
        OvsdbConnectionInfo other = (OvsdbConnectionInfo) obj;
        if (channel == null) {
            if (other.channel != null)
                return false;
        } else if (!getRemoteAddress().equals(other.getRemoteAddress())) {
            return false;
        } else if (!getLocalAddress().equals(other.getLocalAddress())) {
            return false;
        } else if (getRemotePort() != other.getRemotePort()) {
            return false;
        } else if (getLocalPort() != other.getLocalPort()) {
            return false;
        }
        if (type != other.type)
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "ConnectionInfo [Remote-address=" + this.getRemoteAddress().getHostAddress() +
                             ", Remote-port=" + this.getRemotePort() +
                             ", Local-address" + this.getLocalAddress().getHostAddress() +
                             ", Local-port=" + this.getLocalPort() +
                             ", type=" + type + "]";
    }
}
