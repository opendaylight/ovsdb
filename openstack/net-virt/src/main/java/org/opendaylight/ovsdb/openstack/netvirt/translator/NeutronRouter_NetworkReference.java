/*
 * Copyright (c) 2013, 2015 IBM Corporation and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */

package org.opendaylight.ovsdb.openstack.netvirt.translator;

import java.io.Serializable;

import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.NONE)

public class NeutronRouter_NetworkReference implements Serializable {
    private static final long serialVersionUID = 1L;

    // See OpenStack Network API v2.0 Reference for description of
    // annotated attributes

    @XmlElement(name = "network_id")
    String networkID;

    @XmlElement(name = "enable_snat")
    Boolean enableSNAT;

    @XmlElement(name = "external_fixed_ips")
    List<Neutron_IPs> externalFixedIPs;

    public NeutronRouter_NetworkReference() {
    }

    public String getNetworkID() {
        return networkID;
    }

    public void setNetworkID(String networkID) {
        this.networkID = networkID;
    }

    public Boolean getEnableSNAT() {
        return enableSNAT;
    }

    public void setEnableSNAT(Boolean enableSNAT) {
        this.enableSNAT = enableSNAT;
    }

    public List<Neutron_IPs> getExternalFixedIPs() {
        return externalFixedIPs;
    }

    public void setExternalFixedIPs(List<Neutron_IPs> externalFixedIPs) {
        this.externalFixedIPs = externalFixedIPs;
    }

    @Override
    public String toString() {
        return "NeutronRouterNetworkReference [networkID=" + networkID +
            " enableSNAT=" + enableSNAT +
            " externalFixedIPs=" + externalFixedIPs + "]";
    }

}
