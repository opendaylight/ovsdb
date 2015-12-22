/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.services.arp;

import org.opendaylight.controller.liblldp.NetUtils;
import org.opendaylight.ovsdb.openstack.netvirt.api.GatewayMacResolverListener;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.IpAddress;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.RemoveFlowInput;

/**
*
* @author Anil Vishnoi (avishnoi@Brocade.com)
*
*/

public final class ArpResolverMetadata {

    private final GatewayMacResolverListener gatewayMacResolverListener;
    private final Ipv4Address gatewayIpAddress;
    private final Long externalNetworkBridgeDpid;
    private final Ipv4Address arpRequestSourceIp;
    private final MacAddress arpRequestSourceMacAddress;
    private final boolean periodicRefresh;
    private RemoveFlowInput flowToRemove;
    private MacAddress gatewayMacAddress;
    private boolean gatewayMacAddressResolved;
    private int numberOfOutstandingArpRequests;
    private static final int MAX_OUTSTANDING_ARP_REQUESTS = 2;

    public ArpResolverMetadata(final GatewayMacResolverListener gatewayMacResolverListener,
                               final Long externalNetworkBridgeDpid,
            final Ipv4Address gatewayIpAddress, final Ipv4Address arpRequestSourceIp,
            final MacAddress arpRequestMacAddress, final boolean periodicRefresh){
        this.gatewayMacResolverListener = gatewayMacResolverListener;
        this.externalNetworkBridgeDpid = externalNetworkBridgeDpid;
        this.gatewayIpAddress = gatewayIpAddress;
        this.arpRequestSourceIp = arpRequestSourceIp;
        this.arpRequestSourceMacAddress = arpRequestMacAddress;
        this.periodicRefresh = periodicRefresh;
        this.gatewayMacAddress = null;
        this.gatewayMacAddressResolved = false;
        this.numberOfOutstandingArpRequests = 0;
    }

    public RemoveFlowInput getFlowToRemove() {
        return flowToRemove;
    }
    public boolean isPeriodicRefresh() {
        return periodicRefresh;
    }
    public void setFlowToRemove(RemoveFlowInput flowToRemove) {
        this.flowToRemove = flowToRemove;
    }
    public Ipv4Address getGatewayIpAddress() {
        return gatewayIpAddress;
    }
    public MacAddress getGatewayMacAddress() {
        return gatewayMacAddress;
    }
    public void setGatewayMacAddress(MacAddress gatewayMacAddress) {
        if (gatewayMacAddress != null) {
            if (gatewayMacResolverListener != null &&
                    !gatewayMacAddress.equals(this.gatewayMacAddress)) {
                gatewayMacResolverListener.gatewayMacResolved(externalNetworkBridgeDpid,
                        new IpAddress(gatewayIpAddress), gatewayMacAddress);
            }
            gatewayMacAddressResolved = true;
            numberOfOutstandingArpRequests = 0;
        } else {
            gatewayMacAddressResolved = false;
        }
        this.gatewayMacAddress = gatewayMacAddress;
    }

    public Long getExternalNetworkBridgeDpid() {
        return externalNetworkBridgeDpid;
    }
    public Ipv4Address getArpRequestSourceIp() {
        return arpRequestSourceIp;
    }
    public MacAddress getArpRequestSourceMacAddress() {
        return arpRequestSourceMacAddress;
    }

    /**
     * This method is used to determine whether to use the broadcast MAC or the unicast MAC as the destination address
     * for an ARP request packet based on whether one of the last MAX_OUTSTANDING_ARP_REQUESTS requests has been
     * answered.
     *
     * A counter (numberOfOutstandingArpRequests) is maintained to track outstanding ARP requests.  This counter is
     * incremented in this method and reset when setGatewayMacAddress() is called with an updated MAC address after an
     * ARP reply is received. It is therefore expected that this method be called exactly once for each ARP request
     * event, and not be called for other reasons, or it may result in more broadcast ARP request packets being sent
     * than needed.
     *
     * @return Destination MAC address to be used in ARP request packet:  Either the unicast MAC or the broadcast MAC
     * as described above.
     */
    public MacAddress getArpRequestDestMacAddress() {

        numberOfOutstandingArpRequests++;

        if (numberOfOutstandingArpRequests > MAX_OUTSTANDING_ARP_REQUESTS) {
            gatewayMacAddressResolved = false;
        }

        if (gatewayMacAddressResolved) {
            return gatewayMacAddress;
        } else {
            return ArpUtils.bytesToMac(NetUtils.getBroadcastMACAddr());
        }
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime
                * result
                + ((arpRequestSourceMacAddress == null) ? 0 : arpRequestSourceMacAddress
                        .hashCode());
        result = prime
                * result
                + ((arpRequestSourceIp == null) ? 0 : arpRequestSourceIp
                        .hashCode());
        result = prime
                * result
                + ((externalNetworkBridgeDpid == null) ? 0
                        : externalNetworkBridgeDpid.hashCode());
        result = prime
                * result
                + ((gatewayIpAddress == null) ? 0 : gatewayIpAddress.hashCode());
        result = prime * result + (periodicRefresh ? 1231 : 1237);
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
        ArpResolverMetadata other = (ArpResolverMetadata) obj;
        if (arpRequestSourceMacAddress == null) {
            if (other.arpRequestSourceMacAddress != null)
                return false;
        } else if (!arpRequestSourceMacAddress.equals(other.arpRequestSourceMacAddress))
            return false;
        if (arpRequestSourceIp == null) {
            if (other.arpRequestSourceIp != null)
                return false;
        } else if (!arpRequestSourceIp.equals(other.arpRequestSourceIp))
            return false;
        if (externalNetworkBridgeDpid == null) {
            if (other.externalNetworkBridgeDpid != null)
                return false;
        } else if (!externalNetworkBridgeDpid
                .equals(other.externalNetworkBridgeDpid))
            return false;
        if (gatewayIpAddress == null) {
            if (other.gatewayIpAddress != null)
                return false;
        } else if (!gatewayIpAddress.equals(other.gatewayIpAddress))
            return false;
        if (periodicRefresh != other.periodicRefresh)
            return false;
        return true;
    }

}
