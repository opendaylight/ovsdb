/*
 * Copyright (c) 2014, 2015 SDN Hub, LLC. and others. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt.api;

import com.google.common.collect.Maps;

import java.util.Map;

/**
 * Store configuration for each load balancer instance created.
 */

public class LoadBalancerConfiguration {
    public static final String PROTOCOL_TCP = "TCP";
    public static final String PROTOCOL_HTTP = "HTTP";
    public static final String PROTOCOL_HTTPS = "HTTPS";

    public class LoadBalancerPoolMember {
        String ipAddr;
        String macAddr;
        String protocol;
        Integer port;
        int index;

        public LoadBalancerPoolMember(String ipAddr, String macAddr, String protocol, Integer port) {
            this.ipAddr = ipAddr;
            this.macAddr = macAddr;
            this.protocol = protocol;
            this.port = port;
            this.index = -1;
        }
        public LoadBalancerPoolMember(String ipAddr, String macAddr, String protocol, Integer port, int index) {
            this.ipAddr = ipAddr;
            this.macAddr = macAddr;
            this.protocol = protocol;
            this.port = port;
            this.index = index;
        }
        public String getIP() {
            return ipAddr;
        }
        public String getMAC() {
            return macAddr;
        }
        public String getProtocol() {
            return protocol;
        }
        public Integer getPort() {
            return port;
        }
        public int getIndex() {
            return index;
        }
        public void setIndex(int index) {
            this.index = index;
        }

        /**
         * Overridden equals() where index is not checked.
         */
        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            LoadBalancerPoolMember other = (LoadBalancerPoolMember) obj;
            if (ipAddr == null) {
                if (other.ipAddr != null) {
                    return false;
                }
            } else if (!ipAddr.equals(other.ipAddr)) {
                return false;
            }
            if (macAddr == null) {
                if (other.macAddr != null) {
                    return false;
                }
            } else if (!macAddr.equals(other.macAddr)) {
                return false;
            }
            if (port == null) {
                if (other.port != null) {
                    return false;
                }
            } else if (!port.equals(other.port)) {
                return false;
            }
            if (protocol == null) {
                if (other.protocol != null) {
                    return false;
                }
            } else if (!protocol.equals(other.protocol)) {
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            return "LoadBalancerPoolMember [ip=" + ipAddr + ", mac=" + macAddr +
                    ", protocol=" + protocol + ", port=" + port + ", index=" + index + "]";
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((ipAddr == null) ? 0 : ipAddr.hashCode());
            result = prime * result + ((macAddr == null) ? 0 : macAddr.hashCode());
            result = prime * result + ((protocol == null) ? 0 : protocol.hashCode());
            result = prime * result + ((port == null) ? 0 : port.hashCode());
            return result;
        }
    }

    private String name;
    private String vip;
    private String vmac; //Used when a dummy neutron port is created for the VIP
    private String providerNetworkType;
    private String providerSegmentationId;
    private Map <String, LoadBalancerPoolMember> members;

    public LoadBalancerConfiguration() {
        this.members = Maps.newHashMap();
    }

    public LoadBalancerConfiguration(String name, String vip) {
        this.members = Maps.newHashMap();
        this.name = name;
        this.vip = vip;
        this.vmac = null;
    }

    public LoadBalancerConfiguration(String name, String vip, String vmac) {
        this.members = Maps.newHashMap();
        this.name = name;
        this.vip = vip;
        this.vmac = vmac;
    }

    public LoadBalancerConfiguration(LoadBalancerConfiguration lbConfig) {
        this.members = Maps.newHashMap(lbConfig.getMembers());
        this.name = lbConfig.getName();
        this.vip = lbConfig.getVip();
        this.vmac = lbConfig.getVmac();
        this.providerNetworkType = lbConfig.getProviderNetworkType();
        this.providerSegmentationId = lbConfig.getProviderSegmentationId();
    }

    public Map<String, LoadBalancerPoolMember> getMembers() {
        return this.members;
    }

    public Map<String, LoadBalancerPoolMember> addMember(String uuid, LoadBalancerPoolMember member) {
        //If index is not set for this object, update it before inserting
        if (member.getIndex() == -1) {
            member.setIndex(members.size());
        }
        this.members.put(uuid, member);
        return this.members;
    }
    public Map<String, LoadBalancerPoolMember> addMember(String uuid, String ipAddr, String macAddr, String protocol, Integer port) {
        this.members.put(uuid,
                new LoadBalancerPoolMember(ipAddr, macAddr, protocol, port, members.size()));
        return this.members;
    }
    public Map<String, LoadBalancerPoolMember> removeMember(String uuid) {
        this.members.remove(uuid);
        /* Update indices of all other members
         */
        int index = 0;
        for(Map.Entry<String, LoadBalancerPoolMember> entry : this.getMembers().entrySet()) {
            ((LoadBalancerPoolMember) entry.getValue()).setIndex(index++);
        }
        return this.members;
    }

    public boolean isValid() {
        if (members.size() == 0) {
            return false;
        } else if (providerNetworkType == null) {
            return false;
        }
        return true;
    }

    public void setVip(String vip) {
        this.vip = vip;
    }

    public String getVip() {
        return this.vip;
    }

    public void setVmac(String vmac) {
        this.vmac = vmac;
    }

    public String getVmac() {
        return this.vmac;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }

    public void setProviderSegmentationId(String providerSegmentationId) {
        this.providerSegmentationId = providerSegmentationId;
    }

    public String getProviderSegmentationId() {
        return this.providerSegmentationId;
    }
    public void setProviderNetworkType(String providerNetworkType) {
        this.providerNetworkType = providerNetworkType;
    }

    public String getProviderNetworkType() {
        return this.providerNetworkType;
    }

    @Override
    public String toString() {
        return "LoadBalancerConfiguration [name=" + name +
                ", vip=" + vip + ", vmac=" + vmac +
                ", networkType=" + providerNetworkType +
                ", segmentationId=" + providerSegmentationId +
                ", members=" + members + "]";
    }
}
