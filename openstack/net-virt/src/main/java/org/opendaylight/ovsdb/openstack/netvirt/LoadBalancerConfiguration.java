/*
 * Copyright (C) 2014 SDN Hub, LLC.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Srini Seetharaman
 */

package org.opendaylight.ovsdb.openstack.netvirt;

import com.google.common.collect.Maps;
import java.util.Map;

/**
 * Store configuration for each load balancer instance created.
 */

public class LoadBalancerConfiguration {

    private class LoadBalancerPoolMember {
        String ipAddr;
        String macAddr;
        String protocol;
        Integer port;

        public LoadBalancerPoolMember(String ipAddr, String macAddr, String protocol, Integer port) {
            this.ipAddr = ipAddr;
            this.macAddr = macAddr;
            this.protocol = protocol;
            this.port = port;
        }

        public boolean equals(LoadBalancerPoolMember other) {
            if (other.ipAddr != ipAddr)
                return false;
            else if (other.macAddr != macAddr)
                return false;
            else if (other.protocol != protocol)
                return false;
            else if (other.port != port)
                return false;
            else
                return true;
        }

        @Override
        public String toString() {
            return "LoadBalancerPoolMember [ip=" + ipAddr + ", mac=" + macAddr +
                    ", protocol=" + protocol + ", port=" + port + "]";
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = super.hashCode();
            result = prime * result + ((port == null) ? 0 : ipAddr.hashCode());
            result = prime * result + ((port == null) ? 0 : macAddr.hashCode());
            result = prime * result + ((port == null) ? 0 : protocol.hashCode());
            result = prime * result + ((port == null) ? 0 : port.hashCode());
            return result;
        }
    }

    private String name;
    private String vip;
    private Map <String, LoadBalancerPoolMember> members;

    public LoadBalancerConfiguration() {
        this.members = Maps.newHashMap();
    }

    public LoadBalancerConfiguration(String name, String vip) {
        this.members = Maps.newHashMap();
        this.name = name;
        this.vip = vip;
    }

    public Map<String, LoadBalancerPoolMember> getMembers() {
        return this.members;
    }
    public Map<String, LoadBalancerPoolMember> addMember(String uuid, String ipAddr, String macAddr, String protocol, Integer port) {
        this.members.put(uuid,
                new LoadBalancerPoolMember(ipAddr, macAddr, protocol, port));
        return this.members;
    }
    public Map<String, LoadBalancerPoolMember> removeMember(String uuid) {
        this.members.remove(uuid);
        return this.members;
    }

    public boolean isValid() {
        if (members.size() == 0)
            return false;
        return true;
    }
    public void setVip(String vip) {
        this.vip = vip;
    }

    public String getVip() {
        return this.vip;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }
}

