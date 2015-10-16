/*
 * Copyright (c) 2013, 2015 IBM Corporation and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt.translator;

import java.io.Serializable;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.Inet6Address;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.net.util.SubnetUtils;
import org.apache.commons.net.util.SubnetUtils.SubnetInfo;
import org.opendaylight.ovsdb.openstack.netvirt.translator.crud.NeutronCRUDInterfaces;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)

public class NeutronSubnet implements Serializable, INeutronObject {
    private static final Logger LOGGER = LoggerFactory
            .getLogger(NeutronCRUDInterfaces.class);

    private static final long serialVersionUID = 1L;
    private static final int IPV4_VERSION = 4;
    private static final int IPV6_VERSION = 6;
    private static final int IPV6_LENGTH = 128;
    private static final int IPV6_LENGTH_BYTES = 8;
    private static final long IPV6_LSB_MASK = 0x000000FF;
    private static final int IPV6_BYTE_OFFSET = 7;

    // See OpenStack Network API v2.0 Reference for description of
    // annotated attributes

    @XmlElement (name = "id")
    String subnetUUID;

    @XmlElement (name = "network_id")
    String networkUUID;

    @XmlElement (name = "name")
    String name;

    @XmlElement (defaultValue = "4", name = "ip_version")
    Integer ipVersion;

    @XmlElement (name = "cidr")
    String cidr;

    @XmlElement (name = "gateway_ip")
    String gatewayIP;

    @XmlElement (name = "dns_nameservers")
    List<String> dnsNameservers;

    @XmlElement (name = "allocation_pools")
    List<NeutronSubnetIPAllocationPool> allocationPools;

    @XmlElement (name = "host_routes")
    List<NeutronSubnet_HostRoute> hostRoutes;

    @XmlElement (defaultValue = "true", name = "enable_dhcp")
    Boolean enableDHCP;

    @XmlElement (name = "tenant_id")
    String tenantID;

    @XmlElement (name = "ipv6_address_mode", nillable = true)
    String ipV6AddressMode;

    @XmlElement (name = "ipv6_ra_mode", nillable = true)
    String ipV6RaMode;

    /* stores the OpenStackPorts associated with an instance
     * used to determine if that instance can be deleted.
     *
     * @deprecated, will be removed in Boron
     */

    List<NeutronPort> myPorts;

    public NeutronSubnet() {
        myPorts = new ArrayList<NeutronPort>();
    }

    // @deprecated - will be removed in Boron
    public void setPorts(List<NeutronPort> arg) {
        myPorts = arg;
    }

    public String getID() { return subnetUUID; }

    public void setID(String id) { this.subnetUUID = id; }

    public String getSubnetUUID() {
        return subnetUUID;
    }

    public void setSubnetUUID(String subnetUUID) {
        this.subnetUUID = subnetUUID;
    }

    public String getNetworkUUID() {
        return networkUUID;
    }

    public void setNetworkUUID(String networkUUID) {
        this.networkUUID = networkUUID;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getIpVersion() {
        return ipVersion;
    }

    public void setIpVersion(Integer ipVersion) {
        this.ipVersion = ipVersion;
    }

    public String getCidr() {
        return cidr;
    }

    public void setCidr(String cidr) {
        this.cidr = cidr;
    }

    public String getGatewayIP() {
        return gatewayIP;
    }

    public void setGatewayIP(String gatewayIP) {
        this.gatewayIP = gatewayIP;
    }

    public List<String> getDnsNameservers() {
        return dnsNameservers;
    }

    public void setDnsNameservers(List<String> dnsNameservers) {
        this.dnsNameservers = dnsNameservers;
    }

    public List<NeutronSubnetIPAllocationPool> getAllocationPools() {
        return allocationPools;
    }

    public void setAllocationPools(List<NeutronSubnetIPAllocationPool> allocationPools) {
        this.allocationPools = allocationPools;
    }

    public List<NeutronSubnet_HostRoute> getHostRoutes() {
        return hostRoutes;
    }

    public void setHostRoutes(List<NeutronSubnet_HostRoute> hostRoutes) {
        this.hostRoutes = hostRoutes;
    }

    public boolean isEnableDHCP() {
        if (enableDHCP == null) {
            return true;
        }
        return enableDHCP;
    }

    public Boolean getEnableDHCP() { return enableDHCP; }

    public void setEnableDHCP(Boolean newValue) {
            enableDHCP = newValue;
    }

    public String getTenantID() {
        return tenantID;
    }

    public void setTenantID(String tenantID) {
        this.tenantID = tenantID;
    }

    public String getIpV6AddressMode() { return ipV6AddressMode; }

    public void setIpV6AddressMode(String ipV6AddressMode) { this.ipV6AddressMode = ipV6AddressMode; }

    public String getIpV6RaMode() { return ipV6RaMode; }

    public void setIpV6RaMode(String ipV6RaMode) { this.ipV6RaMode = ipV6RaMode; }

    /**
     * This method copies selected fields from the object and returns them
     * as a new object, suitable for marshaling.
     *
     * @param fields
     *            List of attributes to be extracted
     * @return an OpenStackSubnets object with only the selected fields
     * populated
     */

    public NeutronSubnet extractFields(List<String> fields) {
        NeutronSubnet ans = new NeutronSubnet();
        Iterator<String> i = fields.iterator();
        while (i.hasNext()) {
            String s = i.next();
            if (s.equals("id")) {
                ans.setSubnetUUID(this.getSubnetUUID());
            }
            if (s.equals("network_id")) {
                ans.setNetworkUUID(this.getNetworkUUID());
            }
            if (s.equals("name")) {
                ans.setName(this.getName());
            }
            if (s.equals("ip_version")) {
                ans.setIpVersion(this.getIpVersion());
            }
            if (s.equals("cidr")) {
                ans.setCidr(this.getCidr());
            }
            if (s.equals("gateway_ip")) {
                ans.setGatewayIP(this.getGatewayIP());
            }
            if (s.equals("dns_nameservers")) {
                List<String> nsList = new ArrayList<String>();
                nsList.addAll(this.getDnsNameservers());
                ans.setDnsNameservers(nsList);
            }
            if (s.equals("allocation_pools")) {
                List<NeutronSubnetIPAllocationPool> aPools = new ArrayList<NeutronSubnetIPAllocationPool>();
                aPools.addAll(this.getAllocationPools());
                ans.setAllocationPools(aPools);
            }
            if (s.equals("host_routes")) {
                List<NeutronSubnet_HostRoute> hRoutes = new ArrayList<NeutronSubnet_HostRoute>();
                hRoutes.addAll(this.getHostRoutes());
                ans.setHostRoutes(hRoutes);
            }
            if (s.equals("enable_dhcp")) {
                ans.setEnableDHCP(this.getEnableDHCP());
            }
            if (s.equals("tenant_id")) {
                ans.setTenantID(this.getTenantID());
            }
            if (s.equals("ipv6_address_mode")) {
                ans.setIpV6AddressMode(this.getIpV6AddressMode());
            }
            if (s.equals("ipv6_ra_mode")) {
                ans.setIpV6RaMode(this.getIpV6RaMode());
            }
        }
        return ans;
    }

    // @deprecated - will be removed in Boron
    public List<NeutronPort> getPortsInSubnet() {
        return myPorts;
    }

    // @deprecated - will be removed in Boron
    public List<NeutronPort> getPortsInSubnet(String ignore) {
       List<NeutronPort> answer = new ArrayList<NeutronPort>();
       for (NeutronPort port : myPorts) {
           if (!port.getDeviceOwner().equalsIgnoreCase(ignore)) {
                answer.add(port);
            }
        }
        return answer;
    }

    /* test to see if the cidr address used to define this subnet
     * is a valid network address (an necessary condition when creating
     * a new subnet)
     */
    public boolean isValidCIDR() {
        // fix for Bug 2290 - need to wrap the existing test as
        // IPv4 because SubnetUtils doesn't support IPv6
        if (ipVersion == IPV4_VERSION) {
            try {
                SubnetUtils util = new SubnetUtils(cidr);
                SubnetInfo info = util.getInfo();
                if (!info.getNetworkAddress().equals(info.getAddress())) {
                    return false;
                }
            } catch (IllegalArgumentException e) {
                LOGGER.warn("Failure in isValidCIDR()", e);
                return false;
            }
            return true;
        }
        if (ipVersion == IPV6_VERSION) {
            // fix for Bug2290 - this is custom code because no classes
            // with ODL-friendly licenses have been found
            // extract address (in front of /) and length (after /)
            String[] parts = cidr.split("/");
            if (parts.length != 2) {
                return false;
            }
            try {
                int length = Integer.parseInt(parts[1]);
                //TODO?: limit check on length
                // convert to byte array
                byte[] addrBytes = ((Inet6Address) InetAddress.getByName(parts[0])).getAddress();
                int i;
                for (i = length; i < IPV6_LENGTH; i++) {
                    if (((((int) addrBytes[i/IPV6_LENGTH_BYTES]) & IPV6_LSB_MASK) & (1 << (IPV6_BYTE_OFFSET-(i%IPV6_LENGTH_BYTES)))) != 0) {
                        return(false);
                    }
                }
                return(true);
            } catch (UnknownHostException e) {
                LOGGER.warn("Failure in isValidCIDR()", e);
                return(false);
            }
        }
        return false;
    }

    /* test to see if the gateway IP specified overlaps with specified
     * allocation pools (an error condition when creating a new subnet
     * or assigning a gateway IP)
     */
    public boolean gatewayIP_Pool_overlap() {
        Iterator<NeutronSubnetIPAllocationPool> i = allocationPools.iterator();
        while (i.hasNext()) {
            NeutronSubnetIPAllocationPool pool = i.next();
            if (ipVersion == IPV4_VERSION && pool.contains(gatewayIP)) {
                return true;
            }
            if (ipVersion == IPV6_VERSION && pool.containsV6(gatewayIP)) {
                return true;
            }
        }
        return false;
    }

    public boolean initDefaults() {
        if (enableDHCP == null) {
            enableDHCP = true;
        }
        if (ipVersion == null) {
            ipVersion = IPV4_VERSION;
        }
        dnsNameservers = new ArrayList<String>();
        if (hostRoutes == null) {
            hostRoutes = new ArrayList<NeutronSubnet_HostRoute>();
        }
        if (allocationPools == null) {
            allocationPools = new ArrayList<NeutronSubnetIPAllocationPool>();
            if (ipVersion == IPV4_VERSION) {
                try {
                    SubnetUtils util = new SubnetUtils(cidr);
                    SubnetInfo info = util.getInfo();
                    if (gatewayIP == null || ("").equals(gatewayIP)) {
                        gatewayIP = info.getLowAddress();
                    }
                    if (allocationPools.size() < 1) {
                        NeutronSubnetIPAllocationPool source =
                            new NeutronSubnetIPAllocationPool(info.getLowAddress(),
                                    info.getHighAddress());
                        allocationPools = source.splitPool(gatewayIP);
                    }
                } catch (IllegalArgumentException e) {
                    LOGGER.warn("Failure in initDefault()", e);
                    return false;
                }
            }
            if (ipVersion == IPV6_VERSION) {
                String[] parts = cidr.split("/");
                if (parts.length != 2) {
                    return false;
                }
                try {
                    int length = Integer.parseInt(parts[1]);
                    BigInteger lowAddress_bi = NeutronSubnetIPAllocationPool.convertV6(parts[0]);
                    String lowAddress = NeutronSubnetIPAllocationPool.bigIntegerToIP(lowAddress_bi.add(BigInteger.ONE));
                    BigInteger mask = BigInteger.ONE.shiftLeft(length).subtract(BigInteger.ONE);
                    String highAddress = NeutronSubnetIPAllocationPool.bigIntegerToIP(lowAddress_bi.add(mask).subtract(BigInteger.ONE));
                    if (gatewayIP == null || ("").equals(gatewayIP)) {
                        gatewayIP = lowAddress;
                    }
                    if (allocationPools.size() < 1) {
                        NeutronSubnetIPAllocationPool source =
                            new NeutronSubnetIPAllocationPool(lowAddress,
                                    highAddress);
                        allocationPools = source.splitPoolV6(gatewayIP);
                    }
                } catch (Exception e) {
                    LOGGER.warn("Failure in initDefault()", e);
                    return false;
                }
            }
        }
        return true;
    }

    /* this method tests to see if the supplied IPv4 address
     * is valid for this subnet or not
     */
    public boolean isValidIP(String ipAddress) {
        if (ipVersion == IPV4_VERSION) {
            try {
                SubnetUtils util = new SubnetUtils(cidr);
                SubnetInfo info = util.getInfo();
                return info.isInRange(ipAddress);
            } catch (IllegalArgumentException e) {
                LOGGER.warn("Failure in isValidIP()", e);
                return false;
            }
        }

        if (ipVersion == IPV6_VERSION) {
            String[] parts = cidr.split("/");
            try {
                int length = Integer.parseInt(parts[1]);
                byte[] cidrBytes = ((Inet6Address) InetAddress.getByName(parts[0])).getAddress();
                byte[] ipBytes =  ((Inet6Address) InetAddress.getByName(ipAddress)).getAddress();
                int i;
                for (i = 0; i < length; i++) {
                    if (((((int) cidrBytes[i/IPV6_LENGTH_BYTES]) & IPV6_LSB_MASK) & (1 << (IPV6_BYTE_OFFSET-(i%IPV6_LENGTH_BYTES)))) !=
                        ((((int) ipBytes[i/IPV6_LENGTH_BYTES]) & IPV6_LSB_MASK) & (1 << (IPV6_BYTE_OFFSET-(i%IPV6_LENGTH_BYTES))))) {
                        return(false);
                    }
                }
                return(true);
            } catch (UnknownHostException e) {
                LOGGER.warn("Failure in isValidIP()", e);
                return(false);
            }
        }
        return false;
    }

    /* method to get the lowest available address of the subnet.
     * go through all the allocation pools and keep the lowest of their
     * low addresses.
     */
    public String getLowAddr() {
        String ans = null;
        Iterator<NeutronSubnetIPAllocationPool> i = allocationPools.iterator();
        while (i.hasNext()) {
            NeutronSubnetIPAllocationPool pool = i.next();
            if (ans == null) {
                ans = pool.getPoolStart();
            }
            else {
                if (ipVersion == IPV4_VERSION &&
                    NeutronSubnetIPAllocationPool.convert(pool.getPoolStart()) <
                            NeutronSubnetIPAllocationPool.convert(ans)) {
                    ans = pool.getPoolStart();
                }
                if (ipVersion == IPV6_VERSION &&
                    NeutronSubnetIPAllocationPool.convertV6(pool.getPoolStart()).compareTo(NeutronSubnetIPAllocationPool.convertV6(ans)) < 0) {
                    ans = pool.getPoolStart();
                }
           }
        }
        return ans;
    }

    @Override
    public String toString() {
        return "NeutronSubnet [subnetUUID=" + subnetUUID + ", networkUUID=" + networkUUID + ", name=" + name
                + ", ipVersion=" + ipVersion + ", cidr=" + cidr + ", gatewayIP=" + gatewayIP + ", dnsNameservers="
                + dnsNameservers + ", allocationPools=" + allocationPools + ", hostRoutes=" + hostRoutes
                + ", enableDHCP=" + enableDHCP + ", tenantID=" + tenantID
                + ", ipv6AddressMode=" + ipV6AddressMode
                + ", ipv6RaMode=" + ipV6RaMode + "]";
    }
}
