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

    private RemoveFlowInput flowToRemove;
    private Ipv4Address gatewayIpAddress;
    private MacAddress gatewayMacAddress;
    private boolean periodicRefresh;

    public ArpResolverMetadata(){

    }
    public ArpResolverMetadata(RemoveFlowInput flowToRemove, Ipv4Address gatewayIpAddress, boolean periodicRefresh){
        this.flowToRemove = flowToRemove;
        this.gatewayIpAddress = gatewayIpAddress;
        this.periodicRefresh = periodicRefresh;
    }

    public ArpResolverMetadata(RemoveFlowInput flowToRemove, Ipv4Address gatewayIpAddress, MacAddress gatewayMacAddress, boolean periodicRefresh){
        this.flowToRemove = flowToRemove;
        this.gatewayIpAddress = gatewayIpAddress;
        this.gatewayMacAddress = gatewayMacAddress;
        this.periodicRefresh = periodicRefresh;
    }

    public RemoveFlowInput getFlowToRemove() {
        return flowToRemove;
    }
    public boolean isPeriodicRefresh() {
        return periodicRefresh;
    }
    public void setPeriodicRefresh(boolean periodicRefresh) {
        this.periodicRefresh = periodicRefresh;
    }
    public void setFlowToRemove(RemoveFlowInput flowToRemove) {
        this.flowToRemove = flowToRemove;
    }
    public Ipv4Address getGatewayIpAddress() {
        return gatewayIpAddress;
    }
    public void setGatewayIpAddress(Ipv4Address gatewayIpAddress) {
        this.gatewayIpAddress = gatewayIpAddress;
    }
    public MacAddress getGatewayMacAddress() {
        return gatewayMacAddress;
    }
    public void setGatewayMacAddress(MacAddress gatewayMacAddress) {
        this.gatewayMacAddress = gatewayMacAddress;
    }

}
