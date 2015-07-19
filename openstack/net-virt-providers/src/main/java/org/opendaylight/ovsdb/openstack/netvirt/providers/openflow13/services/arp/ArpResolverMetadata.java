/*
 * Copyright (c) 2015 Brocade Communications Systems, Inc. and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.ovsdb.openstack.netvirt.providers.openflow13.services.arp;

import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev100924.Ipv4Address;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.yang.types.rev100924.MacAddress;
import org.opendaylight.yang.gen.v1.urn.opendaylight.flow.service.rev130819.RemoveFlowInput;

/**
*
* @author Anil Vishnoi (avishnoi@Brocade.com)
*
*/

public final class ArpResolverMetadata {

    private final Ipv4Address gatewayIpAddress;
    private final Long externalNetworkBridgeDpid;
    private final Ipv4Address arpRequestSourceIp;
    private final MacAddress arpRequestSourceMacAddress;
    private final boolean periodicRefresh;
    private RemoveFlowInput flowToRemove;
    private MacAddress gatewayMacAddress;

    public ArpResolverMetadata(Long externalNetworkBridgeDpid,
            Ipv4Address gatewayIpAddress, Ipv4Address arpRequestSourceIp,
            MacAddress arpRequestMacAddress, boolean periodicRefresh){
        this.externalNetworkBridgeDpid = externalNetworkBridgeDpid;
        this.gatewayIpAddress = gatewayIpAddress;
        this.arpRequestSourceIp = arpRequestSourceIp;
        this.arpRequestSourceMacAddress = arpRequestMacAddress;
        this.periodicRefresh = periodicRefresh;
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
        this.gatewayMacAddress = gatewayMacAddress;
    }

    public Long getExternalNetworkBridgeDpid() {
        return externalNetworkBridgeDpid;
    }
    public Ipv4Address getArpRequestSourceIp() {
        return arpRequestSourceIp;
    }
    public MacAddress getArpRequestMacAddress() {
        return arpRequestSourceMacAddress;
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
